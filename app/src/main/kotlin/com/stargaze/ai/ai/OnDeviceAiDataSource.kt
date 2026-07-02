package com.stargaze.ai.ai

import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinates the on-device AI tier across a **catalog of selectable models**.
 *
 * Users choose which model to download (e.g. a small "Standard" model or a larger high-quality one).
 * This source tracks the selected model, exposes download state, downloads/verifies/deletes models by
 * id, and answers questions using whichever model is currently selected and ready. URLs and SHA-256
 * hashes come from the bundled catalog (non-secret); integrity is enforced by [GemmaModelManager].
 *
 * Never a mock: if no model is downloaded, [isDownloaded] is false and the repository uses other
 * tiers (offline knowledge / cloud) instead.
 */
class OnDeviceAiDataSource(
    private val deviceCapability: DeviceCapability,
    private val modelManager: GemmaModelManager,
    private val engine: OnDeviceAiEngine,
    private val catalog: ModelCatalog,
) : OnDeviceAi {

    val downloadState: StateFlow<ModelState> get() = modelManager.state

    val totalRamMb: Long by lazy { deviceCapability.totalRamMb() }

    /** All catalog models offered to the user. */
    val availableModels: List<ModelCatalogEntry> get() = catalog.entries

    /** Whether on-device AI is possible at all (a catalog exists). */
    val isSupported: Boolean get() = catalog.entries.isNotEmpty()

    @Volatile
    private var selectedModelId: String? = null

    /** The currently selected model (explicit selection, else the device-appropriate default). */
    fun selectedModel(): ModelCatalogEntry? =
        selectedModelId?.let { catalog.byId(it) } ?: catalog.defaultFor(totalRamMb)

    fun selectModel(id: String) { selectedModelId = id }

    fun isDownloaded(id: String): Boolean =
        catalog.byId(id)?.let { modelManager.isReady(it.toSpec()) } ?: false

    override fun isDownloaded(): Boolean = selectedModel()?.let { isDownloaded(it.id) } ?: false

    /** True if this device meets the model's recommended minimum RAM. */
    fun isSuitable(entry: ModelCatalogEntry): Boolean = totalRamMb >= entry.minRamMb

    /** Downloads + verifies the given model. */
    suspend fun download(id: String, requireUnmetered: Boolean = true): ModelState {
        val entry = catalog.byId(id) ?: return ModelState.Failed("Unknown model")
        return modelManager.download(entry.toSpec(), requireUnmetered)
    }

    fun deleteModel(id: String) {
        catalog.byId(id)?.let { modelManager.delete(it.toSpec()) }
    }

    override suspend fun ask(question: String): String? {
        val entry = selectedModel() ?: return null
        val path = modelManager.readyModelPath(entry.toSpec()) ?: return null
        if (!engine.ensureLoaded(path)) return null
        return engine.generate(question)
    }

    override suspend fun explainScene(question: String, sceneContext: String): String? {
        val entry = selectedModel() ?: return null
        val path = modelManager.readyModelPath(entry.toSpec()) ?: return null
        if (!engine.ensureLoaded(path)) return null
        return engine.explainScene(question, sceneContext)
    }

    suspend fun release() = engine.close()
}
