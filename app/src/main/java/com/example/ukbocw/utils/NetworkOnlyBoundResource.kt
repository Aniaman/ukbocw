package com.example.ukbocw.utils

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class NetworkOnlyBoundResource<ResultType, RequestType> @MainThread
internal constructor(private val contextProviders: ContextProviders) {

    private val result = MediatorLiveData<ResponseWrapper<RequestType>>()

    init {
        val apiResponse = createCall()
        result.addSource(apiResponse) { response ->
            result.removeSource(apiResponse)
            when (response) {
                is ApiSuccessResponse -> {
                    GlobalScope.launch(contextProviders.Main) {
                        result.value = ResponseWrapper.success(response.body)
                        asLiveData()
                    }
                }

                is ApiEmptyResponse -> {
                    GlobalScope.launch(contextProviders.Main) {
                        result.value = ResponseWrapper.success(null)
                        asLiveData()
                    }
                }

                is ApiErrorResponse -> {
                    result.value = ResponseWrapper.error(
                        null,
                        response.code,
                        response.errorMessage,
                        response.errorBody
                    )
                    onFetchFailed()
                }
            }
        }
    }

    protected fun onFetchFailed() {}

    fun asLiveData(): LiveData<ResponseWrapper<RequestType>> {
        return result
    }

    @MainThread
    protected abstract fun createCall(): LiveData<ApiResponse<RequestType>>
}
