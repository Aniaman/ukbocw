package com.example.ukbocw

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ukbocw.databinding.ActivitySplashScreenBinding
import com.example.ukbocw.utils.DeviceRootState
import com.example.ukbocw.utils.PreferenceHelper
import com.example.ukbocw.utils.RootCheck
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashScreen : AppCompatActivity() {
    private lateinit var sharePreference: PreferenceHelper
    private lateinit var binding: ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (val rootCheckResult = RootCheck.enquireIsRooted(this)) {
            is DeviceRootState.Rooted -> {
                startActivity(
                    Intent(this, RootDeviceCheck::class.java)
                )
                finish()
            }

            DeviceRootState.NotRooted -> {
                // no op
            }
        }

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = getColor(R.color.white)
        sharePreference = PreferenceHelper(this)



        binding.version.text =
            "App-version ${packageManager.getPackageInfo(packageName, 0).versionName}"
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