package com.example.aitaskgenius.util

import android.util.Log
import com.example.aitaskgenius.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class AiManager {

    /**
     * Ahora la generación se delega al servidor PHP.
     */
    suspend fun generateRecipe(dishName: String): AiRecipeResponse? = withContext(Dispatchers.IO) {
        try {
            Log.d("AiManager", "Solicitando generación de receta a proxy PHP para: $dishName")
            
            val response = ApiClient.instance.generateRecipe(dishName).awaitResponse()

            if (response.isSuccessful) {
                Log.d("AiManager", "Receta generada exitosamente desde el servidor")
                return@withContext response.body()
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AiManager", "Error en el servidor proxy (${response.code()}): $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("AiManager", "Error conectando con el proxy de IA: ${e.message}", e)
            null
        }
    }
}

data class AiRecipeResponse(
    val ingredients: List<AiIngredient>, 
    val steps: List<String>, 
    val category: String, 
    val servings: Int,
    val image_search_term: String? // NUEVO: Término optimizado para Unsplash
)

data class AiIngredient(
    val name: String, 
    val quantity: Double, 
    val unit: String
)
