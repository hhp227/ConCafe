//
//  MainView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct MainView: View {
    let initialTab: String?
    
    let onNavigationAction: (NavigationAction) -> Void

    @State private var selectedTab = "home"

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $selectedTab) {
                HomeView(onNavigationAction: onNavigationAction)
                    .tabItem { Label("홈", systemImage: "house.fill") }
                    .tag("home")
                ExploreView()
                    .tabItem { Label("탐색", systemImage: "magnifyingglass") }
                    .tag("explore")
                if #available(iOS 16.0, *) {
                    RankingView()
                        .tabItem { Label("랭킹", systemImage: "trophy.fill") }
                        .tag("ranking")
                } else {
                    RankingView()
                        .tabItem { Label("랭킹", systemImage: "star.fill") }
                        .tag("ranking")
                }
                CheckInView()
                    .tabItem { Label("체크인", systemImage: "checkmark.seal.fill") }
                    .tag("checkin")
                MyInfoView()
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
        .onAppear {
            selectedTab = initialTab ?? "home"
        }
    }
}

struct MainView_Previews: PreviewProvider {
    static var previews: some View {
        MainView(initialTab: nil, onNavigationAction: { _ in })
    }
}
