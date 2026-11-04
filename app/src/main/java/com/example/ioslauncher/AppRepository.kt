package com.example.ioslauncher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class AppEntry(val label: String, val pkg: String, val drawable: Drawable)

class AppRepository(private val ctx: Context) {
    fun load(): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
            .sortedBy { it.label.lowercase() }
    }
}
