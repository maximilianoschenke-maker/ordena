package app.ordena.domain

/** Categoría que la propia app declara en su manifiesto (ApplicationInfo.category, Android 8+). */
enum class DeclaredCategory { GAME, AUDIO, VIDEO, IMAGE, SOCIAL, NEWS, MAPS, PRODUCTIVITY, ACCESSIBILITY, UNDEFINED }

/** Cosas que el sistema sabe que la app puede hacer (resuelto con intents). */
enum class Capability { DIALER, SMS, CAMERA, BROWSER, MAPS, MUSIC, EMAIL, CALENDAR }

/** Todo lo que se sabe de una app para decidir su carpeta. No depende de Android: se puede testear en la JVM. */
data class AppSignals(
    val packageName: String,
    val label: String,
    val declaredCategory: DeclaredCategory = DeclaredCategory.UNDEFINED,
    val isGameFlag: Boolean = false,
    val isSystem: Boolean = false,
    val capabilities: Set<Capability> = emptySet()
)

/** Categoría candidata con su puntaje y la razón legible de por qué. */
data class Candidate(val categoryId: String, val score: Int, val reason: String)

/** Resultado del clasificador: candidatas de mayor a menor puntaje. */
data class Classification(val ranked: List<Candidate>)

/**
 * Asigna puntajes a cada categoría según señales independientes y devuelve el ranking.
 * Pesos (mayor gana):
 *  - app conocida por paquete exacto ........ 100 (prefijo de estudio de juegos: 90)
 *  - capacidad fuerte (llamar, cámara) ........ 65–70
 *  - categoría declarada por la app ........... 60
 *  - capacidad media (mapas, correo, web) ..... 45–50
 *  - palabra clave en el nombre ............... 30 (+20 si también en el paquete)
 *  - capacidad débil (reproduce audio) ........ 35
 *  - app del sistema sin otra pista ........... 10 → Herramientas
 * Las candidatas con menos de [MIN_SCORE] se descartan (salvo la de app del sistema).
 */
object Classifier {

    const val MIN_SCORE = 20
    private const val SYSTEM_FALLBACK_SCORE = 10

    private val declaredMap = mapOf(
        DeclaredCategory.GAME to ("juegos" to "La app se declara como juego"),
        DeclaredCategory.AUDIO to ("musica" to "La app se declara de música y audio"),
        DeclaredCategory.VIDEO to ("video" to "La app se declara de video"),
        DeclaredCategory.IMAGE to ("fotos" to "La app se declara de fotos"),
        DeclaredCategory.SOCIAL to ("social" to "La app se declara como red social"),
        DeclaredCategory.NEWS to ("noticias" to "La app se declara de noticias"),
        DeclaredCategory.MAPS to ("mapas" to "La app se declara de mapas"),
        DeclaredCategory.PRODUCTIVITY to ("productividad" to "La app se declara de productividad"),
        DeclaredCategory.ACCESSIBILITY to ("herramientas" to "La app se declara de accesibilidad")
    )

    private data class CapabilityRule(val categoryId: String, val score: Int, val reason: String)

    private val capabilityMap = mapOf(
        Capability.DIALER to CapabilityRule("mensajeria", 70, "Puede hacer llamadas"),
        Capability.SMS to CapabilityRule("mensajeria", 65, "Puede enviar mensajes"),
        Capability.CAMERA to CapabilityRule("fotos", 70, "Abre la cámara"),
        Capability.MAPS to CapabilityRule("mapas", 50, "Abre direcciones y mapas"),
        Capability.EMAIL to CapabilityRule("productividad", 50, "Envía correos"),
        Capability.CALENDAR to CapabilityRule("productividad", 50, "Maneja tu calendario"),
        Capability.BROWSER to CapabilityRule("herramientas", 45, "Es un navegador web"),
        Capability.MUSIC to CapabilityRule("musica", 35, "Reproduce audio")
    )

    private const val LABEL_KEYWORD_SCORE = 30
    private const val PACKAGE_KEYWORD_SCORE = 20

    fun classify(app: AppSignals): Classification {
        // Mejor (puntaje, razón) por categoría; las señales distintas de palabras clave no se suman,
        // gana la más fuerte. Las palabras clave sí se suman (nombre + paquete) porque son independientes.
        val best = LinkedHashMap<String, Candidate>()
        fun offer(categoryId: String, score: Int, reason: String) {
            val current = best[categoryId]
            if (current == null || score > current.score) best[categoryId] = Candidate(categoryId, score, reason)
        }

        KnownApps.lookup(app.packageName)?.let { match ->
            val score = if (match.kind == KnownApps.MatchKind.EXACT) 100 else 90
            val reason = if (match.kind == KnownApps.MatchKind.EXACT) {
                "Es una app conocida de esta categoría"
            } else {
                "Es de un estudio de videojuegos"
            }
            offer(match.categoryId, score, reason)
        }

        declaredMap[app.declaredCategory]?.let { (categoryId, reason) -> offer(categoryId, 60, reason) }
        if (app.isGameFlag) offer("juegos", 60, "La app se declara como juego")

        app.capabilities.forEach { capability ->
            capabilityMap[capability]?.let { offer(it.categoryId, it.score, it.reason) }
        }

        val labelWords = TextNormalizer.words(app.label)
        val packageWords = packageTokens(app.packageName)
        for (category in CategoryCatalog.all) {
            val labelHit = category.keywords.firstOrNull { matchesKeyword(labelWords, it) }
            val packageHit = category.keywords.firstOrNull { matchesKeyword(packageWords, it) }
            if (labelHit == null && packageHit == null) continue
            val score = (if (labelHit != null) LABEL_KEYWORD_SCORE else 0) +
                (if (packageHit != null) PACKAGE_KEYWORD_SCORE else 0)
            val reason = if (labelHit != null) {
                "Su nombre incluye «$labelHit»"
            } else {
                "Su identificador incluye «$packageHit»"
            }
            offer(category.id, score, reason)
        }

        val catalogOrder = CategoryCatalog.all.withIndex().associate { it.value.id to it.index }
        val ranked = best.values
            .filter { it.score >= MIN_SCORE }
            .sortedWith(compareByDescending<Candidate> { it.score }.thenBy { catalogOrder[it.categoryId] ?: Int.MAX_VALUE })
            .toMutableList()

        if (app.isSystem && ranked.none { it.categoryId == "herramientas" }) {
            ranked += Candidate("herramientas", SYSTEM_FALLBACK_SCORE, "Es una app del sistema")
        }
        return Classification(ranked)
    }

    /** "com.bancoestado.app_v2" -> [com, bancoestado, app, v2] */
    fun packageTokens(packageName: String): List<String> = packageName.split('.', '_', '-').flatMap { TextNormalizer.words(it) }

    /**
     * Una palabra clave coincide con una lista de palabras si:
     *  - es una frase de varias palabras que aparece seguida ("smart home"),
     *  - o alguna palabra es igual (ignorando plural),
     *  - o, si la clave tiene 4+ letras, alguna palabra empieza con ella ("bank" → "banking"),
     *  - o, si tiene 6+ letras, alguna palabra la contiene ("wallet" → "mywallet").
     * Claves de 1–3 letras ("fit", "pay", "tv") solo coinciden exactas para evitar falsos positivos.
     */
    fun matchesKeyword(words: List<String>, keyword: String): Boolean {
        if (keyword.isEmpty() || words.isEmpty()) return false
        if (keyword.contains(' ')) return TextNormalizer.containsPhrase(words, keyword.split(' '))
        return words.any { word ->
            word == keyword ||
                (keyword.length >= 4 && TextNormalizer.sameWord(word, keyword)) ||
                (keyword.length >= 4 && word.startsWith(keyword)) ||
                (keyword.length >= 6 && word.contains(keyword))
        }
    }
}
