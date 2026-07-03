package com.stargaze.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargaze.ai.ai.ModelCatalogEntry
import com.stargaze.ai.ai.ModelState
import com.stargaze.ai.ai.OnDeviceAiDataSource
import com.stargaze.ai.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One model as shown in the picker, with device-specific state. */
data class ModelOption(
    val id: String,
    val label: String,
    val description: String,
    val approxMb: Int,
    val suitable: Boolean,        // device meets recommended RAM
    val downloaded: Boolean,
    val selected: Boolean,
)

data class AiSettingsUiState(
    val disclosureAcknowledged: Boolean = false,
    val cloudAiConsent: Boolean = false,
    val onDeviceAiEnabled: Boolean = false,
    val highContrast: Boolean = false,
    val textScale: Float = 1.0f,
    val onDeviceSupported: Boolean = false,
    val totalRamMb: Long = 0,
    val models: List<ModelOption> = emptyList(),
    val downloadState: ModelState = ModelState.Absent,
    val downloadingModelId: String? = null,
)

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val onDeviceAi: OnDeviceAiDataSource,
) : ViewModel() {

    val uiState: StateFlow<AiSettingsUiState> =
        combine(settingsRepository.settings, onDeviceAi.downloadState) { settings, dl ->
            // Restore the user's persisted model choice into the (in-memory) data source.
            if (settings.selectedModelId.isNotBlank()) onDeviceAi.selectModel(settings.selectedModelId)
            val selectedId = onDeviceAi.selectedModel()?.id
            AiSettingsUiState(
                disclosureAcknowledged = settings.aiDisclosureAcknowledged,
                cloudAiConsent = settings.cloudAiConsent,
                onDeviceAiEnabled = settings.onDeviceAiEnabled,
                highContrast = settings.highContrast,
                textScale = settings.textScale,
                onDeviceSupported = onDeviceAi.isSupported,
                totalRamMb = onDeviceAi.totalRamMb,
                models = onDeviceAi.availableModels.map { it.toOption(selectedId) },
                downloadState = dl,
                downloadingModelId = if (dl is ModelState.Downloading || dl is ModelState.Verifying) selectedId else null,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiSettingsUiState())

    private fun ModelCatalogEntry.toOption(selectedId: String?): ModelOption = ModelOption(
        id = id,
        label = label,
        description = description,
        approxMb = approxMb,
        suitable = onDeviceAi.isSuitable(this),
        downloaded = onDeviceAi.isDownloaded(id),
        selected = id == selectedId,
    )

    fun acknowledgeDisclosure(allowCloud: Boolean) = viewModelScope.launch {
        settingsRepository.setCloudAiConsent(allowCloud)
        settingsRepository.setAiDisclosureAcknowledged(true)
    }

    fun setCloudConsent(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setCloudAiConsent(enabled)
    }

    fun setOnDeviceEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setOnDeviceAiEnabled(enabled)
    }

    fun setHighContrast(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setHighContrast(enabled)
    }

    fun setTextScale(scale: Float) = viewModelScope.launch {
        settingsRepository.setTextScale(scale)
    }

    fun selectModel(id: String) {
        onDeviceAi.selectModel(id)
        viewModelScope.launch { settingsRepository.setSelectedModelId(id) }
    }

    fun downloadModel(id: String) = viewModelScope.launch {
        onDeviceAi.selectModel(id)
        settingsRepository.setSelectedModelId(id)
        onDeviceAi.download(id, requireUnmetered = true)
    }

    fun deleteModel(id: String) = viewModelScope.launch {
        onDeviceAi.deleteModel(id)
        if (uiState.value.models.count { it.downloaded } <= 1) {
            settingsRepository.setOnDeviceAiEnabled(false)
        }
    }
}
