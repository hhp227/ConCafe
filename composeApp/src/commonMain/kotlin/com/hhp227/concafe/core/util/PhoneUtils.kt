package com.hhp227.concafe.core.util

fun formatKoreanPhoneNumber(input: String): String {
    val digits = input.filter { it.isDigit() }.take(11)
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
    }
}
