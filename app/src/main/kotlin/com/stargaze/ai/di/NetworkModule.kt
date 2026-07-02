package com.stargaze.ai.di

import android.content.Context
import com.stargaze.ai.BuildConfig
import com.stargaze.ai.network.AiService
import com.stargaze.ai.network.AuthInterceptor
import com.stargaze.ai.network.SecurePrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Networking dependencies. The [AiService] is provided as nullable: it only exists when a valid
 * HTTPS proxy base URL is configured at build time. When null, the app runs entirely on the
 * offline knowledge engine — a real, functional fallback, not a degraded mode.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSecurePrefs(@ApplicationContext context: Context): SecurePrefs = SecurePrefs(context)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true   // tolerate evolving server contract (untrusted input)
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(authInterceptor)

        // Logging is gated to debug builds only — never log request/response bodies in release.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(securePrefs: SecurePrefs): AuthInterceptor = AuthInterceptor(securePrefs)

    /**
     * The configured base URL, or null if none/invalid. Must be HTTPS.
     */
    @Provides
    @Singleton
    @AiProxyBaseUrl
    fun provideAiProxyBaseUrl(): String? {
        val url = BuildConfig.AI_PROXY_BASE_URL.trim()
        return when {
            url.isBlank() -> null
            !url.startsWith("https://") -> null // refuse non-HTTPS base URLs outright
            url.endsWith("/") -> url
            else -> "$url/"
        }
    }

    @Provides
    @Singleton
    fun provideAiService(
        @AiProxyBaseUrl baseUrl: String?,
        okHttpClient: OkHttpClient,
        json: Json,
    ): AiService? {
        if (baseUrl == null) return null
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AiService::class.java)
    }
}
