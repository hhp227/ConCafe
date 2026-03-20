package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult

interface PagingDataSource {
    fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T>
}