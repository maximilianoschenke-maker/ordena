package app.ordena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ordena.data.InstalledApp
import app.ordena.domain.CategoryCatalog
import app.ordena.domain.CategoryMatcher
import app.ordena.domain.Codec

/**
 * Configurar carpetas: escribir cualquier tema ("juegos", "bancos", "redes sociales"…) y BuenOrden
 * entiende a qué categoría se refiere, muestra qué apps irían ahí y la agrega.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    state: UiState,
    onBack: () -> Unit,
    onAddFromText: (String) -> AddResult,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    previewFolder: (String) -> List<InstalledApp>,
    onRenameFolder: (String, String) -> Unit,
    onMoveFolder: (String, Int) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }
    val customs = state.settings.customs
    val renamed = state.settings.folderNames
    val activeIds = state.settings.active
    val match = remember(text, customs, renamed) { CategoryMatcher.match(text, customs, renamed) }
    val hasText = Codec.cleanName(text).any { it.isLetterOrDigit() }
    val alreadyActive = match != null && match.id in activeIds
    val preview = remember(match?.id, state.refreshedAt) { match?.let { previewFolder(it.id) }.orEmpty() }

    fun submit() {
        if (!hasText || alreadyActive) return
        if (onAddFromText(text) !is AddResult.Invalid) text = ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("categories_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
    ) {
        item(key = "header") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(UiIcons.BACK, "Volver", onBack, Modifier.testTag("back"))
                }
                Spacer(Modifier.height(18.dp))
                ScreenTitle("Categorías")
                Text(
                    "Escribe cualquier tema y BuenOrden entiende qué apps van ahí.",
                    color = Palette.TextMuted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.height(18.dp))
                CategoryInput(
                    text = text,
                    onTextChange = { text = it },
                    buttonLabel = when {
                        alreadyActive -> "Ya está"
                        hasText && match == null -> "Crear"
                        else -> "Agregar"
                    },
                    buttonEnabled = hasText && !alreadyActive,
                    onSubmit = ::submit
                )
            }
        }

        item(key = "feedback") {
            Column {
                when {
                    match != null -> MatchCard(
                        kicker = if (alreadyActive) "Ya tienes esta carpeta" else "Entendí «${text.trim()}» como",
                        name = match.name,
                        iconPath = CategoryCatalog.byId[match.id]?.iconPath ?: CategoryCatalog.FOLDER_ICON,
                        detail = previewText(preview)
                    )
                    hasText -> UnknownCard(text.trim())
                    else -> Suggestions(activeIds, onAddCategory)
                }
            }
        }

        item(key = "list_header") {
            Column {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Tus carpetas", Modifier.weight(1f))
                    val visible = state.folders.count { it.id != CategoryCatalog.OTHER }
                    Text(
                        if (visible == 1) "1 carpeta" else "$visible carpetas",
                        color = Palette.TextMuted,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        val rows = state.folders.filter { it.id != CategoryCatalog.OTHER }
        itemsIndexed(rows, key = { _, folder -> "row_${folder.id}" }) { index, folder ->
            FolderConfigRow(
                folder = folder,
                canMoveUp = index > 0,
                canMoveDown = index < rows.lastIndex,
                onRename = { renamingId = folder.id },
                onMoveUp = { onMoveFolder(folder.id, -1) },
                onMoveDown = { onMoveFolder(folder.id, +1) },
                onRemove = { onRemoveCategory(folder.id) }
            )
        }

        item(key = "footer") {
            Column {
                Text(
                    "Si quitas una carpeta, sus apps pasan a la más parecida que tengas (por ejemplo, Mapas → Viajes) o a «Otras apps».",
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Spacer(Modifier.height(18.dp))
                PrimaryButton("Listo", onClick = onBack, modifier = Modifier.fillMaxWidth().testTag("categories_done"))
            }
        }
    }

    val renamingFolder = renamingId?.let { id -> state.folders.firstOrNull { it.id == id } }
    if (renamingFolder != null) {
        RenameDialog(
            current = renamingFolder.name,
            originalName = CategoryCatalog.byId[renamingFolder.id]?.name,
            onSave = {
                onRenameFolder(renamingFolder.id, it)
                renamingId = null
            },
            onDismiss = { renamingId = null }
        )
    }
}

private fun previewText(apps: List<InstalledApp>): String = when (apps.size) {
    0 -> "Por ahora ninguna de tus apps calza aquí. Puedes mover apps a mano."
    1 -> "1 app: ${apps[0].label}"
    else -> {
        val names = apps.take(3).joinToString(", ") { it.label }
        val rest = apps.size - 3
        "${apps.size} apps: $names" + if (rest > 0) " y $rest más" else ""
    }
}

@Composable
private fun CategoryInput(
    text: String,
    onTextChange: (String) -> Unit,
    buttonLabel: String,
    buttonEnabled: Boolean,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.Surface)
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(28.dp))
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = { onTextChange(it.take(40)) },
            singleLine = true,
            textStyle = TextStyle(color = Palette.Text, fontSize = 16.sp, fontFamily = LocalAppFont.current),
            cursorBrush = SolidColor(Palette.Text),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .testTag("category_input")
                .semantics { contentDescription = "Escribe una categoría" },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text("Ej.: juegos, bancos, redes sociales", color = Palette.TextDim, fontSize = 16.sp, maxLines = 1)
                    }
                    inner()
                }
            }
        )
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (buttonEnabled) LocalAccent.current else Palette.SurfaceHigher)
                .clickable(enabled = buttonEnabled, role = Role.Button, onClick = onSubmit)
                .testTag("category_add")
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                buttonLabel,
                color = if (buttonEnabled) Palette.OnAccent else Palette.TextDim,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MatchCard(kicker: String, name: String, iconPath: String, detail: String) {
    Spacer(Modifier.height(12.dp))
    Card(Modifier.testTag("match_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBox(iconPath, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(kicker, color = Palette.TextDim, fontSize = 12.sp)
                Text(
                    name,
                    color = Palette.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("match_name")
                )
                Text(detail, color = Palette.TextMuted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun UnknownCard(text: String) {
    Spacer(Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Surface)
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(22.dp))
            .testTag("unknown_card")
            .padding(16.dp)
    ) {
        Column {
            Text("No conozco «$text» todavía", color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Créala igual: quedará vacía y podrás mover apps a ella. BuenOrden pondrá ahí las apps cuyo nombre la mencione.",
                color = Palette.TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(activeIds: List<String>, onAdd: (String) -> Unit) {
    val suggestions = CategoryCatalog.suggestionOrder.filter { it !in activeIds }.distinct().take(10)
    if (suggestions.isEmpty()) return
    Spacer(Modifier.height(16.dp))
    SectionLabel("Sugeridas")
    Spacer(Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { id ->
            val def = CategoryCatalog.byId.getValue(id)
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(Palette.Surface)
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(19.dp))
                    .clickable(role = Role.Button, onClickLabel = "Agregar ${def.name}") { onAdd(id) }
                    .testTag("suggest_$id")
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ ${def.name}", color = Palette.Text, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FolderConfigRow(
    folder: Folder,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .testTag("config_${folder.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClickLabel = "Renombrar ${folder.name}", onClick = onRename)
                .testTag("rename_${folder.id}")
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBox(folder.iconPath, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    folder.name,
                    color = Palette.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val count = folder.apps.size
                Text(
                    when (count) {
                        0 -> if (folder.isCustom) "Vacía · mueve apps aquí" else "Sin apps (no se muestra)"
                        1 -> "1 app"
                        else -> "$count apps"
                    },
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        SmallIconButton(UiIcons.UP, "Subir ${folder.name}", enabled = canMoveUp, onClick = onMoveUp)
        SmallIconButton(UiIcons.DOWN, "Bajar ${folder.name}", enabled = canMoveDown, onClick = onMoveDown)
        SmallIconButton(UiIcons.CLOSE, "Quitar ${folder.name}", enabled = true, onClick = onRemove)
    }
}

@Composable
private fun SmallIconButton(path: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        LineIcon(path, contentDescription = null, size = 16.dp, tint = if (enabled) Palette.TextMuted else Palette.SurfaceHigher)
    }
}
