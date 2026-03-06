package org.hhp227.concafe.domain.model

data class Goods(
    val id: String,
    val name: String,
    val price: Int,
    val image: String?,
    val stock: Int
)
