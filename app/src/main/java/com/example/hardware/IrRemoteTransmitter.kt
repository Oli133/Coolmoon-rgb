package com.example.hardware

import android.content.Context
import android.hardware.ConsumerIrManager
import com.example.model.IrRemoteKey

data class IrTransmitResult(
    val transmitted: Boolean,
    val necSummary: String,
    val message: String
)

class IrRemoteTransmitter(context: Context) {

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasIrHardware: Boolean
        get() = runCatching { irManager?.hasIrEmitter() == true }.getOrDefault(false)

    fun transmitKey(key: IrRemoteKey): IrTransmitResult {
        val necHex = CoolMoonProtocolEncoder.formatNecHex(key.necCommandByte)
        val manager = irManager
        if (manager == null || !hasIrHardware) {
            return IrTransmitResult(
                transmitted = false,
                necSummary = necHex,
                message = "No physical IR emitter on this device ($necHex encoded)"
            )
        }
        return try {
            val pattern = CoolMoonProtocolEncoder.encodeNecIrPattern(key.necCommandByte)
            manager.transmit(38000, pattern)
            IrTransmitResult(
                transmitted = true,
                necSummary = necHex,
                message = "Transmitted ${key.label} via 38kHz IR ($necHex)"
            )
        } catch (e: Exception) {
            IrTransmitResult(
                transmitted = false,
                necSummary = necHex,
                message = "IR transmit failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
