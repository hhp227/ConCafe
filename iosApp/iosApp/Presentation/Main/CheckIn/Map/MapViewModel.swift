//
//  MapViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 4/23/26.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class MapViewModel: ObservableObject {
    private let getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let currentLocationProvider = IosCheckInLocationProvider()

    @Published private(set) var uiState = MapUiState.empty

    let event = PassthroughSubject<MapEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadMapFeed() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        tasks[.mapFeed]?.cancel()
        tasks[.mapFeed] = Task {
            do {
                let result = try await getCheckInGuestFeedUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CheckInGuestFeed {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.currentLocationLabel = feed.currentLocationLabel
                    uiState.mapCafes = feed.mapCafes
                } else if let failure = result as? AppResultFailure {
                    uiState.isLoading = false
                    uiState.errorMessage = "\(failure.error)"
                } else {
                    uiState.isLoading = false
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func detectUserCity() {
        guard uiState.userCityKey == nil else { return }
        tasks[.detectCity]?.cancel()
        tasks[.detectCity] = Task {
            if let cached = currentLocationProvider.getLastKnownLocation() {
                let cityKey = Self.cityKeyFromCoordinates(
                    lat: cached.location.latitude,
                    lng: cached.location.longitude
                )
                if cityKey != nil {
                    uiState.userCityKey = cityKey ?? Self.defaultNearbyCityKey
                    return
                }
            }

            let result = await currentLocationProvider.getCurrentLocation()
            guard result.isSuccess else {
                uiState.userCityKey = Self.defaultNearbyCityKey
                return
            }
            uiState.userCityKey = Self.cityKeyFromCoordinates(
                lat: result.location.latitude,
                lng: result.location.longitude
            ) ?? Self.defaultNearbyCityKey
        }
    }

    private func observeCafeDetailEvent() {
        tasks[.cafeDetailEvent]?.cancel()
        tasks[.cafeDetailEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeDetailEventPublisher.events) {
                    if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                        patchCafe(updated.cafe)
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func patchCafe(_ cafe: Cafe) {
        uiState.mapCafes = uiState.mapCafes.map { item in
            guard item.id == cafe.id else { return item }
            return CheckInCafeSummary(
                id: item.id,
                name: cafe.name,
                locationLabel: cafe.region.city,
                geoPoint: item.geoPoint,
                rating: cafe.ratingAvg,
                checkInCount: item.checkInCount,
                thumbnailImage: item.thumbnailImage
            )
        }
    }

    func initializeRegion(_ regionKey: String?) {
        guard uiState.selectedRegion == .all,
              let regionKey,
              let region = ExploreUiState.RegionFilter.allCases.first(where: { $0.name == regionKey }) else { return }
        uiState.selectedRegion = region
    }

    func onAction(_ action: MapAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .regionChanged(let region):
            uiState.selectedRegion = region
        case .searchQueryChanged(let query):
            uiState.searchQuery = query
        }
    }

    init(
        getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase = KoinInitializerKt.resolveGetCheckInGuestFeedUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher()
    ) {
        self.getCheckInGuestFeedUseCase = getCheckInGuestFeedUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher

        observeCafeDetailEvent()
        detectUserCity()
        loadMapFeed()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case mapFeed
        case cafeDetailEvent
        case detectCity
    }

    private static let defaultNearbyCityKey = "seoul"

    private static func cityKeyFromCoordinates(lat: Double, lng: Double) -> String? {
        if (37.4...37.7).contains(lat) && (126.7...127.2).contains(lng) { return "seoul" }
        if (35.0...35.4).contains(lat) && (128.8...129.3).contains(lng) { return "busan" }
        if (35.7...36.0).contains(lat) && (128.4...128.8).contains(lng) { return "daegu" }
        if (35.35...35.60).contains(lat) && (139.50...139.75).contains(lng) { return "yokohama" }
        if (35.5...35.9).contains(lat) && (139.3...139.9).contains(lng) { return "tokyo" }
        if (34.5...34.9).contains(lat) && (135.3...135.7).contains(lng) { return "osaka" }
        return nil
    }
}
