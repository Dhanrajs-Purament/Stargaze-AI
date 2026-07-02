package com.stargaze.ai.ui.guide

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stargaze.ai.ui.components.SheetTitle
import com.stargaze.ai.ui.components.SkySheet
import com.stargaze.ai.ui.components.TypingIndicator
import com.stargaze.ai.ui.theme.StarColors

/** Renders simple `**bold**` markup as styled text (safe rendering of model output). */
private fun renderMarkup(text: String): AnnotatedString = buildAnnotatedString {
    val regex = Regex("""\*\*(.+?)\*\*""")
    var last = 0
    for (m in regex.findAll(text)) {
        append(text.substring(last, m.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = StarColors.Ink)) { append(m.groupValues[1]) }
        last = m.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}

@Composable
fun GuideSheet(onDismiss: () -> Unit, viewModel: GuideViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    val sendQuestion = {
        val trimmed = input.trim()
        if (trimmed.isNotEmpty()) {
            viewModel.ask(trimmed)
            input = ""
        }
    }

    SkySheet(onDismiss = onDismiss, maxHeightFraction = 0.9f) {
        SheetTitle(title = "\u2728 AI Sky Guide", subtitle = "Ask me anything about the night sky")

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.messages.size) { i ->
                MessageBubble(state.messages[i])
            }
        }

        // Suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            viewModel.suggestions.take(3).forEach { s ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(StarColors.Card)
                        .border(1.dp, StarColors.Line, RoundedCornerShape(20.dp))
                        .clickable { viewModel.ask(s) }
                        .padding(horizontal = 13.dp, vertical = 9.dp)
                        .semantics { contentDescription = "Suggested question: $s" },
                ) { Text(s, color = StarColors.Muted, fontSize = 12.sp) }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask the sky...", color = StarColors.Faint) },
                shape = RoundedCornerShape(22.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendQuestion() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = StarColors.Card,
                    unfocusedContainerColor = StarColors.Card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = StarColors.Ink,
                    unfocusedTextColor = StarColors.Ink,
                ),
            )
            Spacer(Modifier.width(9.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(StarColors.BrandGradient)
                    .clickable(onClick = sendQuestion)
                    .semantics { contentDescription = "Send question" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color(0xFF0A0C1A),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.fromUser) Alignment.End else Alignment.Start
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = alignment,
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (message.fromUser) 16.dp else 5.dp,
                        bottomEnd = if (message.fromUser) 5.dp else 16.dp,
                    ),
                )
                .background(if (message.fromUser) StarColors.Accent else StarColors.Card2)
                .border(
                    1.dp,
                    if (message.fromUser) Color.Transparent else StarColors.Line,
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (message.pending) {
                TypingIndicator()
            } else if (message.fromUser) {
                Text(message.text, color = Color(0xFF0A0C1A), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            } else {
                Column {
                    Text("\u2728 STARGAZE AI", color = StarColors.Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(4.dp))
                    Text(renderMarkup(message.text), color = Color(0xFFD7DCFF), fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
    }
}
