//
//  ScheduleView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI

struct ScheduleView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        Text("출근표 관리 화면")
            .navigationTitle("출근표 관리")
            .navigationBarTitleDisplayMode(.inline)
    }
}

struct ScheduleView_Previews: PreviewProvider {
    static var previews: some View {
        ScheduleView(onNavigationAction: { _ in })
    }
}
