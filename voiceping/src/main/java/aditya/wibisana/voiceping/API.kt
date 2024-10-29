package aditya.wibisana.voiceping

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

const val client_id = "2359media"
const val client_secret = "2359admin"
const val grant_type = "password"

interface API {
    @FormUrlEncoded
    @POST("v2/oauth/token")
    fun signIn(
        @Field("username") username: String,
        @Field("password") pwd: String,
        @Field("client_id") clientId: String = client_id,
        @Field("client_secret") clientSecret: String = client_secret,
        @Field("grant_type") grantType: String = grant_type
    ): Call<SignIn>
}

@Keep
data class SignIn (
    val username: String,
    val id: Int,
    @SerializedName("access_token") val accessToken: String,
    val uuid: String,
    @SerializedName("socket_url") val socketUrl: String,
    val email: String,
    val phone: String
)

