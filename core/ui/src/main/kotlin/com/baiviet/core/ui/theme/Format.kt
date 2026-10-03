package com.baiviet.core.ui.theme

/** Định dạng số xu kiểu Việt Nam: 1.250.000 */
fun formatCoins(value: Long): String {
    val negative = value < 0
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return if (negative) "−$grouped" else grouped
}

/** Rút gọn: 1,2K / 3,5M */
fun formatCoinsShort(value: Long): String {
    val abs = kotlin.math.abs(value)
    val sign = if (value < 0) "−" else ""
    return when {
        abs >= 1_000_000 -> sign + trim(abs / 1_000_000.0) + "M"
        abs >= 10_000 -> sign + trim(abs / 1_000.0) + "K"
        else -> sign + formatCoins(abs)
    }
}

private fun trim(v: Double): String {
    val s = String.format(java.util.Locale.US, "%.1f", v)
    return (if (s.endsWith(".0")) s.dropLast(2) else s).replace('.', ',')
}

/** Số có dấu: +1.200 / −600 */
fun formatSignedCoins(value: Long): String = if (value > 0) "+${formatCoins(value)}" else formatCoins(value)
