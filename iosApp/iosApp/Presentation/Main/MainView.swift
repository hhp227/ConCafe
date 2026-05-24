//
//  MainView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import UIKit
import Shared

struct MainView: View {
    let initialTab: String?

    let hasUnreadNotifications: Bool

    @StateObject private var viewModel = MainViewModel()

    let onNavigationAction: (NavigationAction) -> Void

    @State private var selectedTab: String = ""

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $selectedTab) {
                HomeView(onNavigationAction: handleHomeNavigationAction)
                    .tabItem { Label(String(localized: String.LocalizationValue("main_tab_home"), table: "Localizable"), systemImage: "house.fill") }
                    .tag("home")
                ExploreView(onNavigationAction: onNavigationAction)
                    .tabItem { Label(String(localized: String.LocalizationValue("main_tab_explore"), table: "Localizable"), systemImage: "magnifyingglass") }
                    .tag("explore")
                roleBasedThirdTabView
                    .tag(viewModel.uiState.thirdTab.route)
                rankingTabView
                    .tag("ranking")
                CommunityView(onNavigationAction: onNavigationAction)
                    .tag(MainNavigationTab.community.route)
                MyInfoView(onNavigationAction: onNavigationAction)
                    .tabItem { Label(String(localized: String.LocalizationValue("main_tab_my_info"), table: "Localizable"), systemImage: "person") }
                    .tag("myinfo")
            }
        }
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(selectedTab == MainNavigationTab.community.route ? .large : .inline)
        .compatOpaqueNavigationBarBackground()
        .toolbar {
            ToolbarItem(placement: .principal) {
                if selectedTab != MainNavigationTab.community.route {
                    ConCafeLogo()
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    if selectedTab == "myinfo" && viewModel.uiState.currentUser != nil {
                        onNavigationAction(.navigateToSettings)
                    } else {
                        onNavigationAction(.navigateToNotification)
                    }
                } label: {
                    if selectedTab == "myinfo" && viewModel.uiState.currentUser != nil {
                        Image(systemName: "gearshape")
                            .accessibilityLabel(String(localized: String.LocalizationValue("common_settings"), table: "Localizable"))
                    } else {
                        // [변경] contentViewModel.uiState.hasUnreadNotifications → hasUnreadNotifications 파라미터 직접 사용
                        Image(systemName: "bell")
                            .overlay(alignment: .topTrailing) {
                                if hasUnreadNotifications {
                                    Circle()
                                        .fill(Color.red)
                                        .frame(width: 8, height: 8)
                                        .offset(x: 2, y: -2)
                                }
                            }
                            .accessibilityLabel(String(localized: String.LocalizationValue("common_notification"), table: "Localizable"))
                    }
                }
            }
        }
        .onAppear {
            if let initialTab = initialTab, !initialTab.isEmpty {
                selectedTab = initialTab
                viewModel.onAction(.selectTab(route: initialTab))
            } else {
                selectedTab = viewModel.uiState.selectedTab
            }
            onNavigationAction(.refreshUnreadNotificationCount)
        }
        .onChange(of: viewModel.uiState.selectedTab) { newValue in
            if selectedTab != newValue {
                selectedTab = newValue
            }
        }
        .onChange(of: selectedTab) { newValue in
            if viewModel.uiState.selectedTab != newValue {
                viewModel.onAction(.selectTab(route: newValue))
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .showError:
                break
            case .navigateToSignUp:
                onNavigationAction(.navigateToSignUp)
            }
        }
    }

    private func handleHomeNavigationAction(_ action: NavigationAction) {
        switch action {
        case .navigateToCommunity:
            selectedTab = MainNavigationTab.community.route
            viewModel.onAction(.selectTab(route: MainNavigationTab.community.route))
        default:
            onNavigationAction(action)
        }
    }

    @ViewBuilder
    private var roleBasedThirdTabView: some View {
        switch viewModel.uiState.thirdTab {
        case .fanManagement:
            FanManagementView(onNavigationAction: onNavigationAction)
                .tabItem { Label(String(localized: String.LocalizationValue("main_tab_fan_management"), table: "Localizable"), systemImage: "person.2.fill") }
        case .cafeManagement:
            CafeManagementView(onNavigationAction: onNavigationAction)
                .tabItem { Label(String(localized: String.LocalizationValue("main_tab_cafe_management"), table: "Localizable"), systemImage: "storefront.fill") }
        case .adminOperations:
            AdminOperationsView(onNavigationAction: onNavigationAction)
                .tabItem { Label(String(localized: String.LocalizationValue("main_tab_admin_operations"), table: "Localizable"), systemImage: "shield.lefthalf.filled") }
        default:
            CheckInView(onNavigationAction: onNavigationAction)
                .tabItem { Label(String(localized: String.LocalizationValue("main_tab_checkin"), table: "Localizable"), systemImage: "checkmark.seal.fill") }
        }
    }

    @ViewBuilder
    private var rankingTabView: some View {
        RankingView(onNavigationAction: onNavigationAction)
            .tabItem {
                Label(
                    String(localized: String.LocalizationValue("main_tab_ranking"), table: "Localizable"),
                    systemImage: compatSystemImageName(iOS16: "trophy.fill", fallback: "star.fill")
                )
            }
    }

    private var navigationTitle: String {
        switch selectedTab {
        case MainNavigationTab.home.route:
            return String(localized: String.LocalizationValue("main_tab_home"), table: "Localizable")
        case MainNavigationTab.explore.route:
            return String(localized: String.LocalizationValue("main_tab_explore"), table: "Localizable")
        case MainNavigationTab.checkIn.route:
            return String(localized: String.LocalizationValue("main_tab_checkin"), table: "Localizable")
        case MainNavigationTab.fanManagement.route:
            return String(localized: String.LocalizationValue("main_tab_fan_management"), table: "Localizable")
        case MainNavigationTab.cafeManagement.route:
            return String(localized: String.LocalizationValue("main_tab_cafe_management"), table: "Localizable")
        case MainNavigationTab.adminOperations.route:
            return String(localized: String.LocalizationValue("main_tab_admin_operations"), table: "Localizable")
        case MainNavigationTab.ranking.route:
            return String(localized: String.LocalizationValue("main_tab_ranking"), table: "Localizable")
        case MainNavigationTab.community.route:
            return String(localized: String.LocalizationValue("community_title"), table: "Localizable")
        case MainNavigationTab.myInfo.route:
            return String(localized: String.LocalizationValue("main_tab_my_info"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("main_tab_home"), table: "Localizable")
        }
    }

    init(
        initialTab: String? = nil,
        hasUnreadNotifications: Bool = false,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.initialTab = initialTab
        self.hasUnreadNotifications = hasUnreadNotifications
        self.onNavigationAction = onNavigationAction

        AppBarAppearance.configureDefaultAppearance()
    }
}

struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView(initialTab: "home", hasUnreadNotifications: false, onNavigationAction: { _ in })
    }
}
