package com.hhp227.concafe.domain.policy

import com.hhp227.concafe.domain.model.MainNavigationTab
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole

class MainNavigationPolicy {
    fun resolveThirdTab(user: User?): MainNavigationTab {
        return when (user?.role) {
            UserRole.ADMIN -> MainNavigationTab.ADMIN_OPERATIONS
            UserRole.CAFE_OWNER -> MainNavigationTab.CAFE_MANAGEMENT
            UserRole.CAST -> MainNavigationTab.FAN_MANAGEMENT
            UserRole.VISITOR,
            null -> MainNavigationTab.CHECK_IN
        }
    }

    fun resolveMainTabs(user: User?): List<MainNavigationTab> {
        return listOf(
            MainNavigationTab.HOME,
            MainNavigationTab.EXPLORE,
            resolveThirdTab(user),
            MainNavigationTab.RANKING,
            MainNavigationTab.MY_INFO
        )
    }

    fun normalizeMainTab(route: String?, user: User?): String {
        val availableRoutes = resolveMainTabs(user).map { it.route } + MainNavigationTab.COMMUNITY.route
        val thirdTabRoutes = setOf(
            MainNavigationTab.CHECK_IN.route,
            MainNavigationTab.FAN_MANAGEMENT.route,
            MainNavigationTab.CAFE_MANAGEMENT.route,
            MainNavigationTab.ADMIN_OPERATIONS.route
        )

        return when {
            route == null -> MainNavigationTab.HOME.route
            thirdTabRoutes.contains(route) -> resolveThirdTab(user).route
            availableRoutes.contains(route) -> route
            else -> MainNavigationTab.HOME.route
        }
    }

    fun resolveThirdTabRoute(user: User?): String {
        return resolveThirdTab(user).route
    }

    fun resolveMainRoutes(user: User?): List<String> {
        return resolveMainTabs(user).map { it.route }
    }

    fun normalizeMainTabRoute(route: String?, user: User?): String {
        return normalizeMainTab(route, user)
    }
}
