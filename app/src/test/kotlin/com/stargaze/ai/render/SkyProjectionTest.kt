package com.stargaze.ai.render

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the gnomonic sky projection. Pure math (no Android), so it runs as a JVM unit test.
 */
class SkyProjectionTest {

    private fun projection() = SkyProjection(
        viewWidthPx = 1080f,
        viewHeightPx = 1920f,
        centerAzimuthDeg = 180.0,
        centerAltitudeDeg = 35.0,
        fieldOfViewDeg = 78.0,
    )

    @Test
    fun objectAtViewCenter_projectsToScreenCenter() {
        val p = projection()
        val pt = p.project(180.0, 35.0)
        assertNotNull(pt)
        // Centre of the screen, within a pixel.
        assertTrue(kotlin.math.abs(pt!!.x - 540f) < 1f)
        assertTrue(kotlin.math.abs(pt.y - 960f) < 1f)
    }

    @Test
    fun objectBehindViewer_isNotProjectable() {
        val p = projection()
        // Looking south (180); an object due north and low should be behind the view.
        val pt = p.project(0.0, -20.0)
        assertNull(pt)
    }

    @Test
    fun objectToTheRight_projectsRightOfCentre() {
        val p = projection()
        val pt = p.project(200.0, 35.0) // 20 deg east of centre
        assertNotNull(pt)
        assertTrue("expected x > centre", pt!!.x > 540f)
    }

    @Test
    fun higherObject_projectsAboveCentre() {
        val p = projection()
        val pt = p.project(180.0, 55.0) // 20 deg higher than centre
        assertNotNull(pt)
        assertTrue("expected y < centre (screen y grows downward)", pt!!.y < 960f)
    }
}
