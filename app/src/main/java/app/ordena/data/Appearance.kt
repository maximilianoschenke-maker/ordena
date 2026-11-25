package app.ordena.data

/** Efecto estático del título del inicio. */
enum class TitleEffect(val id: String, val label: String) {
    DISINTEGRATE("desintegracion", "Desintegración"),
    SPEED("velocidad", "Velocidad"),
    NONE("ninguno", "Sin efecto")
    ;

    companion object {
        fun fromId(id: String?): TitleEffect = entries.firstOrNull { it.id == id } ?: DISINTEGRATE
    }
}

/** Tamaño de los recuadros de carpeta en el inicio. */
enum class FolderSize(val id: String, val label: String) {
    COMPACT("compacto", "Compactas"),
    NORMAL("normal", "Normales"),
    LARGE("grande", "Grandes")
    ;

    companion object {
        fun fromId(id: String?): FolderSize = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

object AppearanceDefaults {
    const val TITLE = "BuenOrden"
    const val TITLE_MAX_LENGTH = 18
    const val FONT_ID = "geist"
    const val ACCENT_ID = "blanco"
    val TITLE_PRESETS = listOf("BuenOrden", "Inicio")

    /** Título limpio: sin saltos, espacios simples, largo máximo; vacío = el predeterminado. */
    fun cleanTitle(raw: String): String {
        val clean = raw.filterNot { it.isISOControl() }.replace(Regex("\\s+"), " ").trim().take(TITLE_MAX_LENGTH).trim()
        return clean.ifEmpty { TITLE }
    }
}
