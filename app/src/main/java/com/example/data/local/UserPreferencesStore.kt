package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.snapDataStore by preferencesDataStore(name = "nilesh_snapchat_prefs")

class UserPreferencesStore(private val context: Context) {

    private object Keys {
        val NEXT_SIM_SNAP_AT = longPreferencesKey("next_sim_snap_at")
        val LAST_SIM_SNAP_AT = longPreferencesKey("last_sim_snap_at")
        val BEAUTY_DEFAULT = floatPreferencesKey("beauty_default")
    }

    val beautyDefault: Flow<Float> = context.snapDataStore.data.map { prefs ->
        prefs[Keys.BEAUTY_DEFAULT] ?: 0.25f
    }

    suspend fun getNextSimSnapAt(): Long {
        val prefs = context.snapDataStore.data.first()
        return prefs[Keys.NEXT_SIM_SNAP_AT] ?: 0L
    }

    suspend fun setSimSnapSchedule(lastAtMillis: Long, nextAtMillis: Long) {
        context.snapDataStore.edit { prefs ->
            prefs[Keys.LAST_SIM_SNAP_AT] = lastAtMillis
            prefs[Keys.NEXT_SIM_SNAP_AT] = nextAtMillis
        }
    }

    suspend fun setBeautyDefault(value: Float) {
        context.snapDataStore.edit { prefs ->
            prefs[Keys.BEAUTY_DEFAULT] = value.coerceIn(0f, 1f)
        }
    }
}
