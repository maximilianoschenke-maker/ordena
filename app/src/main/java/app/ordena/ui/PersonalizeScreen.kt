package app.ordena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ordena.data.AppearanceDefaults
import app.ordena.data.FolderSize
import app.ordena.data.TitleEffect

/**
 * Personalizar la apariencia, con una vista previa fija arriba que cambia al instante.
 * Todo se guarda apenas se elige.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizeScreen(
    state: UiState,
    onBack: () -> Unit,
    onSetFont: (String) -> Unit,
    onSetAccent: (String) -> Unit,
    onSetTitle: (String) -> Unit,
    onSetEffect: (TitleEffect) -> Unit,
    onSetFolderSize: (FolderSize) -> Unit,
    onSetShowDate: (Boolean) -> Unit,
    onSetShowDock: (Boolean) -> Unit,
    onSetMono: (Boolean) -> Unit,
    onSetSortByUsage: (Boolean) -> Unit,
    onResetAppearance: () -> Unit
) {
    val s = state.settings
    var titleDraft by rememberSaveable { mutableStateOf(s.titleText) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(UiIcons.BACK, "Volver", onBack, Modifier.testTag("back"))
            Spacer(Modifier.width(12.dp))
            ScreenTitle("Personalizar", size = 26)
        }
        Spacer(Modifier.height(16.dp))
        Preview(state)

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .testTag("personalize_options")
        ) {
            Section("Tipografía")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFonts.all.forEach { font ->
                    OptionChip(
                        selected = font.id == s.fontId,
                        onClick = { onSetFont(font.id) },
                        tag = "font_${font.id}"
                    ) { color ->
                        Column {
                            Text(font.label, color = color, fontFamily = font.family, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(font.description, color = color.copy(alpha = 0.7f), fontFamily = font.family, fontSize = 11.sp)
                        }
                    }
                }
            }

            Section("Texto del título")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Palette.Surface)
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(26.dp))
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = titleDraft,
                    onValueChange = {
                        titleDraft = it.take(AppearanceDefaults.TITLE_MAX_LENGTH)
                        // Un título vacío no se guarda (volvería a "BuenOrden" mientras se escribe).
                        if (titleDraft.isNotBlank()) onSetTitle(titleDraft)
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = Palette.Text, fontSize = 16.sp, fontFamily = LocalAppFont.current),
                    cursorBrush = SolidColor(Palette.Text),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("title_input")
                        .semantics { contentDescription = "Texto del título" }
                )
                Text("${titleDraft.length}/${AppearanceDefaults.TITLE_MAX_LENGTH}", color = Palette.TextDim, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppearanceDefaults.TITLE_PRESETS.forEach { preset ->
                    Chip(
                        preset,
                        onClick = {
                            titleDraft = preset
                            onSetTitle(preset)
                        },
                        selected = s.titleText == preset
                    )
                }
            }

            Section("Efecto del título")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TitleEffect.entries.forEach { effect ->
                    Chip(
                        effect.label,
                        onClick = { onSetEffect(effect) },
                        selected = s.titleEffect == effect,
                        modifier = Modifier.testTag("effect_${effect.id}")
                    )
                }
            }
            Text(
                "Es un dibujo fijo, no se mueve ni gasta batería.",
                color = Palette.TextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Section("Color de acento")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Accents.all.forEach { accent ->
                    val selected = accent.id == s.accentId
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .selectable(selected = selected, role = Role.RadioButton) { onSetAccent(accent.id) }
                            .testTag("accent_${accent.id}")
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .border(2.dp, if (selected) Palette.Text else Color.Transparent, CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(accent.color)
                        )
                        Text(
                            accent.label,
                            color = if (selected) Palette.Text else Palette.TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Section("Carpetas")
            Card(padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                Column {
                    LabeledRow("Tamaño") {
                        Segmented(
                            options = FolderSize.entries.map { it.label },
                            selectedIndex = FolderSize.entries.indexOf(s.folderSize),
                            onSelect = { onSetFolderSize(FolderSize.entries[it]) },
                            tags = FolderSize.entries.map { "size_${it.id}" }
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))
                    LabeledRow("Orden de las apps") {
                        Segmented(
                            options = listOf("Más usadas", "A–Z"),
                            selectedIndex = if (s.sortByUsage) 0 else 1,
                            onSelect = { onSetSortByUsage(it == 0) }
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))
                    ToggleRow(
                        title = "Íconos monocromáticos",
                        subtitle = "Todas las apps en blanco y negro",
                        checked = s.monoIcons,
                        onCheckedChange = onSetMono,
                        showDivider = false
                    )
                }
            }

            Section("Pantalla de inicio")
            Card(padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                Column {
                    ToggleRow(
                        title = "Mostrar la fecha",
                        subtitle = null,
                        checked = s.showDate,
                        onCheckedChange = onSetShowDate,
                        showDivider = true
                    )
                    ToggleRow(
                        title = "Mostrar la barra de accesos",
                        subtitle = "Teléfono, mensajes, cámara y navegador",
                        checked = s.showDock,
                        onCheckedChange = onSetShowDock,
                        showDivider = false
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SecondaryButton(
                "Restaurar apariencia original",
                onClick = {
                    titleDraft = AppearanceDefaults.TITLE
                    onResetAppearance()
                },
                modifier = Modifier.fillMaxWidth().testTag("reset_appearance")
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Miniatura del inicio con la configuración actual. */
@Composable
private fun Preview(state: UiState) {
    val s = state.settings
    val spec = tileSpec(s.folderSize)
    val columns = when (s.folderSize) {
        FolderSize.COMPACT -> 4
        FolderSize.NORMAL -> 3
        FolderSize.LARGE -> 2
    }
    Card(Modifier.testTag("personalize_preview")) {
        Column {
            if (s.showDate) Text("Vista previa", color = Palette.TextMuted, fontSize = 12.sp)
            EffectTitle(text = s.titleText, effect = s.titleEffect, fontSize = 30.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.homeFolders.take(columns).forEach { folder ->
                    FolderTile(folder, onClick = {}, size = s.folderSize, modifier = Modifier.weight(1f))
                }
                // Si hay pocas carpetas, se rellena para que los recuadros mantengan su ancho.
                repeat((columns - state.homeFolders.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f).height(spec.height))
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Section(title: String) {
    SectionLabel(title, Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp))
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        content()
    }
}

/** Opción seleccionable con contenido propio (p. ej. el nombre de una tipografía escrito en ella). */
@Composable
private fun OptionChip(selected: Boolean, onClick: () -> Unit, tag: String, content: @Composable (Color) -> Unit) {
    val accent = LocalAccent.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent else Palette.Surface)
            .border(1.dp, if (selected) accent else Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        content(if (selected) Palette.OnAccent else Palette.Text)
    }
}
