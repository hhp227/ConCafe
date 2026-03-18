package com.hhp227.concafe.presentation.main.cafemanagement

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
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.usecase.CreateCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeRegistrationClaimEventPublisher
import com.hhp227.concafe.domain.usecase.GetCafeManagementUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase

class CafeManagementViewModel(
    private val createCafeOwnerClaimUseCase: CreateCafeOwnerClaimUseCase,
    private val getCafeManagementUseCase: GetCafeManagementUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeManagementUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeManagementEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()
    private var currentUserId: String? = null

    private fun loadCafeManagement() {
        viewModelScope.launch {
            when (val result = getCafeManagementUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            ownedCafes = result.data.ownedCafes,
                            searchableCafes = result.data.searchableCafes,
                            pendingClaims = result.data.pendingClaims
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            ownedCafes = emptyList(),
                            searchableCafes = emptyList(),
                            pendingClaims = emptyList(),
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun clickCafe(cafeId: String) {
        viewModelScope.launch {
            _event.emit(CafeManagementEvent.NavigateToCafeDashboard(cafeId))
        }
    }

    private fun clickCafeDetail(cafeId: String) {
        viewModelScope.launch {
            _event.emit(CafeManagementEvent.NavigateToCafe(cafeId))
        }
    }

    private fun changeCafeSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                cafeSearchQuery = query,
                infoMessage = null
            )
        }
    }

    private fun clickClaimCafe(cafeId: String) {
        val cafeName = _uiState.value.searchableCafes.firstOrNull { it.id == cafeId }?.name ?: "선택한 카페"
        viewModelScope.launch {
            when (val result = createCafeOwnerClaimUseCase.invoke(cafeId)) {
                is AppResult.Success -> {
                    loadCafeManagement()
                    _uiState.update {
                        it.copy(infoMessage = "$cafeName 운영자 신청을 등록했습니다.")
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(infoMessage = result.error.toString())
                    }
                }
            }
        }
    }

    private fun toggleCafeListExpanded() {
        _uiState.update {
            it.copy(
                isShowingAllCafes = !it.isShowingAllCafes
            )
        }
    }

    private fun clickCreateCafe() {
        viewModelScope.launch { _event.emit(CafeManagementEvent.NavigateToCafeInfoRegistration) }
    }

    private fun dismissInfoMessage() {
        _uiState.update {
            it.copy(
                infoMessage = null
            )
        }
    }

    private fun observeSession() {
        jobs[TaskKey.OBSERVE_SESSION]?.cancel()
        jobs[TaskKey.OBSERVE_SESSION] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                currentUserId = user?.id
                loadCafeManagement()
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.observe().collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafeInfo(event.cafe)
                }
            }
        }
    }

    private fun observeCafeRegistrationClaimEvent() {
        jobs[TaskKey.OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT] = viewModelScope.launch {
            cafeRegistrationClaimEventPublisher.observe().collectLatest { claimEvent ->
                val userId = currentUserId ?: return@collectLatest
                val shouldRefresh = when (claimEvent) {
                    is CafeRegistrationClaimEvent.Created -> claimEvent.requesterUserId == userId
                    is CafeRegistrationClaimEvent.Approved -> claimEvent.requesterUserId == userId
                    is CafeRegistrationClaimEvent.Rejected -> claimEvent.requesterUserId == userId
                }
                if (shouldRefresh) {
                    loadCafeManagement()
                }
            }
        }
    }

    private fun patchCafeInfo(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                ownedCafes = state.ownedCafes.map { item ->
                    if (item.id == cafe.id) {
                        item.copy(
                            name = cafe.name,
                            city = cafe.region.city,
                            rating = cafe.ratingAvg
                        )
                    } else {
                        item
                    }
                },
                searchableCafes = state.searchableCafes.map { item ->
                    if (item.id == cafe.id) {
                        item.copy(
                            name = cafe.name,
                            location = "${cafe.region.city} ${cafe.region.address}"
                        )
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun onAction(action: CafeManagementAction) {
        when (action) {
            is CafeManagementAction.ClickCafe -> clickCafe(action.cafeId)
            is CafeManagementAction.ClickCafeDetail -> clickCafeDetail(action.cafeId)
            is CafeManagementAction.ChangeCafeSearchQuery -> changeCafeSearchQuery(action.query)
            is CafeManagementAction.ClickClaimCafe -> clickClaimCafe(action.cafeId)
            CafeManagementAction.ToggleCafeListExpanded -> toggleCafeListExpanded()
            CafeManagementAction.ClickCreateCafe -> clickCreateCafe()
            CafeManagementAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }

    init {
        observeSession()
        observeCafeDetailEvent()
        observeCafeRegistrationClaimEvent()
        loadCafeManagement()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        OBSERVE_SESSION,
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAFE_REGISTRATION_CLAIM_EVENT
    }
}
