package app.ordena.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryMatcherTest {

    private fun id(text: String, customs: List<CustomCategory> = emptyList()) = CategoryMatcher.match(text, customs)?.id

    @Test
    fun entiendeLosEjemplosDelUsuario() {
        assertEquals("juegos", id("juegos"))
        assertEquals("finanzas", id("bancos"))
        assertEquals("social", id("redes sociales"))
    }

    @Test
    fun ignoraMayusculasTildesYEspacios() {
        assertEquals("social", id("  Redes   Sociales "))
        assertEquals("musica", id("Música"))
        assertEquals("ninos", id("NIÑOS"))
        assertEquals("tramites", id("Trámites"))
        assertEquals("educacion", id("educación"))
    }

    @Test
    fun ignoraPlurales() {
        assertEquals("juegos", id("juego"))
        assertEquals("finanzas", id("banco"))
        assertEquals("social", id("red social"))
        assertEquals("viajes", id("viaje"))
    }

    @Test
    fun entiendePalabrasCotidianasYEnIngles() {
        assertEquals("finanzas", id("plata"))
        assertEquals("transporte", id("micro"))
        assertEquals("video", id("pelis"))
        assertEquals("deporte", id("gym"))
        assertEquals("social", id("rrss"))
        assertEquals("comida", id("delivery"))
        assertEquals("juegos", id("games"))
        assertEquals("finanzas", id("banking"))
        assertEquals("musica", id("music"))
        assertEquals("clima", id("tiempo"))
    }

    @Test
    fun entiendeMientrasSeEscribe() {
        assertEquals("finanzas", id("ban"))
        assertEquals("juegos", id("jue"))
        assertEquals("social", id("redes soc"))
    }

    @Test
    fun encuentraLaCategoriaDentroDeUnaFrase() {
        assertEquals("finanzas", id("apps de bancos"))
        assertEquals("juegos", id("mis juegos"))
    }

    @Test
    fun laFraseExactaGanaALaContenida() {
        // "juegos para niños" es sinónimo exacto de Niños; "juegos" solo está contenido.
        assertEquals("ninos", id("juegos para niños"))
    }

    @Test
    fun noInventaCategorias() {
        assertNull(id("xyzzy"))
        assertNull(id("a"))
        assertNull(id(""))
        assertNull(id("   "))
        assertNull(id("!!"))
    }

    @Test
    fun cadaSinonimoLlevaASuPropiaCategoria() {
        CategoryCatalog.all.forEach { cat ->
            cat.synonyms.forEach { synonym ->
                val match = CategoryMatcher.match(synonym)
                assertEquals("«$synonym» debería ser ${cat.id}", cat.id, match?.id)
                assertEquals("«$synonym» debería ser exacto", 3, match?.strength)
            }
            assertEquals(cat.id, id(cat.name))
        }
    }

    @Test
    fun lasCarpetasDelUsuarioGananEmpates() {
        val customs = listOf(CustomCategory("c_universidad", "Universidad"))
        val match = CategoryMatcher.match("universidad", customs)
        assertEquals("c_universidad", match?.id)
        assertTrue(match!!.isCustom)
        assertEquals("c_universidad", id("Universidad", customs))
    }

    @Test
    fun reconoceCarpetasRenombradas() {
        val renamed = mapOf("finanzas" to "Mi Plata", "herramientas" to "Utilidades del cel")
        val match = CategoryMatcher.match("mi plata", renamed = renamed)
        assertEquals("finanzas", match?.id)
        assertEquals("Mi Plata", match?.name)
        // Los sinónimos originales siguen funcionando y devuelven el nombre nuevo.
        assertEquals("Mi Plata", CategoryMatcher.match("bancos", renamed = renamed)?.name)
        assertEquals("herramientas", CategoryMatcher.match("utilidades del cel", renamed = renamed)?.id)
        // Un nombre vacío no rompe nada.
        assertEquals("Juegos", CategoryMatcher.match("juegos", renamed = mapOf("juegos" to ""))?.name)
    }
}
