package com.example.aitaskgenius

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aitaskgenius.data.model.Ingredient
import com.example.aitaskgenius.data.model.Recipe
import com.example.aitaskgenius.data.model.RecipeCategory
import com.example.aitaskgenius.data.repository.RecipeRepository
import com.example.aitaskgenius.databinding.ActivityMainBinding
import com.example.aitaskgenius.network.ApiClient
import com.example.aitaskgenius.util.AiManager
import com.example.aitaskgenius.util.ImageManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var selectedImageUri: String? = null
    private val tempSteps = mutableListOf<com.example.aitaskgenius.data.model.PreparationStep>()
    private lateinit var binding: ActivityMainBinding
    private val recipeRepository = RecipeRepository
    private val tempIngredients = mutableListOf<Ingredient>()
    
    // Inicializamos el Manager de IA
    private val aiManager = AiManager()

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val contentResolver = applicationContext.contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            selectedImageUri = it.toString()
            binding.ivDishPhoto.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecipeRepository.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivMainLogo.setOnLongClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
            true 
        }

        setupSpinners()
        setupButtons()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges()) showExitConfirmationDialog()
                else {
                    isEnabled = false 
                    onBackPressedDispatcher.onBackPressed() 
                }
            }
        })
    }

    private fun setupSpinners() {
        val categories = RecipeCategory.entries.map { it.name }
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        
        val units = listOf("gr", "Kg", "ml", "lt", "pieza", "taza", "vaso", "vaso tequilero", "cuchara ch", "cuchara gd")
        binding.spinnerUnit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units)
    }

    private fun setupButtons() {
        binding.btnSupport.setOnClickListener { startActivity(Intent(this, SupportActivity::class.java)) }
        binding.btnAddIngredient.setOnClickListener { addIngredientToList() }
        binding.btnSave.setOnClickListener { saveRecipeData() }
        binding.btnGoToSearch.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }
        binding.btnAddStep.setOnClickListener { addStepToList() }
        binding.btnSelectPhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnGenerateAI.setOnClickListener { generateRecipeWithAI() }
    }

    private fun saveRecipeData() {
        val name = binding.etDishName.text.toString().trim()
        val servingsStr = binding.etServings.text.toString().trim()
        val selectedCategory = RecipeCategory.valueOf(binding.spinnerCategory.selectedItem.toString())

        if (name.isEmpty() || servingsStr.isEmpty() || tempIngredients.isEmpty() || tempSteps.isEmpty()) {
            showError("Complete todos los campos")
            return
        }

        val finalImageUri = selectedImageUri ?: "android.resource://${packageName}/${R.drawable.splash_chef}"
        val servings = servingsStr.toInt()
        val newRecipe = Recipe(
            id = (System.currentTimeMillis() % 10000).toInt(),
            title = name,
            description = "Receta de IA para $servings personas",
            category = selectedCategory,
            baseServings = servings,
            ingredients = ArrayList(tempIngredients),
            preparationSteps = ArrayList(tempSteps),
            servingSuggestion = "Servir caliente",
            imageUrl = finalImageUri
        )

        // Usamos el ImageManager para el Base64
        newRecipe.imageEncoded = ImageManager.getBase64FromUri(this, finalImageUri)
        recipeRepository.saveRecipe(newRecipe)
        exportToRemoteDatabase(newRecipe)
        clearFields()
    }

    private fun exportToRemoteDatabase(recipe: Recipe) {
        ApiClient.instance.exportRecipe(recipe).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) Toast.makeText(this@MainActivity, "Sincronizado", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("HTTP_EXPORT", "Fallo de red: ${t.message}")
            }
        })
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun addStepToList() {
        val desc = binding.etStepDescription.text.toString().trim()
        if (desc.isNotEmpty()) {
            tempSteps.add(com.example.aitaskgenius.data.model.PreparationStep(tempSteps.size + 1, desc))
            updateStepsPreview()
            binding.etStepDescription.text?.clear()
        }
    }

    private fun updateStepsPreview() {
        binding.tvAddedStepsList.text = tempSteps.joinToString("\n") { "${it.orderprep}. ${it.description}" }.ifEmpty { "Pasos: ninguno" }
    }

    private fun addIngredientToList() {
        val name = binding.etIngredientName.text.toString().trim()
        val qtyStr = binding.etIngredientQty.text.toString().trim()
        val unit = binding.spinnerUnit.selectedItem.toString()

        if (name.isNotEmpty() && qtyStr.isNotEmpty()) {
            tempIngredients.add(Ingredient(name, qtyStr.toDouble(), unit))
            updateIngredientPreview()
            binding.etIngredientName.text?.clear()
            binding.etIngredientQty.text?.clear()
        }
    }

    private fun updateIngredientPreview() {
        binding.tvAddedIngredientsList.text = tempIngredients.joinToString("\n") { "- ${it.baseQuantity} ${it.unit} de ${it.name}" }.ifEmpty { "Ingredientes: ninguno" }
    }

    private fun clearFields() {
        binding.etDishName.text?.clear()
        binding.etServings.text?.clear()
        binding.ivDishPhoto.setImageResource(R.drawable.splash_chef)
        selectedImageUri = null
        tempIngredients.clear()
        tempSteps.clear()
        updateStepsPreview()
        updateIngredientPreview()
    }

    private fun hasUnsavedChanges() = binding.etDishName.text.toString().isNotEmpty()

    private fun showExitConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Deseas salir?")
            .setPositiveButton("SALIR") { _, _ -> performLogout() }
            .setNegativeButton("QUEDARME", null)
            .show()
    }

    private fun performLogout() {
        Firebase.auth.signOut()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun generateRecipeWithAI() {
        val dishName = binding.etDishName.text.toString().trim()
        if (dishName.isEmpty()) {
            showError("Escribe un nombre")
            return
        }

        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "IA Generando Receta... ✨", Toast.LENGTH_SHORT).show()
                
                // Llamamos al AiManager
                val aiRecipe = aiManager.generateRecipe(dishName)

                if (aiRecipe != null) {
                    binding.etServings.setText(aiRecipe.servings.toString())
                    val catIndex = RecipeCategory.entries.indexOfFirst { it.name.equals(aiRecipe.category, true) }
                    if (catIndex != -1) binding.spinnerCategory.setSelection(catIndex)

                    tempIngredients.clear()
                    tempSteps.clear()
                    aiRecipe.ingredients.forEach { tempIngredients.add(Ingredient(it.name, it.quantity, it.unit)) }
                    aiRecipe.steps.forEachIndexed { i, s -> tempSteps.add(com.example.aitaskgenius.data.model.PreparationStep(i + 1, s)) }

                    updateIngredientPreview()
                    updateStepsPreview()
                    
                    // Descargamos imagen con ImageManager
                    val imageFile = ImageManager.downloadAiImage(this@MainActivity, dishName)
                    imageFile?.let {
                        selectedImageUri = Uri.fromFile(it).toString()
                        binding.ivDishPhoto.setImageBitmap(android.graphics.BitmapFactory.decodeFile(it.absolutePath))
                        Toast.makeText(this@MainActivity, "¡Listo! ✨", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showError("La IA no pudo generar la receta")
                }
            } catch (e: Exception) {
                Log.e("AI_ERROR", "Error: ${e.message}")
                showError("IA ocupada, intente de nuevo")
            }
        }
    }
}
