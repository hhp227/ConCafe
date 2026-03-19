//
//  FanManagementViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class FanManagementViewModel: ObservableObject {
    private let getFanManagementDataUseCase: GetFanManagementDataUseCase

    private let createCastClaimUseCase: CreateCastClaimUseCase

    private let getMyCastClaimStatusUseCase: GetMyCastClaimStatusUseCase

    private let getMyRequestableCastPageUseCase: GetMyRequestableCastPageUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let castClaimEventPublisher: CastClaimEventPublisher

    private let castEventPublisher: CastEventPublisher

    private let scheduleManagementEventPublisher: ScheduleManagementEventPublisher

    @Published private(set) var uiState = FanManagementUiState.empty

    let event = PassthroughSubject<FanManagementEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeSession() {
        tasks[.session]?.cancel()
        tasks[.session] = Task {
            do {
                for try await _ in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    self.unbindCastEvent()
                    self.loadFanManagement()
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }
    
    private func observeCastClaimEvent() {
        tasks[.castClaimEvent]?.cancel()
        tasks[.castClaimEvent] = Task {
            do {
                for try await event in asyncSequence(for: castClaimEventPublisher.events) {
                    switch event {
                    case is Shared.CastClaimEvent.Created, is Shared.CastClaimEvent.Updated:
                        self.loadFanManagement()
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func bindCastEvent(_ castId: String) {
        tasks[.castEvent]?.cancel()
        tasks[.castEvent] = Task {
            do {
                for try await event in asyncSequence(for: castEventPublisher.events) {
                    switch event {
                    case let event as Shared.CastEvent.Created:
                        if event.cast.id == castId {
                            self.loadFanManagement()
                        }
                    case let event as Shared.CastEvent.Updated:
                        if event.cast.id == castId, let currentData = self.uiState.fanManagementData {
                            self.uiState.fanManagementData = FanManagementData(
                                user: currentData.user,
                                detail: CastDetail(
                                    cast: event.cast,
                                    cafe: currentData.detail.cafe,
                                    images: currentData.detail.images,
                                    schedule: currentData.detail.schedule
                                ),
                                followers: currentData.followers
                            )
                        }
                    case let event as Shared.CastEvent.Deleted:
                        if event.castId == castId {
                            self.loadFanManagement()
                        }
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func bindScheduleManagementEvent(_ castId: String) {
        tasks[.scheduleEvent]?.cancel()
        tasks[.scheduleEvent] = Task {
            do {
                for try await event in asyncSequence(for: scheduleManagementEventPublisher.events) {
                    switch event {
                    case let event as Shared.ScheduleManagementEvent.Updated:
                        if event.castId == castId {
                            self.loadFanManagement()
                        }
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func unbindCastEvent() {
        tasks.removeValue(forKey: .castEvent)?.cancel()
        tasks.removeValue(forKey: .scheduleEvent)?.cancel()
    }

    private func setInfoMessage(_ message: String) {
        uiState.infoMessage = message
    }

    private func loadFanManagement() {
        uiState.isLoading = true
        uiState.errorMessage = nil
        uiState.infoMessage = nil

        Task {
            var claimStatus: FanManagementUiState.CastClaimStatusCard?
            var claimSheet: FanManagementUiState.CastClaimSheet?
            do {
                let claimResult = try await getMyCastClaimStatusUseCase.invoke()
                if let success = claimResult as? AppResultSuccess<AnyObject>,
                   let data = success.data as? Shared.MyCastClaimStatus {
                    claimStatus = Self.toStatusCard(data)
                    let initialPage: PagedResult<Shared.CastClaimCandidate>?
                    if Self.shouldLoadRequestableCastPage(data) {
                        let pageResult = try await getMyRequestableCastPageUseCase.invoke(cursor: nil)
                        if let pageSuccess = pageResult as? AppResultSuccess<AnyObject>,
                           let page = pageSuccess.data as? PagedResult<Shared.CastClaimCandidate> {
                            initialPage = page
                        } else {
                            initialPage = nil
                        }
                    } else {
                        initialPage = nil
                    }
                    claimSheet = Self.toSheet(data, initialCandidatePage: initialPage)
                } else {
                    claimStatus = nil
                    claimSheet = nil
                }
                let result = try await getFanManagementDataUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? Shared.FanManagementData {
                    let cast = data.detail.cast
                    bindCastEvent(cast.id)
                    bindScheduleManagementEvent(cast.id)
                    uiState = FanManagementUiState(
                        isLoading: false,
                        errorMessage: nil,
                        fanManagementData: data,
                        castClaimStatus: claimStatus,
                        castClaimSheet: claimSheet,
                        isClaimSheetVisible: false,
                        infoMessage: nil
                    )
                } else {
                    unbindCastEvent()
                    uiState = .empty
                    uiState.isLoading = false
                    uiState.errorMessage = claimStatus == nil ? "팬관리 데이터를 불러오지 못했습니다." : nil
                    uiState.castClaimStatus = claimStatus
                    uiState.castClaimSheet = claimSheet
                }
            } catch {
                if Task.isCancelled { return }
                unbindCastEvent()
                uiState = .empty
                uiState.isLoading = false
                uiState.errorMessage = claimStatus == nil ? "팬관리 데이터를 불러오지 못했습니다." : nil
                uiState.castClaimStatus = claimStatus
                uiState.castClaimSheet = claimSheet
            }
        }
    }

    private func clickQuickAction(_ quickAction: FanManagementUiState.QuickAction) {
        switch quickAction {
        case .workSchedule:
            event.send(.navigateToSchedule(castId: nil))
        case .cafeDashboard:
            clickClaimProfile()
        }
    }

    private func clickClaimProfile() {
        guard uiState.castClaimSheet != nil else { return }
        uiState.isClaimSheetVisible = true
        uiState.infoMessage = nil
    }

    private func loadMoreClaimCandidates() {
        guard let sheet = uiState.castClaimSheet,
              sheet.canLoadMore,
              !sheet.isLoadingMore,
              let cursor = sheet.nextCursor else { return }
        uiState.castClaimSheet = .init(
            affiliatedCafeId: sheet.affiliatedCafeId,
            affiliatedCafeName: sheet.affiliatedCafeName,
            headline: sheet.headline,
            body: sheet.body,
            requestableCasts: sheet.requestableCasts,
            nextCursor: sheet.nextCursor,
            canLoadMore: sheet.canLoadMore,
            isLoadingMore: true,
            selectedCastId: sheet.selectedCastId,
            canSubmit: sheet.canSubmit,
            isSubmitting: sheet.isSubmitting
        )
        Task {
            do {
                let result = try await getMyRequestableCastPageUseCase.invoke(cursor: cursor)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<Shared.CastClaimCandidate>,
                   let current = uiState.castClaimSheet {
                    uiState.castClaimSheet = .init(
                        affiliatedCafeId: current.affiliatedCafeId,
                        affiliatedCafeName: current.affiliatedCafeName,
                        headline: current.headline,
                        body: current.body,
                        requestableCasts: current.requestableCasts + ((page.items as? [Shared.CastClaimCandidate]) ?? []),
                        nextCursor: page.nextCursor,
                        canLoadMore: page.hasNext,
                        isLoadingMore: false,
                        selectedCastId: current.selectedCastId,
                        canSubmit: current.canSubmit,
                        isSubmitting: current.isSubmitting
                    )
                } else if let current = uiState.castClaimSheet {
                    uiState.castClaimSheet = .init(
                        affiliatedCafeId: current.affiliatedCafeId,
                        affiliatedCafeName: current.affiliatedCafeName,
                        headline: current.headline,
                        body: current.body,
                        requestableCasts: current.requestableCasts,
                        nextCursor: current.nextCursor,
                        canLoadMore: current.canLoadMore,
                        isLoadingMore: false,
                        selectedCastId: current.selectedCastId,
                        canSubmit: current.canSubmit,
                        isSubmitting: current.isSubmitting
                    )
                }
            } catch {
                if Task.isCancelled { return }
                if let current = uiState.castClaimSheet {
                    uiState.castClaimSheet = .init(
                        affiliatedCafeId: current.affiliatedCafeId,
                        affiliatedCafeName: current.affiliatedCafeName,
                        headline: current.headline,
                        body: current.body,
                        requestableCasts: current.requestableCasts,
                        nextCursor: current.nextCursor,
                        canLoadMore: current.canLoadMore,
                        isLoadingMore: false,
                        selectedCastId: current.selectedCastId,
                        canSubmit: current.canSubmit,
                        isSubmitting: current.isSubmitting
                    )
                }
            }
        }
    }

    private func selectClaimCandidate(_ castId: String) {
        guard let sheet = uiState.castClaimSheet else { return }
        uiState.castClaimSheet = .init(
            affiliatedCafeId: sheet.affiliatedCafeId,
            affiliatedCafeName: sheet.affiliatedCafeName,
            headline: sheet.headline,
            body: sheet.body,
            requestableCasts: sheet.requestableCasts,
            nextCursor: sheet.nextCursor,
            canLoadMore: sheet.canLoadMore,
            isLoadingMore: sheet.isLoadingMore,
            selectedCastId: castId,
            canSubmit: true,
            isSubmitting: sheet.isSubmitting
        )
    }

    private func dismissClaimSheet() {
        uiState.isClaimSheetVisible = false
    }

    private func submitCastClaim() {
        guard let sheet = uiState.castClaimSheet, let castId = sheet.selectedCastId else { return }
        uiState.castClaimSheet = .init(
            affiliatedCafeId: sheet.affiliatedCafeId,
            affiliatedCafeName: sheet.affiliatedCafeName,
            headline: sheet.headline,
            body: sheet.body,
            requestableCasts: sheet.requestableCasts,
            nextCursor: sheet.nextCursor,
            canLoadMore: sheet.canLoadMore,
            isLoadingMore: sheet.isLoadingMore,
            selectedCastId: sheet.selectedCastId,
            canSubmit: sheet.canSubmit,
            isSubmitting: true
        )
        Task {
            do {
                let result = try await createCastClaimUseCase.invoke(cafeId: sheet.affiliatedCafeId, castId: castId, message: nil)
                if result is AppResultSuccess<AnyObject> {
                    uiState.isClaimSheetVisible = false
                    uiState.infoMessage = "캐스트 프로필 연결 요청을 보냈습니다."
                    loadFanManagement()
                } else if let failure = result as? AppResultFailure {
                    uiState.infoMessage = "\(failure.error)"
                    if let current = uiState.castClaimSheet {
                        uiState.castClaimSheet = .init(
                            affiliatedCafeId: current.affiliatedCafeId,
                            affiliatedCafeName: current.affiliatedCafeName,
                            headline: current.headline,
                            body: current.body,
                            requestableCasts: current.requestableCasts,
                            nextCursor: current.nextCursor,
                            canLoadMore: current.canLoadMore,
                            isLoadingMore: current.isLoadingMore,
                            selectedCastId: current.selectedCastId,
                            canSubmit: current.canSubmit,
                            isSubmitting: false
                        )
                    }
                }
            } catch {
                if Task.isCancelled { return }
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func clickRecentFollower(_ id: String) {
        guard let follower = uiState.fanManagementData?.followers.first(where: { $0.id == id }) else { return }
        setInfoMessage("\(follower.nickname) 팬 상세 화면은 다음 단계에서 연결합니다.")
    }

    private func clickTopFan(_ id: String) {
        setInfoMessage("TOP 팬 기능은 다음 단계에서 제공합니다.")
    }

    func onAction(_ action: FanManagementAction) {
        switch action {
        case .clickClaimProfile:
            clickClaimProfile()
        case .loadMoreClaimCandidates:
            loadMoreClaimCandidates()
        case .selectClaimCandidate(let castId):
            selectClaimCandidate(castId)
        case .submitCastClaim:
            submitCastClaim()
        case .dismissClaimSheet:
            dismissClaimSheet()
        case .clickEditProfile:
            guard let detail = uiState.fanManagementData?.detail else { return }
            event.send(.navigateToCastEdit(cafeId: detail.cast.cafeId, castId: detail.cast.id))
        case .clickPrimaryAnnouncement:
            setInfoMessage("팬 공지 작성 흐름은 다음 단계에서 연결합니다.")
        case .clickQuickAction(let quickAction):
            clickQuickAction(quickAction)
        case .clickViewAllFollowers:
            setInfoMessage("전체 팔로워 목록은 다음 단계에서 제공합니다.")
        case .clickRecentFollower(let id):
            clickRecentFollower(id)
        case .clickTopFan(let id):
            clickTopFan(id)
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        getFanManagementDataUseCase: GetFanManagementDataUseCase = KoinInitializerKt.resolveGetFanManagementDataUseCase(),
        createCastClaimUseCase: CreateCastClaimUseCase = KoinInitializerKt.resolveCreateCastClaimUseCase(),
        getMyCastClaimStatusUseCase: GetMyCastClaimStatusUseCase = KoinInitializerKt.resolveGetMyCastClaimStatusUseCase(),
        getMyRequestableCastPageUseCase: GetMyRequestableCastPageUseCase = KoinInitializerKt.resolveGetMyRequestableCastPageUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        castClaimEventPublisher: CastClaimEventPublisher = KoinInitializerKt.resolveCastClaimEventPublisher(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher(),
        scheduleManagementEventPublisher: ScheduleManagementEventPublisher = KoinInitializerKt.resolveScheduleManagementEventPublisher()
    ) {
        self.getFanManagementDataUseCase = getFanManagementDataUseCase
        self.createCastClaimUseCase = createCastClaimUseCase
        self.getMyCastClaimStatusUseCase = getMyCastClaimStatusUseCase
        self.getMyRequestableCastPageUseCase = getMyRequestableCastPageUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.castClaimEventPublisher = castClaimEventPublisher
        self.castEventPublisher = castEventPublisher
        self.scheduleManagementEventPublisher = scheduleManagementEventPublisher

        observeSession()
        observeCastClaimEvent()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case session
        case castClaimEvent
        case castEvent
        case scheduleEvent
    }

    private static func toStatusCard(_ status: Shared.MyCastClaimStatus) -> FanManagementUiState.CastClaimStatusCard? {
        guard let cafeId = status.affiliatedCafeId,
              let cafeName = status.affiliatedCafeName else { return nil }
        if status.hasLinkedProfile {
            return .init(
                affiliatedCafeId: cafeId,
                affiliatedCafeName: cafeName,
                headline: "캐스트 프로필 연결 완료",
                body: "\(status.linkedCastName ?? "내 프로필")이(가) 소속 카페와 연결되어 있습니다.",
                accent: .linked
            )
        }
        if let pending = status.pendingClaim {
            return .init(
                affiliatedCafeId: cafeId,
                affiliatedCafeName: cafeName,
                headline: "프로필 연결 승인 대기 중",
                body: "카페 운영자가 \(pending.createdAtLabel)에 접수된 요청을 확인 중입니다.",
                accent: .pending
            )
        }
        if status.latestRejectedClaim != nil {
            return .init(
                affiliatedCafeId: cafeId,
                affiliatedCafeName: cafeName,
                headline: "프로필 연결이 반려되었습니다",
                body: "팬관리에서 다시 신청할 수 있습니다.",
                accent: .rejected
            )
        }
        if status.hasRequestableCasts {
            return .init(
                affiliatedCafeId: cafeId,
                affiliatedCafeName: cafeName,
                headline: "소속 카페 프로필 연결이 필요합니다",
                body: "팬관리에서 내 캐스트 프로필을 선택해 연결 요청을 보내세요.",
                accent: .pending
            )
        }
        return .init(
            affiliatedCafeId: cafeId,
            affiliatedCafeName: cafeName,
            headline: "아직 연결 가능한 캐스트 프로필이 없습니다",
            body: "운영자가 캐스트 프로필을 만든 뒤 다시 연결 요청을 진행할 수 있습니다.",
            accent: .rejected
        )
    }

    private static func toSheet(
        _ status: Shared.MyCastClaimStatus,
        initialCandidatePage: PagedResult<Shared.CastClaimCandidate>?
    ) -> FanManagementUiState.CastClaimSheet? {
        guard let cafeId = status.affiliatedCafeId, let cafeName = status.affiliatedCafeName else { return nil }
        let initialCandidates = (initialCandidatePage?.items as? [Shared.CastClaimCandidate]) ?? []
        let selectedId = initialCandidates.first?.castId
        if status.hasLinkedProfile {
            return .init(affiliatedCafeId: cafeId, affiliatedCafeName: cafeName, headline: "캐스트 프로필 연결 완료", body: "\(status.linkedCastName ?? "내 프로필")이(가) 이미 연결되어 있습니다.", requestableCasts: [], nextCursor: nil, canLoadMore: false, isLoadingMore: false, selectedCastId: nil, canSubmit: false, isSubmitting: false)
        }
        if let pending = status.pendingClaim {
            return .init(affiliatedCafeId: cafeId, affiliatedCafeName: cafeName, headline: "승인 대기 중", body: "카페 운영자가 \(pending.createdAtLabel)에 접수된 요청을 확인 중입니다.", requestableCasts: [], nextCursor: nil, canLoadMore: false, isLoadingMore: false, selectedCastId: nil, canSubmit: false, isSubmitting: false)
        }
        return .init(
            affiliatedCafeId: cafeId,
            affiliatedCafeName: cafeName,
            headline: status.latestRejectedClaim == nil ? "캐스트 프로필 연결" : "다시 연결 요청하기",
            body: status.latestRejectedClaim == nil ? "연결할 캐스트 프로필을 선택하고 신청을 보내세요." : "반려된 이후 다시 신청할 수 있습니다. 연결할 프로필을 선택해 주세요.",
            requestableCasts: initialCandidates,
            nextCursor: initialCandidatePage?.nextCursor,
            canLoadMore: initialCandidatePage?.hasNext ?? false,
            isLoadingMore: false,
            selectedCastId: selectedId,
            canSubmit: selectedId != nil,
            isSubmitting: false
        )
    }

    private static func shouldLoadRequestableCastPage(_ status: Shared.MyCastClaimStatus) -> Bool {
        !status.hasLinkedProfile && status.pendingClaim == nil && status.hasRequestableCasts
    }
}
