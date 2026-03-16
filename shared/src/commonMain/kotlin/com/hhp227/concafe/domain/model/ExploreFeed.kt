package com.hhp227.concafe.domain.model

data class ExploreFeed(
    val cafes: List<Cafe>,
    val cafesNextCursor: String?,
    val hasMoreCafes: Boolean,
    val maids: List<Cast>,
    val maidsNextCursor: String?,
    val hasMoreMaids: Boolean
)
