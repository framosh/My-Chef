package com.example.aitaskgenius

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Importante para el diálogo
import androidx.appcompat.app.AppCompatActivity
import com.example.aitaskgenius.data.model.Recipe
import com.example.aitaskgenius.data.model.RecipeCategory
import com.example.aitaskgenius.data.repository.RecipeRepository
import com.example.aitaskgenius.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var currentRecipes: List<Recipe> = emptyList()
    private var recipeToDelete: Recipe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabBack.setOnClickListener { finish() }

        setupCategorySpinner()
        setupButtons()
        setupListView()
        setupDeleteButton() // Inicializamos el nuevo listener del botón borrar

        // Cargamos todas las recetas al inicio
        updateRecipeList(RecipeRepository.getAllRecipes())
    }

    private fun setupListView() {
        // CLIC CORTO: Abrir detalle del platillo
        binding.lvRecipes.setOnItemClickListener { _, _, position, _ ->
            val selectedRecipe = currentRecipes[position]
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("RECIPE_ID", selectedRecipe.id)
            startActivity(intent)
        }

        // CLIC LARGO: Marcar para eliminar
        binding.lvRecipes.setOnItemLongClickListener { _, view, position, _ ->
            recipeToDelete = currentRecipes[position]

            // Marcamos visualmente la fila seleccionada con el color café claro
            view.setBackgroundColor(android.graphics.Color.parseColor("#D7CCC8"))

            // Mostramos el botón flotante de eliminar (el rojo)
            binding.fabDelete.visibility = View.VISIBLE

            Toast.makeText(this, "Seleccionado: ${recipeToDelete?.title}", Toast.LENGTH_SHORT).show()
            true // Indica que el evento fue manejado
        }
    }

    private fun setupDeleteButton() {
        binding.fabDelete.setOnClickListener {
            recipeToDelete?.let { recipe ->

                // EXPLICACIÓN PARA ALUMNOS: Creamos un cuadro de diálogo para confirmar la acción
                AlertDialog.Builder(this)
                    .setTitle("Eliminar Receta")
                    .setMessage("¿Estás seguro de que deseas eliminar '${recipe.title}'? Esta acción no se puede deshacer.")
                    .setPositiveButton("ELIMINAR") { _, _ ->
                        // 1. Eliminamos de la base de datos a través del repositorio
                        RecipeRepository.deleteRecipeById(recipe.id)

                        // 2. Feedback visual y ocultamos el botón
                        Toast.makeText(this, "Receta eliminada correctamente", Toast.LENGTH_SHORT).show()
                        binding.fabDelete.visibility = View.GONE
                        recipeToDelete = null

                        // 3. Refrescamos la lista con los datos actuales del repositorio
                        updateRecipeList(RecipeRepository.getAllRecipes())
                    }
                    .setNegativeButton("CANCELAR") { dialog, _ ->
                        // Si cancela, ocultamos el botón y refrescamos para quitar la marca visual
                        binding.fabDelete.visibility = View.GONE
                        recipeToDelete = null
                        updateRecipeList(currentRecipes)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = RecipeCategory.entries.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerSearchCategory.adapter = adapter

        binding.spinnerSearchCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategoryName = categories[position]
                val selectedCategory = RecipeCategory.valueOf(selectedCategoryName)

                // Filtramos por categoría seleccionada
                val filteredResults = RecipeRepository.getRecipesByCategory(selectedCategory)
                updateRecipeList(filteredResults)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearchName.text.toString()
            val results = if (query.isEmpty()) {
                RecipeRepository.getAllRecipes()
            } else {
                RecipeRepository.searchRecipesByName(query)
            }
            updateRecipeList(results)
        }
    }

    private fun updateRecipeList(recipes: List<Recipe>) {
        currentRecipes = recipes

        if (recipes.isEmpty()) {
            binding.lvRecipes.adapter = null
            return
        }

        val recipeNames = recipes.map { it.title }
        val adapter = ArrayAdapter(
            this,
            R.layout.item_recipe_name,
            R.id.text1,
            recipeNames
        )
        binding.lvRecipes.adapter = adapter
    }
}