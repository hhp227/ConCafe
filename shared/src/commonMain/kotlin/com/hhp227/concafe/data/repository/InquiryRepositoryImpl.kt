package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.InquiryCreate
import com.hhp227.concafe.domain.repository.InquiryRepository

class InquiryRepositoryImpl : InquiryRepository {
    override suspend fun createInquiry(
        userId: String,
        userNickname: String,
        input: InquiryCreate
    ): Inquiry {
        TODO("Not yet implemented")
    }
}