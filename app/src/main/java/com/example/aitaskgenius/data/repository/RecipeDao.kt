package com.example.aitaskgenius.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.aitaskgenius.data.model.Recipe

@Dao
interface RecipeDao {

    // Obtener todas las recetas ordenadas por título
    @Query("SELECT * FROM recipes ORDER BY title ASC")
    fun getAllRecipes(): List<Recipe>

    // Insertar una receta. Si el ID ya existe, lo reemplaza (útil para editar)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecipe(recipe: Recipe)

    // Buscar recetas por nombre (usando el operador LIKE para coincidencias parciales)
    // EXPLICACIÓN: El operador || concatena los % para buscar en cualquier parte del texto
    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :searchQuery || '%'")
    fun searchRecipesByName(searchQuery: String): List<Recipe>

    // Filtrar por categoría
    @Query("SELECT * FROM recipes WHERE category = :categoryName")
    fun getRecipesByCategory(categoryName: String): List<Recipe>

    // Opcional: Borrar una receta específica
    @Query("DELETE FROM recipes WHERE id = :recipeId")
    fun deleteRecipeById(recipeId: Int)
}