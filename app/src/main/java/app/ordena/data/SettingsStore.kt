package app.ordena.data

import android.content.Context
import app.ordena.domain.CategoryCatalog
import app.ordena.domain.Codec
import app.ordena.domain.CustomCategory

/** Configuración del usuario. Inmutable: cada cambio crea una copia. */
data class LauncherSettings(
    val active: List<String> = CategoryCatalog.defaultActive,
    val customs: List<CustomCategory> = emptyList(),
    val overrides: Map<String, String> = emptyMap(),
    val launchCounts: Map<String, Int> = emptyMap(),
    val monoIcons: Boolean = true,
    val sortByUsage: Boolean = true,
    val notifyNewApps: Boolean = true,
    val onboardingDone: Boolean = false,
    val knownPackages: Set<String> = emptySet()
) {
    /** Quita ids que ya no existen (p. ej. una carpeta del usuario borrada) y repetidos. */
    fun sanitized(): LauncherSettings {
        val customIds = customs.map { it.id }.toSet()
        val cleanActive = active.distinct().filter { it in CategoryCatalog.byId || it in customIds }
        return copy(
            active = cleanActive,
            folderNames = folderNames.filterKeys { it in CategoryCatalog.byId }.filterValues { it.isNotBlank() },
            titleText = AppearanceDefaults.cleanTitle(titleText)
        )
    }
}

/**
 * Guarda la configuración en SharedPreferences. Se usa texto simple (ver [Codec]) para que un
 * valor dañado nunca impida abrir la pantalla de inicio: lo ilegible se ignora.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): LauncherSettings = try {
        LauncherSettings(
            active = if (prefs.contains(KEY_ACTIVE)) {
                Codec.decodeIds(prefs.getString(KEY_ACTIVE, null))
            } else {
                CategoryCatalog.defaultActive
            },
            customs = Codec.decodeCustoms(prefs.getString(KEY_CUSTOMS, null)),
            overrides = Codec.decodeMap(prefs.getString(KEY_OVERRIDES, null)),
            launchCounts = Codec.decodeCounts(prefs.getString(KEY_LAUNCHES, null)),
            monoIcons = prefs.getBoolean(KEY_MONO, true),
            sortByUsage = prefs.getBoolean(KEY_SORT_USAGE, true),
            notifyNewApps = prefs.getBoolean(KEY_NOTIFY_NEW, true),
            onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false),
            // getStringSet devuelve un set interno que no se debe modificar: se copia.
            knownPackages = prefs.getStringSet(KEY_KNOWN, null)?.toSet().orEmpty()
        ).sanitized()
    } catch (e: ClassCastException) {
        // Un valor guardado con otro tipo (versión antigua): se parte de cero en vez de cerrarse.
        LauncherSettings()
    }

    fun write(settings: LauncherSettings) {
        prefs.edit()
            .putString(KEY_ACTIVE, Codec.encodeIds(settings.active))
            .putString(KEY_CUSTOMS, Codec.encodeCustoms(settings.customs))
            .putString(KEY_OVERRIDES, Codec.encodeMap(settings.overrides))
            .putString(KEY_LAUNCHES, Codec.encodeCounts(settings.launchCounts))
            .putBoolean(KEY_MONO, settings.monoIcons)
            .putBoolean(KEY_SORT_USAGE, settings.sortByUsage)
            .putBoolean(KEY_NOTIFY_NEW, settings.notifyNewApps)
            .putBoolean(KEY_ONBOARDING, settings.onboardingDone)
            .putStringSet(KEY_KNOWN, HashSet(settings.knownPackages))
            .putString(KEY_FONT, settings.fontId)
            .putString(KEY_ACCENT, settings.accentId)
            .putString(KEY_TITLE, settings.titleText)
            .putString(KEY_TITLE_EFFECT, settings.titleEffect.id)
            .putString(KEY_FOLDER_SIZE, settings.folderSize.id)
            .putBoolean(KEY_SHOW_DATE, settings.showDate)
            .putBoolean(KEY_SHOW_DOCK, settings.showDock)
            .putString(KEY_FOLDER_NAMES, Codec.encodeMap(settings.folderNames))
            .putString(KEY_HIDDEN, Codec.encodeIds(settings.hiddenPackages.sorted()))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "ordena_settings"
        const val KEY_ACTIVE = "active_categories"
        const val KEY_CUSTOMS = "custom_categories"
        const val KEY_OVERRIDES = "overrides"
        const val KEY_LAUNCHES = "launch_counts"
        const val KEY_MONO = "mono_icons"
        const val KEY_SORT_USAGE = "sort_by_usage"
        const val KEY_NOTIFY_NEW = "notify_new_apps"
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_KNOWN = "known_packages"
        const val KEY_FONT = "font"
        const val KEY_ACCENT = "accent"
        const val KEY_TITLE = "title_text"
        const val KEY_TITLE_EFFECT = "title_effect"
        const val KEY_FOLDER_SIZE = "folder_size"
        const val KEY_SHOW_DATE = "show_date"
        const val KEY_SHOW_DOCK = "show_dock"
        const val KEY_FOLDER_NAMES = "folder_names"
        const val KEY_HIDDEN = "hidden_packages"
    }
}
