package app.ordena.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import app.ordena.domain.AppSignals
import app.ordena.domain.Capability
import app.ordena.domain.DeclaredCategory
import kotlin.math.roundToInt

/** Una entrada abrible del cajón de apps (una app puede tener más de una). */
data class InstalledApp(
    /** Único por actividad y perfil (personal / trabajo). */
    val key: String,
    val packageName: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWorkProfile: Boolean,
    val signals: AppSignals
)

/**
 * Acceso a las apps instaladas mediante [LauncherApps], la API oficial para pantallas de inicio:
 * lista las actividades abribles de todos los perfiles, las abre y avisa cuando se instala,
 * actualiza o desinstala algo. No necesita permisos especiales.
 */
class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val packageManager = context.packageManager
    private val density = context.resources.displayMetrics.density
    private val densityDpi = context.resources.displayMetrics.densityDpi

    /** Tamaño en px de los íconos guardados: suficiente para el ícono más grande de la UI (56 dp). */
    private val iconSizePx = (56 * density).roundToInt().coerceAtLeast(96)

    @Volatile
    private var infoByKey: Map<String, LauncherActivityInfo> = emptyMap()

    // Hasta ~400 íconos de ~150 KB como máximo (≈ 60 MB en el peor caso, normalmente mucho menos).
    private val iconCache = object : LruCache<String, ImageBitmap>(400) {}

    /** Debe llamarse fuera del hilo principal. */
    fun loadApps(): List<InstalledApp> {
        val capabilities = loadCapabilities()
        val myUser = Process.myUserHandle()
        val newInfo = HashMap<String, LauncherActivityInfo>()
        val result = ArrayList<InstalledApp>()

        val profiles = try {
            launcherApps.profiles
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron leer los perfiles", e)
            listOf(myUser)
        }

        for (user in profiles) {
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (e: Exception) {
                // Perfil de trabajo bloqueado o en pausa: se omite sin romper la lista.
                Log.w(TAG, "No se pudo leer el perfil $user", e)
                emptyList()
            }
            val serial = try {
                userManager.getSerialNumberForUser(user)
            } catch (e: Exception) {
                -1L
            }
            for (info in activities) {
                val component = info.componentName
                val pkg = component.packageName
                if (pkg == context.packageName) continue // BuenOrden no se muestra a sí misma.
                val appInfo = info.applicationInfo
                val label = info.label?.toString()?.trim().orEmpty().ifEmpty { pkg }
                val key = "${component.flattenToShortString()}#$serial"
                newInfo[key] = info
                result += InstalledApp(
                    key = key,
                    packageName = pkg,
                    label = label,
                    component = component,
                    user = user,
                    isWorkProfile = user != myUser,
                    signals = AppSignals(
                        packageName = pkg,
                        label = label,
                        declaredCategory = declaredCategory(appInfo),
                        isGameFlag = isGameFlag(appInfo),
                        isSystem = isSystem(appInfo),
                        capabilities = capabilities[pkg].orEmpty()
                    )
                )
            }
        }
        infoByKey = newInfo
        return result
    }

    fun cachedIcon(key: String): ImageBitmap? = iconCache.get(key)

    /** Debe llamarse fuera del hilo principal. */
    fun loadIcon(key: String): ImageBitmap? {
        iconCache.get(key)?.let { return it }
        val info = infoByKey[key] ?: return null
        val bitmap: ImageBitmap? = try {
            info.getBadgedIcon(densityDpi)
                ?.toBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
                ?.asImageBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo cargar el ícono de $key", e)
            null
        }
        if (bitmap != null) iconCache.put(key, bitmap)
        return bitmap
    }

    /** Olvida los íconos de un paquete (por ejemplo, tras una actualización que cambió su ícono). */
    fun invalidateIcons(packageName: String) {
        iconCache.snapshot().keys
            .filter { it.substringBefore('/') == packageName }
            .forEach { iconCache.remove(it) }
    }

    fun launch(app: InstalledApp, sourceBounds: Rect? = null): Boolean = try {
        launcherApps.startMainActivity(app.component, app.user, sourceBounds, null)
        true
    } catch (e: Exception) {
        // App desinstalada en el intertanto, deshabilitada o perfil de trabajo en pausa.
        Log.w(TAG, "No se pudo abrir ${app.component}", e)
        false
    }

    fun openAppInfo(app: InstalledApp): Boolean = try {
        launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        true
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo abrir la información de ${app.component}", e)
        false
    }

    /** Avisa (en el hilo principal) cuando cambia el conjunto de apps. */
    fun registerCallback(onChanged: (packageName: String?) -> Unit): LauncherApps.Callback {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = onChanged(packageName)
            override fun onPackageAdded(packageName: String, user: UserHandle) = onChanged(packageName)
            override fun onPackageChanged(packageName: String, user: UserHandle) = onChanged(packageName)
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onChanged(null)
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onChanged(null)
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        return callback
    }

    fun unregisterCallback(callback: LauncherApps.Callback) {
        try {
            launcherApps.unregisterCallback(callback)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo quitar el callback", e)
        }
    }

    private fun declaredCategory(info: ApplicationInfo): DeclaredCategory {
        if (Build.VERSION.SDK_INT >= 31 && info.category == ApplicationInfo.CATEGORY_ACCESSIBILITY) {
            return DeclaredCategory.ACCESSIBILITY
        }
        return when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> DeclaredCategory.GAME
            ApplicationInfo.CATEGORY_AUDIO -> DeclaredCategory.AUDIO
            ApplicationInfo.CATEGORY_VIDEO -> DeclaredCategory.VIDEO
            ApplicationInfo.CATEGORY_IMAGE -> DeclaredCategory.IMAGE
            ApplicationInfo.CATEGORY_SOCIAL -> DeclaredCategory.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> DeclaredCategory.NEWS
            ApplicationInfo.CATEGORY_MAPS -> DeclaredCategory.MAPS
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> DeclaredCategory.PRODUCTIVITY
            else -> DeclaredCategory.UNDEFINED
        }
    }

    @Suppress("DEPRECATION")
    private fun isGameFlag(info: ApplicationInfo): Boolean = (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    private fun isSystem(info: ApplicationInfo): Boolean = (info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0

    /**
     * Qué apps saben hacer qué cosa, preguntándole al sistema quién responde a cada intent.
     * Cada intent necesita su entrada en <queries> del manifiesto (visibilidad de paquetes, Android 11+).
     */
    private fun loadCapabilities(): Map<String, Set<Capability>> {
        val result = HashMap<String, MutableSet<Capability>>()
        fun add(capability: Capability, intent: Intent) {
            packagesHandling(intent).forEach { result.getOrPut(it) { HashSet() } += capability }
        }
        add(Capability.DIALER, Intent(Intent.ACTION_DIAL, Uri.parse("tel:123")))
        add(Capability.SMS, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:123")))
        add(Capability.EMAIL, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hola@ejemplo.cl")))
        add(Capability.CAMERA, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        add(
            Capability.BROWSER,
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ejemplo.cl")).addCategory(Intent.CATEGORY_BROWSABLE)
        )
        add(Capability.MAPS, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Santiago")))
        add(
            Capability.MUSIC,
            Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://media/external/audio/media/1"), "audio/*")
        )
        add(Capability.CALENDAR, Intent(Intent.ACTION_INSERT).setType("vnd.android.cursor.dir/event"))
        return result
    }

    private fun packagesHandling(intent: Intent): Set<String> = try {
        val infos = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        infos.mapNotNull { it.activityInfo?.packageName }.toSet()
    } catch (e: Exception) {
        Log.w(TAG, "Falló la consulta de $intent", e)
        emptySet()
    }

    private companion object {
        const val TAG = "BuenOrden"
    }
}
