package com.hhp227.concafe.data.source

interface MyInfoRemoteDataSource {
    suspend fun isReviewPromptDismissed(userId: String, visitId: String): Boolean

    suspend fun dismissReviewPrompt(userId: String, visitId: String)
}
