package com.example.ukbocw.retrofit

import androidx.lifecycle.LiveData
import com.example.ukbocw.data.SurveyResponse
import com.example.ukbocw.data.users
import com.example.ukbocw.utils.ApiResponse
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface ApiService {
    //    @POST("${ENDPOINT}api/v1/auth")
//    suspend fun authentication(
//        @Body obj: JsonObject,
//        @HeaderMap headers: HashMap<String, String>
//    ): Response<users>
    @POST("${ENDPOINT}api/v1/auth")
    fun authentication(
        @Body obj: JsonObject,
        @HeaderMap headers: HashMap<String, String>
    ): LiveData<ApiResponse<users>>

    @POST("${ENDPOINT}api/v1/survey")
    fun survey(
        @Body obj: JsonObject,
        @HeaderMap headers: HashMap<String, String>
    ): LiveData<ApiResponse<SurveyResponse>>

    companion object {
        const val ENDPOINT = "https://animeshsrivastava.tech/ukbocwwb/"
    }
}