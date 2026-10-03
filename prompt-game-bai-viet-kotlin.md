# PROMPT: Xây dựng ứng dụng "Bài Việt – Tổng Hợp Game Bài" bằng Kotlin

> Dán toàn bộ file này cho AI lập trình. Đây là bản đặc tả đầy đủ: sản phẩm, kỹ thuật, giao diện, luật chơi 8 game, bot, hướng dẫn, kiểm thử và lộ trình.

---

## 0. Vai trò và quy tắc làm việc

Bạn là **Senior Android Engineer kiêm Game Designer**, thành thạo Kotlin, Jetpack Compose, kiến trúc sạch và lập trình game bài. Nhiệm vụ: xây dựng **hoàn chỉnh** một ứng dụng Android chơi bài offline với máy, gồm 8 thể loại bài phổ biến ở Việt Nam, luật chơi chuẩn như ngoài đời và trên các cổng game, giao diện hiện đại, đẹp ngang các game bài di động hàng đầu hiện nay.

Quy tắc bắt buộc:

1. Viết code đầy đủ, build và chạy được. Không để `TODO`, không viết "phần này bạn tự làm", không stub giả.
2. Làm theo lộ trình ở **mục 10**. Hết mỗi giai đoạn: project build được, chơi được phần đã làm, test xanh.
3. Nếu câu trả lời bị giới hạn độ dài: dừng đúng ở ranh giới một file, ghi **"Còn lại: …"** (danh sách file), lượt sau viết tiếp — không viết lại file đã xong.
4. Mọi con số của luật (tiền phạt, hệ số thưởng, thời gian, điều kiện) đặt trong `RuleConfig` của từng game. Không hard-code rải rác.
5. Luật có nhiều biến thể vùng miền/cổng game: cài **bản phổ biến nhất** (mô tả trong tài liệu này) làm mặc định, đưa biến thể vào màn **"Luật nhà"** (bật/tắt, chỉnh số).
6. Màn **"Luật chơi"** đọc số liệu từ `RuleConfig` đang áp dụng → luật hiển thị luôn khớp luật đang chơi.
7. Toàn bộ chữ hiển thị bằng **tiếng Việt có dấu**, đặt trong `strings.xml` (dùng `plurals` khi cần).
8. Engine luật là **Kotlin thuần** (không phụ thuộc Android), có unit test cho từng luật.
9. Bot **không bao giờ** được nhìn bài úp của người khác (chỉ nhận `viewOf(player)`). Có test chứng minh.
10. Khi một điểm luật trong tài liệu chưa đủ rõ để code, chọn cách hiểu phổ biến nhất, ghi chú lại trong `docs/RULE_DECISIONS.md` và đưa thành tùy chọn Luật nhà nếu hợp lý.

---

## 1. Tổng quan sản phẩm

- Tên tạm: **"Bài Việt – Tổng Hợp Game Bài"**.
- Nền tảng: Android (điện thoại + máy tính bảng). Sảnh có thể dọc hoặc ngang; **bàn chơi luôn nằm ngang (landscape)**.
- Chơi offline với máy. Mọi thể loại đều có **3 chế độ**:
  - **2 người**: Bạn + 1 máy
  - **3 người**: Bạn + 2 máy
  - **4 người**: Bạn + 3 máy
- 3 độ khó bot: **Dễ / Thường / Khó**.
- Tiền ảo **"xu"** chỉ để giải trí: KHÔNG nạp tiền thật, KHÔNG đổi thưởng, KHÔNG mua bán/chuyển xu giữa người dùng. Có thông báo chơi giải trí lành mạnh và nhãn độ tuổi phù hợp chính sách cửa hàng ứng dụng về "simulated gambling".

| # | Game | Lá/người | Số người | Đặc trưng chính |
|---|---|---|---|---|
| 1 | Tiến lên miền Nam | 13 | 2–4 | Đánh hết bài trước, chặt heo, tới trắng |
| 2 | Sâm lốc | 10 | 2–4 | Gần giống Tiến lên, không so chất, báo Sâm, báo 1 |
| 3 | Phỏm (Tá lả) | 9 (người đi đầu 10) | 2–4 | Ăn/bốc, hạ phỏm, gửi bài, ít điểm nhất thắng |
| 4 | Mậu binh (Binh xập xám) | 13 | 2–4 | Xếp 3 chi, so chi, mậu binh tới trắng |
| 5 | Xì dách | 2 → tối đa 5 | 2–4 | Gần 21 nhất, có nhà cái, cái xét bài |
| 6 | Poker Texas Hold'em | 2 riêng + 5 chung | 2–4 | No-limit, 4 vòng cược, side pot |
| 7 | Liêng | 3 | 2–4 | Sáp > Liêng > Ảnh > Điểm, tố/theo/úp |
| 8 | Ba cây (Cào) | 3 | 2–4 | Điểm hàng đơn vị, Ba tiên, nặn bài |

---

## 2. Công nghệ và kiến trúc

### 2.1 Stack

- **Kotlin** bản ổn định mới nhất, JDK 17, Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`). Dùng phiên bản ổn định mới nhất của mọi thư viện.
- **UI**: Jetpack Compose + Material 3, Compose Animation, `Canvas` để vẽ lá bài vector.
- **Kiến trúc**: Clean Architecture + MVI (`UiState` / `Intent` / `Effect`), `ViewModel`, Coroutines + `StateFlow`.
- **DI**: Hilt.
- **Điều hướng**: Navigation Compose với route type-safe (kotlinx.serialization).
- **Lưu trữ**: DataStore (cài đặt, luật nhà), Room (thống kê, lịch sử ván, ván đang dở).
- **Hiệu ứng**: Lottie Compose cho banner thắng lớn; hệ particle tự viết bằng Canvas.
- **Âm thanh**: SoundPool (hiệu ứng), Media3/ExoPlayer (nhạc nền). Rung: `HapticFeedback`.
- **Test**: JUnit, kotlinx-coroutines-test, Turbine, Compose UI Test.
- `minSdk 26`, `targetSdk`/`compileSdk` mới nhất. Bật R8, baseline profile cho màn bàn chơi.

### 2.2 Cấu trúc module

```
:app                    // MainActivity, NavHost, theme, Hilt setup
:core:cards             // Card, Suit, Rank, Deck, xáo bài, tiện ích tổ hợp
:core:engine            // GameEngine, PlayerId, Settlement, GameEvent, RuleConfig, Rng
:core:ai                // khung Bot, BotLevel, think-time, Monte Carlo helper
:core:ui                // design system, CardView, CardFan, ChipStack, Avatar, TableScaffold, hiệu ứng
:core:data              // Room, DataStore, ví xu, thống kê, lưu ván
:game:tienlen
:game:samloc
:game:phom
:game:maubinh
:game:xidach
:game:poker
:game:lieng
:game:bacay
```

Mỗi `:game:*` gồm: `rules/` (luật thuần + RuleConfig), `engine/`, `bot/`, `ui/` (màn chơi, luật chơi, hướng dẫn, luật nhà), `src/test/`.

### 2.3 Hợp đồng lõi (bắt buộc tuân theo)

```kotlin
enum class Suit(val symbol: String, val viName: String) {
    SPADE("♠", "Bích"), CLUB("♣", "Chuồn"), DIAMOND("♦", "Rô"), HEART("♥", "Cơ")
}
enum class Rank(val label: String) { ACE("A"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5"),
    SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"), TEN("10"), JACK("J"), QUEEN("Q"), KING("K") }
data class Card(val rank: Rank, val suit: Suit)

// QUAN TRỌNG: thứ tự lớn nhỏ của lá và chất KHÁC NHAU giữa các game
// (Tiến lên: 2 lớn nhất; Phỏm: A nhỏ nhất; Poker: A cao/thấp; Liêng/Ba cây: Rô lớn nhất...).
// Mỗi game tự định nghĩa Comparator riêng. KHÔNG dùng Rank.ordinal / Suit.ordinal làm thứ tự chung.

@JvmInline value class PlayerId(val seat: Int)

interface RuleConfig   // mỗi game có data class @Serializable riêng, có giá trị mặc định

interface GameEngine<S : Any, A : Any, V : Any, R : RuleConfig> {
    fun start(table: TableConfig<R>, seed: Long): S
    fun currentActors(state: S): Set<PlayerId>          // Mậu binh: nhiều người xếp bài cùng lúc
    fun legalActions(state: S, player: PlayerId): List<A>
    fun apply(state: S, player: PlayerId, action: A): Transition<S>  // ném IllegalActionException nếu sai luật
    fun isFinished(state: S): Boolean
    fun settle(state: S): Settlement                     // tiền +/- từng người + danh sách lý do
    fun viewOf(state: S, player: PlayerId): V            // CHỈ thông tin người đó được phép thấy
}
data class Transition<S>(val state: S, val events: List<GameEvent>)

interface Bot<V, A> { suspend fun decide(view: V, legal: List<A>, level: BotLevel): A }

data class Settlement(val deltas: Map<PlayerId, Long>, val lines: List<SettlementLine>) // tổng deltas = 0
data class SettlementLine(val from: PlayerId?, val to: PlayerId?, val amount: Long, val reason: String)
```

Yêu cầu thêm:
- State **bất biến** (data class + persistent collections). Mọi thay đổi đi qua `apply` → dễ test, replay, lưu/khôi phục ván.
- RNG: seed từ `SecureRandom`, xáo **Fisher–Yates**; lưu seed + log hành động để tái hiện lỗi.
- Engine phát `GameEvent` (Dealt, Played, Passed, Cut, Drew, Ate, Melded, Revealed, Bet, PotWon, Announced, Settled…). UI tiêu thụ hàng đợi sự kiện và chạy animation tuần tự; state hiển thị chỉ cập nhật sau khi animation tương ứng xong.
- Game loop trong ViewModel: lấy người đến lượt → nếu là người thật thì chờ Intent (có đồng hồ) → nếu là bot thì `decide` trên `Dispatchers.Default` kèm thời gian "suy nghĩ" → `apply` → phát sự kiện. Hủy đúng cách khi thoát bàn.
- Tổng tiền mỗi ván luôn bằng 0 (zero-sum). Người chơi không thể mất quá số xu đang có (chặn ở mức số dư).

### 2.4 Ví dụ RuleConfig (phong cách bắt buộc)

```kotlin
@Serializable
data class TienLenRules(
    val scoring: TlScoring = TlScoring.COUNT_CARDS,     // COUNT_CARDS (Đếm lá) | RANKING (Xếp hạng)
    val require3SpadesOnFirstMove: Boolean = true,
    val fourPairsCutWithoutTurn: Boolean = true,
    val quadThreeInstantWinFirstGame: Boolean = true,
    val sameColorInstantWinCount: Int = 13,
    val fiveConsecutivePairsInstantWin: Boolean = true,
    val instantWinCountsPenalties: Boolean = false,
    val forbidFinishWithTwo: Boolean = false,
    val blackTwoCards: Int = 3, val redTwoCards: Int = 6,
    val threePairsCards: Int = 9, val quadCards: Int = 12, val fourPairsCards: Int = 18,
    val congCards: Int = 26, val instantWinCards: Int = 26,
    val turnSeconds: Int = 20,
) : RuleConfig
```

---

## 3. Thiết kế giao diện (hiện đại, sang, như game bài di động hàng đầu)

Toàn bộ hình ảnh, icon, logo **tự thiết kế**, không sao chép tài sản/thương hiệu của game khác.

### 3.1 Design system

- **Nền**: gradient navy đậm → tím than; bàn chơi là **nỉ (felt)** màu xanh lục ngọc (mặc định) hoặc đỏ rượu/xanh dương, có vignette, viền gỗ bóng hoặc viền vàng bo tròn, ánh spotlight nhẹ ở tâm bàn.
- **Màu nhấn**: vàng gold `#F5C451` (xu, nút chính), xanh ngọc `#2DD4BF` (hành động hợp lệ, lượt của bạn), đỏ `#EF4444` (cảnh báo, thua), trắng ngà cho chữ.
- **Glassmorphism** cho HUD, panel, bảng kết quả: nền mờ, viền sáng 1dp, bo góc 20dp, bóng mềm.
- **Font**: đóng gói font hỗ trợ tiếng Việt (ví dụ "Be Vietnam Pro" cho chữ thường, một font đậm có cá tính cho tiêu đề và số tiền). Số tiền dùng chữ số đều (tabular).
- **Lá bài**: vẽ vector bằng Canvas — góc có số + chất, giữa có bố cục chất chuẩn, J/Q/K là hình cách điệu tự vẽ; bo góc, đổ bóng mềm, sắc nét ở mọi mật độ điểm ảnh. Tùy chọn **bộ bài 4 màu** (♣ xanh lá, ♦ xanh dương) và **cỡ bài lớn**. Nhiều mẫu lưng bài và mặt bàn có thể đổi.
- Kích thước chạm tối thiểu 48dp; độ tương phản chữ đạt chuẩn.

### 3.2 Danh sách màn hình

1. **Splash** có logo động ngắn.
2. **Sảnh (Lobby)**: thanh trên gồm avatar, tên, cấp độ, số xu (đếm chạy khi thay đổi), nút Quà hằng ngày, Cài đặt. Lưới 8 thẻ game lớn có minh họa riêng, nhãn vùng ("Miền Nam", "Miền Bắc", "Quốc tế"), hiệu ứng nổi/nhún khi chạm.
3. **Chọn bàn** (mỗi game): chọn **2 / 3 / 4 người** (minh họa ghế), độ khó, mức cược B (ví dụ 100 / 500 / 1K / 5K / 10K xu — khóa mức không đủ xu), các nút **Luật nhà**, **Luật chơi**, **Hướng dẫn**, **Vào bàn**. Riêng Xì dách/Ba cây: chọn vai trò cái (Bạn làm cái / Máy làm cái / Luân phiên). Poker: chọn số xu mang vào bàn (buy-in).
4. **Bàn chơi** (xem 3.3).
5. **Bảng kết quả ván**: kính mờ, thứ hạng, tiền +/- (xanh/đỏ, số chạy), danh sách lý do chi tiết ("Thối 1 heo đỏ: −6 lá", "Chặt 3 đôi thông: +9 lá"…), nút **Ván mới**, **Đổi bàn**, **Xem lại ván** (log dạng chữ).
6. **Luật chơi / Hướng dẫn / Ván tập** cho từng game (mục 7).
7. **Hồ sơ & thống kê**: số ván, thắng, tỉ lệ thắng, ván thắng lớn nhất cho từng game; thành tích (achievement) đơn giản.
8. **Cài đặt**: nhạc nền, hiệu ứng âm thanh, rung, tốc độ bot (chậm/thường/nhanh), tốc độ chia bài, bộ bài 4 màu, cỡ bài lớn, mặt bàn, lưng bài, tự xếp bài khi chia.

### 3.3 Bố cục bàn chơi

```
4 người:          [Bot 3 – trên]
        [Bot 4 – trái]          [Bot 2 – phải]
                  [BẠN – dưới]
3 người: Bot ở trên-trái và trên-phải.   2 người: Bot ở trên.
Chiều đi mặc định: ngược chiều kim đồng hồ → Bạn → phải → trên → trái.
```

- Mỗi ghế: avatar tròn có **vòng đếm giờ** đổi màu xanh → vàng → đỏ, tên, số xu, số lá còn lại, nhãn trạng thái (Bỏ lượt, Báo 1, Báo Sâm, Dằn, Úp, Tất tay, Móm…). Người đến lượt có viền sáng nhấp nháy nhẹ.
- **Bài trên tay** xếp hình quạt ở dưới; chạm để nhấc lá (chọn), kéo ngang để quét chọn nhiều lá, vuốt lên để đánh. Lá không thể dùng hợp lệ có thể làm mờ khi bật "Gợi ý".
- **Nút hành động** lớn ở góc phải dưới, chỉ sáng khi hợp lệ: Đánh / Bỏ lượt / Xếp bài / Gợi ý (Tiến lên, Sâm); Bốc / Ăn / Hạ / Gửi (Phỏm); Rút / Dằn / Xét (Xì dách); Theo / Tố / Úp / Tất tay (Liêng, Poker có thêm Check)…
- **Tâm bàn**: bài vừa đánh, nọc (Phỏm), 5 lá chung (Poker), chồng chip pot, mức cược bàn.
- **Thanh trên**: nút thoát (hỏi xác nhận; thoát giữa ván = xử thua theo luật game), nút luật nhanh (bottom sheet), nút âm thanh.

### 3.4 Animation và cảm giác chơi

- **Chia bài**: lá bay từ tâm bàn tới từng ghế theo đường cong, so le 40–60ms/lá, có tiếng "xoẹt".
- **Lật bài**: xoay `rotationY` với `cameraDistance`, đổi mặt ở 90°.
- **Đánh bài**: lá bay từ tay/ghế vào giữa, xoay ngẫu nhiên ±8°, chồng lớp tự nhiên; vòng mới thì dọn bài với hiệu ứng trượt.
- **Sự kiện lớn** (Chặt heo, Tới trắng, Ù, Mậu binh, Ăn Sâm, Xì bàng, Thùng phá sảnh…): banner lớn + rung + particle + âm thanh riêng.
- **Chip bay** từ người thua sang người thắng, số xu đếm chạy.
- **Nặn bài** (Ba cây, Liêng, Xì dách): kéo góc lá để hé dần mặt bài — tính năng đặc trưng của game bài Việt.
- Bot "suy nghĩ" 0.6–1.8 giây ngẫu nhiên (theo tốc độ trong cài đặt), thỉnh thoảng gửi biểu cảm (emoji) đơn giản.
- 60fps: dùng `graphicsLayer`, `Animatable`, key ổn định, tránh recomposition thừa; kiểm tra trên máy tầm trung.
- Hỗ trợ TalkBack cơ bản: `contentDescription` cho lá bài ("Át cơ", "Mười bích").

---

## 4. Hệ thống chung

- **Ví xu**: tặng 50.000 xu khi tạo hồ sơ; quà đăng nhập 7 ngày tăng dần; cứu trợ khi số xu < mức cược nhỏ nhất (tối đa 3 lần/ngày). Bot có xu đủ chơi; bot "phá sản" thì được thay bằng bot mới.
- **Mức cược B**: mọi tiền thắng/thua trong luật được tính theo B (hoặc "lá" với 1 lá = 1B ở game đếm lá).
- **Số xu tối thiểu vào bàn** = `minBalanceMultiplier × B` trong RuleConfig mỗi game.
- **Đồng hồ lượt**: mặc định 20 giây (Mậu binh xếp bài 60 giây; Xì dách 15 giây). Hết giờ thì tự hành động an toàn: Tiến lên/Sâm bỏ lượt (nếu đang đi tự do thì đánh lá nhỏ nhất); Phỏm bốc rồi đánh lá rác điểm cao nhất không phá phỏm; Mậu binh tự xếp theo phương án tốt nhất; Xì dách dằn nếu đủ tuổi, không thì rút; Poker check nếu được, không thì úp; Liêng theo nếu chưa có ai tố, không thì úp.
- **Luật nhà**: mỗi game có màn bật/tắt biến thể, lưu theo game, hiện tóm tắt luật đang áp dụng trên bàn.
- **Lưu ván đang dở** khi app vào nền/bị tắt; mở lại cho chơi tiếp.
- **Thoát giữa ván**: xử thua như người về cuối (ghi rõ trong Luật chơi).

---

## 5. Luật chi tiết 8 game (phần quan trọng nhất — cài đặt chính xác)

> Ký hiệu chất: ♠ Bích, ♣ Chuồn (Tép), ♦ Rô, ♥ Cơ. "B" = mức cược bàn. Các con số là **mặc định** trong RuleConfig, chỉnh được ở Luật nhà.

### 5.1 Tiến lên miền Nam (TLMN)

**Bộ bài & chia**: 52 lá, mỗi người **13 lá** kể cả khi chơi 2–3 người (phần dư úp bỏ, không ai dùng). Đi ngược chiều kim đồng hồ.

**Thứ tự lá**: 3 < 4 < 5 < 6 < 7 < 8 < 9 < 10 < J < Q < K < A < 2 (lá 2 gọi là **heo**).
**Thứ tự chất**: ♠ < ♣ < ♦ < ♥. So 2 lá: so số trước, bằng số thì so chất.

**Người đi đầu**:
- Ván đầu tiên: người giữ **3♠** đi trước, nước đầu tiên phải có 3♠ (Luật nhà có thể tắt bắt buộc này). Nếu không ai có 3♠ (2–3 người), người giữ lá nhỏ nhất đi trước và phải đánh lá đó.
- Các ván sau: người về Nhất ván trước đi trước, đánh tự do.

**Tổ hợp hợp lệ**:
- **Rác** (lẻ): 1 lá.
- **Đôi**: 2 lá cùng số.
- **Sám** (bộ ba): 3 lá cùng số.
- **Sảnh**: ≥ 3 lá liên tiếp, không cần cùng chất, **không chứa 2**; A chỉ đứng cuối (Q-K-A hợp lệ; A-2-3, K-A-2 không hợp lệ). Dài tối đa 12 lá (3 → A).
- **Đôi thông**: ≥ 3 đôi liên tiếp (ví dụ 55-66-77), không chứa 2.
- **Tứ quý**: 4 lá cùng số.

**Chặn bài**: phải đánh **cùng loại, cùng số lá**, lớn hơn. So bộ theo **lá lớn nhất** của bộ (số rồi chất). Ví dụ: đôi 9♥9♠ chặn được 9♦9♣; sảnh 5-6-7♥ chặn được 5-6-7♦.

**Vòng và bỏ lượt**: người đã bỏ lượt thì mất quyền đánh trong vòng đó (trừ 4 đôi thông nếu bật luật). Khi mọi người khác đều bỏ, người đánh cuối được **đi tự do** vòng mới. Nếu người đánh cuối đã hết bài, quyền đi tự do chuyển cho người kế tiếp còn bài.

**Hàng chặt** (mặc định):

| Hàng | Chặt được | Điều kiện |
|---|---|---|
| 3 đôi thông | 1 heo; 3 đôi thông nhỏ hơn | Phải còn trong vòng |
| Tứ quý | 1 heo, đôi heo, 3 đôi thông, tứ quý nhỏ hơn | Phải còn trong vòng |
| 4 đôi thông | 1 heo, đôi heo, 3 đôi thông, tứ quý, 4 đôi thông nhỏ hơn | Không cần vòng (Luật nhà) |

- Sám heo (ba lá 2) không hàng nào chặt được.
- **Chặt chồng**: hàng vừa chặt có thể bị hàng lớn hơn chặt lại; người bị chặt **cuối cùng** gánh toàn bộ tiền của các lần chặt trước trong chuỗi, trả cho người chặt cuối cùng.

**Tới trắng** (kiểm tra ngay sau khi chia, thắng ngay, ván kết thúc):
1. Sảnh rồng: 12 lá 3 → A.
2. Tứ quý heo.
3. 5 đôi thông.
4. 6 đôi bất kỳ.
5. 13 lá đồng màu (cùng đỏ hoặc cùng đen) — Luật nhà: 12 lá.
6. Chỉ ván đầu tiên: tứ quý 3 (Luật nhà).

Nhiều người cùng tới trắng: ưu tiên theo thứ tự trên; cùng loại thì so lá lớn nhất.

**Tính tiền mặc định — chế độ "Đếm lá" (1 lá = 1B)**:
- Ván kết thúc khi có người **đầu tiên hết bài** (về Nhất).
- Mỗi người còn lại trả người về Nhất: **số lá còn lại × B**.
- **Thối** (còn trên tay khi ván kết thúc), tính **thay cho** 1 lá thường: heo đen 3 lá, heo đỏ 6 lá, 3 đôi thông 9 lá, tứ quý 12 lá, 4 đôi thông 18 lá.
- **Chặt** (thanh toán ngay khi chuỗi chặt kết thúc): người bị chặt trả người chặt theo giá trị phần bị chặt (heo đen 3, heo đỏ 6, đôi heo = cộng 2 lá heo, 3 đôi thông 9, tứ quý 12, 4 đôi thông 18).
- **Cóng** (chưa đánh được lá nào khi có người về): 26 lá + tiền thối heo/hàng.
- **Tới trắng**: mỗi người trả 26 lá (Luật nhà: cộng tiền thối heo/hàng).

**Luật nhà "Xếp hạng"** (thay cho Đếm lá): chơi tiếp đến khi còn 1 người. 4 người: Nhất +2B, Nhì +1B, Ba −1B, Bét −2B; 3 người: Nhất +1B, Nhì 0, Bét −1B; 2 người: thắng +1B. Hệ số: heo đen ½B, heo đỏ 1B, 3 đôi thông 1,5B, tứ quý 3B, 4 đôi thông 3B; cóng 2B + thối; tới trắng 2B mỗi người.

**Luật nhà khác**: bắt buộc 3♠ nước đầu; 4 đôi thông chặt không cần vòng; tứ quý 3 ván đầu; đồng màu 12/13 lá; 5 đôi thông tới trắng; tới trắng tính thối; cấm về bằng heo (mặc định tắt); tứ quý chặt đôi heo (mặc định bật).

### 5.2 Sâm lốc

**Bộ bài & chia**: 52 lá, mỗi người **10 lá** (app hỗ trợ 2–4 người). Phần dư úp bỏ.

**Thứ tự lá**: 3 < 4 < … < K < A < 2. **Không so chất**: hai bộ bằng nhau thì không chặn được nhau.

**Tổ hợp**: rác, đôi, sám, tứ quý, **sảnh** (≥ 3 lá liên tiếp, không cần cùng chất). Trong sảnh, A có thể đứng đầu (A-2-3) hoặc cuối (Q-K-A), 2 có thể đứng đầu (2-3-4…); không được vòng qua (K-A-2 sai). So sảnh cùng độ dài theo lá cuối: A-2-3 < 2-3-4 < 3-4-5 < … < Q-K-A. Lưu ý: lá 2 đánh lẻ là lá lớn nhất, nhưng trong sảnh thì đứng thấp. **Không có đôi thông.**

**Chặt**: tứ quý chặt được **1 heo**; tứ quý lớn chặt tứ quý nhỏ. (Luật nhà: tứ quý chặt đôi heo — mặc định tắt.)

**Người đi trước**: có người báo Sâm thì người đó đi trước. Không ai báo: ván đầu ngẫu nhiên; ván sau người thắng ván trước.

**Báo Sâm**:
- Sau khi chia, có **pha báo Sâm**: lần lượt từ người đi trước dự kiến, mỗi người có 8 giây chọn **Báo Sâm** hoặc **Bỏ qua**. Chỉ người báo đầu tiên được nhận.
- Người báo Sâm đánh liên tục; sau mỗi nước, những người khác lần lượt được chặn.
- Có bất kỳ ai chặn được → **Sâm thất bại (đền Sâm)**, ván kết thúc ngay.
- Đánh hết 10 lá không ai chặn → **ăn Sâm**, ván kết thúc.

**Báo 1**: người còn 1 lá tự động "Báo 1" (hiện nhãn). Người ngồi **ngay trước** người báo, khi đánh lẻ (đi tự do hoặc chặn lẻ) phải đánh **lá lẻ lớn nhất** đang có. Nếu không làm vậy và người báo về ngay → người vi phạm **đền cả làng**. App hiện cảnh báo trước khi người chơi đánh sai; bot Thường/Khó luôn tuân thủ, bot Dễ thỉnh thoảng phạm.

**Về bằng heo**: nước cuối cùng có chứa lá 2 → bị xử **thối 2**: người đó không được tính thắng, trả mỗi người 15 lá, ván kết thúc.

**Ăn trắng** (ngay khi chia): tứ quý heo; sảnh rồng 10 lá liên tiếp; 5 đôi; 10 lá đồng màu. (Luật nhà: 3 sám — mặc định tắt.)

**Tính tiền mặc định (1 lá = 1B)**:
- Người về Nhất ăn của mỗi người: số lá còn lại × B.
- Thối heo: 15 lá/con; thối tứ quý: 15 lá (tính thay cho các lá đó). Luật nhà: heo đen 10 / heo đỏ 15.
- Cóng: 15 lá + tiền thối heo/tứ quý nếu có.
- Tứ quý chặt heo: người bị chặt trả 15 lá/con; tứ quý chặt tứ quý: 15 lá; chặt chồng: người bị chặt cuối gánh tổng.
- Ăn Sâm: mỗi người trả 20 lá. Đền Sâm: người báo trả mỗi người 20 lá.
- Ăn trắng: mỗi người trả 20 lá.
- Đền báo 1: người vi phạm trả toàn bộ số tiền những người thua khác lẽ ra phải trả.

### 5.3 Phỏm (Tá lả)

**Bộ bài & chia**: người đi đầu **10 lá**, mỗi người khác **9 lá**, phần còn lại úp làm **nọc** (2 người: 33 lá; 3 người: 24; 4 người: 15).

**Điểm lá**: A = 1, 2–10 theo số, J = 11, Q = 12, K = 13. A là lá nhỏ nhất, không nối vòng.

**Phỏm**:
- Phỏm ngang: ≥ 3 lá cùng số (ví dụ 7♠7♥7♦).
- Phỏm dọc: ≥ 3 lá liên tiếp **cùng chất** (A-2-3♥ hợp lệ; Q-K-A không hợp lệ).
- **Cạ**: 2 lá có thể thành phỏm khi có thêm 1 lá.

**Diễn biến**:
1. Người đi đầu đánh 1 lá (không bốc).
2. Người kế tiếp: **Ăn** lá vừa đánh của người liền trước (chỉ khi lá đó tạo phỏm với bài trên tay) **hoặc Bốc** 1 lá từ nọc; sau đó **Đánh** 1 lá.
3. Lá ăn cùng các lá tạo phỏm với nó được đặt ngửa trước mặt (mọi người thấy).
4. Mỗi người đánh đủ **4 lá rác**. Ở lượt cuối của mình: bốc/ăn → **Hạ phỏm** → **Gửi bài** → đánh lá rác thứ 4.

**Ràng buộc khi ăn** (engine phải chặn):
- Mỗi phỏm chứa tối đa **1 lá ăn** (không ăn 2 lá vào cùng một phỏm).
- Không được đánh lá đã ăn, không được đánh lá làm vỡ phỏm chứa lá đã ăn (lá bị "trói").
- Lá đã ăn phải nằm trong phỏm khi hạ.

**Cây chốt**: lá rác thứ 4 (lượt cuối) của người liền trước. Ăn lá này gọi là **ăn chốt**.

**Chuyển bài / Tái**: khi một lá bị ăn, chồng rác của người bị ăn thiếu 1 lá → chuyển lá rác trên cùng của người đánh liền trước người bị ăn sang chồng người bị ăn. Người bị chuyển mất lá phải đánh bù (**tái**): đến lượt thì bốc/ăn, có thể gửi tiếp, rồi đánh 1 lá. Lượt đi xoay vòng, bỏ qua người đã hạ và đã đủ 4 lá rác. Bất biến phải giữ: mỗi người kết thúc với đúng 4 lá rác; nọc không bao giờ cạn trước khi ván kết thúc (viết test mô phỏng nhiều lần ăn).

**Gửi bài**: sau khi hạ, được gửi lá rác vào phỏm của người **đã hạ trước** (nối phỏm dọc hoặc thêm vào phỏm ngang). Lá đã gửi không tính điểm. Người móm không được gửi (Luật nhà).

**Kết thúc & xếp hạng**:
- Ván kết thúc khi mọi người hạ xong và đủ 4 lá rác, hoặc có người Ù.
- Tổng điểm lá rác còn lại **thấp nhất** là Nhất, rồi Nhì, Ba, Bét. **Bằng điểm: người hạ trước xếp trên.**
- **Móm**: không có phỏm nào để hạ → xếp sau tất cả.

**Ù**: sau khi bốc/ăn, nếu toàn bộ bài trên tay xếp được thành phỏm (cho phép thừa đúng 1 lá để đánh đi) → **Ù**, thắng ngay. **Ù khan** (Luật nhà, mặc định bật): bài chia ra không có phỏm và không có cạ nào → được báo Ù khan.

**Tính tiền mặc định**:
- Bị ăn 1 lá thường: trả người ăn 1B. Bị ăn chốt: 4B. (Luật nhà: tăng dần 1B / 2B / 3B theo số lần bị ăn.)
- Kết thúc thường: Nhì trả Nhất 1B, Ba trả 2B, Bét trả 3B; mỗi người Móm trả 4B.
- **Ù**: mỗi người trả 5B.
- **Ù đền** (người đền trả 5B × số người còn lại, thay cả làng; người khác không trả):
  - (a) để **cùng một người ăn 3 lá** rồi người đó Ù;
  - (b) đánh cây chốt cho nhà dưới ăn và nhà dưới Ù;
  - (c) Luật nhà (mặc định bật): đã ăn chốt mà sau đó trong vòng cuối có người Ù → người ăn chốt sau cùng đền.

### 5.4 Mậu binh (Binh xập xám)

**Bộ bài & chia**: 52 lá, mỗi người **13 lá** (2–4 người). Mỗi người có **60 giây** xếp bài, bấm **Xếp xong**; hết giờ hệ thống tự xếp theo phương án tốt nhất hợp lệ.

**3 chi**:
- **Chi 1 (chi đầu)**: 5 lá — phải mạnh nhất.
- **Chi 2 (chi giữa)**: 5 lá.
- **Chi 3 (chi cuối)**: 3 lá.
- Bắt buộc **Chi 1 ≥ Chi 2 ≥ Chi 3**. Sai → **binh lủng**.

**Thứ tự lá**: 2 < 3 < … < K < A. **Không so chất.**

**Xếp hạng chi 5 lá** (mạnh → yếu): Thùng phá sảnh > Tứ quý > Cù lũ > Thùng > Sảnh > Sám cô > Thú (2 đôi) > Đôi > Mậu thầu.
- Sảnh lớn nhất 10-J-Q-K-A; **A-2-3-4-5 lớn thứ hai** (Luật nhà: nhỏ nhất).
- So cùng loại: Tứ quý theo số tứ quý; Cù lũ theo bộ ba; Thùng/Mậu thầu so lần lượt từ lá cao xuống; Thú so đôi lớn → đôi nhỏ → lá lẻ; Đôi so đôi rồi các lá lẻ. Bằng hoàn toàn → hòa chi.

**Chi 3 (3 lá)**: chỉ có Sám cô > Đôi > Mậu thầu (không có sảnh/thùng 3 lá). Khi kiểm tra Chi 2 ≥ Chi 3: so loại trước, rồi so lá chính, rồi các lá phụ đang có.

**So bài**: mỗi cặp người chơi so lần lượt chi 1, chi 2, chi 3 (UI lật lần lượt từng chi cho cả bàn như ngoài đời). Thắng 1 chi = +1 chi, thua = −1 chi.

**Chi thưởng** (thắng chi đó bằng hàng sau thì được thay cho 1 chi):

| Hàng | Ở chi | Thắng được |
|---|---|---|
| Sám cô | Chi 3 | 3 chi |
| Cù lũ | Chi 2 | 2 chi |
| Tứ quý | Chi 1 | 4 chi |
| Tứ quý | Chi 2 | 8 chi |
| Thùng phá sảnh | Chi 1 | 5 chi |
| Thùng phá sảnh | Chi 2 | 10 chi |

- **Sập 3 chi** (thắng cả 3 chi với một người): tổng với người đó **×2**.
- **Sập làng** (từ 3 người trở lên, sập tất cả đối thủ): tổng **×2 thêm**.
- **Binh lủng**: thua mỗi đối thủ hợp lệ 6 chi (như bị sập); hai người cùng lủng thì hòa nhau.

**Mậu binh tới trắng** (không cần so chi; app phát hiện ngay khi chia và hỏi "Báo Mậu binh?"):

| Loại (cao → thấp) | Mô tả | Ăn mỗi người |
|---|---|---|
| Rồng cuốn | 13 lá 2 → A cùng chất | 24 chi |
| Sảnh rồng | 13 lá 2 → A | 12 chi |
| Năm đôi một sám | 5 đôi + 1 bộ ba | 3 chi |
| Lục phé bôn | 6 đôi + 1 lá lẻ | 3 chi |
| Ba thùng | Cả 3 chi đều cùng chất (chi 3 gồm 3 lá cùng chất) | 3 chi |
| Ba sảnh | Cả 3 chi đều là sảnh (chi 3 gồm 3 lá liên tiếp) | 3 chi |

Nhiều người cùng tới trắng: loại cao hơn thắng theo mức của mình; cùng loại thì hòa.

**Tính tiền**: 1 chi = 1B. Tổng hợp theo từng cặp người chơi.

### 5.5 Xì dách (luật Việt Nam, khác Blackjack phương Tây)

**Bộ bài**: 52 lá. Mỗi người (gồm nhà cái) nhận **2 lá úp**, rút tối đa đến **5 lá**.

**Điểm lá**: 2–10 theo số; J, Q, K = 10; **A tính linh hoạt**:
- Bài 2 lá: A = 10 hoặc 11.
- Bài 3 lá: A = 1 hoặc 10.
- Bài 4–5 lá: A = 1.
- Engine tự chọn cách tính tốt nhất (≤ 21, cao nhất).

**Bài đặc biệt** (mạnh → yếu): **Xì bàng** (2 lá A) > **Xì dách** (A + 10/J/Q/K) > **Ngũ linh** (5 lá, tổng ≤ 21) > 21 > 20 > … > 16.

**Trạng thái**: Non (< 16 với con, < 15 với cái), Đủ tuổi (16–21; cái 15–21), Quắc (> 21).

**Diễn biến**:
1. Nhà cái: theo lựa chọn ở màn chọn bàn (Bạn / Máy / Luân phiên mỗi ván). Luật nhà: ai có Xì dách/Xì bàng thì làm cái ván sau.
2. Các con đặt cược 1B–5B trước khi chia.
3. **Kiểm tra đầu ván**: cái có Xì bàng/Xì dách → lật ngay, xét cả bàn. Con có Xì bàng/Xì dách → lật ngay và thắng (trừ khi cái bằng hoặc hơn).
4. **Lượt con** (bắt đầu từ bên phải cái, ngược chiều kim đồng hồ): **Rút** hoặc **Dằn**. Chỉ được Dằn khi ≥ 16. Quắc thì vẫn giữ bài úp (không ai biết) cho tới khi bị xét.
5. **Lượt cái**: phải rút tới ≥ 15 mới được **Xét**. Cái được **xét từng con** (chọn người), sau đó có thể rút thêm (nếu chưa đủ 5 lá) rồi xét tiếp những người còn lại, hoặc **Xét tất cả**. Cái quắc thì lật bài và so với những con chưa xét.

**So thắng thua**:
- Bằng điểm → hòa. Ngũ linh gặp ngũ linh → ít điểm hơn thắng. Xì dách gặp xì dách → hòa.
- Con quắc, cái không quắc → con thua. Cái quắc, con không quắc → con thắng. **Cả hai cùng quắc → hòa** (Luật nhà: con quắc luôn thua).

**Trả thưởng mặc định**: thắng thường 1×; Xì dách 2×; Xì bàng 3×; Ngũ linh 2×.

### 5.6 Poker Texas Hold'em (No-limit)

- Bộ 52 lá. A cao nhất, A cũng là lá thấp trong sảnh A-2-3-4-5.
- **Nút dealer** xoay vòng mỗi ván; **SB = B/2, BB = B**. Đánh 2 người: người giữ nút là SB, đi trước ở preflop, đi sau từ flop.
- **Buy-in**: 50–200 BB (mặc định 100 BB) lấy từ ví; rời bàn thì stack quay về ví.
- **Vòng cược**: Preflop (người sau BB đi trước) → Flop (3 lá chung) → Turn (1 lá) → River (1 lá). Từ flop: người còn chơi đầu tiên bên trái nút đi trước. Đốt 1 lá trước flop/turn/river.
- **Hành động**: Úp (Fold), Xem (Check), Theo (Call), Cược (Bet), Tố thêm (Raise), Tất tay (All-in). Cược tối thiểu = BB; raise tối thiểu = mức tăng của lần raise trước. All-in không đủ mức raise tối thiểu thì không mở lại quyền raise cho người đã hành động.
- **Showdown**: người bet/raise cuối ở river lật trước; không có cược thì người đầu tiên bên trái nút lật trước. Người thua được úp bài (muck).
- **Side pot** nhiều tầng; hòa chia đều, xu lẻ cho người gần bên trái nút nhất.
- **Xếp hạng tay** (tên tiếng Việt trên UI): Thùng phá sảnh lớn (Royal Flush) > Thùng phá sảnh > Tứ quý > Cù lũ > Thùng > Sảnh > Sám cô > Thú (2 đôi) > Đôi > Mậu thầu (lá cao). Ghép **5 lá tốt nhất từ 7 lá**, so kicker đúng chuẩn. Không so chất.
- UI hiện tên tay bài hiện tại của bạn ("Đôi K", "Thùng rô"…). Phần trăm thắng chỉ hiện ở chế độ luyện tập (mặc định tắt).
- Hết stack: nạp lại (rebuy) hoặc rời bàn.

### 5.7 Liêng

**Bộ bài & chia**: 52 lá, mỗi người **3 lá úp**.

**Xếp hạng** (mạnh → yếu): **Sáp > Liêng > Ảnh > Điểm**.
- **Sáp**: 3 lá cùng số. Sáp A lớn nhất, sáp 2 nhỏ nhất. Chỉ so số.
- **Liêng**: 3 lá liên tiếp, không cần cùng chất. Lớn nhất Q-K-A, nhỏ nhất A-2-3; không vòng (K-A-2 sai). Cùng giá trị → so chất lá cao nhất (Luật nhà: hòa).
- **Ảnh** (còn gọi "đĩ"): 3 lá đều là J/Q/K nhưng không phải Sáp hay Liêng (J-Q-K là Liêng). So lá mạnh nhất.
- **Điểm**: A = 1, 2–9 theo số, 10/J/Q/K = 0; lấy tổng chia 10 lấy dư (9 cao nhất, 0 thấp nhất). Bằng điểm → so lá mạnh nhất.

**Thứ tự chất**: **♦ Rô > ♥ Cơ > ♣ Chuồn > ♠ Bích** (Luật nhà: Cơ > Rô). **Lá mạnh nhất** mặc định so chất trước, cùng chất thì so số A > K > Q > J > 10 > … > 2 (Luật nhà: so số trước).

**Cược**:
- Mỗi người bỏ **cược sàn** B vào pot rồi chia bài.
- Tối đa **3 vòng tố**. Mỗi lượt: **Theo**, **Tố** (thêm tối thiểu B, tối đa bằng pot hiện tại), **Úp**, **Tất tay**.
- Vòng kết thúc khi mọi người còn lại đã cược bằng nhau. Hết 3 vòng hoặc chỉ còn 1 người → lật bài.
- Có side pot khi tất tay. Hòa thì chia pot.
- Người chơi được **nặn bài** (xem từ từ) trước khi quyết định.

### 5.8 Ba cây (Cào)

**Bộ bài & chia**: 52 lá, mỗi người **3 lá úp**.

**Điểm**: A = 1, 2–9 theo số, 10/J/Q/K = 10 (tính 0). Tổng chia 10 lấy dư, **9 nút** cao nhất, 0 ("bù") thấp nhất.

**Ba tiên (Ba cào)**: 3 lá đều là J/Q/K → lớn hơn 9 nút. Luật nhà: Sáp (3 lá cùng số) đứng trên Ba tiên (mặc định tắt); Ba tiên ăn ×2 (mặc định tắt).

**Bằng điểm**: so **lá mạnh nhất** — chất trước **♦ Rô > ♥ Cơ > ♣ Chuồn > ♠ Bích**, cùng chất so số A > K > … > 2 (Át rô là lá mạnh nhất). Luật nhà: bằng điểm thì hòa.

**Chế độ chơi**:
- **Có cái** (mặc định): nhà cái theo lựa chọn (Bạn / Máy / Luân phiên). Con đặt cược 1B–5B; mỗi con so riêng với cái; thắng ăn 1× cược, thua mất 1×.
- **Ăn tất**: mỗi người bỏ B vào pot, bài cao nhất ăn cả pot; hòa chia đều.
- Có **nặn bài** trước khi lật, lật lần lượt có kịch tính.

---

## 6. Bot (AI máy)

Nguyên tắc chung:
- Chỉ dùng `viewOf(player)` + lịch sử bài đã lộ. Không bao giờ đọc bài úp.
- **Dễ**: quyết định đơn giản, đôi khi sai nhẹ (không đếm bài, bỏ lượt dù chặn được, tố/theo ngẫu nhiên hơn).
- **Thường**: heuristic tốt theo từng game.
- **Khó**: heuristic + **đếm bài** + **mô phỏng Monte Carlo** (lấy mẫu bài ẩn phù hợp với thông tin đã biết) cho các quyết định quan trọng; giới hạn thời gian ≤ 800ms, chạy trên `Dispatchers.Default`, hủy được.

Theo từng game:
- **Tiến lên / Sâm**: tách tay bài thành tổ hợp tối ưu (ít lượt nhất để hết bài), không tách sảnh/đôi vô lý; giữ heo và hàng để chặt; khi đi tự do đánh bộ yếu hoặc sảnh dài để xả rác; đối thủ còn ít lá thì chặn chắc, tránh cho đối thủ đi tự do, đánh loại bộ mà đối thủ khó có; tuân thủ Báo 1. Sâm: đánh giá có nên báo Sâm (mô phỏng xem có ai chặn được không, chỉ báo khi xác suất thắng rất cao).
- **Phỏm**: theo dõi lá đã đánh/đã ăn để đoán cạ của đối thủ; ưu tiên giữ cạ "sống"; tránh đánh lá dễ bị nhà dưới ăn (đặc biệt ở vòng chốt — "xé cạ" khi cần); đánh rác điểm cao trước; quyết định ăn/bốc theo điểm kỳ vọng; tự tìm cách hạ và gửi tối ưu (bài toán phủ tập — dùng tìm kiếm vét cạn có cắt tỉa).
- **Mậu binh**: duyệt các cách chia 13 lá thành 5-5-3 hợp lệ (có cắt tỉa), chấm điểm theo xác suất thắng từng chi (bảng tra hoặc mô phỏng) + chi thưởng; luôn tránh binh lủng; tự nhận Mậu binh tới trắng. Hàm này dùng chung cho nút **"Xếp gợi ý"** của người chơi.
- **Xì dách**: con — bắt buộc rút khi < 16, từ 16–17 cân nhắc theo số lá đã lộ và độ khó, săn Ngũ linh khi có 4 lá tổng thấp. Cái — rút tới ≥ 15; nếu điểm cao (≥ 18) thì xét tất cả; nếu 15–17 thì ưu tiên xét những con đã rút nhiều lá (khả năng quắc cao), né những con dằn sớm với 2 lá, rồi cân nhắc rút thêm.
- **Poker**: preflop theo bảng tay bài theo vị trí và số người; postflop ước lượng equity bằng Monte Carlo (500–3000 lần), so với pot odds; kích thước cược theo độ mạnh; bluff/semi-bluff theo tần suất tùy độ khó; Khó có thêm theo dõi phong cách người chơi (VPIP/AF đơn giản).
- **Liêng / Ba cây**: tính xác suất thắng của tay bài so với số đối thủ; tố/theo/úp theo ngưỡng; có tố láo (bluff) ở Thường/Khó với tần suất hợp lý.

---

## 7. Luật chơi, Hướng dẫn và Ván tập (bắt buộc cho cả 8 game)

Mỗi game có 3 phần, viết bằng tiếng Việt tự nhiên như người chơi lâu năm hướng dẫn người mới, **minh họa bằng chính component lá bài** của app (không dùng ảnh chụp):

1. **Luật chơi** (màn có mục lục, cuộn được):
   - Giới thiệu ngắn, vùng miền phổ biến.
   - Bộ bài, số người, cách chia, thứ tự lá và chất.
   - Các tổ hợp/bộ bài, mỗi loại có ví dụ minh họa bằng lá bài.
   - Diễn biến một ván từng bước.
   - Tình huống đặc biệt (chặt, tới trắng, báo Sâm, báo 1, Ù, ăn chốt, binh lủng, xét bài, side pot…).
   - **Bảng tính tiền** đọc từ RuleConfig đang áp dụng.
   - Từ điển thuật ngữ (heo, chặt, thối, cóng, cạ, móm, tái, gửi, dằn, quắc, non, tố, úp, sáp, ảnh, nút…).
   - Mẹo chơi cho người mới.
2. **Hướng dẫn nhanh**: 4–6 thẻ trượt, mỗi thẻ 1 ý chính + hình minh họa; hiện tự động lần đầu vào game, có nút xem lại.
3. **Ván tập** (tutorial tương tác): ván bài được **sắp sẵn bài** (seed cố định), có hướng dẫn chỉ tay (coach marks), chỉ cho phép thao tác đúng bước:
   - Tiến lên: đánh rác, chặn đôi, chặt heo bằng 3 đôi thông, bỏ lượt và đi tự do.
   - Sâm: báo Sâm thành công, tình huống Báo 1.
   - Phỏm: ăn cây, bốc nọc, hạ phỏm, gửi bài, Ù.
   - Mậu binh: xếp 3 chi đúng, ví dụ binh lủng, sập 3 chi.
   - Xì dách: rút/dằn, A đổi giá trị, Ngũ linh, cái xét bài.
   - Poker: blind, 4 vòng cược, all-in và side pot.
   - Liêng: tố/theo/úp, so Sáp – Liêng – Ảnh – Điểm.
   - Ba cây: tính điểm, Ba tiên, nặn bài.

Trong bàn chơi có nút **"?"** mở bottom sheet luật tóm tắt và nút **"Gợi ý"** (đánh dấu nước đi hợp lệ/khuyên dùng).

---

## 8. Kiểm thử (bắt buộc)

**Unit test luật** — ít nhất các trường hợp:
- Tiến lên: sảnh có A cuối hợp lệ, sảnh chứa 2 không hợp lệ; so đôi theo chất; 3 đôi thông không chặt được đôi heo; tứ quý chặt đôi heo; 4 đôi thông chặt khi đã bỏ lượt; chặt chồng chuyển tiền đúng người; người về khi đang giữ vòng thì quyền đi tự do chuyển đúng người kế; mỗi loại tới trắng; tính cóng và thối.
- Sâm: sảnh A-2-3 và 2-3-4 hợp lệ, K-A-2 không hợp lệ; hai bộ bằng nhau không chặn được; báo Sâm bị chặn → đền; báo 1 vi phạm → đền làng; về bằng heo → thối 2.
- Phỏm: phỏm dọc khác chất không hợp lệ; không ăn 2 lá vào 1 phỏm; không đánh lá bị trói; ăn chốt tính 4B; chuyển bài/tái giữ đúng bất biến 4 lá rác; Ù đền đủ 3 trường hợp; bằng điểm người hạ trước thắng; móm xếp cuối.
- Mậu binh: phát hiện binh lủng (kể cả so chi 2 năm lá với chi 3 ba lá); A-2-3-4-5 đứng thứ hai; từng loại chi thưởng; sập 3 chi, sập làng; từng loại tới trắng (ba thùng/ba sảnh tính đúng chi 3 ba lá).
- Xì dách: A = 11/10 với 2 lá, 10/1 với 3 lá, 1 với 4–5 lá; ngũ linh vs ngũ linh; cả hai quắc → hòa; cái chưa đủ 15 không được xét.
- Poker: bộ đánh giá tay bài đúng với bộ test chuẩn (sảnh A-5, kicker, thùng vs sảnh, hai cù lũ); side pot 3 tầng; incomplete raise.
- Liêng / Ba cây: sáp > liêng > ảnh > điểm; J-Q-K là liêng không phải ảnh; so chất Rô > Cơ > Chuồn > Bích; Ba tiên > 9 nút.

**Test thuộc tính & mô phỏng**:
- Xáo bài luôn ra đúng 52 lá khác nhau.
- Mỗi game: cho bot đấu bot **10.000 ván** ngẫu nhiên → không crash, không trạng thái bất hợp lệ, tổng tiền mỗi ván = 0.
- Bot không truy cập được bài úp (test bằng cách kiểm tra `viewOf` không chứa bài ẩn).
- Lưu/khôi phục ván giữa chừng cho ra state y hệt.

**UI test**: luồng Sảnh → Chọn bàn → Chơi 1 ván → Kết quả; nút hành động chỉ bật khi hợp lệ.

---

## 9. Hiệu năng, lưu trữ, chất lượng

- Khởi động lạnh < 2 giây trên máy tầm trung; bàn chơi 60fps.
- Không cấp quyền thừa (không cần mạng, không cần danh bạ, vị trí…).
- Xử lý xoay màn hình, vào nền, thiếu bộ nhớ: không mất ván.
- Code tuân Kotlin coding conventions, có KDoc cho phần luật, chạy detekt/ktlint.
- `README.md` hướng dẫn build, cấu trúc project, cách thêm game mới.

---

## 10. Lộ trình phát triển

1. **Giai đoạn 1 – Nền móng**: project, module, design system, CardView vẽ vector, animation chia/lật/đánh bài, Sảnh, Chọn bàn, ví xu, cài đặt.
2. **Giai đoạn 2 – Tiến lên miền Nam** hoàn chỉnh (engine, bot 3 mức, UI bàn chơi, luật chơi, hướng dẫn, ván tập, test). Đây là mẫu chuẩn cho các game sau.
3. **Giai đoạn 3 – Sâm lốc** (tái dùng phần lớn từ Tiến lên).
4. **Giai đoạn 4 – Phỏm (Tá lả)**.
5. **Giai đoạn 5 – Mậu binh**.
6. **Giai đoạn 6 – Xì dách và Ba cây** (chung khung nhà cái, nặn bài).
7. **Giai đoạn 7 – Liêng và Poker** (chung khung cược, pot, side pot).
8. **Giai đoạn 8 – Hoàn thiện**: thống kê, thành tích, âm thanh/nhạc, hiệu ứng lớn, lưu ván, tối ưu hiệu năng, test mô phỏng 10.000 ván, README.

Sau mỗi giai đoạn: tóm tắt đã làm, cách chạy thử, và các quyết định luật đã ghi vào `docs/RULE_DECISIONS.md`.

---

## 11. Định nghĩa hoàn thành (checklist cuối)

- [ ] 8 game chơi được ở cả 3 chế độ 2/3/4 người, 3 độ khó bot.
- [ ] Luật mặc định đúng như tài liệu; Luật nhà bật/tắt hoạt động và màn Luật chơi hiển thị đúng số liệu đang áp dụng.
- [ ] Mỗi game có Luật chơi, Hướng dẫn nhanh, Ván tập tương tác.
- [ ] Giao diện ngang, hiện đại, có đủ animation chia/lật/đánh/chip/banner, nặn bài, âm thanh, rung.
- [ ] Bot không gian lận; test mô phỏng 10.000 ván/game qua; tiền zero-sum.
- [ ] Lưu và tiếp tục ván dở; thống kê từng game.
- [ ] Không có tính năng tiền thật; có thông báo chơi giải trí lành mạnh.
- [ ] Build release chạy được, README đầy đủ.
