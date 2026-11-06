package com.aethra.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.Locale

data class LauncherApp(
    val id: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val icon: Drawable
)

class LauncherRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val preferences = appContext.getSharedPreferences("aethra_preferences", Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    fun loadApps(): List<LauncherApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { resolved ->
                val activityInfo = resolved.activityInfo ?: return@mapNotNull null
                if (activityInfo.packageName == appContext.packageName) return@mapNotNull null

                val label = resolved.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isBlank()) return@mapNotNull null

                val component = ComponentName(activityInfo.packageName, activityInfo.name)
                LauncherApp(
                    id = component.flattenToString(),
                    label = label,
                    packageName = activityInfo.packageName,
                    component = component,
                    icon = resolved.loadIcon(packageManager)
                )
            }
            .distinctBy { it.id }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun favoriteIds(): Set<String> =
        preferences.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet()

    fun toggleFavorite(id: String, current: Set<String>): Set<String> {
        val updated = current.toMutableSet()
        if (!updated.add(id)) updated.remove(id)
        preferences.edit().putStringSet(KEY_FAVORITES, updated.toSet()).apply()
        return updated
    }

    fun launch(app: LauncherApp): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    private companion object {
        const val KEY_FAVORITES = "favorite_apps"
    }
}
