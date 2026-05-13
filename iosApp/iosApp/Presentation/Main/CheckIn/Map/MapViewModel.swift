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

    private let getCheckInMapCafePageUseCase: GetCheckInMapCafePageUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

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
                    if uiState.selectedRegion == .all && uiState.userCityKey == nil {
                        uiState.mapCafes = feed.mapCafes
                    }
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

    private func observeSession() {
        tasks[.session]?.cancel()
        tasks[.session] = Task {
            do {
                for try await user in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    uiState.isLoggedIn = user != nil
                    if user != nil {
                        uiState.isLoginPromptVisible = false
                    }
                }
            } catch {
                print("Error: \(error)")
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
                    uiState.userCityKey = cityKey
                    if uiState.selectedRegion == .all,
                       let cityKey {
                        loadMapCafesForRegion(cityKey)
                    }
                    return
                }
            }

            let result = await currentLocationProvider.getCurrentLocation()
            guard result.isSuccess else { return }
            let cityKey = Self.cityKeyFromCoordinates(
                lat: result.location.latitude,
                lng: result.location.longitude
            )
            uiState.userCityKey = cityKey
            if uiState.selectedRegion == .all,
               let cityKey {
                loadMapCafesForRegion(cityKey)
            }
        }
    }

    private func loadMapCafesForRegion(_ regionKey: String) {
        tasks[.mapRegion]?.cancel()
        tasks[.mapRegion] = Task {
            do {
                let result = try await getCheckInMapCafePageUseCase.invoke(regionKey: regionKey, pageSize: 80)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let cafes = success.data as? [CheckInCafeSummary] {
                    uiState.mapCafes = cafes
                    uiState.errorMessage = nil
                } else if let failure = result as? AppResultFailure {
                    uiState.errorMessage = "\(failure.error)"
                }
            } catch {
                if Task.isCancelled { return }
                uiState.errorMessage = error.localizedDescription
            }
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
              let region = ExploreUiState.RegionFilter.allCases.first(where: { $0.label == regionKey || $0.rawValue == regionKey || (regionKey == "yokohama" && $0 == .etc) }) else { return }
        uiState.selectedRegion = region
        loadMapCafesForRegion(region.rawValue)
    }

    func onAction(_ action: MapAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .cafeTapped(let id):
            if uiState.isLoggedIn {
                event.send(.navigateToCafe(id: id))
            } else {
                uiState.isLoginPromptVisible = true
            }
        case .loginPromptSignInTapped:
            uiState.isLoginPromptVisible = false
            event.send(.navigateToSignIn)
        case .dismissLoginPrompt:
            uiState.isLoginPromptVisible = false
        case .regionChanged(let region):
            uiState.selectedRegion = region
            if region == .all {
                if let userCityKey = uiState.userCityKey {
                    loadMapCafesForRegion(userCityKey)
                }
            } else {
                loadMapCafesForRegion(region.rawValue)
            }
        case .searchQueryChanged(let query):
            uiState.searchQuery = query
        }
    }

    init(
        getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase = KoinInitializerKt.resolveGetCheckInGuestFeedUseCase(),
        getCheckInMapCafePageUseCase: GetCheckInMapCafePageUseCase = KoinInitializerKt.resolveGetCheckInMapCafePageUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher()
    ) {
        self.getCheckInGuestFeedUseCase = getCheckInGuestFeedUseCase
        self.getCheckInMapCafePageUseCase = getCheckInMapCafePageUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher

        observeSession()
        observeCafeDetailEvent()
        detectUserCity()
        loadMapFeed()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case session
        case mapFeed
        case mapRegion
        case cafeDetailEvent
        case detectCity
    }

    private static func cityKeyFromCoordinates(lat: Double, lng: Double) -> String? {
        if (37.4...37.7).contains(lat) && (126.7...127.2).contains(lng) { return "seoul" }
        if (35.0...35.4).contains(lat) && (128.8...129.3).contains(lng) { return "busan" }
        if (35.7...36.0).contains(lat) && (128.4...128.8).contains(lng) { return "daegu" }
        if (35.35...35.60).contains(lat) && (139.50...139.75).contains(lng) { return "etc" }
        if (35.5...35.9).contains(lat) && (139.3...139.9).contains(lng) { return "tokyo" }
        if (34.5...34.9).contains(lat) && (135.3...135.7).contains(lng) { return "osaka" }
        return nil
    }
}
