package com.hhp227.concafe.presentation.main.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeOwnerClaimEvent
import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.hhp227.concafe.domain.event.publisher.CafeOwnerClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeRegistrationClaimEventPublisher
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.usecase.ApproveCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.ApproveCafeRegistrationClaimUseCase
import com.hhp227.concafe.domain.usecase.GetAdminInquiryPageUseCase
import com.hhp227.concafe.domain.usecase.GetAdminReportPageUseCase
import com.hhp227.concafe.domain.usecase.GetAdminOperationsMetricsUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCafeOwnerClaimsUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCafeRegistrationClaimsUseCase
import com.hhp227.concafe.domain.usecase.RejectCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.RejectCafeRegistrationClaimUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdminOperationsViewModel(
    private val getPendingCafeRegistrationClaimsUseCase: GetPendingCafeRegistrationClaimsUseCase,
    private val getPendingCafeOwnerClaimsUseCase: GetPendingCafeOwnerClaimsUseCase,
    private val getAdminOperationsMetricsUseCase: GetAdminOperationsMetricsUseCase,
    private val getAdminInquiryPageUseCase: GetAdminInquiryPageUseCase,
    private val getAdminReportPageUseCase: GetAdminReportPageUseCase,
    private val approveCafeRegistrationClaimUseCase: ApproveCafeRegistrationClaimUseCase,
    private val approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase,
    private val rejectCafeRegistrationClaimUseCase: RejectCafeRegistrationClaimUseCase,
    private val rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase,
    private val cafeOwnerClaimEventPublisher: CafeOwnerClaimEventPublisher,
    private val cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminOperationsUiState())
    val uiState: StateFlow<AdminOperationsUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AdminOperationsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadPendingRequests() {
        viewModelScope.launch {
            val registrationResult = getPendingCafeRegistrationClaimsUseCase.invoke()
            val ownerClaimResult = getPendingCafeOwnerClaimsUseCase.invoke()
            val adminMetricsResult = getAdminOperationsMetricsUseCase.invoke()

            when {
                registrationResult is AppResult.Success
                    && ownerClaimResult is AppResult.Success
                    && adminMetricsResult is AppResult.Success -> {
                    val registrationClaims = registrationResult.data.sortedByDescending { it.requestedAt }
                    val ownerClaims = ownerClaimResult.data.sortedByDescending { it.requestedAt }
                    val totalUsersCount = adminMetricsResult.data.totalUsersCount
                    val activeCafesCount = adminMetricsResult.data.activeCafesCount
                    val reportItemsCount = adminMetricsResult.data.reportItemsCount
                    val pendingCount = registrationClaims.size + ownerClaims.size

                    _uiState.update { state ->
                        state.copy(
                            totalUsersCount = totalUsersCount,
                            activeCafesCount = activeCafesCount,
                            reportItemsCount = reportItemsCount,
                            pendingCafeRegistrationClaims = registrationClaims,
                            pendingCafeOwnerClaims = ownerClaims,
                            metrics = buildAdminMetrics(
                                totalUsersCount = totalUsersCount,
                                activeCafesCount = activeCafesCount,
                                pendingCount = pendingCount,
                                reportItemsCount = reportItemsCount
                            ),
                            infoMessage = null
                        )
                    }
                    AdminPendingCache.snapshot = AdminPendingSnapshot(
                        registrationClaims = registrationClaims,
                        ownerClaims = ownerClaims,
                        totalUsersCount = totalUsersCount,
                        activeCafesCount = activeCafesCount,
                        reportItemsCount = reportItemsCount
                    )
                }
                registrationResult is AppResult.Failure -> {
                    _uiState.update { state -> state.copy(infoMessage = registrationResult.error.toString()) }
                }
                ownerClaimResult is AppResult.Failure -> {
                    _uiState.update { state -> state.copy(infoMessage = ownerClaimResult.error.toString()) }
                }
                adminMetricsResult is AppResult.Failure -> {
                    _uiState.update { state -> state.copy(infoMessage = adminMetricsResult.error.toString()) }
                }
            }
        }
    }

    private fun refreshPendingClaimsOnly() {
        viewModelScope.launch {
            val registrationResult = getPendingCafeRegistrationClaimsUseCase.invoke()
            val ownerClaimResult = getPendingCafeOwnerClaimsUseCase.invoke()

            if (registrationResult is AppResult.Success && ownerClaimResult is AppResult.Success) {
                val registrationClaims = registrationResult.data.sortedByDescending { it.requestedAt }
                val ownerClaims = ownerClaimResult.data.sortedByDescending { it.requestedAt }

                _uiState.update { state ->
                    val pendingCount = registrationClaims.size + ownerClaims.size
                    state.copy(
                        pendingCafeRegistrationClaims = registrationClaims,
                        pendingCafeOwnerClaims = ownerClaims,
                        metrics = buildAdminMetrics(
                            totalUsersCount = state.totalUsersCount,
                            activeCafesCount = state.activeCafesCount,
                            pendingCount = pendingCount,
                            reportItemsCount = state.reportItemsCount
                        )
                    )
                }
            }
        }
    }

    private fun showCachedPendingRequests() {
        val snapshot = AdminPendingCache.snapshot ?: return
        _uiState.update { state ->
            val pendingCount = snapshot.registrationClaims.size + snapshot.ownerClaims.size
            state.copy(
                totalUsersCount = snapshot.totalUsersCount,
                activeCafesCount = snapshot.activeCafesCount,
                reportItemsCount = snapshot.reportItemsCount,
                pendingCafeRegistrationClaims = snapshot.registrationClaims,
                pendingCafeOwnerClaims = snapshot.ownerClaims,
                metrics = buildAdminMetrics(
                    totalUsersCount = snapshot.totalUsersCount,
                    activeCafesCount = snapshot.activeCafesCount,
                    pendingCount = pendingCount,
                    reportItemsCount = snapshot.reportItemsCount
                )
            )
        }
    }

    private fun loadInitialInquiries() {
        loadInquiryPage(cursor = null, append = false)
    }

    private fun loadInitialReports() {
        loadReportPage(cursor = null, append = false)
    }

    private fun loadMoreInquiries() {
        val state = _uiState.value
        val cursor = state.inquiryNextCursor
        if (cursor == null || !state.canLoadMoreInquiries || state.isLoadingMoreInquiries) {
            return
        }
        loadInquiryPage(cursor = cursor, append = true)
    }

    private fun loadMoreReports() {
        val state = _uiState.value
        val cursor = state.reportNextCursor
        if (cursor == null || !state.canLoadMoreReports || state.isLoadingMoreReports) return
        loadReportPage(cursor = cursor, append = true)
    }

    private fun loadInquiryPage(cursor: String?, append: Boolean) {
        jobs[TaskKey.INQUIRY_PAGE]?.cancel()
        jobs[TaskKey.INQUIRY_PAGE] = viewModelScope.launch {
            if (append) {
                _uiState.update { state ->
                    state.copy(isLoadingMoreInquiries = true)
                }
                delay(PAGINATION_DELAY_MILLIS)
            }
            val result = getAdminInquiryPageUseCase.invoke(
                cursor = cursor,
                pageSize = ADMIN_INQUIRY_PAGE_SIZE
            )
            when (result) {
                is AppResult.Success -> {
                    val sortedItems = result.data.items.sortedByDescending { inquiry ->
                        inquiry.createdAt
                    }
                    _uiState.update { state ->
                        state.copy(
                            inquiries = if (append) {
                                state.inquiries + sortedItems
                            } else {
                                sortedItems
                            },
                            inquiryNextCursor = result.data.nextCursor,
                            canLoadMoreInquiries = result.data.hasNext,
                            isLoadingMoreInquiries = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoadingMoreInquiries = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun loadReportPage(cursor: String?, append: Boolean) {
        jobs[TaskKey.REPORT_PAGE]?.cancel()
        jobs[TaskKey.REPORT_PAGE] = viewModelScope.launch {
            if (append) {
                _uiState.update { it.copy(isLoadingMoreReports = true) }
                delay(PAGINATION_DELAY_MILLIS)
            }
            when (val result = getAdminReportPageUseCase.invoke(cursor = cursor, pageSize = ADMIN_REPORT_PAGE_SIZE)) {
                is AppResult.Success -> {
                    val sortedItems = result.data.items.sortedByDescending { it.createdAt }
                    _uiState.update { state ->
                        state.copy(
                            reports = if (append) state.reports + sortedItems else sortedItems,
                            reportNextCursor = result.data.nextCursor,
                            canLoadMoreReports = result.data.hasNext,
                            isLoadingMoreReports = false
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoadingMoreReports = false, infoMessage = result.error.toString())
                }
            }
        }
    }

    private fun observeClaimEvents() {
        jobs[TaskKey.CLAIM_EVENT]?.cancel()
        jobs[TaskKey.CLAIM_EVENT] = viewModelScope.launch {
            cafeRegistrationClaimEventPublisher.events.collectLatest { claimEvent ->
                when (claimEvent) {
                    is CafeRegistrationClaimEvent.Created -> refreshPendingClaimsOnly()
                    is CafeRegistrationClaimEvent.Approved -> applyRegistrationApprovalLocally(claimEvent.claimId)
                    is CafeRegistrationClaimEvent.Rejected -> removeRegistrationClaimLocally(claimEvent.claimId)
                }
            }
        }
    }

    private fun observeOwnerClaimEvents() {
        jobs[TaskKey.OWNER_CLAIM_EVENT]?.cancel()
        jobs[TaskKey.OWNER_CLAIM_EVENT] = viewModelScope.launch {
            cafeOwnerClaimEventPublisher.events.collectLatest { claimEvent ->
                when (claimEvent) {
                    is CafeOwnerClaimEvent.Created -> refreshPendingClaimsOnly()
                    is CafeOwnerClaimEvent.Approved -> removeOwnerClaimLocally(claimEvent.claimId)
                    is CafeOwnerClaimEvent.Rejected -> removeOwnerClaimLocally(claimEvent.claimId)
                }
            }
        }
    }

    private fun startClaimPolling() {
        jobs[TaskKey.CLAIM_POLLING]?.cancel()
        jobs[TaskKey.CLAIM_POLLING] = viewModelScope.launch {
            while (isActive) {
                delay(ADMIN_CLAIM_POLLING_INTERVAL_MILLIS)

                if (isActive) {
                    refreshPendingClaimsOnly()
                }
            }
        }
    }

    private fun removeRegistrationClaimLocally(claimId: String) {
        _uiState.update { state ->
            val nextRegistrationClaims = state.pendingCafeRegistrationClaims.filterNot { it.claimId == claimId }
            val nextOwnerClaims = state.pendingCafeOwnerClaims
            val pendingCount = nextRegistrationClaims.size + nextOwnerClaims.size
            val nextState = state.copy(
                pendingCafeRegistrationClaims = nextRegistrationClaims,
                pendingCafeOwnerClaims = nextOwnerClaims,
                metrics = buildAdminMetrics(
                    totalUsersCount = state.totalUsersCount,
                    activeCafesCount = state.activeCafesCount,
                    pendingCount = pendingCount,
                    reportItemsCount = state.reportItemsCount
                )
            )
            AdminPendingCache.snapshot = AdminPendingSnapshot(
                registrationClaims = nextRegistrationClaims,
                ownerClaims = nextOwnerClaims,
                totalUsersCount = state.totalUsersCount,
                activeCafesCount = state.activeCafesCount,
                reportItemsCount = state.reportItemsCount
            )
            nextState
        }
    }

    private fun applyRegistrationApprovalLocally(claimId: String) {
        _uiState.update { state ->
            val hadClaim = state.pendingCafeRegistrationClaims.any { it.claimId == claimId }
            val nextRegistrationClaims = state.pendingCafeRegistrationClaims.filterNot { it.claimId == claimId }
            val nextOwnerClaims = state.pendingCafeOwnerClaims
            val nextActiveCafesCount = if (hadClaim) {
                state.activeCafesCount + 1
            } else {
                state.activeCafesCount
            }
            val pendingCount = nextRegistrationClaims.size + nextOwnerClaims.size
            val nextState = state.copy(
                activeCafesCount = nextActiveCafesCount,
                pendingCafeRegistrationClaims = nextRegistrationClaims,
                pendingCafeOwnerClaims = nextOwnerClaims,
                metrics = buildAdminMetrics(
                    totalUsersCount = state.totalUsersCount,
                    activeCafesCount = nextActiveCafesCount,
                    pendingCount = pendingCount,
                    reportItemsCount = state.reportItemsCount
                )
            )
            AdminPendingCache.snapshot = AdminPendingSnapshot(
                registrationClaims = nextRegistrationClaims,
                ownerClaims = nextOwnerClaims,
                totalUsersCount = state.totalUsersCount,
                activeCafesCount = nextActiveCafesCount,
                reportItemsCount = state.reportItemsCount
            )
            nextState
        }
    }

    private fun removeOwnerClaimLocally(claimId: String) {
        _uiState.update { state ->
            val nextRegistrationClaims = state.pendingCafeRegistrationClaims
            val nextOwnerClaims = state.pendingCafeOwnerClaims.filterNot { it.claimId == claimId }
            val pendingCount = nextRegistrationClaims.size + nextOwnerClaims.size
            val nextState = state.copy(
                pendingCafeRegistrationClaims = nextRegistrationClaims,
                pendingCafeOwnerClaims = nextOwnerClaims,
                metrics = buildAdminMetrics(
                    totalUsersCount = state.totalUsersCount,
                    activeCafesCount = state.activeCafesCount,
                    pendingCount = pendingCount,
                    reportItemsCount = state.reportItemsCount
                )
            )
            AdminPendingCache.snapshot = AdminPendingSnapshot(
                registrationClaims = nextRegistrationClaims,
                ownerClaims = nextOwnerClaims,
                totalUsersCount = state.totalUsersCount,
                activeCafesCount = state.activeCafesCount,
                reportItemsCount = state.reportItemsCount
            )
            nextState
        }
    }

    private fun handlePendingResult(id: String, approved: Boolean) {
        val state = _uiState.value
        val selectedFilter = state.selectedPendingFilter
        val requestTitle = when (selectedFilter) {
            PendingFilter.CAFE_REGISTRATION -> {
                state.pendingCafeRegistrationClaims.firstOrNull { it.claimId == id }?.cafeName
            }
            PendingFilter.ROLE_CLAIM -> {
                state.pendingCafeOwnerClaims.firstOrNull { it.claimId == id }?.let { claim ->
                    "점장 권한 신청 - ${claim.requesterNickname}"
                }
            }
        } ?: return

        viewModelScope.launch {
            val result = when (selectedFilter) {
                PendingFilter.CAFE_REGISTRATION -> {
                    if (approved) {
                        approveCafeRegistrationClaimUseCase.invoke(id)
                    } else {
                        rejectCafeRegistrationClaimUseCase.invoke(id)
                    }
                }
                PendingFilter.ROLE_CLAIM -> {
                    if (approved) {
                        approveCafeOwnerClaimUseCase.invoke(id)
                    } else {
                        rejectCafeOwnerClaimUseCase.invoke(id)
                    }
                }
            }
            when (result) {
                is AppResult.Success -> {
                    if (selectedFilter == PendingFilter.CAFE_REGISTRATION) {
                        if (approved) {
                            applyRegistrationApprovalLocally(id)
                        } else {
                            removeRegistrationClaimLocally(id)
                        }
                    } else {
                        removeOwnerClaimLocally(id)
                    }
                    _uiState.update { state ->
                        state.copy(
                            infoMessage = if (approved) {
                                "$requestTitle 요청을 승인했습니다."
                            } else {
                                "$requestTitle 요청을 반려했습니다."
                            }
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(infoMessage = result.error.toString()) }
                }
            }
        }
    }

    fun onAction(action: AdminOperationsAction) {
        when (action) {
            AdminOperationsAction.ClickNotifications -> {
                _uiState.update {
                    it.copy(hasUnreadNotifications = false, infoMessage = "새 알림을 모두 확인했습니다.")
                }
            }
            AdminOperationsAction.ClickSeeAllPending -> {
                _uiState.update { it.copy(infoMessage = "전체보기 연결은 다음 단계에서 이어집니다.") }
            }
            AdminOperationsAction.ClickBannerRegister -> {
                viewModelScope.launch {
                    _event.emit(AdminOperationsEvent.NavigateToBannerEdit)
                }
            }
            AdminOperationsAction.LoadMoreInquiries -> {
                loadMoreInquiries()
            }
            AdminOperationsAction.LoadMoreReports -> {
                loadMoreReports()
            }
            is AdminOperationsAction.SelectPendingFilter -> {
                _uiState.update { it.copy(selectedPendingFilter = action.filter, infoMessage = null) }
            }
            is AdminOperationsAction.ApprovePending -> {
                handlePendingResult(action.id, approved = true)
            }
            is AdminOperationsAction.RejectPending -> {
                handlePendingResult(action.id, approved = false)
            }
            is AdminOperationsAction.ClickQuickMenu -> {
                viewModelScope.launch {
                    when (action.id) {
                        ADMIN_BANNER_MENU_ID -> _event.emit(AdminOperationsEvent.NavigateToBanner)
                        ADMIN_USER_MENU_ID -> _event.emit(AdminOperationsEvent.NavigateToUserManagement)
                        ADMIN_DORMANT_MENU_ID -> _event.emit(AdminOperationsEvent.NavigateToDormantAccount)
                        else -> {
                            val label = _uiState.value.quickMenus.firstOrNull { it.id == action.id }?.title ?: "메뉴"
                            _uiState.update { it.copy(infoMessage = "$label 연결은 다음 단계에서 이어집니다.") }
                        }
                    }
                }
            }
            AdminOperationsAction.DismissInfoMessage -> {
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }

    init {
        showCachedPendingRequests()
        observeClaimEvents()
        observeOwnerClaimEvents()
        startClaimPolling()
        loadPendingRequests()
        loadInitialInquiries()
        loadInitialReports()
    }

    override fun onCleared() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        CLAIM_EVENT,
        OWNER_CLAIM_EVENT,
        CLAIM_POLLING,
        INQUIRY_PAGE,
        REPORT_PAGE
    }
}

private const val ADMIN_CLAIM_POLLING_INTERVAL_MILLIS = 60_000L
private const val ADMIN_INQUIRY_PAGE_SIZE = 10
private const val ADMIN_REPORT_PAGE_SIZE = 10
private const val PAGINATION_DELAY_MILLIS = 1_000L

private const val ADMIN_BANNER_MENU_ID = "banner"
private const val ADMIN_USER_MENU_ID = "users"
private const val ADMIN_DORMANT_MENU_ID = "dormant"

private data class AdminPendingSnapshot(
    val registrationClaims: List<PendingCafeRegistrationClaimPreview>,
    val ownerClaims: List<PendingCafeOwnerClaimPreview>,
    val totalUsersCount: Int,
    val activeCafesCount: Int,
    val reportItemsCount: Int
)

private object AdminPendingCache {
    var snapshot: AdminPendingSnapshot? = null
}
