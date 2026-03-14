package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.BannerEvent
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDetailEvent
import com.hhp227.concafe.domain.model.CastClaimEvent as CastClaimDomainEvent
import com.hhp227.concafe.domain.model.CastEvent as CastDomainEvent
import com.hhp227.concafe.domain.usecase.ApproveCastClaimUseCase
import com.hhp227.concafe.domain.usecase.DeleteCastUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCastClaimsForCafeUseCase
import com.hhp227.concafe.domain.usecase.ObserveBannerEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeDetailEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastClaimEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastEventUseCase
import com.hhp227.concafe.domain.usecase.RejectCastClaimUseCase

class CafeDashboardViewModel(
    private val cafeId: String,
    private val getCafeCastPageUseCase: GetCafeCastPageUseCase,
    private val getCafeDashboardUseCase: GetCafeDashboardUseCase,
    private val getPendingCastClaimsForCafeUseCase: GetPendingCastClaimsForCafeUseCase,
    private val approveCastClaimUseCase: ApproveCastClaimUseCase,
    private val rejectCastClaimUseCase: RejectCastClaimUseCase,
    private val deleteCastUseCase: DeleteCastUseCase,
    private val observeBannerEventUseCase: ObserveBannerEventUseCase,
    private val observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase,
    private val observeCastClaimEventUseCase: ObserveCastClaimEventUseCase,
    private val observeCastEventUseCase: ObserveCastEventUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeDashboardEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadCafeDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessage = null) }

            when (val result = getCafeDashboardUseCase.invoke(cafeId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cafe = result.data,
                            isLoading = false
                        )
                    }
                    refreshCastPreviews(resetMessage = false)
                    refreshClaimData(resetMessage = false)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            cafe = null,
                            castPreviews = emptyList(),
                            pendingCastClaims = emptyList(),
                            nextCastCursor = null,
                            hasMoreCasts = false,
                            isLoading = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun loadCastPage(cursor: String?, pageSize: Int, append: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingMoreCasts = append,
                    infoMessage = if (append) it.infoMessage else null
                )
            }

            when (val result = getCafeCastPageUseCase.invoke(cafeId, cursor, pageSize)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        val mergedItems = if (append) state.castPreviews + result.data.items else result.data.items
                        val nextSelectedCastId = state.selectedCastId?.takeIf { selectedId ->
                            mergedItems.any { it.id == selectedId }
                        }
                        state.copy(
                            castPreviews = mergedItems,
                            selectedCastId = nextSelectedCastId,
                            nextCastCursor = result.data.nextCursor,
                            hasMoreCasts = result.data.hasNext,
                            isLoadingMoreCasts = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMoreCasts = false,
                            infoMessage = "소속 캐스트 목록을 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun refreshCastPreviews(resetMessage: Boolean = true) {
        if (resetMessage) {
            _uiState.update { it.copy(infoMessage = null) }
        }
        loadCastPage(
            cursor = null,
            pageSize = getCafeCastPageUseCase.defaultPageSize(),
            append = false
        )
    }

    private fun clickLoadMoreCasts() {
        val currentState = _uiState.value
        if (currentState.isLoadingMoreCasts || !currentState.hasMoreCasts) return
        loadCastPage(
            cursor = currentState.nextCastCursor,
            pageSize = getCafeCastPageUseCase.defaultPageSize(),
            append = true
        )
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(CafeDashboardEvent.NavigateBack)
        }
    }

    private fun clickShortcut(shortcut: CafeDashboardShortcut) {
        when (shortcut) {
            CafeDashboardShortcut.CAFE_SETTINGS -> viewModelScope.launch {
                _event.emit(CafeDashboardEvent.NavigateToCafeInfoEdit(cafeId))
            }
            CafeDashboardShortcut.HOME_BANNER -> viewModelScope.launch {
                _event.emit(CafeDashboardEvent.NavigateToBannerEdit)
            }
            CafeDashboardShortcut.EVENT_MANAGEMENT -> viewModelScope.launch {
                _event.emit(CafeDashboardEvent.NavigateToNoticeEvent(cafeId))
            }
            CafeDashboardShortcut.MENU_GOODS -> viewModelScope.launch {
                _event.emit(CafeDashboardEvent.NavigateToMenuGoods(cafeId))
            }
            CafeDashboardShortcut.CAST_MANAGEMENT -> viewModelScope.launch {
                _event.emit(CafeDashboardEvent.NavigateToCastEdit(cafeId = cafeId))
            }
            CafeDashboardShortcut.CAST_SCHEDULE -> {
                val selectedCastId = _uiState.value.selectedCastId
                if (selectedCastId == null) {
                    _uiState.update {
                        it.copy(infoMessage = "출근표를 관리할 캐스트를 목록에서 선택해 주세요.")
                    }
                } else {
                    viewModelScope.launch {
                        _event.emit(CafeDashboardEvent.NavigateToSchedule(selectedCastId))
                    }
                }
            }
            CafeDashboardShortcut.EXTERNAL_LINKS -> {
                _uiState.update {
                    it.copy(infoMessage = "${shortcut.title} 연결은 다음 단계에서 이어집니다.")
                }
            }
        }
    }

    private fun dismissInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun clickCastSchedule(castId: String) {
        _uiState.update {
            it.copy(
                selectedCastId = if (it.selectedCastId == castId) null else castId,
                infoMessage = null
            )
        }
    }

    private fun clickDeleteCast() {
        val selectedCastId = _uiState.value.selectedCastId
        if (selectedCastId == null) {
            _uiState.update { it.copy(infoMessage = "삭제할 캐스트를 목록에서 선택해 주세요.") }
            return
        }
        _uiState.update { it.copy(isDeleteCastDialogVisible = true, infoMessage = null) }
    }

    private fun dismissDeleteCastDialog() {
        _uiState.update { it.copy(isDeleteCastDialogVisible = false) }
    }

    private fun confirmDeleteCast() {
        val selectedCastId = _uiState.value.selectedCastId
        if (selectedCastId == null) {
            _uiState.update {
                it.copy(
                    isDeleteCastDialogVisible = false,
                    infoMessage = "삭제할 캐스트를 목록에서 선택해 주세요."
                )
            }
            return
        }

        viewModelScope.launch {
            when (val result = deleteCastUseCase.invoke(selectedCastId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDeleteCastDialogVisible = false,
                            infoMessage = "캐스트 프로필을 삭제했습니다."
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isDeleteCastDialogVisible = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun clickApproveCastClaim(claimId: String) {
        viewModelScope.launch {
            when (val result = approveCastClaimUseCase.invoke(claimId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(infoMessage = "캐스트 프로필 연결 요청을 승인했습니다.") }
                    refreshClaimData(resetMessage = false)
                    refreshCastPreviews(resetMessage = false)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(infoMessage = result.error.toString()) }
                }
            }
        }
    }

    private fun clickRejectCastClaim(claimId: String) {
        viewModelScope.launch {
            when (val result = rejectCastClaimUseCase.invoke(claimId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(infoMessage = "캐스트 프로필 연결 요청을 반려했습니다.") }
                    refreshClaimData(resetMessage = false)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(infoMessage = result.error.toString()) }
                }
            }
        }
    }

    private fun refreshClaimData(resetMessage: Boolean = true) {
        if (resetMessage) {
            _uiState.update { it.copy(infoMessage = null) }
        }
        loadPendingCastClaims()
    }

    private fun loadPendingCastClaims() {
        viewModelScope.launch {
            when (val result = getPendingCastClaimsForCafeUseCase.invoke(cafeId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(pendingCastClaims = result.data) }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(pendingCastClaims = emptyList()) }
                }
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            observeCafeDetailEventUseCase.invoke().collectLatest { event ->
                when (event) {
                    is CafeDetailEvent.CafeInfoUpdated -> if (event.cafeId == cafeId) {
                        patchCafeInfo(event.cafe)
                    }
                    is CafeDetailEvent.MenuCreated,
                    is CafeDetailEvent.MenuUpdated,
                    is CafeDetailEvent.MenuDeleted,
                    is CafeDetailEvent.GoodsCreated,
                    is CafeDetailEvent.GoodsUpdated,
                    is CafeDetailEvent.GoodsDeleted -> Unit
                }
            }
        }
    }

    private fun observeBannerEvent() {
        jobs[TaskKey.OBSERVE_BANNER_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_BANNER_EVENT] = viewModelScope.launch {
            observeBannerEventUseCase.invoke().collectLatest { event ->
                when (event) {
                    is BannerEvent.Created -> if (event.banner.cafeId == cafeId) {
                        loadCafeDashboard()
                    }
                }
            }
        }
    }

    private fun patchCafeInfo(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                cafe = state.cafe?.copy(
                    name = cafe.name,
                    city = cafe.region.city,
                    rating = cafe.ratingAvg
                )
            )
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            observeCastEventUseCase.invoke().collectLatest { event ->
                when (event) {
                    is CastDomainEvent.Created -> if (event.cafeId == cafeId) {
                        refreshCastPreviews()
                    }
                    is CastDomainEvent.Updated -> if (event.cafeId == cafeId) {
                        _uiState.update { state ->
                            state.copy(
                                castPreviews = state.castPreviews.map { preview ->
                                    if (preview.id == event.cast.id) {
                                        preview.copy(name = event.cast.name)
                                    } else {
                                        preview
                                    }
                                }
                            )
                        }
                    }
                    is CastDomainEvent.Deleted -> if (event.cafeId == cafeId) {
                        _uiState.update { state ->
                            state.copy(
                                castPreviews = state.castPreviews.filterNot { it.id == event.castId },
                                selectedCastId = state.selectedCastId?.takeUnless { it == event.castId },
                                isDeleteCastDialogVisible = false
                            )
                        }
                        refreshClaimData(resetMessage = false)
                    }
                }
            }
        }
    }

    private fun observeCastClaimEvent() {
        jobs[TaskKey.OBSERVE_CAST_CLAIM_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_CLAIM_EVENT] = viewModelScope.launch {
            observeCastClaimEventUseCase.invoke().collectLatest { event ->
                when (event) {
                    is CastClaimDomainEvent.Created -> if (event.claim.cafeId == cafeId) {
                        refreshClaimData()
                    }
                    is CastClaimDomainEvent.Updated -> if (event.claim.cafeId == cafeId) {
                        refreshClaimData()
                    }
                }
            }
        }
    }

    fun onAction(action: CafeDashboardAction) {
        when (action) {
            CafeDashboardAction.ClickBack -> clickBack()
            is CafeDashboardAction.ClickShortcut -> clickShortcut(action.shortcut)
            is CafeDashboardAction.ClickCastSchedule -> clickCastSchedule(action.castId)
            CafeDashboardAction.ClickDeleteCast -> clickDeleteCast()
            CafeDashboardAction.ConfirmDeleteCast -> confirmDeleteCast()
            CafeDashboardAction.DismissDeleteCastDialog -> dismissDeleteCastDialog()
            is CafeDashboardAction.ClickApproveCastClaim -> clickApproveCastClaim(action.claimId)
            is CafeDashboardAction.ClickRejectCastClaim -> clickRejectCastClaim(action.claimId)
            CafeDashboardAction.ClickLoadMoreCasts -> clickLoadMoreCasts()
            CafeDashboardAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }

    init {
        observeBannerEvent()
        observeCafeDetailEvent()
        observeCastClaimEvent()
        observeCastEvent()
        loadCafeDashboard()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        OBSERVE_BANNER_EVENT,
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT,
        OBSERVE_CAST_CLAIM_EVENT
    }
}
