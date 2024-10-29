package aditya.wibisana.vplite

import okhttp3.OkHttpClient
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import voiceping.API

class SignInAPITest {
    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://voiceoverping.net/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val api: API = retrofit.create(API::class.java)

    @Test
    fun signIn() {
        val username = "1@vplite.com"
        val password = "mysecretpassword"
        val result = api.signIn(username, password).execute()
        assert(result.isSuccessful)
    }
}