package com.example.aitaskgenius

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SignInActivity"

class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Inicializar Firebase Auth y Credential Manager
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)

        val btnGoogle = findViewById<Button>(R.id.google_sign_in_button)
        btnGoogle.setOnClickListener {
            launchGoogleSignIn()
        }
    }

    private fun launchGoogleSignIn() {
        // 1. Configurar la opción de Google ID

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false) // DEBE ESTAR EN FALSE
            .setAutoSelectEnabled(false)         // Cámbialo a FALSE para pruebas
            .build()

        /*val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false) // Cambiado a false para que siempre permita elegir cuenta
            .build()*/

        // 2. Crear la solicitud
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // 3. Ejecutar el flujo usando Corrutinas (Forma recomendada para CredentialManager)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@SignInActivity,
                    request = request
                )
                handleSignInResult(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Error de Credential Manager: ${e.message}")
                Toast.makeText(this@SignInActivity, "Error al obtener credenciales", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignInResult(credential: Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                firebaseAuthWithGoogle(idToken)
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar el token: ${e.message}")
            }
        } else {
            Log.w(TAG, "Tipo de credencial no reconocido")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                auth.signInWithCredential(firebaseCredential).await()
                Log.d(TAG, "Autenticación en Firebase exitosa")

                // Navegar a la MainActivity tras el éxito
                startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error en Firebase: ${e.message}")
                Toast.makeText(this@SignInActivity, "Error de autenticación en la nube", Toast.LENGTH_SHORT).show()
            }
        }
    }
}