import Foundation
import UIKit

func saveCompressedImageToTemporaryFileAsync(
    _ image: UIImage,
    maxBytes: Int = 1_048_575,
    completion: @escaping (String?) -> Void
) {
    DispatchQueue.global(qos: .userInitiated).async {
        let backgroundResult = autoreleasepool {
            saveCompressedImageToTemporaryFile(image, maxBytes: maxBytes)
        }

        DispatchQueue.main.async {
            if backgroundResult != nil {
                completion(backgroundResult)
            } else {
                completion(saveCompressedImageToTemporaryFile(image, maxBytes: maxBytes))
            }
        }
    }
}

func saveCompressedImageToTemporaryFile(_ image: UIImage, maxBytes: Int = 1_048_575) -> String? {
    let fileName = "concafe-image-\(UUID().uuidString).jpg"
    let fileURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
    let normalizedImage = normalizedForEncoding(image) ?? image

    if let originalData = normalizedImage.jpegData(compressionQuality: 1.0), originalData.count <= maxBytes {
        do {
            try originalData.write(to: fileURL, options: [.atomic])
            return fileURL.absoluteString
        } catch {
            return nil
        }
    }

    var workingImage = normalizedImage
    var quality: CGFloat = 0.9
    var attempt = 0

    while attempt < 12 {
        guard let jpegData = workingImage.jpegData(compressionQuality: quality) else {
            return nil
        }
        if jpegData.count <= maxBytes {
            do {
                try jpegData.write(to: fileURL, options: [.atomic])
                return fileURL.absoluteString
            } catch {
                return nil
            }
        }

        if quality > 0.55 {
            quality -= 0.05
        } else {
            guard let resized = resizeImage(workingImage, scale: 0.85) else {
                return nil
            }
            workingImage = resized
            quality = 0.85
        }
        attempt += 1
    }

    return nil
}

private func resizeImage(_ image: UIImage, scale: CGFloat) -> UIImage? {
    let targetSize = CGSize(
        width: max(image.size.width * scale, 480),
        height: max(image.size.height * scale, 480)
    )
    let renderer = UIGraphicsImageRenderer(size: targetSize)
    return renderer.image { _ in
        image.draw(in: CGRect(origin: .zero, size: targetSize))
    }
}

private func normalizedForEncoding(_ image: UIImage) -> UIImage? {
    guard image.cgImage == nil || image.imageOrientation != .up else {
        return image
    }
    let renderer = UIGraphicsImageRenderer(size: image.size)
    return renderer.image { _ in
        image.draw(in: CGRect(origin: .zero, size: image.size))
    }
}
