package app.ordena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ordena.data.InstalledApp

@Composable
fun FolderScreen(
    folder: Folder,
    monochrome: Boolean,
    sortByUsage: Boolean,
    onBack: () -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onLongPress: (InstalledApp) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(UiIcons.BACK, "Volver al inicio", onBack, Modifier.testTag("back"))
            Spacer(Modifier.width(14.dp))
            CategoryIconBox(folder.iconPath, size = 44.dp)
        }
        Spacer(Modifier.height(20.dp))
        ScreenTitle(folder.name, Modifier.testTag("folder_title"), size = 32)
        val count = folder.apps.size
        val order = if (sortByUsage) "las más usadas primero" else "en orden alfabético"
        Text(
            when (count) {
                0 -> "Carpeta vacía"
                1 -> "1 app"
                else -> "$count apps · $order"
            },
            color = Palette.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (count == 0) {
            Text(
                "Para llenarla, mantén presionada una app en otra carpeta y elige esta carpeta en «Mover a…».",
                color = Palette.TextDim,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            Text(
                "Mantén presionada una app para moverla.",
                color = Palette.TextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 78.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
                modifier = Modifier.fillMaxSize().testTag("folder_apps")
            ) {
                items(folder.apps, key = { it.key }) { app ->
                    AppCell(
                        app = app,
                        monochrome = monochrome,
                        onClick = { onLaunch(app) },
                        onLongClick = { onLongPress(app) }
                    )
                }
            }
        }
    }
}
