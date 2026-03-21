package com.example.aitaskgenius.logic

import com.example.aitaskgenius.data.model.Ingredient
import com.example.aitaskgenius.data.model.Recipe

/**
 * CLASE DE LÓGICA: RecipeCalculator
 * El objetivo de esta clase es realizar cálculos matemáticos relacionados con las recetas.
 * Esto separa la interfaz de usuario de la lógica de negocio.
 */
class RecipeCalculator {

    /**
     * Función para calcular las cantidades ajustadas según el número de comensales.
     *
     * EXPLICACIÓN PARA ALUMNOS:
     * Para ajustar una receta, primero obtenemos un "factor de escala" dividiendo
     * los nuevos comensales entre los comensales base de la receta original.
     * Luego, multiplicamos cada ingrediente por ese factor.
     */
    fun calculateIngredientsForServings(recipe: Recipe, targetServings: Int): List<Ingredient> {
        // 1. Calculamos el factor de conversión
        // Usamos .toDouble() para no perder precisión decimal en la división
        val scaleFactor = targetServings.toDouble() / recipe.baseServings.toDouble()

        // 2. Creamos una nueva lista de ingredientes con las cantidades actualizadas
        return recipe.ingredients.map { ingredient ->
            ingredient.copy(
                baseQuantity = ingredient.baseQuantity * scaleFactor
            )
        }
    }

    /**
     * Función ejemplo para estimar el tiempo total (opcional para el curso)
     */
    fun formatPreparationSummary(recipe: Recipe): String {
        return "Esta receta de ${recipe.title} está pensada originalmente para ${recipe.baseServings} personas."
    }
}