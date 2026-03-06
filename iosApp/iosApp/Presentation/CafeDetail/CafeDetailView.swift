//
//  CafeDetailView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import SwiftUI

struct CafeDetailView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        VStack {
            Text("카페 상세")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct CafeDetailView_Previews: PreviewProvider {
    static var previews: some View {
        CafeDetailView(onNavigationAction: { _ in })
    }
}
