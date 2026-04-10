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
                    contentMode: .fit
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            Button {
                onNavigationAction(.navigateBack)
            } label: {
                Image(systemName: "chevron.left")
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .background(Color.black.opacity(0.35))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .padding(.leading, 12)
            .padding(.top, 12)
        }
        .navigationBarBackButtonHidden(true)
    }
}

struct PictureView_Previews: PreviewProvider {
    static var previews: some View {
        PictureView(imageUrl: "", onNavigationAction: { _ in })
    }
}
