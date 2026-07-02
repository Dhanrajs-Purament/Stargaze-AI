package com.stargaze.ai.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargaze.ai.data.AiGuideRepository
import com.stargaze.ai.data.LocationRepository
import com.stargaze.ai.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val text: String, val fromUser: Boolean, val pending: Boolean = false)

data class GuideUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            "Hi! I'm your AI Sky Guide. Point your phone at the sky, tap any star, planet or satellite, " +
                "or ask me a question. What would you like to explore tonight?",
            fromUser = false,
        ),
    ),
    val isPro: Boolean = false,
    val questionsUsedToday: Int = 0,
    val quotaReached: Boolean = false,
)

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val aiGuideRepository: AiGuideRepository,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val suggestions = listOf(
        "What should I look at tonight?",
        "Where is the ISS now?",
        "Why is Mars red?",
        "Find me Orion",
        "What's a light-year?",
    )

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    init {
        settingsRepository.settings.onEach { s ->
            _uiState.value = _uiState.value.copy(
                isPro = s.isPro,
                questionsUsedToday = s.aiQuestionsUsedToday,
                quotaReached = !s.isPro && s.aiQuestionsUsedToday >= SettingsRepository.FREE_DAILY_AI_LIMIT,
            )
        }.launchIn(viewModelScope)
    }

    fun ask(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val state = _uiState.value
        if (state.quotaReached) {
            appendAi("You've reached today's free limit of ${SettingsRepository.FREE_DAILY_AI_LIMIT} questions. " +
                "Upgrade to Pro for unlimited AI Sky Guide access, or come back tomorrow.")
            return
        }

        // Add user message + a pending AI bubble.
        _uiState.value = state.copy(
            messages = state.messages + ChatMessage(trimmed, fromUser = true) + ChatMessage("", fromUser = false, pending = true),
        )

        viewModelScope.launch {
            val location = locationRepository.location.value
            val answer = aiGuideRepository.ask(trimmed, location, System.currentTimeMillis())
            // Replace pending bubble with the answer.
            val msgs = _uiState.value.messages.toMutableList()
            val idx = msgs.indexOfLast { it.pending }
            if (idx >= 0) msgs[idx] = ChatMessage(answer.text, fromUser = false)
            _uiState.value = _uiState.value.copy(messages = msgs)
            if (!_uiState.value.isPro) settingsRepository.incrementAiUsage()
        }
    }

    private fun appendAi(text: String) {
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + ChatMessage(text, fromUser = false))
    }
}
