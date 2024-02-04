package com.example.ukbocw.retrofit

import com.example.ukbocw.data.SurveyResponse
import com.example.ukbocw.data.users
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface ApiService {
    @POST("${ENDPOINT}api/v1/auth")
    suspend fun authentication(
        @Body obj: JsonObject,
        @HeaderMap headers: HashMap<String, String>
    ): Response<users>

    @POST("${ENDPOINT}api/v1/survey")
    suspend fun survey(
        @Body obj: JsonObject,
        @HeaderMap headers: HashMap<String, String>
    ): Response<SurveyResponse>

    companion object {
        const val ENDPOINT = "https://animeshsrivastava.tech/ukbocwwb/"
    }
}