//
//  CastView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct CastView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        VStack {
            Text("캐스트 상세")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}


struct CastView_Previews: PreviewProvider {
    static var previews: some View {
        CastView(onNavigationAction: { _ in })
    }
}
