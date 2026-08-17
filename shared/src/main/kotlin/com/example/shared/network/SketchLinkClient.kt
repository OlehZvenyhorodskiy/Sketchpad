package com.example.shared.network

import com.example.shared.protocol.SketchLinkPacket
import com.example.shared.protocol.SketchLinkPacketType
import com.example.shared.protocol.SketchLinkStrokeEvent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

enum class SketchLinkClientState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STREAMING,
    RECONNECTING
}

class SketchLinkClient(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 10_000
        }
    }

    private var activeSession: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var senderJob: Job? = null

    private val _state = MutableStateFlow(SketchLinkClientState.DISCONNECTED)
    val state: StateFlow<SketchLinkClientState> = _state.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val outgoingQueue = ConcurrentLinkedQueue<SketchLinkPacket>()
    private val offlineBuffer = ConcurrentLinkedQueue<SketchLinkPacket>()

    private var currentHost = "127.0.0.1"
    private var currentPort = 8765
    private var currentPin = ""
    private var isIntentionalDisconnect = false

    fun connect(host: String, port: Int = 8765, pin: String) {
        currentHost = host
        currentPort = port
        currentPin = pin
        isIntentionalDisconnect = false

        connectionJob?.cancel()
        connectionJob = scope.launch {
            var attempt = 0
            while (!isIntentionalDisconnect && isActive) {
                try {
                    updateState(if (attempt == 0) SketchLinkClientState.CONNECTING else SketchLinkClientState.RECONNECTING)

                    client.webSocket(host = currentHost, port = currentPort, path = "/sketchlink") {
                        activeSession = this
                        attempt = 0

                        // Handshake
                        val handshake = SketchLinkPacket(
                            type = SketchLinkPacketType.HANDSHAKE_REQUEST,
                            pin = currentPin
                        )
                        send(Frame.Text(handshake.toJson()))

                        val response = incoming.receive()
                        if (response is Frame.Text) {
                            val packet = SketchLinkPacket.fromJson(response.readText())
                            if (packet?.type == SketchLinkPacketType.HANDSHAKE_RESPONSE) {
                                updateState(SketchLinkClientState.CONNECTED)

                                // Flush offline buffer
                                while (!offlineBuffer.isEmpty()) {
                                    val buffered = offlineBuffer.poll() ?: break
                                    send(Frame.Text(buffered.toJson()))
                                }

                                startSenderLoop()

                                // Listen incoming / keepalive
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val inPacket = SketchLinkPacket.fromJson(frame.readText())
                                        if (inPacket?.type == SketchLinkPacketType.PONG) {
                                            _latencyMs.value = System.currentTimeMillis() - inPacket.timestamp
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    activeSession = null
                    senderJob?.cancel()
                    if (!isIntentionalDisconnect) {
                        attempt++
                        val backoffDelay = (attempt * 1000L).coerceAtMost(10_000L)
                        updateState(SketchLinkClientState.RECONNECTING)
                        delay(backoffDelay)
                    }
                }
            }
            updateState(SketchLinkClientState.DISCONNECTED)
        }
    }

    private fun updateState(newState: SketchLinkClientState) {
        _state.value = newState
        _isConnected.value = (newState == SketchLinkClientState.CONNECTED || newState == SketchLinkClientState.STREAMING)
    }

    private fun startSenderLoop() {
        senderJob?.cancel()
        senderJob = scope.launch {
            while (isActive && activeSession != null) {
                val packet = outgoingQueue.poll()
                if (packet != null) {
                    try {
                        activeSession?.send(Frame.Text(packet.toJson()))
                    } catch (e: Exception) {
                        // Store to offline buffer (max 30 seconds of strokes ~ 3600 packets)
                        if (offlineBuffer.size < 4000) {
                            offlineBuffer.add(packet)
                        }
                        break
                    }
                } else {
                    delay(8) // ~120Hz polling
                }
            }
        }
    }

    fun sendStrokeEvent(event: SketchLinkStrokeEvent, type: SketchLinkPacketType = SketchLinkPacketType.STROKE_MOVE) {
        val packet = SketchLinkPacket(
            type = type,
            strokeEvent = event,
            timestamp = System.currentTimeMillis()
        )

        if (_state.value == SketchLinkClientState.CONNECTED || _state.value == SketchLinkClientState.STREAMING) {
            updateState(SketchLinkClientState.STREAMING)
            outgoingQueue.add(packet)
        } else {
            // Buffer up to 4000 packets (~30s)
            if (offlineBuffer.size < 4000) {
                offlineBuffer.add(packet)
            }
        }
    }

    fun sendClear() {
        val packet = SketchLinkPacket(
            type = SketchLinkPacketType.CLEAR_CANVAS,
            timestamp = System.currentTimeMillis()
        )
        outgoingQueue.add(packet)
    }

    fun disconnect() {
        isIntentionalDisconnect = true
        connectionJob?.cancel()
        senderJob?.cancel()
        scope.launch {
            try {
                activeSession?.close()
            } catch (_: Exception) {}
            activeSession = null
            updateState(SketchLinkClientState.DISCONNECTED)
        }
    }
}
