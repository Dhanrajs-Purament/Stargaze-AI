package com.stargaze.ai.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stargaze.ai.ui.components.GlassCard
import com.stargaze.ai.ui.components.SheetTitle
import com.stargaze.ai.ui.components.SkySheet
import com.stargaze.ai.ui.onboarding.PrimaryButton
import com.stargaze.ai.ui.theme.StarColors

@Composable
fun LearnSheet(onDismiss: () -> Unit, viewModel: LearnViewModel = hiltViewModel()) {
    val quiz by viewModel.quiz.collectAsState()
    var selectedTour by remember { mutableStateOf<Tour?>(null) }

    SkySheet(onDismiss = onDismiss, maxHeightFraction = 0.88f) {
        when {
            quiz.active || quiz.finished -> {
                // Clear tour selection when entering quiz so back goes back to tour home.
                selectedTour = null
                when {
                    quiz.active -> QuizActive(quiz, viewModel::answer)
                    quiz.finished -> QuizResult(quiz.score, quiz.questions.size, onAgain = viewModel::startQuiz, onClose = viewModel::closeQuiz)
                }
            }
            selectedTour != null -> TourDetail(
                tour = selectedTour!!,
                onBack = { selectedTour = null },
                onAdvance = { viewModel.advanceTour(selectedTour!!.title) },
            )
            else -> LearnHome(viewModel, onStartQuiz = viewModel::startQuiz, onSelectTour = { selectedTour = it })
        }
    }
}

@Composable
private fun LearnHome(
    viewModel: LearnViewModel,
    onStartQuiz: () -> Unit,
    onSelectTour: (Tour) -> Unit,
) {
    val tours by viewModel.tours.collectAsState()
    LazyColumn {
        item {
            SheetTitle(
                title = "\uD83C\uDF93 Learn the sky",
                subtitle = "AI-guided tours, quizzes & missions",
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(StarColors.Accent.copy(alpha = 0.16f), StarColors.Accent2.copy(alpha = 0.10f))))
                    .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("\uD83E\uDDE0 Daily Quiz", color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("6 quick questions \u00B7 build your streak", color = StarColors.Muted, fontSize = 13.sp)
                }
                PrimaryButton("Play", onClick = onStartQuiz)
            }
            Spacer(Modifier.height(18.dp))
            Text("Guided tours", color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }
        items(tours.size) { i ->
            val t = tours[i]
            TourRow(t, onClick = { onSelectTour(t) })
            Spacer(Modifier.height(11.dp))
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Your badges", color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.badges.size) { i ->
                    BadgeItem(viewModel.badges[i])
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TourRow(t: Tour, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StarColors.Card)
            .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Open tour ${t.title}, ${t.progress}% complete"
                role = androidx.compose.ui.semantics.Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(t.emoji, fontSize = 28.sp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, color = StarColors.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(t.subtitle, color = StarColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x1AFFFFFF)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(t.progress / 100f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(StarColors.BrandGradient),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("\u2192", color = StarColors.Accent, fontSize = 18.sp)
    }
}

@Composable
private fun BadgeItem(b: Badge) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp)) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (b.earned) StarColors.Gold.copy(alpha = 0.2f) else StarColors.Card)
                .border(1.dp, if (b.earned) StarColors.Gold.copy(alpha = 0.5f) else StarColors.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(if (b.earned) b.emoji else "\uD83D\uDD12", fontSize = 26.sp) }
        Spacer(Modifier.height(6.dp))
        Text(b.name, color = StarColors.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TourDetail(tour: Tour, onBack: () -> Unit, onAdvance: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "← Back to tours",
            color = StarColors.Accent,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).semantics { role = Role.Button; contentDescription = "Back to tours" }.padding(vertical = 4.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text("${tour.emoji} ${tour.title}", color = StarColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(tour.subtitle, color = StarColors.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        tour.objectives.forEachIndexed { i, objective ->
            val done = i < tour.completed
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (done) "✓" else "○", color = if (done) StarColors.Green else StarColors.Muted, fontSize = 18.sp)
                Spacer(Modifier.width(12.dp))
                Text(objective, color = if (done) StarColors.Muted else StarColors.Ink, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        val allDone = tour.completed >= tour.objectives.size
        PrimaryButton(
            text = if (allDone) "Tour complete" else "Mark objective ${tour.completed + 1} done",
            enabled = !allDone,
            onClick = onAdvance,
        )
    }
}

@Composable
private fun QuizActive(quiz: QuizState, onAnswer: (Int) -> Unit) {
    val q = quiz.questions[quiz.index]
    Column {
        Text("\uD83E\uDDE0 Daily Quiz", color = StarColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Question ${quiz.index + 1} of ${quiz.questions.size}", color = StarColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("Score ${quiz.score}", color = StarColors.Muted, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x1AFFFFFF)),
        ) {
            Box(Modifier.fillMaxWidth(quiz.index.toFloat() / quiz.questions.size).height(5.dp).clip(RoundedCornerShape(3.dp)).background(StarColors.BrandGradient))
        }
        Spacer(Modifier.height(16.dp))
        Text(q.prompt, color = StarColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
        Spacer(Modifier.height(16.dp))
        q.options.forEachIndexed { i, option ->
            val bg = when {
                quiz.locked && i == q.correctIndex -> StarColors.Green.copy(alpha = 0.15f)
                quiz.locked && i == quiz.selectedIndex -> StarColors.Red.copy(alpha = 0.13f)
                else -> StarColors.Card
            }
            val border = when {
                quiz.locked && i == q.correctIndex -> StarColors.Green
                quiz.locked && i == quiz.selectedIndex -> StarColors.Red
                else -> StarColors.Line
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(13.dp))
                    .clickable(enabled = !quiz.locked) { onAnswer(i) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) { Text(option, color = StarColors.Ink, fontSize = 15.sp) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuizResult(score: Int, total: Int, onAgain: () -> Unit, onClose: () -> Unit) {
    val pct = if (total == 0) 0 else score * 100 / total
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (pct >= 80) "\uD83C\uDF1F" else if (pct >= 50) "\u2728" else "\uD83C\uDF19", fontSize = 54.sp)
        Spacer(Modifier.height(10.dp))
        Text("$score / $total correct", color = StarColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (pct >= 80) "Stellar! You're a real sky-watcher." else if (pct >= 50) "Nice work - keep exploring!" else "Keep learning, the sky rewards the curious.",
            color = StarColors.Muted, fontSize = 13.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton("Play again", onClick = onAgain)
        Spacer(Modifier.height(12.dp))
        Text("Close", color = StarColors.Muted, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onClose))
    }
}
