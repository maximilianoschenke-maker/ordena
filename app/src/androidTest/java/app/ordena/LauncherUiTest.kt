package app.ordena

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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

    private fun start(onboardingDone: Boolean = true) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences(SettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean(SettingsStore.KEY_ONBOARDING, onboardingDone)
            .commit()
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
}
