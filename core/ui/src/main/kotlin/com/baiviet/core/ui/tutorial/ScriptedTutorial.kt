package com.baiviet.core.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baiviet.core.cards.Card
import com.baiviet.core.ui.audio.GameAudio
import com.baiviet.core.ui.audio.Sfx
import com.baiviet.core.ui.card.CardSqueezeDialog
import com.baiviet.core.ui.card.PlayingCard
import com.baiviet.core.ui.table.ActionButton
import com.baiviet.core.ui.table.ActionStyle
import com.baiviet.core.ui.table.FeltColor
import com.baiviet.core.ui.table.GlassPanel
import com.baiviet.core.ui.table.LockLandscape
import com.baiviet.core.ui.table.RoundIconButton
import com.baiviet.core.ui.table.TableFelt
import com.baiviet.core.ui.theme.BvColors

/**
 * Một hàng bài trên bàn tập: một ghế (đối thủ, nhà cái, bạn) hoặc một khu vực (bài chung, chi 1…).
 *
 * @param cards lá bài; `null` = lá úp
 * @param note nhãn phụ (điểm, trạng thái, số chip…)
 */
@Immutable
data class TutorialRow(
    val label: String,
    val cards: List<Card?>,
    val note: String? = null,
    val isYou: Boolean = false,
    val highlight: Boolean = false,
)

/**
 * Một bước ván tập. Người học chỉ đi tiếp được bằng đúng nút [correct];
 * bấm nút khác sẽ hiện [wrongHint] (chỉ cho phép thao tác đúng bước).
 *
 * @param actions các nút hành động hiển thị (như trên bàn thật)
 * @param correct chỉ số nút đúng; null = chỉ có nút "Tiếp tục"
 * @param squeeze nếu khác null, bấm đúng sẽ mở màn nặn lá này trước khi sang bước sau
 * @param banner dòng chữ lớn mừng sự kiện (Xì bàng, Sáp…) hiển thị ở bước này
 */
@Immutable
data class TutorialStep(
    val coach: String,
    val rows: List<TutorialRow>,
    val actions: List<String> = emptyList(),
    val correct: Int? = null,
    val wrongHint: String = "Chưa đúng bước — làm theo hướng dẫn nhé.",
    val squeeze: Card? = null,
    val banner: String? = null,
)

/**
 * Ván tập tương tác theo kịch bản: bàn bài sắp sẵn (seed cố định) bên trái,
 * bong bóng hướng dẫn (coach mark) và nút hành động bên phải; nút đúng nhấp nháy.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScriptedTutorial(
    title: String,
    steps: List<TutorialStep>,
    onBack: () -> Unit,
    felt: FeltColor = FeltColor.GREEN,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var wrong by rememberSaveable { mutableStateOf(false) }
    var squeezing by rememberSaveable { mutableStateOf(false) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    val last = index >= steps.lastIndex

    fun advance() {
        wrong = false
        if (last) onBack() else index++
    }

    LockLandscape()
    TableFelt(felt = felt) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton("←", "Quay lại", onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = BvColors.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Bước ${index + 1} / ${steps.size}", color = BvColors.TextSecondary, fontSize = 12.sp)
                }
                if (index > 0) {
                    ActionButton("Làm lại", {
                        index = 0
                        wrong = false
                    }, style = ActionStyle.SECONDARY)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Bàn bài
                AnimatedContent(
                    targetState = index,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1.4f).fillMaxHeight(),
                    label = "tutorialTable",
                ) { i ->
                    val s = steps[i.coerceIn(0, steps.lastIndex)]
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        s.banner?.let {
                            Text(
                                it,
                                color = BvColors.Gold,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                        s.rows.forEach { RowView(it) }
                    }
                }
                // Hướng dẫn + nút
                GlassPanel(Modifier.weight(1f).fillMaxHeight(), strong = true) {
                    Column(Modifier.fillMaxSize().padding(14.dp)) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Text("👉 Hướng dẫn", color = BvColors.Teal, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(step.coach, color = BvColors.Ivory, style = MaterialTheme.typography.bodyMedium)
                            if (wrong) {
                                Spacer(Modifier.height(8.dp))
                                Text(step.wrongHint, color = BvColors.Amber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (step.correct == null) {
                                ActionButton(if (last) "Hoàn thành" else "Tiếp tục", ::advance, pulse = true)
                            } else {
                                step.actions.forEachIndexed { i, label ->
                                    val ok = i == step.correct
                                    ActionButton(
                                        label,
                                        onClick = {
                                            if (ok) {
                                                if (step.squeeze != null) squeezing = true else advance()
                                            } else {
                                                wrong = true
                                                GameAudio.play(Sfx.LOSE)
                                            }
                                        },
                                        style = if (ok) ActionStyle.PRIMARY else ActionStyle.SECONDARY,
                                        pulse = ok,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        val sq = step.squeeze
        if (squeezing && sq != null) {
            CardSqueezeDialog(
                card = sq,
                onRevealed = {},
                onDismiss = {
                    squeezing = false
                    advance()
                },
            )
        }
    }
}

@Composable
private fun RowView(row: TutorialRow) {
    val border = when {
        row.highlight -> BvColors.Gold
        row.isYou -> BvColors.Teal
        else -> Color.White.copy(alpha = 0.15f)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .border(if (row.highlight) 2.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.label, color = if (row.isYou) BvColors.Teal else BvColors.Ivory, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            row.note?.let { Text(it, color = BvColors.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        if (row.cards.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.cards.forEach { card ->
                    Box { PlayingCard(card = card, width = if (row.isYou) 50.dp else 40.dp, faceUp = card != null) }
                }
            }
        }
    }
}
