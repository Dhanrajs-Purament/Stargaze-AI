package com.stargaze.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stargaze_settings")

/** User-facing preferences and the client-side free-tier AI quota counter. */
data class UserSettings(
    val onboardingComplete: Boolean = false,
    val nightMode: Boolean = false,
    val highContrast: Boolean = false,
    val showConstellationLines: Boolean = true,
    val showLabels: Boolean = true,
    val showSatellites: Boolean = true,
    val isPro: Boolean = false,
    val aiQuestionsUsedToday: Int = 0,
    /** Whether the first-run AI/privacy disclosure has been shown and acknowledged. */
    val aiDisclosureAcknowledged: Boolean = false,
    /** Explicit opt-in to send questions to the cloud AI. OFF by default (privacy by default). */
    val cloudAiConsent: Boolean = false,
    /** Whether the user has enabled the optional on-device AI model. */
    val onDeviceAiEnabled: Boolean = false,
    /** The id of the on-device model the user selected from the catalog (empty = default). */
    val selectedModelId: String = "",
    /** UI text scale factor: 0.85 = small, 1.0 = default, 1.15 = medium, 1.3 = large. */
    val textScale: Float = 1.0f,
)

@Singleton
class SettingsRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val NIGHT = booleanPreferencesKey("night_mode")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val LINES = booleanPreferencesKey("show_lines")
        val LABELS = booleanPreferencesKey("show_labels")
        val SATS = booleanPreferencesKey("show_satellites")
        val PRO = booleanPreferencesKey("is_pro")
        val AI_USED = intPreferencesKey("ai_used_count")
        val AI_DATE = stringPreferencesKey("ai_used_date")
        val AI_DISCLOSURE = booleanPreferencesKey("ai_disclosure_ack")
        val CLOUD_AI_CONSENT = booleanPreferencesKey("cloud_ai_consent")
        val ON_DEVICE_AI = booleanPreferencesKey("on_device_ai_enabled")
        val SELECTED_MODEL = stringPreferencesKey("selected_model_id")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val today = LocalDate.now().toString()
        val usedToday = if (prefs[Keys.AI_DATE] == today) (prefs[Keys.AI_USED] ?: 0) else 0
        UserSettings(
            onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
            nightMode = prefs[Keys.NIGHT] ?: false,
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
            showConstellationLines = prefs[Keys.LINES] ?: true,
            showLabels = prefs[Keys.LABELS] ?: true,
            showSatellites = prefs[Keys.SATS] ?: true,
            isPro = prefs[Keys.PRO] ?: false,
            aiQuestionsUsedToday = usedToday,
            aiDisclosureAcknowledged = prefs[Keys.AI_DISCLOSURE] ?: false,
            cloudAiConsent = prefs[Keys.CLOUD_AI_CONSENT] ?: false,
            onDeviceAiEnabled = prefs[Keys.ON_DEVICE_AI] ?: false,
            selectedModelId = prefs[Keys.SELECTED_MODEL] ?: "",
            textScale = prefs[Keys.TEXT_SCALE] ?: 1.0f,
        )
    }

    suspend fun setOnboardingComplete(complete: Boolean) =
        context.dataStore.edit { it[Keys.ONBOARDING] = complete }

    suspend fun setNightMode(enabled: Boolean) =
        context.dataStore.edit { it[Keys.NIGHT] = enabled }

    suspend fun setHighContrast(enabled: Boolean) =
        context.dataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }

    suspend fun setShowConstellationLines(enabled: Boolean) =
        context.dataStore.edit { it[Keys.LINES] = enabled }

    suspend fun setShowLabels(enabled: Boolean) =
        context.dataStore.edit { it[Keys.LABELS] = enabled }

    suspend fun setShowSatellites(enabled: Boolean) =
        context.dataStore.edit { it[Keys.SATS] = enabled }

    suspend fun setPro(isPro: Boolean) =
        context.dataStore.edit { it[Keys.PRO] = isPro }

    suspend fun setAiDisclosureAcknowledged(acknowledged: Boolean) =
        context.dataStore.edit { it[Keys.AI_DISCLOSURE] = acknowledged }

    suspend fun setCloudAiConsent(consented: Boolean) =
        context.dataStore.edit { it[Keys.CLOUD_AI_CONSENT] = consented }

    suspend fun setOnDeviceAiEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.ON_DEVICE_AI] = enabled }

    suspend fun setSelectedModelId(id: String) =
        context.dataStore.edit { it[Keys.SELECTED_MODEL] = id }

    suspend fun setTextScale(scale: Float) =
        context.dataStore.edit { it[Keys.TEXT_SCALE] = scale.coerceIn(0.85f, 1.3f) }

    /** Atomically increments today's AI question counter, resetting if the day rolled over. */
    suspend fun incrementAiUsage() = context.dataStore.edit { prefs ->
        val today = LocalDate.now().toString()
        if (prefs[Keys.AI_DATE] != today) {
            prefs[Keys.AI_DATE] = today
            prefs[Keys.AI_USED] = 1
        } else {
            prefs[Keys.AI_USED] = (prefs[Keys.AI_USED] ?: 0) + 1
        }
    }

    companion object {
        /** Free-tier daily AI question cap; enforced client-side and re-validated server-side later. */
        const val FREE_DAILY_AI_LIMIT = 5
    }
}
