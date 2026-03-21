package com.example.aitaskgenius.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * CLASE DE DATOS: Recipe
 * Añadimos @Entity para que Room la reconozca como una tabla.
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val id: Int, // Room requiere una llave primaria
    val title: String,
    val description: String,
    val category: RecipeCategory,
    val baseServings: Int,
    val ingredients: List<Ingredient>,
    val preparationSteps: List<PreparationStep>,
    val imageUrl: String? = null,
    val servingSuggestion: String,

@Ignore // Esto evita que Room lo guarde en el celular, pero Retrofit lo enviará
var imageEncoded: String? = null
) {
    // Constructor secundario para que Room no se confunda
    constructor(id: Int, title: String, description: String, category: RecipeCategory,
        baseServings: Int, ingredients: List<Ingredient>,
    preparationSteps: List<PreparationStep>, imageUrl: String?,
    servingSuggestion: String) :
    this(id, title, description, category, baseServings, ingredients,
        preparationSteps, imageUrl, servingSuggestion, null)
}

/**
 * ENUM CLASS: Categorías
 */
enum class RecipeCategory {
    SOPAS, BOCADILLOS, GUISADOS, ENSALADAS, POSTRES, PASTELES, SALSAS, BEBIDAS, COKTELES
}

/**
 * Representa un ingrediente
 */
data class Ingredient(
    val name: String,
    val baseQuantity: Double,
    val unit: String
)

/**
 * Representa un paso específico
 */
data class PreparationStep(
    val orderprep: Int,
    val description: String
)

/**
 * CONVERTIDORES: RecipeConverters
 * EXPLICACIÓN PARA ALUMNOS:
 * Room no puede guardar listas directamente. Esta clase convierte
 * las listas en un String (JSON) para guardarlas y de vuelta a objetos al leerlas.
 */
class RecipeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromIngredientList(value: List<Ingredient>): String = gson.toJson(value)

    @TypeConverter
    fun toIngredientList(value: String): List<Ingredient> {
        val listType = object : TypeToken<List<Ingredient>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStepList(value: List<PreparationStep>): String = gson.toJson(value)

    @TypeConverter
    fun toStepList(value: String): List<PreparationStep> {
        val listType = object : TypeToken<List<PreparationStep>>() {}.type
        return gson.fromJson(value, listType)
    }
}