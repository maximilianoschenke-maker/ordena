# Ordena

Pantalla de inicio (launcher) para Android que **ordena tus apps en carpetas por tema**, de forma automática y sin internet.

- Clasifica cada app usando la categoría que la propia app declara, lo que sabe hacer (llamar, abrir la cámara, navegar…), un diccionario de apps conocidas y las palabras de su nombre («banco», «fit», «game»…).
- **Configura tus carpetas escribiéndolas:** «juegos», «bancos», «redes sociales», «delivery», «micro», «gym», «plata», «rrss»… Entiende tildes, plurales, español e inglés. Si escribes algo que no conoce, crea la carpeta igual.
- **Aprende de ti:** mantén presionada una app para moverla de carpeta y Ordena lo recuerda.
- **Apps nuevas:** cuando instalas algo, lo guarda solo y te cuenta dónde y por qué.
- Diseño negro sobrio, íconos monocromáticos opcionales, búsqueda que entiende categorías.
- Sin permisos especiales, sin internet, sin anuncios.

## Instalar en tu teléfono

1. Desde el teléfono abre la página **Releases** de este repositorio (o el enlace directo `releases/latest/download/ordena.apk`).
2. Toca `ordena.apk` para descargarlo y ábrelo.
3. Si Android lo pide, permite **instalar apps de esta fuente** (es el permiso para apps fuera de Play Store).
4. Toca **Instalar** y abre Ordena.
5. En la bienvenida toca **Elegir Ordena como inicio** (o ve a *Ajustes del teléfono → Apps → Apps predeterminadas → App de inicio → Ordena*).

Para volver a tu pantalla de inicio anterior, elígela en ese mismo menú. Para desinstalar Ordena, hazlo como con cualquier app.

Las actualizaciones se instalan encima de la versión anterior sin perder tu configuración.

## Cómo se construye

Cada cambio en `main` dispara GitHub Actions (`.github/workflows/android.yml`), que:

1. Corre las pruebas unitarias del clasificador y de las categorías (`app/src/test`).
2. Compila el APK debug y el APK final (release, optimizado con R8).
3. Levanta un emulador Android 14 y corre las pruebas de interfaz (`app/src/androidTest`): bienvenida, carpetas, búsqueda, configurar categorías escribiéndolas y mover una app.
4. Instala el APK final en el emulador, lo elige como pantalla de inicio, lo recorre, guarda capturas y falla si la app se cae.
5. Si todo pasó, publica `ordena.apk` en **Releases**.

Para compilar localmente: Android Studio (JDK 17) → abrir la carpeta → *Run*. O `./gradlew assembleRelease`.

## Estructura

```
app/src/main/java/app/ordena/
├── domain/        Lógica pura (sin Android, con pruebas unitarias)
│   ├── CategoryCatalog.kt   27 categorías: nombres, sinónimos, palabras clave, ícono, respaldo
│   ├── CategoryMatcher.kt   Entiende lo que el usuario escribe ("bancos" → Bancos y finanzas)
│   ├── Classifier.kt        Puntajes por señal (app conocida, categoría declarada, capacidades, nombre)
│   ├── KnownApps.kt         Paquetes de apps populares
│   ├── Organizer.kt         Carpeta final según tu configuración y lo que moviste a mano
│   ├── TextNormalizer.kt    Minúsculas, sin tildes, plurales
│   └── Codec.kt             Guardado robusto de la configuración
├── data/          Apps instaladas (LauncherApps) y configuración (SharedPreferences)
├── ui/            Pantallas en Jetpack Compose
├── HomeRole.kt    Ser (o pedir ser) la pantalla de inicio
└── MainActivity.kt
```

## Firma del APK

El APK se firma con `app/ordena-release.jks` (incluida en el repositorio) para que cada actualización se instale encima de la anterior. Es adecuada para uso personal. Si vas a distribuir la app, crea tu propia clave y pásala con los secretos `ORDENA_KEYSTORE_PASSWORD`, `ORDENA_KEY_ALIAS` y `ORDENA_KEY_PASSWORD` (y reemplaza el archivo).

## Licencias

Código: MIT. Fuente Geist: SIL Open Font License 1.1 (`FONT-LICENSE-Geist.txt`).
