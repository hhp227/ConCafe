//
//  MainView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct MainView: View {
    let initialTab: String?

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
                    .tag(viewModel.uiState.thirdTabRoute)
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
                    onNavigationAction(.navigateToNotification)
                } label: {
                    Image(systemName: "bell")
                }
                .accessibilityLabel("알림")
            }
        }
        .task {
            viewModel.onAction(.enter(preferredRoute: initialTab ?? selectedTab))
        }
        .onChange(of: initialTab) { newValue in
            viewModel.onAction(.refreshNavigation(preferredRoute: newValue ?? selectedTab))
        }
        .onChange(of: selectedTab) { newValue in
            viewModel.onAction(.refreshNavigation(preferredRoute: newValue))
        }
        .onReceive(viewModel.$uiState) { state in
            selectedTab = state.selectedTab
        }
        .onReceive(viewModel.event) { _ in
        }
    }

    @ViewBuilder
    private var roleBasedThirdTabView: some View {
        switch viewModel.uiState.thirdTabRoute {
        case "fanManagement":
            FanManagementView()
                .tabItem { Label("팬관리", systemImage: "person.2.fill") }
        case "cafeManagement":
            CafeManagementView()
                .tabItem { Label("카페관리", systemImage: "storefront.fill") }
        case "adminOperations":
            AdminOperationsView()
                .tabItem { Label("운영관리", systemImage: "shield.lefthalf.filled") }
        default:
            CheckInView()
                .tabItem { Label("체크인", systemImage: "checkmark.seal.fill") }
        }
    }

    @ViewBuilder
    private var rankingTabView: some View {
        if #available(iOS 16.0, *) {
            RankingView()
                .tabItem { Label("랭킹", systemImage: "trophy.fill") }
        } else {
            RankingView()
                .tabItem { Label("랭킹", systemImage: "star.fill") }
        }
    }
}

private struct FanManagementView: View {
    var body: some View {
        RoleManagementPlaceholderView(
            title: "팬관리",
            description: "캐스트가 팔로워, 출근 일정, 팬 대상 공지를 관리하는 메인 탭입니다."
        )
    }
}

private struct CafeManagementView: View {
    var body: some View {
        RoleManagementPlaceholderView(
            title: "카페관리",
            description: "카페 운영자가 공지, 이벤트, 메뉴, 캐스트 운영을 관리하는 메인 탭입니다."
        )
    }
}

private struct AdminOperationsView: View {
    var body: some View {
        RoleManagementPlaceholderView(
            title: "운영관리",
            description: "관리자가 승인, Claim, 신고, 밴 처리를 수행하는 메인 탭입니다."
        )
    }
}

private struct RoleManagementPlaceholderView: View {
    let title: String

    let description: String

    var body: some View {
        VStack(spacing: 12) {
            Text(title)
                .font(.title2)
                .bold()
            Text(description)
                .font(.body)
                .foregroundStyle(Color(hex: "6B6170"))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
        .background(
            LinearGradient(
                colors: [Color(hex: "FFF7FB"), Color(hex: "FFEDF5")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }
}

struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView(initialTab: nil, onNavigationAction: { _ in })
    }
}
