//
//  DetailView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct DetailView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        VStack {
            Text("상세")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}


struct DetailView_Previews: PreviewProvider {
    static var previews: some View {
        DetailView(onNavigationAction: { _ in })
    }
}
