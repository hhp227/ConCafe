package com.hhp227.concafe.presentation.main.fanmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.di.*
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.event.publisher.CastClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.publisher.ScheduleManagementEventPublisher
import com.hhp227.concafe.domain.model.CastClaimCandidate
import com.hhp227.concafe.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.event.CastClaimEvent as CastClaimDomainEvent
import com.hhp227.concafe.domain.event.CastEvent as CastDomainEvent
import com.hhp227.concafe.domain.event.ScheduleManagementEvent as ScheduleManagementDomainEvent

class FanManagementViewModel(
    private val getFanManagementDataUseCase: GetFanManagementDataUseCase,
    private val createCastClaimUseCase: CreateCastClaimUseCase,
    private val sendFanAnnouncementUseCase: SendFanAnnouncementUseCase,
    private val getMyCastClaimStatusUseCase: GetMyCastClaimStatusUseCase,
    private val getMyRequestableCastPageUseCase: GetMyRequestableCastPageUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val castClaimEventPublisher: CastClaimEventPublisher,
    private val castEventPublisher: CastEventPublisher,
    private val scheduleManagementEventPublisher: ScheduleManagementEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(FanManagementUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<FanManagementEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun observeSession() {
        jobs[TaskKey.OBSERVE_SESSION]?.cancel()
        jobs[TaskKey.OBSERVE_SESSION] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                unbindCastEvent()
                loadFanManagement()
            }
        }
    }

    private fun observeCastClaimEvent() {
        jobs[TaskKey.OBSERVE_CAST_CLAIM_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_CLAIM_EVENT] = viewModelScope.launch {
            castClaimEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastClaimDomainEvent.Created,
                    is CastClaimDomainEvent.Updated -> refreshClaimUiOnly()
                }
            }
        }
    }

    private fun startClaimStatusPolling() {
        jobs[TaskKey.POLL_CLAIM_STATUS]?.cancel()
        jobs[TaskKey.POLL_CLAIM_STATUS] = viewModelScope.launch {
            while (isActive) {
                delay(CLAIM_STATUS_POLLING_INTERVAL_MILLIS)

                if (isActive) {
                    refreshClaimUiOnly()
                }
            }
        }
    }

    private fun bindCastEvent(castId: String) {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastDomainEvent.Created -> if (event.cast.id == castId) {
                        loadFanManagement()
                    }
                    is CastDomainEvent.Updated -> if (event.cast.id == castId) {
                        _uiState.update { state ->
                            val currentData = state.fanManagementData ?: return@update state
                            state.copy(
                                fanManagementData = currentData.copy(
                                    detail = currentData.detail.copy(cast = event.cast)
                                )
                            )
                        }
                    }
                    is CastDomainEvent.Deleted -> if (event.castId == castId) {
                        loadFanManagement()
                    }
                }
            }
        }
    }

    private fun bindScheduleManagementEvent(castId: String) {
        jobs[TaskKey.OBSERVE_SCHEDULE_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_SCHEDULE_EVENT] = viewModelScope.launch {
            scheduleManagementEventPublisher.events.collectLatest { event ->
                when (event) {
                    is ScheduleManagementDomainEvent.Updated -> if (event.castId == castId) {
                        loadFanManagement()
                    }
                }
            }
        }
    }

    private fun unbindCastEvent() {
        jobs.remove(TaskKey.OBSERVE_CAST_EVENT)?.cancel()
        jobs.remove(TaskKey.OBSERVE_SCHEDULE_EVENT)?.cancel()
    }

    private fun setInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private suspend fun resolveClaimUi(
        includeCandidatePage: Boolean,
        fallbackSheet: FanManagementUiState.CastClaimSheet?
    ): ClaimUiState {
        val claimStatusResult = getMyCastClaimStatusUseCase.invoke()

        if (claimStatusResult !is AppResult.Success) {
            return ClaimUiState(statusCard = null, claimSheet = null)
        }
        val status = claimStatusResult.data
        val cafeId = status.affiliatedCafeId
        val cafeName = status.affiliatedCafeName ?: "소속 카페"

        if (cafeId == null) {
            return ClaimUiState(statusCard = null, claimSheet = null)
        }
        val pendingClaim = status.pendingClaim
        val latestRejectedClaim = status.latestRejectedClaim
        val statusCard = when {
            status.hasLinkedProfile -> FanManagementUiState.CastClaimStatusCard(
                affiliatedCafeId = cafeId,
                affiliatedCafeName = cafeName,
                headline = "캐스트 프로필 연결 완료",
                body = "${status.linkedCastName ?: "내 프로필"}이(가) 소속 카페와 연결되어 있습니다.",
                accent = FanManagementUiState.Accent.LINKED
            )
            pendingClaim != null -> FanManagementUiState.CastClaimStatusCard(
                affiliatedCafeId = cafeId,
                affiliatedCafeName = cafeName,
                headline = "프로필 연결 승인 대기 중",
                body = "카페 운영자가 ${pendingClaim.createdAtLabel}에 접수된 요청을 확인 중입니다.",
                accent = FanManagementUiState.Accent.PENDING
            )
            latestRejectedClaim != null -> FanManagementUiState.CastClaimStatusCard(
                affiliatedCafeId = cafeId,
                affiliatedCafeName = cafeName,
                headline = "프로필 연결이 반려되었습니다",
                body = "소속 카페 대시보드에서 다시 신청할 수 있습니다.",
                accent = FanManagementUiState.Accent.REJECTED
            )
            status.hasRequestableCasts -> FanManagementUiState.CastClaimStatusCard(
                affiliatedCafeId = cafeId,
                affiliatedCafeName = cafeName,
                headline = "소속 카페 프로필 연결이 필요합니다",
                body = "카페 대시보드에서 내 캐스트 프로필을 선택해 연결 요청을 보내세요.",
                accent = FanManagementUiState.Accent.PENDING
            )
            else -> FanManagementUiState.CastClaimStatusCard(
                affiliatedCafeId = cafeId,
                affiliatedCafeName = cafeName,
                headline = "아직 연결 가능한 캐스트 프로필이 없습니다",
                body = "운영자가 캐스트 프로필을 만든 뒤 다시 연결 요청을 진행할 수 있습니다.",
                accent = FanManagementUiState.Accent.REJECTED
            )
        }
        val shouldLoadPage = !status.hasLinkedProfile && pendingClaim == null && status.hasRequestableCasts
        val shouldFetchCandidatePage = shouldLoadPage && (
            includeCandidatePage
                || fallbackSheet == null
                || fallbackSheet.affiliatedCafeId != cafeId
                || fallbackSheet.requestableCasts.isEmpty()
        )
        val initialCandidatePage: PagedResult<CastClaimCandidate>? = if (shouldFetchCandidatePage) {
            when (val pageResult = getMyRequestableCastPageUseCase.invoke(cursor = null)) {
                is AppResult.Success -> pageResult.data
                is AppResult.Failure -> null
            }
        } else {
            null
        }
        val initialCandidates = initialCandidatePage?.items.orEmpty()
        val selectedId = initialCandidates.firstOrNull()?.castId
        val claimSheet = when {
            status.hasLinkedProfile -> FanManagementUiState.CastClaimSheet(
                affiliatedCafeId = cafeId,
                affiliatedCafeName = cafeName,
                headline = "캐스트 프로필 연결 완료",
                body = "${status.linkedCastName ?: "내 프로필"}이(가) 이미 연결되어 있습니다.",
                requestableCasts = emptyList(),
                nextCursor = null,
                canLoadMore = false,
                isLoadingMore = false,
                selectedCastId = null,
                canSubmit = false
            )
            pendingClaim != null -> FanManagementUiState.CastClaimSheet(
                affiliatedCafeId = cafeId,
                affiliatedCafeName = cafeName,
                headline = "승인 대기 중",
                body = "카페 운영자가 ${pendingClaim.createdAtLabel}에 접수된 요청을 확인 중입니다.",
                requestableCasts = emptyList(),
                nextCursor = null,
                canLoadMore = false,
                isLoadingMore = false,
                selectedCastId = null,
                canSubmit = false
            )
            else -> FanManagementUiState.CastClaimSheet(
                affiliatedCafeId = cafeId,
                affiliatedCafeName = cafeName,
                headline = if (latestRejectedClaim != null) "다시 연결 요청하기" else "캐스트 프로필 연결",
                body = if (latestRejectedClaim != null) {
                    "반려된 이후 다시 신청할 수 있습니다. 연결할 프로필을 선택해 주세요."
                } else {
                    "연결할 캐스트 프로필을 선택하고 신청을 보내세요."
                },
                requestableCasts = if (shouldFetchCandidatePage) {
                    initialCandidates
                } else {
                    fallbackSheet?.requestableCasts.orEmpty()
                },
                nextCursor = if (shouldFetchCandidatePage) {
                    initialCandidatePage?.nextCursor
                } else {
                    fallbackSheet?.nextCursor
                },
                canLoadMore = if (shouldFetchCandidatePage) {
                    initialCandidatePage?.hasNext ?: false
                } else {
                    fallbackSheet?.canLoadMore ?: false
                },
                isLoadingMore = false,
                selectedCastId = if (shouldFetchCandidatePage) selectedId else fallbackSheet?.selectedCastId,
                canSubmit = if (shouldFetchCandidatePage) selectedId != null else fallbackSheet?.selectedCastId != null
            )
        }

        return ClaimUiState(statusCard = statusCard, claimSheet = claimSheet)
    }

    private fun refreshClaimUiOnly() {
        viewModelScope.launch {
            runCatching {
                resolveClaimUi(
                    includeCandidatePage = false,
                    fallbackSheet = _uiState.value.castClaimSheet
                )
            }.onSuccess { claimUiState ->
                _uiState.update { state ->
                    state.copy(
                        castClaimStatus = claimUiState.statusCard,
                        castClaimSheet = claimUiState.claimSheet,
                        isClaimSheetVisible = state.isClaimSheetVisible && claimUiState.claimSheet != null
                    )
                }
            }
        }
    }

    private fun loadFanManagement() {
        jobs[TaskKey.LOAD_FAN_MANAGEMENT]?.cancel()
        jobs[TaskKey.LOAD_FAN_MANAGEMENT] = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            val claimUiState = resolveClaimUi(
                includeCandidatePage = true,
                fallbackSheet = null
            )
            when (val result = getFanManagementDataUseCase.invoke()) {
                is AppResult.Success -> {
                    val data = result.data
                    val cast = data.detail.cast
                    bindCastEvent(cast.id)
                    bindScheduleManagementEvent(cast.id)
                    _uiState.value = FanManagementUiState(
                        isLoading = false,
                        errorMessage = null,
                        fanManagementData = data,
                        castClaimStatus = claimUiState.statusCard,
                        castClaimSheet = claimUiState.claimSheet,
                        infoMessage = null
                    )
                }
                is AppResult.Failure -> {
                    unbindCastEvent()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (claimUiState.statusCard == null) "팬관리 데이터를 불러오지 못했습니다." else null,
                            castClaimStatus = claimUiState.statusCard,
                            fanManagementData = null,
                            castClaimSheet = claimUiState.claimSheet
                        )
                    }
                }
            }
        }
    }

    private fun clickQuickAction(quickAction: FanManagementUiState.QuickAction) {
        when (quickAction) {
            FanManagementUiState.QuickAction.WORK_SCHEDULE -> {
                viewModelScope.launch {
                    _event.emit(FanManagementEvent.NavigateToSchedule)
                }
            }
            FanManagementUiState.QuickAction.CAFE_DASHBOARD -> clickClaimProfile()
        }
    }

    private fun clickClaimProfile() {
        if (_uiState.value.castClaimSheet != null) {
            _uiState.update { it.copy(isClaimSheetVisible = true, infoMessage = null) }
        }
    }

    private fun loadMoreClaimCandidates() {
        val currentSheet = _uiState.value.castClaimSheet ?: return
        val cursor = currentSheet.nextCursor ?: return
        if (!currentSheet.canLoadMore || currentSheet.isLoadingMore) return
        jobs[TaskKey.CLAIM_CANDIDATE_PAGE]?.cancel()
        jobs[TaskKey.CLAIM_CANDIDATE_PAGE] = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    castClaimSheet = state.castClaimSheet?.copy(isLoadingMore = true)
                )
            }
            when (val result = getMyRequestableCastPageUseCase.invoke(cursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        val sheet = state.castClaimSheet ?: return@update state
                        state.copy(
                            castClaimSheet = sheet.copy(
                                requestableCasts = sheet.requestableCasts + result.data.items,
                                nextCursor = result.data.nextCursor,
                                canLoadMore = result.data.hasNext,
                                isLoadingMore = false
                            )
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            castClaimSheet = state.castClaimSheet?.copy(isLoadingMore = false)
                        )
                    }
                }
            }
        }
    }

    private fun selectClaimCandidate(castId: String) {
        _uiState.update { state ->
            val sheet = state.castClaimSheet ?: return@update state
            state.copy(
                castClaimSheet = sheet.copy(
                    selectedCastId = castId,
                    canSubmit = true
                )
            )
        }
    }

    private fun dismissClaimSheet() {
        _uiState.update { it.copy(isClaimSheetVisible = false) }
    }

    private fun openAnnouncementSheet() {
        val currentState = _uiState.value
        val fanManagementData = currentState.fanManagementData
        val castClaimStatus = currentState.castClaimStatus

        if (fanManagementData == null || castClaimStatus == null) {
            setInfoMessage("소속 카페 연결 후 팬 공지를 작성할 수 있습니다.")
            return
        }
        if (castClaimStatus.accent != FanManagementUiState.Accent.LINKED) {
            setInfoMessage("소속 카페 연결 후 팬 공지를 작성할 수 있습니다.")
            return
        }
        _uiState.update { state ->
            state.copy(
                isAnnouncementSheetVisible = true,
                announcementTitle = "",
                announcementBody = "",
                isSendingAnnouncement = false,
                infoMessage = null
            )
        }
    }

    private fun submitAnnouncement() {
        val currentState = _uiState.value
        val fanManagementData = currentState.fanManagementData ?: run {
            setInfoMessage("소속 카페 연결 후 팬 공지를 작성할 수 있습니다.")
            return
        }
        val castClaimStatus = currentState.castClaimStatus ?: run {
            setInfoMessage("소속 카페 연결 후 팬 공지를 작성할 수 있습니다.")
            return
        }
        val cast = fanManagementData.detail.cast
        val title = currentState.announcementTitle.trim()
        val body = currentState.announcementBody.trim()

        if (castClaimStatus.accent != FanManagementUiState.Accent.LINKED) {
            setInfoMessage("소속 카페 연결 후 팬 공지를 작성할 수 있습니다.")
            return
        }
        if (title.isBlank() || body.isBlank()) {
            setInfoMessage("제목과 내용을 모두 입력해 주세요.")
            return
        }
        _uiState.update { state ->
            state.copy(
                isSendingAnnouncement = true,
                infoMessage = null
            )
        }
        viewModelScope.launch {
            when (
                val result = sendFanAnnouncementUseCase.invoke(
                    userId = fanManagementData.user.id,
                    cafeId = cast.cafeId,
                    castId = cast.id,
                    title = title,
                    body = body
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isAnnouncementSheetVisible = false,
                            announcementTitle = "",
                            announcementBody = "",
                            isSendingAnnouncement = false,
                            infoMessage = "팔로워에게 팬 공지를 전송했습니다."
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            isSendingAnnouncement = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun submitCastClaim() {
        val sheet = _uiState.value.castClaimSheet ?: return
        val castId = sheet.selectedCastId ?: return
        _uiState.update { state ->
            state.copy(
                castClaimSheet = sheet.copy(isSubmitting = true),
                infoMessage = null
            )
        }
        viewModelScope.launch {
            when (val result = createCastClaimUseCase.invoke(sheet.affiliatedCafeId, castId, null)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isClaimSheetVisible = false, infoMessage = "캐스트 프로필 연결 요청을 보냈습니다.") }
                    refreshClaimUiOnly()
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            castClaimSheet = state.castClaimSheet?.copy(isSubmitting = false),
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun clickRecentFollower(followerId: String) {
        val follower = uiState.value.fanManagementData?.followers?.firstOrNull { it.id == followerId } ?: return
        setInfoMessage("${follower.nickname} 팬 상세 화면은 다음 단계에서 연결합니다.")
    }

    private fun clickTopFan(@Suppress("UNUSED_PARAMETER") fanId: String) {
        setInfoMessage("TOP 팬 기능은 다음 단계에서 제공합니다.")
    }

    fun onAction(action: FanManagementAction) {
        when (action) {
            FanManagementAction.Refresh -> loadFanManagement()
            FanManagementAction.ClickClaimProfile -> clickClaimProfile()
            FanManagementAction.LoadMoreClaimCandidates -> loadMoreClaimCandidates()
            is FanManagementAction.SelectClaimCandidate -> selectClaimCandidate(action.castId)
            FanManagementAction.SubmitCastClaim -> submitCastClaim()
            FanManagementAction.DismissClaimSheet -> dismissClaimSheet()
            FanManagementAction.ClickEditProfile -> {
                val detail = uiState.value.fanManagementData?.detail ?: return
                viewModelScope.launch {
                    _event.emit(
                        FanManagementEvent.NavigateToCastEdit(
                            cafeId = detail.cast.cafeId,
                            castId = detail.cast.id
                        )
                    )
                }
            }
            FanManagementAction.ClickPrimaryAnnouncement -> {
                openAnnouncementSheet()
            }
            is FanManagementAction.ChangeAnnouncementTitle -> _uiState.update {
                it.copy(announcementTitle = action.value)
            }
            is FanManagementAction.ChangeAnnouncementBody -> _uiState.update {
                it.copy(announcementBody = action.value)
            }
            FanManagementAction.SubmitAnnouncement -> submitAnnouncement()
            FanManagementAction.DismissAnnouncementSheet -> _uiState.update {
                it.copy(isAnnouncementSheetVisible = false, isSendingAnnouncement = false)
            }
            is FanManagementAction.ClickQuickAction -> clickQuickAction(action.quickAction)
            FanManagementAction.ClickViewAllFollowers -> {
                setInfoMessage("전체 팔로워 목록은 다음 단계에서 제공합니다.")
            }
            is FanManagementAction.ClickRecentFollower -> clickRecentFollower(action.followerId)
            is FanManagementAction.ClickTopFan -> clickTopFan(action.fanId)
            FanManagementAction.DismissInfoMessage -> {
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        observeSession()
        observeCastClaimEvent()
        startClaimStatusPolling()
    }
}

private enum class TaskKey {
    LOAD_FAN_MANAGEMENT,
    OBSERVE_SESSION,
    OBSERVE_CAST_EVENT,
    OBSERVE_SCHEDULE_EVENT,
    OBSERVE_CAST_CLAIM_EVENT,
    CLAIM_CANDIDATE_PAGE,
    POLL_CLAIM_STATUS
}

private data class ClaimUiState(
    val statusCard: FanManagementUiState.CastClaimStatusCard?,
    val claimSheet: FanManagementUiState.CastClaimSheet?
)

private const val CLAIM_STATUS_POLLING_INTERVAL_MILLIS = 30_000L
