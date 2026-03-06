//
//  CastDetailView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct CastDetailView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        VStack {
            Text("캐스트 상세")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}


struct CastDetailView_Previews: PreviewProvider {
    static var previews: some View {
        CastDetailView(onNavigationAction: { _ in })
    }
}
