package com.stargaze.ai.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Where the model download currently stands, surfaced to the UI. */
sealed interface ModelState {
    data object Absent : ModelState
    data class Downloading(val progress: Float) : ModelState   // 0f..1f
    data object Verifying : ModelState
    data object Ready : ModelState
    data class Failed(val reason: String) : ModelState
}

/**
 * Describes a downloadable model: its remote URL and the **expected SHA-256** of the file. The hash
 * is the security control: a model file is only accepted if its content hash matches, which prevents
 * a tampered or man-in-the-middled model from being loaded and executed on the device.
 */
data class ModelSpec(
    val modelId: String,
    val fileName: String,
    val url: String,
    val sha256: String,
)

/**
 * Downloads, verifies, stores, and removes the on-device Gemma model file.
 *
 * Security & robustness:
 *  - HTTPS-only (enforced here and by the network security config).
 *  - Streams to a temp file, computes SHA-256 while writing, and only promotes to the final path if
 *    the hash matches the pinned [ModelSpec.sha256]. Mismatch => file deleted, [ModelState.Failed].
 *  - Stored in app-private internal storage (`filesDir`), not external/world-readable storage.
 *  - Wi-Fi-only guard to avoid surprising users with multi-GB mobile-data downloads.
 *
 * The model URL/hash are injected (not hardcoded secrets) so the publisher can point at their own
 * hosting of the Gemma weights, in compliance with the Gemma Terms of Use.
 */
class GemmaModelManager(
    private val context: Context,
    private val client: OkHttpClient,
) {
    private val _state = MutableStateFlow<ModelState>(ModelState.Absent)
    val state: StateFlow<ModelState> = _state.asStateFlow()

    private fun modelFile(spec: ModelSpec) = File(context.filesDir, spec.fileName)

    /** Absolute path of a verified, ready model file, or null if not present. */
    fun readyModelPath(spec: ModelSpec): String? {
        val f = modelFile(spec)
        return if (f.exists() && f.length() > 0) f.absolutePath else null
    }

    fun isReady(spec: ModelSpec): Boolean = readyModelPath(spec) != null

    fun isOnUnmeteredNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Downloads and verifies the model. Suspends until done. Updates [state] throughout.
     *
     * @param requireUnmetered when true, refuses to download over a metered (mobile) connection.
     */
    suspend fun download(spec: ModelSpec, requireUnmetered: Boolean = true): ModelState =
        withContext(Dispatchers.IO) {
            if (isReady(spec)) {
                _state.value = ModelState.Ready
                return@withContext ModelState.Ready
            }
            if (!spec.url.startsWith("https://")) {
                return@withContext fail("Model URL must be HTTPS")
            }
            if (requireUnmetered && !isOnUnmeteredNetwork()) {
                return@withContext fail("Connect to Wi-Fi to download the AI model")
            }

            val finalFile = modelFile(spec)
            val tmpFile = File(context.filesDir, spec.fileName + ".part")
            tmpFile.delete()

            try {
                _state.value = ModelState.Downloading(0f)
                val request = Request.Builder().url(spec.url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext fail("Download failed: HTTP ${response.code}")
                    val body = response.body ?: return@withContext fail("Empty response body")
                    val total = body.contentLength().takeIf { it > 0 }
                    val digest = MessageDigest.getInstance("SHA-256")

                    body.byteStream().use { input ->
                        tmpFile.outputStream().use { output ->
                            val buffer = ByteArray(1 shl 16)
                            var readTotal = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                digest.update(buffer, 0, n)
                                readTotal += n
                                if (total != null) {
                                    _state.value = ModelState.Downloading((readTotal.toFloat() / total).coerceIn(0f, 1f))
                                }
                            }
                        }
                    }

                    _state.value = ModelState.Verifying
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!actual.equals(spec.sha256, ignoreCase = true)) {
                        tmpFile.delete()
                        return@withContext fail("Model integrity check failed")
                    }
                }

                if (!tmpFile.renameTo(finalFile)) {
                    tmpFile.copyTo(finalFile, overwrite = true)
                    tmpFile.delete()
                }
                _state.value = ModelState.Ready
                ModelState.Ready
            } catch (e: IOException) {
                tmpFile.delete()
                fail("Network error: ${e.message ?: "unknown"}")
            } catch (e: Exception) {
                tmpFile.delete()
                fail("Unexpected error: ${e.message ?: "unknown"}")
            }
        }

    /** Deletes the downloaded model to reclaim storage. */
    fun delete(spec: ModelSpec) {
        modelFile(spec).delete()
        File(context.filesDir, spec.fileName + ".part").delete()
        _state.value = ModelState.Absent
    }

    private fun fail(reason: String): ModelState {
        val s = ModelState.Failed(reason)
        _state.value = s
        return s
    }
}
