package com.baiviet.game.xidach.rules

import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank

/**
 * Đánh giá tay bài Xì Dách theo đúng luật Việt Nam.
 */
object XiDachEvaluator {

    /**
     * Đánh giá danh sách lá bài của một người chơi (hoặc nhà cái).
     *
     * @param cards danh sách lá bài (từ 2 đến 5 lá)
     * @param isDealer true nếu là nhà cái (để xét mốc non/đủ tuổi 15 hay 16)
     */
    fun evaluate(cards: List<Card>, isDealer: Boolean): XiDachHand {
        require(cards.size in 2..5) { "Số lá bài Xì Dách phải từ 2 đến 5, nhận được ${cards.size}" }

        val aceCount = cards.count { it.rank == Rank.ACE }
        val nonAceCards = cards.filter { it.rank != Rank.ACE }
        val nonAceSum = nonAceCards.sumOf { cardPoint(it.rank) }

        // 1. Kiểm tra 2 lá đầu
        if (cards.size == 2) {
            if (aceCount == 2) {
                return XiDachHand(cards = cards, score = 21, type = XiDachHandType.XI_BANG, isDealer = isDealer)
            }
            if (aceCount == 1 && isTenCard(nonAceCards[0].rank)) {
                return XiDachHand(cards = cards, score = 21, type = XiDachHandType.XI_DACH, isDealer = isDealer)
            }
            // 1 Ace + 1 lá thường (2..9): A có thể là 10 hoặc 11
            val score = if (aceCount == 1) {
                // 11 + V luôn <= 20 và > 10 + V
                11 + nonAceSum
            } else {
                nonAceSum
            }
            return XiDachHand(cards = cards, score = score, type = XiDachHandType.NORMAL, isDealer = isDealer)
        }

        // 2. Bài 3 lá: A có thể là 1 hoặc 10
        if (cards.size == 3) {
            val bestScore = calculateBestScore3Cards(nonAceSum, aceCount)
            return XiDachHand(cards = cards, score = bestScore, type = XiDachHandType.NORMAL, isDealer = isDealer)
        }

        // 3. Bài 4 hoặc 5 lá: A chỉ tính là 1
        val total = nonAceSum + aceCount
        if (cards.size == 5 && total <= 21) {
            return XiDachHand(cards = cards, score = total, type = XiDachHandType.NGU_LINH, isDealer = isDealer)
        }

        return XiDachHand(cards = cards, score = total, type = XiDachHandType.NORMAL, isDealer = isDealer)
    }

    /**
     * So sánh giữa Nhà Con và Nhà Cái.
     *
     * @return 1 nếu Con thắng, -1 nếu Cái thắng, 0 nếu Hòa
     */
    fun compare(player: XiDachHand, dealer: XiDachHand, bothBustPlayerLoses: Boolean = false): Int {
        // Cả hai cùng Xì bàng -> Hòa
        if (player.type == XiDachHandType.XI_BANG && dealer.type == XiDachHandType.XI_BANG) return 0
        if (player.type == XiDachHandType.XI_BANG) return 1
        if (dealer.type == XiDachHandType.XI_BANG) return -1

        // Xì dách
        if (player.type == XiDachHandType.XI_DACH && dealer.type == XiDachHandType.XI_DACH) return 0
        if (player.type == XiDachHandType.XI_DACH) return 1
        if (dealer.type == XiDachHandType.XI_DACH) return -1

        // Ngũ linh
        if (player.type == XiDachHandType.NGU_LINH && dealer.type == XiDachHandType.NGU_LINH) {
            // Ngũ linh gặp ngũ linh: ít điểm hơn thắng!
            return when {
                player.score < dealer.score -> 1
                player.score > dealer.score -> -1
                else -> 0
            }
        }
        if (player.type == XiDachHandType.NGU_LINH) return 1
        if (dealer.type == XiDachHandType.NGU_LINH) return -1

        // Bài thông thường / Quắc
        val playerBust = player.isBust
        val dealerBust = dealer.isBust

        if (playerBust && dealerBust) {
            return if (bothBustPlayerLoses) -1 else 0
        }
        if (playerBust) return -1
        if (dealerBust) return 1

        // Cả hai cùng không quắc -> so điểm
        return when {
            player.score > dealer.score -> 1
            player.score < dealer.score -> -1
            else -> 0
        }
    }

    /**
     * Hệ số nhân tiền thưởng dựa trên loại bài thắng.
     */
    fun payoutMultiplier(winningHand: XiDachHand, rules: XiDachRules): Int = when (winningHand.type) {
        XiDachHandType.XI_BANG -> rules.xiBangMultiplier
        XiDachHandType.XI_DACH -> rules.xiDachMultiplier
        XiDachHandType.NGU_LINH -> rules.nguLinhMultiplier
        XiDachHandType.NORMAL -> 1
    }

    private fun cardPoint(rank: Rank): Int = when (rank) {
        Rank.TWO -> 2
        Rank.THREE -> 3
        Rank.FOUR -> 4
        Rank.FIVE -> 5
        Rank.SIX -> 6
        Rank.SEVEN -> 7
        Rank.EIGHT -> 8
        Rank.NINE -> 9
        Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING -> 10
        Rank.ACE -> 1 // mặc định
    }

    private fun isTenCard(rank: Rank): Boolean =
        rank == Rank.TEN || rank == Rank.JACK || rank == Rank.QUEEN || rank == Rank.KING

    private fun calculateBestScore3Cards(nonAceSum: Int, aceCount: Int): Int {
        if (aceCount == 0) return nonAceSum

        val candidateScores = mutableListOf<Int>()
        // Với mỗi A, có thể chọn giá trị 10 hoặc 1 (tối đa 3 lá)
        for (tens in 0..aceCount) {
            val ones = aceCount - tens
            val sum = nonAceSum + tens * 10 + ones * 1
            candidateScores.add(sum)
        }

        // Ưu tiên điểm cao nhất mà <= 21
        val validScores = candidateScores.filter { it <= 21 }
        return if (validScores.isNotEmpty()) {
            validScores.maxOrNull() ?: candidateScores.minOrNull() ?: nonAceSum
        } else {
            // Nếu tất cả đều quắc, lấy điểm nhỏ nhất để hạn chế quắc nặng
            candidateScores.minOrNull() ?: nonAceSum
        }
    }
}
