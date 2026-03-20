package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult

interface VisitDataSource {
    val visits: MutableList<Visit>
    fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult
}