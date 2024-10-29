package aditya.wibisana.voiceping

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class APIService {
    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://voiceoverping.net/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: API = retrofit.create(API::class.java)
}