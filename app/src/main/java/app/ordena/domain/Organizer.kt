package app.ordena.domain

/** Dónde quedó una app y por qué. */
data class Placement(val categoryId: String, val reason: String, val isUserChoice: Boolean = false)

/**
 * Decide la carpeta final de una app considerando lo que el usuario configuró:
 *  1. Si el usuario movió la app a mano (y esa carpeta sigue activa), se respeta.
 *  2. Si el nombre de la app coincide con una carpeta creada por el usuario, va ahí.
 *  3. Se recorre el ranking del clasificador; si la mejor categoría no está activa,
 *     se prueba con sus categorías de respaldo (p. ej. Mapas → Viajes).
 *  4. Si nada calza, va a "Otras apps".
 */
object Organizer {

    private const val CUSTOM_NAME_SCORE = 80

    fun place(
        app: AppSignals,
        classification: Classification,
        active: Collection<String>,
        customs: List<CustomCategory>,
        overrides: Map<String, String>
    ): Placement {
        val activeSet = active.toHashSet()

        overrides[app.packageName]?.let { chosen ->
            if (chosen in activeSet) return Placement(chosen, "Tú la pusiste aquí", isUserChoice = true)
        }

        val labelWords = TextNormalizer.words(app.label)
        val customHits = customs
            .filter { it.id in activeSet }
            .filter { custom ->
                val nameWords = TextNormalizer.words(custom.name).filter { it.length >= 3 }
                nameWords.isNotEmpty() && TextNormalizer.containsPhrase(labelWords, nameWords)
            }
            .map { Candidate(it.id, CUSTOM_NAME_SCORE, "Su nombre coincide con la carpeta «${it.name}»") }

        val candidates = (customHits + classification.ranked).sortedByDescending { it.score }
        for (candidate in candidates) {
            if (candidate.categoryId in activeSet) return Placement(candidate.categoryId, candidate.reason)
            val fallbacks = CategoryCatalog.byId[candidate.categoryId]?.fallbacks.orEmpty()
            fallbacks.firstOrNull { it in activeSet }?.let { return Placement(it, candidate.reason) }
        }
        return Placement(CategoryCatalog.OTHER, "No encontramos una carpeta que le calce")
    }
}
