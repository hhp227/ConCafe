package org.hhp227.concafe.presentation.main.cafemanagement

data class CafeManagementUiState(
    val ownedCafes: List<OwnedCafe> = emptyList(),
    val searchableCafes: List<SearchableCafe> = emptyList(),
    val pendingClaims: List<PendingClaim> = emptyList(),
    val isShowingAllCafes: Boolean = false,
    val cafeSearchQuery: String = "",
    val infoMessage: String? = null
) {
    val featuredCafe: OwnedCafe?
        get() = ownedCafes.firstOrNull()

    val hasOwnedCafes: Boolean
        get() = ownedCafes.isNotEmpty()

    val visibleOwnedCafes: List<OwnedCafe>
        get() = if (isShowingAllCafes) ownedCafes else ownedCafes.take(DEFAULT_VISIBLE_CAFE_COUNT)

    val hasHiddenOwnedCafes: Boolean
        get() = ownedCafes.size > DEFAULT_VISIBLE_CAFE_COUNT

    val filteredSearchableCafes: List<SearchableCafe>
        get() = searchableCafes.filter {
            cafeSearchQuery.isBlank() ||
                it.name.contains(cafeSearchQuery, ignoreCase = true) ||
                it.location.contains(cafeSearchQuery, ignoreCase = true)
        }

    data class OwnedCafe(
        val id: String,
        val name: String,
        val city: String,
        val isApproved: Boolean,
        val todayVisitors: Int,
        val todayCheckIns: Int,
        val todayReviews: Int,
        val rating: Double,
        val castCount: Int,
        val noticeCount: Int,
        val externalLinkCount: Int
    )

    data class PendingClaim(
        val cafeName: String,
        val requestedAt: String,
        val status: String,
        val message: String
    )

    data class SearchableCafe(
        val id: String,
        val name: String,
        val location: String
    )

    companion object {
        private const val DEFAULT_VISIBLE_CAFE_COUNT = 2

        fun preview() = CafeManagementUiState(
            ownedCafes = listOf(
                OwnedCafe(
                    id = "cafe-1",
                    name = "Maid Dream Tokyo",
                    city = "Tokyo",
                    isApproved = true,
                    todayVisitors = 18,
                    todayCheckIns = 12,
                    todayReviews = 3,
                    rating = 4.7,
                    castCount = 8,
                    noticeCount = 2,
                    externalLinkCount = 4
                ),
                OwnedCafe(
                    id = "cafe-2",
                    name = "Seoul Maid Cafe",
                    city = "Seoul",
                    isApproved = true,
                    todayVisitors = 11,
                    todayCheckIns = 7,
                    todayReviews = 1,
                    rating = 4.5,
                    castCount = 5,
                    noticeCount = 1,
                    externalLinkCount = 3
                ),
                OwnedCafe(
                    id = "cafe-3",
                    name = "Akihabara Butler Cafe",
                    city = "Tokyo",
                    isApproved = false,
                    todayVisitors = 0,
                    todayCheckIns = 0,
                    todayReviews = 0,
                    rating = 0.0,
                    castCount = 3,
                    noticeCount = 0,
                    externalLinkCount = 1
                )
            ),
            searchableCafes = listOf(
                SearchableCafe("cafe-1", "Maid Dream Tokyo", "Tokyo Akihabara"),
                SearchableCafe("cafe-2", "Seoul Maid Cafe", "서울 마포구 연남동"),
                SearchableCafe("cafe-3", "Ribbon Cafe Hongdae", "서울 마포구 홍대입구"),
                SearchableCafe("cafe-4", "Akihabara Butler Cafe", "Tokyo Chiyoda")
            ),
            pendingClaims = listOf(
                PendingClaim(
                    cafeName = "Ribbon Cafe Hongdae",
                    requestedAt = "2026.03.10",
                    status = "승인 대기 중",
                    message = "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
                )
            )
        )

        fun emptyPreview() = CafeManagementUiState(
            searchableCafes = listOf(
                SearchableCafe("cafe-1", "Maid Dream Tokyo", "Tokyo Akihabara"),
                SearchableCafe("cafe-2", "Seoul Maid Cafe", "서울 마포구 연남동"),
                SearchableCafe("cafe-3", "Ribbon Cafe Hongdae", "서울 마포구 홍대입구"),
                SearchableCafe("cafe-4", "Akihabara Butler Cafe", "Tokyo Chiyoda")
            ),
            pendingClaims = listOf(
                PendingClaim(
                    cafeName = "Pink Castle Sinchon",
                    requestedAt = "2026.03.09",
                    status = "승인 대기 중",
                    message = "기존 카페 운영자 신청이 검토 중입니다"
                )
            )
        )
    }
}
