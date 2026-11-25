package app.ordena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    state: UiState,
    isDefaultHome: Boolean,
    onRequestDefaultHome: () -> Unit,
    onReviewCategories: () -> Unit,
    onStart: () -> Unit
) {
    val folders = state.homeFolders
    val appCount = state.apps.size
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .testTag("onboarding")
    ) {
        Spacer(Modifier.height(24.dp))
        BrandMark()
        Spacer(Modifier.height(36.dp))
        ScreenTitle("Tus apps,\nya ordenadas")
        Text(
            "Encontramos $appCount apps y las pusimos en ${folders.size} carpetas según su tema. " +
                "Todo ocurrió en tu teléfono, sin internet.",
            color = Palette.TextMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Spacer(Modifier.height(24.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            folders.take(12).forEach { folder ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Palette.Surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LineIcon(folder.iconPath, contentDescription = null, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("${folder.shortName} · ${folder.apps.size}", color = Palette.Text, fontSize = 13.sp)
                }
            }
        }

        if (!isDefaultHome) {
            Spacer(Modifier.height(24.dp))
            Card {
                Column {
                    Text(
                        "Úsala como pantalla de inicio",
                        color = Palette.Text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Así el botón de inicio te trae a tus carpetas. Puedes volver a tu pantalla anterior cuando quieras desde Ajustes del teléfono.",
                        color = Palette.TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )
                    SecondaryButton(
                        "Elegir BuenOrden como inicio",
                        onClick = onRequestDefaultHome,
                        modifier = Modifier.fillMaxWidth().testTag("set_default_home")
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        PrimaryButton("Empezar", onClick = onStart, modifier = Modifier.fillMaxWidth().testTag("onboarding_start"))
        Spacer(Modifier.height(10.dp))
        SecondaryButton(
            "Revisar categorías",
            onClick = onReviewCategories,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_categories")
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(LocalAccent.current)
                .padding(7.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Dot(1f)
                    Dot(0.45f)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Dot(0.45f)
                    Dot(1f)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("BuenOrden", color = Palette.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Dot(alpha: Float) {
    Box(
        Modifier
            .size(6.5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF0A0A0A).copy(alpha = alpha))
    )
}
