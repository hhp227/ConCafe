//
//  IosDeviceLocationDataSource.swift
//  ConCafe
//

import CoreLocation
import Foundation
import Shared

/// CoreLocation 기반 기기 위치 소스. shared 모듈의 DeviceLocationDataSource 구현체로,
/// 체크인에 쓸 수 있는 위치인지는 CheckInLocationPolicy로 판정한다.
final class IosDeviceLocationDataSource: NSObject, DeviceLocationDataSource, CLLocationManagerDelegate {
    private let locationManager = CLLocationManager()

    private let locationPolicy = CheckInLocationPolicy()

    private var continuation: CheckedContinuation<any CurrentLocation, Never>?

    private var pendingAuthContinuation: CheckedContinuation<CLAuthorizationStatus, Never>?

    private var locationTimeoutWorkItem: DispatchWorkItem?

    private var locationRetryCount = 0

    override init() {
        super.init()
        locationManager.delegate = self
    }

    func requestPermission() async throws -> LocationPermissionStatus {
        if CLLocationManager.locationServicesEnabled() == false {
            return .serviceUnavailable
        }
        let resolvedStatus = await resolveAuthorizationStatus(locationManager.authorizationStatus)
        let hasFullAccuracy = locationManager.accuracyAuthorization == .fullAccuracy

        if (resolvedStatus == .authorizedAlways || resolvedStatus == .authorizedWhenInUse) && hasFullAccuracy {
            return .granted
        } else if resolvedStatus == .denied || resolvedStatus == .restricted || !hasFullAccuracy {
            return .preciseLocationRequired
        } else {
            return .requestPending
        }
    }

    func getCurrentLocation() async throws -> any CurrentLocation {
        guard hasPreciseLocationPermission() else {
            return CurrentLocationUnavailable(reason: .permissionRequired)
        }
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationRetryCount = 0
        return await withCheckedContinuation { continuation in
            self.continuation = continuation
            let timeoutWorkItem = DispatchWorkItem { [weak self] in
                guard let self else { return }
                self.finish(with: CurrentLocationUnavailable(reason: .lowAccuracy))
            }

            self.locationTimeoutWorkItem?.cancel()
            self.locationTimeoutWorkItem = timeoutWorkItem
            DispatchQueue.main.asyncAfter(deadline: .now() + Self.locationTimeoutSeconds, execute: timeoutWorkItem)
            self.locationManager.requestLocation()
        }
    }

    func getLastKnownLocation() async throws -> GeoPoint? {
        guard let location = locationManager.location else { return nil }
        return GeoPoint(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude)
    }

    private func hasPreciseLocationPermission() -> Bool {
        let status = locationManager.authorizationStatus
        let hasFullAccuracy = locationManager.accuracyAuthorization == .fullAccuracy
        return (status == .authorizedAlways || status == .authorizedWhenInUse) && hasFullAccuracy
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

    private func finish(with location: any CurrentLocation) {
        locationTimeoutWorkItem?.cancel()
        locationTimeoutWorkItem = nil
        continuation?.resume(returning: location)
        continuation = nil
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
                locationPolicy.isAcceptable(
                    accuracyMeters: location.horizontalAccuracy,
                    ageMillis: Int64(now.timeIntervalSince(location.timestamp) * 1000)
                )
            }
            .min { lhs, rhs in
                if lhs.horizontalAccuracy == rhs.horizontalAccuracy {
                    return lhs.timestamp > rhs.timestamp
                }
                return lhs.horizontalAccuracy < rhs.horizontalAccuracy
            }

        if let resolved = candidate {
            finish(
                with: CurrentLocationAvailable(
                    point: GeoPoint(
                        latitude: resolved.coordinate.latitude,
                        longitude: resolved.coordinate.longitude
                    )
                )
            )
        } else if locationRetryCount < Self.maxRetryCount {
            locationRetryCount += 1
            manager.requestLocation()
        } else {
            finish(with: CurrentLocationUnavailable(reason: .lowAccuracy))
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        finish(with: CurrentLocationUnavailable(reason: .lowAccuracy))
    }

    private static let locationTimeoutSeconds: TimeInterval = 8

    private static let maxRetryCount = 1
}
