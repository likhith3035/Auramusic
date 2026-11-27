package com.music.bitchord.desktop.connect

import com.music.bitchord.data.connect.AuraCommandRequest
import com.music.bitchord.data.connect.AuraConnectConstants
import com.music.bitchord.data.connect.AuraConnectDevice
import com.music.bitchord.data.connect.AuraDeviceType
import com.music.bitchord.data.connect.AuraErrorResponse
import com.music.bitchord.data.connect.AuraPairRequest
import com.music.bitchord.data.connect.AuraPairResponse
import com.music.bitchord.data.connect.AuraPlaybackState
import com.music.bitchord.data.connect.AuraStatusResponse
import com.music.bitchord.data.connect.AuraTransferSession
import com.music.bitchord.data.connect.toSong
import com.music.bitchord.data.model.Song
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Aura Connect Host daemon running inside AuraMusic Desktop.
 *
 * Exposes:
 * 1. UDP discovery responder and beacon broadcaster on port 8764.
 * 2. PIN-paired, bearer-authenticated HTTP control server on port 8765.
 */
object AuraConnectHost {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val random = SecureRandom()
    private val hexChars = "0123456789abcdef".toCharArray()

    private var httpServer: HttpServer? = null
    private var executor: ExecutorService? = null
    private var udpSocket: DatagramSocket? = null
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var broadcastJob: Job? = null
    private var listenJob: Job? = null

    private val issuedTokens = ConcurrentHashMap.newKeySet<String>()
    private val pairAttempts = ConcurrentHashMap<String, PairAttemptWindow>()

    private data class PairAttemptWindow(val startedMs: Long, val failures: Int)

    private val _pairingPin = MutableStateFlow(generatePin())
    val pairingPin: StateFlow<String> = _pairingPin.asStateFlow()

    private val _pairedCount = MutableStateFlow(0)
    val pairedCount: StateFlow<Int> = _pairedCount.asStateFlow()

    private val deviceId: String = "desktop_" + UUID.randomUUID().toString().take(8)
    private var deviceName: String = runCatching {
        val host = InetAddress.getLocalHost().hostName
        if (host.isNotBlank()) "PC ($host)" else "Desktop PC"
    }.getOrDefault("Desktop PC")

    private var boundPort: Int = AuraConnectConstants.DEFAULT_HTTP_PORT

    // Callbacks to the active Desktop playback engine and state
    var getState: () -> AuraPlaybackState = { AuraPlaybackState() }
    var onPlay: () -> Unit = {}
    var onPause: () -> Unit = {}
    var onTogglePlayPause: () -> Unit = {}
    var onNext: () -> Unit = {}
    var onPrevious: () -> Unit = {}
    var onSeek: (Long) -> Unit = {}
    var onSetVolume: (Float) -> Unit = {}
    var onToggleMute: () -> Unit = {}
    var onPlayIndex: (Int) -> Unit = {}
    var onRemoveIndex: (Int) -> Unit = {}
    var onMoveIndex: (from: Int, to: Int) -> Unit = { _, _ -> }
    var onAddTrack: (track: Song, playNext: Boolean) -> Unit = { _, _ -> }
    var onClearQueue: () -> Unit = {}
    var onToggleShuffle: () -> Unit = {}
    var onToggleRepeat: () -> Unit = {}
    var onSetEqPreset: (String) -> Unit = {}
    var onTransferSession: (AuraTransferSession) -> Unit = {}

    private fun generatePin(): String = "%06d".format(random.nextInt(1_000_000))

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            out[i * 2] = hexChars[v ushr 4]
            out[i * 2 + 1] = hexChars[v and 0x0F]
        }
        return String(out)
    }

    fun regeneratePairingPin() {
        _pairingPin.value = generatePin()
        pairAttempts.clear()
    }

    fun revokeAllClients() {
        issuedTokens.clear()
        _pairedCount.value = 0
        regeneratePairingPin()
    }

    /**
     * Starts the Aura Connect HTTP server and UDP discovery broadcaster.
     */
    fun start(customDeviceName: String? = null) {
        if (httpServer != null) return
        if (customDeviceName != null && customDeviceName.isNotBlank()) {
            deviceName = customDeviceName
        }
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // 1. Try binding HTTP server on 8765 or subsequent ports
        var port = AuraConnectConstants.DEFAULT_HTTP_PORT
        var server: HttpServer? = null
        for (attempt in 0 until 5) {
            try {
                server = HttpServer.create(InetSocketAddress(port), 0)
                boundPort = port
                break
            } catch (_: Exception) {
                port++
            }
        }

        if (server == null) {
            return
        }

        val pool = Executors.newFixedThreadPool(4) { runnable ->
            Thread(runnable, "AuraConnect-HTTP").apply { isDaemon = true }
        }
        executor = pool
        httpServer = server
        server.executor = pool

        // Pair endpoint: POST /api/pair
        server.createContext("/api/pair", HttpHandler { exchange ->
            try {
                if (rejectBrowserOrigin(exchange)) return@HttpHandler
                if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                    sendError(exchange, 405, "Method not allowed", "method_not_allowed")
                    return@HttpHandler
                }
                val peerKey = exchange.remoteAddress?.address?.hostAddress ?: "unknown"
                if (isPairRateLimited(peerKey)) {
                    sendError(exchange, 429, "Too many failed pairing attempts. Try again in a minute.", "rate_limited")
                    return@HttpHandler
                }

                val body = readBoundedBody(exchange, maxBytes = 8 * 1024)
                val req = json.decodeFromString(AuraPairRequest.serializer(), body)
                val expectedBytes = _pairingPin.value.toByteArray(StandardCharsets.UTF_8)
                val providedBytes = req.pin.trim().toByteArray(StandardCharsets.UTF_8)
                if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
                    recordPairFailure(peerKey)
                    sendError(exchange, 403, "Invalid pairing PIN", "invalid_pin")
                    return@HttpHandler
                }

                pairAttempts.remove(peerKey)
                val token = generateToken()
                issuedTokens.add(token)
                _pairedCount.value = issuedTokens.size

                val resp = AuraPairResponse(
                    token = token,
                    device = currentDeviceInfo(exchange),
                )
                sendJson(exchange, 200, json.encodeToString(AuraPairResponse.serializer(), resp))
            } catch (e: Exception) {
                sendError(exchange, 400, e.message ?: "Invalid pairing request", "bad_request")
            } finally {
                runCatching { exchange.close() }
            }
        })

        // Status endpoint: GET /api/status
        server.createContext("/api/status", HttpHandler { exchange ->
            try {
                if (rejectBrowserOrigin(exchange)) return@HttpHandler
                if (!exchange.requestMethod.equals("GET", ignoreCase = true)) {
                    sendError(exchange, 405, "Method not allowed", "method_not_allowed")
                    return@HttpHandler
                }
                if (!isAuthorized(exchange)) {
                    sendError(exchange, 401, "Pairing required", "pairing_required")
                    return@HttpHandler
                }

                val status = AuraStatusResponse(
                    device = currentDeviceInfo(exchange),
                    playback = getState(),
                )
                sendJson(exchange, 200, json.encodeToString(AuraStatusResponse.serializer(), status))
            } catch (e: Exception) {
                sendError(exchange, 500, e.message ?: "Failed to read status", "internal_error")
            } finally {
                runCatching { exchange.close() }
            }
        })

        // Command endpoint: POST /api/command
        server.createContext("/api/command", HttpHandler { exchange ->
            try {
                if (rejectBrowserOrigin(exchange)) return@HttpHandler
                if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                    sendError(exchange, 405, "Method not allowed", "method_not_allowed")
                    return@HttpHandler
                }
                if (!isAuthorized(exchange)) {
                    sendError(exchange, 401, "Pairing required", "pairing_required")
                    return@HttpHandler
                }

                val body = readBoundedBody(exchange, maxBytes = 256 * 1024)
                val cmd = json.decodeFromString(AuraCommandRequest.serializer(), body)

                when (cmd.action) {
                    "PLAY" -> onPlay()
                    "PAUSE" -> onPause()
                    "PLAY_PAUSE" -> onTogglePlayPause()
                    "NEXT" -> onNext()
                    "PREVIOUS" -> onPrevious()
                    "SEEK" -> cmd.positionMs?.let { onSeek(it) }
                    "SET_VOLUME" -> cmd.volume?.let { onSetVolume(it) }
                    "TOGGLE_MUTE" -> onToggleMute()
                    "PLAY_INDEX" -> cmd.index?.let { onPlayIndex(it) }
                    "REMOVE_INDEX" -> cmd.index?.let { onRemoveIndex(it) }
                    "MOVE_INDEX" -> cmd.fromIndex?.let { from -> cmd.toIndex?.let { to -> onMoveIndex(from, to) } }
                    "ADD_TRACK" -> cmd.track?.let { onAddTrack(it.toSong(), cmd.playNext == true) }
                    "CLEAR_QUEUE" -> onClearQueue()
                    "TOGGLE_SHUFFLE" -> onToggleShuffle()
                    "TOGGLE_REPEAT" -> onToggleRepeat()
                    "SET_EQ_PRESET" -> cmd.eqPreset?.let { onSetEqPreset(it) }
                    "TRANSFER_SESSION" -> cmd.session?.let { onTransferSession(it) }
                }

                val status = AuraStatusResponse(
                    device = currentDeviceInfo(exchange),
                    playback = getState(),
                )
                sendJson(exchange, 200, json.encodeToString(AuraStatusResponse.serializer(), status))
            } catch (e: Exception) {
                sendError(exchange, 400, e.message ?: "Invalid command", "bad_request")
            } finally {
                runCatching { exchange.close() }
            }
        })

        server.start()

        // 2. Start UDP Discovery Broadcaster & Listener
        startUdpDiscovery()
    }

    private fun currentDeviceInfo(exchange: HttpExchange) = AuraConnectDevice(
        id = deviceId,
        name = deviceName,
        deviceType = AuraDeviceType.DESKTOP,
        host = exchange.localAddress.address.hostAddress ?: "127.0.0.1",
        port = boundPort,
        lastSeenMs = System.currentTimeMillis(),
    )

    private fun rejectBrowserOrigin(exchange: HttpExchange): Boolean {
        val origin = exchange.requestHeaders.getFirst("Origin")
        if (!origin.isNullOrBlank() || exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
            sendError(exchange, 403, "Cross-origin browser access disabled", "forbidden_origin")
            return true
        }
        return false
    }

    private fun isAuthorized(exchange: HttpExchange): Boolean {
        val header = exchange.requestHeaders.getFirst("Authorization")?.trim() ?: return false
        if (!header.startsWith("Bearer ", ignoreCase = true)) return false
        val provided = header.substring(7).trim()
        if (provided.isEmpty()) return false
        val providedBytes = provided.toByteArray(StandardCharsets.UTF_8)
        for (expected in issuedTokens) {
            if (MessageDigest.isEqual(expected.toByteArray(StandardCharsets.UTF_8), providedBytes)) {
                return true
            }
        }
        return false
    }

    private fun isPairRateLimited(peerKey: String): Boolean {
        val now = System.currentTimeMillis()
        val window = pairAttempts[peerKey] ?: return false
        if (now - window.startedMs > 60_000L) {
            pairAttempts.remove(peerKey)
            return false
        }
        return window.failures >= 5
    }

    private fun recordPairFailure(peerKey: String) {
        val now = System.currentTimeMillis()
        pairAttempts.compute(peerKey) { _, prev ->
            if (prev == null || now - prev.startedMs > 60_000L) {
                PairAttemptWindow(now, 1)
            } else {
                prev.copy(failures = prev.failures + 1)
            }
        }
    }

    private fun readBoundedBody(exchange: HttpExchange, maxBytes: Int): String {
        val bytes = exchange.requestBody.use { stream ->
            val buf = ByteArray(maxBytes + 1)
            var total = 0
            while (total < buf.size) {
                val read = stream.read(buf, total, buf.size - total)
                if (read <= 0) break
                total += read
            }
            if (total > maxBytes) {
                throw IllegalArgumentException("Request body too large")
            }
            buf.copyOf(total)
        }
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun sendError(exchange: HttpExchange, statusCode: Int, message: String, code: String) {
        val body = json.encodeToString(AuraErrorResponse.serializer(), AuraErrorResponse(error = message, code = code))
        sendJson(exchange, statusCode, body)
    }

    private fun sendJson(exchange: HttpExchange, statusCode: Int, jsonBody: String) {
        val responseBytes = jsonBody.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(statusCode, responseBytes.size.toLong())
        exchange.responseBody.use { os: OutputStream ->
            os.write(responseBytes)
        }
    }

    private fun startUdpDiscovery() {
        val socket = try {
            DatagramSocket(AuraConnectConstants.DISCOVERY_UDP_PORT).apply {
                broadcast = true
            }
        } catch (_: Exception) {
            // Port might be in use or restricted; fallback to ephemeral port for outgoing beacons
            try {
                DatagramSocket().apply { broadcast = true }
            } catch (_: Exception) {
                null
            }
        } ?: return

        udpSocket = socket

        // Beacon payload format:
        // AURA_CONNECT_BEACON|<id>|<name>|<deviceType>|<port>
        val beaconString = "${AuraConnectConstants.BEACON_MAGIC}|$deviceId|$deviceName|DESKTOP|$boundPort"
        val beaconBytes = beaconString.toByteArray(StandardCharsets.UTF_8)

        // Periodic broadcast to local subnet
        broadcastJob = scope.launch {
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            while (isActive) {
                try {
                    val packet = DatagramPacket(beaconBytes, beaconBytes.size, broadcastAddr, AuraConnectConstants.DISCOVERY_UDP_PORT)
                    socket.send(packet)
                } catch (_: Exception) {}
                delay(3500)
            }
        }

        // Listener for direct discover queries from phones
        listenJob = scope.launch {
            val buf = ByteArray(1024)
            while (isActive) {
                try {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                    if (msg.startsWith(AuraConnectConstants.DISCOVERY_MAGIC)) {
                        // Reply unicast directly to the inquiring client
                        val replyPacket = DatagramPacket(beaconBytes, beaconBytes.size, packet.address, packet.port)
                        socket.send(replyPacket)
                    }
                } catch (_: Exception) {
                    if (!isActive) break
                }
            }
        }
    }

    /**
     * Gracefully stops the HTTP server, worker pool, and UDP background tasks.
     */
    fun stop() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        broadcastJob = null
        listenJob = null
        scope.cancel()
        runCatching { udpSocket?.close() }
        udpSocket = null
        runCatching { httpServer?.stop(0) }
        httpServer = null
        runCatching { executor?.shutdownNow() }
        executor = null
    }
}
