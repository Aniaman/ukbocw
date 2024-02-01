package com.example.ukbocw.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class users(
    val access_token: String,
    val message: String,
    val status: String,
    val status_code: Int,
    val user: User
) : Parcelable