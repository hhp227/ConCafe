//
//  CheckInQrScannerView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/12/26.
//

import SwiftUI
import AVFoundation

struct CheckInQrScannerView: UIViewControllerRepresentable {
    let onScanned: (String) -> Void
    
    let onScanFailed: (String) -> Void
    
    let onCanceled: () -> Void

    func makeUIViewController(context: Context) -> CheckInQrScannerViewController {
        let controller = CheckInQrScannerViewController()
        controller.onScanned = onScanned
        controller.onScanFailed = onScanFailed
        controller.onCanceled = onCanceled
        return controller
    }

    func updateUIViewController(_ uiViewController: CheckInQrScannerViewController, context: Context) {
        uiViewController.onScanned = onScanned
        uiViewController.onScanFailed = onScanFailed
        uiViewController.onCanceled = onCanceled
    }
}

final class CheckInQrScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onScanned: ((String) -> Void)?
    
    var onScanFailed: ((String) -> Void)?
    
    var onCanceled: (() -> Void)?

    private let captureSession = AVCaptureSession()
    
    private var previewLayer: AVCaptureVideoPreviewLayer?
    
    private var isHandlingResult = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        configureSession()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if !isHandlingResult {
            onCanceled?()
        }
    }

    deinit {
        captureSession.stopRunning()
    }

    private func configureSession() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            startScanner()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    guard let self else { return }
                    if granted {
                        self.startScanner()
                    } else {
                        self.isHandlingResult = true
                        self.onScanFailed?("카메라 접근 권한이 필요합니다.")
                    }
                }
            }
        default:
            isHandlingResult = true
            onScanFailed?("카메라 접근 권한이 필요합니다.")
        }
    }

    private func startScanner() {
        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
            isHandlingResult = true
            onScanFailed?("카메라를 찾을 수 없습니다.")
            return
        }
        guard let videoInput = try? AVCaptureDeviceInput(device: videoCaptureDevice) else {
            isHandlingResult = true
            onScanFailed?("카메라 입력을 초기화할 수 없습니다.")
            return
        }

        if captureSession.canAddInput(videoInput) {
            captureSession.addInput(videoInput)
        } else {
            isHandlingResult = true
            onScanFailed?("카메라 입력을 사용할 수 없습니다.")
            return
        }

        let metadataOutput = AVCaptureMetadataOutput()
        if captureSession.canAddOutput(metadataOutput) {
            captureSession.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            isHandlingResult = true
            onScanFailed?("QR 스캐너를 시작할 수 없습니다.")
            return
        }

        let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.videoGravity = .resizeAspectFill
        previewLayer.frame = view.bounds
        self.previewLayer = previewLayer
        view.layer.addSublayer(previewLayer)

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.captureSession.startRunning()
        }
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard !isHandlingResult else { return }
        guard let readableObject = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              readableObject.type == .qr,
              let rawValue = readableObject.stringValue?.trimmingCharacters(in: .whitespacesAndNewlines),
              !rawValue.isEmpty else { return }

        isHandlingResult = true
        captureSession.stopRunning()
        onScanned?(rawValue)
    }
}

#Preview {
    CheckInQrScannerView(onScanned: { _ in }, onScanFailed: { _ in }, onCanceled: {})
}
