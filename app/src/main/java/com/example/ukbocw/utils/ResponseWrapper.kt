package com.example.ukbocw.utils

data class ResponseWrapper<out T>(
    val status: Status,
    val data: T?,
    val code: Int?,
    val message: String?,
    val errorbody: String? = null
) {
    companion object {
        fun <T> loading(data: T?): ResponseWrapper<T> =
            ResponseWrapper(status = Status.LOADING, code = null, data = data, message = null)

        fun <T> success(data: T?): ResponseWrapper<T> =
            ResponseWrapper(status = Status.SUCCESS, code = null, data = data, message = null)

        fun <T> error(
            data: T?,
            code: Int?,
            message: String,
            errorbody: String? = null
        ): ResponseWrapper<T> =
            ResponseWrapper(
                status = Status.ERROR,
                data = data,
                code = code,
                message = message,
                errorbody = errorbody
            )

        fun <T> empty(): ResponseWrapper<T> {
            return ResponseWrapper(Status.EMPTY, null, null, null)
        }

    }
}

enum class Status {
    SUCCESS,
    ERROR,
    LOADING,
    EMPTY
}