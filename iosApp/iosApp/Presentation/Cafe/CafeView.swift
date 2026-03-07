//
//  CafeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import SwiftUI

struct CafeView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        VStack {
            Text("카페 상세")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct CafeView_Previews: PreviewProvider {
    static var previews: some View {
        CafeView(onNavigationAction: { _ in })
    }
}
