package app.ordena.domain

/**
 * Entiende lo que el usuario escribe al agregar una carpeta o buscar:
 * "bancos", "plata", "Redes Sociales", "rrss", "juegos para niños", "apps de bancos", "ban"...
 *
 * Fuerza de la coincidencia (mayor gana):
 *  3 = la frase completa es un sinónimo (ignorando tildes, mayúsculas y plurales)
 *  2 = lo escrito es el comienzo de un sinónimo (mientras se escribe: "ban" → bancos)
 *  1 = un sinónimo aparece dentro de lo escrito ("apps de bancos")
 * Las carpetas creadas por el usuario se revisan primero, así ganan los empates.
 */
object CategoryMatcher {

    data class Match(val id: String, val name: String, val isCustom: Boolean, val strength: Int)

    private const val EXACT = 3
    private const val PREFIX = 2
    private const val CONTAINED = 1

    /**
     * @param renamed nombres que el usuario le puso a carpetas de BuenOrden (id → nombre): también
     *        se reconocen al escribirlos, y la coincidencia devuelve ese nombre.
     */
    fun match(
        input: String,
        customs: List<CustomCategory> = emptyList(),
        renamed: Map<String, String> = emptyMap()
    ): Match? {
        val query = TextNormalizer.words(input)
        val compact = query.joinToString("")
        if (compact.length < 2) return null

        var best: Match? = null
        fun consider(id: String, name: String, isCustom: Boolean, phrases: List<String>) {
            val strength = phrases.maxOfOrNull { strength(query, it.split(' ')) } ?: 0
            if (strength > 0 && strength > (best?.strength ?: 0)) best = Match(id, name, isCustom, strength)
        }

        customs.forEach { consider(it.id, it.name, true, listOf(TextNormalizer.normalize(it.name))) }
        CategoryCatalog.all.forEach { cat ->
            val customName = renamed[cat.id]?.takeIf { it.isNotBlank() }
            val phrases = cat.synonyms + TextNormalizer.normalize(cat.name) +
                listOfNotNull(customName?.let { TextNormalizer.normalize(it) }?.takeIf { it.isNotEmpty() })
            consider(cat.id, customName ?: cat.name, false, phrases)
        }
        return best
    }

    private fun strength(query: List<String>, synonym: List<String>): Int {
        if (synonym.isEmpty() || synonym.first().isEmpty()) return 0
        if (TextNormalizer.samePhrase(query, synonym)) return EXACT
        if (query.joinToString("").length >= 3 && isPrefix(query, synonym)) return PREFIX
        if (TextNormalizer.containsPhrase(query, synonym)) return CONTAINED
        return 0
    }

    /** "redes soc" es prefijo de "redes sociales"; "ban" de "bancos". */
    private fun isPrefix(query: List<String>, synonym: List<String>): Boolean {
        if (query.isEmpty() || query.size > synonym.size) return false
        for (i in 0 until query.size - 1) {
            if (!TextNormalizer.sameWord(query[i], synonym[i])) return false
        }
        return synonym[query.size - 1].startsWith(query.last())
    }
}
