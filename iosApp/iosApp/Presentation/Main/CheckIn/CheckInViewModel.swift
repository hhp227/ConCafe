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

    private func loadUserFeed() {
        tasks[.userFeed]?.cancel()
        tasks[.userFeed] = Task {
            do {
                let result = try await getCheckInUserFeedUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CheckInUserFeed {
                    uiState.todayVisits = feed.todayVisits
                    uiState.recentVisits = feed.recentVisits
                } else if let failure = result as? AppResultFailure {
                    uiState.todayVisits = []
                    uiState.recentVisits = []
                    uiState.errorMessage = "\(failure.error)"
                } else {
                    uiState.todayVisits = []
                    uiState.recentVisits = []
                }
            } catch {
                if Task.isCancelled { return }
                uiState.todayVisits = []
                uiState.recentVisits = []
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
                    } else {
                        self.loadUserFeed()
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func submitNewVisit(cafeId: String, visitedAt: String, memo: String?) {
        if cafeId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            uiState.errorMessage = "카페를 선택해 주세요."
            return
        }
        if visitedAt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            uiState.errorMessage = "방문 시간을 입력해 주세요."
            return
        }

        uiState.errorMessage = nil

        tasks[.submitVisit]?.cancel()
        tasks[.submitVisit] = Task {
            do {
                let normalizedMemo = memo?.trimmingCharacters(in: .whitespacesAndNewlines)

                let result = try await createVisitUseCase.invoke(
                    cafeId: cafeId,
                    visitedAt: visitedAt,
                    memo: normalizedMemo?.isEmpty == true ? nil : normalizedMemo
                )

                if result is AppResultSuccess<AnyObject> {
                    uiState.isNewVisitSheetVisible = false
                    uiState.errorMessage = nil
                    loadUserFeed()
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

    private func patchCafe(_ cafe: Cafe) {
        uiState.mapCafes = uiState.mapCafes.map { item in
            guard item.id == cafe.id else { return item }
            return CheckInCafeSummary(
                id: item.id,
                name: cafe.name,
                locationLabel: cafe.region.city,
                geoPoint: item.geoPoint,
                rating: cafe.ratingAvg,
                checkInCount: item.checkInCount
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
                checkInCount: item.checkInCount
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
        Task {
            _ = try? await dismissReviewPromptUseCase.invoke(visitId: prompt.visitId)
            uiState.reviewPrompt = nil
        }
    }

    private func writeReviewPrompt() {
        guard let prompt = uiState.reviewPrompt else { return }
        Task {
            _ = try? await dismissReviewPromptUseCase.invoke(visitId: prompt.visitId)
            uiState.reviewPrompt = nil
            event.send(.navigateToReviewEdit(cafeId: prompt.cafeId))
        }
    }
    
    func onAction(_ action: CheckInAction) {
        switch action {
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .castTapped(let id):
            event.send(.navigateToCast(id: id))
        case .checkInTapped:
            if uiState.currentUser == nil {
                uiState.isLoginPromptVisible = true
                uiState.isNewVisitSheetVisible = false
            } else {
                uiState.isNewVisitSheetVisible = true
            }
        case .signInTapped, .signUpTapped:
            uiState.isLoginPromptVisible = false
            event.send(.navigateToSignIn)
        case .dismissLoginPrompt:
            uiState.isLoginPromptVisible = false
        case .dismissNewVisitSheet:
            uiState.isNewVisitSheetVisible = false
        case .dismissReviewPrompt:
            dismissReviewPrompt()
        case .writeReviewPromptTapped:
            writeReviewPrompt()
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
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher()
    ) {
        self.getCheckInGuestFeedUseCase = getCheckInGuestFeedUseCase
        self.getCheckInUserFeedUseCase = getCheckInUserFeedUseCase
        self.createVisitUseCase = createVisitUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.shouldShowReviewPromptUseCase = shouldShowReviewPromptUseCase
        self.dismissReviewPromptUseCase = dismissReviewPromptUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.castEventPublisher = castEventPublisher

        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
        loadGuestFeed()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case guestFeed
        case userFeed
        case submitVisit
        case session
        case cafeDetailEvent
        case castEvent
    }
}
