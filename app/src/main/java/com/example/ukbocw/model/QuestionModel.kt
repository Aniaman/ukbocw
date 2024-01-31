package com.example.ukbocw.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuestionOptionType(
    val options: List<String>
) : Parcelable
