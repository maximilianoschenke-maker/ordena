package app.ordena.domain

/**
 * Una categoría (carpeta) que Ordena conoce.
 *
 * @param synonyms frases que el usuario puede escribir para referirse a ella ("bancos", "plata").
 * @param keywords palabras que, si aparecen en el nombre o el identificador de una app, sugieren
 *        que la app pertenece aquí ("banco", "wallet", "pay").
 * @param fallbacks si el usuario quita esta carpeta, sus apps van a la primera de estas que siga activa.
 * @param iconPath trazo SVG (viewBox 24x24) del ícono de línea.
 */
data class CategoryDef(
    val id: String,
    val name: String,
    val shortName: String,
    val synonyms: List<String>,
    val keywords: List<String>,
    val fallbacks: List<String>,
    val iconPath: String
)

/** Carpeta creada por el usuario con un nombre que Ordena no reconoció. */
data class CustomCategory(val id: String, val name: String)

object CategoryCatalog {

    const val OTHER = "otras"
    const val OTHER_NAME = "Otras apps"
    const val CUSTOM_PREFIX = "c_"
    const val FOLDER_ICON = "M3 6h7l2 2h9v11H3z"

    private fun def(
        id: String,
        name: String,
        shortName: String,
        synonyms: String,
        keywords: String,
        fallbacks: String,
        iconPath: String
    ) = CategoryDef(
        id = id,
        name = name,
        shortName = shortName,
        // Las frases van separadas por "|"; se normalizan aquí para no depender de cómo se escribieron.
        synonyms = synonyms.split('|').map { TextNormalizer.normalize(it) }.filter { it.isNotEmpty() }.distinct(),
        keywords = keywords.split('|').map { TextNormalizer.normalize(it) }.filter { it.isNotEmpty() }.distinct(),
        fallbacks = fallbacks.split('|').map { it.trim() }.filter { it.isNotEmpty() },
        iconPath = iconPath
    )

    /** Orden de la lista = orden de preferencia cuando dos categorías empatan. */
    val all: List<CategoryDef> = listOf(
        def(
            "mensajeria",
            "Mensajería",
            "Mensajería",
            "mensajeria|mensajes|mensaje|chat|chats|sms|llamadas|telefono|contactos|messaging|messages|calls",
            "messenger|message|messages|mensajes|mensajeria|chat|sms|dialer|phone|telefono|contacts|contactos|calls|llamadas|telegram|whatsapp",
            "social",
            "M4 5h16v11H9l-5 4z"
        ),
        def(
            "social",
            "Redes sociales",
            "Redes sociales",
            "redes sociales|redes|red social|social|sociales|rrss|social media|networks",
            "social|facebook|instagram|tiktok|twitter|threads|snapchat|pinterest|linkedin|reddit|tumblr|mastodon|bluesky|community|comunidad",
            "mensajeria",
            "M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM3 20a6 6 0 0 1 12 0M16 5.2a3 3 0 0 1 0 5.6M21 20a6 6 0 0 0-3.5-5.5"
        ),
        def(
            "fotos",
            "Fotos y cámara",
            "Fotos",
            "fotos|foto|camara|galeria|fotografia|imagenes|photos|photo|camera|gallery|selfies",
            "photo|photos|foto|fotos|camera|camara|gallery|galeria|selfie|pics|collage|retouch|lightroom|vsco|picsart|snapseed|canva",
            "video",
            "M4 8h3l2-3h6l2 3h3v11H4zM12 16a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"
        ),
        def(
            "video",
            "Video y streaming",
            "Video",
            "video|videos|streaming|series|peliculas|pelis|tv|television|cine|movies|films",
            "video|videos|tv|movie|movies|cine|cinema|stream|streaming|series|netflix|youtube|twitch|vlc|iptv|anime",
            "musica|fotos",
            "M3 5h18v14H3zM10 9l5 3-5 3z"
        ),
        def(
            "musica",
            "Música y podcasts",
            "Música",
            "musica|canciones|audio|podcasts|podcast|radio|music|songs",
            "music|musica|radio|podcast|podcasts|audio|song|songs|sound|fm|spotify|deezer|karaoke|lyrics|playlist|equalizer|ecualizador",
            "video",
            "M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM20 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z"
        ),
        def(
            "juegos",
            "Juegos",
            "Juegos",
            "juegos|juego|videojuegos|jueguitos|games|game|gaming|arcade",
            "game|games|juego|juegos|gaming|puzzle|chess|ajedrez|solitaire|solitario|sudoku|crossword|crucigrama|casino|poker|racing|arcade|clash|shooter|rpg|trivia|bingo|slots|mahjong|domino",
            "ninos",
            "M7 8h10a5 5 0 0 1 5 5v1a3 3 0 0 1-5.4 1.8L15 14H9l-1.6 1.8A3 3 0 0 1 2 14v-1a5 5 0 0 1 5-5zM7 10.5v3M5.5 12h3M16 11h.01M18 13h.01"
        ),
        def(
            "compras",
            "Compras",
            "Compras",
            "compras|compra|tiendas|tienda|shopping|ofertas|ecommerce|comprar|retail|marketplace|supermercado",
            "shop|shopping|tienda|tiendas|market|marketplace|mall|deals|ofertas|outlet|coupon|cupones|retail|compras|aliexpress|amazon|ebay|temu|shein|mercadolibre|falabella|ripley|lider|jumbo|sodimac|supermercado",
            "comida",
            "M5 8h14l-1 12H6zM9 8V6a3 3 0 0 1 6 0v2"
        ),
        def(
            "finanzas",
            "Bancos y finanzas",
            "Bancos",
            "bancos|banco|finanzas|finanza|dinero|plata|pagos|pago|billetera|wallet|tarjetas|tarjeta|inversiones|ahorro|bank|banking|finance|money",
            "bank|banking|banco|bancos|bancaria|finance|finanzas|financial|wallet|billetera|pay|pago|pagos|money|dinero|credit|credito|tarjeta|invest|inversiones|broker|trading|stocks|bolsa|ahorro|savings|seguros|cuentarut|tenpo|fintual|mercadopago|paypal|bci|santander|itau|scotiabank|bice|bancoestado|afp",
            "",
            "M3 10h18M5 10v8M9.5 10v8M14.5 10v8M19 10v8M3 20h18M12 3l9 5H3z"
        ),
        def(
            "comida",
            "Comida y delivery",
            "Comida",
            "comida|comidas|delivery|restaurantes|restaurant|pedidos|food|recetas|cocina",
            "food|foods|comida|delivery|eats|pedidos|pedidosya|rappi|restaurant|restaurante|restaurantes|pizza|burger|sushi|recetas|recipes|cocina|cooking",
            "compras",
            "M7 3v8a2 2 0 0 0 4 0V3M9 13v8M17 3c-2 0-3 2-3 5s1 4 3 4v9"
        ),
        def(
            "transporte",
            "Transporte",
            "Transporte",
            "transporte|movilidad|taxi|taxis|auto|autos|micro|micros|metro|bus|buses|bicicleta|scooter|estacionamiento|bencina|transport",
            "taxi|ride|rides|cab|uber|cabify|didi|moovit|transit|transporte|bus|metro|micro|bike|bici|scooter|parking|estacionamiento|bencina|gasolina|fuel|copec|movilidad|bip",
            "mapas|viajes",
            "M5 17V11l2-5h10l2 5v6zM3 17h18M7 17v2M17 17v2M5 11h14"
        ),
        def(
            "mapas",
            "Mapas y navegación",
            "Mapas",
            "mapas|mapa|gps|navegacion|maps|rutas|direcciones|navigation",
            "map|maps|mapa|mapas|gps|navigation|navegacion|waze|navigator",
            "viajes|transporte",
            "M12 21s-7-6.5-7-12a7 7 0 0 1 14 0c0 5.5-7 12-7 12zM12 11a2 2 0 1 0 0-4 2 2 0 0 0 0 4z"
        ),
        def(
            "viajes",
            "Viajes",
            "Viajes",
            "viajes|viaje|vuelos|vuelo|hoteles|hotel|turismo|travel|vacaciones|aerolineas|trips",
            "travel|viajes|viaje|trip|trips|flight|flights|vuelos|vuelo|hotel|hotels|hoteles|booking|airbnb|airline|airlines|aerolinea|latam|jetsmart|despegar|expedia|trivago|tourism|turismo|hostel",
            "mapas|transporte",
            "M10.5 13.5 3 11l18-7-7 18-2.5-7.5zM10.5 13.5 21 4"
        ),
        def(
            "salud",
            "Salud",
            "Salud",
            "salud|bienestar|medico|medicos|doctor|farmacia|meditacion|health|clinica|sueno|mental",
            "health|salud|medic|medical|medico|clinic|clinica|doctor|pharmacy|farmacia|hospital|meditation|meditacion|mindfulness|sleep|sueno|calm|wellness|bienestar|isapre|fonasa",
            "deporte",
            "M12 8v8M8 12h8M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z"
        ),
        def(
            "deporte",
            "Deporte y fitness",
            "Deporte",
            "deporte|deportes|fitness|ejercicio|ejercicios|gimnasio|gym|correr|running|entrenamiento|futbol|sports",
            "fit|fitness|workout|workouts|gym|run|running|runner|sport|sports|deporte|deportes|futbol|soccer|football|yoga|pilates|strava|steps|pasos|training|entrenamiento|cycling|ciclismo|fitbit",
            "salud",
            "M6 7v10M18 7v10M3 10v4M21 10v4M6 12h12"
        ),
        def(
            "noticias",
            "Noticias",
            "Noticias",
            "noticias|noticia|diarios|diario|prensa|news|periodicos|actualidad",
            "news|noticias|noticia|diario|diarios|prensa|cnn|bbc|emol|latercera|biobio|cooperativa|t13|24horas",
            "lectura",
            "M4 5h13v14H6a2 2 0 0 1-2-2zM17 9h3v8a2 2 0 0 1-2 2M7 9h7M7 13h7M7 16h4"
        ),
        def(
            "lectura",
            "Lectura y libros",
            "Lectura",
            "lectura|libros|libro|leer|ebooks|comics|revistas|books|mangas|audiolibros",
            "books|libro|libros|ebook|ebooks|reader|read|lectura|kindle|comic|comics|manga|wattpad|audible|audiobook|audiobooks|audiolibros|magazine|revistas|bible|biblia",
            "noticias",
            "M4 19V5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2zM8 7h7"
        ),
        def(
            "educacion",
            "Educación",
            "Educación",
            "educacion|estudio|estudios|estudiar|cursos|curso|idiomas|colegio|universidad|clases|aprender|learning|school",
            "edu|education|educacion|learn|learning|school|colegio|classroom|clases|course|courses|curso|cursos|duolingo|language|idiomas|ingles|english|math|mates|matematicas|study|estudio|university|universidad|tutor|flashcards|dictionary|diccionario",
            "productividad",
            "M2 9l10-5 10 5-10 5zM6 11v5c3 2 9 2 12 0v-5"
        ),
        def(
            "productividad",
            "Trabajo y productividad",
            "Trabajo",
            "trabajo|productividad|oficina|notas|tareas|documentos|calendario|agenda|correo|email|work|office",
            "office|docs|document|documents|documentos|sheets|slides|excel|powerpoint|pdf|scanner|escaner|mail|email|gmail|outlook|correo|calendar|calendario|agenda|notes|notas|note|tasks|tareas|dropbox|onedrive|zoom|slack|notion|trello|asana|jira|chatgpt|gemini|copilot|claude|scan|recordatorios|reminders|reminder",
            "herramientas",
            "M4 4h16v16H4zM8 12l3 3 5-6"
        ),
        def(
            "herramientas",
            "Herramientas",
            "Herramientas",
            "herramientas|utilidades|utilidad|tools|sistema|archivos|ajustes|utilities",
            "settings|ajustes|configuracion|calculator|calculadora|clock|reloj|alarm|alarma|files|file|archivos|flashlight|linterna|torch|browser|navegador|chrome|firefox|opera|internet|launcher|cleaner|keyboard|teclado|translate|traductor|recorder|grabadora|compass|brujula|qr|barcode|battery|bateria|wifi|bluetooth|backup|tools|toolbox|utility|utilities|entel|movistar|wom|vtr|claro",
            "",
            "M4 7h10M18 7h2M4 17h4M12 17h8M16 5v4M10 15v4"
        ),
        def(
            "clima",
            "Clima",
            "Clima",
            "clima|tiempo|meteorologia|weather|pronostico",
            "weather|clima|meteo|meteorologia|forecast|pronostico|lluvia|radar|accuweather|tiempo",
            "herramientas",
            "M7 18h10a4 4 0 0 0 0-8 6 6 0 0 0-11.5 1.5A3.5 3.5 0 0 0 7 18z"
        ),
        def(
            "hogar",
            "Hogar inteligente",
            "Hogar",
            "hogar|casa|domotica|smart home|luces|hogar inteligente|home",
            "smarthome|smart home|domotica|alexa|hue|tuya|smartthings|smartlife|nest|iot|ewelink|roomba",
            "herramientas",
            "M3 11l9-7 9 7v9H3zM9 20v-6h6v6"
        ),
        def(
            "citas",
            "Citas",
            "Citas",
            "citas|cita|dating|pareja|ligar",
            "dating|citas|tinder|bumble|hinge|badoo|happn|grindr|okcupid",
            "social",
            "M12 20s-8-5-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 9c0 6-8 11-8 11z"
        ),
        def(
            "cripto",
            "Criptomonedas",
            "Cripto",
            "cripto|criptomonedas|criptomoneda|bitcoin|crypto|ethereum",
            "crypto|cripto|bitcoin|btc|ethereum|binance|coinbase|blockchain|buda|kraken|metamask",
            "finanzas",
            "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM10 8h3a2 2 0 0 1 0 4h-3zM10 12h4a2 2 0 0 1 0 4h-4zM10 7v10"
        ),
        def(
            "ninos",
            "Niños y familia",
            "Niños",
            "ninos|nino|ninas|infantil|familia|kids|hijos|juegos para ninos|bebes",
            "kids|kid|ninos|infantil|child|children|baby|bebe|family|familia|junior|toddler|cartoon|crianza",
            "juegos",
            "M12 3a5 5 0 0 1 5 5c0 4-5 7-5 7s-5-3-5-7a5 5 0 0 1 5-5zM12 15v6"
        ),
        def(
            "seguridad",
            "Seguridad",
            "Seguridad",
            "seguridad|antivirus|contrasenas|claves|vpn|privacidad|security|passwords",
            "security|seguridad|antivirus|virus|password|passwords|contrasenas|authenticator|auth|2fa|vpn|privacy|privacidad|bitwarden|lastpass|keepass|nordvpn|applock|firewall",
            "herramientas",
            "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"
        ),
        def(
            "tramites",
            "Trámites y gobierno",
            "Trámites",
            "tramites|tramite|gobierno|servicios publicos|municipalidad|impuestos|gob",
            "gob|gov|gobierno|government|tramites|tramite|municipalidad|comuna|municipal|sii|chileatiende|registro|claveunica|impuestos|tax|taxes|licencia",
            "productividad",
            "M6 3h9l4 4v14H6zM14 3v5h5M9 13h7M9 17h5"
        ),
        def(
            "personalizacion",
            "Personalización",
            "Personalizar",
            "personalizacion|fondos|fondos de pantalla|temas|wallpapers|iconos|widgets",
            "wallpaper|wallpapers|fondos|theme|themes|temas|icons|iconos|widget|widgets|kwgt|klwp|zedge|ringtones|tonos",
            "herramientas",
            "M12 3a9 9 0 0 0 0 18c1.5 0 2-1 2-2s-1-1.5-1-2.5S14 15 15 15h2a4 4 0 0 0 4-4c0-4.4-4-8-9-8zM7.5 11h.01M12 7h.01M16.5 11h.01"
        )
    )

    val byId: Map<String, CategoryDef> = all.associateBy { it.id }

    /** Carpetas con que parte la app. Las demás se pueden agregar escribiéndolas. */
    val defaultActive: List<String> = listOf(
        "mensajeria", "social", "fotos", "video", "musica", "juegos", "compras", "comida",
        "finanzas", "transporte", "viajes", "productividad", "salud", "noticias", "herramientas"
    )

    /** Sugerencias que se muestran al configurar, en este orden. */
    val suggestionOrder: List<String> = listOf(
        "mapas", "deporte", "educacion", "lectura", "clima",
        "hogar", "seguridad", "cripto", "tramites", "citas", "ninos", "personalizacion"
    ) + defaultActive

    fun isCustom(id: String): Boolean = id.startsWith(CUSTOM_PREFIX)

    init {
        // Errores de catálogo deben detectarse al instante (y en los tests), no en el teléfono.
        require(byId.size == all.size) { "Hay ids de categoría repetidos" }
        all.forEach { cat ->
            require(!cat.id.startsWith(CUSTOM_PREFIX) && cat.id != OTHER) { "Id reservado: ${cat.id}" }
            require(cat.fallbacks.all { it in byId && it != cat.id }) { "Fallback inválido en ${cat.id}" }
            require(cat.synonyms.isNotEmpty() && cat.keywords.isNotEmpty()) { "Categoría vacía: ${cat.id}" }
        }
        require(defaultActive.all { it in byId })
    }
}
