package com.baiviet.game.phom.rules

import com.baiviet.core.cards.Card

/**
 * Kết quả phân chia bài thành các phỏm và các lá rác.
 *
 * @param melds các phỏm rời nhau (mỗi lá xuất hiện tối đa 1 lần)
 * @param trash các lá rác còn lại
 * @param trashPoints tổng điểm các lá rác (A=1, 2..10 theo số, J=11, Q=12, K=13)
 */
data class PhomPartition(
    val melds: List<PhomMeld>,
    val trash: List<Card>,
    val trashPoints: Int,
)

/**
 * Thuật toán tìm phỏm tối ưu và phân tích bài trong Phỏm (Tá Lả).
 */
object PhomMeldFinder {

    /**
     * Tìm tất cả các phỏm thô (ngang hoặc dọc) có thể tạo từ các lá trong [cards].
     */
    fun findAllRawMelds(cards: List<Card>): List<PhomMeld> {
        val result = mutableListOf<PhomMeld>()
        val distinct = cards.distinct()

        // 1. Phỏm ngang (3 hoặc 4 lá cùng rank)
        val byRank = distinct.groupBy { it.rank }
        for ((_, rankCards) in byRank) {
            if (rankCards.size >= 3) {
                // Các tổ hợp 3 lá
                for (i in 0 until rankCards.size) {
                    for (j in i + 1 until rankCards.size) {
                        for (k in j + 1 until rankCards.size) {
                            PhomMeld.classify(listOf(rankCards[i], rankCards[j], rankCards[k]))?.let { result.add(it) }
                        }
                    }
                }
                // Tổ hợp 4 lá
                if (rankCards.size == 4) {
                    PhomMeld.classify(rankCards)?.let { result.add(it) }
                }
            }
        }

        // 2. Phỏm dọc (≥ 3 lá liên tiếp cùng chất)
        val bySuit = distinct.groupBy { it.suit }
        for ((_, suitCards) in bySuit) {
            val sorted = suitCards.sortedBy { it.phomIndex }
            for (i in 0 until sorted.size) {
                for (j in i + 2 until sorted.size) {
                    val sub = sorted.subList(i, j + 1)
                    PhomMeld.classify(sub)?.let { result.add(it) }
                }
            }
        }

        return result
    }

    /**
     * Tìm tất cả các cạ (2 lá có thể ghép thành phỏm) trong [cards].
     */
    fun findAllCa(cards: List<Card>): List<PhomCa> {
        val caList = mutableListOf<PhomCa>()
        val sorted = cards.distinct().sortedPhom()

        for (i in 0 until sorted.size) {
            for (j in i + 1 until sorted.size) {
                val c1 = sorted[i]
                val c2 = sorted[j]

                // Cạ ngang (cùng rank, khác chất)
                if (c1.rank == c2.rank) {
                    caList.add(PhomCa(PhomCaType.SAME_RANK, c1 to c2, emptyList()))
                }

                // Cạ dọc (cùng chất)
                if (c1.suit == c2.suit) {
                    val diff = Math.abs(c1.phomIndex - c2.phomIndex)
                    if (diff == 1) {
                        // Liên tiếp
                        val needed = mutableListOf<com.baiviet.core.cards.Rank>()
                        val minIdx = minOf(c1.phomIndex, c2.phomIndex)
                        val maxIdx = maxOf(c1.phomIndex, c2.phomIndex)
                        if (minIdx > 0) needed.add(indexToRank(minIdx - 1))
                        if (maxIdx < 12) needed.add(indexToRank(maxIdx + 1))
                        caList.add(PhomCa(PhomCaType.CONSECUTIVE, c1 to c2, needed))
                    } else if (diff == 2) {
                        // Cách 1 lá ở giữa
                        val midIdx = (c1.phomIndex + c2.phomIndex) / 2
                        caList.add(PhomCa(PhomCaType.GAP_ONE, c1 to c2, listOf(indexToRank(midIdx))))
                    }
                }
            }
        }
        return caList
    }

    private fun indexToRank(idx: Int): com.baiviet.core.cards.Rank = when (idx) {
        0 -> com.baiviet.core.cards.Rank.ACE
        1 -> com.baiviet.core.cards.Rank.TWO
        2 -> com.baiviet.core.cards.Rank.THREE
        3 -> com.baiviet.core.cards.Rank.FOUR
        4 -> com.baiviet.core.cards.Rank.FIVE
        5 -> com.baiviet.core.cards.Rank.SIX
        6 -> com.baiviet.core.cards.Rank.SEVEN
        7 -> com.baiviet.core.cards.Rank.EIGHT
        8 -> com.baiviet.core.cards.Rank.NINE
        9 -> com.baiviet.core.cards.Rank.TEN
        10 -> com.baiviet.core.cards.Rank.JACK
        11 -> com.baiviet.core.cards.Rank.QUEEN
        12 -> com.baiviet.core.cards.Rank.KING
        else -> error("Invalid rank index: $idx")
    }

    /**
     * Kiểm tra Ù khan: bài trên tay hoàn toàn không có phỏm nào và không có cạ nào.
     */
    fun isUKhan(hand: List<Card>): Boolean {
        if (findAllRawMelds(hand).isNotEmpty()) return false
        if (findAllCa(hand).isNotEmpty()) return false
        return true
    }

    /**
     * Tìm cách hạ phỏm tối ưu nhất từ [hand]:
     * - Mọi lá trong [eatenCards] bắt buộc phải nằm trong đúng 1 phỏm riêng biệt.
     * - Tối thiểu hóa tổng điểm các lá rác còn lại.
     */
    fun findBestPartition(
        hand: List<Card>,
        eatenCards: Set<Card>,
    ): PhomPartition {
        val allMelds = findAllRawMelds(hand)
        val validCombinations = mutableListOf<List<PhomMeld>>()

        // Backtrack tìm các tập phỏm rời nhau thỏa mãn eatenCards
        fun search(
            startIndex: Int,
            currentMelds: List<PhomMeld>,
            usedCards: Set<Card>,
            coveredEaten: Set<Card>,
        ) {
            // Kiểm tra xem hiện tại đã bao phủ đủ eatenCards chưa
            if (coveredEaten.size == eatenCards.size) {
                validCombinations.add(currentMelds)
            }

            for (i in startIndex until allMelds.size) {
                val candidate = allMelds[i]
                // Không được trùng lá bài đã dùng
                if (candidate.cards.any { it in usedCards }) continue

                // Không được chứa > 1 lá ăn trong 1 phỏm
                val eatenInCandidate = candidate.cards.filter { it in eatenCards }
                if (eatenInCandidate.size > 1) continue

                // Nếu phỏm này chứa lá ăn mà lá đó đã được phủ rồi thì bỏ qua
                if (eatenInCandidate.isNotEmpty() && eatenInCandidate.first() in coveredEaten) continue

                val newCovered = if (eatenInCandidate.isNotEmpty()) coveredEaten + eatenInCandidate.first() else coveredEaten
                val meldWithEaten = if (eatenInCandidate.isNotEmpty()) candidate.copy(eatenCard = eatenInCandidate.first()) else candidate

                search(
                    i + 1,
                    currentMelds + meldWithEaten,
                    usedCards + candidate.cards,
                    newCovered,
                )
            }
        }

        search(0, emptyList(), emptySet(), emptySet())

        // Trong các cách xếp hợp lệ, chọn cách có điểm rác thấp nhất
        // Nếu không có cách nào (hoặc không có phỏm), trả về toàn bộ bài là rác
        if (validCombinations.isEmpty()) {
            return PhomPartition(
                melds = emptyList(),
                trash = hand.sortedPhom(),
                trashPoints = hand.sumOf { it.phomPoint },
            )
        }

        var bestPartition = PhomPartition(
            melds = emptyList(),
            trash = hand.sortedPhom(),
            trashPoints = Int.MAX_VALUE,
        )

        for (melds in validCombinations) {
            val meldedCards = melds.flatMap { it.cards }.toSet()
            val trash = (hand - meldedCards).sortedPhom()
            val points = trash.sumOf { it.phomPoint }

            // Ưu tiên điểm rác thấp hơn; nếu bằng điểm thì ưu tiên số lá trong phỏm nhiều hơn
            if (points < bestPartition.trashPoints ||
                (points == bestPartition.trashPoints && meldedCards.size > bestPartition.melds.sumOf { it.size })
            ) {
                bestPartition = PhomPartition(melds, trash, points)
            }
        }

        return bestPartition
    }

    /**
     * Kiểm tra xem [hand] có thể Ù hay không (tất cả bài đều nằm trong phỏm, rác = 0 lá khi có 9 lá, hoặc ≤ 1 lá khi có 10 lá).
     */
    fun checkU(hand: List<Card>, eatenCards: Set<Card>): Boolean {
        val partition = findBestPartition(hand, eatenCards)
        return when (hand.size) {
            9 -> partition.trash.isEmpty() && partition.melds.isNotEmpty()
            10 -> partition.trash.size <= 1 && partition.melds.isNotEmpty()
            else -> false
        }
    }
}
