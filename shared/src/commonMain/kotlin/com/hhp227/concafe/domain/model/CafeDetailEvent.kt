package com.hhp227.concafe.domain.model

sealed class CafeDetailEvent {
    data class CafeInfoUpdated(val cafeId: String) : CafeDetailEvent()

    data class MenuGoodsCreated(val cafeId: String, val itemId: String) : CafeDetailEvent()

    data class MenuGoodsUpdated(val cafeId: String, val itemId: String) : CafeDetailEvent()

    data class MenuGoodsDeleted(val cafeId: String, val itemId: String) : CafeDetailEvent()
}
