package app.ordena.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifierTest {

    private val defaults = CategoryCatalog.defaultActive

    private fun top(app: AppSignals) = Classifier.classify(app).ranked.firstOrNull()?.categoryId

    private fun placeDefault(app: AppSignals, overrides: Map<String, String> = emptyMap()) = Organizer.place(app, Classifier.classify(app), defaults, emptyList(), overrides)

    @Test
    fun appsConocidas() {
        assertEquals("mensajeria", top(AppSignals("com.whatsapp", "WhatsApp")))
        assertEquals("social", top(AppSignals("com.instagram.android", "Instagram")))
        assertEquals("musica", top(AppSignals("com.spotify.music", "Spotify")))
        assertEquals("productividad", top(AppSignals("com.google.android.gm", "Gmail")))
        assertEquals("herramientas", top(AppSignals("com.android.chrome", "Chrome")))
        assertEquals("productividad", top(AppSignals("com.Slack", "Slack")))
    }

    @Test
    fun appConocidaGanaAPalabrasEnganosas() {
        // El paquete de TikTok contiene "musically", pero es una red social.
        assertEquals("social", top(AppSignals("com.zhiliaoapp.musically", "TikTok")))
        // Photomath contiene "photo", pero es educación.
        assertEquals("educacion", top(AppSignals("com.microblink.photomath", "Photomath")))
    }

    @Test
    fun bancosPorNombreAunqueNoSeanConocidos() {
        assertEquals("finanzas", top(AppSignals("cl.bancosur.app", "Banco Sur")))
        assertEquals("finanzas", top(AppSignals("cl.example.cuentarut", "BancoEstado")))
        assertEquals("finanzas", top(AppSignals("com.example.wallet", "Mi Billetera")))
        assertEquals("finanzas", top(AppSignals("com.example.app", "Mercado Pago")))
    }

    @Test
    fun categoriaDeclaradaPorLaApp() {
        assertEquals("juegos", top(AppSignals("com.example.x", "Zeta", DeclaredCategory.GAME)))
        assertEquals("juegos", top(AppSignals("com.example.x", "Zeta", isGameFlag = true)))
        assertEquals("noticias", top(AppSignals("com.example.x", "Zeta", DeclaredCategory.NEWS)))
        // Un juego con "Run" en el nombre sigue siendo juego.
        assertEquals("juegos", top(AppSignals("com.example.temple", "Temple Run", DeclaredCategory.GAME)))
        assertEquals("juegos", top(AppSignals("com.example.words", "Wordscapes", DeclaredCategory.GAME)))
    }

    @Test
    fun estudiosDeJuegos() {
        assertEquals("juegos", top(AppSignals("com.supercell.clashofclans", "Clash of Clans")))
        assertEquals("juegos", top(AppSignals("com.king.candycrushsaga", "Candy Crush Saga")))
    }

    @Test
    fun capacidadesDelSistema() {
        assertEquals("fotos", top(AppSignals("com.vendor.cam", "Lente", capabilities = setOf(Capability.CAMERA))))
        assertEquals("mensajeria", top(AppSignals("com.vendor.phone", "Teléfono", capabilities = setOf(Capability.DIALER))))
        assertEquals("herramientas", top(AppSignals("com.vendor.web", "Internet", capabilities = setOf(Capability.BROWSER))))
    }

    @Test
    fun appsDesconocidas() {
        assertEquals(CategoryCatalog.OTHER, placeDefault(AppSignals("com.zeta.app", "Zeta")).categoryId)
        val system = placeDefault(AppSignals("com.vendor.mystery", "Servicio X", isSystem = true))
        assertEquals("herramientas", system.categoryId)
        assertEquals("Es una app del sistema", system.reason)
    }

    @Test
    fun usaRespaldoSiLaCategoriaNoEstaActiva() {
        // Mapas y Lectura no están entre las carpetas iniciales; Comida sí.
        assertEquals("viajes", placeDefault(AppSignals("com.waze", "Waze")).categoryId)
        assertEquals("noticias", placeDefault(AppSignals("com.amazon.kindle", "Kindle")).categoryId)
        assertEquals("comida", placeDefault(AppSignals("com.ubercab.eats", "Uber Eats")).categoryId)
        // Sin Comida activa, el delivery cae en Compras.
        val sinComida = defaults - "comida"
        val eats = AppSignals("com.ubercab.eats", "Uber Eats")
        assertEquals("compras", Organizer.place(eats, Classifier.classify(eats), sinComida, emptyList(), emptyMap()).categoryId)
        // Pomo Fit → Deporte (inactiva) → Salud.
        val fit = placeDefault(AppSignals("com.pomo.fit", "Pomo Fit"))
        assertEquals("salud", fit.categoryId)
        assertEquals("Su nombre incluye «fit»", fit.reason)
    }

    @Test
    fun siAgregoLaCategoriaLaAppSeMueveSola() {
        val active = defaults + "deporte"
        val app = AppSignals("com.pomo.fit", "Pomo Fit")
        assertEquals("deporte", Organizer.place(app, Classifier.classify(app), active, emptyList(), emptyMap()).categoryId)
    }

    @Test
    fun sinRespaldoActivoVaAOtras() {
        val app = AppSignals("com.android.settings", "Ajustes")
        val active = defaults - "herramientas"
        assertEquals(CategoryCatalog.OTHER, Organizer.place(app, Classifier.classify(app), active, emptyList(), emptyMap()).categoryId)
    }

    @Test
    fun respetaLoQueElUsuarioMovio() {
        val app = AppSignals("com.whatsapp", "WhatsApp")
        val moved = placeDefault(app, mapOf("com.whatsapp" to "social"))
        assertEquals("social", moved.categoryId)
        assertTrue(moved.isUserChoice)
        // Si la carpeta elegida ya no existe, vuelve a clasificarse sola.
        val orphan = placeDefault(app, mapOf("com.whatsapp" to "c_borrada"))
        assertEquals("mensajeria", orphan.categoryId)
        assertFalse(orphan.isUserChoice)
    }

    @Test
    fun carpetaDelUsuarioPorNombre() {
        val customs = listOf(CustomCategory("c_universidad", "Universidad"))
        val app = AppSignals("cl.uchile.app", "Universidad de Chile")
        val placement = Organizer.place(app, Classifier.classify(app), defaults + "c_universidad", customs, emptyMap())
        assertEquals("c_universidad", placement.categoryId)
    }

    @Test
    fun appsChilenasComunes() {
        assertEquals("finanzas", placeDefault(AppSignals("cl.bancoestado.app", "BancoEstado")).categoryId)
        assertEquals("transporte", placeDefault(AppSignals("com.redmovilidad", "Red Movilidad")).categoryId)
        assertEquals("transporte", placeDefault(AppSignals("cl.copec.app", "Copec")).categoryId)
        assertEquals("herramientas", placeDefault(AppSignals("cl.entel.app", "Mi Entel")).categoryId)
        assertEquals("noticias", placeDefault(AppSignals("cl.emol.app", "Emol")).categoryId)
        assertEquals("productividad", placeDefault(AppSignals("cl.gob.claveunica", "ClaveÚnica")).categoryId)
        // "Claro Video" empata Video y Herramientas: gana Video por orden del catálogo.
        assertEquals("video", top(AppSignals("com.claro.video", "Claro Video")))
    }

    @Test
    fun palabrasCortasSoloExactas() {
        assertTrue(Classifier.matchesKeyword(listOf("pomo", "fit"), "fit"))
        assertFalse(Classifier.matchesKeyword(listOf("profit"), "fit"))
        assertTrue(Classifier.matchesKeyword(listOf("banking"), "bank"))
        assertTrue(Classifier.matchesKeyword(listOf("mywallet"), "wallet"))
        assertFalse(Classifier.matchesKeyword(listOf("taxi"), "tax"))
        assertTrue(Classifier.matchesKeyword(listOf("mi", "smart", "home"), "smart home"))
    }

    @Test
    fun tokensDePaquete() {
        assertEquals(listOf("com", "bancoestado", "app", "v2"), Classifier.packageTokens("com.bancoestado.app_v2"))
    }

    @Test
    fun catalogoConsistente() {
        assertTrue(KnownApps.categoriesUsed().all { it in CategoryCatalog.byId })
        CategoryCatalog.all.forEach { cat ->
            cat.keywords.forEach { assertEquals(TextNormalizer.normalize(it), it) }
            assertTrue(cat.iconPath.isNotBlank())
        }
    }
}
