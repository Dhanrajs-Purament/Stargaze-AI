package com.stargaze.ai.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Tour(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val objectives: List<String>,
    val completed: Int = 0,
) {
    val progress: Int
        get() = if (objectives.isEmpty()) 0 else (completed * 100 / objectives.size).coerceIn(0, 100)
}

data class Badge(val emoji: String, val name: String, val earned: Boolean)
data class QuizQuestion(val prompt: String, val options: List<String>, val correctIndex: Int)

data class QuizState(
    val active: Boolean = false,
    val finished: Boolean = false,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val score: Int = 0,
    val selectedIndex: Int? = null,
    val locked: Boolean = false,
)

@HiltViewModel
class LearnViewModel @Inject constructor() : ViewModel() {

    private val initialTours = listOf(
        Tour("\uD83C\uDF1F", "Your First Night", "Find the Moon, a planet, and a constellation", listOf("Find the Moon", "Find one visible planet", "Find one constellation"), completed = 2),
        Tour("\uD83D\uDEF0\uFE0F", "Catch the ISS", "Track the space station across the sky", listOf("Open the satellite layer", "Wait for an ISS pass", "Confirm the ISS in your sky"), completed = 1),
        Tour("\uD83E\uDE90", "Spot All the Planets", "Track every visible planet tonight", listOf("Find Mercury or Venus", "Find Mars or Jupiter", "Find Saturn (or farther)"), completed = 0),
        Tour("\uD83D\uDC1D", "Reading Orion", "Master the sky's most famous constellation", listOf("Locate Orion", "Identify Betelgeuse and Rigel", "Trace Orion's Belt"), completed = 0),
    )

    private val _tours = MutableStateFlow(initialTours)
    val tours: StateFlow<List<Tour>> = _tours.asStateFlow()

    fun advanceTour(title: String) {
        _tours.value = _tours.value.map { t ->
            if (t.title == title && t.completed < t.objectives.size) {
                t.copy(completed = t.completed + 1)
            } else {
                t
            }
        }
    }

    val badges = listOf(
        Badge("\uD83C\uDF19", "First Light", true),
        Badge("\u2734\uFE0F", "Star Spotter", true),
        Badge("\uD83D\uDEF0\uFE0F", "Sat Tracker", true),
        Badge("\uD83E\uDE90", "Planet Hunter", false),
        Badge("\u2604\uFE0F", "Meteor Watcher", false),
        Badge("\uD83D\uDD2D", "Sky Master", false),
    )

    private val allQuestions = listOf(
        QuizQuestion("Which star is the brightest in the night sky?", listOf("Polaris", "Sirius", "Betelgeuse", "Vega"), 1),
        QuizQuestion("Orion's Belt is made of how many bright stars?", listOf("2", "3", "4", "5"), 1),
        QuizQuestion("Which planet is famous for its rings?", listOf("Mars", "Jupiter", "Saturn", "Venus"), 2),
        QuizQuestion("What is the North Star called?", listOf("Vega", "Sirius", "Polaris", "Rigel"), 2),
        QuizQuestion("How often does the ISS orbit the Earth?", listOf("Once a day", "Every ~90 minutes", "Once a week", "Every 6 hours"), 1),
        QuizQuestion("Which constellation contains the Big Dipper?", listOf("Orion", "Leo", "Ursa Major", "Cygnus"), 2),
    )

    private val _quiz = MutableStateFlow(QuizState())
    val quiz: StateFlow<QuizState> = _quiz.asStateFlow()

    fun startQuiz() {
        _quiz.value = QuizState(active = true, questions = allQuestions.shuffled().take(5))
    }

    fun answer(optionIndex: Int) {
        val s = _quiz.value
        if (s.locked || !s.active) return
        val correct = s.questions[s.index].correctIndex == optionIndex
        _quiz.value = s.copy(selectedIndex = optionIndex, locked = true, score = if (correct) s.score + 1 else s.score)
        viewModelScope.launch {
            delay(850)
            val next = s.index + 1
            _quiz.value = if (next >= s.questions.size) {
                _quiz.value.copy(active = false, finished = true)
            } else {
                _quiz.value.copy(index = next, selectedIndex = null, locked = false)
            }
        }
    }

    fun closeQuiz() { _quiz.value = QuizState() }
}
