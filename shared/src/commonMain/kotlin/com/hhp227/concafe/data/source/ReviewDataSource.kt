package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.Review

interface ReviewDataSource {
    val reviews: MutableList<Review>
    fun refreshReviewProjections(cafeId: String, taggedCastIds: List<String>)
}