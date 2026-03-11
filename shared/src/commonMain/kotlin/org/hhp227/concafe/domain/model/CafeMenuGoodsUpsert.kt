package org.hhp227.concafe.domain.model

data class CafeMenuGoodsUpsert(
    val cafeId: String,
    val itemId: String? = null,
    val name: String,
    val price: Int,
    val category: String,
    val description: String,
    val isInStock: Boolean,
    val imageUrl: String? = null
)
