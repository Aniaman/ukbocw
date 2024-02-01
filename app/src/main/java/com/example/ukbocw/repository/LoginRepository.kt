package com.example.ukbocw.repository

import com.example.ukbocw.retrofit.ApiService
import com.example.ukbocw.utils.AuthenticatorHeader
import com.google.gson.JsonObject
import javax.inject.Inject

class LoginRepository @Inject constructor(private val apiServices: ApiService) {
    private val authenticatorHeader = AuthenticatorHeader()

    suspend fun authentication(userData: JsonObject) =
        apiServices.authentication(userData, authenticatorHeader.getMainCustomerHeader())
}