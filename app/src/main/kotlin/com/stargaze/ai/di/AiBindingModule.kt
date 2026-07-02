package com.stargaze.ai.di

import com.stargaze.ai.ai.OnDeviceAi
import com.stargaze.ai.ai.OnDeviceAiDataSource
import com.stargaze.ai.data.AiPrefs
import com.stargaze.ai.data.AiPrefsProvider
import com.stargaze.ai.data.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Singleton

/** Wires the AI repository's collaborators (on-device tier + consent snapshot) to real implementations. */
@Module
@InstallIn(SingletonComponent::class)
object AiBindingModule {

    @Provides
    @Singleton
    fun provideOnDeviceAi(dataSource: OnDeviceAiDataSource): OnDeviceAi = dataSource

    @Provides
    @Singleton
    fun provideAiPrefsProvider(settingsRepository: SettingsRepository): AiPrefsProvider =
        AiPrefsProvider {
            val s = settingsRepository.settings.first()
            AiPrefs(cloudAiConsent = s.cloudAiConsent, onDeviceAiEnabled = s.onDeviceAiEnabled)
        }
}
