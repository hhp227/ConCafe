//
//  CheckInViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class CheckInViewModel: ObservableObject {
    private let getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase

    private let getCheckInUserFeedUseCase: GetCheckInUserFeedUseCase

    private let createVisitUseCase: CreateVisitUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let shouldShowReviewPromptUseCase: ShouldShowReviewPromptUseCase

    private let dismissReviewPromptUseCase: DismissReviewPromptUseCase

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let castEventPublisher: CastEventPublisher

    private let visitEventPublisher: VisitEventPublisher

    private let currentLocationProvider = IosCheckInLocationProvider()

    @Published private(set) var uiState = CheckInUiState.empty

    let event = PassthroughSubject<CheckInEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadGuestFeed() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        tasks[.guestFeed]?.cancel()
        tasks[.guestFeed] = Task {
            do {
                let result = try await getCheckInGuestFeedUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CheckInGuestFeed {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.currentLocationLabel = feed.currentLocationLabel
                    uiState.mapCafes = feed.mapCafes
                    uiState.popularCafes = feed.popularCafes
                    uiState.popularCasts = feed.popularCasts
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
                    self.uiState.currentUser = user
                    self.uiState.isLoginPromptVisible = user == nil ? self.uiState.isLoginPromptVisible : false
                    self.uiState.isNewVisitSheetVisible = false

                    if user == nil {
                        self.uiState.reviewPrompt = nil
                        self.uiState.todayVisits = []
                        self.uiState.recentVisits = []
                        self.uiState.recentVisitsNextCursor = nil
                        self.uiState.canLoadMoreRecentVisits = false
                        self.uiState.isLoadingMoreRecentVisits = false
                    } else {
                        self.refreshRecentVisitPage()
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func loadRecentVisitPage(cursor: String?, append: Bool) {
        tasks[.recentVisitPage]?.cancel()
        tasks[.recentVisitPage] = Task {
            uiState.isLoadingMoreRecentVisits = append

            do {
                let result = try await getCheckInUserFeedUseCase.invoke(cursor: cursor, pageSize: Self.recentVisitPageSize)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CheckInUserFeed {
                    let loadedRecentVisits = feed.recentVisits
                    var nextState = uiState
                    let mergedRecentVisits = append ? (nextState.recentVisits + loadedRecentVisits) : loadedRecentVisits
                    let mergedTodayVisits = mergedRecentVisits
                        .filter { TimeUtils.isCurrentDateVisitedAt($0.visitedAt) }
                        .prefix(Self.todayVisitLimit)

                    nextState.todayVisits = Array(mergedTodayVisits)
                    nextState.recentVisits = mergedRecentVisits
                    nextState.recentVisitsNextCursor = feed.recentVisitsNextCursor
                    nextState.canLoadMoreRecentVisits = feed.canLoadMoreRecentVisits
                    nextState.isLoadingMoreRecentVisits = false
                    uiState = nextState
                } else if let failure = result as? AppResultFailure {
                    var nextState = uiState
                    nextState.isLoadingMoreRecentVisits = false
                    nextState.errorMessage = "\(failure.error)"
                    if !append {
                        nextState.todayVisits = []
                        nextState.recentVisits = []
                        nextState.recentVisitsNextCursor = nil
                        nextState.canLoadMoreRecentVisits = false
                    }
                    uiState = nextState
                } else {
                    var nextState = uiState
                    nextState.isLoadingMoreRecentVisits = false
                    if !append {
                        nextState.todayVisits = []
                        nextState.recentVisits = []
                        nextState.recentVisitsNextCursor = nil
                        nextState.canLoadMoreRecentVisits = false
                    }
                    uiState = nextState
                }
            } catch {
                if Task.isCancelled { return }
                var nextState = uiState
                nextState.isLoadingMoreRecentVisits = false
                nextState.errorMessage = error.localizedDescription
                if !append {
                    nextState.todayVisits = []
                    nextState.recentVisits = []
                    nextState.recentVisitsNextCursor = nil
                    nextState.canLoadMoreRecentVisits = false
                }
                uiState = nextState
            }
        }
    }

    private func refreshRecentVisitPage() {
        loadRecentVisitPage(cursor: nil, append: false)
    }

    private func loadMoreRecentVisitPage() {
        if uiState.canLoadMoreRecentVisits,
           !uiState.isLoadingMoreRecentVisits,
           let cursor = uiState.recentVisitsNextCursor {
            loadRecentVisitPage(cursor: cursor, append: true)
        }
    }

    private func detectUserCity() {
        guard uiState.userCityKey == nil else { return }
        tasks[.detectCity]?.cancel()
        tasks[.detectCity] = Task {
            let result = await currentLocationProvider.getCurrentLocation()
            guard result.isSuccess else { return }
            let cityKey = Self.cityKeyFromCoordinates(
                lat: result.location.latitude,
                lng: result.location.longitude
            )
            uiState.userCityKey = cityKey
        }
    }

    private func submitNewVisit(cafeId: String, visitedAt: String, memo: String?) {
        if cafeId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            uiState.errorMessage = "카페를 선택해 주세요."
        } else if visitedAt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            uiState.errorMessage = "방문 시간을 입력해 주세요."
        } else {
            uiState.errorMessage = nil

            tasks[.submitVisit]?.cancel()
            uiState.errorMessage = "현재 위치를 확인하는 중입니다. 잠시만 기다려 주세요."
            tasks[.submitVisit] = Task {
                do {
                    let locationResult = await currentLocationProvider.getCurrentLocation()

                    if !locationResult.isSuccess {
                        uiState.errorMessage = locationResult.message
                        return
                    }
                    let resolvedLocation = locationResult.location
                    let normalizedMemo = memo?.trimmingCharacters(in: .whitespacesAndNewlines)

                    let result = try await createVisitUseCase.invoke(
                        cafeId: cafeId,
                        visitedAt: visitedAt,
                        memo: normalizedMemo?.isEmpty == true ? nil : normalizedMemo,
                        latitude: resolvedLocation.latitude,
                        longitude: resolvedLocation.longitude
                    )

                    if result is AppResultSuccess<AnyObject> {
                        uiState.isNewVisitSheetVisible = false
                        uiState.preselectCafeId = nil
                        uiState.errorMessage = nil
                        refreshRecentVisitPage()
                        if let success = result as? AppResultSuccess<AnyObject>,
                           let visit = success.data as? Visit {
                            await maybeShowReviewPrompt(visit: visit)
                        }
                    } else if let failure = result as? AppResultFailure {
                        uiState.errorMessage = "\(failure.error)"
                    } else {
                        uiState.errorMessage = "체크인 저장에 실패했습니다."
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.errorMessage = error.localizedDescription
                }
            }
        }
    }

    private func maybeShowReviewPrompt(visit: Visit) async {
        guard visit.verified else { return }

        do {
            let result = try await shouldShowReviewPromptUseCase.invoke(visitId: visit.id)
            guard let success = result as? AppResultSuccess<AnyObject>,
                  let shouldShow = success.data as? NSNumber,
                  shouldShow.boolValue else { return }

            let cafeName =
                uiState.mapCafes.first(where: { $0.id == visit.cafeId })?.name ??
                uiState.popularCafes.first(where: { $0.id == visit.cafeId })?.name ??
                uiState.todayVisits.first(where: { $0.cafeId == visit.cafeId })?.cafeName ??
                uiState.recentVisits.first(where: { $0.cafeId == visit.cafeId })?.cafeName ??
                "방문한 카페"

            uiState.reviewPrompt = CheckInUiState.ReviewPrompt(
                visitId: visit.id,
                cafeId: visit.cafeId,
                cafeName: cafeName
            )
        } catch {
            return
        }
    }

    private func observeCafeDetailEvent() {
        tasks[.cafeDetailEvent]?.cancel()
        tasks[.cafeDetailEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeDetailEventPublisher.events) {
                    if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                        self.patchCafe(updated.cafe)
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeCastEvent() {
        tasks[.castEvent]?.cancel()
        tasks[.castEvent] = Task {
            do {
                for try await event in asyncSequence(for: castEventPublisher.events) {
                    switch event {
                    case let updated as Shared.CastEvent.Updated:
                        self.patchCast(updated.cast)
                    case let deleted as Shared.CastEvent.Deleted:
                        self.uiState.popularCasts.removeAll { $0.id == deleted.castId }
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeVisitEvent() {
        tasks[.visitEvent]?.cancel()
        tasks[.visitEvent] = Task {
            do {
                for try await event in asyncSequence(for: visitEventPublisher.events) {
                    switch event {
                    case let _ as Shared.VisitEvent.Created:
                        refreshRecentVisitPage()
                    case let _ as Shared.VisitEvent.Deleted:
                        refreshRecentVisitPage()
                    default:
                        break
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
        uiState.popularCafes = uiState.popularCafes.map { item in
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
        uiState.popularCasts = uiState.popularCasts.map { item in
            guard item.cafeId == cafe.id else { return item }
            return CheckInCastSummary(
                id: item.id,
                cafeId: item.cafeId,
                cafeName: cafe.name,
                name: item.name,
                profileImage: item.profileImage,
                todayVisit: item.todayVisit
            )
        }
        uiState.recentVisits = uiState.recentVisits.map { item in
            if item.cafeId == cafe.id {
                return CheckInVisitEntry(
                    id: item.id,
                    cafeId: item.cafeId,
                    cafeName: cafe.name,
                    cafeImage: cafe.thumbnailImage ?? "",
                    visitedAt: item.visitedAt,
                    visitedLabel: item.visitedLabel,
                    memo: item.memo,
                    verified: item.verified
                )
            } else {
                return item
            }
        }
        uiState.todayVisits = uiState.todayVisits.map { item in
            if item.cafeId == cafe.id {
                return CheckInVisitEntry(
                    id: item.id,
                    cafeId: item.cafeId,
                    cafeName: cafe.name,
                    cafeImage: cafe.thumbnailImage ?? "",
                    visitedAt: item.visitedAt,
                    visitedLabel: item.visitedLabel,
                    memo: item.memo,
                    verified: item.verified
                )
            } else {
                return item
            }
        }
        if let prompt = uiState.reviewPrompt, prompt.cafeId == cafe.id {
            uiState.reviewPrompt = CheckInUiState.ReviewPrompt(
                visitId: prompt.visitId,
                cafeId: prompt.cafeId,
                cafeName: cafe.name
            )
        }
    }

    private func patchCast(_ cast: Cast) {
        uiState.popularCasts = uiState.popularCasts.map { item in
            guard item.id == cast.id else { return item }
            return CheckInCastSummary(
                id: item.id,
                cafeId: item.cafeId,
                cafeName: item.cafeName,
                name: cast.name,
                profileImage: cast.profileImage,
                todayVisit: item.todayVisit
            )
        }
    }

    private func dismissReviewPrompt() {
        guard let prompt = uiState.reviewPrompt else { return }
        tasks[.reviewPromptAction]?.cancel()
        tasks[.reviewPromptAction] = Task {
            _ = try? await dismissReviewPromptUseCase.invoke(visitId: prompt.visitId)
            uiState.reviewPrompt = nil
        }
    }

    private func writeReviewPrompt() {
        guard let prompt = uiState.reviewPrompt else { return }
        tasks[.reviewPromptAction]?.cancel()
        tasks[.reviewPromptAction] = Task {
            _ = try? await dismissReviewPromptUseCase.invoke(visitId: prompt.visitId)
            uiState.reviewPrompt = nil
            event.send(.navigateToReviewEdit(cafeId: prompt.cafeId))
        }
    }

    private func requestLocationPermissionOnEntry() {
        tasks[.locationPermission]?.cancel()
        tasks[.locationPermission] = Task {
            let permissionResult = await currentLocationProvider.requestPermissionIfNeeded()
            if !permissionResult.isGranted && permissionResult.requiresSettings {
                event.send(.openLocationSettings)
            }
        }
    }

    private func requestCheckInPermissionAndOpenSheet(preselectCafeId: String? = nil) {
        tasks[.locationPermission]?.cancel()
        tasks[.locationPermission] = Task {
            let permissionResult = await currentLocationProvider.requestPermissionIfNeeded()

            if permissionResult.isGranted {
                uiState.isNewVisitSheetVisible = true
                uiState.preselectCafeId = preselectCafeId
                uiState.errorMessage = nil

                detectUserCity()
            } else {
                uiState.isNewVisitSheetVisible = false
                uiState.errorMessage = permissionResult.message
                if permissionResult.requiresSettings {
                    event.send(.openLocationSettings)
                }
            }
        }
    }
    
    func onAction(_ action: CheckInAction) {
        switch action {
        case .cafeTapped(let id):
            if uiState.currentUser == nil {
                uiState.isLoginPromptVisible = true
                uiState.isNewVisitSheetVisible = false
            } else {
                event.send(.navigateToCafe(id: id))
            }
        case .castTapped(let id):
            if uiState.currentUser == nil {
                uiState.isLoginPromptVisible = true
                uiState.isNewVisitSheetVisible = false
            } else {
                event.send(.navigateToCast(id: id))
            }
        case .checkInTapped:
            if uiState.currentUser == nil {
                uiState.isLoginPromptVisible = true
                uiState.isNewVisitSheetVisible = false
            } else {
                requestCheckInPermissionAndOpenSheet()
            }
        case .checkInForCafeTapped(let cafeId):
            if uiState.currentUser == nil {
                uiState.isLoginPromptVisible = true
                uiState.isNewVisitSheetVisible = false
            } else {
                requestCheckInPermissionAndOpenSheet(preselectCafeId: cafeId)
            }
        case .signInTapped, .signUpTapped:
            uiState.isLoginPromptVisible = false
            event.send(.navigateToSignIn)
        case .dismissLoginPrompt:
            uiState.isLoginPromptVisible = false
        case .dismissError:
            uiState.errorMessage = nil
        case .dismissNewVisitSheet:
            uiState.isNewVisitSheetVisible = false
            uiState.preselectCafeId = nil
        case .dismissReviewPrompt:
            dismissReviewPrompt()
        case .writeReviewPromptTapped:
            writeReviewPrompt()
        case .loadMoreRecentVisits:
            loadMoreRecentVisitPage()
        case .submitNewVisit(let cafeId, let visitedAt, let memo):
            submitNewVisit(cafeId: cafeId, visitedAt: visitedAt, memo: memo)
        }
    }

    init(
        getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase = KoinInitializerKt.resolveGetCheckInGuestFeedUseCase(),
        getCheckInUserFeedUseCase: GetCheckInUserFeedUseCase = KoinInitializerKt.resolveGetCheckInUserFeedUseCase(),
        createVisitUseCase: CreateVisitUseCase = KoinInitializerKt.resolveCreateVisitUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        shouldShowReviewPromptUseCase: ShouldShowReviewPromptUseCase = KoinInitializerKt.resolveShouldShowReviewPromptUseCase(),
        dismissReviewPromptUseCase: DismissReviewPromptUseCase = KoinInitializerKt.resolveDismissReviewPromptUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher(),
        visitEventPublisher: VisitEventPublisher = KoinInitializerKt.resolveVisitEventPublisher()
    ) {
        self.getCheckInGuestFeedUseCase = getCheckInGuestFeedUseCase
        self.getCheckInUserFeedUseCase = getCheckInUserFeedUseCase
        self.createVisitUseCase = createVisitUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.shouldShowReviewPromptUseCase = shouldShowReviewPromptUseCase
        self.dismissReviewPromptUseCase = dismissReviewPromptUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.castEventPublisher = castEventPublisher
        self.visitEventPublisher = visitEventPublisher

        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
        observeVisitEvent()
        detectUserCity()
        loadGuestFeed()
        requestLocationPermissionOnEntry()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case guestFeed
        case recentVisitPage
        case submitVisit
        case session
        case cafeDetailEvent
        case castEvent
        case visitEvent
        case reviewPromptAction
        case locationPermission
        case detectCity
    }

    private static let todayVisitLimit = 4

    private static let recentVisitPageSize: Int32 = 12

    private static func cityKeyFromCoordinates(lat: Double, lng: Double) -> String? {
        if (37.4...37.7).contains(lat) && (126.7...127.2).contains(lng) { return "seoul" }
        if (35.0...35.4).contains(lat) && (128.8...129.3).contains(lng) { return "busan" }
        if (35.7...36.0).contains(lat) && (128.4...128.8).contains(lng) { return "daegu" }
        if (35.5...35.9).contains(lat) && (139.3...139.9).contains(lng) { return "tokyo" }
        if (34.5...34.9).contains(lat) && (135.3...135.7).contains(lng) { return "osaka" }
        return nil
    }
}
