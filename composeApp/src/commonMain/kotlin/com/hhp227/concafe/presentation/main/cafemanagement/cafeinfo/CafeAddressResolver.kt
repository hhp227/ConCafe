package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

data class CafeResolvedAddress(
    val latitude: Double,
    val longitude: Double,
    val fullAddress: String
)

expect suspend fun resolveCafeAddress(query: String): CafeResolvedAddress?
