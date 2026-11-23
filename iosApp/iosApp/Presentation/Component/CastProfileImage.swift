//
//  CastProfileImage.swift
//  ConCafe
//

import SwiftUI
import Foundation

/// 캐스트 목록용 프로필 이미지.
/// 프로필 이미지 → 소속 카페 썸네일 → 기본 로고(maid_logo) 순서로 표시한다.
struct CastProfileImage: View {
    let profileImageUrl: String?

    let cafeImageUrl: String?

    var displaySize: ImageDisplaySize = .thumbnail

    var body: some View {
        if let url = ImageUrlUtils.normalizedRemoteUrl(from: profileImageUrl)
            ?? ImageUrlUtils.normalizedRemoteUrl(from: cafeImageUrl) {
            CachedAsyncImage(
                url: url,
                placeholder: Color.clear,
                displaySize: displaySize
            )
        } else {
            Image("maid_logo")
                .resizable()
                .scaledToFill()
        }
    }

    static func resolvedImageUrl(profileImageUrl: String?, cafeImageUrl: String?) -> String? {
        let profile = profileImageUrl?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !profile.isEmpty { return profile }
        let cafe = cafeImageUrl?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return cafe.isEmpty ? nil : cafe
    }
}
