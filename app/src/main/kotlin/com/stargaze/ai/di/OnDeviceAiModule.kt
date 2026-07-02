package com.stargaze.ai.di

import android.content.Context
import com.stargaze.ai.ai.DeviceCapability
import com.stargaze.ai.ai.GemmaModelManager
import com.stargaze.ai.ai.ModelCatalog
import com.stargaze.ai.ai.OnDeviceAiDataSource
import com.stargaze.ai.ai.OnDeviceAiEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** Provides the on-device (Gemma 3n / MediaPipe) AI tier components as singletons. */
@Module
@InstallIn(SingletonComponent::class)
object OnDeviceAiModule {

    @Provides
    @Singleton
    fun provideDeviceCapability(@ApplicationContext context: Context): DeviceCapability =
        DeviceCapability(context)

    @Provides
    @Singleton
    fun provideGemmaModelManager(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): GemmaModelManager = GemmaModelManager(context, okHttpClient)

    @Provides
    @Singleton
    fun provideOnDeviceAiEngine(@ApplicationContext context: Context): OnDeviceAiEngine =
        OnDeviceAiEngine(context)

    @Provides
    @Singleton
    fun provideModelCatalog(@ApplicationContext context: Context): ModelCatalog = ModelCatalog(context)

    @Provides
    @Singleton
    fun provideOnDeviceAiDataSource(
        deviceCapability: DeviceCapability,
        modelManager: GemmaModelManager,
        engine: OnDeviceAiEngine,
        catalog: ModelCatalog,
    ): OnDeviceAiDataSource = OnDeviceAiDataSource(deviceCapability, modelManager, engine, catalog)
}
