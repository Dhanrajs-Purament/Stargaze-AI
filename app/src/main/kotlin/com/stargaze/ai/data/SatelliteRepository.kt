package com.stargaze.ai.data

import android.content.Context
import com.stargaze.ai.astronomy.AnalyticSatelliteProvider
import com.stargaze.ai.astronomy.SatelliteProvider
import com.stargaze.ai.astronomy.Tle
import com.stargaze.ai.astronomy.TleSatelliteProvider
import com.stargaze.ai.astronomy.parseTleDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies a real [SatelliteProvider] backed by live TLE data with an offline-first strategy:
 *
 *  1. **Cache** — a TLE file in app-private storage, refreshed from the network when older than the TTL.
 *  2. **Network** — CelesTrak's public GP API over HTTPS (the canonical free TLE source).
 *  3. **Bundled asset** — a packaged fallback TLE so satellites work on first launch / fully offline.
 *  4. **Analytic** — if even parsing fails, the analytic provider keeps the feature alive.
 *
 * The catalog-number map binds the app's tracked satellites to their NORAD IDs so matching is exact.
 */
@Singleton
class SatelliteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    private val cacheFile = File(context.filesDir, "satellites.tle")

    /** Builds the best available provider for the current time. Safe to call on a background thread. */
    suspend fun provider(): SatelliteProvider = withContext(Dispatchers.IO) {
        val tles = loadTles()
        if (tles.isNotEmpty()) {
            TleSatelliteProvider(tles, CATALOG_NUMBERS, fallback = AnalyticSatelliteProvider())
        } else {
            AnalyticSatelliteProvider()
        }
    }

    /** Forces a network refresh of the TLE cache. Returns true if fresh data was written. */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) { fetchAndCache() }

    private fun loadTles(): List<Tle> {
        // Use cache if fresh; otherwise try a refresh, then fall back to cache, then bundled asset.
        if (isCacheFresh()) {
            parseTleDocument(cacheFile.readText()).takeIf { it.isNotEmpty() }?.let { return it }
        }
        if (fetchAndCache() && cacheFile.exists()) {
            parseTleDocument(cacheFile.readText()).takeIf { it.isNotEmpty() }?.let { return it }
        }
        if (cacheFile.exists()) {
            parseTleDocument(cacheFile.readText()).takeIf { it.isNotEmpty() }?.let { return it }
        }
        return runCatching {
            context.assets.open(BUNDLED_ASSET).bufferedReader().use { parseTleDocument(it.readText()) }
        }.getOrDefault(emptyList())
    }

    private fun isCacheFresh(): Boolean =
        cacheFile.exists() && (System.currentTimeMillis() - cacheFile.lastModified()) < CACHE_TTL_MS

    private fun fetchAndCache(): Boolean {
        return try {
            val combined = StringBuilder()
            for (url in TLE_SOURCES) {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.let { combined.appendLine(it) }
                    }
                }
            }
            val text = combined.toString()
            // Only overwrite the cache if the payload actually contains valid TLEs.
            if (parseTleDocument(text).isNotEmpty()) {
                cacheFile.writeText(text)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        // CelesTrak GP API (HTTPS). "stations" covers ISS & CSS; "science" covers Hubble.
        val TLE_SOURCES = listOf(
            "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=tle",
            "https://celestrak.org/NORAD/elements/gp.php?GROUP=science&FORMAT=tle",
        )
        const val BUNDLED_ASSET = "tle/fallback_satellites.tle"
        const val CACHE_TTL_MS = 12L * 60 * 60 * 1000 // 12 hours

        // App satellite short names -> NORAD catalog numbers for exact TLE matching.
        val CATALOG_NUMBERS = mapOf(
            "ISS" to 25544,
            "HST" to 20580,
            "CSS" to 48274,
        )
    }
}
