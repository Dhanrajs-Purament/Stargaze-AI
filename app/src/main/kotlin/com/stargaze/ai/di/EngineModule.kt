package com.stargaze.ai.di

import com.stargaze.ai.astronomy.KnowledgeEngine
import com.stargaze.ai.astronomy.SkyEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the pure-Kotlin astronomy engine as app-wide singletons. */
@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideSkyEngine(): SkyEngine = SkyEngine()

    @Provides
    @Singleton
    fun provideKnowledgeEngine(skyEngine: SkyEngine): KnowledgeEngine = KnowledgeEngine(skyEngine)

    @Provides
    @Singleton
    fun provideSceneContextBuilder(skyEngine: SkyEngine): com.stargaze.ai.astronomy.SceneContextBuilder =
        com.stargaze.ai.astronomy.SceneContextBuilder(skyEngine)
}
