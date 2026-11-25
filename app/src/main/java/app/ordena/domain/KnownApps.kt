package app.ordena.domain

/**
 * Apps populares cuyo identificador de paquete se conoce con certeza.
 * Solo se incluyen paquetes verificables; el resto se clasifica con las demás señales.
 * Las claves se comparan en minúsculas.
 */
object KnownApps {

    private val byCategory: Map<String, List<String>> = mapOf(
        "mensajeria" to listOf(
            "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.thoughtcrime.securesms",
            "com.facebook.orca", "com.facebook.mlite", "com.google.android.apps.messaging",
            "com.samsung.android.messaging", "com.android.mms", "com.google.android.dialer",
            "com.android.dialer", "com.samsung.android.dialer", "com.google.android.contacts",
            "com.android.contacts", "com.samsung.android.app.contacts", "com.discord",
            "jp.naver.line.android", "com.viber.voip", "com.skype.raider", "com.google.android.apps.tachyon",
            "com.tencent.mm"
        ),
        "social" to listOf(
            "com.facebook.katana", "com.facebook.lite", "com.instagram.android", "com.instagram.barcelona",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.twitter.android",
            "com.snapchat.android", "com.pinterest", "com.linkedin.android", "com.reddit.frontpage",
            "com.tumblr", "com.bereal.ft", "xyz.blueskyweb.app", "org.joinmastodon.android"
        ),
        "musica" to listOf(
            "com.spotify.music", "com.google.android.apps.youtube.music", "com.soundcloud.android",
            "com.shazam.android", "deezer.android.app", "com.amazon.mp3", "com.apple.android.music",
            "au.com.shiftyjelly.pocketcasts", "com.google.android.apps.podcasts", "com.aspiro.tidal",
            "fm.castbox.audiobook.radio.podcast"
        ),
        "video" to listOf(
            "com.google.android.youtube", "com.netflix.mediaclient", "com.disney.disneyplus",
            "com.amazon.avod.thirdpartyclient", "com.wbd.stream", "com.hbo.hbonow", "tv.twitch.android.app",
            "com.plexapp.android", "com.google.android.videos", "org.videolan.vlc",
            "com.mxtech.videoplayer.ad", "com.lemon.lvoverseas", "com.crunchyroll.crunchyroid",
            "com.google.android.apps.youtube.creator"
        ),
        "fotos" to listOf(
            "com.google.android.apps.photos", "com.google.android.googlecamera", "com.sec.android.app.camera",
            "com.sec.android.gallery3d", "com.android.camera", "com.android.camera2", "com.android.gallery3d",
            "com.adobe.lrmobile", "com.vsco.cam", "com.picsart.studio", "com.niksoftware.snapseed",
            "com.miui.gallery"
        ),
        "juegos" to listOf(
            "com.roblox.client", "com.mojang.minecraftpe", "com.activision.callofduty.shooter",
            "com.dts.freefireth", "com.dts.freefiremax", "com.tencent.ig", "com.innersloth.spacemafia",
            "com.chess", "com.nianticlabs.pokemongo", "com.mobile.legends"
        ),
        "compras" to listOf(
            "com.mercadolibre",
            "com.amazon.mshop.android.shopping",
            "com.alibaba.aliexpresshd",
            "com.einnovation.temu",
            "com.zzkko",
            "com.ebay.mobile"
        ),
        "finanzas" to listOf(
            "com.mercadopago.wallet",
            "com.google.android.apps.walletnfcrel",
            "com.paypal.android.p2pmobile",
            "com.samsung.android.spay",
            "com.revolut.revolut",
            "com.transferwise.android"
        ),
        "cripto" to listOf(
            "com.binance.dev",
            "com.coinbase.android",
            "com.wallet.crypto.trustapp",
            "io.metamask"
        ),
        "comida" to listOf("com.ubercab.eats", "com.grability.rappi", "com.pedidosya"),
        "transporte" to listOf("com.ubercab", "com.didiglobal.passenger", "com.cabify.rider", "com.tranzmate"),
        "mapas" to listOf(
            "com.google.android.apps.maps",
            "com.waze",
            "com.here.app.maps",
            "net.osmand",
            "app.organicmaps"
        ),
        "viajes" to listOf(
            "com.airbnb.android",
            "com.booking",
            "com.tripadvisor.tripadvisor",
            "com.expedia.bookings",
            "com.trivago"
        ),
        "salud" to listOf(
            "com.sec.android.app.shealth",
            "com.calm.android",
            "com.getsomeheadspace.android",
            "com.google.android.apps.healthdata"
        ),
        "deporte" to listOf(
            "com.google.android.apps.fitness",
            "com.strava",
            "com.nike.plusgps",
            "com.fitbit.fitbitmobile",
            "com.myfitnesspal.android",
            "com.garmin.android.apps.connectmobile"
        ),
        "noticias" to listOf(
            "com.google.android.apps.magazines",
            "flipboard.app",
            "com.cnn.mobile.android.phone",
            "bbc.mobile.news.ww",
            "com.nytimes.android"
        ),
        "lectura" to listOf(
            "com.amazon.kindle",
            "com.google.android.apps.books",
            "com.audible.application",
            "wp.wattpad",
            "com.medium.reader"
        ),
        "educacion" to listOf(
            "com.duolingo",
            "com.google.android.apps.classroom",
            "com.microblink.photomath",
            "com.quizlet.quizletandroid",
            "org.khanacademy.android"
        ),
        "productividad" to listOf(
            "com.google.android.gm", "com.microsoft.office.outlook", "com.google.android.calendar",
            "com.samsung.android.calendar", "com.google.android.apps.docs", "com.google.android.apps.docs.editors.docs",
            "com.google.android.apps.docs.editors.sheets", "com.google.android.apps.docs.editors.slides",
            "com.google.android.keep", "com.microsoft.teams", "us.zoom.videomeetings", "com.slack",
            "com.notion.id", "com.microsoft.office.word", "com.microsoft.office.excel",
            "com.microsoft.office.powerpoint", "com.microsoft.office.officehubrow", "com.todoist", "com.trello",
            "com.dropbox.android", "com.microsoft.skydrive", "com.adobe.reader", "com.openai.chatgpt",
            "com.anthropic.claude", "com.google.android.apps.bard", "com.microsoft.copilot",
            "com.samsung.android.app.notes"
        ),
        "herramientas" to listOf(
            "com.android.settings", "com.google.android.deskclock", "com.sec.android.app.clockpackage",
            "com.android.deskclock", "com.google.android.calculator", "com.sec.android.app.popupcalculator",
            "com.android.calculator2", "com.google.android.apps.nbu.files", "com.sec.android.app.myfiles",
            "com.android.documentsui", "com.android.vending", "com.google.android.googlequicksearchbox",
            "com.google.android.apps.translate", "com.android.chrome", "org.mozilla.firefox", "com.brave.browser",
            "com.opera.browser", "com.microsoft.emmx", "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android", "com.google.android.apps.googleassistant", "com.android.stk",
            "com.google.android.apps.recorder", "com.sec.android.app.voicenote", "com.android.soundrecorder",
            "com.sec.android.app.samsungapps", "com.google.android.apps.subscriptions.red"
        ),
        "clima" to listOf("com.weather.weather", "com.accuweather.android", "com.sec.android.daemonapp"),
        "hogar" to listOf(
            "com.google.android.apps.chromecast.app",
            "com.amazon.dee.app",
            "com.xiaomi.smarthome",
            "com.philips.lighting.hue2",
            "com.samsung.android.oneconnect",
            "com.tuya.smartlife",
            "com.tuya.smart"
        ),
        "citas" to listOf(
            "com.tinder",
            "com.bumble.app",
            "co.hinge.app",
            "com.badoo.mobile",
            "com.grindrapp.android",
            "com.ftw_and_co.happn"
        ),
        "seguridad" to listOf(
            "com.google.android.apps.authenticator2",
            "com.x8bit.bitwarden",
            "com.lastpass.lpandroid",
            "com.nordvpn.android",
            "com.kunzisoft.keepass.free",
            "com.azure.authenticator",
            "com.avast.android.mobilesecurity"
        ),
        "ninos" to listOf("com.google.android.apps.youtube.kids", "com.google.android.apps.kids.familylink"),
        "personalizacion" to listOf("net.zedge.android", "org.kustom.widget", "com.google.android.apps.wallpaper")
    )

    /** Prefijos de estudios de juegos: todo lo que publican es un juego. */
    private val gamePublisherPrefixes = listOf(
        "com.supercell.", "com.king.", "com.gameloft.", "com.miniclip.", "com.ea.gp.", "com.ea.game.",
        "com.rovio.", "com.zynga.", "com.playrix.", "com.outfit7.", "com.kiloo.", "com.imangi.", "com.halfbrick."
    )

    private val exact: Map<String, String> = buildMap {
        byCategory.forEach { (category, packages) ->
            packages.forEach { pkg ->
                val key = pkg.lowercase()
                check(put(key, category) == null) { "Paquete repetido en KnownApps: $pkg" }
            }
        }
    }

    enum class MatchKind { EXACT, PREFIX }

    data class Match(val categoryId: String, val kind: MatchKind)

    fun lookup(packageName: String): Match? {
        val pkg = packageName.lowercase()
        exact[pkg]?.let { return Match(it, MatchKind.EXACT) }
        if (gamePublisherPrefixes.any { pkg.startsWith(it) }) return Match("juegos", MatchKind.PREFIX)
        return null
    }

    /** Para tests: todas las categorías usadas deben existir en el catálogo. */
    internal fun categoriesUsed(): Set<String> = byCategory.keys + "juegos"
}
