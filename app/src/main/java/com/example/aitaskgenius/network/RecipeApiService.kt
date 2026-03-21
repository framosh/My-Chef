package com.example.aitaskgenius.network

import com.example.aitaskgenius.data.model.Recipe
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Define cómo se comunica la App con tu servidor
interface RecipeApiService {
    // @POST especifica que esta es una petición de tipo POST
    // "save_recipe.php" es el nombre del archivo en tu servidor que recibirá los datos
    @POST("add_recipe.php")
    fun exportRecipe(@Body recipe: Recipe): Call<Void> // @Body convierte el objeto Recipe a JSON

    // En RecipeApiService.kt
    @POST("update_recipe.php") // <--- Nuevo proceso PHP
    fun updateRecipe(@Body recipe: Recipe): Call<Void>
}
