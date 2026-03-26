package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.Goods

sealed class CafeDetailEvent {
    data class CafeInfoUpdated(val cafeId: String, val cafe: Cafe) : CafeDetailEvent()
    data class FavoriteToggled(val cafeId: String, val isFavorite: Boolean) : CafeDetailEvent()

    data class MenuCreated(
        val cafeId: String,
        val menu: CafeMenu
    ) : CafeDetailEvent()

    data class MenuUpdated(
        val cafeId: String,
        val menu: CafeMenu
    ) : CafeDetailEvent()

    data class MenuDeleted(val cafeId: String, val itemId: String) : CafeDetailEvent()

    data class GoodsCreated(
        val cafeId: String,
        val goods: Goods
    ) : CafeDetailEvent()

    data class GoodsUpdated(
        val cafeId: String,
        val goods: Goods
    ) : CafeDetailEvent()

    data class GoodsDeleted(val cafeId: String, val itemId: String) : CafeDetailEvent()
}
