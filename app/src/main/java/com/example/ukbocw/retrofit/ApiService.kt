package com.example.ukbocw.retrofit

import com.example.ukbocw.data.users
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface ApiService {
    @POST("https://animeshsrivastava.tech/ukbocwwb/api/v1/auth")
    suspend fun authentication(
        @Body obj: JsonObject,
        @HeaderMap headers: HashMap<String, String>
    ): Response<users>

    companion object {
        const val ENDPOINT = "https://animeshsrivastava.tech/ukbocwwb/"
    }
}