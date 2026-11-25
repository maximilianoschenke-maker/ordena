package app.ordena

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ordena.data.SettingsStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas de punta a punta en un emulador real: la app lee las apps instaladas del emulador
 * (Ajustes, Reloj, Contactos, etc.) y se recorre como lo haría una persona.
 */
@RunWith(AndroidJUnit4::class)
class LauncherUiTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private fun start(onboardingDone: Boolean = true, knownPackages: Set<String>? = null) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val editor = context.getSharedPreferences(SettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean(SettingsStore.KEY_ONBOARDING, onboardingDone)
        if (knownPackages != null) editor.putStringSet(SettingsStore.KEY_KNOWN, knownPackages)
        editor.commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val readyTag = if (onboardingDone) "folder_grid" else "onboarding"
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTag(readyTag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun openFolder(id: String) {
        compose.onNodeWithTag("folder_grid").performScrollToNode(hasTestTag("folder_$id"))
        compose.onNodeWithTag("folder_$id").performClick()
        compose.waitForIdle()
    }

    private fun typeCategory(text: String) {
        compose.onNodeWithTag("categories_list").performScrollToNode(hasTestTag("category_input"))
        compose.onNodeWithTag("category_input").performTextClearance()
        compose.onNodeWithTag("category_input").performTextInput(text)
        compose.waitForIdle()
    }

    @Test
    fun bienvenidaLlevaAlInicio() {
        start(onboardingDone = false)
        compose.onNodeWithTag("onboarding").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_start").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("folder_grid").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("folder_grid").assertIsDisplayed()
    }

    @Test
    fun inicioMuestraCarpetaHerramientas() {
        start()
        compose.onNodeWithTag("folder_grid").performScrollToNode(hasTestTag("folder_herramientas"))
        compose.onNodeWithTag("folder_herramientas").assertIsDisplayed()
    }

    @Test
    fun categoriasEntiendenLoQueSeEscribe() {
        start()
        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("open_categories").performClick()
        compose.waitForIdle()

        typeCategory("bancos")
        compose.onNodeWithTag("match_name").assertTextEquals("Bancos y finanzas")

        typeCategory("redes sociales")
        compose.onNodeWithTag("match_name").assertTextEquals("Redes sociales")

        typeCategory("juegos")
        compose.onNodeWithTag("match_name").assertTextEquals("Juegos")

        // Palabra cotidiana para una categoría que no viene activada: se entiende y se agrega.
        typeCategory("gym")
        compose.onNodeWithTag("match_name").assertTextEquals("Deporte y fitness")
        compose.onNodeWithTag("category_add").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("categories_list").performScrollToNode(hasTestTag("config_deporte"))
        compose.onNodeWithTag("config_deporte").assertIsDisplayed()

        // Algo que no conoce: ofrece crearla igual.
        typeCategory("zzqx")
        compose.onNodeWithTag("unknown_card").assertIsDisplayed()
    }

    @Test
    fun busquedaEntiendeCategoriasYApps() {
        start()
        compose.onNodeWithTag("search_input").performTextInput("herramientas")
        compose.waitForIdle()
        compose.onNodeWithTag("search_folder").assertIsDisplayed()

        compose.onNodeWithTag("search_input").performTextClearance()
        compose.onNodeWithTag("search_input").performTextInput("sett")
        compose.waitForIdle()
        compose.onAllNodesWithText("Settings").onFirst().assertIsDisplayed()
    }

    @Test
    fun avisaCuandoSeInstalaUnaAppNueva() {
        // BuenOrden "recuerda" solo una app: todas las demás le parecen recién instaladas.
        start(knownPackages = setOf("com.android.settings"))
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("new_app_banner").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("new_app_banner").assertIsDisplayed()
        compose.onNodeWithText("Listo").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("new_app_banner").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun moverUnaAppDeCarpeta() {
        start()
        openFolder("herramientas")
        compose.onNodeWithTag("folder_title").assertTextEquals("Herramientas")

        compose.onNodeWithTag("app_com.android.settings").performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("move_sheet").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Trabajo").performClick()

        // Ajustes sale de Herramientas…
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("app_com.android.settings").fetchSemanticsNodes().isEmpty()
        }
        // …y aparece en Trabajo y productividad.
        compose.onNodeWithTag("back").performClick()
        compose.waitForIdle()
        openFolder("productividad")
        compose.onNodeWithTag("app_com.android.settings").assertIsDisplayed()
    }

    @Test
    fun personalizarTipografiaTituloYEfecto() {
        start()
        // Título y efecto por defecto.
        compose.onNodeWithContentDescription("BuenOrden").assertIsDisplayed()

        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("open_personalize").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("personalize_preview").assertIsDisplayed()

        compose.onNodeWithTag("personalize_options").performScrollToNode(hasTestTag("font_exo2"))
        compose.onNodeWithTag("font_exo2").performClick()
        compose.onNodeWithTag("personalize_options").performScrollToNode(hasTestTag("title_input"))
        compose.onNodeWithTag("title_input").performTextClearance()
        compose.onNodeWithTag("title_input").performTextInput("Mi teléfono")
        compose.onNodeWithTag("personalize_options").performScrollToNode(hasTestTag("effect_velocidad"))
        compose.onNodeWithTag("effect_velocidad").performClick()
        compose.onNodeWithTag("personalize_options").performScrollToNode(hasTestTag("accent_menta"))
        compose.onNodeWithTag("accent_menta").performClick()
        compose.onNodeWithTag("personalize_options").performScrollToNode(hasTestTag("size_compacto"))
        compose.onNodeWithTag("size_compacto").performClick()
        compose.waitForIdle()

        // La vista previa ya muestra el título nuevo.
        compose.onNodeWithContentDescription("Mi teléfono").assertIsDisplayed()

        // Y el inicio también.
        compose.onNodeWithTag("back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("back").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Mi teléfono").assertIsDisplayed()
        compose.onNodeWithTag("folder_grid").assertIsDisplayed()
    }

    @Test
    fun renombrarYReordenarCarpetas() {
        start()
        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("open_categories").performClick()
        compose.waitForIdle()

        // Renombrar Herramientas.
        compose.onNodeWithTag("categories_list").performScrollToNode(hasTestTag("rename_herramientas"))
        compose.onNodeWithTag("rename_herramientas").performClick()
        compose.onNodeWithTag("rename_input").performTextClearance()
        compose.onNodeWithTag("rename_input").performTextInput("Utilidades")
        compose.onNodeWithTag("rename_save").performClick()
        compose.waitForIdle()

        // Bajar Mensajería: la primera carpeta pasa a ser Redes sociales.
        compose.onNodeWithTag("categories_list").performScrollToNode(hasTestTag("config_mensajeria"))
        compose.onNodeWithContentDescription("Bajar Mensajería").performClick()
        compose.waitForIdle()
        val configRows = SemanticsMatcher("fila de carpeta") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("config_") == true
        }
        // Se ordena por posición en pantalla (no por el orden interno de la lista).
        val order = compose.onAllNodes(configRows).fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
        assertEquals(listOf("config_social", "config_mensajeria"), order.take(2))

        // En el inicio, la carpeta aparece con su nombre nuevo.
        compose.onNodeWithTag("back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("folder_grid").performScrollToNode(hasTestTag("folder_herramientas"))
        compose.onNodeWithTag("folder_herramientas").assert(hasText("Utilidades"))
    }

    @Test
    fun ocultarYVolverAMostrarUnaApp() {
        start()
        openFolder("herramientas")
        compose.onNodeWithTag("app_com.android.settings").performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("move_sheet").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ocultar de las carpetas").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("app_com.android.settings").fetchSemanticsNodes().isEmpty()
        }

        // Se vuelve a mostrar desde Ajustes › Apps ocultas.
        compose.onNodeWithTag("back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("unhide_com.android.settings"))
        compose.onNodeWithTag("unhide_com.android.settings").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("back").performClick()
        compose.waitForIdle()
        openFolder("herramientas")
        compose.onNodeWithTag("app_com.android.settings").assertIsDisplayed()
    }
}
