package com.hhp227.concafe.core.util

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

object RatingUtils {
    fun formatOneDecimal(rating: Double): String {
        val rounded = (rating * 10).roundToInt()
        return formatScaledOneDecimal(rounded)
    }

    fun formatOneDecimal(rating: Float): String {
        return formatOneDecimal(rating.toDouble())
    }

    fun formatOneDecimalTruncated(rating: Double): String {
        val truncated = (rating * 10).toInt()
        return formatScaledOneDecimal(truncated)
    }

    private fun formatScaledOneDecimal(scaled: Int): String {
        val integer = scaled / 10
        val decimal = scaled.absoluteValue % 10
        return "$integer.$decimal"
    }
}
