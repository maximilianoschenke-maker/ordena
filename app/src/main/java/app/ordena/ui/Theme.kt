package app.ordena.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.ordena.R
import app.ordena.data.AppearanceDefaults

/** Paleta: negros y grises neutros. El acento se elige en Personalizar (ver [Accents]). */
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
    val OnAccent = Color(0xFF0A0A0A) // todos los acentos son claros: el texto sobre ellos es casi negro
    val Scrim = Color(0x99000000)
}

data class AppFont(val id: String, val label: String, val description: String, val family: FontFamily)

/** Tipografías incluidas (todas con licencia SIL Open Font License, ver /licenses). */
object AppFonts {
    private fun family(regular: Int, medium: Int, semibold: Int) = FontFamily(
        Font(regular, FontWeight.Normal),
        Font(medium, FontWeight.Medium),
        Font(semibold, FontWeight.SemiBold)
    )

    val all: List<AppFont> = listOf(
        AppFont(
            "geist",
            "Geist",
            "Neutra y precisa",
            family(R.font.geist_regular, R.font.geist_medium, R.font.geist_semibold)
        ),
        AppFont(
            "space_grotesk",
            "Space Grotesk",
            "Técnica, con carácter",
            family(R.font.spacegrotesk_regular, R.font.spacegrotesk_medium, R.font.spacegrotesk_semibold)
        ),
        AppFont(
            "sora",
            "Sora",
            "Geométrica y amplia",
            family(R.font.sora_regular, R.font.sora_medium, R.font.sora_semibold)
        ),
        AppFont(
            "manrope",
            "Manrope",
            "Elegante y suave",
            family(R.font.manrope_regular, R.font.manrope_medium, R.font.manrope_semibold)
        ),
        AppFont(
            "exo2",
            "Exo 2",
            "Futurista",
            family(R.font.exo2_regular, R.font.exo2_medium, R.font.exo2_semibold)
        )
    )

    fun byId(id: String): AppFont = all.firstOrNull { it.id == id } ?: all.first()
}

data class AccentOption(val id: String, val label: String, val color: Color)

object Accents {
    val all: List<AccentOption> = listOf(
        AccentOption("blanco", "Blanco", Color(0xFFFAFAFA)),
        AccentOption("champana", "Champaña", Color(0xFFD9C9A8)),
        AccentOption("hielo", "Azul hielo", Color(0xFFA8BFF0)),
        AccentOption("menta", "Menta", Color(0xFFB5DCC2))
    )

    fun byId(id: String): AccentOption = all.firstOrNull { it.id == id } ?: all.first()
}

/** Tipografía elegida (para campos de texto y dibujos que no heredan el estilo de Material). */
val LocalAppFont = staticCompositionLocalOf { AppFonts.all.first().family }

/** Color de acento elegido: botones principales, interruptores, selección y detalles del título. */
val LocalAccent = staticCompositionLocalOf { Accents.all.first().color }

private val BaseTypography = Typography()

private fun TextStyle.with(font: FontFamily) = copy(fontFamily = font)

private fun typographyFor(font: FontFamily) = Typography(
    displayLarge = BaseTypography.displayLarge.with(font),
    displayMedium = BaseTypography.displayMedium.with(font),
    displaySmall = BaseTypography.displaySmall.with(font),
    headlineLarge = BaseTypography.headlineLarge.with(font),
    headlineMedium = BaseTypography.headlineMedium.with(font),
    headlineSmall = BaseTypography.headlineSmall.with(font),
    titleLarge = BaseTypography.titleLarge.with(font),
    titleMedium = BaseTypography.titleMedium.with(font),
    titleSmall = BaseTypography.titleSmall.with(font),
    bodyLarge = BaseTypography.bodyLarge.with(font),
    bodyMedium = BaseTypography.bodyMedium.with(font),
    bodySmall = BaseTypography.bodySmall.with(font),
    labelLarge = BaseTypography.labelLarge.with(font),
    labelMedium = BaseTypography.labelMedium.with(font),
    labelSmall = BaseTypography.labelSmall.with(font)
)

private fun colorsFor(accent: Color) = darkColorScheme(
    primary = accent,
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
fun OrdenaTheme(
    fontId: String = AppearanceDefaults.FONT_ID,
    accentId: String = AppearanceDefaults.ACCENT_ID,
    content: @Composable () -> Unit
) {
    val font = AppFonts.byId(fontId).family
    val accent = Accents.byId(accentId).color
    val typography = remember(font) { typographyFor(font) }
    val colors = remember(accent) { colorsFor(accent) }
    CompositionLocalProvider(LocalAppFont provides font, LocalAccent provides accent) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
