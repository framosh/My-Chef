package com.example.aitaskgenius

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aitaskgenius.data.model.Ingredient
import com.example.aitaskgenius.data.model.Recipe
import com.example.aitaskgenius.data.repository.RecipeRepository
import com.example.aitaskgenius.databinding.ActivityRecipeDetailBinding
import com.example.aitaskgenius.logic.RecipeCalculator
import com.example.aitaskgenius.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeDetailBinding
    private var recipe: Recipe? = null
    private val calculator = RecipeCalculator()

    // 1. LANZADOR PARA SELECCIONAR NUEVA IMAGEN Y SINCRONIZAR
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { newUri ->
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                applicationContext.contentResolver.takePersistableUriPermission(newUri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            binding.ivDetailImage.setImageURI(newUri)

            recipe?.let { currentRecipe ->
                // Actualización local (Room)
                val updatedRecipe = currentRecipe.copy(imageUrl = newUri.toString())
                RecipeRepository.saveRecipe(updatedRecipe)
                recipe = updatedRecipe

                // Sincronización remota (Servidor)
                lifecycleScope.launch {
                    val base64 = withContext(Dispatchers.IO) {
                        getBase64FromUri(newUri.toString())
                    }

                    if (base64 != null) {
                        // Importante: Asignamos el Base64 antes de enviar
                        updatedRecipe.imageEncoded = base64
                        exportToRemoteDatabase(updatedRecipe)
                    } else {
                        Log.e("REMOTE_SYNC", "Error al convertir imagen a Base64")
                    }
                }
                Toast.makeText(this, "Imagen actualizada localmente", Toast.LENGTH_SHORT).show()
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

    private fun loadRecipeData(it: Recipe) {
        binding.tvDetailTitle.text = it.title
        binding.tvDetailInfo.text = "${it.category} • Base original: ${it.baseServings} personas"

        it.imageUrl?.let { uriString ->
            if (uriString.isNotEmpty()) {
                try {
                    val imageUri = Uri.parse(uriString)
                    binding.ivDetailImage.setImageURI(imageUri)
                } catch (e: Exception) {
                    binding.ivDetailImage.setImageResource(android.R.drawable.stat_notify_error)
                }
            }
        }

        binding.etTargetServings.setText(it.baseServings.toString())
        displayIngredients(it.ingredients)

        binding.tvDetailSteps.text = it.preparationSteps.joinToString("\n\n") { step ->
            "${step.orderprep}. ${step.description.trim()}"
        }
    }

    private fun calculateNewQuantities() {
        val targetStr = binding.etTargetServings.text.toString()
        if (targetStr.isNotEmpty() && recipe != null) {
            val targetPeople = targetStr.toInt()
            if (targetPeople > 0) {
                val adjustedIngredients = calculator.calculateIngredientsForServings(recipe!!, targetPeople)
                displayIngredients(adjustedIngredients)
                Toast.makeText(this, "Ajustado para $targetPeople personas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayIngredients(ingredients: List<Ingredient>) {
        binding.tvDetailIngredients.text = ingredients.joinToString("\n") { ing ->
            "• ${String.format("%.2f", ing.baseQuantity)} ${ing.unit} de ${ing.name}"
        }
    }

    private fun shareRecipeByWhatsapp() {
        recipe?.let { r ->
            val message = StringBuilder()
            message.append("*🍽️ RECETA: ${r.title.uppercase()}*\n")
            message.append("_Asistente: AiTaskGenius_\n\n")
            message.append("*👥 Cantidad de Comensales:* ${binding.etTargetServings.text}\n\n")
            message.append("*🛒 INGREDIENTES:*\n")
            message.append(binding.tvDetailIngredients.text)
            message.append("\n\n*👨‍🍳 PREPARACIÓN:*\n")
            message.append(binding.tvDetailSteps.text)
            message.append("\n\n_¡Buen provecho!_")

            val intent = Intent(Intent.ACTION_SEND)
            if (!r.imageUrl.isNullOrEmpty()) {
                val imageUri = Uri.parse(r.imageUrl)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_STREAM, imageUri)
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
                val chooser = Intent.createChooser(intent, "Compartir receta")
                startActivity(chooser)
            }
        }
    }

    private fun getBase64FromUri(uriString: String?): String? {
        if (uriString == null) return null
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun exportToRemoteDatabase(recipe: Recipe) {
        ApiClient.instance.updateRecipe(recipe).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("REMOTE_SYNC", "Sincronización exitosa en servidor")
                    Toast.makeText(this@RecipeDetailActivity, "Sincronizado con servidor", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("REMOTE_SYNC", "Error servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("REMOTE_SYNC", "Fallo de red: ${t.message}")
            }
        })
    }
}