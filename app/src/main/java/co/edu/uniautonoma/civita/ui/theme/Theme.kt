package co.edu.uniautonoma.civita.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CivitaColors = lightColorScheme(
    primary = TealDark,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Teal,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = Gold,
    background = SurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Ink,
    onBackground = Ink,
)

@Composable
fun CivitaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CivitaColors, typography = CivitaTypography,
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20),
            large = androidx.compose.foundation.shape.RoundedCornerShape(28)), content = content)
}
