package com.example.vrtheater.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("vr_settings")

object SettingsKeys {
    val eyeSeparation = floatPreferencesKey("eye_separation") // in NDC, default 0.03
    val k1 = floatPreferencesKey("k1")
    val k2 = floatPreferencesKey("k2")
    val screenScale = floatPreferencesKey("screen_scale")
    val screenTilt = floatPreferencesKey("screen_tilt")
}

data class VRSettings(
    val eyeSeparation: Float = 0.03f,
    val k1: Float = 0.22f,
    val k2: Float = 0.24f,
    val screenScale: Float = 1.0f,
    val screenTilt: Float = 0.0f
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<VRSettings> = context.dataStore.data.map { prefs ->
        VRSettings(
            eyeSeparation = prefs[SettingsKeys.eyeSeparation] ?: 0.03f,
            k1 = prefs[SettingsKeys.k1] ?: 0.22f,
            k2 = prefs[SettingsKeys.k2] ?: 0.24f,
            screenScale = prefs[SettingsKeys.screenScale] ?: 1.0f,
            screenTilt = prefs[SettingsKeys.screenTilt] ?: 0.0f,
        )
    }

    suspend fun update(block: (VRSettings) -> VRSettings) {
        context.dataStore.edit { prefs ->
            val current = VRSettings(
                eyeSeparation = prefs[SettingsKeys.eyeSeparation] ?: 0.03f,
                k1 = prefs[SettingsKeys.k1] ?: 0.22f,
                k2 = prefs[SettingsKeys.k2] ?: 0.24f,
                screenScale = prefs[SettingsKeys.screenScale] ?: 1.0f,
                screenTilt = prefs[SettingsKeys.screenTilt] ?: 0.0f,
            )
            val updated = block(current)
            prefs[SettingsKeys.eyeSeparation] = updated.eyeSeparation
            prefs[SettingsKeys.k1] = updated.k1
            prefs[SettingsKeys.k2] = updated.k2
            prefs[SettingsKeys.screenScale] = updated.screenScale
            prefs[SettingsKeys.screenTilt] = updated.screenTilt
        }
    }
}