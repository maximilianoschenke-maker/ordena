package app.ordena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Diálogo para renombrar una carpeta.
 * @param originalName nombre de fábrica (solo carpetas de BuenOrden): si se ofrece, permite volver a él.
 * @param onSave recibe el nombre nuevo; "" significa volver al nombre original.
 */
@Composable
fun RenameDialog(current: String, originalName: String?, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    val canSave = text.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        titleContentColor = Palette.Text,
        textContentColor = Palette.TextMuted,
        title = { Text("Renombrar carpeta") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.Background)
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.take(30) },
                        singleLine = true,
                        textStyle = TextStyle(color = Palette.Text, fontSize = 16.sp, fontFamily = LocalAppFont.current),
                        cursorBrush = SolidColor(Palette.Text),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(text) }),
                        modifier = Modifier.fillMaxWidth().testTag("rename_input")
                    )
                }
                if (originalName != null && current != originalName) {
                    TextButton(onClick = { onSave("") }, modifier = Modifier.padding(top = 6.dp)) {
                        Text("Volver a «$originalName»", color = Palette.TextMuted)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = canSave, modifier = Modifier.testTag("rename_save")) {
                Text(
                    "Guardar",
                    color = if (canSave) LocalAccent.current else Palette.TextDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Palette.TextMuted) }
        }
    )
}
