//
//  CachedAsyncImage.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/22.
//

import Foundation
import UIKit
import SwiftUI

struct CachedAsyncImage<Placeholder: View>: View {
    let url: URL?

    let placeholder: Placeholder

    @StateObject private var loader = CachedImageLoader()

    var body: some View {
        ZStack {
            placeholder
            if let image = loader.image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            }
        }
        .task(id: url) {
            await loader.load(from: url)
        }
    }
}

@MainActor
private final class CachedImageLoader: ObservableObject {
    @Published var image: UIImage?

    private static let memoryCache = NSCache<NSString, UIImage>()

    func load(from url: URL?) async {
        guard let url else {
            image = nil
            return
        }
        let cacheKey = url.absoluteString as NSString

        if let cachedImage = Self.memoryCache.object(forKey: cacheKey) {
            image = cachedImage
            return
        }
        if let cachedResponse = URLCache.shared.cachedResponse(for: URLRequest(url: url)),
           let cachedImage = UIImage(data: cachedResponse.data) {
            Self.memoryCache.setObject(cachedImage, forKey: cacheKey)
            image = cachedImage
            return
        }
        do {
            let imageData: Data

            if url.isFileURL {
                imageData = try Data(contentsOf: url)
            } else {
                let (data, response) = try await URLSession.shared.data(from: url)
                imageData = data
                let cachedResponse = CachedURLResponse(response: response, data: data)
                
                URLCache.shared.storeCachedResponse(cachedResponse, for: URLRequest(url: url))
            }
            if let loadedImage = UIImage(data: imageData) {
                Self.memoryCache.setObject(loadedImage, forKey: cacheKey)
                image = loadedImage
            } else {
                image = nil
            }
        } catch {
            image = nil
        }
    }
}
