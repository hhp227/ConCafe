package com.hhp227.concafe.domain.model

enum class MainNavigationTab(
    val route: String
) {
    HOME("home"),
    EXPLORE("explore"),
    CHECK_IN("checkin"),
    FAN_MANAGEMENT("fanManagement"),
    CAFE_MANAGEMENT("cafeManagement"),
    ADMIN_OPERATIONS("adminOperations"),
    RANKING("ranking"),
    COMMUNITY("community"),
    MY_INFO("myinfo");

    companion object {
        fun fromRoute(route: String?): MainNavigationTab? {
            return entries.firstOrNull { it.route == route }
        }
    }
}
