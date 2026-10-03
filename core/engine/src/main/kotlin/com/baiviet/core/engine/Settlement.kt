package com.baiviet.core.engine

import kotlinx.serialization.Serializable

/**
 * Kết quả thanh toán một ván — tổng deltas LUÔN = 0 (zero-sum).
 *
 * @param deltas tiền +/- cho từng người chơi
 * @param lines chi tiết từng khoản thanh toán (hiển thị trên bảng kết quả)
 */
@Serializable
data class Settlement(
    val deltas: Map<PlayerId, Long>,
    val lines: List<SettlementLine>,
) {
    init {
        require(deltas.values.sum() == 0L) {
            "Tổng tiền phải = 0 (zero-sum), hiện tại = ${deltas.values.sum()}"
        }
    }

    /**
     * Chặn mức thua theo số dư: không ai mất quá số xu đang có.
     *
     * Duyệt các dòng theo thứ tự phát sinh; mỗi dòng chỉ trả tối đa phần còn lại
     * của người trả (tiền thắng trong ván không được dùng để trả tiếp).
     * Dòng bị cắt giảm vẫn giữ lý do, số tiền là số thực trả. Kết quả vẫn zero-sum.
     */
    fun cappedBy(balances: Map<PlayerId, Long>): Settlement {
        val remaining = balances.toMutableMap()
        val newLines = lines.mapNotNull { line ->
            val from = line.from ?: return@mapNotNull line
            val left = remaining[from] ?: Long.MAX_VALUE
            val paid = minOf(line.amount, left.coerceAtLeast(0L))
            remaining[from] = left - paid
            if (paid <= 0L) null else line.copy(amount = paid)
        }
        return fromLines(deltas.keys, newLines)
    }

    companion object {
        /** Dựng Settlement từ các dòng có đủ người trả và người nhận. */
        fun fromLines(players: Collection<PlayerId>, lines: List<SettlementLine>): Settlement {
            val deltas = players.associateWith { 0L }.toMutableMap()
            for (line in lines) {
                line.from?.let { deltas[it] = (deltas[it] ?: 0L) - line.amount }
                line.to?.let { deltas[it] = (deltas[it] ?: 0L) + line.amount }
            }
            return Settlement(deltas, lines)
        }
    }
}

/**
 * Một dòng thanh toán chi tiết.
 *
 * @param from người trả (null = hệ thống/pot)
 * @param to người nhận (null = hệ thống/pot)
 * @param amount số tiền (> 0)
 * @param reason lý do tiếng Việt (VD "Thối 1 heo đỏ: −6 lá")
 */
@Serializable
data class SettlementLine(
    val from: PlayerId?,
    val to: PlayerId?,
    val amount: Long,
    val reason: String,
)
