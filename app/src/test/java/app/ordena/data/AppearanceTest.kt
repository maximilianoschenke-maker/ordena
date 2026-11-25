package app.ordena.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceTest {

    @Test
    fun tituloLimpio() {
        assertEquals("Mi teléfono", AppearanceDefaults.cleanTitle("  Mi \n teléfono  "))
        assertEquals("BuenOrden", AppearanceDefaults.cleanTitle("   "))
        assertEquals(AppearanceDefaults.TITLE_MAX_LENGTH, AppearanceDefaults.cleanTitle("x".repeat(40)).length)
    }

    @Test
    fun idsDesconocidosVuelvenAlPredeterminado() {
        assertEquals(TitleEffect.DISINTEGRATE, TitleEffect.fromId(null))
        assertEquals(TitleEffect.DISINTEGRATE, TitleEffect.fromId("otro"))
        assertEquals(TitleEffect.SPEED, TitleEffect.fromId("velocidad"))
        assertEquals(FolderSize.NORMAL, FolderSize.fromId("raro"))
        assertEquals(FolderSize.LARGE, FolderSize.fromId("grande"))
    }
}
