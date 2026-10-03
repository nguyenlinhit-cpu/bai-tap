package com.baiviet.core.data

import android.content.Context
import com.baiviet.core.engine.PlayerId
import com.baiviet.core.engine.Settlement
import kotlin.random.Random

/**
 * Phiên bàn chơi dùng chung cho các game: số dư thật của bạn, xu của bot, tên bot,
 * thay bot hết xu, chặn thua theo số dư và ghi ví + thống kê sau mỗi ván.
 *
 * Ghế 0 luôn là người chơi.
 */
class TableSession(
    private val context: Context,
    private val wallet: WalletRepository,
    val gameId: String,
    val playerCount: Int,
    val betUnit: Long,
    /** Số xu tối thiểu = minMultiplier × B. */
    private val minMultiplier: Long,
    private val random: Random = Random.Default,
) {
    var myBalance: Long = 0L
        private set
    private val botNames = mutableListOf<String>()
    private val botBalances = mutableListOf<Long>()

    val minBalance: Long get() = betUnit * minMultiplier

    suspend fun init(saved: SavedTable? = null) {
        wallet.ensureProfile()
        myBalance = wallet.current()
        if (saved != null && saved.botNames.size == playerCount - 1 && saved.botBalances.size == playerCount - 1) {
            botNames += saved.botNames
            botBalances += saved.botBalances
            return
        }
        val names = context.resources.getStringArray(R.array.bot_names).toMutableList().also { it.shuffle(random) }
        repeat(playerCount - 1) {
            botNames += names[it]
            botBalances += freshBotBalance()
        }
    }

    private fun freshBotBalance(): Long = minBalance * random.nextLong(3, 12)

    fun name(seat: Int): String = if (seat == 0) context.getString(R.string.you) else botNames[seat - 1]

    fun names(): Map<PlayerId, String> = (0 until playerCount).associate { PlayerId(it) to name(it) }

    fun balance(seat: Int): Long = if (seat == 0) myBalance else botBalances[seat - 1]

    fun balances(): Map<PlayerId, Long> = (0 until playerCount).associate { PlayerId(it) to balance(it) }

    /** Ảnh chụp bàn để lưu ván dở (xem [SavedGameRepository]). */
    fun snapshot(difficulty: Int, option: Int, round: Int, stateJson: String?, extra: String? = null) = SavedTable(
        gameId = gameId,
        players = playerCount,
        difficulty = difficulty,
        bet = betUnit,
        option = option,
        botNames = botNames.toList(),
        botBalances = botBalances.toList(),
        round = round,
        stateJson = stateJson,
        extra = extra,
    )

    /** Người chơi còn đủ xu chơi tiếp? */
    val canContinue: Boolean get() = myBalance >= minBalance

    /**
     * Chốt ván: chặn theo số dư, ghi ví (+ thống kê), cập nhật xu bot, thay bot hết xu.
     *
     * @return settlement đã chặn và danh sách thông báo (bot được thay…)
     */
    suspend fun settle(raw: Settlement): Pair<Settlement, List<String>> {
        val settlement = raw.cappedBy(balances())
        myBalance = wallet.applyGameResult(gameId, settlement.deltas[PlayerId(0)] ?: 0L)
        val notes = mutableListOf<String>()
        for (i in botBalances.indices) {
            botBalances[i] = (botBalances[i] + (settlement.deltas[PlayerId(i + 1)] ?: 0L)).coerceAtLeast(0L)
            if (botBalances[i] < minBalance) {
                val old = botNames[i]
                val pool = context.resources.getStringArray(R.array.bot_names).filter { it !in botNames }
                botNames[i] = pool.randomOrNull(random) ?: old
                botBalances[i] = freshBotBalance()
                notes += "$old hết xu, ${botNames[i]} vào thay."
            }
        }
        return settlement to notes
    }

    /** Thoát giữa ván: trừ khoản phạt (đã chặn theo số dư). */
    suspend fun forfeit(raw: Settlement) {
        val settlement = raw.cappedBy(balances())
        myBalance = wallet.applyGameResult(gameId, settlement.deltas[PlayerId(0)] ?: 0L)
    }
}
