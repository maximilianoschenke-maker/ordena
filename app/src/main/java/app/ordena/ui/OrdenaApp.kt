package app.ordena.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ordena.data.InstalledApp
import app.ordena.domain.CategoryCatalog
import kotlinx.coroutines.flow.Flow

/** Rutas: "home", "settings", "categories" o "folder:<id>". */
private const val HOME = "home"
private const val SETTINGS = "settings"
private const val CATEGORIES = "categories"
private const val PERSONALIZE = "personalize"
private const val FOLDER_PREFIX = "folder:"

private val BackStackSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

@Composable
fun OrdenaApp(
    viewModel: LauncherViewModel,
    homePresses: Flow<Unit>,
    isDefaultHome: Boolean,
    onRequestDefaultHome: () -> Unit,
    onDock: (DockAction) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val backStack = rememberSaveable(saver = BackStackSaver) { mutableListOf(HOME).toMutableStateList() }
    var query by rememberSaveable { mutableStateOf("") }
    var moveKey by rememberSaveable { mutableStateOf<String?>(null) }

    fun push(route: String) {
        focusManager.clearFocus()
        backStack.add(route)
    }

    fun pop() {
        focusManager.clearFocus()
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun goHome() {
        focusManager.clearFocus()
        backStack.clear()
        backStack.add(HOME)
        query = ""
        moveKey = null
    }

    LaunchedEffect(homePresses) { homePresses.collect { goHome() } }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val current = backStack.lastOrNull() ?: HOME
    // En el inicio el botón atrás no hace nada (como cualquier launcher); en otras pantallas, vuelve.
    BackHandler(enabled = true) {
        when {
            moveKey != null -> moveKey = null
            current == HOME && query.isNotEmpty() -> query = ""
            current != HOME -> pop()
        }
    }

    val launch: (InstalledApp) -> Unit = { app ->
        viewModel.launch(app)
        // Al volver, el inicio queda limpio.
        query = ""
        focusManager.clearFocus()
    }
    val openMove: (InstalledApp) -> Unit = { app -> moveKey = app.key }

    OrdenaTheme(fontId = state.settings.fontId, accentId = state.settings.accentId) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Palette.Background)
                .safeDrawingPadding()
        ) {
            if (!state.loading && !state.settings.onboardingDone) {
                OnboardingScreen(
                    state = state,
                    isDefaultHome = isDefaultHome,
                    onRequestDefaultHome = onRequestDefaultHome,
                    onReviewCategories = {
                        viewModel.completeOnboarding()
                        goHome()
                        push(CATEGORIES)
                    },
                    onStart = {
                        viewModel.completeOnboarding()
                        goHome()
                    }
                )
            } else {
                MainContent(
                    state = state,
                    current = current,
                    query = query,
                    onQueryChange = { query = it },
                    isDefaultHome = isDefaultHome,
                    viewModel = viewModel,
                    push = ::push,
                    pop = ::pop,
                    isTop = { it == backStack.lastOrNull() },
                    launch = launch,
                    openMove = openMove,
                    onRequestDefaultHome = onRequestDefaultHome,
                    onDock = onDock
                )
            }

            val moving = moveKey?.let { key -> state.apps.firstOrNull { it.key == key } }
            if (moving != null) {
                MoveSheet(
                    app = moving,
                    placement = state.placements[moving.key],
                    folders = state.folders,
                    monochrome = state.settings.monoIcons,
                    onMove = { viewModel.moveApp(moving, it) },
                    onCreateFolder = { viewModel.createFolderAndMove(moving, it) },
                    onResetToAuto = { viewModel.resetApp(moving.packageName) },
                    onAppInfo = { viewModel.openAppInfo(moving) },
                    onDismiss = { moveKey = null },
                    isHidden = moving.packageName in state.settings.hiddenPackages,
                    onToggleHidden = {
                        if (moving.packageName in state.settings.hiddenPackages) {
                            viewModel.unhideApp(moving.packageName)
                        } else {
                            viewModel.hideApp(moving)
                        }
                    }
                )
            } else if (moveKey != null && !state.loading) {
                // La app se desinstaló mientras la hoja estaba abierta.
                LaunchedEffect(moveKey) { moveKey = null }
            }
        }
    }
}

@Composable
private fun MainContent(
    state: UiState,
    current: String,
    query: String,
    onQueryChange: (String) -> Unit,
    isDefaultHome: Boolean,
    viewModel: LauncherViewModel,
    push: (String) -> Unit,
    pop: () -> Unit,
    isTop: (String) -> Boolean,
    launch: (InstalledApp) -> Unit,
    openMove: (InstalledApp) -> Unit,
    onRequestDefaultHome: () -> Unit,
    onDock: (DockAction) -> Unit
) {
    Crossfade(targetState = current, animationSpec = tween(180), label = "screen") { route ->
        when {
            route == SETTINGS -> SettingsScreen(
                state = state,
                isDefaultHome = isDefaultHome,
                onBack = pop,
                onOpenCategories = { push(CATEGORIES) },
                onOpenPersonalize = { push(PERSONALIZE) },
                onRequestDefaultHome = onRequestDefaultHome,
                onSetNotify = viewModel::setNotifyNewApps,
                onResetApp = viewModel::resetApp,
                onUnhideApp = viewModel::unhideApp,
                onResetAll = viewModel::resetAll,
                folderName = viewModel::folderName
            )
            route == PERSONALIZE -> PersonalizeScreen(
                state = state,
                onBack = pop,
                onSetFont = viewModel::setFont,
                onSetAccent = viewModel::setAccent,
                onSetTitle = viewModel::setTitleText,
                onSetEffect = viewModel::setTitleEffect,
                onSetFolderSize = viewModel::setFolderSize,
                onSetShowDate = viewModel::setShowDate,
                onSetShowDock = viewModel::setShowDock,
                onSetMono = viewModel::setMonoIcons,
                onSetSortByUsage = viewModel::setSortByUsage,
                onResetAppearance = viewModel::resetAppearance
            )
            route == CATEGORIES -> CategoriesScreen(
                state = state,
                onBack = pop,
                onAddFromText = { viewModel.addCategoryFromText(it) },
                onAddCategory = viewModel::addCategory,
                onRemoveCategory = viewModel::removeCategory,
                previewFolder = viewModel::previewFolder,
                onRenameFolder = viewModel::renameFolder,
                onMoveFolder = viewModel::moveFolder
            )
            route.startsWith(FOLDER_PREFIX) -> {
                val id = route.removePrefix(FOLDER_PREFIX)
                val folder = state.folders.firstOrNull { it.id == id }
                if (folder == null) {
                    // La carpeta desapareció (se quitó o quedó sin apps "Otras"): volver.
                    LaunchedEffect(route) { if (!state.loading && isTop(route)) pop() }
                } else {
                    FolderScreen(
                        folder = folder,
                        monochrome = state.settings.monoIcons,
                        sortByUsage = state.settings.sortByUsage,
                        onBack = pop,
                        onLaunch = launch,
                        onLongPress = openMove,
                        onRename = { name: String -> viewModel.renameFolder(folder.id, name) }.takeIf { folder.id != CategoryCatalog.OTHER },
                        originalName = CategoryCatalog.byId[folder.id]?.name
                    )
                }
            }
            else -> HomeScreen(
                state = state,
                query = query,
                onQueryChange = onQueryChange,
                onOpenFolder = { push(FOLDER_PREFIX + it) },
                onLaunch = launch,
                onLongPress = openMove,
                onOpenSettings = { push(SETTINGS) },
                onDismissNewApp = viewModel::dismissNewApp,
                onChangeNewApp = openMove,
                onDock = onDock
            )
        }
    }
}
