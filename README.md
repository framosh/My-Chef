# My Chef - AiTask Genius ✨

**My Chef** es una aplicación inteligente para Android diseñada para simplificar la creación y gestión de recetas culinarias. Utilizando el poder de la Inteligencia Artificial, la aplicación permite a los usuarios generar recetas completas y visualizarlas instantáneamente.

## 🚀 Características Principales

- **Generador Mágico de Recetas**: Ingresa el nombre de un platillo y deja que **Google Gemini AI** genere automáticamente la lista de ingredientes, pasos de preparación, categorías y porciones recomendadas.
- **Visualización con IA**: Integración con **Pollinations AI** para generar imágenes realistas y profesionales de los platillos creados, dándole vida visual a cada receta.
- **Sincronización Dual**:
    - **Local**: Almacenamiento persistente con **Room Database** para acceso sin conexión.
    - **Remota**: Sincronización con servidores MySQL externos mediante **Retrofit** y PHP.
- **Autenticación Segura**: Sistema de acceso mediante **Firebase Auth** y Google Sign-In moderno.
- **Arquitectura Limpia**: Código organizado de forma modular utilizando Managers especializados (`AiManager`, `ImageManager`) para facilitar el mantenimiento y la escalabilidad.

## 🛠️ Tecnologías Utilizadas

- **Lenguaje**: [Kotlin](https://kotlinlang.org/)
- **IA Generativa**: [Google AI SDK (Gemini)](https://ai.google.dev/) & [Pollinations AI](https://pollinations.ai/)
- **Backend**: [Firebase](https://firebase.google.com/) (Auth, Realtime Database, Analytics)
- **Base de Datos Local**: [Room](https://developer.android.com/training/data-storage/room)
- **Redes**: [Retrofit](https://square.github.io/retrofit/) & [GSON](https://github.com/google/gson)
- **UI**: Material Design 3, ViewBinding, ScrollView & ConstraintLayout.
- **Concurrencia**: Kotlin Coroutines & Lifecycle Scope.

## 📋 Requisitos Previos

Para ejecutar este proyecto, necesitarás:
1. Una **API Key** de Google AI Studio (Gemini).
2. Configurar un proyecto en **Firebase** y descargar el archivo `google-services.json`.
3. Un servidor web con soporte para PHP/MySQL para la sincronización remota.

## 📂 Estructura del Proyecto (Lógica)

- `util/AiManager.kt`: Gestiona toda la comunicación y el procesamiento de prompts con Gemini.
- `util/ImageManager.kt`: Encargado de la limpieza de datos, descarga de imágenes por IA y conversión a Base64.
- `network/ApiClient.kt`: Configuración de Retrofit para la conexión con la base de datos remota.
- `data/repository/`: Manejo de la persistencia local con Room.

---
Desarrollado con ❤️ para amantes de la cocina y la tecnología.
