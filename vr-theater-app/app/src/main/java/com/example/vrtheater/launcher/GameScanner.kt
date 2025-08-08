package com.example.vrtheater.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

object GameScanner {
    data class AppInfo(val packageName: String, val appLabel: String)

    fun scanInstalledGames(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_DISABLED_COMPONENTS)
        val apps = resolveInfos.mapNotNull { ri ->
            val ai = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
            val label = ai.loadLabel(pm)?.toString() ?: ai.packageName
            val isGame = if (android.os.Build.VERSION.SDK_INT >= 26) {
                ai.category == ApplicationInfo.CATEGORY_GAME
            } else true
            if (isGame) AppInfo(ai.packageName, label) else null
        }.distinctBy { it.packageName }
        return apps.sortedBy { it.appLabel.lowercase() }
    }

    fun launchApp(context: Context, packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}