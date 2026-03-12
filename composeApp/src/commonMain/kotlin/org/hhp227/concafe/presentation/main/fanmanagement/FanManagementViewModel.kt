package com.hhp227.concafe.presentation.main.fanmanagement

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
import com.hhp227.concafe.di.resolveGetFanManagementDataUseCase
import com.hhp227.concafe.di.resolveObserveCastVersionUseCase
import com.hhp227.concafe.di.resolveObserveCurrentUserUseCase
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.GetFanManagementDataUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastVersionUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase

class FanManagementViewModel(
    private val getFanManagementDataUseCase: GetFanManagementDataUseCase = resolveGetFanManagementDataUseCase(),
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase = resolveObserveCurrentUserUseCase(),
    private val observeCastVersionUseCase: ObserveCastVersionUseCase = resolveObserveCastVersionUseCase()
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
                unbindCastVersion()
                loadFanManagement()
            }
        }
    }

    private fun bindCastVersion(castId: String) {
        jobs[TaskKey.OBSERVE_CAST_VERSION]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_VERSION] = viewModelScope.launch {
            var isInitialEmission = true
            observeCastVersionUseCase.invoke(castId).collectLatest {
                if (isInitialEmission) {
                    isInitialEmission = false
                    return@collectLatest
                }
                loadFanManagement()
            }
        }
    }

    private fun unbindCastVersion() {
        jobs.remove(TaskKey.OBSERVE_CAST_VERSION)?.cancel()
    }

    private fun setInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private fun loadFanManagement() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
        }
        viewModelScope.launch {
            when (val result = getFanManagementDataUseCase.invoke()) {
                is AppResult.Success -> {
                    val data = result.data
                    val detail = data.detail
                    val cast = detail.cast
                    bindCastVersion(cast.id)
                    _uiState.value = FanManagementUiState(
                        isLoading = false,
                        errorMessage = null,
                        fanManagementData = data,
                        stats = listOf(
                            FanManagementUiState.StatCard(
                                label = "전체 팔로워",
                                value = data.followers.size.toString(),
                                highlight = FanManagementUiState.Highlight.DEFAULT
                            ),
                            FanManagementUiState.StatCard(
                                label = "근무 일정",
                                value = detail.schedule.size.toString(),
                                highlight = FanManagementUiState.Highlight.PRIMARY
                            ),
                            FanManagementUiState.StatCard(
                                label = "평점",
                                value = cast.rating.toOneDecimalString(),
                                highlight = FanManagementUiState.Highlight.DEFAULT
                            )
                        ),
                        recentFollowers = data.followers.take(10).mapIndexed { index, user ->
                            FanManagementUiState.RecentFollower(
                                id = user.id,
                                name = user.nickname,
                                joinedLabel = when (index) {
                                    0 -> "방금 전"
                                    1 -> "2시간 전"
                                    2 -> "5시간 전"
                                    else -> "최근"
                                },
                                accent = index == 0
                            )
                        },
                        topFans = emptyList(),
                        infoMessage = null
                    )
                }
                is AppResult.Failure -> {
                    unbindCastVersion()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "팬관리 데이터를 불러오지 못했습니다.",
                            fanManagementData = null,
                            stats = emptyList(),
                            recentFollowers = emptyList(),
                            topFans = emptyList()
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
        }
    }

    private fun clickRecentFollower(followerId: String) {
        val follower = uiState.value.recentFollowers.firstOrNull { it.id == followerId } ?: return
        setInfoMessage("${follower.name} 팬 상세 화면은 다음 단계에서 연결합니다.")
    }

    private fun clickTopFan(fanId: String) {
        val fan = uiState.value.topFans.firstOrNull { it.id == fanId } ?: return
        setInfoMessage("${fan.name} 활동 리포트는 다음 단계에서 제공합니다.")
    }

    fun onAction(action: FanManagementAction) {
        when (action) {
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
                setInfoMessage("팬 공지 작성 흐름은 다음 단계에서 연결합니다.")
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
    }
}

private fun Double.toOneDecimalString(): String {
    val normalized = (this * 10).toInt() / 10.0
    val text = normalized.toString()
    return if (text.contains('.')) text else "$text.0"
}

private enum class TaskKey {
    OBSERVE_SESSION,
    OBSERVE_CAST_VERSION
}
