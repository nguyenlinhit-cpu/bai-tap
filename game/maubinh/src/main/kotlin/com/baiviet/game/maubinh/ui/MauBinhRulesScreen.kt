package com.baiviet.game.maubinh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.baiviet.core.data.HouseRulesRepository
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.theme.BvColors
import com.baiviet.game.maubinh.rules.MauBinhRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MauBinhRulesViewModel @Inject constructor(repo: HouseRulesRepository) : ViewModel() {
    val rules: StateFlow<MauBinhRules> = repo.rules(MauBinhRules.GAME_ID, MauBinhRules.serializer(), MauBinhRules.DEFAULT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MauBinhRules.DEFAULT)
}

private data class MauBinhSection(val title: String, val content: @Composable (MauBinhRules) -> Unit)

@Composable
fun MauBinhRulesScreen(
    onBack: () -> Unit,
    onOpenHouseRules: () -> Unit,
    viewModel: MauBinhRulesViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val sections = listOf(
        MauBinhSection("Tổng quan & 3 Chi") { r ->
            Text(
                "• Mậu Binh (Binh Xập Xám) dùng bộ bài 52 lá, mỗi người được chia 13 lá (2–4 người chơi).\n" +
                "• Người chơi có 60 giây để xếp bài thành 3 chi:\n" +
                "  - Chi 1 (chi đầu): 5 lá — bắt buộc phải mạnh nhất\n" +
                "  - Chi 2 (chi giữa): 5 lá — mạnh thứ nhì\n" +
                "  - Chi 3 (chi cuối): 3 lá — yếu nhất\n" +
                "• Quy tắc bất di bất dịch: Chi 1 ≥ Chi 2 ≥ Chi 3. Vi phạm quy tắc này sẽ bị tính là BINH LỦNG.",
                style = MaterialTheme.typography.bodyMedium,
                color = BvColors.TextSecondary,
            )
        },
        MauBinhSection("Thứ tự & Xếp hạng Chi") { r ->
            Text(
                "• Thứ bậc lá bài: 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < 10 < J < Q < K < A. TUYỆT ĐỐI KHÔNG SO CHẤT.\n" +
                "• Xếp hạng chi 5 lá (mạnh đến yếu):\n" +
                "  1. Thùng phá sảnh (5 lá liên tiếp cùng chất)\n" +
                "  2. Tứ quý (4 lá cùng rank)\n" +
                "  3. Cù lũ (1 bộ ba + 1 đôi)\n" +
                "  4. Thùng (5 lá cùng chất)\n" +
                "  5. Sảnh (5 lá có rank liên tiếp)\n" +
                "     - Sảnh lớn nhất: 10-J-Q-K-A\n" +
                "     - Sảnh A-2-3-4-5: ${if (r.aceTwoThreeFourFiveIsLowest) "Sảnh nhỏ nhất (Luật nhà)" else "Sảnh lớn thứ nhì (mặc định)"}\n" +
                "  6. Sám cô (1 bộ ba)\n" +
                "  7. Thú (2 đôi)\n" +
                "  8. Một đôi\n" +
                "  9. Mậu thầu (không tạo thành bộ nào)\n" +
                "• Chi 3 (3 lá): chỉ xét Sám cô > Đôi > Mậu thầu (không tính thùng/sảnh 3 lá).",
                style = MaterialTheme.typography.bodyMedium,
                color = BvColors.TextSecondary,
            )
        },
        MauBinhSection("Binh Lủng & Phạt") { r ->
            Text(
                "• Nếu xếp Chi 1 < Chi 2, hoặc Chi 2 < Chi 3, người chơi sẽ bị Binh Lủng.\n" +
                "• Người binh lủng bị xử thua cả 3 chi với tất cả người chơi không lủng, phạt ${r.foulPenaltyChi} chi/người (tương đương bị sập).\n" +
                "• Hai người cùng lủng thì hòa nhau (0 chi).",
                style = MaterialTheme.typography.bodyMedium,
                color = BvColors.TextSecondary,
            )
        },
        MauBinhSection("Chi Thưởng Đặc Biệt") { r ->
            Text(
                "Khi thắng một chi bằng các hàng đặc biệt sau, người chơi sẽ được cộng thêm chi thưởng thay vì 1 chi thông thường:\n" +
                "• Sám cô ở Chi 3: ${r.bonusChiSamChi3} chi\n" +
                "• Cù lũ ở Chi 2: ${r.bonusChiCuLuChi2} chi\n" +
                "• Tứ quý ở Chi 1: ${r.bonusChiTuQuyChi1} chi\n" +
                "• Tứ quý ở Chi 2: ${r.bonusChiTuQuyChi2} chi\n" +
                "• Thùng phá sảnh ở Chi 1: ${r.bonusChiThungPhaSanhChi1} chi\n" +
                "• Thùng phá sảnh ở Chi 2: ${r.bonusChiThungPhaSanhChi2} chi",
                style = MaterialTheme.typography.bodyMedium,
                color = BvColors.TextSecondary,
            )
        },
        MauBinhSection("Sập 3 Chi & Sập Làng") { r ->
            Text(
                "• Sập 3 chi: Người thắng cả 3 chi (chi 1, 2, 3) với một đối thủ thì tổng số chi ăn với người đó được NHÂN ĐÔI (×2).\n" +
                "• Sập làng: Trong bàn từ 3 người trở lên, nếu một người sập 3 chi với TẤT CẢ đối thủ, tổng số chi được NHÂN ĐÔI THÊM MỘT LẦN NỮA (×4).",
                style = MaterialTheme.typography.bodyMedium,
                color = BvColors.TextSecondary,
            )
        },
        MauBinhSection("Mậu Binh Tới Trắng") { r ->
            Text(
                "Khi nhận bài chia ban đầu đạt các điều kiện sau, người chơi thắng ngay lập tức không cần so chi:\n" +
                "1. Rồng cuốn: 13 lá từ 2 đến A cùng chất — Ăn ${r.instantWinDragonRollChi} chi/người\n" +
                "2. Sảnh rồng: 13 lá từ 2 đến A khác chất — Ăn ${r.instantWinDragonStraightChi} chi/người\n" +
                "3. Năm đôi một sám: 5 đôi + 1 bộ ba — Ăn ${r.instantWinFivePairsTripleChi} chi/người\n" +
                "4. Lục phé bôn: 6 đôi + 1 lá lẻ — Ăn ${r.instantWinSixPairsChi} chi/người\n" +
                "5. Ba thùng: Cả 3 chi đều là thùng — Ăn ${r.instantWinThreeFlushesChi} chi/người\n" +
                "6. Ba sảnh: Cả 3 chi đều là sảnh — Ăn ${r.instantWinThreeStraightsChi} chi/người\n" +
                "• Nếu nhiều người cùng tới trắng: loại bài cao hơn sẽ thắng; cùng loại thì hòa nhau.",
                style = MaterialTheme.typography.bodyMedium,
                color = BvColors.TextSecondary,
            )
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BvColors.NavyDark, Color(0xFF0F172A), BvColors.NavyDark),
                ),
            )
            .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(glyph = "←", contentDescription = "Quay lại", onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Luật chơi Mậu Binh",
                        style = MaterialTheme.typography.titleLarge,
                        color = BvColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row {
                    RoundIconButton(glyph = "⚙", contentDescription = "Luật nhà", onClick = onOpenHouseRules)
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(sections) { idx, s ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable {
                                scope.launch {
                                    listState.animateScrollToItem(idx)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = BvColors.Gold,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                itemsIndexed(sections) { _, s ->
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = BvColors.Gold,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        s.content(rules)
                    }
                }
            }
        }
    }
}
