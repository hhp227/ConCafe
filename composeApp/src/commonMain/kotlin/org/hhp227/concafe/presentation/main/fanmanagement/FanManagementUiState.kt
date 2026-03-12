package org.hhp227.concafe.presentation.main.fanmanagement

data class FanManagementUiState(
    val title: String = "팬 관리",
    val castProfile: CastProfile = CastProfile(
        castId = "maid-1",
        cafeId = "cafe-1",
        stageName = "사쿠라",
        localizedName = "Sakura",
        cafeName = "메이드 하우스",
        profileAccent = "SK",
        isOnline = true
    ),
    val stats: List<StatCard> = listOf(
        StatCard(label = "전체 팔로워", value = "1,240", highlight = Highlight.DEFAULT),
        StatCard(label = "오늘 신규", value = "+12", highlight = Highlight.PRIMARY),
        StatCard(label = "소통 지수", value = "98.5", highlight = Highlight.DEFAULT)
    ),
    val recentFollowers: List<RecentFollower> = listOf(
        RecentFollower(id = "ken", name = "Ken", joinedLabel = "방금 전", accent = true),
        RecentFollower(id = "sophie", name = "Sophie", joinedLabel = "2시간 전"),
        RecentFollower(id = "minjun", name = "Minjun", joinedLabel = "5시간 전"),
        RecentFollower(id = "hana", name = "Hana", joinedLabel = "어제")
    ),
    val topFans: List<TopFan> = listOf(
        TopFan(id = "master-k", rank = 1, name = "마스터 K", pointsLabel = "42,500P", isBest = true),
        TopFan(id = "moonlight", rank = 2, name = "달빛나그네", pointsLabel = "38,200P"),
        TopFan(id = "melodyfan", rank = 3, name = "멜로디팬", pointsLabel = "31,900P")
    ),
    val notificationCount: Int = 1,
    val infoMessage: String? = "오늘 신규 팬 12명이 유입되었습니다."
) {
    data class CastProfile(
        val castId: String,
        val cafeId: String,
        val stageName: String,
        val localizedName: String,
        val cafeName: String,
        val profileAccent: String,
        val isOnline: Boolean
    )

    data class StatCard(
        val label: String,
        val value: String,
        val highlight: Highlight
    )

    data class RecentFollower(
        val id: String,
        val name: String,
        val joinedLabel: String,
        val accent: Boolean = false
    ) {
        val initial: String
            get() = name.take(1).uppercase()
    }

    data class TopFan(
        val id: String,
        val rank: Int,
        val name: String,
        val pointsLabel: String,
        val isBest: Boolean = false
    )

    enum class QuickAction(
        val title: String,
        val subtitle: String
    ) {
        WORK_SCHEDULE("출근 관리", "이번 주 스케줄을 조정합니다.")
    }

    enum class Highlight {
        DEFAULT,
        PRIMARY
    }
}
