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

    private var locationRetryCount = 0

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
        let hasFullAccuracy = locationManager.accuracyAuthorization == .fullAccuracy

        if (resolvedStatus == .authorizedAlways || resolvedStatus == .authorizedWhenInUse) && hasFullAccuracy {
            return IosCheckInPermissionResult(
                isGranted: true,
                message: "",
                requiresSettings: false
            )
        } else {
            return IosCheckInPermissionResult(
                isGranted: false,
                message: Self.preciseLocationRequiredMessage,
                requiresSettings: resolvedStatus == .denied || resolvedStatus == .restricted || !hasFullAccuracy
            )
        }
    }

    func getCurrentLocation() async -> IosCheckInLocationResult {
        let status = locationManager.authorizationStatus
        let hasFullAccuracy = locationManager.accuracyAuthorization == .fullAccuracy
        guard (status == .authorizedAlways || status == .authorizedWhenInUse) && hasFullAccuracy else {
            return IosCheckInLocationResult(
                isSuccess: false,
                location: fallbackLocation,
                message: Self.preciseLocationRequiredMessage
            )
        }
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationRetryCount = 0
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
                                message: Self.lowAccuracyMessage
                            )
                        )
                    }
                }

                self.locationTimeoutWorkItem?.cancel()
                self.locationTimeoutWorkItem = timeoutWorkItem
                DispatchQueue.main.asyncAfter(deadline: .now() + 8.0, execute: timeoutWorkItem)
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
        let now = Date()
        let candidate = locations
            .filter { location in
                let age = now.timeIntervalSince(location.timestamp)
                let isRecent = age >= 0 && age <= Self.maxLocationAgeSeconds
                let isAccurate = location.horizontalAccuracy >= 0 && location.horizontalAccuracy <= Self.maxAllowedAccuracyMeters
                return isRecent && isAccurate
            }
            .min { lhs, rhs in
                if lhs.horizontalAccuracy == rhs.horizontalAccuracy {
                    return lhs.timestamp > rhs.timestamp
                }
                return lhs.horizontalAccuracy < rhs.horizontalAccuracy
            }

        if let resolved = candidate {
            locationTimeoutWorkItem?.cancel()
            locationTimeoutWorkItem = nil
            continuation?.resume(
                returning: IosCheckInLocationResult(
                    isSuccess: true,
                    location: resolved.coordinate,
                    message: ""
                )
            )
            continuation = nil
        } else {
            if locationRetryCount < Self.maxRetryCount {
                locationRetryCount += 1
                manager.requestLocation()
                return
            }
            locationTimeoutWorkItem?.cancel()
            locationTimeoutWorkItem = nil
            continuation?.resume(
                returning: IosCheckInLocationResult(
                    isSuccess: false,
                    location: fallbackLocation,
                    message: Self.lowAccuracyMessage
                )
            )
            continuation = nil
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        locationTimeoutWorkItem?.cancel()
        locationTimeoutWorkItem = nil
        continuation?.resume(
            returning: IosCheckInLocationResult(
                isSuccess: false,
                location: fallbackLocation,
                message: Self.lowAccuracyMessage
            )
        )
        continuation = nil
    }

    func getLastKnownLocation() -> IosCheckInLocationResult? {
        guard let location = locationManager.location else { return nil }
        return IosCheckInLocationResult(
            isSuccess: true,
            location: location.coordinate,
            message: ""
        )
    }

    override init() {
        super.init()
        locationManager.delegate = self
    }

    private static let maxLocationAgeSeconds: TimeInterval = 30

    private static let maxAllowedAccuracyMeters: CLLocationAccuracy = 80

    private static let maxRetryCount = 1

    private static let preciseLocationRequiredMessage = "정확한 위치 권한이 필요합니다. 설정에서 정확한 위치를 허용해 주세요."

    private static let lowAccuracyMessage = "위치 정확도가 낮습니다. 정확한 위치를 켜고 잠시 후 다시 시도해 주세요."
}
