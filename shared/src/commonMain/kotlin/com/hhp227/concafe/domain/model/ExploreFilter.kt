package com.hhp227.concafe.domain.model

data class ExploreRegionFilter(
    val key: String,
    val country: String?,
    val city: String?
) {
    companion object {
        fun from(key: String): ExploreRegionFilter {
            return when (key.lowercase()) {
                "all", "전체" -> ExploreRegionFilter(key = "all", country = null, city = null)
                "seoul", "서울" -> ExploreRegionFilter(key = "seoul", country = "KR", city = "Seoul")
                "busan", "부산" -> ExploreRegionFilter(key = "busan", country = "KR", city = "Busan")
                "daegu", "대구" -> ExploreRegionFilter(key = "daegu", country = "KR", city = "Daegu")
                "tokyo", "도쿄" -> ExploreRegionFilter(key = "tokyo", country = "JP", city = "Tokyo")
                "osaka", "오사카" -> ExploreRegionFilter(key = "osaka", country = "JP", city = "Osaka")
                else -> throw IllegalArgumentException("unsupported region: $key")
            }
        }
    }
}

data class ExploreSortFilter(
    val key: String,
    val cafeSort: CafeSort,
    val castSort: CastSort
) {
    companion object {
        fun from(key: String): ExploreSortFilter {
            return when (key.lowercase()) {
                "popular", "인기순" -> ExploreSortFilter(
                    key = "popular",
                    cafeSort = CafeSort.POPULAR,
                    castSort = CastSort.POPULAR
                )

                "latest", "최신순" -> ExploreSortFilter(
                    key = "latest",
                    cafeSort = CafeSort.LATEST,
                    castSort = CastSort.LATEST
                )

                "rating", "평점순" -> ExploreSortFilter(
                    key = "rating",
                    cafeSort = CafeSort.RATING,
                    castSort = CastSort.FOLLOWERS
                )

                else -> throw IllegalArgumentException("unsupported sort: $key")
            }
        }
    }
}
