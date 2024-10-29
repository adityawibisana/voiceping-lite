package aditya.wibisana.voiceping

import aditya.wibisana.voiceping.player.ConnectionState
import aditya.wibisana.voiceping.player.VPWebSocketClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URI
import javax.net.ssl.SSLSocketFactory

class SignInAPITest {
    private val api = APIService().api
    private lateinit var uuid: String
    private lateinit var serverSocketUrl: String
    private lateinit var client: WebSocketClient

    @Before
    fun setUp() {
        signIn() // Run signIn before each test
    }

    @Test
    fun signIn() {
        val username = "1@vplite.com"
        val password = "mysecretpassword"
        val result = api.signIn(username, password).execute()
        uuid = result.body()!!.uuid
        serverSocketUrl = result.body()!!.socketUrl
        assert(result.isSuccessful)
    }

    @Test
    fun createSocketClient(): Unit = runBlocking {
        val uri = URI(serverSocketUrl)
        val header = HashMap<String, String>()
        header["VoicePingToken"] = uuid
        header["DeviceId"] = "Android unit test - Aditya Wibisana"
        withTimeout(10_000) {
            val ws = VPWebSocketClient(uri, header, 10_000, 1)
            val scope = CoroutineScope(Job())
            scope.launch {
                ws.connectionState.collectLatest {
                    when (it) {
                        ConnectionState.CONNECTING -> { }
                        ConnectionState.CONNECTED -> {
                            println("Connected to ${uri.host}")
                            scope.cancel()
                        }
                        ConnectionState.CONNECTED_CLEAN -> { }
                        ConnectionState.DISCONNECTED -> { }
                    }
                }
            }.join()
        }
    }
}