package app.ordena.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Normaliza texto para comparar lo que escribe el usuario con nombres de categorías y de apps:
 * minúsculas, sin tildes ni ñ, solo letras y números separados por un espacio.
 *
 * "Redes Sociales!" -> "redes sociales", "Niños" -> "ninos", "Música" -> "musica".
 */
object TextNormalizer {

    private val combiningMarks = Regex("\\p{Mn}+")
    private val nonAlphanumeric = Regex("[^a-z0-9]+")

    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed
            .replace(combiningMarks, "")
            .replace(nonAlphanumeric, " ")
            .trim()
    }

    fun words(input: String?): List<String> = normalize(input).split(' ').filter { it.isNotEmpty() }

    /**
     * Formas posibles de una palabra sin plural. En español el plural agrega "s" (viaje -> viajes)
     * o "es" (red -> redes); no se puede saber cuál sin un diccionario, así que se prueban ambas.
     */
    fun variants(word: String): Set<String> {
        val result = linkedSetOf(word)
        if (word.length > 3 && word.endsWith("s")) result += word.dropLast(1)
        if (word.length > 4 && word.endsWith("es")) result += word.dropLast(2)
        return result
    }

    /** Dos palabras son la misma si comparten alguna forma sin plural. */
    fun sameWord(a: String, b: String): Boolean {
        if (a == b) return true
        val va = variants(a)
        return variants(b).any { it in va }
    }

    /** Frases de igual largo cuyas palabras coinciden una a una (ignorando plurales). */
    fun samePhrase(a: List<String>, b: List<String>): Boolean = a.size == b.size && a.indices.all { sameWord(a[it], b[it]) }

    /** true si [phrase] aparece como palabras seguidas dentro de [text]. */
    fun containsPhrase(text: List<String>, phrase: List<String>): Boolean {
        if (phrase.isEmpty() || phrase.size > text.size) return false
        for (start in 0..text.size - phrase.size) {
            if (phrase.indices.all { sameWord(text[start + it], phrase[it]) }) return true
        }
        return false
    }
}
