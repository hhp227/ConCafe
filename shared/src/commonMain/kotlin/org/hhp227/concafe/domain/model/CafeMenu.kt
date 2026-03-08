package org.hhp227.concafe.domain.model

data class CafeMenu(
    val id: String,
    val name: String,
    val price: Int,
    val description: String,
    val image: String?,
    val category: String
)
