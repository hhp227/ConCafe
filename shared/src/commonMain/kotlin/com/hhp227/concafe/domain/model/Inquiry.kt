package com.hhp227.concafe.domain.model

data class Inquiry(
    val id: String,
    val userId: String,
    val userNickname: String,
    val inquiryType: String,
    val title: String,
    val content: String,
    val status: InquiryStatus,
    val createdAt: String,
    val createdAtLabel: String
)

data class InquiryCreate(
    val inquiryType: String,
    val title: String,
    val content: String
)

enum class InquiryStatus {
    PENDING,
    ANSWERED
}
