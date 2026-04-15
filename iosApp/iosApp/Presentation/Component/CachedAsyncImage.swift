//
//  CachedAsyncImage.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/22.
//

import Foundation
import ImageIO
import UIKit
import SwiftUI

/// Composable image sizes matching Compose `ImageDisplaySize`.
///
/// - `thumbnail`: Small list cards / avatars (~512 px max dimension)
/// - `medium`:    Banners, event cards (~1200 px max dimension)
/// - `full`:      Full-screen viewer — no downscaling
enum ImageDisplaySize {
    case thumbnail
    case medium
    case full

    var maxPixels: Int? {
        switch self {
        case .thumbnail: return 512
        case .medium:    return 1200
        case .full:      return nil
        }
    }
}

struct CachedAsyncImage<Placeholder: View>: View {
    let url: URL?

    let placeholder: Placeholder

    let contentMode: ContentMode

    /// Controls how aggressively the image is downsampled before display.
    /// Matches the `ImageDisplaySize` enum used on Android/Desktop.
    let displaySize: ImageDisplaySize

    @StateObject private var loader = CachedImageLoader()

    var body: some View {
        ZStack {
            placeholder
            if let image = loader.image {
                if contentMode == .fit {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                } else {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                }
            }
        }
        .task(id: cacheKey) {
            await loader.load(from: url, maxPixels: displaySize.maxPixels, cacheKey: cacheKey)
        }
    }

    init(
        url: URL?,
        placeholder: Placeholder,
        contentMode: ContentMode = .fill,
        displaySize: ImageDisplaySize = .thumbnail
    ) {
        self.url = url
        self.placeholder = placeholder
        self.contentMode = contentMode
        self.displaySize = displaySize
    }

    private var cacheKey: String {
        guard let url else { return "" }
        let sizeTag = displaySize.maxPixels.map { "\($0)" } ?? "full"
        return "\(url.absoluteString)|\(sizeTag)"
    }
}

@MainActor
private final class CachedImageLoader: ObservableObject {
    @Published var image: UIImage?

    private static let memoryCache = NSCache<NSString, UIImage>()

    func load(from url: URL?, maxPixels: Int?, cacheKey: String) async {
        guard let url, !cacheKey.isEmpty else {
            image = nil
            return
        }
        let nsKey = cacheKey as NSString

        if let cached = Self.memoryCache.object(forKey: nsKey) {
            image = cached
            return
        }

        let loaded = await Task.detached(priority: .userInitiated) {
            return await Self.fetchAndDecode(url: url, maxPixels: maxPixels, nsKey: nsKey)
        }.value

        image = loaded
    }

    /// Runs off the main thread. Fetches data (network or file), decodes at target size.
    private static func fetchAndDecode(url: URL, maxPixels: Int?, nsKey: NSString) -> UIImage? {
        // Disk/URLCache hit — avoids a network round-trip.
        let request = URLRequest(url: url)
        if let cachedResponse = URLCache.shared.cachedResponse(for: request),
           let img = decode(data: cachedResponse.data, maxPixels: maxPixels) {
            memoryCache.setObject(img, forKey: nsKey)
            return img
        }

        let data: Data?
        if url.isFileURL {
            data = try? Data(contentsOf: url)
        } else {
            // Synchronous fetch on background thread.
            var fetchedData: Data?
            let semaphore = DispatchSemaphore(value: 0)
            URLSession.shared.dataTask(with: request) { d, response, _ in
                if let d, let response {
                    fetchedData = d
                    let cached = CachedURLResponse(response: response, data: d)
                    URLCache.shared.storeCachedResponse(cached, for: request)
                }
                semaphore.signal()
            }.resume()
            semaphore.wait()
            data = fetchedData
        }

        guard let data, let img = decode(data: data, maxPixels: maxPixels) else { return nil }
        memoryCache.setObject(img, forKey: nsKey)
        return img
    }

    /// Decodes image data using ImageIO — downsamples to `maxPixels` without loading full
    /// resolution into memory first (matches WWDC best-practice for image loading perf).
    private static func decode(data: Data, maxPixels: Int?) -> UIImage? {
        let sourceOptions = [kCGImageSourceShouldCache: false] as CFDictionary
        guard let source = CGImageSourceCreateWithData(data as CFData, sourceOptions) else {
            return UIImage(data: data)
        }

        if let maxPx = maxPixels {
            let thumbOptions: [CFString: Any] = [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceShouldCacheImmediately: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: maxPx
            ]
            if let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, thumbOptions as CFDictionary) {
                return UIImage(cgImage: cgImage)
            }
        }

        // FULL or thumbnail fallback: decode at native size.
        let fullOptions = [kCGImageSourceShouldCacheImmediately: true] as CFDictionary
        if let cgImage = CGImageSourceCreateImageAtIndex(source, 0, fullOptions) {
            return UIImage(cgImage: cgImage)
        }
        return UIImage(data: data)
    }
}
