//
//  NotificationView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI

struct NotificationView: View {
    var body: some View {
        Text("알림")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .navigationTitle("알림")
            .navigationBarTitleDisplayMode(.inline)
    }
}

struct NotificationView_Previews: PreviewProvider {
    static var previews: some View {
        NotificationView()
    }
}
