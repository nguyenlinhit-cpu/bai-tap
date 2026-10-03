package com.baiviet.core.engine

/**
 * Marker interface cho cấu hình luật mỗi game.
 *
 * Mỗi game có data class `@Serializable` riêng implement interface này,
 * với giá trị mặc định cho tất cả các field.
 * Ví dụ: [TienLenRules], [SamLocRules], [PhomRules]…
 */
interface RuleConfig
