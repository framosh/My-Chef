package com.example.aitaskgenius.data.repository

import android.content.Context
import com.example.aitaskgenius.data.model.Recipe
import com.example.aitaskgenius.data.model.RecipeCategory

/**
 * REPOSITORIO: RecipeRepository
 *
 * EXPLICACIÓN PARA ALUMNOS:
 * El repositorio ahora es un Singleton (object) que gestiona el acceso a la Base de Datos.
 * Ya no usamos una lista mutable en memoria, ahora usamos el DAO para leer y escribir.
 */
object RecipeRepository {

    private var recipeDao: RecipeDao? = null

    /**
     * INICIALIZACIÓN:
     * Room necesita un 'Context' para abrir el archivo de la base de datos.
     * Llamaremos a esta función en el onCreate de MainActivity.
     */
    fun init(context: Context) {
        if (recipeDao == null) {
            val db = AppDatabase.getDatabase(context)
            recipeDao = db.recipeDao()
        }
    }

    /**
     * Guarda la receta de forma permanente en el almacenamiento físico.
     */
    fun saveRecipe(recipe: Recipe) {
        recipeDao?.insertRecipe(recipe)
        println("Log Educativo: Receta '${recipe.title}' guardada en Room.")
    }

    /**
     * Obtiene todas las recetas directamente desde Room.
     */
    fun getAllRecipes(): List<Recipe> {
        return recipeDao?.getAllRecipes() ?: emptyList()
    }

    /**
     * Realiza una búsqueda de texto directamente en la base de datos (más eficiente).
     */
    fun searchRecipesByName(query: String): List<Recipe> {
        return recipeDao?.searchRecipesByName(query) ?: emptyList()
    }

    /**
     * Filtra por categoría.
     */
    fun getRecipesByCategory(category: RecipeCategory): List<Recipe> {
        // Usamos el .name del Enum para comparar con el String guardado en la DB
        return recipeDao?.getRecipesByCategory(category.name) ?: emptyList()
    }

    fun deleteRecipeById(id: Int) {
        recipeDao?.deleteRecipeById(id)
    }
}
