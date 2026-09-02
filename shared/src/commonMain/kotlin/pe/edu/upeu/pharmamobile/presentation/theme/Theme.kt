package pe.edu.upeu.pharmamobile.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Corporate Theme Palette (Pharma Teal & Fresh Surfaces)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF74F8E5),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF051F1C),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF),
    onTertiaryContainer = Color(0xFF001E31),
    background = Color(0xFFF8FCFA),
    onBackground = Color(0xFF111D1B),
    surface = Color(0xFFF4FBF9),
    onSurface = Color(0xFF111D1B),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946)
)

// Dark Calibrated Slate Palette (Avoids harsh binary black/white inversion)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF53DBC9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF74F8E5),
    secondary = Color(0xFFB0CCC6),
    onSecondary = Color(0xFF1B3531),
    secondaryContainer = Color(0xFF324B47),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFFACCAE5),
    onTertiary = Color(0xFF133348),
    tertiaryContainer = Color(0xFF2C4961),
    onTertiaryContainer = Color(0xFFCCE5FF),
    background = Color(0xFF101514),
    onBackground = Color(0xFFE1E3E2),
    surface = Color(0xFF161D1C),
    onSurface = Color(0xFFE1E3E2),
    surfaceVariant = Color(0xFF26302E),
    onSurfaceVariant = Color(0xFFBEC9C5)
)

@Composable
fun PharmaMobilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
