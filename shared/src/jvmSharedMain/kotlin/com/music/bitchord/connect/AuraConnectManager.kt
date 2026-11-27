package com.music.bitchord.connect

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
import com.music.bitchord.data.connect.toConnectTrack
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Aura Connect Manager: handles zero-configuration local network discovery (UDP)
 * and authenticated remote control / playback handoff (HTTP / JSON).
 */
object AuraConnectManager {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val clientId: String = "mobile_" + UUID.randomUUID().toString().take(8)
    private val pairedTokens = ConcurrentHashMap<String, String>()

    private val _discoveredDevices = MutableStateFlow<List<AuraConnectDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<AuraConnectDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<AuraConnectDevice?>(null)
    val connectedDevice: StateFlow<AuraConnectDevice?> = _connectedDevice.asStateFlow()

    private val _remotePlaybackState = MutableStateFlow<AuraPlaybackState?>(null)
    val remotePlaybackState: StateFlow<AuraPlaybackState?> = _remotePlaybackState.asStateFlow()

    private val _pairingRequiredForDevice = MutableStateFlow<AuraConnectDevice?>(null)
    val pairingRequiredForDevice: StateFlow<AuraConnectDevice?> = _pairingRequiredForDevice.asStateFlow()

    private val _pairingError = MutableStateFlow<String?>(null)
    val pairingError: StateFlow<String?> = _pairingError.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private var discoveryJob: Job? = null
    private var syncJob: Job? = null
    private var udpSocket: DatagramSocket? = null

    /**
     * Starts background scanning for Aura Connect devices on the local Wi-Fi.
     */
    fun startDiscovery() {
        if (discoveryJob != null) return

        discoveryJob = scope.launch {
            val socket = try {
                DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 3000
                }
            } catch (_: Exception) {
                null
            } ?: return@launch
            udpSocket = socket

            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val discoveryBytes = AuraConnectConstants.DISCOVERY_MAGIC.toByteArray(StandardCharsets.UTF_8)
            val receiveBuf = ByteArray(2048)

            launch {
                // Periodic probe broadcast every 4 seconds
                while (isActive) {
                    try {
                        val packet = DatagramPacket(discoveryBytes, discoveryBytes.size, broadcastAddr, AuraConnectConstants.DISCOVERY_UDP_PORT)
                        socket.send(packet)
                    } catch (_: Exception) {}
                    delay(4000)
                }
            }

            // Listener loop
            while (isActive) {
                try {
                    val packet = DatagramPacket(receiveBuf, receiveBuf.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                    if (msg.startsWith(AuraConnectConstants.BEACON_MAGIC)) {
                        parseBeacon(msg, packet.address)
                    }
                } catch (_: Exception) {
                    // Timeout or network drop; prune expired devices
                    pruneExpiredDevices()
                }
            }
        }
    }

    private fun parseBeacon(message: String, senderAddr: InetAddress) {
        if (!senderAddr.isSiteLocalAddress && !senderAddr.isLoopbackAddress && !senderAddr.isLinkLocalAddress) {
            return
        }
        val hostAddress = senderAddr.hostAddress ?: return
        // Format: AURA_CONNECT_BEACON|<id>|<name>|<deviceType>|<port>
        val parts = message.split("|")
        if (parts.size < 5) return

        val id = parts[1].trim()
        val name = parts[2].trim().take(64)
        val typeStr = parts[3].trim()
        val port = parts[4].trim().toIntOrNull() ?: return
        if (id.isBlank() || name.isBlank() || port !in 1..65535) return

        // Pin active session endpoint so spoofed beacons cannot hijack an active connection mid-flight.
        val active = _connectedDevice.value
        if (active != null && active.id == id && (active.host != hostAddress || active.port != port)) {
            return
        }

        val deviceType = runCatching { AuraDeviceType.valueOf(typeStr) }.getOrDefault(AuraDeviceType.DESKTOP)
        val now = System.currentTimeMillis()
        val device = AuraConnectDevice(
            id = id,
            name = name,
            deviceType = deviceType,
            host = hostAddress,
            port = port,
            lastSeenMs = now,
        )

        val currentList = _discoveredDevices.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == id || (it.host == hostAddress && it.port == port) }
        if (existingIndex >= 0) {
            currentList[existingIndex] = device
        } else {
            currentList.add(device)
        }
        _discoveredDevices.value = currentList
    }

    private fun pruneExpiredDevices() {
        val now = System.currentTimeMillis()
        val threshold = 14000L
        val active = _discoveredDevices.value.filter { now - it.lastSeenMs < threshold }
        if (active.size != _discoveredDevices.value.size) {
            _discoveredDevices.value = active
            val currentConnected = _connectedDevice.value
            if (currentConnected != null && active.none { it.id == currentConnected.id }) {
                disconnect()
            }
        }
    }

    fun dismissPairingPrompt() {
        _pairingRequiredForDevice.value = null
        _pairingError.value = null
    }

    /**
     * Pairs with a Desktop host using its displayed 6-digit PIN and establishes connection.
     */
    suspend fun pairWithPin(device: AuraConnectDevice, pin: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanPin = pin.trim()
        if (cleanPin.length != 6 || !cleanPin.all { it.isDigit() }) {
            val msg = "Enter the 6-digit PIN shown on ${device.name}."
            _pairingError.value = msg
            return@withContext Result.failure(IllegalArgumentException(msg))
        }
        var conn: HttpURLConnection? = null
        try {
            val url = java.net.URI.create("http://${device.host}:${device.port}/api/pair").toURL()
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val req = AuraPairRequest(pin = cleanPin, clientId = clientId)
            val body = json.encodeToString(AuraPairRequest.serializer(), req)
            conn.outputStream.use { os: OutputStream ->
                os.write(body.toByteArray(StandardCharsets.UTF_8))
            }
            val code = conn.responseCode
            if (code == 200) {
                val respText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val pairResp = json.decodeFromString(AuraPairResponse.serializer(), respText)
                pairedTokens[device.id] = pairResp.token
                _pairingError.value = null
                _pairingRequiredForDevice.value = null
                _connectionError.value = null
                connect(device)
                Result.success(Unit)
            } else {
                val errBody = runCatching { conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } }.getOrNull()
                val errMsg = errBody?.let {
                    runCatching { json.decodeFromString(AuraErrorResponse.serializer(), it).error }.getOrNull()
                } ?: "Pairing failed (HTTP $code)"
                _pairingError.value = errMsg
                Result.failure(IllegalStateException(errMsg))
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unable to reach ${device.name}"
            _pairingError.value = errMsg
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Connects to a remote Aura device for playback control and monitoring.
     */
    fun connect(device: AuraConnectDevice) {
        if (pairedTokens[device.id].isNullOrBlank()) {
            _pairingError.value = null
            _pairingRequiredForDevice.value = device
            return
        }
        _pairingRequiredForDevice.value = null
        _connectionError.value = null
        _connectedDevice.value = device
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive && _connectedDevice.value?.id == device.id) {
                fetchStatus(device)
                delay(1200)
            }
        }
    }

    /**
     * Disconnects from the current remote device.
     */
    fun disconnect() {
        syncJob?.cancel()
        syncJob = null
        _connectedDevice.value = null
        _remotePlaybackState.value = null
    }

    /**
     * Fetches current state from the remote device.
     */
    suspend fun fetchStatus(device: AuraConnectDevice): AuraPlaybackState? = withContext(Dispatchers.IO) {
        val token = pairedTokens[device.id] ?: return@withContext null
        var conn: HttpURLConnection? = null
        try {
            val url = java.net.URI.create("http://${device.host}:${device.port}/api/status").toURL()
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2000
                readTimeout = 2000
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
            }
            when (val code = conn.responseCode) {
                200 -> {
                    val body = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    val status = json.decodeFromString(AuraStatusResponse.serializer(), body)
                    _remotePlaybackState.value = status.playback
                    _connectionError.value = null
                    status.playback
                }
                401 -> {
                    pairedTokens.remove(device.id)
                    _connectedDevice.value = null
                    _remotePlaybackState.value = null
                    _pairingRequiredForDevice.value = device
                    _pairingError.value = "Session expired — enter PIN again."
                    null
                }
                else -> {
                    _connectionError.value = "Status error (HTTP $code)"
                    null
                }
            }
        } catch (e: Exception) {
            _connectionError.value = e.message ?: "Connection failed"
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Sends a remote command to the connected device.
     */
    fun sendCommand(
        action: String,
        positionMs: Long? = null,
        volume: Float? = null,
        index: Int? = null,
        fromIndex: Int? = null,
        toIndex: Int? = null,
        track: com.music.bitchord.data.connect.ConnectTrack? = null,
        playNext: Boolean? = null,
        repeatMode: String? = null,
        shuffleMode: String? = null,
        eqPreset: String? = null,
        session: AuraTransferSession? = null,
    ) {
        val device = _connectedDevice.value ?: return
        val token = pairedTokens[device.id] ?: run {
            _pairingRequiredForDevice.value = device
            return
        }
        scope.launch {
            var conn: HttpURLConnection? = null
            try {
                val url = java.net.URI.create("http://${device.host}:${device.port}/api/command").toURL()
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2500
                    readTimeout = 2500
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", "Bearer $token")
                }
                val cmd = AuraCommandRequest(
                    action = action,
                    positionMs = positionMs,
                    volume = volume,
                    index = index,
                    fromIndex = fromIndex,
                    toIndex = toIndex,
                    track = track,
                    playNext = playNext,
                    repeatMode = repeatMode,
                    shuffleMode = shuffleMode,
                    eqPreset = eqPreset,
                    session = session,
                )
                val body = json.encodeToString(AuraCommandRequest.serializer(), cmd)
                conn.outputStream.use { os: OutputStream ->
                    os.write(body.toByteArray(StandardCharsets.UTF_8))
                }

                when (val code = conn.responseCode) {
                    200 -> {
                        val resp = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                        val status = json.decodeFromString(AuraStatusResponse.serializer(), resp)
                        _remotePlaybackState.value = status.playback
                        _connectionError.value = null
                    }
                    401 -> {
                        pairedTokens.remove(device.id)
                        _connectedDevice.value = null
                        _remotePlaybackState.value = null
                        _pairingRequiredForDevice.value = device
                        _pairingError.value = "Session expired — enter PIN again."
                    }
                    else -> {
                        _connectionError.value = "Command rejected (HTTP $code)"
                    }
                }
            } catch (e: Exception) {
                _connectionError.value = e.message ?: "Command failed"
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        sendCommand("SEEK", positionMs = positionMs)
    }

    fun setVolume(volume: Float) {
        sendCommand("SET_VOLUME", volume = volume.coerceIn(0f, 1f))
    }

    fun toggleMute() {
        sendCommand("TOGGLE_MUTE")
    }

    fun playIndex(index: Int) {
        sendCommand("PLAY_INDEX", index = index)
    }

    fun removeIndex(index: Int) {
        sendCommand("REMOVE_INDEX", index = index)
    }

    fun moveIndex(from: Int, to: Int) {
        sendCommand("MOVE_INDEX", fromIndex = from, toIndex = to)
    }

    fun addTrack(song: Song, playNext: Boolean = false) {
        sendCommand("ADD_TRACK", track = song.toConnectTrack(), playNext = playNext)
    }

    fun clearQueue() {
        sendCommand("CLEAR_QUEUE")
    }

    fun toggleShuffle() {
        sendCommand("TOGGLE_SHUFFLE")
    }

    fun toggleRepeat() {
        sendCommand("TOGGLE_REPEAT")
    }

    fun setEqPreset(preset: String) {
        sendCommand("SET_EQ_PRESET", eqPreset = preset)
    }

    /**
     * Seamlessly hands off active playback session from this device to a remote device.
     */
    fun transferPlaybackToDevice(
        device: AuraConnectDevice,
        currentTrack: Song?,
        positionMs: Long,
        queue: List<Song>,
        queueIndex: Int,
        isPlaying: Boolean = true,
    ) {
        connect(device)
        val session = AuraTransferSession(
            track = currentTrack?.toConnectTrack(),
            positionMs = positionMs,
            queue = queue.map { it.toConnectTrack() },
            queueIndex = queueIndex.coerceIn(0, queue.size.coerceAtLeast(1) - 1),
            isPlaying = isPlaying,
        )
        sendCommand("TRANSFER_SESSION", session = session)
    }

    /**
     * Stops discovery and releases network resources.
     */
    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        syncJob?.cancel()
        syncJob = null
        runCatching { udpSocket?.close() }
        udpSocket = null
        _discoveredDevices.value = emptyList()
        disconnect()
    }
}
