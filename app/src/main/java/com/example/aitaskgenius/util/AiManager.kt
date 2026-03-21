package com.example.aitaskgenius.util

import android.util.Log
import com.example.aitaskgenius.data.model.RecipeCategory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.gson.Gson

class AiManager {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyA-MLLj8CHrehX1W9P6-6r_qpdQHhR3S1U",
        requestOptions = RequestOptions(apiVersion = "v1beta")
    )

    suspend fun generateRecipe(dishName: String): AiRecipeResponse? {
        val categories = RecipeCategory.entries.joinToString(", ") { it.name }
        val prompt = "Genera una receta para '$dishName' estrictamente en formato JSON: {\"category\":\"una de: $categories\", \"servings\":4, \"ingredients\":[{\"name\":\"nombre\",\"quantity\":1.0,\"unit\":\"unidad\"}], \"steps\":[\"paso 1\"]}. No incluyas texto explicativo."

        return try {
            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: ""
            if (jsonText.isNotEmpty()) {
                Gson().fromJson(jsonText, AiRecipeResponse::class.java)
            } else null
        } catch (e: Exception) {
            Log.e("AiManager", "Error generating recipe: ${e.message}", e)
            null
        }
    }
}

// Mantenemos estas clases aquí para que sean accesibles
data class AiRecipeResponse(val ingredients: List<AiIngredient>, val steps: List<String>, val category: String, val servings: Int)
data class AiIngredient(val name: String, val quantity: Double, val unit: String)
