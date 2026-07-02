package com.stargaze.ai.astronomy

/** Constellation figures (line segments by star name) ported from the validated prototype. */
object ConstellationCatalog {

    val constellations: List<Constellation> = listOf(
        Constellation("Orion", "🏹", "The Hunter", listOf(
            "Betelgeuse" to "Bellatrix", "Bellatrix" to "Mintaka", "Mintaka" to "Alnilam",
            "Alnilam" to "Alnitak", "Alnitak" to "Betelgeuse", "Mintaka" to "Rigel",
            "Alnitak" to "Saiph", "Saiph" to "Rigel",
        )),
        Constellation("Ursa Major", "🐻", "The Great Bear", listOf(
            "Alkaid" to "Mizar", "Mizar" to "Alioth", "Alioth" to "Megrez", "Megrez" to "Dubhe",
            "Dubhe" to "Merak", "Merak" to "Phecda", "Phecda" to "Megrez",
        )),
        Constellation("Ursa Minor", "🧸", "The Little Bear", listOf(
            "Polaris" to "Kochab", "Kochab" to "Pherkad",
        )),
        Constellation("Cassiopeia", "👑", "The Queen", listOf(
            "Caph" to "Schedar", "Schedar" to "Gamma Cas", "Gamma Cas" to "Ruchbah", "Ruchbah" to "Segin",
        )),
        Constellation("Canis Major", "🐕", "The Great Dog", listOf(
            "Mirzam" to "Sirius", "Sirius" to "Wezen", "Wezen" to "Adhara", "Adhara" to "Sirius",
        )),
        Constellation("Scorpius", "🦂", "The Scorpion", listOf(
            "Dschubba" to "Antares", "Antares" to "Wei", "Wei" to "Sargas", "Sargas" to "Shaula",
        )),
        Constellation("Gemini", "👯", "The Twins", listOf(
            "Castor" to "Pollux", "Pollux" to "Alhena",
        )),
        Constellation("Leo", "🦁", "The Lion", listOf(
            "Regulus" to "Algieba", "Algieba" to "Zosma", "Zosma" to "Denebola",
        )),
        Constellation("Pegasus", "🐎", "The Winged Horse", listOf(
            "Markab" to "Scheat", "Scheat" to "Algenib", "Algenib" to "Markab", "Markab" to "Enif",
        )),
        Constellation("Andromeda", "⛓️", "The Chained Princess", listOf(
            "Alpheratz" to "Mirach", "Mirach" to "Almach",
        )),
        Constellation("Cygnus", "🦢", "The Swan", listOf(
            "Deneb" to "Sadr", "Sadr" to "Albireo", "Sadr" to "Gienah Cygni", "Sadr" to "Delta Cygni",
        )),
        Constellation("Crux", "✝️", "The Southern Cross", listOf(
            "Acrux" to "Gacrux", "Mimosa" to "Delta Cru",
        )),
        Constellation("Auriga", "🚗", "The Charioteer", listOf(
            "Capella" to "Menkalinan", "Menkalinan" to "Elnath", "Elnath" to "Capella",
        )),
        Constellation("Perseus", "⚔️", "The Hero", listOf(
            "Mirfak" to "Algol",
        )),
        Constellation("Taurus", "🐂", "The Bull", listOf(
            "Aldebaran" to "Elnath",
        )),
        Constellation("Sagittarius", "🏹", "The Archer", listOf(
            "Kaus Australis" to "Nunki",
        )),
    )

    val byName: Map<String, Constellation> = constellations.associateBy { it.name }
}

/** Solar-system catalog with presentation colours and educational descriptions. */
object PlanetCatalog {

    val planets: List<Planet> = listOf(
        Planet(Body.SUN, 0xFFFFD36B, "Our star - a 4.6-billion-year-old ball of plasma lighting the solar system."),
        Planet(Body.MOON, 0xFFDFE6FF, "Earth's only natural satellite; it controls the tides and shows phases."),
        Planet(Body.MERCURY, 0xFFC9B8A0, "The smallest planet and the fastest, whipping around the Sun in 88 days."),
        Planet(Body.VENUS, 0xFFFFE7A8, "The hottest planet, cloaked in thick clouds of sulfuric acid."),
        Planet(Body.MARS, 0xFFFF8A5C, "The Red Planet - rusty soil, giant volcanoes, and a thin atmosphere."),
        Planet(Body.JUPITER, 0xFFE8C9A0, "The largest planet; its Great Red Spot is a centuries-old storm."),
        Planet(Body.SATURN, 0xFFF0DCAA, "The jewel of the solar system, famous for its dazzling rings."),
    )

    val byBody: Map<Body, Planet> = planets.associateBy { it.body }
}
