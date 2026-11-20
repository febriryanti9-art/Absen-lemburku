package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeUtils {

    private const val PAYLOAD_PREFIX = "OVERTIME_COMPANY:"
    private val VALID_CODE_REGEX = Regex("^[A-Z0-9][A-Z0-9_\\-]+$")

    fun encodeCompanyQrPayload(companyCode: String): String {
        val normalized = normalizeCode(companyCode)
        return "$PAYLOAD_PREFIX$normalized"
    }

    fun parseCompanyCodeFromQr(rawText: String): String? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null

        val extracted = if (trimmed.startsWith(PAYLOAD_PREFIX, ignoreCase = true)) {
            trimmed.substring(PAYLOAD_PREFIX.length)
        } else {
            trimmed
        }

        val normalized = normalizeCode(extracted)
        return normalized.takeIf { it.length in 3..24 && VALID_CODE_REGEX.matches(it) }
    }

    private fun normalizeCode(raw: String): String {
        return raw.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9_\\-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(24)
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            val darkColor = Color.rgb(15, 23, 42) // Slate 900 for crisp scan contrast
            val lightColor = Color.WHITE

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) darkColor else lightColor
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }
}
