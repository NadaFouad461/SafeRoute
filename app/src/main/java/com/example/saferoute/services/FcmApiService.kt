package com.example.saferoute.services


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST


data class FcmPayload(val message: FcmMessage)
data class FcmMessage(
    val token: String,
    val notification: FcmNotification,
    val data: Map<String, String>
)

data class FcmNotification(val title: String, val body: String)

interface FcmApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/projects/myproject-76176/messages:send")
    fun sendNotification(
        @Header("Authorization") barierToken: String,
        @Body payload: FcmPayload
    ): Call<Void>
}