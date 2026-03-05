//
//  HomeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct HomeView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("홈 화면")
            Button("상세화면 이동") {
                onNavigationAction(.navigateToDetail(id: "id"))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView(onNavigationAction: { _ in })
    }
}
