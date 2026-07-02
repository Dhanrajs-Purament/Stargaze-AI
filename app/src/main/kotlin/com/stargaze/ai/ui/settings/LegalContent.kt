package com.stargaze.ai.ui.settings

/** Concise, in-app legal/privacy summaries. The full canonical documents live in docs/legal and on
 *  the published website; these in-app versions keep the user informed without leaving the app. */
object LegalContent {

    data class Doc(val title: String, val body: String)

    val privacy = Doc(
        "Privacy Policy",
        """
        StarGaze AI is published by Hacklet Pvt Ltd (Thane, Maharashtra, India). It is offline-first
        and account-less. By default, your data stays on your device.

        WHAT WE PROCESS
        • Location — used only on your device to compute what's in your sky. Not sent to us unless you
          enable cloud AI and ask a location-aware question.
        • Camera — used only for the live AR background. Frames are never recorded or transmitted.
        • Settings & AI usage count — stored locally on your device.
        • AI questions — answered on-device by default. Sent to our AI service only if you turn on
          cloud AI.

        WHAT WE DON'T DO
        • No accounts, no login. No third-party advertising or tracking SDKs. We never sell your data.

        YOUR CONTROLS & RIGHTS
        • Cloud AI is off by default; toggle it any time in the AI settings.
        • Clear app data or uninstall to remove all on-device data.
        • Depending on your region (GDPR, India DPDP, CCPA) you may have rights to access or delete
          data; for cloud-AI data contact privacy@hacklet.in.

        SECURITY
        • HTTPS-only networking, no secrets in the app, encrypted storage for any sensitive token,
          least-privilege permissions.

        This is an in-app summary. See the full Privacy Policy on our website.
        """.trimIndent(),
    )

    val terms = Doc(
        "Terms of Service",
        """
        By using StarGaze AI you agree to these terms (summary).

        • The app is an educational stargazing companion. All content, including AI answers, is for
          general education only — not professional, navigational, medical, legal, or safety advice,
          and may be inaccurate.
        • Don't misuse the app or submit unlawful or harmful content to the AI.
        • Pro is billed through the platform's billing system; renewals, trials, and refunds follow
          the platform's terms and your consumer rights.
        • The app is provided "as is" to the extent permitted by law; your statutory consumer rights
          are unaffected.

        See the full Terms of Service on our website.
        """.trimIndent(),
    )

    val aiDisclosure = Doc(
        "AI Disclosure",
        """
        You are interacting with an automated AI system, not a human. Answers may be incomplete or
        wrong and are for education only.

        HOW ANSWERS ARE GENERATED (most private first)
        1. On-device knowledge engine — deterministic, computed locally. Nothing leaves your device.
        2. On-device AI model (optional) — Google Gemma 3n runs on your phone. Nothing leaves your
           device.
        3. Cloud AI (opt-in, off by default) — your question and coarse location/time are sent over
           HTTPS to our AI service.

        AI models can "hallucinate" and reflect biases. Always verify important information.
        Gemma is used under the Gemma Terms of Use and Prohibited Use Policy.

        CAMERA / SKY ANALYSIS
        When you use "Identify the sky", the camera image is processed ON YOUR DEVICE to detect stars
        and identify constellations — the photo is not uploaded. If you ask the AI to explain the
        scene and have enabled cloud AI, a still image and a computed description may be sent securely
        to our AI service; otherwise everything stays on your device.
        """.trimIndent(),
    )

    val all = listOf(privacy, aiDisclosure, terms)
}
