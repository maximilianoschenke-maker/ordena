package app.ordena.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecAndTextTest {

    @Test
    fun normaliza() {
        assertEquals("redes sociales", TextNormalizer.normalize("Redes Sociales!"))
        assertEquals("ninos", TextNormalizer.normalize("Niños"))
        assertEquals("musica", TextNormalizer.normalize("  Música  "))
        assertEquals("", TextNormalizer.normalize(null))
        assertEquals("24 horas", TextNormalizer.normalize("24-Horas"))
    }

    @Test
    fun plurales() {
        assertTrue(TextNormalizer.sameWord("viaje", "viajes"))
        assertTrue(TextNormalizer.sameWord("red", "redes"))
        assertTrue(TextNormalizer.sameWord("juego", "juegos"))
        assertFalse(TextNormalizer.sameWord("ban", "bancos"))
        assertTrue(TextNormalizer.containsPhrase(listOf("apps", "de", "bancos"), listOf("banco")))
        assertFalse(TextNormalizer.containsPhrase(listOf("bancos"), listOf("apps", "de", "bancos")))
    }

    @Test
    fun idsIdaYVuelta() {
        assertEquals(listOf("a", "b"), Codec.decodeIds(Codec.encodeIds(listOf("a", "b"))))
        assertEquals(listOf("a", "b"), Codec.decodeIds("a, b,,a"))
        assertEquals(emptyList<String>(), Codec.decodeIds(null))
        assertEquals(emptyList<String>(), Codec.decodeIds(""))
    }

    @Test
    fun mapaIdaYVueltaConCaracteresRaros() {
        val original = linkedMapOf("com.a" to "juegos", "com.b" to "c_mi\u001Fcarpeta")
        val decoded = Codec.decodeMap(Codec.encodeMap(original))
        assertEquals("juegos", decoded["com.a"])
        assertEquals("c_mi carpeta", decoded["com.b"])
        assertEquals(2, decoded.size)
        assertTrue(Codec.decodeMap("basura sin separadores").isEmpty())
    }

    @Test
    fun conteos() {
        val decoded = Codec.decodeCounts(Codec.encodeCounts(mapOf("com.a" to 3, "com.b" to 0)))
        assertEquals(3, decoded["com.a"])
        assertEquals(0, decoded["com.b"])
        assertTrue(Codec.decodeCounts("com.x\u001Fno-es-numero").isEmpty())
    }

    @Test
    fun carpetasDelUsuario() {
        val customs = listOf(CustomCategory("c_universidad", "Universidad"), CustomCategory("c_casa", "Casa"))
        assertEquals(customs, Codec.decodeCustoms(Codec.encodeCustoms(customs)))
        // Claves que no son carpetas del usuario se descartan.
        assertTrue(Codec.decodeCustoms("juegos\u001FJuegos").isEmpty())
    }

    @Test
    fun nombresEIdsNuevos() {
        assertEquals("Mi carpeta", Codec.cleanName("  Mi \n  carpeta  "))
        assertEquals(30, Codec.cleanName("x".repeat(50)).length)
        assertEquals("c_mis_ramos", Codec.newCustomId("Mis Ramos", emptyList()))
        assertEquals("c_mis_ramos_2", Codec.newCustomId("mis ramos", listOf("c_mis_ramos")))
        assertEquals("c_mis_ramos_3", Codec.newCustomId("mis ramos", listOf("c_mis_ramos", "c_mis_ramos_2")))
        assertEquals("c_carpeta", Codec.newCustomId("⭐⭐", emptyList()))
    }
}
