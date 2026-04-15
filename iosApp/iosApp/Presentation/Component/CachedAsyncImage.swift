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

        // 1. 메모리 캐시 히트 — 메인 스레드에서 즉시 반환
        if let cached = Self.memoryCache.object(forKey: nsKey) {
            image = cached
            return
        }

        // 2. 네트워크/디스크 I/O 및 디코딩 — 백그라운드 스레드에서 실행
        //    Task.detached: @MainActor 컨텍스트를 상속하지 않으므로 메인 스레드를 블로킹하지 않음
        let loaded = await Task.detached(priority: .userInitiated) {
            await Self.fetchAndDecode(url: url, maxPixels: maxPixels, nsKey: nsKey)
        }.value

        // 3. 결과 반영 — @MainActor이므로 자동으로 메인 스레드에서 실행
        image = loaded
    }

    /// 백그라운드 스레드에서 실행. async URLSession API 사용으로 스레드를 블로킹하지 않음.
    private static func fetchAndDecode(url: URL, maxPixels: Int?, nsKey: NSString) async -> UIImage? {
        let request = URLRequest(url: url)

        // URLCache 디스크 히트 — 네트워크 왕복 없이 즉시 디코딩
        if let cachedResponse = URLCache.shared.cachedResponse(for: request),
           let img = decode(data: cachedResponse.data, maxPixels: maxPixels) {
            memoryCache.setObject(img, forKey: nsKey)
            return img
        }

        let data: Data?
        if url.isFileURL {
            data = try? Data(contentsOf: url)
        } else {
            // async/await — 스레드를 블로킹하지 않고 대기
            data = await fetchRemoteData(request: request)
        }

        guard let data, let img = decode(data: data, maxPixels: maxPixels) else { return nil }
        memoryCache.setObject(img, forKey: nsKey)
        return img
    }

    /// URLSession async API: DispatchSemaphore 없이 협력 스레드 풀을 블로킹하지 않음
    private static func fetchRemoteData(request: URLRequest) async -> Data? {
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            let cached = CachedURLResponse(response: response, data: data)
            URLCache.shared.storeCachedResponse(cached, for: request)
            return data
        } catch {
            return nil
        }
    }

    /// ImageIO 기반 디코딩 — `maxPixels` 지정 시 풀 해상도를 메모리에 올리지 않고 바로 목표 크기로 디코딩
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

        // FULL: 네이티브 크기로 디코딩
        let fullOptions = [kCGImageSourceShouldCacheImmediately: true] as CFDictionary
        if let cgImage = CGImageSourceCreateImageAtIndex(source, 0, fullOptions) {
            return UIImage(cgImage: cgImage)
        }
        return UIImage(data: data)
    }
}
