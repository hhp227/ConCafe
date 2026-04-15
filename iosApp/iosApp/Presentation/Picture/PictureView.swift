//
//  PictureView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/31.
//

import SwiftUI

struct PictureView: View {
    let imageUrl: String

    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.black
                .ignoresSafeArea()
            if let url = URL(string: imageUrl.trimmingCharacters(in: .whitespacesAndNewlines)) {
                CachedAsyncImage(
                    url: url,
                    placeholder: Color.black,
                    contentMode: .fit,
                    displaySize: .full
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }
}

struct PictureView_Previews: PreviewProvider {
    static var previews: some View {
        PictureView(imageUrl: "", onNavigationAction: { _ in })
    }
}
