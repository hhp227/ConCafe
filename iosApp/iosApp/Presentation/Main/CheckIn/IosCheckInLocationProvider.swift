//
//  IosCheckInLocationProvider.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/28.
//

import Foundation
import CoreLocation

struct IosCheckInLocationResult {
    let isSuccess: Bool

    let location: CLLocationCoordinate2D

    let message: String
}

struct IosCheckInPermissionResult {
    let isGranted: Bool

    let message: String

    let requiresSettings: Bool
}

final class IosCheckInLocationProvider: NSObject, CLLocationManagerDelegate {
    private let locationManager = CLLocationManager()

    private var continuation: CheckedContinuation<IosCheckInLocationResult, Never>?

    private var pendingAuthContinuation: CheckedContinuation<CLAuthorizationStatus, Never>?

    private var locationTimeoutWorkItem: DispatchWorkItem?

    private let fallbackLocation = CLLocationCoordinate2D(latitude: 0.0, longitude: 0.0)

    func requestPermissionIfNeeded() async -> IosCheckInPermissionResult {
        if CLLocationManager.locationServicesEnabled() == false {
            return IosCheckInPermissionResult(
                isGranted: false,
                message: "위치 서비스를 사용할 수 없습니다.",
                requiresSettings: true
            )
        }
        let status = locationManager.authorizationStatus
        let resolvedStatus = await resolveAuthorizationStatus(status)

        if resolvedStatus == .authorizedAlways || resolvedStatus == .authorizedWhenInUse {
            return IosCheckInPermissionResult(
                isGranted: true,
                message: "",
                requiresSettings: false
            )
        } else {
            return IosCheckInPermissionResult(
                isGranted: false,
                message: "위치 권한이 필요합니다. 설정에서 위치 권한을 허용해 주세요.",
                requiresSettings: resolvedStatus == .denied || resolvedStatus == .restricted
            )
        }
    }

    func getCurrentLocation() async -> IosCheckInLocationResult {
        let status = locationManager.authorizationStatus
        guard status == .authorizedAlways || status == .authorizedWhenInUse else {
            return IosCheckInLocationResult(
                isSuccess: false,
                location: fallbackLocation,
                message: "위치 권한이 없습니다."
            )
        }
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        return await withCheckedContinuation { continuation in
                self.continuation = continuation
                let timeoutWorkItem = DispatchWorkItem { [weak self] in
                    guard let self else { return }
                    if let pendingContinuation = self.continuation {
                        self.continuation = nil
                        pendingContinuation.resume(
                            returning: IosCheckInLocationResult(
                                isSuccess: false,
                                location: self.fallbackLocation,
                                message: "현재 위치 확인에 실패했습니다. 잠시 후 다시 시도해 주세요."
                            )
                        )
                    }
                }

                self.locationTimeoutWorkItem?.cancel()
                self.locationTimeoutWorkItem = timeoutWorkItem
                DispatchQueue.main.asyncAfter(deadline: .now() + 3.0, execute: timeoutWorkItem)
                self.locationManager.requestLocation()
            }
    }

    private func resolveAuthorizationStatus(_ status: CLAuthorizationStatus) async -> CLAuthorizationStatus {
        if status == .notDetermined {
            return await requestWhenInUseAuthorization()
        } else {
            return status
        }
    }

    private func requestWhenInUseAuthorization() async -> CLAuthorizationStatus {
        return await withCheckedContinuation { continuation in
            self.pendingAuthContinuation = continuation
            self.locationManager.requestWhenInUseAuthorization()
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        if pendingAuthContinuation != nil {
            pendingAuthContinuation?.resume(returning: manager.authorizationStatus)
            pendingAuthContinuation = nil
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        locationTimeoutWorkItem?.cancel()
        locationTimeoutWorkItem = nil
        let location = locations.first

        if location != nil {
            continuation?.resume(
                returning: IosCheckInLocationResult(
                    isSuccess: true,
                    location: location!.coordinate,
                    message: ""
                )
            )
        } else {
            continuation?.resume(
                returning: IosCheckInLocationResult(
                    isSuccess: false,
                    location: fallbackLocation,
                    message: "현재 위치를 확인할 수 없습니다."
                )
            )
        }
        continuation = nil
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        locationTimeoutWorkItem?.cancel()
        locationTimeoutWorkItem = nil
        continuation?.resume(
            returning: IosCheckInLocationResult(
                isSuccess: false,
                location: fallbackLocation,
                message: "현재 위치를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."
            )
        )
        continuation = nil
    }

    override init() {
        super.init()
        locationManager.delegate = self
    }
}
