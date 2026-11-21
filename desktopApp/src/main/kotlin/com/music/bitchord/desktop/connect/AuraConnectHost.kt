package com.music.bitchord.desktop.connect

import com.music.bitchord.data.connect.AuraConnectConstants
import com.music.bitchord.data.connect.AuraConnectDevice
import com.music.bitchord.data.connect.AuraDeviceType
import com.music.bitchord.data.connect.AuraPlaybackState
import com.music.bitchord.data.connect.AuraStatusResponse
import com.music.bitchord.data.connect.AuraCommandRequest
import com.music.bitchord.data.connect.AuraTransferSession
import com.music.bitchord.data.connect.toConnectTrack
import com.music.bitchord.data.connect.toSong
import com.music.bitchord.data.model.Song
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Aura Connect Host daemon running inside AuraMusic Desktop.
 *
 * Exposes:
 * 1. UDP discovery responder and beacon broadcaster on port 8764.
 * 2. Embedded HTTP control server on port 8765 for real-time remote commands and state handoff.
 */
object AuraConnectHost {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var httpServer: HttpServer? = null
    private var udpSocket: DatagramSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + Job())
    private var broadcastJob: Job? = null
    private var listenJob: Job? = null

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
    var onAddTrack: (track: com.music.bitchord.data.model.Song, playNext: Boolean) -> Unit = { _, _ -> }
    var onClearQueue: () -> Unit = {}
    var onToggleShuffle: () -> Unit = {}
    var onToggleRepeat: () -> Unit = {}
    var onSetEqPreset: (String) -> Unit = {}
    var onTransferSession: (AuraTransferSession) -> Unit = {}

    /**
     * Starts the Aura Connect HTTP server and UDP discovery broadcaster.
     */
    fun start(customDeviceName: String? = null) {
        if (httpServer != null) return
        if (customDeviceName != null && customDeviceName.isNotBlank()) {
            deviceName = customDeviceName
        }

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

        httpServer = server
        server.executor = Executors.newFixedThreadPool(4)

        // Status endpoint: GET /api/status
        server.createContext("/api/status", HttpHandler { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@HttpHandler
            }

            val status = AuraStatusResponse(
                device = AuraConnectDevice(
                    id = deviceId,
                    name = deviceName,
                    deviceType = AuraDeviceType.DESKTOP,
                    host = exchange.localAddress.address.hostAddress ?: "127.0.0.1",
                    port = boundPort,
                    lastSeenMs = System.currentTimeMillis(),
                ),
                playback = getState(),
            )
            val responseBytes = json.encodeToString(AuraStatusResponse.serializer(), status)
                .toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, responseBytes.size.toLong())
            val os: OutputStream = exchange.responseBody
            os.write(responseBytes)
            os.close()
        })

        // Command endpoint: POST /api/command
        server.createContext("/api/command", HttpHandler { exchange ->
            handleCors(exchange)
            if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
                exchange.sendResponseHeaders(204, -1)
                return@HttpHandler
            }

            if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                exchange.sendResponseHeaders(405, -1)
                return@HttpHandler
            }

            try {
                val body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).readText()
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
                    device = AuraConnectDevice(
                        id = deviceId,
                        name = deviceName,
                        deviceType = AuraDeviceType.DESKTOP,
                        host = exchange.localAddress.address.hostAddress ?: "127.0.0.1",
                        port = boundPort,
                        lastSeenMs = System.currentTimeMillis(),
                    ),
                    playback = getState(),
                )
                val responseBytes = json.encodeToString(AuraStatusResponse.serializer(), status)
                    .toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
                exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                val os: OutputStream = exchange.responseBody
                os.write(responseBytes)
                os.close()
            } catch (e: Exception) {
                val err = """{"error":"${e.message ?: "Invalid command"}"}""".toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(400, err.size.toLong())
                exchange.responseBody.write(err)
                exchange.responseBody.close()
            }
        })

        server.start()

        // 2. Start UDP Discovery Broadcaster & Listener
        startUdpDiscovery()
    }

    private fun handleCors(exchange: HttpExchange) {
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type, Authorization")
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
     * Gracefully stops the HTTP server and UDP background tasks.
     */
    fun stop() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        broadcastJob = null
        listenJob = null
        runCatching { udpSocket?.close() }
        udpSocket = null
        runCatching { httpServer?.stop(0) }
        httpServer = null
    }
}
