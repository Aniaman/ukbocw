package com.example.ukbocw

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ukbocw.utils.PreferenceHelper

class SplashScreen : AppCompatActivity() {
    private lateinit var sharePreference: PreferenceHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = getColor(R.color.white)
        sharePreference = PreferenceHelper(this)
        val userAccess = sharePreference.getDataFromPref("userAccessToken")

        if (userAccess!!.isEmpty()) {
            Handler().postDelayed({
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }, 3000)
        } else {
            Handler().postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }, 3000)
        }


    }
}