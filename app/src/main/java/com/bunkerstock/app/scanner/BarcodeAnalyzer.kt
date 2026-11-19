package com.bunkerstock.app.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeAnalyzer(
    private val onCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val locked = AtomicBoolean(false)

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39
                ),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    fun unlock() {
        locked.set(false)
    }

    override fun analyze(image: ImageProxy) {
        if (locked.get()) {
            image.close()
            return
        }
        try {
            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val width = image.width
            val height = image.height
            val source = PlanarYUVLuminanceSource(
                bytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(bitmap)
            if (result != null && result.text.isNotBlank() && locked.compareAndSet(false, true)) {
                onCodeDetected(result.text.trim())
            }
        } catch (_: Exception) {
            // Frame without decodable barcode
        } finally {
            reader.reset()
            image.close()
        }
    }
}
