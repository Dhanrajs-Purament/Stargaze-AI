package com.stargaze.ai.ui.sky

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stargaze.ai.astronomy.Angles
import com.stargaze.ai.astronomy.Constellation
import com.stargaze.ai.astronomy.GeoLocation
import com.stargaze.ai.astronomy.SkyEngine
import com.stargaze.ai.astronomy.SkyObject
import com.stargaze.ai.astronomy.StarCatalog
import com.stargaze.ai.data.LocationRepository
import com.stargaze.ai.data.SettingsRepository
import com.stargaze.ai.render.RenderPlanet
import com.stargaze.ai.render.RenderSatellite
import com.stargaze.ai.render.RenderStar
import com.stargaze.ai.render.SkySnapshot
import com.stargaze.ai.render.SkyViewState
import com.stargaze.ai.sensors.DeviceOrientation
import com.stargaze.ai.sensors.OrientationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Sky screen HUD and controls. */
data class SkyUiState(
    val viewState: SkyViewState = SkyViewState(),
    val location: GeoLocation = GeoLocation.MUMBAI,
    val timeOffsetMinutes: Int = 0,
    val selected: SkyObject? = null,
    val sensorAvailable: Boolean = false,
    val identifyState: IdentifyState = IdentifyState.Idle,
)

/** State of the camera-based "Identify sky" plate-solving flow. */
sealed interface IdentifyState {
    data object Idle : IdentifyState
    data object Scanning : IdentifyState
    data class Solved(
        val constellations: List<String>,
        val driftCorrectedDeg: Double,
        val confidence: Double,
    ) : IdentifyState
    data class Failed(val reason: String) : IdentifyState
}

@HiltViewModel
class SkyViewModel @Inject constructor(
    application: Application,
    private val skyEngine: SkyEngine,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val satelliteRepository: com.stargaze.ai.data.SatelliteRepository,
) : AndroidViewModel(application) {

    private val orientationProvider = OrientationProvider(application)
    private val plateSolver = com.stargaze.ai.astronomy.PlateSolver()

    private val _uiState = MutableStateFlow(
        SkyUiState(sensorAvailable = orientationProvider.isAvailable),
    )
    val uiState: StateFlow<SkyUiState> = _uiState.asStateFlow()

    private val _snapshot = MutableStateFlow(SkySnapshot())
    val snapshot: StateFlow<SkySnapshot> = _snapshot.asStateFlow()

    private var sensorJob: Job? = null
    private var recomputeJob: Job? = null

    init {
        // React to persisted display toggles.
        settingsRepository.settings.onEach { s ->
            _uiState.value = _uiState.value.copy(
                viewState = _uiState.value.viewState.copy(
                    nightMode = s.nightMode,
                    showConstellationLines = s.showConstellationLines,
                    showLabels = s.showLabels,
                    showSatellites = s.showSatellites,
                ),
            )
        }.launchIn(viewModelScope)

        // React to location changes.
        locationRepository.location.onEach { loc ->
            _uiState.value = _uiState.value.copy(location = loc)
            recomputeSnapshot()
        }.launchIn(viewModelScope)

        viewModelScope.launch { locationRepository.refresh() }

        // Install the real TLE + SGP4 satellite provider (offline-first: cache/bundled → live).
        viewModelScope.launch {
            runCatching { satelliteRepository.provider() }.getOrNull()?.let {
                skyEngine.satelliteProvider = it
                recomputeSnapshot()
            }
        }
        recomputeSnapshot()
    }

    private fun currentEpochMillis(): Long =
        System.currentTimeMillis() + _uiState.value.timeOffsetMinutes * 60_000L

    /** Recompute all celestial positions for the current location + time. Runs off the main thread. */
    fun recomputeSnapshot() {
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch(Dispatchers.Default) {
            val loc = _uiState.value.location
            val now = currentEpochMillis()
            val stars = skyEngine.stars.map {
                val h = skyEngine.horizontalOf(it, loc, now)
                RenderStar(it, h.azimuthDeg, h.altitudeDeg)
            }
            val planets = skyEngine.planets.map {
                val h = skyEngine.horizontalOf(it, loc, now)
                RenderPlanet(it, h.azimuthDeg, h.altitudeDeg)
            }
            val sats = skyEngine.satellites.map {
                val h = skyEngine.horizontalOf(it, loc, now)
                RenderSatellite(it, h.azimuthDeg, h.altitudeDeg)
            }
            _snapshot.value = SkySnapshot(stars, planets, sats, skyEngine.constellations, loc)
        }
    }

    // ---- View controls ----

    fun pan(deltaAzDeg: Double, deltaAltDeg: Double) {
        if (_uiState.value.viewState.arMode) disableAr()
        val vs = _uiState.value.viewState
        _uiState.value = _uiState.value.copy(
            viewState = vs.copy(
                centerAzimuthDeg = Angles.normalizeDegrees(vs.centerAzimuthDeg + deltaAzDeg),
                centerAltitudeDeg = (vs.centerAltitudeDeg + deltaAltDeg).coerceIn(-10.0, 88.0),
            ),
        )
    }

    fun zoom(scale: Float) {
        val vs = _uiState.value.viewState
        _uiState.value = _uiState.value.copy(
            viewState = vs.copy(fieldOfViewDeg = (vs.fieldOfViewDeg / scale).coerceIn(25.0, 100.0)),
        )
    }

    fun recenter() {
        val vs = _uiState.value.viewState
        _uiState.value = _uiState.value.copy(
            viewState = vs.copy(centerAzimuthDeg = 180.0, centerAltitudeDeg = 35.0, fieldOfViewDeg = 78.0),
            selected = null,
        )
        if (vs.arMode) disableAr()
    }

    fun select(obj: SkyObject?) {
        _uiState.value = _uiState.value.copy(selected = obj)
    }

    /** Point the view at a sky object (used by "locate" / search / tonight). */
    fun locate(obj: SkyObject) {
        val loc = _uiState.value.location
        val now = currentEpochMillis()
        val target = when (obj) {
            is SkyObject.StarObject -> skyEngine.horizontalOf(obj.star, loc, now)
            is SkyObject.PlanetObject -> skyEngine.horizontalOf(obj.planet, loc, now)
            is SkyObject.SatelliteObject -> skyEngine.horizontalOf(obj.satellite, loc, now)
            is SkyObject.ConstellationObject -> skyEngine.centroidOf(obj.constellation, loc, now)
        } ?: return
        val vs = _uiState.value.viewState
        _uiState.value = _uiState.value.copy(
            selected = obj,
            viewState = vs.copy(
                centerAzimuthDeg = target.azimuthDeg,
                centerAltitudeDeg = target.altitudeDeg.coerceAtLeast(12.0),
            ),
        )
    }

    fun locateConstellation(name: String) {
        StarCatalog.byName // ensure loaded
        skyEngine.constellations.firstOrNull { it.name == name }?.let {
            locate(SkyObject.ConstellationObject(it))
        }
    }

    // ---- Time travel ----

    fun setTimeOffsetMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(timeOffsetMinutes = minutes.coerceIn(-720, 720))
        recomputeSnapshot()
    }

    fun resetTime() = setTimeOffsetMinutes(0)

    // ---- AR / sensors ----

    fun toggleAr() {
        if (_uiState.value.viewState.arMode) disableAr() else enableAr()
    }

    private fun enableAr() {
        if (!orientationProvider.isAvailable) return
        _uiState.value = _uiState.value.copy(viewState = _uiState.value.viewState.copy(arMode = true))
        sensorJob?.cancel()
        sensorJob = orientationProvider.orientation()
            .onEach { o: DeviceOrientation ->
                val vs = _uiState.value.viewState
                if (vs.arMode) {
                    _uiState.value = _uiState.value.copy(
                        viewState = vs.copy(
                            centerAzimuthDeg = o.azimuthDeg,
                            centerAltitudeDeg = o.altitudeDeg.coerceIn(-10.0, 88.0),
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun disableAr() {
        sensorJob?.cancel(); sensorJob = null
        _uiState.value = _uiState.value.copy(viewState = _uiState.value.viewState.copy(arMode = false))
    }

    // ---- Settings passthrough ----

    fun toggleNightMode() = viewModelScope.launch {
        settingsRepository.setNightMode(!_uiState.value.viewState.nightMode)
    }
    fun toggleLines() = viewModelScope.launch {
        settingsRepository.setShowConstellationLines(!_uiState.value.viewState.showConstellationLines)
    }
    fun toggleLabels() = viewModelScope.launch {
        settingsRepository.setShowLabels(!_uiState.value.viewState.showLabels)
    }
    fun toggleSatellites() = viewModelScope.launch {
        settingsRepository.setShowSatellites(!_uiState.value.viewState.showSatellites)
    }

    fun refreshLocation() = viewModelScope.launch { locationRepository.refresh() }

    fun constellationByName(name: String): Constellation? =
        skyEngine.constellations.firstOrNull { it.name == name }

    // ---- Camera plate solving ("Identify sky") ----

    fun beginIdentify() {
        _uiState.value = _uiState.value.copy(identifyState = IdentifyState.Scanning)
    }

    fun cancelIdentify() {
        _uiState.value = _uiState.value.copy(identifyState = IdentifyState.Idle)
    }

    /**
     * Runs the plate solver on a captured frame's detections, using the current view orientation as
     * the prior. On success, applies the drift correction (recenters precisely) and reports the
     * identified constellations. Detections are in the analyzer grid; the projection uses the same
     * grid dimensions so pixel coordinates are consistent.
     */
    fun onFrameDetections(stars: List<com.stargaze.ai.astronomy.DetectedStar>, gridWidth: Int, gridHeight: Int) {
        if (_uiState.value.identifyState != IdentifyState.Scanning) return
        val vs = _uiState.value.viewState
        val solution = plateSolver.solve(
            detections = stars,
            location = _uiState.value.location,
            epochMillis = currentEpochMillis(),
            priorAzimuthDeg = vs.centerAzimuthDeg,
            priorAltitudeDeg = vs.centerAltitudeDeg,
            fieldOfViewDeg = vs.fieldOfViewDeg,
            rollDeg = 0.0,
            widthPx = gridWidth,
            heightPx = gridHeight,
        )
        if (solution.solved) {
            val drift = kotlin.math.hypot(solution.azimuthOffsetDeg, solution.altitudeOffsetDeg)
            _uiState.value = _uiState.value.copy(
                viewState = vs.copy(
                    centerAzimuthDeg = solution.correctedAzimuthDeg,
                    centerAltitudeDeg = solution.correctedAltitudeDeg,
                ),
                identifyState = IdentifyState.Solved(
                    constellations = solution.constellations,
                    driftCorrectedDeg = drift,
                    confidence = solution.confidence,
                ),
            )
            recomputeSnapshot()
        }
        // If unsolved, remain in Scanning so the next frame can try again.
    }
}
