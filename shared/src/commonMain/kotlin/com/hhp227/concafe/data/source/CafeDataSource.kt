package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeRegistrationClaim

interface CafeDataSource {
    val cafes: MutableList<Cafe>
    val cafeDetailsById: MutableMap<String, CafeDetail>
    val ownedCafeIdsByUser: MutableMap<String, MutableList<String>>
    val pendingCafeClaimsByUser: MutableMap<String, MutableList<CafeManagementData.PendingClaimSummary>>
    val pendingCafeRegistrationClaimsByUser: MutableMap<String, MutableList<CafeRegistrationClaim>>
    val cafeCheckInCountById: Map<String, Int>
    val cafeTodayCheckInCountById: Map<String, Int>
    val cafeTodayReviewCountById: Map<String, Int>
    val onShiftCastIdsByCafeId: Map<String, Set<String>>
    val cafeHomeBannerPreviewByCafeId: MutableMap<String, CafeDashboardData.HomeBannerPreview>
    fun cafeDetail(cafeId: String): CafeDetail?
    fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail
    fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail
    fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail
}