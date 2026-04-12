//
//  CafeManagementQrCodeSaver.swift
//  ConCafe
//
//  Created by 홍희표 on 4/12/26.
//

import Foundation
import Photos
import UIKit

func saveDashboardQrImageToPhotoLibrary(
    _ image: UIImage,
    completion: @escaping (Bool) -> Void
) {
    let saveBlock = {
        PHPhotoLibrary.shared().performChanges({
            PHAssetChangeRequest.creationRequestForAsset(from: image)
        }) { success, _ in
            DispatchQueue.main.async {
                completion(success)
            }
        }
    }

    switch PHPhotoLibrary.authorizationStatus(for: .addOnly) {
    case .authorized, .limited:
        saveBlock()
    case .notDetermined:
        PHPhotoLibrary.requestAuthorization(for: .addOnly) { status in
            switch status {
            case .authorized, .limited:
                saveBlock()
            default:
                DispatchQueue.main.async {
                    completion(false)
                }
            }
        }
    default:
        DispatchQueue.main.async {
            completion(false)
        }
    }
}
