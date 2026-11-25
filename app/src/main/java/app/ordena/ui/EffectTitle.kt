package app.ordena.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ordena.data.TitleEffect
import kotlin.math.pow
import kotlin.random.Random

/**
 * Título con un efecto futurista estático (no se anima): "desintegración" (las letras se deshacen
 * en píxeles hacia la derecha) o "velocidad" (letras inclinadas, cortes finos y líneas de movimiento). Se dibuja con un
 * generador aleatorio de semilla fija, así se ve siempre igual para el mismo texto.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun EffectTitle(
    text: String,
    effect: TitleEffect,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 34.sp,
    color: Color = Palette.Text
) {
    val measurer = rememberTextMeasurer()
    val font = LocalAppFont.current
    val accent = LocalAccent.current
    val density = LocalDensity.current
    val trailPx = with(density) { 52.dp.toPx() }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val maxWidthPx = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else 2000f
        val layout = remember(text, effect, font, fontSize, maxWidthPx) {
            val style = TextStyle(
                fontFamily = font,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1).sp,
                color = color
            )
            // Espacio libre para la estela (velocidad) o las partículas (desintegración).
            val available = when (effect) {
                // La inclinación corre la parte de arriba de las letras ~20 % de su alto.
                TitleEffect.SPEED -> maxWidthPx - trailPx - with(density) { (fontSize.value * SPEED_SLANT).dp.toPx() }
                TitleEffect.DISINTEGRATE -> maxWidthPx * 0.82f
                TitleEffect.NONE -> maxWidthPx
            }.coerceAtLeast(1f)
            val first = measurer.measure(text, style, softWrap = false, maxLines = 1)
            if (first.size.width <= available) {
                first
            } else {
                // Títulos largos: se achica la letra para que quepa en una línea.
                val scale = (available / first.size.width).coerceIn(0.5f, 1f)
                measurer.measure(text, style.copy(fontSize = fontSize * scale), softWrap = false, maxLines = 1)
            }
        }
        val heightDp = with(density) { layout.size.height.toDp() }
        val seed = text.hashCode() * 31 + effect.ordinal

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(heightDp)
                // Capa propia: los "borrados" (BlendMode.Clear) solo afectan al título, no al fondo.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .semantics {
                    heading()
                    contentDescription = text
                }
                .testTag("home_title")
        ) {
            when (effect) {
                TitleEffect.DISINTEGRATE -> drawDisintegrate(layout, color, accent, seed)
                TitleEffect.SPEED -> drawSpeed(layout, color, accent, seed, trailPx)
                TitleEffect.NONE -> drawText(layout, color = color)
            }
        }
    }
}

private fun DrawScope.drawDisintegrate(layout: TextLayoutResult, color: Color, accent: Color, seed: Int) {
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()
    val rnd = Random(seed)
    drawText(layout, color = color)

    // 1) En el último tercio se borran celdas finas, cada vez más hacia la derecha (máx. ~44 %):
    //    la palabra se sigue leyendo completa.
    val cell = 1.6.dp.toPx().coerceAtLeast(2f)
    val start = w * 0.72f
    var x = start
    while (x < w) {
        val t = ((x - start) / (w - start)).coerceIn(0f, 1f)
        val probability = 0.02f + 0.42f * t * t
        var y = 0f
        while (y < h) {
            if (rnd.nextFloat() < probability) {
                drawRect(Color.Black, Offset(x, y), Size(cell, cell), blendMode = BlendMode.Clear)
            }
            y += cell
        }
        x += cell
    }

    // 2) Desde el final de la palabra los pedazos se alejan hacia la derecha y algo hacia arriba:
    //    nítidos cerca de las letras, pequeños y tenues lejos.
    val baseline = layout.firstBaseline
    val glyphTop = baseline * 0.3f
    val originX = w * 0.97f
    val spread = (size.width - originX - cell).coerceAtLeast(0f).coerceAtMost(w * 0.32f + 80.dp.toPx())
    repeat(110) {
        val t = rnd.nextFloat().pow(1.7f)
        val px = originX + t * spread
        val drift = t * h * 0.35f * rnd.nextFloat()
        val py = glyphTop + rnd.nextFloat() * (baseline - glyphTop) - drift
        val side = cell * (1.3f - t * 0.8f).coerceAtLeast(0.5f)
        val alpha = (0.9f * (1f - t).pow(1.1f)).coerceIn(0.05f, 0.9f)
        val tint = if (rnd.nextFloat() < 0.22f) accent else color
        drawRect(tint.copy(alpha = alpha), Offset(px, py.coerceAtLeast(0f)), Size(side, side))
    }
}

/** Inclinación de las letras en el efecto de velocidad (desplazamiento horizontal por cada px de alto). */
private const val SPEED_SLANT = 0.2f

private fun DrawScope.drawSpeed(layout: TextLayoutResult, color: Color, accent: Color, seed: Int, trail: Float) {
    val w = layout.size.width.toFloat()
    val rnd = Random(seed)
    val baseline = layout.firstBaseline
    val glyphTop = baseline * 0.3f

    // 1) Letras inclinadas hacia adelante (como en movimiento), girando sobre la línea base.
    val canvas = drawContext.canvas
    canvas.save()
    canvas.translate(trail, baseline)
    canvas.skew(-SPEED_SLANT, 0f)
    canvas.translate(0f, -baseline)
    drawText(layout, color = color)
    canvas.restore()

    // 2) Dos cortes oscuros muy finos que atraviesan parte de la palabra.
    val cut = 1.2.dp.toPx()
    repeat(2) {
        val y = glyphTop + (0.25f + rnd.nextFloat() * 0.5f) * (baseline - glyphTop)
        val x0 = trail + w * (0.05f + rnd.nextFloat() * 0.25f)
        val x1 = trail + w * (0.55f + rnd.nextFloat() * 0.4f)
        drawRect(Color.Black, Offset(x0, y), Size(x1 - x0, cut), blendMode = BlendMode.Clear)
    }

    // 3) Líneas de velocidad que llegan desde la izquierda y terminan justo antes de la primera letra.
    val end = trail - 6.dp.toPx()
    repeat(12) {
        val y = glyphTop + rnd.nextFloat() * (baseline - glyphTop)
        val x0 = rnd.nextFloat() * end * 0.5f
        if (end - x0 < 6.dp.toPx()) return@repeat
        val tint = if (rnd.nextFloat() < 0.35f) accent else color
        val thickness = (0.8f + rnd.nextFloat() * 1.6f).dp.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(tint.copy(alpha = 0f), tint.copy(alpha = 0.7f)),
                startX = x0,
                endX = end
            ),
            topLeft = Offset(x0, y),
            size = Size(end - x0, thickness)
        )
    }
}
