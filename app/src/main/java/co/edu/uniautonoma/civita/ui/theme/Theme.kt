package co.edu.uniautonoma.civita.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp

private val CivitaColors = lightColorScheme(
    primary = TealDark, onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFB5F0E8),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF00201C),
    secondary = androidx.compose.ui.graphics.Color(0xFF4D5D77),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD9E2F8),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF091B32),
    tertiary = Gold, onTertiary = androidx.compose.ui.graphics.Color.White,
    background = SurfaceLight, onBackground = Ink,
    surface = androidx.compose.ui.graphics.Color.White, onSurface = Ink,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE1E8EB),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF414B50),
    outline = androidx.compose.ui.graphics.Color(0xFF71797C),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    onError = androidx.compose.ui.graphics.Color.White,
)

private val CivitaDarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF7AD9CE),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003731),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF005047),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF9CF1E5),
    secondary = androidx.compose.ui.graphics.Color(0xFFB9C7E2),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF233149),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF394761),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD9E2F8),
    tertiary = androidx.compose.ui.graphics.Color(0xFFE8C36D),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF3C2F00),
    background = androidx.compose.ui.graphics.Color(0xFF101719),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE0E5E7),
    surface = androidx.compose.ui.graphics.Color(0xFF171F22),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E5E7),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF41494C),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC1C9CB),
    outline = androidx.compose.ui.graphics.Color(0xFF8B9496),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError = androidx.compose.ui.graphics.Color(0xFF690005),
)

@Composable
fun CivitaTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) CivitaDarkColors else CivitaColors
    MaterialTheme(colorScheme = colors, typography = CivitaTypography,
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)), content = content)
}
