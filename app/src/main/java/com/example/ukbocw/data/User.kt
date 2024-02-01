package com.example.ukbocw.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    val _id: Id,
    val created_at: String,
    val id: String,
    val modified_at: String,
    val status: String,
    val user_email: String,
    val user_id: String,
    val user_name: String,
    val user_password: String,
    val user_phone: String,
    val user_profile_image: String,
    val user_role: String
) : Parcelable