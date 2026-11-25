package app.ordena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ordena.BuildConfig
import app.ordena.domain.CategoryCatalog

@Composable
fun SettingsScreen(
    state: UiState,
    isDefaultHome: Boolean,
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onSetNotify: (Boolean) -> Unit,
    onSetMono: (Boolean) -> Unit,
    onSetSortByUsage: (Boolean) -> Unit,
    onResetApp: (String) -> Unit,
    onResetAll: () -> Unit,
    folderName: (String) -> String
) {
    val s = state.settings
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(UiIcons.BACK, "Volver al inicio", onBack, Modifier.testTag("back"))
                Spacer(Modifier.width(12.dp))
                ScreenTitle("Ajustes", size = 26)
            }
        }

        if (!isDefaultHome) {
            item(key = "default_home") {
                Column {
                    Spacer(Modifier.height(22.dp))
                    Card {
                        Column {
                            Text(
                                "Ordena aún no es tu pantalla de inicio",
                                color = Palette.Text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Elígela como app de inicio para que el botón de inicio te traiga aquí.",
                                color = Palette.TextMuted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                            )
                            PrimaryButton(
                                "Usar como pantalla de inicio",
                                onClick = onRequestDefaultHome,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        item(key = "categories") {
            Column {
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Palette.Surface)
                        .clickable(role = Role.Button, onClick = onOpenCategories)
                        .testTag("open_categories")
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIconBox(CategoryCatalog.FOLDER_ICON, size = 40.dp, filled = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Categorías", color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${s.active.size} activas · escribe «juegos», «bancos»…",
                            color = Palette.TextDim,
                            fontSize = 12.sp
                        )
                    }
                    LineIcon(UiIcons.CHEVRON, contentDescription = null, size = 18.dp, tint = Palette.TextDim)
                }
            }
        }

        item(key = "auto") {
            Column {
                SectionLabel("Orden automático", Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp))
                Card(padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                    ToggleRow(
                        title = "Avisar cuando ordene una app nueva",
                        subtitle = "Te muestra dónde quedó y por qué",
                        checked = s.notifyNewApps,
                        onCheckedChange = onSetNotify,
                        showDivider = false
                    )
                }
            }
        }

        item(key = "look") {
            Column {
                SectionLabel("Apariencia", Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp))
                Card(padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                    Column {
                        ToggleRow(
                            title = "Íconos monocromáticos",
                            subtitle = "Todas las apps en blanco y negro",
                            checked = s.monoIcons,
                            onCheckedChange = onSetMono,
                            showDivider = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Orden en carpetas",
                                color = Palette.Text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Segmented(
                                options = listOf("Más usadas", "A–Z"),
                                selectedIndex = if (s.sortByUsage) 0 else 1,
                                onSelect = { onSetSortByUsage(it == 0) }
                            )
                        }
                    }
                }
            }
        }

        item(key = "rules") {
            Column {
                SectionLabel("Apps que moviste", Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp))
                Card(padding = PaddingValues(start = 16.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)) {
                    if (s.overrides.isEmpty()) {
                        Text(
                            "Aún no mueves ninguna app. Mantén presionada una app para cambiarla de carpeta.",
                            color = Palette.TextDim,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    } else {
                        Column {
                            val labels = state.apps.associate { it.packageName to it.label }
                            s.overrides.entries.forEachIndexed { index, (pkg, folderId) ->
                                OverrideRow(
                                    appLabel = labels[pkg] ?: pkg,
                                    folderLabel = folderName(folderId),
                                    onRemove = { onResetApp(pkg) }
                                )
                                if (index < s.overrides.size - 1) {
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "reset") {
            Column {
                Spacer(Modifier.height(28.dp))
                SecondaryButton("Restablecer todo", onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth())
                Text(
                    "Ordena ${BuildConfig.VERSION_NAME} · Todo se clasifica en tu teléfono, sin internet.",
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp, start = 4.dp)
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Palette.SurfaceHigh,
            titleContentColor = Palette.Text,
            textContentColor = Palette.TextMuted,
            title = { Text("¿Restablecer Ordena?") },
            text = { Text("Vuelven las carpetas iniciales y se olvidan las apps que moviste a mano.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetAll()
                }) { Text("Restablecer", color = Palette.Text, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancelar", color = Palette.TextMuted) }
            }
        )
    }
}

@Composable
private fun OverrideRow(appLabel: String, folderLabel: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$appLabel → $folderLabel",
            color = Palette.Text,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Olvidar", onClick = onRemove)
                .semantics { contentDescription = "Dejar que Ordena elija la carpeta de $appLabel" },
            contentAlignment = Alignment.Center
        ) {
            LineIcon(UiIcons.CLOSE, contentDescription = null, size = 16.dp, tint = Palette.TextDim)
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
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
                    .clickable(role = Role.RadioButton, onClickLabel = label) { onSelect(index) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Palette.Text else Palette.TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}
