package app.ordena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ordena.data.InstalledApp
import app.ordena.domain.CategoryCatalog
import app.ordena.domain.Placement
import kotlinx.coroutines.launch

/** Hoja inferior para mover una app a otra carpeta (o crear una nueva). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoveSheet(
    app: InstalledApp,
    placement: Placement?,
    folders: List<Folder>,
    monochrome: Boolean,
    onMove: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onResetToAuto: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Cierra con animación y recién después avisa (evita que la hoja desaparezca de golpe).
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }

    val currentId = placement?.categoryId
    val currentName = folders.firstOrNull { it.id == currentId }?.name ?: CategoryCatalog.OTHER_NAME
    val targets = folders.filter { it.id != currentId && it.id != CategoryCatalog.OTHER }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Surface,
        contentColor = Palette.Text,
        scrimColor = Palette.Scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF333333))
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 20.dp)
                .testTag("move_sheet")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, size = 48.dp, monochrome = monochrome)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Mover «${app.label}» a…",
                        color = Palette.Text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    Text("Ahora está en $currentName", color = Palette.TextMuted, fontSize = 13.sp)
                }
            }
            if (placement != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Por qué: ${placement.reason.replaceFirstChar { it.lowercaseChar() }}.",
                    color = Palette.TextDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                targets.forEach { folder ->
                    Chip(folder.shortName, onClick = { closeThen { onMove(folder.id) } })
                }
                Chip("+ Nueva carpeta", onClick = { creating = true }, selected = creating)
            }

            if (creating) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Palette.Background)
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(26.dp))
                        .padding(start = 18.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canCreate = newName.isNotBlank()
                    BasicTextField(
                        value = newName,
                        onValueChange = { newName = it.take(30) },
                        singleLine = true,
                        textStyle = TextStyle(color = Palette.Text, fontSize = 16.sp, fontFamily = Geist),
                        cursorBrush = SolidColor(Palette.Text),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            if (canCreate) closeThen { onCreateFolder(newName) }
                        }),
                        modifier = Modifier.weight(1f).testTag("new_folder_input"),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (newName.isEmpty()) Text("Nombre de la carpeta", color = Palette.TextDim, fontSize = 16.sp)
                                inner()
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (canCreate) Palette.Accent else Palette.SurfaceHigher)
                            .clickable(enabled = canCreate, role = Role.Button) { closeThen { onCreateFolder(newName) } }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Crear",
                            color = if (canCreate) Palette.OnAccent else Palette.TextDim,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "Si escribes una categoría conocida, como «bancos», Ordena usa esa.",
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.BorderSoft))
            if (placement?.isUserChoice == true) {
                SheetAction(UiIcons.UNDO, "Dejar que Ordena elija la carpeta") { closeThen(onResetToAuto) }
            }
            SheetAction(UiIcons.INFO, "Información de la app") { closeThen(onAppInfo) }
        }
    }
}

@Composable
private fun SheetAction(path: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LineIcon(path, contentDescription = null, size = 20.dp, tint = Palette.TextMuted)
        Spacer(Modifier.width(14.dp))
        Text(label, color = Palette.Text, fontSize = 15.sp)
    }
}
