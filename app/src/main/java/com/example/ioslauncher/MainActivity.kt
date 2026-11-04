package com.example.ioslauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val unlockTick = mutableIntStateOf(0)
    private val apps = mutableStateOf(emptyList<AppEntry>())
    private lateinit var store: SettingsStore

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { unlockTick.intValue++ }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(this)
        enableEdgeToEdge()
        ContextCompat.registerReceiver(
            this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        setContent { LauncherApp(apps.value, unlockTick.intValue, store) }
    }

    override fun onStart() {
        super.onStart()
        val fresh = AppRepository(this).load()
        if (fresh.map { it.pkg } != apps.value.map { it.pkg }) apps.value = fresh
    }

    override fun onDestroy() {
        unregisterReceiver(unlockReceiver)
        super.onDestroy()
    }
}
