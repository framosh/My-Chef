package com.example.aitaskgenius.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // CAMBIO: Eliminamos el 'www.' para que coincida con el certificado SSL (mychef.itbp.com.mx)
    private const val BASE_URL = "https://mychef.itbp.com.mx/httpdocs/"

    val instance: RecipeApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(RecipeApiService::class.java)
    }
}
