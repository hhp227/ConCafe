package com.hhp227.concafe.domain.model

data class Report(
    val id: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val reporterUserId: String,
    val reporterNickname: String,
    val reportType: String,
    val status: ReportStatus,
    val createdAt: String,
    val createdAtLabel: String
)

data class ReportCreate(
    val targetType: ReportTargetType,
    val targetId: String,
    val reportType: String
)

enum class ReportTargetType {
    COMMUNITY_POST,
    COMMUNITY_COMMENT
}

enum class ReportStatus {
    PENDING,
    RESOLVED
}
