package app.ordena.ui

import android.app.Application
import android.content.pm.LauncherApps
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ordena.data.AppRepository
import app.ordena.data.AppearanceDefaults
import app.ordena.data.FolderSize
import app.ordena.data.InstalledApp
import app.ordena.data.LauncherSettings
import app.ordena.data.SettingsStore
import app.ordena.data.TitleEffect
import app.ordena.domain.CategoryCatalog
import app.ordena.domain.CategoryMatcher
import app.ordena.domain.Classification
import app.ordena.domain.Classifier
import app.ordena.domain.Codec
import app.ordena.domain.CustomCategory
import app.ordena.domain.Organizer
import app.ordena.domain.Placement
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Una carpeta tal como se muestra. */
data class Folder(
    val id: String,
    val name: String,
    val shortName: String,
    val iconPath: String,
    val apps: List<InstalledApp>,
    val isCustom: Boolean
)

data class NewAppNotice(val app: InstalledApp, val folderName: String, val reason: String, val othersCount: Int)

data class UiState(
    val loading: Boolean = true,
    /** Todas las carpetas activas (incluidas las vacías) y "Otras apps" si tiene algo. */
    val folders: List<Folder> = emptyList(),
    /** Las que se ven en el inicio: las que tienen apps y las creadas por el usuario. */
    val homeFolders: List<Folder> = emptyList(),
    val placements: Map<String, Placement> = emptyMap(),
    val apps: List<InstalledApp> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val newApp: NewAppNotice? = null,
    /** Cambia en cada actualización; sirve para refrescar la fecha del encabezado. */
    val refreshedAt: Long = 0L
)

/** Resultado de intentar agregar una carpeta escrita por el usuario. */
sealed interface AddResult {
    data class Added(val id: String, val name: String) : AddResult
    data class AlreadyActive(val id: String, val name: String) : AddResult
    data object Invalid : AddResult
}

interface IconProvider {
    fun cachedIcon(app: InstalledApp): ImageBitmap?
    suspend fun loadIcon(app: InstalledApp): ImageBitmap?
}

class LauncherViewModel(application: Application) :
    AndroidViewModel(application),
    IconProvider {

    private val repository = AppRepository(application)
    private val store = SettingsStore(application)

    private var settings: LauncherSettings = store.read()
    private var apps: List<InstalledApp> = emptyList()
    private var classifications: Map<String, Classification> = emptyMap()
    private var newAppKey: String? = null
    private var newAppOthers: Int = 0
    private var loaded = false

    private val _state = MutableStateFlow(UiState(settings = settings))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var refreshJob: Job? = null
    private var iconWarmJob: Job? = null
    private val callback: LauncherApps.Callback

    init {
        callback = repository.registerCallback { packageName ->
            packageName?.let { repository.invalidateIcons(it) }
            refresh(delayMillis = 400)
        }
        refresh()
    }

    override fun onCleared() {
        repository.unregisterCallback(callback)
        super.onCleared()
    }

    /** Vuelve a leer las apps instaladas. Llamadas seguidas se agrupan en una sola. */
    fun refresh(delayMillis: Long = 0) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (delayMillis > 0) delay(delayMillis)
            val loadedApps = withContext(Dispatchers.IO) { repository.loadApps() }
            val classified = withContext(Dispatchers.Default) {
                loadedApps.associate { it.key to Classifier.classify(it.signals) }
            }
            apps = loadedApps
            classifications = classified
            loaded = true
            detectNewApps()
            publish()
            warmIcons()
        }
    }

    fun onResume() = refresh(delayMillis = 150)

    // ----- Acciones -----

    fun launch(app: InstalledApp) {
        if (!repository.launch(app)) {
            _messages.tryEmit("No se pudo abrir ${app.label}")
            refresh()
            return
        }
        val count = (settings.launchCounts[app.packageName] ?: 0) + 1
        update(settings.copy(launchCounts = settings.launchCounts + (app.packageName to count)))
    }

    fun openAppInfo(app: InstalledApp) {
        if (!repository.openAppInfo(app)) _messages.tryEmit("No se pudo abrir la información de ${app.label}")
    }

    fun moveApp(app: InstalledApp, folderId: String) {
        val name = folderName(folderId)
        update(settings.copy(overrides = settings.overrides + (app.packageName to folderId)))
        if (newAppKey != null && apps.firstOrNull { it.key == newAppKey }?.packageName == app.packageName) {
            dismissNewApp()
        }
        _messages.tryEmit("${app.label} ahora está en $name")
    }

    /** Devuelve la app a la clasificación automática. */
    fun resetApp(packageName: String) {
        update(settings.copy(overrides = settings.overrides - packageName))
    }

    /** Crea (o reutiliza, si BuenOrden la reconoce) una carpeta con ese nombre y mueve la app ahí. */
    fun createFolderAndMove(app: InstalledApp, name: String) {
        when (val result = addCategoryFromText(name, announce = false)) {
            is AddResult.Added -> moveApp(app, result.id)
            is AddResult.AlreadyActive -> moveApp(app, result.id)
            AddResult.Invalid -> _messages.tryEmit("Escribe un nombre para la carpeta")
        }
    }

    /**
     * Agrega la carpeta que el usuario escribió. Si BuenOrden entiende el texto ("bancos", "rrss"),
     * activa esa categoría; si no, crea una carpeta propia con ese nombre.
     */
    fun addCategoryFromText(text: String, announce: Boolean = true): AddResult {
        val cleanName = Codec.cleanName(text)
        if (cleanName.isEmpty() || cleanName.filter { it.isLetterOrDigit() }.isEmpty()) return AddResult.Invalid
        val match = CategoryMatcher.match(cleanName, settings.customs, settings.folderNames)
        val result = if (match != null) {
            activate(match.id, match.name)
        } else {
            val id = Codec.newCustomId(cleanName, settings.customs.map { it.id })
            val custom = CustomCategory(id, cleanName.replaceFirstChar { it.uppercaseChar() })
            update(settings.copy(customs = settings.customs + custom, active = listOf(id) + settings.active))
            AddResult.Added(id, custom.name)
        }
        if (announce) {
            when (result) {
                is AddResult.Added -> _messages.tryEmit("Carpeta «${result.name}» agregada")
                is AddResult.AlreadyActive -> _messages.tryEmit("Ya tienes la carpeta «${result.name}»")
                AddResult.Invalid -> Unit
            }
        }
        return result
    }

    fun addCategory(id: String) {
        val name = folderName(id)
        val result = activate(id, name)
        if (result is AddResult.Added) _messages.tryEmit("Carpeta «$name» agregada")
    }

    fun removeCategory(id: String) {
        update(settings.copy(active = settings.active - id))
    }

    fun setMonoIcons(enabled: Boolean) = update(settings.copy(monoIcons = enabled))
    fun setSortByUsage(enabled: Boolean) = update(settings.copy(sortByUsage = enabled))
    fun setNotifyNewApps(enabled: Boolean) = update(settings.copy(notifyNewApps = enabled))
    fun completeOnboarding() = update(settings.copy(onboardingDone = true))

    // ----- Personalizar -----

    fun setFont(id: String) = update(settings.copy(fontId = id))
    fun setAccent(id: String) = update(settings.copy(accentId = id))
    fun setTitleText(text: String) = update(settings.copy(titleText = AppearanceDefaults.cleanTitle(text)))
    fun setTitleEffect(effect: TitleEffect) = update(settings.copy(titleEffect = effect))
    fun setFolderSize(size: FolderSize) = update(settings.copy(folderSize = size))
    fun setShowDate(show: Boolean) = update(settings.copy(showDate = show))
    fun setShowDock(show: Boolean) = update(settings.copy(showDock = show))

    /** Vuelve la apariencia a la original sin tocar carpetas ni apps movidas. */
    fun resetAppearance() {
        val defaults = LauncherSettings()
        update(
            settings.copy(
                fontId = defaults.fontId,
                accentId = defaults.accentId,
                titleText = defaults.titleText,
                titleEffect = defaults.titleEffect,
                folderSize = defaults.folderSize,
                showDate = defaults.showDate,
                showDock = defaults.showDock,
                monoIcons = defaults.monoIcons,
                sortByUsage = defaults.sortByUsage
            )
        )
        _messages.tryEmit("Apariencia restaurada")
    }

    // ----- Carpetas: nombre y orden -----

    /** Renombra una carpeta. Vacío o igual al nombre original = vuelve al nombre original. */
    fun renameFolder(id: String, rawName: String) {
        val name = Codec.cleanName(rawName)
        val custom = settings.customs.firstOrNull { it.id == id }
        when {
            custom != null -> {
                if (name.isEmpty()) return
                update(settings.copy(customs = settings.customs.map { if (it.id == id) it.copy(name = name) else it }))
            }
            CategoryCatalog.byId[id] != null -> {
                val original = CategoryCatalog.byId.getValue(id).name
                val names = if (name.isEmpty() || name == original) {
                    settings.folderNames - id
                } else {
                    settings.folderNames + (id to name)
                }
                update(settings.copy(folderNames = names))
            }
        }
    }

    /** Sube (delta = -1) o baja (delta = +1) una carpeta en el inicio. */
    fun moveFolder(id: String, delta: Int) {
        val list = settings.active.toMutableList()
        val from = list.indexOf(id)
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        update(settings.copy(active = list))
    }

    // ----- Apps ocultas -----

    fun hideApp(app: InstalledApp) {
        update(settings.copy(hiddenPackages = settings.hiddenPackages + app.packageName))
        _messages.tryEmit("${app.label} quedó oculta. Puedes encontrarla buscándola.")
    }

    fun unhideApp(packageName: String) {
        update(settings.copy(hiddenPackages = settings.hiddenPackages - packageName))
    }

    fun dismissNewApp() {
        newAppKey = null
        newAppOthers = 0
        publish()
    }

    fun resetAll() {
        store.clear()
        newAppKey = null
        update(LauncherSettings(onboardingDone = true, knownPackages = apps.map { it.packageName }.toSet()))
        _messages.tryEmit("BuenOrden volvió a su configuración inicial")
    }

    /** Qué apps quedarían en una carpeta si se agregara (o las que ya tiene). Para la vista previa. */
    fun previewFolder(id: String): List<InstalledApp> {
        val active = if (id in settings.active) settings.active else listOf(id) + settings.active
        return apps.filter { app ->
            app.packageName !in settings.hiddenPackages && place(app, active).categoryId == id
        }
    }

    fun folderName(id: String): String = when {
        id == CategoryCatalog.OTHER -> CategoryCatalog.OTHER_NAME
        else -> settings.folderNames[id]
            ?: CategoryCatalog.byId[id]?.name
            ?: settings.customs.firstOrNull { it.id == id }?.name
            ?: id
    }

    // ----- Íconos -----

    override fun cachedIcon(app: InstalledApp): ImageBitmap? = repository.cachedIcon(app.key)

    override suspend fun loadIcon(app: InstalledApp): ImageBitmap? = withContext(Dispatchers.IO) { repository.loadIcon(app.key) }

    /** Carga los íconos en segundo plano para que las carpetas se abran sin parpadeo. */
    private fun warmIcons() {
        iconWarmJob?.cancel()
        val snapshot = apps
        iconWarmJob = viewModelScope.launch(Dispatchers.IO) {
            snapshot.forEach { repository.loadIcon(it.key) }
        }
    }

    // ----- Interno -----

    private fun activate(id: String, name: String): AddResult {
        if (id in settings.active) return AddResult.AlreadyActive(id, name)
        update(settings.copy(active = listOf(id) + settings.active))
        return AddResult.Added(id, name)
    }

    private fun update(newSettings: LauncherSettings) {
        settings = newSettings.sanitized()
        store.write(settings)
        publish()
    }

    private fun place(app: InstalledApp, active: List<String> = settings.active): Placement = Organizer.place(
        app.signals,
        classifications[app.key] ?: Classification(emptyList()),
        active,
        settings.customs,
        settings.overrides
    )

    /**
     * Detecta apps recién instaladas comparando con las que BuenOrden ya conocía (guardadas en disco,
     * así funciona aunque el sistema haya cerrado BuenOrden mientras se instalaba algo).
     */
    private fun detectNewApps() {
        val current = apps.map { it.packageName }.toSet()
        if (current.isEmpty()) return // Lectura fallida: no tocar lo guardado.
        val known = settings.knownPackages
        if (known.isNotEmpty() && settings.notifyNewApps) {
            val added = current - known
            if (added.isNotEmpty()) {
                val candidates = apps.filter { it.packageName in added }
                val first = candidates.firstOrNull { !it.isWorkProfile } ?: candidates.first()
                newAppKey = first.key
                newAppOthers = added.size - 1
            }
        }
        if (current != known) {
            settings = settings.copy(knownPackages = current)
            store.write(settings)
        }
    }

    private fun publish() {
        val s = settings
        val placements = apps.associate { it.key to place(it) }
        // Las apps ocultas no van en ninguna carpeta (la búsqueda sí las encuentra).
        val grouped = apps
            .filter { it.packageName !in s.hiddenPackages }
            .groupBy { placements.getValue(it.key).categoryId }
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val byLabel = Comparator<InstalledApp> { a, b -> collator.compare(a.label, b.label) }
        val comparator = if (s.sortByUsage) {
            compareByDescending<InstalledApp> { s.launchCounts[it.packageName] ?: 0 }.then(byLabel)
        } else {
            byLabel
        }

        val folders = s.active.mapNotNull { id ->
            val folderApps = grouped[id].orEmpty().sortedWith(comparator)
            val def = CategoryCatalog.byId[id]
            when {
                def != null -> {
                    val renamed = s.folderNames[id]
                    Folder(id, renamed ?: def.name, renamed ?: def.shortName, def.iconPath, folderApps, isCustom = false)
                }
                else -> s.customs.firstOrNull { it.id == id }?.let {
                    Folder(id, it.name, it.name, CategoryCatalog.FOLDER_ICON, folderApps, isCustom = true)
                }
            }
        }.toMutableList()
        grouped[CategoryCatalog.OTHER]?.let { others ->
            folders += Folder(
                CategoryCatalog.OTHER,
                CategoryCatalog.OTHER_NAME,
                "Otras",
                CategoryCatalog.FOLDER_ICON,
                others.sortedWith(comparator),
                isCustom = false
            )
        }

        val notice = newAppKey?.let { key ->
            val app = apps.firstOrNull { it.key == key } ?: return@let null
            val placement = placements[key] ?: return@let null
            NewAppNotice(app, folderName(placement.categoryId), placement.reason, newAppOthers)
        }
        if (newAppKey != null && notice == null) newAppKey = null // La app ya no está.

        _state.value = UiState(
            loading = !loaded,
            folders = folders,
            homeFolders = folders.filter { it.apps.isNotEmpty() || it.isCustom },
            placements = placements,
            apps = apps,
            settings = s,
            newApp = notice,
            refreshedAt = System.currentTimeMillis()
        )
    }
}
