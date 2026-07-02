package com.stargaze.ai.ui.tonight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stargaze.ai.astronomy.GeoLocation
import com.stargaze.ai.astronomy.SkyEngine
import com.stargaze.ai.astronomy.SkyEvent
import com.stargaze.ai.astronomy.SkyObject
import com.stargaze.ai.data.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class UpItem(val obj: SkyObject, val name: String, val meta: String, val live: Boolean)

data class TonightUiState(
    val dateLabel: String = "",
    val locationName: String = "",
    val qualityScore: Int = 3,
    val headline: String = "",
    val brief: String = "",
    val upItems: List<UpItem> = emptyList(),
    val events: List<SkyEvent> = emptyList(),
)

@HiltViewModel
class TonightViewModel @Inject constructor(
    private val skyEngine: SkyEngine,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TonightUiState())
    val uiState: StateFlow<TonightUiState> = _uiState.asStateFlow()

    init {
        locationRepository.location.onEach { recompute(it) }.launchIn(viewModelScope)
        recompute(locationRepository.location.value)
    }

    fun recompute(location: GeoLocation) {
        val now = System.currentTimeMillis()
        val zoned = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
        val hour = zoned.hour

        val planetsUp = skyEngine.planetsUp(location, now)
        val starsUp = skyEngine.brightStarsUp(location, now, limit = 5)
        val satsUp = skyEngine.satellitesUp(location, now)

        val score = when {
            hour in 19..23 || hour in 0..4 -> 5
            else -> 2
        }
        val headline = when {
            score >= 4 -> "Great sky"
            score >= 3 -> "Fair sky"
            else -> "Daytime"
        }

        val up = buildList {
            satsUp.forEach { p ->
                val s = (p.obj as SkyObject.SatelliteObject).satellite
                add(UpItem(p.obj, "${s.emoji} ${s.shortName}", "Alt ${p.horizontal.altitudeDeg.roundToInt()}\u00B0 \u00B7 ${azName(p.horizontal.azimuthDeg)}", live = true))
            }
            planetsUp.forEach { p ->
                val pl = (p.obj as SkyObject.PlanetObject).planet
                add(UpItem(p.obj, "${pl.symbol} ${pl.displayName}", "Alt ${p.horizontal.altitudeDeg.roundToInt()}\u00B0 \u00B7 ${azName(p.horizontal.azimuthDeg)}", live = false))
            }
            starsUp.forEach { p ->
                val st = (p.obj as SkyObject.StarObject).star
                add(UpItem(p.obj, st.name, "${st.constellation} \u00B7 Alt ${p.horizontal.altitudeDeg.roundToInt()}\u00B0", live = false))
            }
        }

        _uiState.value = TonightUiState(
            dateLabel = zoned.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
            locationName = location.name.ifBlank { "Your location" },
            qualityScore = score,
            headline = headline,
            brief = buildBrief(hour, planetsUp.map { (it.obj as SkyObject.PlanetObject).planet.displayName }, starsUp.map { (it.obj as SkyObject.StarObject).star.name }),
            upItems = up,
            events = skyEngine.events,
        )
    }

    private fun buildBrief(hour: Int, planets: List<String>, stars: List<String>): String {
        if (hour in 6..17) {
            return "It's daytime where you are - the Sun rules the sky. Come back after sunset, or scrub the time slider forward to preview tonight's stars."
        }
        val parts = buildList {
            if (planets.isNotEmpty()) add("Look for ${planets.take(3).joinToString(", ")} above the horizon")
            if (stars.isNotEmpty()) add("the brightest star up is ${stars.first()}")
        }
        return (parts.joinToString(", and ") + ". ") +
            "Find a spot away from city lights, let your eyes adapt for 15 minutes, and point your phone up to begin."
    }

    private fun azName(az: Double): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return dirs[((az / 45.0).roundToInt()) % 8]
    }
}
