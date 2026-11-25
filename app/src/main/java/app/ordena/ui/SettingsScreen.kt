package app.ordena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onOpenPersonalize: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onSetNotify: (Boolean) -> Unit,
    onResetApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onResetAll: () -> Unit,
    folderName: (String) -> String
) {
    val s = state.settings
    var confirmReset by remember { mutableStateOf(false) }
    val labels = remember(state.apps) { state.apps.associate { it.packageName to it.label } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
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
                                "BuenOrden aún no es tu pantalla de inicio",
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

        item(key = "nav") {
            Column {
                Spacer(Modifier.height(22.dp))
                NavRow(
                    iconPath = CategoryCatalog.FOLDER_ICON,
                    title = "Categorías",
                    subtitle = "${s.active.size} carpetas · agrega, renombra y ordena",
                    tag = "open_categories",
                    onClick = onOpenCategories
                )
                Spacer(Modifier.height(10.dp))
                NavRow(
                    iconPath = UiIcons.BRUSH,
                    title = "Personalizar",
                    subtitle = "Tipografía, título, colores y tamaño",
                    tag = "open_personalize",
                    onClick = onOpenPersonalize
                )
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

        item(key = "rules") {
            Column {
                SectionLabel("Apps que moviste", Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp))
                Card(padding = PaddingValues(start = 16.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)) {
                    if (s.overrides.isEmpty()) {
                        EmptyNote("Aún no mueves ninguna app. Mantén presionada una app para cambiarla de carpeta.")
                    } else {
                        Column {
                            s.overrides.entries.forEachIndexed { index, (pkg, folderId) ->
                                RemovableRow(
                                    text = "${labels[pkg] ?: pkg} → ${folderName(folderId)}",
                                    actionLabel = "Dejar que BuenOrden elija la carpeta de ${labels[pkg] ?: pkg}",
                                    iconPath = UiIcons.CLOSE,
                                    onClick = { onResetApp(pkg) }
                                )
                                if (index < s.overrides.size - 1) Divider()
                            }
                        }
                    }
                }
            }
        }

        item(key = "hidden") {
            Column {
                SectionLabel("Apps ocultas", Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp))
                Card(padding = PaddingValues(start = 16.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)) {
                    if (s.hiddenPackages.isEmpty()) {
                        EmptyNote("No hay apps ocultas. Mantén presionada una app y elige «Ocultar de las carpetas».")
                    } else {
                        Column {
                            val hidden = s.hiddenPackages.sortedBy { (labels[it] ?: it).lowercase() }
                            hidden.forEachIndexed { index, pkg ->
                                RemovableRow(
                                    text = labels[pkg] ?: pkg,
                                    actionLabel = "Mostrar ${labels[pkg] ?: pkg} en las carpetas",
                                    iconPath = UiIcons.EYE,
                                    onClick = { onUnhideApp(pkg) },
                                    tag = "unhide_$pkg"
                                )
                                if (index < hidden.size - 1) Divider()
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
                    "BuenOrden ${BuildConfig.VERSION_NAME} · Todo se clasifica en tu teléfono, sin internet.",
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
            title = { Text("¿Restablecer BuenOrden?") },
            text = {
                Text("Vuelven las carpetas y la apariencia iniciales, y se olvidan las apps que moviste u ocultaste.")
            },
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
private fun NavRow(iconPath: String, title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBox(iconPath, size = 40.dp, filled = true)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Palette.TextDim, fontSize = 12.sp)
        }
        LineIcon(UiIcons.CHEVRON, contentDescription = null, size = 18.dp, tint = Palette.TextDim)
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        color = Palette.TextDim,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(vertical = 14.dp)
    )
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))
}

@Composable
private fun RemovableRow(text: String, actionLabel: String, iconPath: String, onClick: () -> Unit, tag: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Palette.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick)
                .semantics { contentDescription = actionLabel }
                .then(if (tag != null) Modifier.testTag(tag) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            LineIcon(iconPath, contentDescription = null, size = 16.dp, tint = Palette.TextMuted)
        }
    }
}
