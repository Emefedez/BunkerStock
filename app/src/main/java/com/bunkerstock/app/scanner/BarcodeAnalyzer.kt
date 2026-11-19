package com.bunkerstock.app.scanner

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

/**
 * High-accuracy bundled offline ML Kit barcode/QR analyzer with 2-frame consensus debouncing:
 * - Zero Google Play Services required (model bundled in APK, works 100% offline)
 * - Handles sensor rotation, glare, low light, and angled QR/EAN/UPC/Code128/DataMatrix
 * - 2-frame identical-value consensus eliminates phantom/hallucinated reads
 * - Continuous live scanning: keeps camera open and emits when a new code is confirmed
 */
class BarcodeAnalyzer(
    private val onCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128
            )
            .build()
    )

    private var lastCandidate: String? = null
    private var candidateCount: Int = 0
    private var lastCandidateTimeMs: Long = 0L

    @Volatile
    private var lastEmittedCode: String? = null

    @Volatile
    private var lastEmitTimeMs: Long = 0L

    fun resetLastEmitted() {
        lastEmittedCode = null
        lastCandidate = null
        candidateCount = 0
    }

    fun close() {
        runCatching { scanner.close() }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstNotNullOfOrNull { it.rawValue?.trim() }
                    ?.takeIf(::isPlausibleCode)
                if (raw != null) {
                    val now = System.currentTimeMillis()
                    if (raw == lastCandidate && (now - lastCandidateTimeMs) <= 1500L) {
                        candidateCount++
                    } else {
                        lastCandidate = raw
                        candidateCount = 1
                    }
                    lastCandidateTimeMs = now

                    if (candidateCount >= 2) {
                        val isSameActive = raw == lastEmittedCode && (now - lastEmitTimeMs) < 3500L
                        if (!isSameActive) {
                            lastEmittedCode = raw
                            lastEmitTimeMs = now
                            onCodeDetected(raw)
                        }
                        candidateCount = 0
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    companion object {
        private fun isPlausibleCode(text: String): Boolean {
            if (text.length < 3 || text.length > 1024) return false
            return text.none { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }
        }

        fun decodeBitmap(bitmap: Bitmap, onResult: (String?) -> Unit) {
            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128
                    )
                    .build()
            )
            val input = InputImage.fromBitmap(bitmap, 0)
            scanner.process(input)
                .addOnSuccessListener { barcodes ->
                    val ml = barcodes.firstNotNullOfOrNull { it.rawValue?.trim() }
                        ?.takeIf(::isPlausibleCode)
                    scanner.close()
                    if (ml != null) {
                        onResult(ml)
                    } else {
                        onResult(decodeBitmapZxingFallback(bitmap))
                    }
                }
                .addOnFailureListener {
                    scanner.close()
                    onResult(decodeBitmapZxingFallback(bitmap))
                }
        }

        private fun decodeBitmapZxingFallback(bitmap: Bitmap): String? {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return null
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val source = RGBLuminanceSource(w, h, pixels)
            val reader = MultiFormatReader().apply {
                setHints(
                    mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(
                            BarcodeFormat.QR_CODE,
                            BarcodeFormat.DATA_MATRIX,
                            BarcodeFormat.EAN_13,
                            BarcodeFormat.EAN_8,
                            BarcodeFormat.UPC_A,
                            BarcodeFormat.UPC_E,
                            BarcodeFormat.CODE_128
                        ),
                        DecodeHintType.TRY_HARDER to true
                    )
                )
            }
            return runCatching {
                reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
            }.getOrElse {
                reader.reset()
                runCatching {
                    reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source))).text
                }.getOrNull()
            }?.trim()?.takeIf(::isPlausibleCode)
        }
    }
}
