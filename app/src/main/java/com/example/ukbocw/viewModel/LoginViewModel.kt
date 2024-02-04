package com.example.ukbocw.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.ukbocw.data.SurveyResponse
import com.example.ukbocw.data.users
import com.example.ukbocw.repository.LoginRepository
import com.example.ukbocw.utils.ResponseWrapper
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading


    private val _surveyResponse = MutableLiveData<SurveyResponse>()
    val surveyResponse: LiveData<SurveyResponse> get() = _surveyResponse

    fun setIsLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun userAuthentication(email: String, password: String): LiveData<ResponseWrapper<users>> =
        loginRepository.authentication(userObject(email, password))

    fun survey(surveyData: JsonObject, token: String): LiveData<ResponseWrapper<SurveyResponse>> =
        loginRepository.survey(surveyData, token)


    private fun userObject(email: String, password: String): JsonObject {
        val obj = JsonObject()
        obj.addProperty("username", email)
        obj.addProperty("password", password)
        return obj
    }
}