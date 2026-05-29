//
//  ImagePrefetcher.swift
//  ConCafe
//
//  Created by 홍희표 on 5/29/26.
//

import Foundation
import SwiftUI

enum ImagePrefetcher {
    static func prefetch(
        _ imageUrls: [String?],
        displaySize: ImageDisplaySize = .thumbnail
    ) {
        let urls = imageUrls.compactMap { ImageUrlUtils.normalizedRemoteUrl(from: $0) }
        prefetch(urls, displaySize: displaySize)
    }

    static func prefetch(
        _ urls: [URL],
        displaySize: ImageDisplaySize = .thumbnail
    ) {
        let uniqueUrls = Array(Dictionary(grouping: urls, by: \.absoluteString).compactMap { $0.value.first })
        guard !uniqueUrls.isEmpty else { return }

        Task { @MainActor in
            CachedImageLoader.prefetch(urls: uniqueUrls, displaySize: displaySize)
        }
    }
}

extension View {
    func lazyListImagePrefetch(
        index: Int,
        imageUrls: [String?],
        aheadCount: Int = 8,
        displaySize: ImageDisplaySize = .thumbnail
    ) -> some View {
        onAppear {
            guard index >= 0 else { return }
            let urls = Array(imageUrls.dropFirst(index + 1).prefix(aheadCount))
            ImagePrefetcher.prefetch(urls, displaySize: displaySize)
        }
    }
}

