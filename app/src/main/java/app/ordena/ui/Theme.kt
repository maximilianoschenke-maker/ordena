package app.ordena.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.ordena.R

/** Paleta: negros y grises neutros, con blanco como único acento. */
object Palette {
    val Background = Color(0xFF0A0A0A)
    val Surface = Color(0xFF141414)
    val SurfaceHigh = Color(0xFF1C1C1C)
    val SurfaceHigher = Color(0xFF262626)
    val Border = Color(0xFF262626)
    val BorderSoft = Color(0xFF1F1F1F)
    val Text = Color(0xFFFAFAFA)
    val TextMuted = Color(0xFFA3A3A3) // 8.6:1 sobre el fondo
    val TextDim = Color(0xFF8A8A8A) // 5.4:1 sobre las tarjetas
    val Accent = Color(0xFFFAFAFA)
    val OnAccent = Color(0xFF0A0A0A)
    val Scrim = Color(0x99000000)
}

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold)
)

private fun TextStyle.geist() = copy(fontFamily = Geist)

private val BaseTypography = Typography()

private val OrdenaTypography = Typography(
    displayLarge = BaseTypography.displayLarge.geist(),
    displayMedium = BaseTypography.displayMedium.geist(),
    displaySmall = BaseTypography.displaySmall.geist(),
    headlineLarge = BaseTypography.headlineLarge.geist(),
    headlineMedium = BaseTypography.headlineMedium.geist(),
    headlineSmall = BaseTypography.headlineSmall.geist(),
    titleLarge = BaseTypography.titleLarge.geist(),
    titleMedium = BaseTypography.titleMedium.geist(),
    titleSmall = BaseTypography.titleSmall.geist(),
    bodyLarge = BaseTypography.bodyLarge.geist(),
    bodyMedium = BaseTypography.bodyMedium.geist(),
    bodySmall = BaseTypography.bodySmall.geist(),
    labelLarge = BaseTypography.labelLarge.geist(),
    labelMedium = BaseTypography.labelMedium.geist(),
    labelSmall = BaseTypography.labelSmall.geist()
)

private val OrdenaColors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.OnAccent,
    secondary = Palette.TextMuted,
    onSecondary = Palette.Background,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextMuted,
    surfaceContainerLowest = Palette.Background,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigher,
    outline = Palette.Border,
    outlineVariant = Palette.BorderSoft,
    scrim = Palette.Scrim
)

@Composable
fun OrdenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OrdenaColors, typography = OrdenaTypography, content = content)
}
