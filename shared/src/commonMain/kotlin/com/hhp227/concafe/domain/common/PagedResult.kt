package com.hhp227.concafe.domain.common

data class PagedResult<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasNext: Boolean
)
