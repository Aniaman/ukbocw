package com.example.ukbocw

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ukbocw.databinding.ActivityMainBinding
import com.example.ukbocw.utils.PreferenceHelper

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharePreference: PreferenceHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = getColor(R.color.white)
        sharePreference = PreferenceHelper(this)
        val userEmail = intent.getStringExtra("userEmail")
        Log.d("accessToken", "${sharePreference.getDataFromPref(userEmail.toString())}")
    }
}