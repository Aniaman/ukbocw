package com.example.ukbocw.repository

import com.example.ukbocw.retrofit.ApiService
import com.example.ukbocw.utils.AuthenticatorHeader
import com.google.gson.JsonObject
import javax.inject.Inject

class LoginRepository @Inject constructor(private val apiServices: ApiService) {
    private val authenticatorHeader = AuthenticatorHeader()

    suspend fun authentication(userData: JsonObject) =
        apiServices.authentication(userData, authenticatorHeader.getAuthHeader())

    suspend fun survey(surveyData: JsonObject, token: String) =
        apiServices.survey(surveyData, authenticatorHeader.getSurveyHeader(token))
}