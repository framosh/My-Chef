package com.example.aitaskgenius

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.aitaskgenius.data.model.Ingredient
import com.example.aitaskgenius.data.model.Recipe
import com.example.aitaskgenius.data.repository.RecipeRepository
import com.example.aitaskgenius.databinding.ActivityRecipeDetailBinding
import com.example.aitaskgenius.logic.RecipeCalculator
import com.example.aitaskgenius.network.ApiClient
import com.example.aitaskgenius.util.ImageManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeDetailBinding
    private var recipe: Recipe? = null
    private val calculator = RecipeCalculator()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { newUri ->
            // 1. Mostrar visualmente de inmediato
            binding.ivDetailImage.setImageURI(newUri)

            recipe?.let { currentRecipe ->
                lifecycleScope.launch {
                    // 2. Convertir URI a File físico
                    val file = ImageManager.uriToFile(this@RecipeDetailActivity, newUri)
                    if (file != null) {
                        // 3. Generar Base64 de la NUEVA imagen
                        val base64 = ImageManager.fileToBase64(file)
                        
                        // 4. Crear objeto actualizado con la nueva URI local y el Base64 para el servidor
                        val updatedRecipe = currentRecipe.copy(
                            imageUrl = newUri.toString(),
                            imageEncoded = base64
                        )

                        // 5. Guardar Localmente
                        RecipeRepository.saveRecipe(updatedRecipe)
                        recipe = updatedRecipe

                        // 6. Sincronizar con Servidor Remoto (JSON Directo con Base64)
                        updateRecipeOnServerSimple(updatedRecipe)
                    } else {
                        Toast.makeText(this@RecipeDetailActivity, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recipeId = intent.getIntExtra("RECIPE_ID", -1)
        recipe = RecipeRepository.getAllRecipes().find { it.id == recipeId }

        recipe?.let { loadRecipeData(it) }

        binding.btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnCalculate.setOnClickListener { calculateNewQuantities() }
        binding.fabBackDetail.setOnClickListener { finish() }
        binding.fabShareWhatsapp.setOnClickListener { shareRecipeByWhatsapp() }
    }

    private fun loadRecipeData(recipeData: Recipe) {
        binding.tvDetailTitle.text = recipeData.title
        binding.tvDetailInfo.text = getString(R.string.recipe_detail_info, recipeData.category, recipeData.baseServings)

        if (!recipeData.imageUrl.isNullOrEmpty()) {
            try {
                binding.ivDetailImage.setImageURI(recipeData.imageUrl.toUri())
            } catch (e: Exception) {
                Log.e("RecipeDetail", "Error carga imagen: ${e.message}")
                binding.ivDetailImage.setImageResource(android.R.drawable.stat_notify_error)
            }
        }

        binding.etTargetServings.setText(recipeData.baseServings.toString())
        displayIngredients(recipeData.ingredients)

        binding.tvDetailSteps.text = recipeData.preparationSteps.joinToString("\n\n") { step ->
            getString(R.string.step_item, step.orderprep, step.description.trim())
        }
    }

    private fun calculateNewQuantities() {
        val targetStr = binding.etTargetServings.text.toString()
        if (targetStr.isNotEmpty() && recipe != null) {
            val targetPeople = targetStr.toInt()
            if (targetPeople > 0) {
                val adjustedIngredients = calculator.calculateIngredientsForServings(recipe!!, targetPeople)
                displayIngredients(adjustedIngredients)
                Toast.makeText(this, getString(R.string.adjustment_toast, targetPeople), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayIngredients(ingredients: List<Ingredient>) {
        binding.tvDetailIngredients.text = ingredients.joinToString("\n") { ing ->
            getString(R.string.ingredient_item, ing.baseQuantity, ing.unit, ing.name)
        }
    }

    private fun shareRecipeByWhatsapp() {
        recipe?.let { r ->
            val message = StringBuilder()
            message.append("*🍽️ RECETA: ${r.title.uppercase()}*\n")
            message.append("_Asistente: My Chef_\n\n")
            message.append("*👥 Comensales:* ${binding.etTargetServings.text}\n\n")
            message.append("*🛒 INGREDIENTES:*\n")
            message.append(binding.tvDetailIngredients.text)
            message.append("\n\n*👨‍🍳 PREPARACIÓN:*\n")
            message.append(binding.tvDetailSteps.text)
            message.append("\n\n_¡Buen provecho!_")

            val intent = Intent(Intent.ACTION_SEND)
            if (!r.imageUrl.isNullOrEmpty()) {
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_STREAM, r.imageUrl.toUri())
                intent.putExtra(Intent.EXTRA_TEXT, message.toString())
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, message.toString())
            }

            intent.setPackage("com.whatsapp")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("RecipeDetail", "WhatsApp error: ${e.message}")
                startActivity(Intent.createChooser(intent, "Compartir receta"))
            }
        }
    }

    /**
     * Sincroniza la actualización de la receta (incluyendo la nueva imagen en Base64)
     * usando una petición POST simple.
     */
    private fun updateRecipeOnServerSimple(updatedRecipe: Recipe) {
        ApiClient.instance.updateRecipe(updatedRecipe).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RecipeDetailActivity, "Servidor actualizado con nueva imagen 🚀", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("REMOTE_SYNC", "Error servidor (${response.code()}): ${response.message()}")
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("REMOTE_SYNC", "Fallo red: ${t.message}")
            }
        })
    }
}
