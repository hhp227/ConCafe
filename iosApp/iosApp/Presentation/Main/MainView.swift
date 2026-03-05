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
            }
        }
        .navigationTitle("ConCafe")
        .navigationBarTitleDisplayMode(.inline)
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
