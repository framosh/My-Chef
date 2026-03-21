package com.example.aitaskgenius.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLEncoder

object ImageManager {

    private const val TAG = "ImageManager"

    fun getBase64FromUri(context: Context, uriString: String?): String? {
        if (uriString == null) return null
        return try {
            val uri = uriString.toUri()
            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return null

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir a Base64: ${e.message}", e)
            null
        }
    }

    /**
     * Descarga una imagen generada por IA siguiendo la documentación oficial de Pollinations.
     */
    suspend fun downloadAiImage(context: Context, query: String): File? = withContext(Dispatchers.IO) {
        // 1. Limpieza del prompt (Normalización para evitar caracteres que rompan la URL)
        val clean = java.text.Normalizer.normalize(query.lowercase(), java.text.Normalizer.Form.NFD)
            .replace("[\\u0300-\\u036f]".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), "")
            .trim()
        
        // 2. Codificación: La nueva API prefiere %20 en lugar de +
        val encoded = URLEncoder.encode("$clean professional culinary photography", "UTF-8").replace("+", "%20")
        
        var attempt = 0
        val maxAttempts = 3
        
        while (attempt < maxAttempts) {
            attempt++
            try {
                val seed = (0..999999).random()
                // URL SEGÚN DOCUMENTACIÓN ACTUAL: image.pollinations.ai/prompt/
                val imageUrl = "https://image.pollinations.ai/prompt/$encoded?width=800&height=600&seed=$seed&model=turbo&nologo=true"

                Log.d(TAG, "Intento $attempt - Solicitando: $imageUrl")
                
                val connection = URL(imageUrl).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                // User-Agent completo para simular navegador real
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connect()

                if (connection.responseCode == 200) {
                    val contentType = connection.contentType
                    if (contentType != null && !contentType.startsWith("image/")) {
                        Log.e(TAG, "El servidor no devolvió una imagen (Tipo: $contentType)")
                        throw Exception("No es una imagen")
                    }

                    val bytes = connection.inputStream.use { it.readBytes() }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    
                    if (bitmap != null) {
                        val file = File(context.cacheDir, "ai_gen_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(file).use { out -> 
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) 
                        }
                        Log.d(TAG, "¡Imagen recibida y guardada!")
                        return@withContext file
                    }
                } else {
                    Log.w(TAG, "Error del servidor: ${connection.responseCode}. Reintentando...")
                    delay(2000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en intento $attempt: ${e.message}")
                delay(1000)
            }
        }
        return@withContext null
    }
}
