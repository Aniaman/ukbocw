package com.example.ukbocw

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ukbocw.databinding.ActivityMainBinding
import com.example.ukbocw.utils.PreferenceHelper
import com.example.ukbocw.utils.setDebounceOnClickListener

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
        //val userEmail = intent.getStringExtra("userEmail")


        binding.button.setDebounceOnClickListener {
            val intent = Intent(this, PersonalQuection::class.java)
            startActivity(intent)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 123)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}