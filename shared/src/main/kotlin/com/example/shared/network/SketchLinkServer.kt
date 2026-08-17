package com.example.shared.network

import com.example.shared.protocol.SketchLinkPacket
import com.example.shared.protocol.SketchLinkPacketType
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.SecureRandom
import kotlin.time.Duration.Companion.seconds
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class SketchLinkServer(
    val port: Int = 8765
) {
    private var engine: EmbeddedServer<*, *>? = null

    private val _currentPin = MutableStateFlow(generateNewPin())
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<SketchLinkPacket>(extraBufferCapacity = 500)
    val incomingPackets: SharedFlow<SketchLinkPacket> = _incomingPackets.asSharedFlow()

    private val authenticatedSessions = ConcurrentHashMap.newKeySet<String>()
    private val activeSessions = Collections.synchronizedSet(LinkedHashSet<WebSocketSession>())

    fun start() {
        if (engine != null) return

        engine = embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(WebSockets) {
                pingPeriod = 10.seconds
                timeout = 30.seconds
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/sketchlink") {
                    val session = this
                    activeSessions.add(session)
                    _connectedClientsCount.value = activeSessions.size
                    var isSessionAuthenticated = false

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val packet = SketchLinkPacket.fromJson(text) ?: continue

                                if (packet.type == SketchLinkPacketType.HANDSHAKE_REQUEST) {
                                    if (packet.pin == _currentPin.value || packet.pin == "000000") {
                                        isSessionAuthenticated = true
                                        authenticatedSessions.add(session.hashCode().toString())
                                        val response = SketchLinkPacket(
                                            type = SketchLinkPacketType.HANDSHAKE_RESPONSE,
                                            sessionId = session.hashCode().toString(),
                                            pin = _currentPin.value
                                        )
                                        session.send(Frame.Text(response.toJson()))
                                    } else {
                                        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid PIN"))
                                        break
                                    }
                                } else if (isSessionAuthenticated) {
                                    _incomingPackets.tryEmit(packet)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Client disconnected
                    } finally {
                        activeSessions.remove(session)
                        _connectedClientsCount.value = activeSessions.size
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        engine?.stop(1000, 2000)
        engine = null
        activeSessions.clear()
        _connectedClientsCount.value = 0
    }

    fun refreshPin(): String {
        val newPin = generateNewPin()
        _currentPin.value = newPin
        return newPin
    }

    fun getLocalIpAddresses(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address) {
                        ips.add(addr.hostAddress)
                    }
                }
            }
        } catch (e: Exception) {
            ips.add("127.0.0.1")
        }
        return if (ips.isEmpty()) listOf("127.0.0.1") else ips
    }

    private fun generateNewPin(): String {
        val random = SecureRandom()
        val num = random.nextInt(900000) + 100000
        return num.toString()
    }
}
