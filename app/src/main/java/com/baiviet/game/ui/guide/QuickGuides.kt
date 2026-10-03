package com.baiviet.game.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baiviet.core.cards.Card
import com.baiviet.core.cards.Rank
import com.baiviet.core.cards.Rank.ACE
import com.baiviet.core.cards.Rank.EIGHT
import com.baiviet.core.cards.Rank.FIVE
import com.baiviet.core.cards.Rank.FOUR
import com.baiviet.core.cards.Rank.JACK
import com.baiviet.core.cards.Rank.KING
import com.baiviet.core.cards.Rank.NINE
import com.baiviet.core.cards.Rank.QUEEN
import com.baiviet.core.cards.Rank.SEVEN
import com.baiviet.core.cards.Rank.SIX
import com.baiviet.core.cards.Rank.TEN
import com.baiviet.core.cards.Rank.THREE
import com.baiviet.core.cards.Rank.TWO
import com.baiviet.core.cards.Suit
import com.baiviet.core.cards.Suit.CLUB
import com.baiviet.core.cards.Suit.DIAMOND
import com.baiviet.core.cards.Suit.HEART
import com.baiviet.core.cards.Suit.SPADE
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.Scrim
import com.baiviet.core.ui.theme.BvColors
import kotlinx.coroutines.launch

/** Một thẻ hướng dẫn nhanh: 1 ý chính + lá bài minh họa. */
data class GuideCard(val title: String, val body: String, val cards: List<Card> = emptyList())

private fun c(r: Rank, s: Suit) = Card(r, s)

/** Hướng dẫn nhanh 4–6 thẻ cho từng game. */
object QuickGuides {
    fun forGame(gameId: String): List<GuideCard> = when (gameId) {
        "tienlen" -> listOf(
            GuideCard("Mục tiêu", "Đánh hết 13 lá trước mọi người. 3 nhỏ nhất, 2 (heo) lớn nhất; chất ♠ < ♣ < ♦ < ♥.", listOf(c(THREE, SPADE), c(TWO, HEART))),
            GuideCard("Các bộ bài", "Rác, đôi, sám (bộ ba), sảnh từ 3 lá (không chứa 2), tứ quý, đôi thông.", listOf(c(FIVE, CLUB), c(SIX, HEART), c(SEVEN, SPADE))),
            GuideCard("Chặn bài", "Đánh bộ cùng loại, cùng số lá và lớn hơn. Không chặn được thì Bỏ lượt.", listOf(c(NINE, SPADE), c(NINE, HEART))),
            GuideCard("Chặt heo", "3 đôi thông chặt 1 heo; tứ quý chặt heo và đôi heo; 4 đôi thông chặt cả tứ quý.", listOf(c(FIVE, SPADE), c(FIVE, HEART), c(SIX, CLUB), c(SIX, DIAMOND), c(SEVEN, SPADE), c(SEVEN, HEART))),
            GuideCard("Thao tác", "Chạm để chọn lá, kéo ngang để quét nhiều lá, vuốt lên để đánh. Nút Gợi ý giúp bạn chọn.", listOf(c(FOUR, SPADE), c(FIVE, CLUB), c(SIX, HEART))),
        )
        "samloc" -> listOf(
            GuideCard("Mục tiêu", "Mỗi người 10 lá, đánh hết bài trước. Không so chất: hai bộ bằng nhau không chặn nhau.", listOf(c(TWO, SPADE), c(TWO, HEART))),
            GuideCard("Sảnh Sâm", "Sảnh từ 3 lá, được A-2-3 và Q-K-A nhưng không vòng K-A-2. Không có đôi thông.", listOf(c(ACE, SPADE), c(TWO, CLUB), c(THREE, HEART))),
            GuideCard("Báo Sâm", "Sau khi chia có 8 giây để báo Sâm: đi một mạch hết bài thì ăn 20 lá mỗi nhà, bị chặn thì đền.", listOf(c(TEN, SPADE), c(JACK, SPADE), c(QUEEN, HEART), c(KING, CLUB))),
            GuideCard("Báo 1", "Ai còn 1 lá phải báo. Người đánh ngay trước họ phải đánh lá lớn nhất, nếu không sẽ đền làng.", listOf(c(ACE, HEART))),
            GuideCard("Về bằng heo", "Lá cuối là heo bị thối 2: không được tính thắng.", listOf(c(TWO, DIAMOND))),
        )
        "phom" -> listOf(
            GuideCard("Mục tiêu", "Mỗi người 9 lá (người đi đầu 10). Ghép phỏm (3 lá cùng số hoặc sảnh cùng chất) để ít điểm rác nhất.", listOf(c(SEVEN, HEART), c(SEVEN, SPADE), c(SEVEN, DIAMOND))),
            GuideCard("Ăn hoặc bốc", "Đến lượt: ăn lá người trước vừa đánh nếu tạo được phỏm, không thì bốc nọc, rồi đánh 1 lá.", listOf(c(FOUR, CLUB), c(FIVE, CLUB), c(SIX, CLUB))),
            GuideCard("Hạ và gửi", "Vòng cuối hạ phỏm, sau đó gửi lá rác vào phỏm người khác để giảm điểm.", listOf(c(NINE, HEART), c(TEN, HEART), c(JACK, HEART))),
            GuideCard("Ù", "Toàn bộ bài thành phỏm là Ù — ăn 5 cược mỗi nhà. Không hạ được phỏm nào là Móm.", listOf(c(ACE, SPADE), c(ACE, HEART), c(ACE, CLUB))),
            GuideCard("Điểm", "A = 1, J = 11, Q = 12, K = 13. Ít điểm rác nhất về Nhất.", listOf(c(ACE, DIAMOND), c(KING, SPADE))),
        )
        "maubinh" -> listOf(
            GuideCard("Mục tiêu", "13 lá xếp thành 3 chi: chi 1 (5 lá) ≥ chi 2 (5 lá) ≥ chi 3 (3 lá).", listOf(c(KING, SPADE), c(KING, HEART), c(KING, CLUB))),
            GuideCard("So chi", "So từng chi với từng người, thắng chi nào ăn chi đó. Không so chất.", listOf(c(TEN, HEART), c(JACK, HEART), c(QUEEN, HEART), c(KING, HEART), c(ACE, HEART))),
            GuideCard("Binh lủng", "Chi dưới nhỏ hơn chi trên là lủng — thua 6 chi mỗi người. Nút Xếp tự động giúp tránh lủng.", listOf(c(TWO, SPADE), c(THREE, CLUB))),
            GuideCard("Sập 3 chi", "Thắng cả 3 chi một người thì nhân đôi tiền với người đó.", listOf(c(ACE, SPADE), c(ACE, HEART))),
            GuideCard("Mậu binh", "Rồng cuốn, sảnh rồng, 6 đôi… là tới trắng — thắng ngay không cần so.", listOf(c(TWO, HEART), c(THREE, HEART), c(FOUR, HEART), c(FIVE, HEART))),
        )
        "xidach" -> listOf(
            GuideCard("Mục tiêu", "Rút bài gần 21 nhất mà không quá. Con đủ 16 mới được dằn, cái đủ 15 mới được xét.", listOf(c(KING, SPADE), c(SIX, HEART))),
            GuideCard("Giá trị lá", "J, Q, K = 10. A = 11 hoặc 10 khi có 2 lá; = 10 hoặc 1 khi có 3 lá; = 1 khi có 4–5 lá.", listOf(c(ACE, HEART), c(JACK, CLUB))),
            GuideCard("Xì bàng, Xì dách", "Hai lá A là Xì bàng (lớn nhất), A + 10/J/Q/K là Xì dách — thắng ngay.", listOf(c(ACE, SPADE), c(ACE, DIAMOND))),
            GuideCard("Ngũ linh", "5 lá mà tổng ≤ 21 là Ngũ linh, thắng gấp đôi.", listOf(c(TWO, SPADE), c(THREE, HEART), c(FOUR, CLUB), c(FIVE, DIAMOND), c(SIX, SPADE))),
            GuideCard("Nặn bài", "Bấm Nặn bài rồi kéo góc để hé lá cuối từng chút — hồi hộp như bàn thật.", listOf(c(NINE, HEART))),
        )
        "poker" -> listOf(
            GuideCard("Mục tiêu", "Ghép 5 lá tốt nhất từ 2 lá tẩy và 5 lá chung.", listOf(c(ACE, HEART), c(ACE, CLUB))),
            GuideCard("Blind", "Hai người sau nút đặt mù nhỏ và mù lớn trước khi chia bài.", listOf(c(KING, SPADE), c(QUEEN, SPADE))),
            GuideCard("4 vòng cược", "Pre-flop, Flop (3 lá), Turn (1 lá), River (1 lá). Mỗi vòng: Xem, Theo, Cược/Tố hoặc Úp.", listOf(c(TEN, HEART), c(JACK, HEART), c(QUEEN, HEART))),
            GuideCard("Thứ hạng", "Thùng phá sảnh > Tứ quý > Cù lũ > Thùng > Sảnh > Sám > Thú > Đôi > Mậu thầu.", listOf(c(TEN, SPADE), c(JACK, SPADE), c(QUEEN, SPADE), c(KING, SPADE), c(ACE, SPADE))),
            GuideCard("All-in & side pot", "Tất tay khi không đủ chip theo; phần cược vượt quá tạo thành pot phụ.", listOf(c(EIGHT, DIAMOND), c(EIGHT, CLUB))),
        )
        "lieng" -> listOf(
            GuideCard("Mục tiêu", "Mỗi người 3 lá, tố – theo – úp để giành pot.", listOf(c(QUEEN, DIAMOND), c(KING, DIAMOND), c(ACE, DIAMOND))),
            GuideCard("Thứ hạng", "Sáp (3 lá cùng số) > Liêng (3 lá liên tiếp) > Ảnh (3 lá J/Q/K) > Điểm.", listOf(c(SEVEN, SPADE), c(SEVEN, HEART), c(SEVEN, CLUB))),
            GuideCard("Liêng", "A-2-3 và Q-K-A đều là liêng; J-Q-K là liêng chứ không phải ảnh.", listOf(c(JACK, CLUB), c(QUEEN, HEART), c(KING, SPADE))),
            GuideCard("Điểm", "Tổng nút lấy hàng đơn vị (A = 1, 10/J/Q/K = 0 khi tính điểm). Bằng nhau so chất Rô > Cơ > Chuồn > Bích.", listOf(c(FOUR, HEART), c(FIVE, CLUB))),
            GuideCard("Hành động", "Theo bằng mức tố, Tố để nâng cược, Úp để bỏ, Tất tay khi muốn dồn hết.", listOf(c(NINE, DIAMOND))),
        )
        "bacay" -> listOf(
            GuideCard("Mục tiêu", "Mỗi người 3 lá, tổng nút lấy hàng đơn vị — 9 nút là cao nhất.", listOf(c(FOUR, HEART), c(TWO, CLUB), c(THREE, DIAMOND))),
            GuideCard("Ba tiên", "3 lá J/Q/K là Ba tiên (Ba cào) — lớn hơn 9 nút.", listOf(c(JACK, SPADE), c(QUEEN, HEART), c(KING, CLUB))),
            GuideCard("So chất", "Bằng nút so lá lớn nhất theo chất Rô > Cơ > Chuồn > Bích.", listOf(c(ACE, DIAMOND), c(ACE, HEART))),
            GuideCard("Nặn bài", "Lá thứ ba úp — kéo góc để nặn từ từ, rồi lật bài.", listOf(c(NINE, CLUB))),
        )
        else -> emptyList()
    }
}

/** Thẻ trượt hướng dẫn nhanh; [onDone] khi bỏ qua hoặc xem xong. */
@Composable
fun QuickGuideDialog(title: String, pages: List<GuideCard>, onDone: () -> Unit) {
    if (pages.isEmpty()) {
        onDone()
        return
    }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Scrim {
        GlassPanel(strong = true, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.86f)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = BvColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
                HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp)) { page ->
                    val p = pages[page]
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(p.title, color = BvColors.Gold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        GuideCardRow(p.cards, 40.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(p.body, color = BvColors.Ivory, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    repeat(pages.size) { i ->
                        Box(
                            Modifier
                                .size(if (i == pager.currentPage) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (i == pager.currentPage) BvColors.Gold else BvColors.TextMuted),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("Bỏ qua", onDone, style = ActionStyle.SECONDARY)
                    val last = pager.currentPage == pages.lastIndex
                    ActionButton(
                        if (last) "Bắt đầu chơi" else "Tiếp",
                        onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        pulse = last,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideCardRow(cards: List<Card>, width: Dp) {
    if (cards.isEmpty()) return
    val step = width * 0.6f
    Box(Modifier.width(width + step * (cards.size - 1)).height(width * 1.45f)) {
        cards.forEachIndexed { i, card ->
            PlayingCard(card = card, width = width, modifier = Modifier.offset(x = step * i), elevation = 2.dp)
        }
    }
}
