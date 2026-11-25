package app.ordena.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Trazos de íconos de interfaz (viewBox 24x24, línea de 1.6). */
object UiIcons {
    const val SETTINGS = "M4 7h10M18 7h2M4 17h4M12 17h8M16 9a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM10 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4z"
    const val SEARCH = "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-3.5-3.5"
    const val BACK = "M15 6l-6 6 6 6"
    const val CHEVRON = "M9 6l6 6-6 6"
    const val CLOSE = "M6 6l12 12M18 6 6 18"
    const val PLUS = "M12 5v14M5 12h14"
    const val PHONE = "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z"
    const val MESSAGE = "M4 5h16v11H9l-5 4z"
    const val CAMERA = "M4 8h3l2-3h6l2 3h3v11H4zM12 16.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z"
    const val GLOBE = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"
    const val INFO = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 11v5M12 8h.01"
    const val HOME = "M3 11l9-7 9 7v9H3zM9 20v-6h6v6"
    const val LOCK = "M5 11h14v10H5zM8 11V8a4 4 0 0 1 8 0v3"
    const val UNDO = "M9 14L4 9l5-5M4 9h10a6 6 0 0 1 0 12h-3"
}

private val vectorCache = HashMap<String, ImageVector>()

/** Convierte un trazo SVG en un ícono de línea; se guarda en caché (solo se usa desde el hilo de UI). */
fun lineIconVector(path: String): ImageVector = vectorCache.getOrPut(path) {
    ImageVector.Builder(
        name = "line",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(path),
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

@Composable
fun LineIcon(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = Palette.Text
) {
    val vector = remember(path) { lineIconVector(path) }
    Icon(imageVector = vector, contentDescription = contentDescription, modifier = modifier.size(size), tint = tint)
}
