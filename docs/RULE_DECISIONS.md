# Quyết định luật chơi

Ghi chú các điểm luật chưa rõ ràng và cách giải quyết.
Xem thêm tùy chọn Luật nhà trong từng game.

## Tiến lên miền Nam

Mặc định theo đặc tả mục 5.1. Mọi con số nằm trong `TienLenRules` (game/tienlen/rules).

| # | Điểm luật | Quyết định | Lý do | Luật nhà? |
|---|-----------|-----------|-------|-----------|
| 1 | 4 đôi thông chặt không cần vòng | Bật mặc định. Người đã bỏ lượt nhưng có 4 đôi thông chặt được bài trên bàn sẽ được hỏi khi lượt đi qua ghế của họ (không ngắt lượt người khác). Đã từ chối thì không bị hỏi lại với cùng bộ bài trên bàn | Phổ biến ở miền Nam; giữ thứ tự lượt rõ ràng, không lộ bài khi người khác chưa đánh | ✅ |
| 2 | Tứ quý 3 ván đầu tới trắng | Bật mặc định | Phổ biến trong game online | ✅ |
| 3 | Cấm về bằng heo | Tắt mặc định. Khi bật: nước đánh làm hết bài mà có lá 2 là không hợp lệ; ngoại lệ duy nhất là đang đi tự do và chỉ còn đúng 1 lá (heo) — bắt buộc phải đánh | Tránh kẹt ván vô hạn | ✅ |
| 4 | Tứ quý chặt đôi heo | Bật mặc định | Theo đặc tả | ✅ |
| 5 | "Chặt" có tính tiền | Mọi nước dùng hàng (3+ đôi thông, tứ quý) đánh lên heo hoặc lên hàng, kể cả hàng cùng loại lớn hơn (3 đôi thông lớn chặt 3 đôi thông nhỏ) | Theo bảng hàng chặt ở đặc tả | — |
| 6 | Chặt chồng | Mỗi lần chặt chồng, tiền = tiền chuỗi trước + giá trị bộ vừa bị chặt; người bị chặt cuối trả hết cho người chặt cuối | Theo đặc tả | — |
| 7 | Thời điểm trả tiền chặt | Chốt khi chuỗi chặt kết thúc (hết vòng hoặc hết ván), cộng vào bảng thanh toán cuối ván; banner hiện ngay lúc chặt | Bảo đảm zero-sum và chặn theo số dư trong một lần thanh toán | — |
| 8 | Đôi thông ≥ 5 đôi (khi tắt 5 đôi thông tới trắng) | Coi như 4 đôi thông: chặt mọi thứ 4 đôi thông chặt được, và chặt đôi thông ≥ 4 ngắn hơn; giá trị bị chặt/thối = 4 đôi thông | Không có luật riêng, chọn cách hiểu tự nhiên | — |
| 9 | Thối hàng | Hàng còn trên tay tính THAY cho các lá tạo nên nó (3 đôi thông = 9 lá chứ không phải 9 + 6). Tách tứ quý trước, sau đó chia dãy đôi liên tiếp thành 3/4 đôi thông sao cho tổng phạt lớn nhất | Đặc tả ghi "tính thay cho 1 lá thường" | — |
| 10 | Người ván đầu khi không ai có 3♠ (2–3 người) | Người giữ lá nhỏ nhất đi trước và phải đánh lá đó (nếu bật luật 3♠) | Theo đặc tả | ✅ (chung với luật 3♠) |
| 11 | 6 đôi tới trắng | Đếm số đôi rời nhau: tứ quý = 2 đôi, sám = 1 đôi, đôi heo được tính | Cách hiểu phổ biến | — |
| 12 | Nhiều người cùng tới trắng, cùng loại | So lá lớn nhất trên tay (theo thứ tự Tiến lên) | Đặc tả: "so lá lớn nhất" | — |
| 13 | Đếm lá: ván kết thúc khi người đầu tiên về | Kể cả khi lá cuối là heo — không ai được chặt lá về | Đơn giản, đúng đặc tả | — |
| 14 | Xếp hạng: cóng | Người cóng bị loại ngay khi có người về Nhất, xếp cuối; trả 2B + thối cho người Nhất thay cho tiền theo hạng | Đặc tả: "cóng 2B + thối" | ✅ (chế độ) |
| 15 | Xếp hạng: ghép cặp trả tiền | Người hạng thấp nhất trả người hạng cao nhất trước (Bét → Nhất, Ba → Nhì) | Khớp bảng +2/+1/−1/−2 | — |
| 16 | Xếp hạng: thối | Thối của người Bét trả cho người Nhất | Phổ biến | — |
| 17 | Thoát giữa ván | Xử như về cuối: Đếm lá = trả số lá còn lại (hoặc tiền cóng nếu chưa đánh) + thối; Xếp hạng = tiền Bét + thối. Người nhận là đối thủ còn ít bài nhất | Đặc tả mục 4 | — |
| 18 | Không mất quá số xu đang có | Duyệt các khoản theo thứ tự (chặt trước, cuối ván sau), mỗi khoản chỉ trả tối đa phần xu còn lại của người trả | Zero-sum, công bằng theo thứ tự phát sinh | — |
| 19 | Hết giờ | Bỏ lượt; nếu đang đi tự do thì đánh lá nhỏ nhất (hoặc lá bắt buộc 3♠) | Đặc tả mục 4 | — |
| 20 | Số xu tối thiểu vào bàn | 30 × B (`minBalanceMultiplier`) — đủ cho trường hợp thua nặng thường gặp (cóng 26 lá + thối) | Đặt trong RuleConfig | — |

## Sâm lốc

| # | Điểm luật | Quyết định | Lý do | Luật nhà? |
|---|-----------|-----------|-------|-----------|
| 1 | Tứ quý chặt đôi heo | Tắt mặc định | Biến thể ít phổ biến | ✅ |
| 2 | 3 sám ăn trắng | Tắt mặc định | Biến thể vùng miền | ✅ |
| 3 | Phân biệt heo đen 10 / đỏ 15 | Tắt mặc định (mặc định mọi heo 15 lá) | Theo đặc tả mục 5.2 | ✅ |
| 4 | Báo 1 và đền làng | Người ngồi ngay trước khi đánh lẻ phải đánh lá lớn nhất có thể. Nếu đánh lá nhỏ hơn và người báo 1 về ngay -> đền làng (gánh toàn bộ tiền thua của cả làng) | Theo đặc tả mục 5.2 & 8 | — |
| 5 | Về bằng heo | Nước cuối cùng có chứa lá 2 -> bị xử thối 2: không được tính thắng, trả mỗi người 15 lá, ván dừng ngay | Theo đặc tả mục 5.2 & 8 | — |
| 6 | Sảnh Sâm Lốc | Dài ≥ 3 lá; cho phép A-2-3 (nhỏ nhất) và 2-3-4, Q-K-A (lớn nhất); không được vòng qua 2 (K-A-2 sai). Không có đôi thông. So sảnh cùng độ dài theo lá cuối | Theo đặc tả mục 5.2 & 8 | — |
| 7 | Không so chất | Hai bộ cùng giá trị số không thể chặn nhau (kể cả rác, đôi, sám, tứ quý, sảnh) | Theo đặc tả mục 5.2 & 8 | — |
| 8 | Pha Báo Sâm | 8s mỗi người sau khi chia; chỉ người báo đầu tiên được nhận. Bị chặn dù 1 nước -> Đền Sâm 20 lá/người; đánh hết 10 lá không ai chặn -> Ăn Sâm 20 lá/người | Theo đặc tả mục 5.2 | — |


## Phỏm

Mặc định theo đặc tả mục 5.3, 7, 8 và 10. Mọi con số nằm trong `PhomRules` (game/phom/rules).

| # | Điểm luật | Quyết định | Lý do | Luật nhà? |
|---|-----------|-----------|-------|-----------|
| 1 | Giá trị điểm & Phỏm dọc | A = 1, 2–10 = số, J = 11, Q = 12, K = 13. Phỏm dọc A-2-3 hợp lệ (A là quân nhỏ nhất). Dãy Q-K-A và K-A-2 KHÔNG hợp lệ | Chuẩn luật Phỏm miền Bắc truyền thống | — |
| 2 | Ràng buộc cây ăn | Mỗi phỏm chỉ chứa tối đa 1 cây ăn. Cây đã ăn và các quân cạ/phỏm liên kết chặt với cây ăn không được phép đánh rác | Đặc tả mục 5.3 & ngăn gian lận đánh lá đã ăn | — |
| 3 | Ăn cây chốt | Vòng thứ 4 là cây chốt. Ăn chốt phạt 4B (người bị ăn trả trực tiếp cho người ăn) | Đặc tả mục 5.3 & chuẩn luật tá lả | — |
| 4 | Ù và Ù khan | Ù (tất cả bài trong phỏm, rác ≤ 1 lá) ăn 5B mỗi người. Ù khan (chia bài không có đôi/cạ nào) ăn như Ù | Bật mặc định; phổ biến trong game online | ✅ |
| 5 | Ù đền làng | 3 trường hợp: (1) Bị ăn 3 cây dẫn đến Ù; (2) Đánh cây chốt để người sau ăn Ù; (3) Ăn chốt rồi đánh chốt để người sau ăn Ù (đền chuyền). Người đền trả toàn bộ tiền thay cả bàn | Đặc tả mục 5.3 & bảo đảm công bằng, tránh thông đồng | ✅ |
| 6 | Xếp hạng & Phá hòa điểm | Ù > Điểm rác tăng dần > Móm (cháy). Hòa điểm: người hạ phỏm trước xếp trên (ưu tiên thứ tự hạ) | Chuẩn luật thi đấu Phỏm | — |
| 7 | Gửi bài (Lay-off) | Chỉ người hạ phỏm thành công mới được gửi bài vào phỏm của người khác. Gửi khi móm tắt mặc định | Đặc tả mục 5.3 | ✅ |
| 8 | Phạt ăn cây lũy tiến | Cây 1: 1B, Cây 2: 2B, Cây 3: 3B. Cây chốt: 4B | Chuẩn luật miền Bắc | ✅ |

## Mậu binh

| # | Điểm luật | Quyết định | Lý do | Luật nhà? |
|---|-----------|-----------|-------|-----------|
| 1 | A-2-3-4-5 trong sảnh | Lớn thứ hai (sau 10-J-Q-K-A) | Phổ biến nhất | ✅ (có thể chuyển sang nhỏ nhất) |
| 2 | So chất | Tuyệt đối không so chất | Chuẩn luật Mậu Binh | ❌ |
| 3 | Chi 3 | Chỉ tính Sám cô > Đôi > Mậu thầu (không tính sảnh/thùng 3 lá) | Chuẩn luật 3 chi | ❌ |
| 4 | Sập 3 chi | Thắng cả 3 chi với 1 người -> nhân đôi chi với người đó (×2) | Luật Mậu Binh chuẩn | ✅ |
| 5 | Sập làng | Sập 3 chi với tất cả người chơi trong bàn (>= 3 người) -> nhân đôi thêm (×4) | Thưởng bài áp đảo | ✅ |
| 6 | Binh lủng | Thua mỗi người không lủng 6 chi; hai người cùng lủng hòa nhau | Phạt lỗi xếp bài | ✅ |
| 7 | Chi thưởng | Sám chi 3 (3 chi), Cù lũ chi 2 (2 chi), Tứ quý chi 1 (4 chi), Tứ quý chi 2 (8 chi), Thùng phá sảnh chi 1 (5 chi), Thùng phá sảnh chi 2 (10 chi) | Chuẩn luật chi thưởng | ❌ |
| 8 | Mậu binh tới trắng | Rồng cuốn (24 chi), Sảnh rồng (12 chi), 5 đôi 1 sám (3 chi), Lục phé bôn (3 chi), Ba thùng (3 chi), Ba sảnh (3 chi) | Thứ bậc tới trắng | ❌ |
| 9 | Thời gian xếp bài | 60 giây, hết giờ tự động xếp phương án tối ưu hợp lệ | Đảm bảo tốc độ ván | ✅ |

## Xì dách

| # | Điểm luật | Quyết định | Lý do | Luật nhà? |
|---|-----------|-----------|-------|-----------|
| 1 | Cả hai cùng quắc | Hòa | Phổ biến nhất | ✅ (con quắc luôn thua) |
| 2 | Tính điểm lá Át (A) | 2 lá tính 10/11; 3 lá tính 1/10; 4–5 lá tính 1 | Chuẩn luật Xì dách Việt Nam, tối ưu điểm ≤ 21 | ❌ |
| 3 | Bài đặc biệt | Xì bàng (2 Át) > Xì dách (Át + Tây/10) > Ngũ linh (5 lá ≤ 21) > Điểm thường (21 > ... > 16) | Chuẩn luật thứ bậc | ❌ |
| 4 | Ngũ linh đối đầu | Người ít điểm hơn thắng (ví dụ 15 thắng 18) | Thưởng bài nhiều lá tổng điểm thấp | ❌ |
| 5 | Mốc đủ tuổi | Con: ≥ 16 mới được Dằn; Cái: ≥ 15 mới được Xét | Chuẩn luật dân gian | ❌ |
| 6 | Kiểm tra đầu ván | Cái có Xì bàng/Xì dách xét cả bàn; Con có lật thắng cái ngay | Tối ưu nhịp ván đấu | ❌ |
| 7 | Chuyển vai trò cái | Giữ nguyên theo chế độ chọn bàn | Trải nghiệm ổn định | ✅ (ai có Xì bàng/Xì dách làm cái ván sau) |
| 8 | Tỷ lệ trả thưởng | Xì bàng 3×, Xì dách 2×, Ngũ linh 2×, Thắng thường 1× | Chuẩn luật trả thưởng | ❌ |

## Poker Texas

_(Theo chuẩn quốc tế, không có biến thể)_

## Liêng

| # | Điểm luật | Quyết định | Lý do | Luật nhà? |
|---|-----------|-----------|-------|-----------|
| 1 | So chất: Rô > Cơ | Mặc định | Phổ biến nhất | ✅ (Cơ > Rô) |

## Ba cây

| # | Điểm luật | Quyết định | Lý do | Luật nhà? |
|---|-----------|-----------|-------|-----------|
| 1 | Tính điểm lá bài | A = 1, 2–9 theo số, 10/J/Q/K = 0; tổng chia 10 lấy dư; 9 nút cao nhất, 0 nút là bù | Chuẩn bài cào 3 lá | ❌ |
| 2 | Ba tiên (Ba cào) | 3 lá J, Q, K lớn hơn 9 nút | Bài đặc biệt chuẩn | ❌ |
| 3 | Sáp đứng trên Ba tiên | Tắt mặc định (Ba tiên > Sáp) | Biến thể ít phổ biến | ✅ (có thể bật Sáp > Ba tiên) |
| 4 | Ba tiên ăn gấp đôi | Tắt mặc định (ăn 1×) | Cân bằng kinh tế bàn chơi | ✅ (có thể bật ăn 2×) |
| 5 | Bằng điểm so lá mạnh nhất | ♦ Rô > ♥ Cơ > ♣ Chuồn > ♠ Bích; cùng chất so số A > K > ... > 2 (Át Rô mạnh nhất) | Chuẩn cào miền Nam | ✅ (có thể bật bằng điểm thì hòa) |
| 6 | Chế độ chơi | Có cái (mặc định) và Nhất ăn tất | Đa dạng hóa thể thức cào 3 lá | ✅ |

## Hệ thống chung (Giai đoạn 8)

| # | Điểm | Quyết định | Lý do |
|---|------|-----------|-------|
| 1 | Lưu ván dở | Sau mỗi nước đi, bàn (tham số, tên + xu bot, ghế xoay vòng) và trạng thái engine được lưu JSON vào Room. App bị tắt/vào nền rồi mở lại → Sảnh hiện nhãn "Ván dở", màn Chọn bàn có nút **Chơi tiếp** đúng chỗ đang dừng | Đặc tả mục 4 & 9: không mất ván khi bị tắt |
| 2 | Bỏ ván dở | Chỉ được "Mở bàn mới" khi đang nghỉ giữa hai ván. Đang giữa ván thì phải chơi tiếp hoặc vào bàn và thoát (bị xử thua) | Không cho né thua bằng cách tắt app |
| 3 | Thoát giữa ván | Tiến lên: như về cuối (luật cũ). Sâm lốc: xử cóng — trả mỗi đối thủ (cóng + heo mặc định) lá. Phỏm: xử móm — trả tiền móm cho người đang ít điểm rác nhất. Mậu binh: xử binh lủng — trả mỗi đối thủ 6 chi. Xì dách / Ba cây: làm con mất tiền cược cho cái; làm cái trả cược cho các nhà chưa xét (Ba cây ăn tất: trả người bài cao nhất). Liêng / Poker: úp bài, mất phần đã bỏ vào pot. Mọi khoản đều chặn theo số dư | Đặc tả mục 4: "xử thua như người về cuối" |
| 4 | Vai trò nhà cái (Xì dách, Ba cây) | Chọn ở màn Chọn bàn: Bạn làm cái / Máy làm cái / Luân phiên. Luân phiên: cái chuyển sang ghế kế tiếp mỗi ván (Xì dách bật luật nhà "Xì bàng/Xì dách làm cái" thì người thắng bằng bài đó làm cái) | Đặc tả mục 3.2 |
| 5 | Buy-in Poker | Chọn 50 / 100 / 200 BB ở màn Chọn bàn (mù lớn = mức cược). Số xu tối thiểu vào bàn = buy-in | Đặc tả mục 3.2 |
| 6 | Số xu tối thiểu vào bàn | Lấy `minBalanceMultiplier` của từng game (Ba cây 20B, Poker = buy-in, các game khác 30B) | Trước đây màn Chọn bàn dùng 30B cho mọi game |
| 7 | Thành tích | Khai trương (thắng 1), Cao thủ nhập môn (10), Cao thủ (50), Chăm chỉ (100 ván), Trúng lớn (≥ 20.000 xu/ván), Đa tài (chơi đủ 8 game), Đại gia (≥ 1.000.000 xu). Cấp độ hồ sơ = 1 + số ván / 10 | Đơn giản, dễ hiểu |
| 8 | Âm thanh | Tổng hợp bằng AudioTrack (không dùng file): chia bài, lật bài, đánh bài, chip, thắng, thua, sự kiện lớn, tích đồng hồ 5 giây cuối; nhạc nền ngũ cung lặp, dừng khi app vào nền. Tắt/bật trong Cài đặt; rung cũng theo Cài đặt | Không cần quyền, không cần mạng, APK nhẹ |
