package com.example.aitaskgenius.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.aitaskgenius.data.model.Recipe
import com.example.aitaskgenius.data.model.RecipeConverters

/**
 * BASE DE DATOS: AppDatabase
 *
 * EXPLICACIÓN PARA ALUMNOS:
 * 1. @Database: Define qué tablas tiene (Recipe).
 * 2. @TypeConverters: Le dice a Room cómo usar los convertidores de GSON que creamos.
 */
@Database(entities = [Recipe::class], version = 1, exportSchema = false)
@TypeConverters(RecipeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    // Método para obtener el DAO
    abstract fun recipeDao(): RecipeDao

    companion object {
        // @Volatile asegura que el valor sea siempre el mismo para todos los hilos
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Método Singleton para obtener la base de datos.
         * Evita crear múltiples conexiones pesadas al mismo tiempo.
         */
        fun getDatabase(context: Context): AppDatabase {
            // Si la instancia existe la devuelve, si no, la crea
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_database" // Nombre del archivo físico en el celular
                )

                    // NOTA: En apps reales usamos Corrutinas, pero para este ejercicio
                    // básico permitiremos consultas en el hilo principal para evitar errores.
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration() // Borra y recrea si cambiamos el modelo
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}