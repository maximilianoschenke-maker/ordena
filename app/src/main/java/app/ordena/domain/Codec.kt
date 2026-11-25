package app.ordena.domain

/**
 * Codificación simple y robusta de la configuración en texto (para SharedPreferences).
 * Usa los separadores ASCII de unidad (0x1F) y registro (0x1E), que nunca aparecen en
 * nombres escritos por el usuario (se eliminan al guardar). Lo que no se pueda leer se ignora
 * en vez de romper la app.
 */
object Codec {

    private const val FIELD = '\u001F'
    private const val RECORD = '\u001E'
    private const val MAX_NAME_LENGTH = 30

    fun encodeIds(ids: List<String>): String = ids.joinToString(",")

    fun decodeIds(raw: String?): List<String> = raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun encodeMap(map: Map<String, String>): String = map.entries.joinToString(RECORD.toString()) { clean(it.key) + FIELD + clean(it.value) }

    fun decodeMap(raw: String?): LinkedHashMap<String, String> {
        val result = LinkedHashMap<String, String>()
        if (raw.isNullOrEmpty()) return result
        raw.split(RECORD).forEach { record ->
            val parts = record.split(FIELD, limit = 2)
            if (parts.size == 2 && parts[0].isNotEmpty()) result[parts[0]] = parts[1]
        }
        return result
    }

    fun encodeCounts(map: Map<String, Int>): String = encodeMap(map.mapValues { it.value.toString() })

    fun decodeCounts(raw: String?): LinkedHashMap<String, Int> {
        val result = LinkedHashMap<String, Int>()
        decodeMap(raw).forEach { (key, value) -> value.toIntOrNull()?.takeIf { it >= 0 }?.let { result[key] = it } }
        return result
    }

    fun encodeCustoms(customs: List<CustomCategory>): String = encodeMap(LinkedHashMap<String, String>().apply { customs.forEach { put(it.id, it.name) } })

    fun decodeCustoms(raw: String?): List<CustomCategory> = decodeMap(raw)
        .filterKeys { CategoryCatalog.isCustom(it) }
        .map { CustomCategory(it.key, it.value) }

    /** Limpia un nombre escrito por el usuario: sin saltos ni separadores, espacios simples, largo máximo. */
    fun cleanName(raw: String): String = raw.filterNot { it.isISOControl() }
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(MAX_NAME_LENGTH)
        .trim()

    /** Id estable para una carpeta nueva, único respecto de [existing]. */
    fun newCustomId(name: String, existing: Collection<String>): String {
        val base = CategoryCatalog.CUSTOM_PREFIX +
            TextNormalizer.words(name).joinToString("_").ifEmpty { "carpeta" }.take(40)
        if (base !in existing) return base
        var n = 2
        while ("${base}_$n" in existing) n++
        return "${base}_$n"
    }

    private fun clean(value: String): String = value.replace(FIELD, ' ').replace(RECORD, ' ')
}
