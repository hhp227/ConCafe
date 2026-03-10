//
//  MainView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct MainView: View {
    @StateObject private var viewModel = MainViewModel()

    let onNavigationAction: (NavigationAction) -> Void
    
    @State private var selectedTab = "home"

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
        .onChange(of: viewModel.uiState.selectedTab) { newValue in
            if selectedTab != newValue {
                selectedTab = newValue
            }
        }
        .onChange(of: selectedTab) { newValue in
            if viewModel.uiState.selectedTab != newValue {
                viewModel.onAction(.refreshNavigation(preferredRoute: newValue))
            }
        }
        .onReceive(viewModel.event) { _ in
        }
    }

    @ViewBuilder
    private var roleBasedThirdTabView: some View {
        switch viewModel.uiState.thirdTab {
        case .fanManagement:
            FanManagementView(title: "팬관리", description: "캐스트가 팔로워, 출근 일정, 팬 대상 공지를 관리하는 메인 탭입니다.")
                .tabItem { Label("팬관리", systemImage: "person.2.fill") }
        case .cafeManagement:
            CafeManagementView(onNavigationAction: onNavigationAction)
                .tabItem { Label("카페관리", systemImage: "storefront.fill") }
        case .adminOperations:
            AdminOperationsView(title: "운영관리", description: "관리자가 승인, Claim, 신고, 밴 처리를 수행하는 메인 탭입니다.")
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
}

struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView(onNavigationAction: { _ in })
    }
}
