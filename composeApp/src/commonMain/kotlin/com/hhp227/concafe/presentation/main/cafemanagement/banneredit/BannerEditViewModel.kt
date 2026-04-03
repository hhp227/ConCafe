package com.hhp227.concafe.presentation.main.cafemanagement.banneredit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeBannerCreate
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.usecase.CreateHomeBannerUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeManagementUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import com.hhp227.concafe.domain.usecase.GetHomeBannerManagementUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.UpdateHomeBannerUseCase
import com.hhp227.concafe.domain.usecase.UploadImageUseCase
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
    private val initialBannerId: String? = null,
    private val createHomeBannerUseCase: CreateHomeBannerUseCase,
    private val updateHomeBannerUseCase: UpdateHomeBannerUseCase,
    private val getCafeManagementUseCase: GetCafeManagementUseCase,
    private val getHomeBannerManagementUseCase: GetHomeBannerManagementUseCase,
    private val getCafeNoticePageUseCase: GetCafeNoticePageUseCase,
    private val getCafeEventPageUseCase: GetCafeEventPageUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val uploadImageUseCase: UploadImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BannerEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<BannerEditEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private var selectorJob: Job? = null

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                _uiState.update { it.copy(isAdmin = user?.role == UserRole.ADMIN) }
            }
        }
    }

    private fun loadOwnedCafeOptions() {
        viewModelScope.launch {
            when (val result = getCafeManagementUseCase.invoke()) {
                is AppResult.Success -> {
                    val options = result.data.ownedCafes
                    _uiState.update { state ->
                        val selectedCafe = initialCafeId?.let { targetCafeId ->
                            options.firstOrNull { it.id == targetCafeId }
                        } ?: state.selectedCafeId?.let { selectedCafeId ->
                            options.firstOrNull { it.id == selectedCafeId }
                        } ?: if (state.isAdmin) {
                            state.selectedCafeOption
                        } else {
                            options.firstOrNull()
                        }
                        state.copy(
                            ownedCafeOptions = options,
                            selectedCafeId = selectedCafe?.id,
                            selectedNoticeId = if (selectedCafe?.id == state.selectedCafeId) {
                                state.selectedNoticeId
                            } else {
                                null
                            },
                            selectedEventId = if (selectedCafe?.id == state.selectedCafeId) {
                                state.selectedEventId
                            } else {
                                null
                            },
                            targetValue = when (state.selectedTarget) {
                                BannerTargetType.CAFE_DETAIL -> selectedCafe?.id.orEmpty()
                                BannerTargetType.EXTERNAL_LINK -> state.targetValue
                                else -> if (selectedCafe?.id == state.selectedCafeId) state.targetValue else ""
                            }
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            ownedCafeOptions = emptyList(),
                            infoMessage = MSG_OWNED_CAFE_LOAD_FAILED
                        )
                    }
                }
            }
        }
    }

    private fun loadEditingBanner() {
        val targetBannerId = initialBannerId ?: return
        viewModelScope.launch {
            when (val result = getHomeBannerManagementUseCase.invoke(initialCafeId)) {
                is AppResult.Success -> {
                    val banner = result.data.firstOrNull { it.id == targetBannerId }
                    if (banner == null) {
                        _uiState.update { it.copy(infoMessage = MSG_EDIT_BANNER_NOT_FOUND) }
                    } else {
                        applyEditingBanner(banner)
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(infoMessage = MSG_EDIT_BANNER_LOAD_FAILED) }
                }
            }
        }
    }

    private fun applyEditingBanner(banner: HomeBanner) {
        val target = when (banner.targetType) {
            BannerLinkTargetType.CAFE_DETAIL -> BannerTargetType.CAFE_DETAIL
            BannerLinkTargetType.EVENT_DETAIL -> BannerTargetType.EVENT_DETAIL
            BannerLinkTargetType.NOTICE -> BannerTargetType.NOTICE
            BannerLinkTargetType.EXTERNAL_LINK -> BannerTargetType.EXTERNAL_LINK
        }
        _uiState.update { state ->
            state.copy(
                editingBannerId = banner.id,
                selectedImageLabel = banner.imageUrl,
                originalImageUrl = banner.imageUrl,
                title = banner.title,
                subtitle = banner.subtitle,
                selectedTarget = target,
                targetValue = banner.targetValue,
                displayDays = banner.displayDays.coerceIn(1, 10),
                selectedCafeId = banner.cafeId,
                selectedNoticeId = if (target == BannerTargetType.NOTICE) banner.targetValue else null,
                selectedEventId = if (target == BannerTargetType.EVENT_DETAIL) banner.targetValue else null,
                selectorType = null,
                selectorQuery = "",
                infoMessage = null
            )
        }
        val editingCafeId = banner.cafeId
        if (!editingCafeId.isNullOrBlank()) {
            if (target == BannerTargetType.NOTICE) {
                loadNoticeOptions(editingCafeId, query = "")
            } else if (target == BannerTargetType.EVENT_DETAIL) {
                loadEventOptions(editingCafeId, query = "")
            }
        }
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(BannerEditEvent.NavigateBack)
        }
    }

    private fun clickImagePicker() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun selectTarget(target: BannerTargetType) {
        _uiState.update { state ->
            state.copy(
                selectedTarget = target,
                selectedCafeId = when (target) {
                    BannerTargetType.CAFE_DETAIL -> when {
                        state.isAdmin -> state.selectedCafeOption
                        initialCafeId != null -> state.ownedCafeOptions.firstOrNull { it.id == initialCafeId }
                        else -> state.selectedCafeOption ?: state.ownedCafeOptions.firstOrNull()
                    }?.id
                    else -> state.selectedCafeId
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
                selectedNoticeId = null,
                selectedEventId = null,
                selectorType = null,
                selectorQuery = "",
                noticeSelectorOptions = emptyList(),
                eventSelectorOptions = emptyList(),
                isSelectorLoading = false
            )
        }
    }

    private fun openCafeSelector() {
        _uiState.update { state ->
            state.copy(
                selectorType = BannerSelectorType.CAFE,
                selectorQuery = "",
                isSelectorLoading = false
            )
        }
    }

    private fun openTargetSelector() {
        val currentState = _uiState.value
        val selectedCafe = currentState.selectedCafeOption

        if (selectedCafe == null) {
            _uiState.update { it.copy(infoMessage = MSG_SELECT_CAFE_FIRST) }
            return
        }

        when (currentState.selectedTarget) {
            BannerTargetType.NOTICE -> {
                _uiState.update {
                    it.copy(
                        selectorType = BannerSelectorType.NOTICE,
                        selectorQuery = "",
                        noticeSelectorOptions = emptyList(),
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
                        eventSelectorOptions = emptyList(),
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
            BannerSelectorType.CAFE -> Unit
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
                            noticeSelectorOptions = result.data.items,
                            isSelectorLoading = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            noticeSelectorOptions = emptyList(),
                            isSelectorLoading = false,
                            infoMessage = MSG_NOTICE_LIST_LOAD_FAILED
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
                            eventSelectorOptions = result.data.items,
                            isSelectorLoading = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            eventSelectorOptions = emptyList(),
                            isSelectorLoading = false,
                            infoMessage = MSG_EVENT_LIST_LOAD_FAILED
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
                val isSameCafe = currentState.selectedCafeId == selectedCafe.id
                _uiState.update {
                    it.copy(
                        selectedCafeId = selectedCafe.id,
                        selectedNoticeId = if (isSameCafe) it.selectedNoticeId else null,
                        selectedEventId = if (isSameCafe) it.selectedEventId else null,
                        targetValue = when (it.selectedTarget) {
                            BannerTargetType.CAFE_DETAIL -> selectedCafe.id
                            BannerTargetType.EXTERNAL_LINK -> it.targetValue
                            else -> if (isSameCafe) it.targetValue else ""
                        },
                        selectorType = null,
                        selectorQuery = "",
                        isSelectorLoading = false
                    )
                }
            }
            BannerSelectorType.NOTICE -> {
                val selectedNotice = currentState.noticeSelectorOptions.firstOrNull { it.id == id } ?: return
                _uiState.update {
                    it.copy(
                        selectedNoticeId = selectedNotice.id,
                        selectedEventId = null,
                        targetValue = selectedNotice.id,
                        selectorType = null,
                        selectorQuery = "",
                        isSelectorLoading = false
                    )
                }
            }
            BannerSelectorType.EVENT -> {
                val selectedEvent = currentState.eventSelectorOptions.firstOrNull { it.id == id } ?: return
                _uiState.update {
                    it.copy(
                        selectedEventId = selectedEvent.id,
                        selectedNoticeId = null,
                        targetValue = selectedEvent.id,
                        selectorType = null,
                        selectorQuery = "",
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
                isSelectorLoading = false
            )
        }
    }

    private fun clickSave() {
        val currentState = _uiState.value

        val validationMessage = when {
            currentState.selectedImageLabel.isNullOrBlank() -> MSG_VALIDATION_IMAGE_REQUIRED
            currentState.title.isBlank() -> MSG_VALIDATION_TITLE_REQUIRED
            currentState.subtitle.isBlank() -> MSG_VALIDATION_SUBTITLE_REQUIRED
            currentState.selectedTarget == BannerTargetType.EXTERNAL_LINK &&
                currentState.targetValue.isBlank() -> MSG_VALIDATION_EXTERNAL_URL_REQUIRED
            currentState.selectedTarget != BannerTargetType.EXTERNAL_LINK &&
                currentState.targetValue.isBlank() -> MSG_VALIDATION_TARGET_REQUIRED
            else -> null
        }

        if (validationMessage != null) {
            if (currentState.selectedImageLabel.isNullOrBlank()) {
                _uiState.update { it.copy(isImageRequiredAlertVisible = true) }
            } else {
                _uiState.update { it.copy(infoMessage = validationMessage) }
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, infoMessage = null) }
        viewModelScope.launch {
            val imageUrl = resolveBannerImageUrl(currentState)
            if (imageUrl == null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        infoMessage = MSG_IMAGE_UPLOAD_FAILED
                    )
                }
                return@launch
            }

            val createInput = HomeBannerCreate(
                cafeId = currentState.selectedCafeId,
                title = currentState.title.trim(),
                subtitle = currentState.subtitle.trim(),
                imageUrl = imageUrl,
                targetType = when (currentState.selectedTarget) {
                    BannerTargetType.CAFE_DETAIL -> BannerLinkTargetType.CAFE_DETAIL
                    BannerTargetType.EVENT_DETAIL -> BannerLinkTargetType.EVENT_DETAIL
                    BannerTargetType.NOTICE -> BannerLinkTargetType.NOTICE
                    BannerTargetType.EXTERNAL_LINK -> BannerLinkTargetType.EXTERNAL_LINK
                },
                targetValue = currentState.targetValue.trim(),
                displayDays = currentState.displayDays
            )
            val editingBannerId = currentState.editingBannerId
            val saveResult = if (!editingBannerId.isNullOrBlank()) {
                updateHomeBannerUseCase.invoke(editingBannerId, createInput)
            } else {
                createHomeBannerUseCase.invoke(createInput)
            }
            when (val result = saveResult) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, infoMessage = null) }
                    _event.emit(BannerEditEvent.NavigateBack)
                }
                is AppResult.Failure -> {
                    val userMessage = when (val error = result.error) {
                        AppError.Unauthorized -> MSG_UNAUTHORIZED
                        AppError.PermissionDenied -> MSG_PERMISSION_DENIED
                        AppError.NotFound -> MSG_TARGET_NOT_FOUND
                        is AppError.ValidationFailed -> error.reason
                        is AppError.NetworkError -> MSG_NETWORK_FAILED
                        is AppError.Unknown -> MSG_SAVE_UNKNOWN
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            infoMessage = userMessage
                        )
                    }
                }
            }
        }
    }

    private suspend fun resolveBannerImageUrl(state: BannerEditUiState): String? {
        val selectedImageLabel = state.selectedImageLabel
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val isRemoteImage = selectedImageLabel.startsWith("http://") || selectedImageLabel.startsWith("https://")
        if (isRemoteImage || selectedImageLabel == state.originalImageUrl) {
            return selectedImageLabel
        }

        return when (val uploadResult = uploadImageUseCase.invoke(localPath = selectedImageLabel, folder = "banners")) {
            is AppResult.Success -> uploadResult.data
            is AppResult.Failure -> null
        }
    }
    
    fun onAction(action: BannerEditAction) {
        when (action) {
            BannerEditAction.ClickBack -> clickBack()
            BannerEditAction.ClickImagePicker -> clickImagePicker()
            is BannerEditAction.SelectImage -> _uiState.update {
                it.copy(
                    selectedImageLabel = action.imageUrl,
                    infoMessage = null,
                    isImageRequiredAlertVisible = false
                )
            }
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
            BannerEditAction.DismissImageRequiredAlert -> _uiState.update { it.copy(isImageRequiredAlertVisible = false) }
            BannerEditAction.ClickSave -> clickSave()
            BannerEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    init {
        observeSession()
        loadOwnedCafeOptions()
        loadEditingBanner()
    }

    companion object {
        private const val MSG_OWNED_CAFE_LOAD_FAILED = "banneredit_info_owned_cafe_load_failed"
        private const val MSG_EDIT_BANNER_NOT_FOUND = "banneredit_info_edit_banner_not_found"
        private const val MSG_EDIT_BANNER_LOAD_FAILED = "banneredit_info_edit_banner_load_failed"
        private const val MSG_SELECT_CAFE_FIRST = "banneredit_info_select_cafe_first"
        private const val MSG_NOTICE_LIST_LOAD_FAILED = "banneredit_info_notice_list_load_failed"
        private const val MSG_EVENT_LIST_LOAD_FAILED = "banneredit_info_event_list_load_failed"
        private const val MSG_VALIDATION_IMAGE_REQUIRED = "banneredit_validation_image_required"
        private const val MSG_VALIDATION_TITLE_REQUIRED = "banneredit_validation_title_required"
        private const val MSG_VALIDATION_SUBTITLE_REQUIRED = "banneredit_validation_subtitle_required"
        private const val MSG_VALIDATION_EXTERNAL_URL_REQUIRED = "banneredit_validation_external_url_required"
        private const val MSG_VALIDATION_TARGET_REQUIRED = "banneredit_validation_target_required"
        private const val MSG_IMAGE_UPLOAD_FAILED = "banneredit_info_image_upload_failed"
        private const val MSG_UNAUTHORIZED = "banneredit_error_unauthorized"
        private const val MSG_PERMISSION_DENIED = "banneredit_error_permission_denied"
        private const val MSG_TARGET_NOT_FOUND = "banneredit_error_target_not_found"
        private const val MSG_NETWORK_FAILED = "banneredit_error_network_failed"
        private const val MSG_SAVE_UNKNOWN = "banneredit_error_save_unknown"
    }
}
