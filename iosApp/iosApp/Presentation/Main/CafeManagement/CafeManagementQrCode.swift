//
//  CafeManagementQrCode.swift
//  ConCafe
//
//  Created by 홍희표 on 4/12/26.
//

import Foundation
import SwiftUICore
import CoreImage.CIFilterBuiltins
import UIKit

struct DashboardQrCodeImageView: View {
    let payload: String

    var body: some View {
        if let image = generateDashboardQrImage(from: payload) {
            Image(uiImage: image)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }
}

private let dashboardQrCiContext = CIContext()

func generateDashboardQrImage(from payload: String) -> UIImage? {
    let filter = CIFilter.qrCodeGenerator()
    filter.message = Data(payload.utf8)
    filter.correctionLevel = "M"

    guard let outputImage = filter.outputImage else {
        return nil
    }
    let transformedImage = outputImage.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
    guard let cgImage = dashboardQrCiContext.createCGImage(transformedImage, from: transformedImage.extent) else {
        return nil
    }
    return UIImage(cgImage: cgImage)
}
