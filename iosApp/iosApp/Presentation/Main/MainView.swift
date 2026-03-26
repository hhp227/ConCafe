//
//  MainView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import UIKit

struct MainView: View {
    let initialTab: String?

    @StateObject private var viewModel = MainViewModel()

    let onNavigationAction: (NavigationAction) -> Void

    @State private var selectedTab: String = ""

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $selectedTab) {
                HomeView(onNavigationAction: onNavigationAction)
                    .tabItem { Label("홈", systemImage: "house.fill") }
                    .tag("home")
                ExploreView(onNavigationAction: onNavigationAction)
                    .tabItem { Label("탐색", systemImage: "magnifyingglass") }
                    .tag("explore")
                roleBasedThirdTabView
                    .tag(viewModel.uiState.thirdTab.route)
                rankingTabView
                    .tag("ranking")
                MyInfoView(onNavigationAction: onNavigationAction)
                    .tabItem { Label("내 정보", systemImage: "person") }
                    .tag("myinfo")
            }
        }
        .navigationTitle("ConCafe")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                ConCafeLogo()
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    if selectedTab == "myinfo" && viewModel.uiState.currentUser != nil {
                        onNavigationAction(.navigateToSettings)
                    } else {
                        onNavigationAction(.navigateToNotification)
                    }
                } label: {
                    Image(systemName: selectedTab == "myinfo" && viewModel.uiState.currentUser != nil ? "gearshape" : "bell")
                }
                .accessibilityLabel(selectedTab == "myinfo" && viewModel.uiState.currentUser != nil ? "설정" : "알림")
            }
        }
        .onAppear {
            if let initialTab = initialTab, !initialTab.isEmpty {
                selectedTab = initialTab

                viewModel.onAction(.selectTab(route: initialTab))
            } else {
                selectedTab = viewModel.uiState.selectedTab
            }
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
        .onReceive(viewModel.event) { _ in
        }
    }

    @ViewBuilder
    private var roleBasedThirdTabView: some View {
        switch viewModel.uiState.thirdTab {
        case .fanManagement:
            FanManagementView(onNavigationAction: onNavigationAction)
                .tabItem { Label("팬관리", systemImage: "person.2.fill") }
        case .cafeManagement:
            CafeManagementView(onNavigationAction: onNavigationAction)
                .tabItem { Label("카페관리", systemImage: "storefront.fill") }
        case .adminOperations:
            AdminOperationsView(onNavigationAction: onNavigationAction)
                .tabItem { Label("운영관리", systemImage: "shield.lefthalf.filled") }
        default:
            CheckInView(onNavigationAction: onNavigationAction)
                .tabItem { Label("체크인", systemImage: "checkmark.seal.fill") }
        }
    }

    @ViewBuilder
    private var rankingTabView: some View {
        RankingView(onNavigationAction: onNavigationAction)
            .tabItem {
                Label(
                    "랭킹",
                    systemImage: compatSystemImageName(iOS16: "trophy.fill", fallback: "star.fill")
                )
            }
    }

    init(
        initialTab: String? = nil,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.initialTab = initialTab
        self.onNavigationAction = onNavigationAction

        Self.configureBarAppearance()
    }

    private static func configureBarAppearance() {
        let backgroundColor = UIColor.systemBackground
        let navigationBarAppearance = UINavigationBarAppearance()
        navigationBarAppearance.configureWithOpaqueBackground()
        navigationBarAppearance.backgroundColor = backgroundColor
        navigationBarAppearance.shadowColor = UIColor.separator
        UINavigationBar.appearance().standardAppearance = navigationBarAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navigationBarAppearance
        UINavigationBar.appearance().compactAppearance = navigationBarAppearance
        let tabBarAppearance = UITabBarAppearance()
        tabBarAppearance.configureWithOpaqueBackground()
        tabBarAppearance.backgroundColor = backgroundColor
        tabBarAppearance.shadowColor = UIColor.separator
        UITabBar.appearance().standardAppearance = tabBarAppearance
        if #available(iOS 15.0, *) {
            UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
        }
    }
}

struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView(initialTab: "home", onNavigationAction: { _ in })
    }
}
