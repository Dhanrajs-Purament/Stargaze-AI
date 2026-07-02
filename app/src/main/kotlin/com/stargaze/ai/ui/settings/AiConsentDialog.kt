package com.stargaze.ai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stargaze.ai.ui.onboarding.PrimaryButton
import com.stargaze.ai.ui.theme.StarColors

/**
 * First-run AI & privacy disclosure. No account/login. Presents an honest choice:
 *  - "Keep it private" → on-device only, cloud AI stays OFF (the default, recommended).
 *  - "Allow cloud AI"  → explicit opt-in to send questions to the cloud proxy.
 * Either choice acknowledges the disclosure so it is not shown again.
 */
@Composable
fun AiConsentDialog(
    onChoice: (allowCloud: Boolean) -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC02030A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(StarColors.Bg2)
                .border(1.dp, StarColors.Line, RoundedCornerShape(22.dp))
                .padding(22.dp),
        ) {
            Text("\u2728", fontSize = 40.sp)
            Spacer(Modifier.height(10.dp))
            Text("How the AI Sky Guide works", color = StarColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "You're chatting with an automated AI tutor — answers can be wrong and are for " +
                    "education only, not professional advice.",
                color = StarColors.Muted, fontSize = 13.5.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(12.dp))
            Bullet("Most answers are computed right on your device — nothing leaves your phone.")
            Bullet("You can add an optional on-device AI model for richer answers, still fully private.")
            Bullet("Cloud AI is OFF by default. Turn it on only if you want the richest answers; then your question text and coarse location are sent securely to our AI service.")
            Spacer(Modifier.height(8.dp))
            Text(
                "Read our Privacy Policy",
                color = StarColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onOpenPrivacy)
                    .semantics { role = Role.Button; contentDescription = "Read privacy policy" }
                    .padding(vertical = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Keep it private (recommended)", modifier = Modifier.fillMaxWidth()) { onChoice(false) }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StarColors.Card)
                    .border(1.dp, StarColors.Line, RoundedCornerShape(16.dp))
                    .clickable { onChoice(true) }
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = "Allow cloud AI"
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Allow cloud AI", color = StarColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text("\u2022", color = StarColors.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color(0xFFD7DCFF), fontSize = 13.sp, lineHeight = 19.sp)
    }
}
