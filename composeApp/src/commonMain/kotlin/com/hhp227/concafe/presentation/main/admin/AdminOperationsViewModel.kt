package com.hhp227.concafe.presentation.main.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.usecase.ApproveCafeOwnerClaimUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCafeOwnerClaimsUseCase
import com.hhp227.concafe.domain.usecase.RejectCafeOwnerClaimUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AdminOperationsViewModel(
    private val getPendingCafeOwnerClaimsUseCase: GetPendingCafeOwnerClaimsUseCase,
    private val approveCafeOwnerClaimUseCase: ApproveCafeOwnerClaimUseCase,
    private val rejectCafeOwnerClaimUseCase: RejectCafeOwnerClaimUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminOperationsUiState())
    val uiState: StateFlow<AdminOperationsUiState> = _uiState.asStateFlow()

    private fun loadPendingRequests() {
        viewModelScope.launch {
            when (val result = getPendingCafeOwnerClaimsUseCase.invoke()) {
                is AppResult.Success -> {
                    val roleClaims = result.data.map { it.toAdminPendingRequest() }
                    _uiState.update { state ->
                        val mergedRequests = defaultCafeRegistrationPendingRequests + roleClaims
                        state.copy(
                            pendingRequests = mergedRequests,
                            metrics = buildAdminMetrics(mergedRequests.size),
                            infoMessage = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            pendingRequests = defaultCafeRegistrationPendingRequests,
                            metrics = buildAdminMetrics(defaultCafeRegistrationPendingRequests.size),
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun handlePendingResult(id: String, approved: Boolean) {
        val request = _uiState.value.pendingRequests.firstOrNull { it.id == id } ?: return
        if (request.type == PendingFilter.ROLE_CLAIM) {
            viewModelScope.launch {
                val result = if (approved) {
                    approveCafeOwnerClaimUseCase.invoke(id)
                } else {
                    rejectCafeOwnerClaimUseCase.invoke(id)
                }
                when (result) {
                    is AppResult.Success -> {
                        loadPendingRequests()
                        _uiState.update { state ->
                            state.copy(
                                infoMessage = if (approved) {
                                    "${request.title} 요청을 승인했습니다."
                                } else {
                                    "${request.title} 요청을 반려했습니다."
                                }
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(infoMessage = result.error.toString()) }
                    }
                }
            }
            return
        }

        _uiState.update { state ->
            val nextRequests = state.pendingRequests.filterNot { it.id == id }
            state.copy(
                pendingRequests = nextRequests,
                metrics = buildAdminMetrics(nextRequests.size),
                infoMessage = if (approved) {
                    "${request.title} 요청을 승인했습니다."
                } else {
                    "${request.title} 요청을 반려했습니다."
                }
            )
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
                val label = _uiState.value.quickMenus.firstOrNull { it.id == action.id }?.title ?: "메뉴"
                _uiState.update { it.copy(infoMessage = "$label 연결은 다음 단계에서 이어집니다.") }
            }
            AdminOperationsAction.DismissInfoMessage -> {
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }

    init {
        loadPendingRequests()
    }
}

private fun PendingCafeOwnerClaimPreview.toAdminPendingRequest(): AdminPendingRequest {
    return AdminPendingRequest(
        id = claimId,
        type = PendingFilter.ROLE_CLAIM,
        title = "점장 권한 신청 - $requesterNickname",
        subtitle = location,
        requestedAt = requestedAt,
        imageUrl = imageUrl.orEmpty()
    )
}

private val defaultCafeRegistrationPendingRequests = listOf(
    AdminPendingRequest(
        id = "pending-cafe-1",
        type = PendingFilter.CAFE_REGISTRATION,
        title = "카페 모카라떼 홍대점",
        subtitle = "서울 마포구 어울마당로 123",
        requestedAt = "2시간 전",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAPTqu6TR5iE7rtn6cuSTaGUwIAdgNS9xaZqDyHkBXX25arxUP3ZAK6wS2HHUj-Efew3j9cuymLzCx7a7fUG8MqyZ1HFdgXXJoSTw9zIlWv0cvk_sjAIt-6daNAoEAg0lQTCaCkZ7CSKX2uNQpH9gyyUjrU2UdHrmBskzC9nIr06ms2YgAbzHhPdxZbEVZN41SPq6gUqSSTdRWJcI5AS-T3HTjq3n3yMJYZ7T_imgYTE1UrUdAnniws6bLwUzX_o9f7XcBOy5Ur9A"
    ),
    AdminPendingRequest(
        id = "pending-cafe-2",
        type = PendingFilter.CAFE_REGISTRATION,
        title = "디저트 빌리지 성수",
        subtitle = "서울 성동구 아차산로 45",
        requestedAt = "5시간 전",
        imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCe9Vib6B40fIweAG4csR1KYxnHTMoec_xzj6GS5343QHIszmvCk4_ZiPt1NOdpLruSfby0tdpH2myNthY3GZjMgDZw8Fjh70hjE55AGaHkmkMJdLkqsuISq4Gsa8WhO-JRD3SIBIY_FAoBdHYRxqq2AVZl7Xmrgp0OorSTkcVTdF6cO14mBMWbvzhU9Hga3y41jSo89iuQ8aG-D8oKHX5PPyeXXGllTSzc7oGE8PMT1rBx-DRviiY0QI2H9AvdAbcm8hHiBGfIVQ"
    )
)

private fun buildAdminMetrics(pendingCount: Int): List<AdminMetricCard> {
    return listOf(
        AdminMetricCard("전체 사용자", "12,540", "1.2%", AdminMetricIcon.USERS, MetricTrend.UP),
        AdminMetricCard("활성 카페", "842", "0.5%", AdminMetricIcon.CAFE, MetricTrend.UP),
        AdminMetricCard("승인 대기", pendingCount.toString(), "${pendingCount}건 대기", AdminMetricIcon.PENDING, MetricTrend.NEW),
        AdminMetricCard("신고 항목", "32", "8%", AdminMetricIcon.REPORT, MetricTrend.DOWN)
    )
}
