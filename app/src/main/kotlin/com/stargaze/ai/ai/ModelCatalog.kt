package com.stargaze.ai.ai

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A selectable on-device model in the picker. URLs/hashes are non-secret; the SHA-256 is the
 * integrity control enforced at download time.
 */
@Serializable
data class ModelCatalogEntry(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String,
    @SerialName("description") val description: String,
    @SerialName("fileName") val fileName: String,
    @SerialName("url") val url: String,
    @SerialName("sha256") val sha256: String,
    @SerialName("approxMb") val approxMb: Int,
    @SerialName("minRamMb") val minRamMb: Long,
) {
    fun toSpec(): ModelSpec = ModelSpec(modelId = id, fileName = fileName, url = url, sha256 = sha256)
}

@Serializable
private data class CatalogFile(@SerialName("models") val models: List<ModelCatalogEntry> = emptyList())

/**
 * Loads the bundled model catalog (assets/models/catalog.json). Kept as data, not code, so new
 * models can be offered by shipping an updated manifest. Entries with blank URL/hash are skipped
 * (placeholders for models not yet hosted).
 */
class ModelCatalog(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    val entries: List<ModelCatalogEntry> = runCatching {
        context.assets.open(ASSET).bufferedReader().use { reader ->
            json.decodeFromString(CatalogFile.serializer(), reader.readText()).models
        }.filter { it.url.isNotBlank() && it.sha256.isNotBlank() }
    }.getOrDefault(emptyList())

    fun byId(id: String): ModelCatalogEntry? = entries.firstOrNull { it.id == id }

    /** The default selection: the first model the device can comfortably run, else the first entry. */
    fun defaultFor(totalRamMb: Long): ModelCatalogEntry? =
        entries.firstOrNull { totalRamMb >= it.minRamMb } ?: entries.firstOrNull()

    private companion object {
        const val ASSET = "models/catalog.json"
    }
}
