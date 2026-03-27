package com.example.aitaskgenius.network

import com.example.aitaskgenius.data.model.Recipe
import com.example.aitaskgenius.util.AiRecipeResponse
import com.example.aitaskgenius.util.UnsplashProxyResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface RecipeApiService {
    
    @POST("add_recipe.php")
    fun exportRecipe(@Body recipe: Recipe): Call<Void>

    @Multipart
    @POST("add_recipe.php")
    fun exportRecipeMultipart(
        @Part("recipe") recipe: RequestBody,
        @Part image: MultipartBody.Part?
    ): Call<Void>

    @POST("update_recipe.php")
    fun updateRecipe(@Body recipe: Recipe): Call<Void>

    @Multipart
    @POST("update_recipe.php")
    fun updateRecipeMultipart(
        @Part("recipe") recipe: RequestBody,
        @Part image: MultipartBody.Part?
    ): Call<Void>

    @GET("generate_recipe_ai.php")
    fun generateRecipe(@Query("dishName") dishName: String): Call<AiRecipeResponse>

    /**
     * NUEVO: Llama al proxy PHP para obtener una imagen segura de Unsplash.
     */
    @GET("get_unsplash_image.php")
    fun getUnsplashImageProxy(@Query("query") query: String): Call<UnsplashProxyResponse>
}
