package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.Inquiry

interface InquiryDataSource {
    val inquiries: MutableList<Inquiry>
}