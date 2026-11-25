package app.ordena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ordena.data.InstalledApp
import app.ordena.domain.CategoryMatcher
import app.ordena.domain.TextNormalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DockAction { PHONE, MESSAGES, CAMERA, BROWSER }

@Composable
fun HomeScreen(
    state: UiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onLongPress: (InstalledApp) -> Unit,
    onOpenSettings: () -> Unit,
    onDismissNewApp: () -> Unit,
    onChangeNewApp: (InstalledApp) -> Unit,
    onDock: (DockAction) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val dateText = remember(state.refreshedAt) { todayText() }
    val searching = query.isNotBlank()

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(dateText, color = Palette.TextMuted, fontSize = 13.sp)
                ScreenTitle("Inicio", Modifier.padding(top = 2.dp))
            }
            RoundIconButton(UiIcons.SETTINGS, "Ajustes", onOpenSettings, Modifier.testTag("open_settings"))
        }
        Spacer(Modifier.height(18.dp))
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = {
                searchApps(state.apps, query).firstOrNull()?.let(onLaunch)
                focusManager.clearFocus()
            }
        )

        val notice = state.newApp
        if (notice != null && !searching) {
            Spacer(Modifier.height(14.dp))
            NewAppBanner(notice, state.settings.monoIcons, onDismissNewApp, onChangeNewApp)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                searching -> SearchResults(state, query, onOpenFolder, onLaunch, onLongPress)
                state.loading -> Text(
                    "Ordenando tus apps…",
                    color = Palette.TextDim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 28.dp)
                )
                state.homeFolders.isEmpty() -> Text(
                    "No encontramos apps para mostrar.",
                    color = Palette.TextDim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 28.dp)
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp),
                    modifier = Modifier.fillMaxSize().testTag("folder_grid")
                ) {
                    items(state.homeFolders, key = { it.id }) { folder ->
                        FolderTile(folder, onClick = { onOpenFolder(folder.id) })
                    }
                }
            }
        }

        if (!searching) {
            Dock(onDock)
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun todayText(): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CL"))
    return LocalDate.now().format(formatter).replaceFirstChar { it.uppercaseChar() }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Border, RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LineIcon(UiIcons.SEARCH, contentDescription = null, size = 18.dp, tint = Palette.TextMuted)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Palette.Text, fontSize = 15.sp, fontFamily = Geist),
            cursorBrush = SolidColor(Palette.Text),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .testTag("search_input")
                .semantics { contentDescription = "Busca una app o una categoría" },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Busca una app o una categoría", color = Palette.TextDim, fontSize = 15.sp)
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Borrar búsqueda") { onQueryChange("") }
                    .semantics { contentDescription = "Borrar búsqueda" },
                contentAlignment = Alignment.Center
            ) {
                LineIcon(UiIcons.CLOSE, contentDescription = null, size = 16.dp, tint = Palette.TextMuted)
            }
        }
    }
}

/** Busca por nombre de app (sin tildes); primero las que empiezan con lo escrito. */
fun searchApps(apps: List<InstalledApp>, query: String): List<InstalledApp> {
    val q = TextNormalizer.normalize(query)
    if (q.isEmpty()) return emptyList()
    return apps
        .mapNotNull { app ->
            val label = TextNormalizer.normalize(app.label)
            val rank = when {
                label.startsWith(q) -> 0
                label.split(' ').any { it.startsWith(q) } -> 1
                label.contains(q) -> 2
                else -> return@mapNotNull null
            }
            rank to app
        }
        .sortedWith(compareBy<Pair<Int, InstalledApp>> { it.first }.thenBy { it.second.label.lowercase() })
        .map { it.second }
        .distinctBy { it.key }
        .take(40)
}

@Composable
private fun SearchResults(
    state: UiState,
    query: String,
    onOpenFolder: (String) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onLongPress: (InstalledApp) -> Unit
) {
    val match = remember(query, state.settings.customs) { CategoryMatcher.match(query, state.settings.customs) }
    val folder = match?.let { m -> state.folders.firstOrNull { it.id == m.id } }
    val results = remember(query, state.apps) { searchApps(state.apps, query) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("search_results"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (folder != null) {
            item(key = "folder") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Palette.Surface)
                            .border(1.dp, Color(0xFF232323), RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button) { onOpenFolder(folder.id) }
                            .testTag("search_folder")
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconBox(folder.iconPath, size = 40.dp, filled = true)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Carpeta", color = Palette.TextDim, fontSize = 12.sp)
                            Text(folder.name, color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            if (folder.apps.size == 1) "1 app" else "${folder.apps.size} apps",
                            color = Palette.TextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        LineIcon(UiIcons.CHEVRON, contentDescription = null, size = 16.dp, tint = Palette.TextDim)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        items(results, key = { it.key }) { app ->
            val placement = state.placements[app.key]
            val folderName = state.folders.firstOrNull { it.id == placement?.categoryId }?.name.orEmpty()
            AppRow(
                app = app,
                subtitle = folderName,
                monochrome = state.settings.monoIcons,
                onClick = { onLaunch(app) },
                onLongClick = { onLongPress(app) }
            )
        }
        if (folder == null && results.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Nada por aquí. Prueba con otra palabra, como «bancos» o «juegos».",
                    color = Palette.TextDim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun NewAppBanner(
    notice: NewAppNotice,
    monochrome: Boolean,
    onDismiss: () -> Unit,
    onChange: (InstalledApp) -> Unit
) {
    Card(Modifier.testTag("new_app_banner")) {
        Column {
            SectionLabel("App nueva ordenada")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(notice.app, size = 44.dp, monochrome = monochrome)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(notice.app.label, color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Se guardó en ${notice.folderName}", color = Palette.TextMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            val extra = if (notice.othersCount > 0) {
                " · y ${notice.othersCount} app${if (notice.othersCount == 1) "" else "s"} más"
            } else {
                ""
            }
            Text(
                "Por qué: ${notice.reason.replaceFirstChar { it.lowercaseChar() }}$extra",
                color = Palette.TextDim,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Cambiar", onClick = { onChange(notice.app) }, modifier = Modifier.weight(1f))
                PrimaryButton("Listo", onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp))
            }
        }
    }
}

@Composable
private fun Dock(onDock: (DockAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.Surface)
            .border(1.dp, Color(0xFF232323), RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DockButton(UiIcons.PHONE, "Teléfono") { onDock(DockAction.PHONE) }
        DockButton(UiIcons.MESSAGE, "Mensajes") { onDock(DockAction.MESSAGES) }
        DockButton(UiIcons.CAMERA, "Cámara") { onDock(DockAction.CAMERA) }
        DockButton(UiIcons.GLOBE, "Navegador") { onDock(DockAction.BROWSER) }
    }
}

@Composable
private fun DockButton(path: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F1F1F))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        LineIcon(path, contentDescription = null, size = 22.dp)
    }
}
