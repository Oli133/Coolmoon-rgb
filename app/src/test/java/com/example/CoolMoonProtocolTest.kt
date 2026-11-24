package com.example

import com.example.data.PlaceholderLightingArchitectRepository
import com.example.hardware.CoolMoonProtocolEncoder
import com.example.model.LightingEffect
import com.example.model.RgbColor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoolMoonProtocolTest {

    @Test
    fun magicHomeColorFrame_hasExpectedChecksum() {
        val frame = CoolMoonProtocolEncoder.encodeMagicHomeColor(RgbColor(255, 0, 128), 100)
        assertEquals(8, frame.size)
        assertEquals(0x31, frame[0].toInt() and 0xFF)
        assertEquals(255, frame[1].toInt() and 0xFF)
        assertEquals(0, frame[2].toInt() and 0xFF)
        assertEquals(128, frame[3].toInt() and 0xFF)
        val expectedChecksum = (0x31 + 255 + 0 + 128 + 0 + 0 + 0x0F) and 0xFF
        assertEquals(expectedChecksum, frame[7].toInt() and 0xFF)
    }

    @Test
    fun necIrPattern_has67PulsesAndLeader() {
        val pattern = CoolMoonProtocolEncoder.encodeNecIrPattern(0x45)
        assertEquals(67, pattern.size)
        assertEquals(9000, pattern[0])
        assertEquals(4500, pattern[1])
        assertEquals(562, pattern.last())
    }

    @Test
    fun wledJson_encodesOnBrightnessAndColors() {
        val json = CoolMoonProtocolEncoder.encodeWledJson(
            isOn = true,
            primary = RgbColor(0, 240, 255),
            secondary = RgbColor(168, 85, 247),
            effect = LightingEffect.RAINBOW_WAVE,
            brightnessPercent = 100,
            speedPercent = 50
        )
        assertTrue(json.contains("\"on\":true"))
        assertTrue(json.contains("\"bri\":255"))
        assertTrue(json.contains("[0,240,255]"))
    }

    @Test
    fun placeholderArchitect_returnsClearlyLabeledSampleResponse() = runTest {
        val repo = PlaceholderLightingArchitectRepository()
        val result = repo.designLightingPlan("Warm sunset desk bias", activeZoneCount = 6)
        assertTrue(result.isSuccess)
        val plan = result.getOrThrow()
        assertTrue(plan.isSampleResponse)
        assertEquals(LightingEffect.BREATHING, plan.effect)
    }
}
