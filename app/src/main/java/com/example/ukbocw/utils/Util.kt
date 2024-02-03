package com.example.ukbocw.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import android.view.View
import com.google.gson.JsonObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.GZIPOutputStream


@SuppressLint("HardwareIds")
fun getAndroidDeviceId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

fun checkForValidPassword(userPassword: String): Boolean {
    val passwordRegex =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
    return userPassword.matches(passwordRegex.toRegex())
}

fun checkForValidEmail(userEmail: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
    return userEmail.matches(emailRegex.toRegex())
}

fun checkForValidaadharNumber(aadharNumber: String): Boolean {
    val aadharRegex = "^[2-9]{1}[0-9]{11}\$"
    return aadharNumber.matches(aadharRegex.toRegex())

}

fun checkForValidPanNumber(panNumber: String): Boolean {
    val panRegex = "^[A-Z]{5}[0-9]{4}[A-Z]{1}\$"
    return panNumber.matches(panRegex.toRegex())
}

fun checkForValidName(name: String): Boolean {
    val nameRegex = "^[A-Z]+(?:[\\s'-][A-Z]+)*$"
    return name.matches(nameRegex.toRegex())
}

fun checkForValidPinCode(name: String): Boolean {
    val nameRegex = "^[1-9][0-9]{5}$"
    return name.matches(nameRegex.toRegex())
}

fun checkForValidPhoneNumber(name: String): Boolean {
    val nameRegex = "^[6-9]\\d{9}$"
    return name.matches(nameRegex.toRegex())
}

fun generateRandomAlphaNumeric(length: Int): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..length)
        .map { chars.random() }
        .joinToString("")
}

fun jsonObjectToBase64(jsonObject: JsonObject): String {
    val jsonString = jsonObject.toString()

    val base64String = Base64.encodeToString(jsonString.toByteArray(), Base64.DEFAULT)

    return base64String
}

fun resizeBitmap(getBitmap: Bitmap, maxSize: Int): Bitmap {
    var width = getBitmap.width
    var height = getBitmap.height
    val x: Double
    if (width >= height && width > maxSize) {
        x = (width / height).toDouble()
        width = maxSize
        height = (maxSize / x).toInt()
    } else if (height >= width && height > maxSize) {
        x = (height / width).toDouble()
        height = maxSize
        width = (maxSize / x).toInt()
    }
    return Bitmap.createScaledBitmap(getBitmap, width, height, false)
}

fun View.setDebounceOnClickListener(onPerformClick: (View?) -> Unit) {
    val listener = DebounceClickListener {
        onPerformClick(it)
    }
    setOnClickListener(listener)
}


fun fileToBase64(filePath: Uri, context: Context): String {
    val file = context.contentResolver.openInputStream(filePath)
    val image = BitmapFactory.decodeStream(file)
    var resizedImage = Bitmap.createScaledBitmap(image, 600, 500, false)
    resizedImage = resizeBitmap(resizedImage, 500)
    val byteArray = ByteArrayOutputStream()
    resizedImage.compress(Bitmap.CompressFormat.PNG, 70, byteArray)
    var imageBytes = byteArray.toByteArray()
    return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
}

fun imageDeleteFromFile(uri: Uri): Boolean {
    val fdelete = File(uri.path)
    if (fdelete.exists()) {
        if (fdelete.delete()) {
            return true
        }
    }
    return false
}

fun compressJson(jsonString: String): String {
    try {
        val inputBytes = jsonString.toByteArray()

        val outputStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(outputStream)
        gzipOutputStream.write(inputBytes)
        gzipOutputStream.close()

        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    } catch (e: IOException) {
        e.printStackTrace()
        // Handle exception
        return ""
    }
}
