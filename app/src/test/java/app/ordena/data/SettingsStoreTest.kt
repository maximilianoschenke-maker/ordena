package app.ordena.data

import android.content.SharedPreferences
import app.ordena.domain.CustomCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guarda y vuelve a leer TODA la configuración: si un campo no se guarda o no se lee, falla. */
class SettingsStoreTest {

    @Test
    fun todosLosCamposSobrevivenGuardarYLeer() {
        val prefs = FakePrefs()
        val original = LauncherSettings(
            active = listOf("juegos", "finanzas", "c_ramos"),
            customs = listOf(CustomCategory("c_ramos", "Ramos")),
            overrides = mapOf("com.whatsapp" to "social"),
            launchCounts = mapOf("com.whatsapp" to 7),
            monoIcons = false,
            sortByUsage = false,
            notifyNewApps = false,
            onboardingDone = true,
            knownPackages = setOf("com.whatsapp", "com.android.settings"),
            fontId = "exo2",
            accentId = "menta",
            titleText = "Mi teléfono",
            titleEffect = TitleEffect.SPEED,
            folderSize = FolderSize.LARGE,
            showDate = false,
            showDock = false,
            folderNames = mapOf("finanzas" to "Mi plata"),
            hiddenPackages = setOf("com.android.stk")
        )
        SettingsStore(prefs).write(original)
        val restored = SettingsStore(prefs).read()
        assertEquals(original, restored)
    }

    @Test
    fun sinNadaGuardadoUsaLosValoresIniciales() {
        assertEquals(LauncherSettings(), SettingsStore(FakePrefs()).read())
    }

    @Test
    fun limpiaValoresInvalidos() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString(SettingsStore.KEY_ACTIVE, "juegos,no_existe,c_borrada")
            .putString(SettingsStore.KEY_TITLE_EFFECT, "raro")
            .putString(SettingsStore.KEY_TITLE, "   ")
            .putString(SettingsStore.KEY_FOLDER_NAMES, "juegos\u001F\u001Eno_existe\u001FX")
            .apply()
        val read = SettingsStore(prefs).read()
        assertEquals(listOf("juegos"), read.active)
        assertEquals(TitleEffect.DISINTEGRATE, read.titleEffect)
        assertEquals(AppearanceDefaults.TITLE, read.titleText)
        assertTrue(read.folderNames.isEmpty())
    }
}

/** SharedPreferences en memoria para pruebas en la JVM. */
private class FakePrefs : SharedPreferences {
    private val values = HashMap<String, Any?>()

    override fun getAll(): MutableMap<String, *> = HashMap(values)
    override fun getString(key: String?, defValue: String?): String? = values[key] as String? ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (values[key] as Set<String>?)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = values[key] as Int? ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as Long? ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as Float? ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = HashMap<String, Any?>()
        private var clearAll = false

        private fun put(key: String?, value: Any?): SharedPreferences.Editor {
            pending[requireNotNull(key)] = value
            return this
        }

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = put(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = put(key, values?.toSet())
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = put(key, value)
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = put(key, value)
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = put(key, value)
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = put(key, value)
        override fun remove(key: String?): SharedPreferences.Editor = put(key, REMOVED)

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            if (clearAll) values.clear()
            pending.forEach { (k, v) -> if (v === REMOVED) values.remove(k) else values[k] = v }
            return true
        }

        override fun apply() {
            commit()
        }
    }

    private companion object {
        val REMOVED = Any()
    }
}
