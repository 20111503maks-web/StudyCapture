package app.studycapture.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studycapture.data.settings.ThemeMode

// Palette from the StudyCapture design canvas ("Ink & paper").
private val LightColors = lightColorScheme(
    primary = Color(0xFF3348D4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E8FF),
    onPrimaryContainer = Color(0xFF1B2A8F),
    secondary = Color(0xFF4A4F5C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECEEF2),
    onSecondaryContainer = Color(0xFF15171C),
    tertiary = Color(0xFF0F7A5C),
    onTertiary = Color.White,
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF15171C),
    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF15171C),
    surfaceVariant = Color(0xFFECEEF2),
    onSurfaceVariant = Color(0xFF4A4F5C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFECEEF2),
    surfaceContainerHighest = Color(0xFFE6E8ED),
    outline = Color(0xFFC4C8D0),
    outlineVariant = Color(0xFFDADDE3),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA0FF),
    onPrimary = Color(0xFF0D1550),
    primaryContainer = Color(0xFF232C66),
    onPrimaryContainer = Color(0xFFC9D1FF),
    secondary = Color(0xFFB4BAC6),
    onSecondary = Color(0xFF15171C),
    secondaryContainer = Color(0xFF252A33),
    onSecondaryContainer = Color(0xFFECEEF2),
    tertiary = Color(0xFF9FE0C5),
    onTertiary = Color(0xFF0B3326),
    background = Color(0xFF0E1014),
    onBackground = Color(0xFFECEEF2),
    surface = Color(0xFF0E1014),
    onSurface = Color(0xFFECEEF2),
    surfaceVariant = Color(0xFF1F232B),
    onSurfaceVariant = Color(0xFFB4BAC6),
    surfaceContainerLowest = Color(0xFF0B0C0F),
    surfaceContainerLow = Color(0xFF171A20),
    surfaceContainer = Color(0xFF171A20),
    surfaceContainerHigh = Color(0xFF1F232B),
    surfaceContainerHighest = Color(0xFF2A2F38),
    outline = Color(0xFF4A505C),
    outlineVariant = Color(0xFF2A2F38),
    error = Color(0xFFFF9C8A),
    onError = Color(0xFF3B0A02),
)

/** Colors the M3 scheme has no slot for: OCR uncertainty and "beyond your notes". */
@Immutable
data class StudyColors(
    val uncertainContainer: Color,
    val onUncertainContainer: Color,
    val uncertainBorder: Color,
    val success: Color,
    val subjectTints: List<Pair<Color, Color>>,
)

private val LightStudyColors = StudyColors(
    uncertainContainer = Color(0xFFFFF4DE),
    onUncertainContainer = Color(0xFF7A4A00),
    uncertainBorder = Color(0xFFE2A33A),
    success = Color(0xFF0F7A5C),
    subjectTints = listOf(
        Color(0xFFE4E8FF) to Color(0xFF1B2A8F),
        Color(0xFFDDF3EA) to Color(0xFF0B5C45),
        Color(0xFFFFE8D6) to Color(0xFF8A3B00),
        Color(0xFFEEE4FB) to Color(0xFF4B2A84),
        Color(0xFFD9F1F4) to Color(0xFF0A5560),
        Color(0xFFFBE3EC) to Color(0xFF85204A),
    ),
)

private val DarkStudyColors = StudyColors(
    uncertainContainer = Color(0xFF2B2313),
    onUncertainContainer = Color(0xFFF3CE84),
    uncertainBorder = Color(0xFF8A6A2A),
    success = Color(0xFF9FE0C5),
    subjectTints = listOf(
        Color(0xFF232C66) to Color(0xFFC9D1FF),
        Color(0xFF12382D) to Color(0xFF9FE0C5),
        Color(0xFF3D2513) to Color(0xFFFFC59A),
        Color(0xFF2E2147) to Color(0xFFD7C2F7),
        Color(0xFF0F3338) to Color(0xFF9FDCE4),
        Color(0xFF3F1A2A) to Color(0xFFF5B3CB),
    ),
)

val LocalStudyColors = staticCompositionLocalOf { LightStudyColors }

private val AppTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.6.sp),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun StudyCaptureTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors: ColorScheme = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalStudyColors provides if (dark) DarkStudyColors else LightStudyColors) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

object StudyTheme {
    val colors: StudyColors @Composable get() = LocalStudyColors.current
}
