package aditya.wibisana.voiceping

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import javax.net.ssl.SSLSocketFactory

class SignInAPITest {
    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://voiceoverping.net/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val api: API = retrofit.create(API::class.java)
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
    fun createSocketClient() = runBlocking {
        val scope = CoroutineScope(Job())

        val uri = URI(serverSocketUrl)
        val header = HashMap<String, String>()
        header["VoicePingToken"] = uuid
        header["DeviceId"] = "Android / IOS id"

        client = object: WebSocketClient(uri, Draft_6455(), header, 0) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("onOpen. Uri:${this.uri.host}")
                scope.cancel("Finished. Connected")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("onClose. Code:$code reason:$reason remote:$remote")
            }

            override fun onMessage(message: String?) {
                println("onMessage")
            }

            override fun onError(ex: Exception?) {
                println("onError")
            }

        }
        client.setSocketFactory(SSLSocketFactory.getDefault())
        client.connect()
        println("Connect executed")
        scope.launch {
            delay(10_000)
            throw(Error("Connection takes too long!"))
        }.join()
    }

    @After
    fun tearDown() {
        client.close() // Close the WebSocket connection after each test
    }
}