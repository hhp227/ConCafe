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
/// - `full`:      Full-screen viewer (~3840 px max dimension to avoid OOM on huge photos)
enum ImageDisplaySize {
    case thumbnail
    case medium
    case full

    var maxPixels: Int? {
        switch self {
        case .thumbnail: return 512
        case .medium:    return 1200
        // Match Android Compose behavior (Size(3840, 3840)).
        // Avoid decoding original-size ultra high resolution images directly.
        case .full:      return 3840
        }
    }
}

enum ImageUrlUtils {
    /// Handles non-ASCII path/query characters (e.g. Korean/Japanese file names).
    static func normalizedRemoteUrl(from raw: String?) -> URL? {
        guard let raw else { return nil }
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        if let url = URL(string: trimmed), url.scheme != nil {
            return url
        }

        if let encoded = trimmed.addingPercentEncoding(withAllowedCharacters: .urlFragmentAllowed),
           let url = URL(string: encoded),
           url.scheme != nil {
            return url
        }

        return nil
    }
}

struct DefaultCachedAsyncImagePlaceholder: View {
    var body: some View {
        ZStack {
            Color.black.opacity(25.0 / 255.0)
            Image(systemName: "photo")
                .font(.system(size: 36, weight: .regular))
                .foregroundStyle(Color.gray)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .allowsHitTesting(false)
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
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: contentMode)
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
        return CachedImageLoader.cacheKey(for: url, displaySize: displaySize)
    }
}

extension CachedAsyncImage where Placeholder == DefaultCachedAsyncImagePlaceholder {
    init(
        url: URL?,
        contentMode: ContentMode = .fill,
        displaySize: ImageDisplaySize = .thumbnail
    ) {
        self.init(
            url: url,
            placeholder: DefaultCachedAsyncImagePlaceholder(),
            contentMode: contentMode,
            displaySize: displaySize
        )
    }
}

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

@MainActor
private final class CachedImageLoader: ObservableObject {
    @Published var image: UIImage?

    /// 현재 표시 중인 캐시 키. 오래된(stale) Task 결과를 걸러내는 데 사용.
    private var loadedKey = ""

    private static var prefetchTasks: [String: Task<Void, Never>] = [:]

    private static let memoryCache: NSCache<NSString, UIImage> = {
        let cache = NSCache<NSString, UIImage>()
        cache.countLimit = 200
        cache.totalCostLimit = 100 * 1024 * 1024 // 100 MB
        return cache
    }()

    private static let diskCacheDirectory: URL = {
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        let directory = base.appendingPathComponent("ConCafeImageCache", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }()

    static func cacheKey(for url: URL, displaySize: ImageDisplaySize) -> String {
        let sizeTag = displaySize.maxPixels.map { "\($0)" } ?? "full"
        return "\(url.absoluteString)|\(sizeTag)"
    }

    static func prefetch(urls: [URL], displaySize: ImageDisplaySize) {
        for url in urls {
            guard prefetchTasks.count < 6 else { break }

            let cacheKey = cacheKey(for: url, displaySize: displaySize)
            let nsKey = cacheKey as NSString

            if memoryCache.object(forKey: nsKey) != nil || prefetchTasks[cacheKey] != nil {
                continue
            }

            let task = Task.detached(priority: .utility) {
                do {
                    _ = try await Self.fetchAndDecode(url: url, maxPixels: displaySize.maxPixels, nsKey: nsKey)
                } catch {
                    // Prefetch is opportunistic. The visible image load will retry if needed.
                }
            }
            prefetchTasks[cacheKey] = task

            Task {
                await task.value
                Self.prefetchTasks[cacheKey] = nil
            }
        }
    }

    func load(from url: URL?, maxPixels: Int?, cacheKey: String) async {
        guard let url, !cacheKey.isEmpty else {
            loadedKey = ""
            image = nil
            return
        }

        // 키가 바뀌면 이전 이미지를 즉시 제거 (잘못된 이미지가 잠깐 보이는 현상 방지)
        if loadedKey != cacheKey {
            image = nil
            loadedKey = cacheKey
        }

        let nsKey = cacheKey as NSString

        // 1. 메모리 캐시 히트 — 메인 스레드에서 즉시 반환
        if let cached = Self.memoryCache.object(forKey: nsKey) {
            image = cached
            return
        }

        // 2. 네트워크/디스크 I/O 및 디코딩 — 백그라운드 스레드에서 실행
        //    Task.detached: @MainActor 컨텍스트를 상속하지 않으므로 메인 스레드를 블로킹하지 않음
        let fetchTask = Task.detached(priority: .userInitiated) {
            try await Self.fetchAndDecode(url: url, maxPixels: maxPixels, nsKey: nsKey)
        }

        do {
            // withTaskCancellationHandler: .task(id:) 취소가 inner Task.detached에도 전파됨
            let loaded = try await withTaskCancellationHandler {
                try await fetchTask.value
            } onCancel: {
                fetchTask.cancel()
            }

            // 3. 취소되지 않았고 여전히 같은 키인 경우에만 반영
            guard !Task.isCancelled, loadedKey == cacheKey else { return }
            image = loaded
        } catch {
            // CancellationError 포함 모든 에러는 무시 (image = nil 유지)
        }
    }

    /// 백그라운드 스레드에서 실행. CancellationError를 throws로 전파하여 stale 업데이트를 방지.
    private static func fetchAndDecode(url: URL, maxPixels: Int?, nsKey: NSString) async throws -> UIImage? {
        let request = URLRequest(url: url)
        let diskURL = diskCacheURL(for: nsKey)

        if FileManager.default.fileExists(atPath: diskURL.path),
           let img = decode(fileURL: diskURL, maxPixels: maxPixels) {
            try? FileManager.default.setAttributes([.modificationDate: Date()], ofItemAtPath: diskURL.path)
            memoryCache.setObject(img, forKey: nsKey)
            return img
        } else if FileManager.default.fileExists(atPath: diskURL.path) {
            try? FileManager.default.removeItem(at: diskURL)
        }

        // URLCache 디스크 히트 — 네트워크 왕복 없이 즉시 디코딩
        if let cachedResponse = URLCache.shared.cachedResponse(for: request),
           let img = decode(data: cachedResponse.data, maxPixels: maxPixels) {
            try? cachedResponse.data.write(to: diskURL, options: .atomic)
            memoryCache.setObject(img, forKey: nsKey)
            return img
        }

        let image: UIImage?
        if url.isFileURL {
            image = decode(fileURL: url, maxPixels: maxPixels)
        } else {
            // 대용량 이미지는 Data 전체를 메모리에 올리면 피크가 커질 수 있으므로
            // 파일로 다운로드한 뒤 파일 기반으로 다운샘플 디코딩한다.
            let downloadedFileURL = try await fetchRemoteFile(request: request)
            image = decode(fileURL: downloadedFileURL, maxPixels: maxPixels)
            if image != nil {
                try? replaceDiskCachedFile(from: downloadedFileURL, to: diskURL)
            }
        }

        // 디코딩 전 취소 여부 확인
        try Task.checkCancellation()

        guard let img = image else { return nil }
        memoryCache.setObject(img, forKey: nsKey)
        return img
    }

    private static func diskCacheURL(for key: NSString) -> URL {
        let encoded = Data(key.description.utf8)
            .base64EncodedString()
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "=", with: "")
        return diskCacheDirectory.appendingPathComponent(encoded)
    }

    private static func replaceDiskCachedFile(from source: URL, to destination: URL) throws {
        if FileManager.default.fileExists(atPath: destination.path) {
            try? FileManager.default.removeItem(at: destination)
        }
        try FileManager.default.copyItem(at: source, to: destination)
        trimDiskCacheIfNeeded()
    }

    private static func trimDiskCacheIfNeeded(maxBytes: Int64 = 300 * 1024 * 1024) {
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: diskCacheDirectory,
            includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey]
        ) else { return }

        let entries = files.compactMap { url -> (url: URL, modified: Date, size: Int64)? in
            guard let values = try? url.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey]) else { return nil }
            return (url, values.contentModificationDate ?? .distantPast, Int64(values.fileSize ?? 0))
        }
        var totalSize = entries.reduce(Int64(0)) { $0 + $1.size }
        guard totalSize > maxBytes else { return }

        for entry in entries.sorted(by: { $0.modified < $1.modified }) {
            if totalSize <= maxBytes { break }
            try? FileManager.default.removeItem(at: entry.url)
            totalSize -= entry.size
        }
    }

    /// URLSession download API: 대용량 응답을 파일로 받아 메모리 피크를 줄인다.
    private static func fetchRemoteFile(request: URLRequest) async throws -> URL {
        let (tempFileURL, response) = try await URLSession.shared.download(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            return tempFileURL
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw URLError(.badServerResponse)
        }
        return tempFileURL
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

        // FULL (or fallback): 네이티브 크기로 디코딩
        let fullOptions = [kCGImageSourceShouldCacheImmediately: true] as CFDictionary
        if let cgImage = CGImageSourceCreateImageAtIndex(source, 0, fullOptions) {
            return UIImage(cgImage: cgImage)
        }
        return UIImage(data: data)
    }

    /// 파일 URL 기반 디코딩. 대용량 이미지에서 Data 로드보다 안정적이다.
    private static func decode(fileURL: URL, maxPixels: Int?) -> UIImage? {
        let sourceOptions = [kCGImageSourceShouldCache: false] as CFDictionary
        guard let source = CGImageSourceCreateWithURL(fileURL as CFURL, sourceOptions) else {
            return UIImage(contentsOfFile: fileURL.path)
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

        let fullOptions = [kCGImageSourceShouldCacheImmediately: true] as CFDictionary
        if let cgImage = CGImageSourceCreateImageAtIndex(source, 0, fullOptions) {
            return UIImage(cgImage: cgImage)
        }
        return UIImage(contentsOfFile: fileURL.path)
    }
}
