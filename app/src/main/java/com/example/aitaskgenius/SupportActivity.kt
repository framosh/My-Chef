package com.example.aitaskgenius

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aitaskgenius.data.model.Recipe
import com.example.aitaskgenius.data.repository.RecipeRepository
import com.example.aitaskgenius.databinding.ActivitySupportBinding
import com.example.aitaskgenius.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class SupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupportBinding
    private val recipeRepository = RecipeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSyncAll.setOnClickListener {
            syncAllLocalRecipesToRemote()
        }
    }

    private fun syncAllLocalRecipesToRemote() {
        lifecycleScope.launch {
            // 1. Obtener todas las recetas de la base de datos local (Room)
            val allRecipes = withContext(Dispatchers.IO) {
                recipeRepository.getAllRecipes()
            }

            if (allRecipes.isEmpty()) {
                Toast.makeText(this@SupportActivity, "No hay recetas locales para exportar", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 2. Preparar la interfaz visual (Barra de progreso y botones)
            binding.btnSyncAll.isEnabled = false
            binding.layoutProgress.visibility = View.VISIBLE
            binding.pbSync.max = allRecipes.size
            binding.pbSync.progress = 0

            var count = 0

            // 3. Procesar cada receta una por una (Secuencial)
            allRecipes.forEach { recipe ->
                count++

                // Actualizamos el texto de estado con el nombre de la receta actual
                binding.tvProgressStatus.text = "Sincronizando ($count de ${allRecipes.size}):\n${recipe.title}"

                // A. Convertir imagen a Base64 en hilo secundario
                val base64Image = withContext(Dispatchers.IO) {
                    getBase64FromUri(recipe.imageUrl)
                }
                recipe.imageEncoded = base64Image

                // B. Enviar al servidor y ESPERAR la respuesta real antes de continuar
                // Esto garantiza que no se amontonen las peticiones
                val isSuccess = withContext(Dispatchers.IO) {
                    try {
                        // .execute() hace que la corrutina espere el resultado del servidor
                        val response = ApiClient.instance.exportRecipe(recipe).execute()
                        response.isSuccessful
                    } catch (e: Exception) {
                        Log.e("SYNC_DEBUG", "Error al enviar '${recipe.title}': ${e.message}")
                        false
                    }
                }

                if (isSuccess) {
                    Log.d("SYNC_DEBUG", "Exportación exitosa: ${recipe.title}")
                }

                // C. Actualizar la barra de progreso visualmente
                binding.pbSync.progress = count

                // D. TIEMPO DE ESPERA (1.5 segundos)
                // Evita saturar el ancho de banda y da un respiro al servidor PHP
                delay(1500)
            }

            // 4. Finalización del proceso
            Toast.makeText(this@SupportActivity, "Sincronización masiva completada", Toast.LENGTH_LONG).show()
            binding.btnSyncAll.isEnabled = true
            binding.tvProgressStatus.text = "¡Proceso finalizado con éxito!\nSe enviaron $count recetas."
        }
    }

    private fun exportSingleRecipe(recipe: Recipe) {
        ApiClient.instance.exportRecipe(recipe).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("SYNC_DEBUG", "Enviada: ${recipe.title}")
                } else {
                    Log.e("SYNC_DEBUG", "Error servidor en ${recipe.title}: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("SYNC_DEBUG", "Error red en ${recipe.title}: ${t.message}")
            }
        })
    }

    private fun getBase64FromUri(uriString: String?): String? {
        if (uriString == null) return null
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }
}
