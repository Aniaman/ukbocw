package com.example.ukbocw.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SurveyResponse(
    val data: Survey,
    val status: String,
    val status_code: Int
) : Parcelable