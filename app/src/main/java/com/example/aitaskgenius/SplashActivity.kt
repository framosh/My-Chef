package com.example.aitaskgenius

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // EXPLICACIÓN: Usamos un Handler para esperar 2500 milisegundos (2.5 segundos)
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = Firebase.auth.currentUser

            if (currentUser != null) {
                // Usuario ya logueado -> A la App
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // Usuario nuevo -> A identificarse
                startActivity(Intent(this, SignInActivity::class.java))
            }
            finish()
        }, 3000)
      /*  Handler(Looper.getMainLooper()).postDelayed({
//            val intent = Intent(this, MainActivity::class.java)
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish() // Cerramos la Splash para que el usuario no regrese a ella con el botón atrás
        }, 7000)*/
    }
}