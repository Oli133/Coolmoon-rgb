package com.example.hardware

import com.example.model.LightingEffect
import com.example.model.RgbColor
import kotlin.math.roundToInt

object CoolMoonProtocolEncoder {

    private const val COOLMOON_IR_ADDRESS = 0x00EF

    /**
     * MagicHome / CoolMoon Wi-Fi & BLE RGBWW static color frame:
     * [0x31, R, G, B, 0x00, 0x00, 0x0F, checksum]
     */
    fun encodeMagicHomeColor(color: RgbColor, brightnessPercent: Int): ByteArray {
        val scale = (brightnessPercent.coerceIn(0, 100) / 100f)
        val r = (color.red * scale).roundToInt().coerceIn(0, 255)
        val g = (color.green * scale).roundToInt().coerceIn(0, 255)
        val b = (color.blue * scale).roundToInt().coerceIn(0, 255)
        val raw = byteArrayOf(
            0x31,
            r.toByte(),
            g.toByte(),
            b.toByte(),
            0x00,
            0x00,
            0x0F
        )
        return appendChecksum(raw)
    }

    /**
     * MagicHome / CoolMoon power toggle frame:
     * ON:  [0x71, 0x23, 0x0F, checksum]
     * OFF: [0x71, 0x24, 0x0F, checksum]
     */
    fun encodeMagicHomePower(isOn: Boolean): ByteArray {
        val cmd = if (isOn) 0x23 else 0x24
        return appendChecksum(byteArrayOf(0x71, cmd.toByte(), 0x0F))
    }

    /**
     * MagicHome / CoolMoon built-in effect frame:
     * [0x61, modeByte, speedDelay(1..31), 0x0F, checksum]
     * Note: MagicHome speed is inverted delay (1 = fastest, 31 = slowest).
     */
    fun encodeMagicHomeEffect(effect: LightingEffect, speedPercent: Int): ByteArray {
        val clamped = speedPercent.coerceIn(1, 100)
        val delay = (31 - ((clamped - 1) * 30 / 99)).coerceIn(1, 31)
        return appendChecksum(
            byteArrayOf(
                0x61,
                effect.protocolModeByte.toByte(),
                delay.toByte(),
                0x0F
            )
        )
    }

    /**
     * ELK-BLEDOM / CoolMoon BLE 9-byte RGB GATT frame:
     * [0x7E, 0x00, 0x05, 0x03, R, G, B, 0x00, 0xEF]
     */
    fun encodeBleElkColor(color: RgbColor, brightnessPercent: Int): ByteArray {
        val scale = brightnessPercent.coerceIn(0, 100) / 100f
        val r = (color.red * scale).roundToInt().coerceIn(0, 255)
        val g = (color.green * scale).roundToInt().coerceIn(0, 255)
        val b = (color.blue * scale).roundToInt().coerceIn(0, 255)
        return byteArrayOf(
            0x7E,
            0x00,
            0x05,
            0x03,
            r.toByte(),
            g.toByte(),
            b.toByte(),
            0x00,
            0xEF.toByte()
        )
    }

    /**
     * ELK-BLEDOM / CoolMoon BLE power frame:
     * ON:  [0x7E, 0x00, 0x04, 0xF0, 0x00, 0x01, 0xFF, 0x00, 0xEF]
     * OFF: [0x7E, 0x00, 0x04, 0x00, 0x00, 0x00, 0xFF, 0x00, 0xEF]
     */
    fun encodeBleElkPower(isOn: Boolean): ByteArray {
        return if (isOn) {
            byteArrayOf(0x7E, 0x00, 0x04, 0xF0.toByte(), 0x00, 0x01, 0xFF.toByte(), 0x00, 0xEF.toByte())
        } else {
            byteArrayOf(0x7E, 0x00, 0x04, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0xEF.toByte())
        }
    }

    /**
     * WLED JSON HTTP API state payload.
     */
    fun encodeWledJson(
        isOn: Boolean,
        primary: RgbColor,
        secondary: RgbColor,
        effect: LightingEffect,
        brightnessPercent: Int,
        speedPercent: Int
    ): String {
        val bri = (brightnessPercent.coerceIn(0, 100) * 255) / 100
        val sx = (speedPercent.coerceIn(0, 100) * 255) / 100
        val fxId = when (effect) {
            LightingEffect.STATIC -> 0
            LightingEffect.BREATHING -> 2
            LightingEffect.RAINBOW_WAVE -> 9
            LightingEffect.DUAL_RING_CHASE -> 28
            LightingEffect.COMET -> 59
            LightingEffect.MARQUEE -> 33
            LightingEffect.METEOR -> 76
            LightingEffect.CANDLE_FLICKER -> 88
            LightingEffect.MOTHERBOARD_SYNC -> 0
        }
        return """{"on":$isOn,"bri":$bri,"seg":[{"col":[[${primary.red},${primary.green},${primary.blue}],[${secondary.red},${secondary.green},${secondary.blue}]],"fx":$fxId,"sx":$sx}]}"""
    }

    /**
     * WLED UDP DRGB Realtime frame: [0x02 (DRGB), 0x02 (2s timeout), R0, G0, B0, ...]
     */
    fun encodeWledUdpDrgb(pixels: List<RgbColor>): ByteArray {
        val out = ByteArray(2 + pixels.size * 3)
        out[0] = 0x02
        out[1] = 0x02
        pixels.forEachIndexed { idx, c ->
            val offset = 2 + idx * 3
            out[offset] = c.red.coerceIn(0, 255).toByte()
            out[offset + 1] = c.green.coerceIn(0, 255).toByte()
            out[offset + 2] = c.blue.coerceIn(0, 255).toByte()
        }
        return out
    }

    /**
     * Generates a 38kHz NEC Infrared microsecond pulse pattern for Android ConsumerIrManager.
     * Frame: 9ms header mark, 4.5ms header space, 32 bits LSB-first (16-bit addr + cmd + ~cmd),
     * 562.5us trailing stop mark.
     */
    fun encodeNecIrPattern(commandByte: Int, address: Int = COOLMOON_IR_ADDRESS): IntArray {
        val pulses = ArrayList<Int>(68)
        // Leader pulse + space
        pulses.add(9000)
        pulses.add(4500)

        val cmd = commandByte and 0xFF
        val invCmd = cmd.inv() and 0xFF
        val addrLow = address and 0xFF
        val addrHigh = (address ushr 8) and 0xFF

        val bytes = intArrayOf(addrLow, addrHigh, cmd, invCmd)
        for (b in bytes) {
            for (bit in 0 until 8) {
                pulses.add(562)
                val isOne = ((b ushr bit) and 0x01) == 1
                pulses.add(if (isOne) 1687 else 562)
            }
        }
        // Stop bit
        pulses.add(562)
        return pulses.toIntArray()
    }

    fun formatNecHex(commandByte: Int, address: Int = COOLMOON_IR_ADDRESS): String {
        val cmd = commandByte and 0xFF
        val invCmd = cmd.inv() and 0xFF
        return "NEC 0x%04X %02X%02X".format(address, cmd, invCmd)
    }

    fun toHexString(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it) }

    private fun appendChecksum(payloadWithoutChecksum: ByteArray): ByteArray {
        var sum = 0
        for (b in payloadWithoutChecksum) {
            sum += (b.toInt() and 0xFF)
        }
        return payloadWithoutChecksum + byteArrayOf((sum and 0xFF).toByte())
    }
}
