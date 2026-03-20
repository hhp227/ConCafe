package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.MyPageSummary

interface MyInfoDataSource {
    val dismissedReviewPromptVisitIdsByUser: MutableMap<String, MutableSet<String>>
    fun defaultMyPageSummary(userId: String): MyPageSummary
}