package aditya.wibisana.voiceping.player

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.enums.Opcode
import org.java_websocket.framing.CloseFrame
import org.java_websocket.framing.Framedata
import org.java_websocket.framing.FramedataImpl1
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class VPWebSocketClient(
    private val socketServerUri: URI,
    httpHeaders: MutableMap<String, String>?,
    connectTimeout: Int,
    private val socketId: Int,
    protocolDraft: Draft = Draft_6455()
) : WebSocketClient(socketServerUri, protocolDraft, httpHeaders, connectTimeout) {

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTING)
    val connectionState = _connectionState.asStateFlow()

    private val tag = "VPWebSocketClient"
    var lastWebSocketPing = 0L
        private set
    var lastConnectionClean = 0L
        private set
    var lastConnected = 0L
        private set
    var isSendingMessageAllowed = true // needed to prevent sending while socket is being closed, especially when socket is reopen-after-closed (unlikely)
        private set
    val creationTimeStamp = System.currentTimeMillis()

    init {
        @Suppress("DEPRECATION")
        socket = if (socketServerUri.toString().startsWith("wss")) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                // Create a trust manager that does not validate certificate chains
                @SuppressLint("CustomX509TrustManager")
                val trustAllCerts = arrayOf<TrustManager>(
                    @SuppressLint("CustomX509TrustManager")
                    object : X509TrustManager {
                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                            // Do nothing
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                            // Do nothing
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )

                val sslContext: SSLContext = try {
                    SSLContext.getInstance("SSL").apply {
                        init(null, trustAllCerts, SecureRandom())
                    }
                } catch (e: NoSuchAlgorithmException) {
                    throw RuntimeException(e)
                } catch (e: KeyManagementException) {
                    throw RuntimeException(e)
                }

                // Create an SSL socket factory with our all-trusting manager
                sslContext.socketFactory.createSocket()
            } else {
                SSLSocketFactory.getDefault().createSocket()
            }
        } else {
            val port = if (socketServerUri.port == -1) 80 else socketServerUri.port
            SocketFactory.getDefault().createSocket(socketServerUri.host, port)
        }

        try {
            log("connecting ${socketServerUri.host}")
            connectionLostTimeout = -1
            connect()
        } catch (e: Exception) {
            log("fails to connect to ${socketServerUri.host} ${e.message}")
        }
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        log("onOpen ${socketServerUri.host}")

        if (isSendingMessageAllowed) {
            _connectionState.value = ConnectionState.CONNECTED
            lastConnected = System.currentTimeMillis()
        } else { // in case for rare situation where connection is reopened, we just re-destroy it
            CoroutineScope(Job() + Dispatchers.IO).launch {
                destroy()
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        isSendingMessageAllowed = false
        log("onClose. Reason:$reason Remote:$remote url:$socketServerUri lastConnected:${((System.currentTimeMillis() - lastConnected)/1000).toInt()} lastClean:${((System.currentTimeMillis() - lastConnectionClean)/1000).toInt()} lastPing:${((System.currentTimeMillis() - lastWebSocketPing)/1000).toInt()}")
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // NOT triggered when turned off network manually from setting. So, this one, along with onClose is needed
    override fun onClosing(code: Int, reason: String?, remote: Boolean) {
        isSendingMessageAllowed = false

        log("onClosing. Reason:$reason Remote:$remote url:$socketServerUri lastConnected:${((System.currentTimeMillis() - lastConnected)/1000).toInt()} lastClean:${((System.currentTimeMillis() - lastConnectionClean)/1000).toInt()} lastPing:${((System.currentTimeMillis() - lastWebSocketPing)/1000).toInt()}")

        _connectionState.value = ConnectionState.DISCONNECTED
        super.onClosing(code, reason, remote)
    }

    override fun onError(ex: Exception?) {
        isSendingMessageAllowed = false
        log("onError ${socketServerUri.host} ex:${ex}")
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun onMessage(message: String?) { }

    override fun onMessage(bytes: ByteBuffer?) {
        super.onMessage(bytes) // if clean, do something here
        try {
//            val message = MessageHelper.getInstance().unpackMessage(bytes?.array()) ?: return
//
//            log("t:${message.messageType} S:${message.senderUserId} T:${message.receiveChannelId} Ct:${message.channelType}")
//            if (message.messageType == MessageType.connection_is_clean.type) {
//                log("connection test: success")
//                connectionState = ConnectionState.CONNECTED_CLEAN
//                lastConnectionClean = System.currentTimeMillis()
//            } else {
//                WebSocketClientManager.processMessage(socketId, message)
//            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onWebsocketPing(conn: WebSocket?, f: Framedata?) {
        super.onWebsocketPing(conn, f)
        lastWebSocketPing = System.currentTimeMillis()

        log("onWebsocketPing ${socketServerUri.host}")
        conn?.sendFrame(FramedataImpl1.get(Opcode.PONG))
    }

    override fun send(data: ByteArray?) {
        try {
            if (isSendingMessageAllowed) {
                super.send(data)
                log("Sent")
            }
        } catch (e: java.lang.Exception) {
            log("Failed sending: $e")
        }
    }

    override fun sendFrame(framedata: Framedata?) {
        try {
            if (isSendingMessageAllowed) {
                super.sendFrame(framedata)
            }
        } catch (e: java.lang.Exception) {
            log("Failed sending: $e")
        }
    }

    fun destroy() {
        isSendingMessageAllowed = false

        try {
            socket.shutdownInput()
        } catch (_: java.lang.Exception) { }

        try {
            socket.shutdownOutput()
        } catch (_: java.lang.Exception) { }

        try {
            close()
        } catch (e: java.lang.Exception) {
            try {
                closeConnection(CloseFrame.GOING_AWAY, "Client initiated-destroy")
                log("closed connection")
            } catch (e: java.lang.Exception) {
                log("close connection fails: $e")
            }
        }
    }

    private fun log(message: String) {
//        Log.v(tag, "socketId:$socketId $message")
    }
}

enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    CONNECTED_CLEAN, // happened after send 25, receive 26 from router. 1-1 might failed if not receiving this within some seconds.
    DISCONNECTED,
}