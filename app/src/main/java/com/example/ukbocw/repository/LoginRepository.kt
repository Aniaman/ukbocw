package com.example.ukbocw.repository

import androidx.lifecycle.LiveData
import com.example.ukbocw.data.SurveyResponse
import com.example.ukbocw.data.users
import com.example.ukbocw.retrofit.ApiService
import com.example.ukbocw.utils.ApiResponse
import com.example.ukbocw.utils.AuthenticatorHeader
import com.example.ukbocw.utils.ContextProviders
import com.example.ukbocw.utils.NetworkOnlyBoundResource
import com.example.ukbocw.utils.ResponseWrapper
import com.google.gson.JsonObject
import javax.inject.Inject

class LoginRepository @Inject constructor(private val apiServices: ApiService) {
    private var coroutineContext: ContextProviders = ContextProviders()
    private val authenticatorHeader = AuthenticatorHeader()


    fun authentication(queryObject: JsonObject): LiveData<ResponseWrapper<users>> {
        return object : NetworkOnlyBoundResource<users, users>(coroutineContext) {
            override fun createCall(): LiveData<ApiResponse<users>> {
                return apiServices.authentication(queryObject, authenticatorHeader.getAuthHeader())
            }
        }.asLiveData()
    }

    fun survey(queryObject: JsonObject, token: String): LiveData<ResponseWrapper<SurveyResponse>> {
        return object : NetworkOnlyBoundResource<SurveyResponse, SurveyResponse>(coroutineContext) {
            override fun createCall(): LiveData<ApiResponse<SurveyResponse>> {
                return apiServices.survey(queryObject, authenticatorHeader.getSurveyHeader(token))
            }
        }.asLiveData()
    }
}