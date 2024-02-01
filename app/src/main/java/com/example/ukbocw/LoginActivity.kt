package com.example.ukbocw

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ukbocw.databinding.ActivityLoginBinding
import com.example.ukbocw.utils.PreferenceHelper
import com.example.ukbocw.utils.checkForValidPassword
import com.example.ukbocw.viewModel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    private val viewModel by viewModels<LoginViewModel>()
    private lateinit var sharePreference: PreferenceHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = getColor(R.color.white)
        sharePreference = PreferenceHelper(this)
        binding.tvSignIn.setOnClickListener {
            signInButtonClick()
        }

    }

    private fun signInButtonClick() {
        if (checkForValidPassword(binding.etPassword.text.toString())) {
            viewModel.userAuthentication(
                binding.etUsername.text.toString(),
                binding.etPassword.text.toString()
            )
            viewModel.userData.observe(this, { user ->
                var userEmail = user.user.user_email
                sharePreference.setDataInPref(
                    userEmail,
                    user.access_token
                )
                val intent = Intent(
                    this@LoginActivity,
                    MainActivity::class.java
                ).putExtra("userEmail", userEmail)
                startActivity(intent)
                finish()
            }
            )
        } else {
            binding.passwordValidation.text =
                "Password length must be 8 and at least 1 special symbol,1 lowercase and 1 uppercase"
            binding.passwordValidation.visibility = View.VISIBLE
        }
    }
}