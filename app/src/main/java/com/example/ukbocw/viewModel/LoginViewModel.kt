package com.example.ukbocw.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ukbocw.data.SurveyResponse
import com.example.ukbocw.data.users
import com.example.ukbocw.repository.LoginRepository
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    application: Application
) :
    AndroidViewModel(application) {

    private val _userData = MutableLiveData<users>()
    val userData: LiveData<users> get() = _userData
    private val _surveyResponse = MutableLiveData<SurveyResponse>()
    val surveyResponse: LiveData<SurveyResponse> get() = _surveyResponse


    fun userAuthentication(email: String, password: String) =
        viewModelScope.launch {
            loginRepository.authentication(userObject(email, password)).let { response ->
                if (response.isSuccessful) {
                    _userData.value = response.body()
                }
            }
        }

    fun survey(surveyData: JsonObject, token: String) = viewModelScope.launch {
        loginRepository.survey(surveyData, token).let { response ->
            if (response.isSuccessful) {
                _surveyResponse.value = response.body()
            }
        }
    }


    private fun userObject(email: String, password: String): JsonObject {
        val obj = JsonObject()
        obj.addProperty("username", email)
        obj.addProperty("password", password)
        return obj
    }
}