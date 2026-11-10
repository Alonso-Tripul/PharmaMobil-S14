# =====================================================================
# Sesion 14 · Reglas R8 de PharmaMobil (version de entrega)
# =====================================================================

# Trazas legibles en Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod

# Modelos de dominio (Producto, Sesion, EnvioPendiente...): el JSON se arma
# a mano con JsonObject, pero se conservan por seguridad y para Crashlytics.
-keep class pe.edu.upeu.pharmamobil.domain.model.** { *; }

# Errores de sesion: se comparan por tipo en tiempo de ejecucion
-keep class pe.edu.upeu.pharmamobil.domain.model.ErrorSesion* { *; }

# kotlinx.serialization (motor JSON de Ktor)
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** serializer(...);
}
-dontwarn kotlinx.serialization.**

# Ktor (motor Android) y su logging
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**

# Koin: ViewModels creados por el contenedor
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-dontwarn org.koin.**

# SQLDelight
-dontwarn app.cash.sqldelight.**

# Firebase (GitLive) - las librerias traen sus reglas; solo silenciar avisos
-dontwarn dev.gitlive.**
-dontwarn kotlinx.coroutines.**
