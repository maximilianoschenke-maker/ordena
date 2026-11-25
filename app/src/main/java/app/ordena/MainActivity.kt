package app.ordena

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import app.ordena.ui.DockAction
import app.ordena.ui.LauncherViewModel
import app.ordena.ui.LocalIconProvider
import app.ordena.ui.OrdenaApp
import app.ordena.ui.OrdenaTheme
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    /** Se emite cuando el usuario toca el botón de inicio estando ya en Ordena. */
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val isDefaultHome = mutableStateOf(false)

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultHome.value = HomeRole.isDefault(this)
        if (!isDefaultHome.value) {
            Toast.makeText(
                this,
                "Puedes elegirla después en Ajustes del teléfono › Apps › App de inicio",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        isDefaultHome.value = HomeRole.isDefault(this)
        setContent {
            OrdenaTheme {
                CompositionLocalProvider(LocalIconProvider provides viewModel) {
                    OrdenaApp(
                        viewModel = viewModel,
                        homePresses = homePresses,
                        isDefaultHome = isDefaultHome.value,
                        onRequestDefaultHome = { HomeRole.request(this, roleRequest) },
                        onDock = ::openDock
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isDefaultHome.value = HomeRole.isDefault(this)
        viewModel.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePresses.tryEmit(Unit)
        }
    }

    private fun openDock(action: DockAction) {
        val intent = when (action) {
            DockAction.PHONE -> Intent(Intent.ACTION_DIAL)
            DockAction.MESSAGES -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)
            DockAction.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            DockAction.BROWSER -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No hay una app para esto en tu teléfono", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "No se pudo abrir", Toast.LENGTH_SHORT).show()
        }
    }
}
