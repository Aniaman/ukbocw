package com.example.ukbocw

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ukbocw.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = getColor(R.color.white)

        binding.tvSignIn.setOnClickListener {
            signinButtonClick()
        }

    }

    private fun signinButtonClick() {
//        if(checkForValidPassword(binding.etPassword.text.toString())) {
//            val i = Intent(
//                this@LoginActivity,
//                PersonalQuection::class.java
//            )
//            finish()
//            startActivity(i);
//        }else{
//            binding.passwordValidation.text =
//                "Password length must be 8 and at least 1 special symbol,1 lowercase and 1 uppercase"
//            binding.passwordValidation.visibility = View.VISIBLE
//        }

        val i = Intent(
            this@LoginActivity,
            PersonalQuection::class.java
        )
        startActivity(i)

    }
}