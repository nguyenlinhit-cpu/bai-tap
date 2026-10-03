# 🎴 Bài Việt – Tổng Hợp Game Bài

Ứng dụng Android chơi bài offline với máy, gồm 8 thể loại phổ biến ở Việt Nam.

## Yêu cầu

- JDK 17
- Android SDK (compileSdk 35, minSdk 26)
- Gradle 8.14+

### Với Nix Flakes

```bash
nix develop          # Tự cài JDK, SDK, Gradle, Kotlin, ktlint, detekt
# hoặc
direnv allow         # Nếu dùng direnv
```

## Build

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew test                          # Unit tests
./gradlew connectedAndroidTest          # UI tests
./gradlew :core:cards:test              # Test module cards
```

## Cấu trúc project

```
:app                    # MainActivity, NavHost, theme, Hilt
:core:cards             # Card, Suit, Rank, Deck, xáo bài
:core:engine            # GameEngine, PlayerId, Settlement, GameEvent, RuleConfig
:core:ai                # Bot, BotLevel, Monte Carlo helper
:core:ui                # Design system, CardView, ChipStack, TableScaffold
:core:data              # Room, DataStore, ví xu, thống kê, lưu ván
:game:tienlen           # Tiến Lên Miền Nam
:game:samloc            # Sâm Lốc
:game:phom              # Phỏm (Tá Lả)
:game:maubinh           # Mậu Binh
:game:xidach            # Xì Dách
:game:poker             # Poker Texas Hold'em
:game:lieng             # Liêng
:game:bacay             # Ba Cây (Cào)
```

## 8 Game

| Game | Vùng | Số người | Đặc trưng |
|------|------|----------|-----------|
| Tiến lên miền Nam | Nam | 2–4 | Đánh hết bài, chặt heo, tới trắng |
| Sâm lốc | Nam | 2–4 | Báo Sâm, báo 1, không so chất |
| Phỏm (Tá lả) | Bắc | 2–4 | Ăn/bốc, hạ phỏm, gửi bài |
| Mậu binh | Nam | 2–4 | Xếp 3 chi, so chi, mậu binh tới trắng |
| Xì dách | Nam | 2–4 | Gần 21, nhà cái, ngũ linh |
| Poker Texas | Quốc tế | 2–4 | No-limit, 4 vòng cược, side pot |
| Liêng | Nam | 2–4 | Sáp > Liêng > Ảnh > Điểm |
| Ba cây (Cào) | Nam | 2–4 | Ba tiên, nặn bài |

## Trạng thái

| Giai đoạn | Nội dung | Trạng thái |
|---|---|---|
| 1 | Nền móng: module, design system, lá bài vector, sảnh, chọn bàn, ví xu, cài đặt | ✅ |
| 2 | Tiến lên miền Nam: engine, bot 3 mức, bàn chơi, luật chơi, hướng dẫn nhanh, ván tập, luật nhà, test (10.000 ván) | ✅ |
| 3 | Sâm lốc: engine, bot 3 mức, báo Sâm (8s), báo 1 & đền làng, thối 2 khi về, ăn trắng, bàn chơi landscape, luật nhà, ván tập, test (10.000 ván) | ✅ |
| 4 | Phỏm (Tá lả): ăn/bốc, hạ phỏm, gửi bài, Ù, Ù khan, Ù đền 3 trường hợp, móm, tính điểm rác, bàn chơi landscape, luật nhà, ván tập, test (10.000 ván) | ✅ |
| 5 | Mậu binh: xếp 3 chi (5-5-3), so chi, không so chất, A-2-3-4-5 lớn thứ hai (luật nhà: nhỏ nhất), chi thưởng (Sám chi 3, Cù lũ chi 2, Tứ quý, Thùng phá sảnh), sập 3 chi, sập làng, phạt binh lủng, tới trắng (6 loại), hand optimizer xếp gợi ý, bot 3 mức, bàn landscape, luật nhà, ván tập, test (10.000 ván) | ✅ |
| 6 | Xì dách + Ba cây (Cào): chung khung nhà cái (Bạn làm cái / Máy làm cái / Luân phiên), nặn bài hồi hộp (kéo vuốt hé lá bài), luật Việt Nam (Xì bàng, Xì dách, Ngũ linh, tính điểm A 10/11 - 1/10 - 1, cả hai quắc hòa, con đủ 16 dằn, cái đủ 15 xét; Ba cây 9 nút, Ba tiên, Sáp, so chất Rô > Cơ > Chuồn > Bích, chế độ có cái & ăn tất), bot 3 mức, luật nhà, ván tập, test (10.000 ván mỗi game) | ✅ |
| 7 | Liêng + Poker Texas Hold'em: chung khung cược/pot/side pot, all-in, incomplete raise, bot 3 mức, luật nhà, ván tập, test (10.000 ván) | ✅ |
| 8 | Hoàn thiện: Hồ sơ & thống kê từng game, 7 thành tích (thông báo khi mở khóa), âm thanh tổng hợp + nhạc nền, rung theo Cài đặt, banner/particle sự kiện lớn có tiếng, **lưu & chơi tiếp ván dở cho cả 8 game**, hướng dẫn nhanh riêng từng game (tự hiện lần đầu), chọn nhà cái (Xì dách/Ba cây), buy-in Poker, sửa lỗi Xì dách/Ba cây/Phỏm/Mậu binh/Sâm lốc (dùng ví thật, chặn theo số dư, thay bot hết xu, đồng hồ lượt, phạt thoát giữa ván), test lưu/khôi phục | ✅ |

## Chạy thử

APK dựng sẵn: `output/BaiViet-1.0.0-release.apk` (cài trực tiếp) và `output/BaiViet-1.0.0-debug.apk`.

1. Mở app → Sảnh → chọn 1 trong 8 game → Chọn bàn (2/3/4 người, độ khó bot, mức cược xu; Xì dách/Ba cây chọn nhà cái; Poker chọn buy-in).
2. **Ván tập**: Mỗi game đều có hướng dẫn kịch bản tương tác trực quan từng bước.
3. **Luật nhà**: Tùy biến các biến thể luật chơi dân gian quen thuộc (Cả hai quắc hòa/con thua, Sáp đứng trên Ba tiên, Ba tiên x2, Bằng điểm thì hòa...). Màn **Luật chơi** tự động cập nhật số liệu thời gian thực.
4. **Nặn bài (Card Squeeze)**: Trải nghiệm vuốt kéo hé mở lá bài chân thực, hồi hộp đặc trưng trong Xì Dách, Ba Cây và Liêng.
5. **Lưu ván dở**: Tắt app giữa ván rồi mở lại → thẻ game có nhãn "Ván dở" → Chọn bàn → **Chơi tiếp**.
6. **Hồ sơ & thống kê**: chạm avatar hoặc nút 🏆 ở Sảnh — số ván, thắng, tỉ lệ thắng, ván thắng lớn nhất từng game, thành tích.
7. **Âm thanh & rung**: bật/tắt nhạc nền, hiệu ứng, rung trong Cài đặt (âm thanh tổng hợp, không cần file).

```bash
./gradlew :game:tienlen:testDebugUnitTest   # Tiến lên: luật, engine, bot, mô phỏng 10.000 ván
./gradlew :game:samloc:testDebugUnitTest    # Sâm lốc: luật, engine, bot, mô phỏng 10.000 ván
./gradlew :game:phom:testDebugUnitTest      # Phỏm: luật, engine, bot, mô phỏng 10.000 ván
./gradlew :game:maubinh:testDebugUnitTest   # Mậu binh: luật, optimizer, bot, mô phỏng 10.000 ván
./gradlew :game:xidach:testDebugUnitTest    # Xì dách: luật, engine, bot, mô phỏng 10.000 ván
./gradlew :game:bacay:testDebugUnitTest     # Ba cây: luật, engine, bot, mô phỏng 10.000 ván
./gradlew :game:poker:testDebugUnitTest     # Poker: đánh giá tay bài, side pot, incomplete raise, 10.000 ván
./gradlew :game:lieng:testDebugUnitTest     # Liêng: sáp > liêng > ảnh > điểm, cược, 10.000 ván
./gradlew testDebugUnitTest --tests '*SaveRestoreTest'   # Lưu/khôi phục ván giữa chừng (cả 8 game)
./gradlew assembleRelease                   # APK release (app/build/outputs/apk/release)
./gradlew :core:engine:test :core:cards:test
./gradlew test                             # Toàn bộ test suite dự án
```

## Thêm game mới

1. Trong `:game:<tên>` (thêm `include` trong `settings.gradle.kts`) tạo `rules/` (RuleConfig `@Serializable` + luật thuần), `engine/` (implement `GameEngine`), `bot/` (implement `Bot` — chỉ dùng `viewOf`), `ui/` (ViewModel game loop + màn chơi, luật chơi, luật nhà, ván tập).
2. Lấy `:game:tienlen` làm mẫu: `TienLenEngine`, `TienLenBot`, `TienLenViewModel`, `TienLenTable`. Game có nhà cái/cược thì xem `:game:xidach`, `:game:lieng` (dùng `TableSession` cho ví, bot, chặn số dư và `SavedGameRepository.saveTable` để lưu ván dở; state engine phải `@Serializable`).
3. Đặt `available = true` trong `GameCatalog`, thêm nhánh trong `NavHost`, hệ số xu tối thiểu trong `TableSetupViewModel.minMultiplier` và thẻ hướng dẫn trong `QuickGuides`.
4. Viết test luật + test mô phỏng bot đấu bot (zero-sum) như `SimulationTest` + test lưu/khôi phục như `TienLenSaveRestoreTest`.

## Quyết định luật

Xem [docs/RULE_DECISIONS.md](docs/RULE_DECISIONS.md)

## Ghi chú

- Xu chỉ để giải trí, KHÔNG sử dụng tiền thật
- Không đổi thưởng, không mua bán xu
