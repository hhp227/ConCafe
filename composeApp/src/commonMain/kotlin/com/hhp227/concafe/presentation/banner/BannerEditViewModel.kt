package com.hhp227.concafe.presentation.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.HomeBannerCreate
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.usecase.CreateHomeBannerUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeManagementUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BannerEditViewModel(
    private val initialCafeId: String? = null,
    private val createHomeBannerUseCase: CreateHomeBannerUseCase,
    private val getCafeManagementUseCase: GetCafeManagementUseCase,
    private val getCafeNoticePageUseCase: GetCafeNoticePageUseCase,
    private val getCafeEventPageUseCase: GetCafeEventPageUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BannerEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<BannerEditEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private var selectorJob: Job? = null

    fun onAction(action: BannerEditAction) {
        when (action) {
            BannerEditAction.ClickBack -> clickBack()
            BannerEditAction.ClickImagePicker -> clickImagePicker()
            is BannerEditAction.ChangeTitle -> _uiState.update { it.copy(title = action.value) }
            is BannerEditAction.ChangeSubtitle -> _uiState.update { it.copy(subtitle = action.value) }
            is BannerEditAction.SelectTarget -> selectTarget(action.target)
            is BannerEditAction.ChangeTargetValue -> _uiState.update { it.copy(targetValue = action.value) }
            is BannerEditAction.ChangeDisplayDays -> _uiState.update {
                it.copy(displayDays = action.value.coerceIn(1, 10))
            }
            BannerEditAction.ClickCafeSelector -> openCafeSelector()
            BannerEditAction.ClickTargetSelector -> openTargetSelector()
            is BannerEditAction.ChangeSelectorQuery -> changeSelectorQuery(action.value)
            is BannerEditAction.SelectSelectorItem -> selectSelectorItem(action.id)
            BannerEditAction.DismissSelector -> dismissSelector()
            BannerEditAction.ClickSave -> clickSave()
            BannerEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    private fun loadOwnedCafeOptions() {
        viewModelScope.launch {
            when (val result = getCafeManagementUseCase.invoke()) {
                is AppResult.Success -> {
                    val options = result.data.ownedCafes.map { cafe ->
                        BannerSelectableItem(
                            id = cafe.id,
                            title = cafe.name,
                            subtitle = cafe.city
                        )
                    }
                    _uiState.update { state ->
                        val selectedCafe = initialCafeId?.let { targetCafeId ->
                            options.firstOrNull { it.id == targetCafeId }
                        } ?: state.selectedCafeOption?.let { current ->
                            options.firstOrNull { it.id == current.id }
                        } ?: if (state.isAdmin) {
                            state.selectedCafeOption
                        } else {
                            options.firstOrNull()
                        }
                        state.copy(
                            ownedCafeOptions = options,
                            selectedCafeOption = selectedCafe,
                            selectedContentOption = if (selectedCafe?.id == state.selectedCafeOption?.id) {
                                state.selectedContentOption
                            } else {
                                null
                            },
                            targetValue = when (state.selectedTarget) {
                                BannerTargetType.CAFE_DETAIL -> selectedCafe?.id.orEmpty()
                                BannerTargetType.EXTERNAL_LINK -> state.targetValue
                                else -> if (selectedCafe?.id == state.selectedCafeOption?.id) state.targetValue else ""
                            }
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            ownedCafeOptions = emptyList(),
                            infoMessage = "운영 카페 목록을 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                _uiState.update { it.copy(isAdmin = user?.role == UserRole.ADMIN) }
            }
        }
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(BannerEditEvent.NavigateBack)
        }
    }

    private fun clickImagePicker() {
        _uiState.update {
            it.copy(
                selectedImageLabel = "banner_cover_mock.png",
                infoMessage = "이미지 업로드 연결은 다음 단계에서 구현됩니다."
            )
        }
    }

    private fun selectTarget(target: BannerTargetType) {
        _uiState.update { state ->
            state.copy(
                selectedTarget = target,
                selectedCafeOption = when (target) {
                    BannerTargetType.CAFE_DETAIL -> when {
                        state.isAdmin -> state.selectedCafeOption
                        initialCafeId != null -> state.ownedCafeOptions.firstOrNull { it.id == initialCafeId }
                        else -> state.selectedCafeOption ?: state.ownedCafeOptions.firstOrNull()
                    }
                    else -> state.selectedCafeOption
                },
                targetValue = when (target) {
                    BannerTargetType.CAFE_DETAIL -> when {
                        state.isAdmin -> state.selectedCafeOption?.id.orEmpty()
                        initialCafeId != null -> state.ownedCafeOptions.firstOrNull { it.id == initialCafeId }?.id.orEmpty()
                        else -> (state.selectedCafeOption ?: state.ownedCafeOptions.firstOrNull())?.id.orEmpty()
                    }
                    BannerTargetType.EXTERNAL_LINK -> ""
                    else -> ""
                },
                selectedContentOption = null,
                selectorType = null,
                selectorQuery = "",
                selectorOptions = emptyList(),
                isSelectorLoading = false
            )
        }
    }

    private fun openCafeSelector() {
        _uiState.update { state ->
            state.copy(
                selectorType = BannerSelectorType.CAFE,
                selectorQuery = "",
                selectorOptions = state.ownedCafeOptions,
                isSelectorLoading = false
            )
        }
    }

    private fun openTargetSelector() {
        val currentState = _uiState.value
        val selectedCafe = currentState.selectedCafeOption

        if (selectedCafe == null) {
            _uiState.update { it.copy(infoMessage = "먼저 운영 카페를 선택해주세요.") }
            return
        }

        when (currentState.selectedTarget) {
            BannerTargetType.NOTICE -> {
                _uiState.update {
                    it.copy(
                        selectorType = BannerSelectorType.NOTICE,
                        selectorQuery = "",
                        selectorOptions = emptyList(),
                        isSelectorLoading = true
                    )
                }
                loadNoticeOptions(selectedCafe.id, query = "")
            }
            BannerTargetType.EVENT_DETAIL -> {
                _uiState.update {
                    it.copy(
                        selectorType = BannerSelectorType.EVENT,
                        selectorQuery = "",
                        selectorOptions = emptyList(),
                        isSelectorLoading = true
                    )
                }
                loadEventOptions(selectedCafe.id, query = "")
            }
            else -> Unit
        }
    }

    private fun changeSelectorQuery(value: String) {
        _uiState.update { it.copy(selectorQuery = value) }
        when (_uiState.value.selectorType) {
            BannerSelectorType.CAFE -> {
                _uiState.update { state ->
                    state.copy(selectorOptions = state.ownedCafeOptions.filter {
                        it.title.contains(value, ignoreCase = true) ||
                            it.subtitle.contains(value, ignoreCase = true)
                    })
                }
            }
            BannerSelectorType.NOTICE -> {
                _uiState.value.selectedCafeOption?.id?.let { loadNoticeOptions(it, value) }
            }
            BannerSelectorType.EVENT -> {
                _uiState.value.selectedCafeOption?.id?.let { loadEventOptions(it, value) }
            }
            null -> Unit
        }
    }

    private fun loadNoticeOptions(cafeId: String, query: String) {
        selectorJob?.cancel()
        selectorJob = viewModelScope.launch {
            _uiState.update { it.copy(isSelectorLoading = true) }
            when (val result = getCafeNoticePageUseCase.invoke(cafeId, query, cursor = null, pageSize = 50)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectorOptions = result.data.items.map { item ->
                                BannerSelectableItem(
                                    id = item.id,
                                    title = item.title,
                                    subtitle = item.displayDate
                                )
                            },
                            isSelectorLoading = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            selectorOptions = emptyList(),
                            isSelectorLoading = false,
                            infoMessage = "공지사항 목록을 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun loadEventOptions(cafeId: String, query: String) {
        selectorJob?.cancel()
        selectorJob = viewModelScope.launch {
            _uiState.update { it.copy(isSelectorLoading = true) }
            when (val result = getCafeEventPageUseCase.invoke(cafeId, query, cursor = null, pageSize = 50)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectorOptions = result.data.items.map { item ->
                                BannerSelectableItem(
                                    id = item.id,
                                    title = item.title,
                                    subtitle = item.periodText
                                )
                            },
                            isSelectorLoading = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            selectorOptions = emptyList(),
                            isSelectorLoading = false,
                            infoMessage = "이벤트 목록을 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun selectSelectorItem(id: String) {
        val currentState = _uiState.value
        when (currentState.selectorType) {
            BannerSelectorType.CAFE -> {
                val selectedCafe = currentState.ownedCafeOptions.firstOrNull { it.id == id } ?: return
                val isSameCafe = currentState.selectedCafeOption?.id == selectedCafe.id
                _uiState.update {
                    it.copy(
                        selectedCafeOption = selectedCafe,
                        selectedContentOption = if (isSameCafe) it.selectedContentOption else null,
                        targetValue = when (it.selectedTarget) {
                            BannerTargetType.CAFE_DETAIL -> selectedCafe.id
                            BannerTargetType.EXTERNAL_LINK -> it.targetValue
                            else -> if (isSameCafe) it.targetValue else ""
                        },
                        selectorType = null,
                        selectorQuery = "",
                        selectorOptions = emptyList(),
                        isSelectorLoading = false
                    )
                }
            }
            BannerSelectorType.NOTICE,
            BannerSelectorType.EVENT -> {
                val selectedTargetItem = currentState.selectorOptions.firstOrNull { it.id == id } ?: return
                _uiState.update {
                    it.copy(
                        selectedContentOption = selectedTargetItem,
                        targetValue = selectedTargetItem.id,
                        selectorType = null,
                        selectorQuery = "",
                        selectorOptions = emptyList(),
                        isSelectorLoading = false
                    )
                }
            }
            null -> Unit
        }
    }

    private fun dismissSelector() {
        selectorJob?.cancel()
        _uiState.update {
            it.copy(
                selectorType = null,
                selectorQuery = "",
                selectorOptions = emptyList(),
                isSelectorLoading = false
            )
        }
    }

    private fun clickSave() {
        val currentState = _uiState.value

        val validationMessage = when {
            currentState.title.isBlank() -> "배너 제목을 입력해주세요."
            currentState.subtitle.isBlank() -> "서브 문구를 입력해주세요."
            currentState.selectedTarget == BannerTargetType.EXTERNAL_LINK &&
                currentState.targetValue.isBlank() -> "외부 URL을 입력해주세요."
            currentState.selectedTarget != BannerTargetType.EXTERNAL_LINK &&
                currentState.targetValue.isBlank() -> "연결 대상을 선택해주세요."
            else -> null
        }

        if (validationMessage != null) {
            _uiState.update { it.copy(infoMessage = validationMessage) }
            return
        }

        _uiState.update { it.copy(isSaving = true, infoMessage = null) }
        viewModelScope.launch {
            when (val result = createHomeBannerUseCase.invoke(currentState.toCreateInput())) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            infoMessage = "배너가 등록되었습니다."
                        )
                    }
                    _event.emit(BannerEditEvent.ShowSaveSuccessMessage)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            infoMessage = result.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    init {
        observeSession()
        loadOwnedCafeOptions()
    }
}

private fun BannerEditUiState.toCreateInput(): HomeBannerCreate {
    return HomeBannerCreate(
        cafeId = selectedCafeOption?.id,
        title = title.trim(),
        subtitle = subtitle.trim(),
        imageUrl = selectedImageLabel,
        targetType = selectedTarget.toDomainType(),
        targetValue = targetValue.trim(),
        displayDays = displayDays
    )
}

private fun BannerTargetType.toDomainType(): BannerLinkTargetType {
    return when (this) {
        BannerTargetType.CAFE_DETAIL -> BannerLinkTargetType.CAFE_DETAIL
        BannerTargetType.EVENT_DETAIL -> BannerLinkTargetType.EVENT_DETAIL
        BannerTargetType.NOTICE -> BannerLinkTargetType.NOTICE
        BannerTargetType.EXTERNAL_LINK -> BannerLinkTargetType.EXTERNAL_LINK
    }
}

private fun AppError.toUserMessage(): String {
    return when (this) {
        AppError.Unauthorized -> "로그인 후 배너를 등록해주세요."
        AppError.PermissionDenied -> "배너 등록 권한이 없습니다."
        AppError.NotFound -> "연결 대상을 찾을 수 없습니다."
        is AppError.ValidationFailed -> reason
        is AppError.NetworkError -> "배너를 등록하지 못했습니다."
        is AppError.Unknown -> "배너 등록 중 오류가 발생했습니다."
    }
}
