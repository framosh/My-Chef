package com.example.aitaskgenius.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.aitaskgenius.network.ApiClient
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageManager {

    private const val TAG = "AI_DEBUG"

    /**
     * Convierte un archivo File a una cadena Base64.
     */
    fun fileToBase64(file: File): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // Comprimimos al 70% para no saturar el JSON
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir imagen a Base64: ${e.message}")
            null
        }
    }

    /**
     * Convierte una Uri de la galería a un archivo File físico en el caché.
     */
    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error en uriToFile: ${e.message}")
            null
        }
    }

    /**
     * Busca una imagen profesional a través del PROXY PHP (Seguro).
     */
    suspend fun downloadAiImage(context: Context, query: String): File? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Solicitando imagen al proxy para: $query")
            
            // Llamamos al nuevo endpoint proxy en tu servidor
            val response = ApiClient.instance.getUnsplashImageProxy(query).awaitResponse()

            if (response.isSuccessful) {
                val imageUrl = response.body()?.imageUrl
                if (!imageUrl.isNullOrEmpty()) {
                    Log.d(TAG, "URL obtenida del proxy: $imageUrl")
                    return@withContext downloadFromUrl(context, imageUrl)
                } else {
                    Log.e(TAG, "El proxy no devolvió ninguna URL")
                }
            } else {
                Log.e(TAG, "Error en el proxy de imágenes: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en el proceso de imagen: ${e.message}")
        }
        return@withContext null
    }

    private suspend fun downloadFromUrl(context: Context, imageUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                if (bitmap != null) {
                    val file = File(context.cacheDir, "recipe_img_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    return@withContext file
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al descargar imagen: ${e.message}")
        }
        return@withContext null
    }
}

/**
 * Modelo para la respuesta del Proxy de Unsplash
 * Usamos @SerializedName para mantener la compatibilidad con el JSON del servidor (image_url)
 * siguiendo las convenciones de nomenclatura de Kotlin (camelCase).
 */
data class UnsplashProxyResponse(
    @SerializedName("image_url") val imageUrl: String
)
