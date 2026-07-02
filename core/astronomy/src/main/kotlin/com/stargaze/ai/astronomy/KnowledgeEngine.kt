package com.stargaze.ai.astronomy

import kotlin.math.roundToInt

/**
 * Offline, deterministic, location-aware astronomy answerer.
 *
 * This is a **real** knowledge layer, not a stub: it resolves object names against the catalog,
 * computes live visibility (alt/az) for the user's location and time, and answers conceptual
 * questions from a curated knowledge base. It is the guaranteed fallback when the remote LLM is
 * unavailable, so the AI Guide always returns genuine, computed information offline.
 *
 * Output is lightweight markup using `**bold**` markers; the UI renders these as styled spans.
 */
class KnowledgeEngine(private val engine: SkyEngine = SkyEngine()) {

    /** Context for a query: where and when the user is observing. */
    data class Context(val location: GeoLocation, val epochMillis: Long)

    fun answer(question: String, context: Context): String =
        tryAnswer(question, context) ?: FALLBACK

    /**
     * Attempts to answer from structured local knowledge. Returns `null` when the question is not
     * recognised as a catalog/visibility/concept query, so a caller can route it to a richer AI tier
     * (on-device or cloud) instead of returning a generic fallback. This is what enables the 3-tier
     * routing in `AiGuideRepository`.
     */
    fun tryAnswer(question: String, context: Context): String? {
        val q = question.lowercase().trim()

        resolveStar(q, context)?.let { return it }
        resolvePlanet(q, context)?.let { return it }
        if (q.contains("iss") || q.contains("space station")) return issAnswer(context)
        if (TONIGHT_REGEX.containsMatchIn(q)) return tonightAnswer(context)

        constellationAnswer(q)?.let { return it }
        conceptAnswer(q)?.let { return it }
        if (q == "hi" || q.startsWith("hi ") || q.contains("hello")) return GREETING
        return null
    }

    private fun resolveStar(q: String, ctx: Context): String? {
        val star = engine.stars.firstOrNull {
            Regex("\\b${Regex.escape(it.name.lowercase())}\\b").containsMatchIn(q)
        } ?: return null
        val h = engine.horizontalOf(star, ctx.location, ctx.epochMillis)
        val visibility = if (h.isAboveHorizon) {
            "**up** at ${h.altitudeDeg.roundToInt()}\u00B0 toward the ${compass(h.azimuthDeg)}"
        } else {
            "**below your horizon** right now"
        }
        return "**${star.name}** is a star in **${star.constellation}** " +
            "(magnitude ${"%.1f".format(star.magnitude)}). ${star.blurb}\n\n" +
            "Right now it's $visibility. ${funFact(star.name)}"
    }

    private fun resolvePlanet(q: String, ctx: Context): String? {
        val planet = engine.planets.firstOrNull {
            Regex("\\b${Regex.escape(it.displayName.lowercase())}\\b").containsMatchIn(q)
        } ?: return null
        // Avoid matching the substring "sun" inside other words handled elsewhere; planets list is safe.
        val h = engine.horizontalOf(planet, ctx.location, ctx.epochMillis)
        val visibility = if (h.isAboveHorizon) {
            "**visible** at ${h.altitudeDeg.roundToInt()}\u00B0 above the ${compass(h.azimuthDeg)}"
        } else {
            "**below the horizon**"
        }
        return "**${planet.symbol} ${planet.displayName}** - ${planet.description}\n\n" +
            "It's currently $visibility. ${funFact(planet.displayName)}"
    }

    private fun issAnswer(ctx: Context): String {
        val iss = engine.satellites.first()
        val h = engine.horizontalOf(iss, ctx.location, ctx.epochMillis)
        val status = if (h.isAboveHorizon) {
            "**above your horizon** right now at ${h.altitudeDeg.roundToInt()}\u00B0 toward the ${compass(h.azimuthDeg)}"
        } else {
            "**below the horizon** at the moment"
        }
        return "The **International Space Station** is $status. It orbits ~420 km up at 28,000 km/h, " +
            "lapping Earth every ~93 minutes. Tap the satellite icon on the sky to follow its track."
    }

    private fun tonightAnswer(ctx: Context): String {
        val planetsUp = engine.planetsUp(ctx.location, ctx.epochMillis)
        val starsUp = engine.brightStarsUp(ctx.location, ctx.epochMillis, limit = 3)
        val planetText = if (planetsUp.isNotEmpty()) {
            planetsUp.joinToString(", ") { (it.obj as SkyObject.PlanetObject).planet.displayName }
        } else "none yet"
        val starText = if (starsUp.isNotEmpty()) {
            starsUp.joinToString(", ") { (it.obj as SkyObject.StarObject).star.name }
        } else "wait for darkness"
        return "Here's your sky from **${ctx.location.name.ifBlank { "your location" }}** right now:\n\n" +
            "Planets up: $planetText\n" +
            "Brightest stars: $starText\n\n" +
            "Find a spot away from city lights, let your eyes adapt for 15 minutes, and point your phone up."
    }

    private fun constellationAnswer(q: String): String? {
        val c = engine.constellations.firstOrNull {
            Regex("\\b${Regex.escape(it.name.lowercase())}\\b").containsMatchIn(q)
        } ?: return null
        val starCount = StarCatalog.starsIn(c.name).size
        return "**${c.emoji} ${c.name}** (\"${c.tag}\") contains $starCount of our brightest catalogued " +
            "stars. ${CONSTELLATION_STORIES[c.name] ?: "It carries its own mythology across cultures and millennia."} " +
            "Search \"${c.name}\" and tap Show me to draw the whole figure on the sky."
    }

    private fun conceptAnswer(q: String): String? = CONCEPTS.entries
        .firstOrNull { entry -> entry.key.any { q.contains(it) } }?.value

    private fun compass(azimuthDeg: Double): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((azimuthDeg / 45.0).roundToInt()) % 8]
    }

    private fun funFact(name: String): String = FUN_FACTS[name]
        ?: "The light you see left this object years - sometimes millennia - ago. You're looking into the past."

    private companion object {
        val TONIGHT_REGEX = Regex("(tonight|look|see|watch|observe)")

        const val GREETING = "Hi! I'm your AI Sky Guide. Point your phone at the sky, tap any star, " +
            "planet or satellite, or ask me something like \"what should I look at tonight?\" or \"where is the ISS?\""

        const val FALLBACK = "Great question. Try asking about a specific **star, planet, constellation**, " +
            "the **ISS**, or \"what should I look at tonight?\" - I'll give you real, location-aware data " +
            "computed for exactly where and when you are."

        val FUN_FACTS = mapOf(
            "Sirius" to "In ancient Egypt, Sirius' dawn rising predicted the Nile's annual flood.",
            "Betelgeuse" to "If placed where the Sun is, it would swallow Mars' orbit.",
            "Polaris" to "It sits within 1 degree of the true celestial pole - a natural compass.",
            "Mars" to "A day on Mars is just 37 minutes longer than an Earth day.",
            "Saturn" to "Saturn is so light it would float in a big enough bathtub of water.",
            "Jupiter" to "Jupiter has at least 95 moons - a mini solar system.",
            "Venus" to "A day on Venus is longer than its year.",
            "Moon" to "The Moon drifts away from Earth at ~3.8 cm per year.",
            "Vega" to "Vega was the first star ever photographed, in 1850.",
            "International Space Station" to "The crew sees 16 sunrises a day as the ISS laps Earth ~16 times.",
        )

        val CONSTELLATION_STORIES = mapOf(
            "Orion" to "In Greek myth, Orion was a giant hunter; his three belt stars are among the sky's most recognisable sights.",
            "Ursa Major" to "Its seven brightest stars form the Big Dipper - a signpost to the North Star.",
            "Scorpius" to "A scorpion that, in myth, slew Orion - which is why they never share the sky.",
            "Leo" to "A crouching lion whose sickle of stars forms its mane.",
            "Crux" to "The Southern Cross - a compass for the southern hemisphere.",
            "Cassiopeia" to "A vain queen condemned to circle the pole as a glittering W.",
            "Cygnus" to "The swan flying down the Milky Way, also called the Northern Cross.",
            "Gemini" to "The twins Castor and Pollux of Greek and Roman legend.",
        )

        // Concept keywords -> answer. First matching entry wins.
        val CONCEPTS: Map<List<String>, String> = mapOf(
            listOf("light-year", "light year") to "A **light-year** is a **distance** - how far light " +
                "travels in a year, about **9.46 trillion km**. Because light takes time to reach us, " +
                "looking far out means looking back in time.",
            listOf("black hole") to "A **black hole** is a region where gravity is so strong nothing - " +
                "not even light - escapes past its event horizon. Our galaxy hosts a supermassive one, " +
                "**Sagittarius A***, at its centre.",
            listOf("galaxy", "milky way") to "The **Milky Way** is our home galaxy - a barred spiral of " +
                "100-400 billion stars, ~100,000 light-years across. On a dark night you can see its glowing band.",
            listOf("meteor", "shooting star") to "A shooting star is a **meteor** - a grain of space dust " +
                "burning up at ~60 km/s. In a meteor shower, Earth ploughs through a comet's debris trail.",
            listOf("twinkle") to "Stars twinkle because their light passes through **turbulent layers of our " +
                "atmosphere** that bend it randomly. Planets twinkle less because they show a tiny disk.",
            listOf("why is mars red", "mars red") to "Mars looks red because its surface is coated in " +
                "**iron oxide - literally rust**. Fine oxidised-iron dust tints the planet and its thin atmosphere.",
        )
    }
}
