package com.stargaze.ai.ui.sky

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stargaze.ai.astronomy.Body
import com.stargaze.ai.astronomy.CoordinateTransforms
import com.stargaze.ai.astronomy.Ephemeris
import com.stargaze.ai.astronomy.GeoLocation
import com.stargaze.ai.astronomy.Horizontal
import com.stargaze.ai.astronomy.SkyObject
import com.stargaze.ai.astronomy.StarCatalog
import com.stargaze.ai.ui.components.GlassCard
import com.stargaze.ai.ui.components.SkySheet
import kotlin.math.roundToInt
import com.stargaze.ai.ui.theme.StarColors

private fun azName(az: Double): String {
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return dirs[((az / 45.0).roundToInt()) % 8]
}

/** Detail sheet for a tapped/selected sky object, with live-computed facts. */
@Composable
fun ObjectDetailSheet(
    obj: SkyObject,
    location: GeoLocation,
    timeOffsetMinutes: Int,
    onLocate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val epochMillis = remember(timeOffsetMinutes) {
        System.currentTimeMillis() + timeOffsetMinutes * 60_000L
    }

    val (title, subtitle, blurb, facts, _) = buildDetail(obj, location, epochMillis)

    SkySheet(onDismiss = onDismiss) {
        Text(title, color = StarColors.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = StarColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

        // Locate button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(StarColors.Card)
                .border(1.dp, StarColors.Line, RoundedCornerShape(13.dp))
                .clickable { onLocate(); onDismiss() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text("\uD83E\uDDED  Point me to $title", color = Color(0xFFCDD6FF), fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }

        Spacer(Modifier.height(12.dp))

        // Facts grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(((facts.size + 1) / 2 * 64).dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
        ) {
            items(facts.size) { i ->
                FactCell(facts[i].first, facts[i].second)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(blurb, color = Color(0xFFD7DCFF), fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun FactCell(key: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(StarColors.Card)
            .border(1.dp, StarColors.Line, RoundedCornerShape(13.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Text(key.uppercase(), color = StarColors.Faint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = StarColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

private data class Detail(
    val title: String,
    val subtitle: String,
    val blurb: String,
    val facts: List<Pair<String, String>>,
    val color: Color,
)

private fun buildDetail(obj: SkyObject, location: GeoLocation, epochMillis: Long): Detail = when (obj) {
    is SkyObject.StarObject -> {
        val s = obj.star
        val h = CoordinateTransforms.equatorialToHorizontal(s.equatorial, location, epochMillis)
        Detail(
            title = s.name,
            subtitle = "${s.constellation} \u00B7 Star \u00B7 ${visibility(h)}",
            blurb = s.blurb,
            facts = listOf(
                "Magnitude" to "%.2f".format(s.magnitude),
                "Constellation" to s.constellation,
                "Altitude" to "${h.altitudeDeg.roundToInt()}\u00B0",
                "Direction" to "${azName(h.azimuthDeg)} ${h.azimuthDeg.roundToInt()}\u00B0",
                "Right Asc." to "%.1fh".format(s.rightAscensionDeg / 15.0),
                "Declination" to "${s.declinationDeg.roundToInt()}\u00B0",
            ),
            color = StarColors.Accent,
        )
    }
    is SkyObject.PlanetObject -> {
        val p = obj.planet
        val eq = Ephemeris.position(p.body, epochMillis)
        val h = CoordinateTransforms.equatorialToHorizontal(eq, location, epochMillis)
        Detail(
            title = "${p.symbol} ${p.displayName}",
            subtitle = "Solar System \u00B7 ${visibility(h)}",
            blurb = p.description,
            facts = listOf(
                "Type" to when (p.body) { Body.SUN -> "Star"; Body.MOON -> "Satellite"; else -> "Planet" },
                "Altitude" to "${h.altitudeDeg.roundToInt()}\u00B0",
                "Direction" to "${azName(h.azimuthDeg)} ${h.azimuthDeg.roundToInt()}\u00B0",
                "Visible now" to if (h.isAboveHorizon) "Yes" else "Below horizon",
                "Right Asc." to "%.1fh".format(eq.rightAscensionDeg / 15.0),
                "Declination" to "${eq.declinationDeg.roundToInt()}\u00B0",
            ),
            color = Color(p.colorHex),
        )
    }
    is SkyObject.SatelliteObject -> {
        val sat = obj.satellite
        Detail(
            title = "${sat.emoji} ${sat.shortName}",
            subtitle = "${sat.name} \u00B7 Satellite",
            blurb = sat.description,
            facts = listOf(
                "Object" to sat.shortName,
                "Orbit period" to "${sat.orbitalPeriodMinutes.roundToInt()} min",
                "Inclination" to "${sat.inclinationDeg}\u00B0",
                "Tracking" to "Live",
            ),
            color = Color(sat.colorHex),
        )
    }
    is SkyObject.ConstellationObject -> {
        val c = obj.constellation
        val starCount = StarCatalog.starsIn(c.name).size
        Detail(
            title = "${c.emoji} ${c.name}",
            subtitle = "${c.tag} \u00B7 $starCount catalogued stars",
            blurb = "${c.name} (\"${c.tag}\") is one of the sky's recognisable figures. Tap \"Point me to\" to draw it.",
            facts = listOf("Stars" to "$starCount", "Type" to "Constellation"),
            color = StarColors.Accent,
        )
    }
}

private fun visibility(h: Horizontal): String = if (h.isAboveHorizon) "Up now" else "Below horizon"
