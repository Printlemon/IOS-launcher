package com.example.ioslauncher

import android.content.Context

enum class IconStyle { Default, Dark, Glass, Tinted }

data class LauncherSettings(
    val style: IconStyle = IconStyle.Default,
    val tint: Int = 0xFF0A84FF.toInt(),
    val tintIntensity: Float = 0.85f,
    val glass: Float = 0.6f,
    val brightness: Float = 1f,
    val sizeDp: Float = 60f,
    val radius: Float = 0.2237f,
    val hidden: Set<String> = emptySet()
)

class SettingsStore(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun load(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            style = runCatching { IconStyle.valueOf(p.getString("style", d.style.name)!!) }.getOrDefault(d.style),
            tint = p.getInt("tint", d.tint),
            tintIntensity = p.getFloat("tintIntensity", d.tintIntensity),
            glass = p.getFloat("glass", d.glass),
            brightness = p.getFloat("brightness", d.brightness),
            sizeDp = p.getFloat("sizeDp", d.sizeDp),
            radius = p.getFloat("radius", d.radius),
            hidden = (p.getStringSet("hidden", emptySet()) ?: emptySet()).toSet()
        )
    }

    fun save(s: LauncherSettings) {
        p.edit()
            .putString("style", s.style.name)
            .putInt("tint", s.tint)
            .putFloat("tintIntensity", s.tintIntensity)
            .putFloat("glass", s.glass)
            .putFloat("brightness", s.brightness)
            .putFloat("sizeDp", s.sizeDp)
            .putFloat("radius", s.radius)
            .putStringSet("hidden", s.hidden)
            .apply()
    }

    fun recents(): List<String> = (p.getString("recents", "") ?: "").split(",").filter { it.isNotEmpty() }

    fun pushRecent(pkg: String) {
        val l = (listOf(pkg) + recents().filter { it != pkg }).take(8)
        p.edit().putString("recents", l.joinToString(",")).apply()
    }
}
