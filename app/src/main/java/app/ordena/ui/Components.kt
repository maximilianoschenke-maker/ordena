package app.ordena.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ordena.data.FolderSize
import app.ordena.data.InstalledApp

val LocalIconProvider = compositionLocalOf<IconProvider?> { null }

private val MonochromeFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun AppIcon(app: InstalledApp, size: Dp, monochrome: Boolean, modifier: Modifier = Modifier) {
    val provider = LocalIconProvider.current
    val bitmap by produceState(initialValue = provider?.cachedIcon(app), app.key, provider) {
        if (value == null && provider != null) value = provider.loadIcon(app)
    }
    Box(modifier.size(size)) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (monochrome) MonochromeFilter else null
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(size * 0.3f))
                    .background(Palette.SurfaceHigher)
            )
        }
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, size: Int = 34) {
    Text(
        text = text,
        modifier = modifier,
        color = Palette.Text,
        fontSize = size.sp,
        lineHeight = (size + 6).sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1).sp
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = Palette.TextDim,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp
    )
}

@Composable
fun RoundIconButton(path: String, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Border, CircleShape)
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        LineIcon(path, contentDescription = null, size = 20.dp)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(if (enabled) LocalAccent.current else Palette.SurfaceHigher)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Palette.OnAccent else Palette.TextDim,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(26.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) LocalAccent.current else Palette.SurfaceHigh)
            .border(1.dp, if (selected) LocalAccent.current else Color(0xFF2E2E2E), RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Palette.OnAccent else Palette.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Surface)
            .border(1.dp, Color(0xFF232323), RoundedCornerShape(22.dp))
            .padding(padding)
    ) { content() }
}

@Composable
fun CategoryIconBox(path: String, size: Dp = 36.dp, filled: Boolean = false) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(if (filled) LocalAccent.current else Palette.SurfaceHigher),
        contentAlignment = Alignment.Center
    ) {
        LineIcon(path, contentDescription = null, size = size * 0.5f, tint = if (filled) Palette.OnAccent else Palette.Text)
    }
}

/** Medidas de los recuadros de carpeta según el tamaño elegido en Personalizar. */
data class TileSpec(
    val minCellWidth: Dp,
    val height: Dp,
    val icon: Dp,
    val nameSize: TextUnit,
    val nameLineHeight: TextUnit,
    val countSize: TextUnit,
    val padding: Dp,
    val corner: Dp
)

fun tileSpec(size: FolderSize): TileSpec = when (size) {
    FolderSize.COMPACT -> TileSpec(76.dp, 88.dp, 20.dp, 12.sp, 15.sp, 11.sp, 10.dp, 18.dp)
    FolderSize.NORMAL -> TileSpec(100.dp, 104.dp, 22.dp, 13.sp, 16.sp, 12.sp, 12.dp, 22.dp)
    FolderSize.LARGE -> TileSpec(150.dp, 124.dp, 26.dp, 15.sp, 19.sp, 13.sp, 14.dp, 24.dp)
}

@Composable
fun FolderTile(folder: Folder, onClick: () -> Unit, modifier: Modifier = Modifier, size: FolderSize = FolderSize.NORMAL) {
    val spec = tileSpec(size)
    val count = folder.apps.size
    val countLabel = when (count) {
        0 -> "Vacía"
        1 -> "1 app"
        else -> "$count apps"
    }
    // Con letra grande del sistema el recuadro crece (todos por igual, para que la grilla quede pareja).
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val shape = RoundedCornerShape(spec.corner)
    Column(
        modifier = modifier
            .height(spec.height * fontScale)
            .clip(shape)
            .background(Palette.Surface)
            .border(1.dp, Color(0xFF232323), shape)
            .clickable(onClickLabel = "Abrir ${folder.name}", role = Role.Button, onClick = onClick)
            .testTag("folder_${folder.id}")
            .padding(spec.padding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LineIcon(folder.iconPath, contentDescription = null, size = spec.icon)
        Column {
            Text(
                folder.shortName,
                color = Palette.Text,
                fontSize = spec.nameSize,
                lineHeight = spec.nameLineHeight,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(countLabel, color = Palette.TextDim, fontSize = spec.countSize, lineHeight = spec.countSize * 1.25f)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCell(app: InstalledApp, monochrome: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClickLabel = "Abrir",
                onLongClickLabel = "Mover a otra carpeta",
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("app_${app.packageName}")
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(app, size = 54.dp, monochrome = monochrome)
        Spacer(Modifier.height(7.dp))
        Text(
            app.label,
            color = Palette.TextMuted,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(app: InstalledApp, subtitle: String, monochrome: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClickLabel = "Abrir",
                onLongClickLabel = "Mover a otra carpeta",
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app, size = 44.dp, monochrome = monochrome)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Palette.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = Palette.TextDim, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Palette.OnAccent,
                    checkedTrackColor = LocalAccent.current,
                    checkedBorderColor = LocalAccent.current,
                    uncheckedThumbColor = Palette.TextDim,
                    uncheckedTrackColor = Color(0xFF2E2E2E),
                    uncheckedBorderColor = Color(0xFF2E2E2E)
                )
            )
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))
    }
}

/** Selector de opciones excluyentes (como "Más usadas / A–Z"). */
@Composable
fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, tags: List<String>? = null) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Background)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) Palette.SurfaceHigher else Palette.Background)
                    .selectable(selected = selected, role = Role.RadioButton) { onSelect(index) }
                    .then(if (tags != null) Modifier.testTag(tags[index]) else Modifier)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Palette.Text else Palette.TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
