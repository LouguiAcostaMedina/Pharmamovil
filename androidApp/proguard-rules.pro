# ==============================================================================
# Reglas de Minificación y Optimización R8 / ProGuard para PharmaMobile (Release)
# ==============================================================================

# Preservar atributos esenciales de reflexión y stacktraces legibles
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# 1. Kotlinx Serialization (Modelos de Transporte y Deserialización JSON)
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    *** Companion;
}
-keepnames class kotlinx.serialization.internal.** { *; }
-keepclassmembers class **$$serializer {
    public static final **$$serializer INSTANCE;
    public static final kotlinx.serialization.KSerializer *;
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}

# ------------------------------------------------------------------------------
# 2. Inyección de Dependencias (Koin)
# ------------------------------------------------------------------------------
-keep class io.insert.koin.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * implements org.koin.core.module.Module { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ------------------------------------------------------------------------------
# 3. Cliente HTTP y Red (Ktor & OkHttp)
# ------------------------------------------------------------------------------
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn okhttp3.**

# ------------------------------------------------------------------------------
# 4. Base de Datos Local y Transaccionalidad (SQLDelight & SQLite)
# ------------------------------------------------------------------------------
-keep class app.cash.sqldelight.** { *; }
-keep class pe.edu.upeu.pharmamobile.db.** { *; }
-keep class androidx.sqlite.** { *; }

# ------------------------------------------------------------------------------
# 5. Modelos de Dominio y Data Transfer Objects (DTOs)
# ------------------------------------------------------------------------------
-keep class pe.edu.upeu.pharmamobile.data.model.** { *; }
-keep class pe.edu.upeu.pharmamobile.domain.model.** { *; }
-keep class pe.edu.upeu.pharmamobile.domain.permission.** { *; }
-keep class pe.edu.upeu.pharmamobile.domain.telemetria.** { *; }
