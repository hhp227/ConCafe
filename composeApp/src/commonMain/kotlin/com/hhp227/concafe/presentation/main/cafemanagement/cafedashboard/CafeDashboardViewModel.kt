package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.BannerEvent
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastClaimEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.CastClaimEvent as CastClaimDomainEvent
import com.hhp227.concafe.domain.event.CastEvent as CastDomainEvent
import com.hhp227.concafe.domain.event.VisitEvent
import com.hhp227.concafe.domain.event.publisher.VisitEventPublisher
import com.hhp227.concafe.domain.usecase.ApproveCastClaimUseCase
import com.hhp227.concafe.domain.usecase.DeleteCastUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import com.hhp227.concafe.domain.usecase.GetPendingCastClaimsForCafeUseCase
import com.hhp227.concafe.domain.usecase.RejectCastClaimUseCase
import com.hhp227.concafe.domain.usecase.CafeExternalLinkLocalUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeSocialMediaUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeReservationUrlUseCase

class CafeDashboardViewModel(
    private val cafeId: String,
    private val getCafeCastPageUseCase: GetCafeCastPageUseCase,
    private val getCafeDashboardUseCase: GetCafeDashboardUseCase,
    private val getPendingCastClaimsForCafeUseCase: GetPendingCastClaimsForCafeUseCase,
    private val approveCastClaimUseCase: ApproveCastClaimUseCase,
    private val rejectCastClaimUseCase: RejectCastClaimUseCase,
    private val cafeExternalLinkLocalUseCase: CafeExternalLinkLocalUseCase,
    private val updateCafeSocialMediaUseCase: UpdateCafeSocialMediaUseCase,
    private val updateCafeReservationUrlUseCase: UpdateCafeReservationUrlUseCase,
    private val deleteCastUseCase: DeleteCastUseCase,
    private val bannerEventPublisher: BannerEventPublisher,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castClaimEventPublisher: CastClaimEventPublisher,
    private val castEventPublisher: CastEventPublisher,
    private val visitEventPublisher: VisitEventPublisher
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
                            instagramId = result.data.socialMedia["instagram"].orEmpty(),
                            twitterId = result.data.socialMedia["twitter"].orEmpty(),
                            tiktokId = result.data.socialMedia["tiktok"].orEmpty(),
                            youtubeId = result.data.socialMedia["youtube"].orEmpty(),
                            reservationUrl = result.data.reservationUrl.orEmpty(),
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
            if (append) delay(PAGINATION_DELAY_MILLIS)
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
                                infoMessage = "dashboard_info_cast_list_load_failed"
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
                _event.emit(CafeDashboardEvent.NavigateToBanner)
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
                        it.copy(infoMessage = "dashboard_info_select_cast_for_schedule")
                    }
                } else {
                    viewModelScope.launch {
                        _event.emit(CafeDashboardEvent.NavigateToSchedule(selectedCastId))
                    }
                }
            }
            CafeDashboardShortcut.EXTERNAL_LINKS -> {
                _uiState.update {
                    it.copy(
                        isExternalLinkSheetVisible = true,
                        infoMessage = null
                    )
                }
            }
            CafeDashboardShortcut.SOCIAL_MEDIA -> {
                _uiState.update {
                    it.copy(
                        isSocialMediaSheetVisible = true,
                        infoMessage = null
                    )
                }
            }
            CafeDashboardShortcut.RESERVATION -> {
                _uiState.update {
                    it.copy(
                        isReservationSheetVisible = true,
                        infoMessage = null
                    )
                }
            }
        }
    }

    private fun clickCreateBanner() {
        viewModelScope.launch {
            _event.emit(CafeDashboardEvent.NavigateToBannerEdit)
        }
    }

    private fun loadExternalLinks() {
        val links = cafeExternalLinkLocalUseCase.load(cafeId).map { persisted ->
            CafeDashboardExternalLink(
                id = persisted.id,
                title = persisted.title,
                url = persisted.url
            )
        }
        _uiState.update { it.copy(externalLinks = links) }
    }

    private fun dismissExternalLinkSheet() {
        _uiState.update {
            it.copy(
                isExternalLinkSheetVisible = false,
                editingExternalLinkId = null,
                externalLinkTitle = "",
                externalLinkUrl = ""
            )
        }
    }

    private fun changeExternalLinkTitle(value: String) {
        _uiState.update { it.copy(externalLinkTitle = value) }
    }

    private fun changeExternalLinkUrl(value: String) {
        _uiState.update { it.copy(externalLinkUrl = value) }
    }

    private fun submitExternalLink() {
        val currentState = _uiState.value
        if (!currentState.isExternalLinkSubmitEnabled) {
            _uiState.update { it.copy(infoMessage = "dashboard_info_external_link_input_required") }
            return
        }
        val isEdit = currentState.editingExternalLinkId != null
        val updatedLinks = cafeExternalLinkLocalUseCase.upsert(
            cafeId = cafeId,
            linkId = currentState.editingExternalLinkId,
            title = currentState.externalLinkTitle,
            url = currentState.externalLinkUrl
        ).map { persisted ->
            CafeDashboardExternalLink(
                id = persisted.id,
                title = persisted.title,
                url = persisted.url
            )
        }

        _uiState.update {
            it.copy(
                externalLinks = updatedLinks,
                isExternalLinkSheetVisible = false,
                editingExternalLinkId = null,
                externalLinkTitle = "",
                externalLinkUrl = "",
                infoMessage = if (isEdit) "dashboard_info_external_link_updated" else "dashboard_info_external_link_added"
            )
        }
    }

    private fun clickExternalLinkItem(linkId: String) {
        val link = _uiState.value.externalLinks.firstOrNull { it.id == linkId } ?: return
        viewModelScope.launch {
            _event.emit(CafeDashboardEvent.NavigateToExternalLink(link.title, link.url))
        }
    }

    private fun clickEditExternalLink(linkId: String) {
        val link = _uiState.value.externalLinks.firstOrNull { it.id == linkId } ?: return
        _uiState.update {
            it.copy(
                isExternalLinkSheetVisible = true,
                editingExternalLinkId = link.id,
                externalLinkTitle = link.title,
                externalLinkUrl = link.url,
                infoMessage = null
            )
        }
    }

    private fun clickDeleteExternalLink(linkId: String) {
        if (_uiState.value.externalLinks.none { it.id == linkId }) return
        val updatedLinks = cafeExternalLinkLocalUseCase.delete(cafeId, linkId).map { persisted ->
            CafeDashboardExternalLink(
                id = persisted.id,
                title = persisted.title,
                url = persisted.url
            )
        }
        _uiState.update {
            it.copy(
                externalLinks = updatedLinks,
                infoMessage = "dashboard_info_external_link_deleted"
            )
        }
    }

    private fun changeSocialMediaInstagram(value: String) {
        _uiState.update { it.copy(instagramId = value) }
    }

    private fun changeSocialMediaTwitter(value: String) {
        _uiState.update { it.copy(twitterId = value) }
    }

    private fun changeSocialMediaTiktok(value: String) {
        _uiState.update { it.copy(tiktokId = value) }
    }

    private fun changeSocialMediaYoutube(value: String) {
        _uiState.update { it.copy(youtubeId = value) }
    }

    private fun submitSocialMedia() {
        val currentState = _uiState.value
        _uiState.update { it.copy(isSavingSocialMedia = true) }
        viewModelScope.launch {
            when (val result = updateCafeSocialMediaUseCase.invoke(
                cafeId = cafeId,
                instagramId = currentState.instagramId.trim().takeIf { it.isNotEmpty() },
                twitterId = currentState.twitterId.trim().takeIf { it.isNotEmpty() },
                tiktokId = currentState.tiktokId.trim().takeIf { it.isNotEmpty() },
                youtubeId = currentState.youtubeId.trim().takeIf { it.isNotEmpty() }
            )) {
                is AppResult.Success -> {
                    val newSocialMedia = buildMap {
                        currentState.instagramId.trim().takeIf { it.isNotEmpty() }?.let { put("instagram", it) }
                        currentState.twitterId.trim().takeIf { it.isNotEmpty() }?.let { put("twitter", it) }
                        currentState.tiktokId.trim().takeIf { it.isNotEmpty() }?.let { put("tiktok", it) }
                        currentState.youtubeId.trim().takeIf { it.isNotEmpty() }?.let { put("youtube", it) }
                    }
                    _uiState.update {
                        it.copy(
                            cafe = it.cafe?.copy(socialMedia = newSocialMedia),
                            isSavingSocialMedia = false,
                            isSocialMediaSheetVisible = false,
                            infoMessage = "dashboard_info_social_media_saved"
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSavingSocialMedia = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun dismissSocialMediaSheet() {
        _uiState.update { it.copy(isSocialMediaSheetVisible = false) }
    }

    private fun dismissReservationSheet() {
        _uiState.update { it.copy(isReservationSheetVisible = false, reservationUrl = _uiState.value.cafe?.reservationUrl.orEmpty()) }
    }

    private fun changeReservationUrl(value: String) {
        _uiState.update { it.copy(reservationUrl = value) }
    }

    private fun submitReservation() {
        val currentState = _uiState.value
        val url = currentState.reservationUrl.trim()
        _uiState.update { it.copy(isSavingReservation = true) }
        viewModelScope.launch {
            when (val result = updateCafeReservationUrlUseCase.invoke(
                cafeId = cafeId,
                reservationUrl = url.takeIf { it.isNotEmpty() }
            )) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cafe = it.cafe?.copy(reservationUrl = url.takeIf { it.isNotEmpty() }),
                            isSavingReservation = false,
                            isReservationSheetVisible = false,
                            infoMessage = "dashboard_info_reservation_saved"
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSavingReservation = false,
                            infoMessage = result.error.toString()
                        )
                    }
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
            _uiState.update { it.copy(infoMessage = "dashboard_info_select_cast_for_delete") }
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
                    infoMessage = "dashboard_info_select_cast_for_delete"
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
                            infoMessage = "dashboard_info_cast_deleted"
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
                    _uiState.update { it.copy(infoMessage = "dashboard_info_cast_claim_approved") }
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
                    _uiState.update { it.copy(infoMessage = "dashboard_info_cast_claim_rejected") }
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
            cafeDetailEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CafeDetailEvent.CafeInfoUpdated -> if (event.cafeId == cafeId) {
                        patchCafeInfo(event.cafe)
                    }
                    is CafeDetailEvent.FavoriteToggled -> Unit
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
            bannerEventPublisher.events.collectLatest { event ->
                when (event) {
                    is BannerEvent.Created -> if (event.banner.cafeId == cafeId) {
                        loadCafeDashboard()
                    }
                    is BannerEvent.Updated -> if (event.banner.cafeId == cafeId) {
                        patchUpdatedBannerPreview(event.banner)
                    }
                    is BannerEvent.Deleted -> if (event.banner.cafeId == cafeId) {
                        loadCafeDashboard()
                    }
                }
            }
        }
    }

    private fun patchUpdatedBannerPreview(updatedBanner: HomeBanner) {
        _uiState.update { state ->
            val currentCafe = state.cafe
            if (currentCafe == null) {
                state
            } else {
                val currentPreview = currentCafe.homeBannerPreview
                val statusLabel = when (updatedBanner.statusLabel.uppercase()) {
                    "ACTIVE" -> "dashboard_banner_status_active"
                    "SCHEDULED" -> "dashboard_banner_status_scheduled"
                    else -> "dashboard_banner_status_hidden"
                }
                state.copy(
                    cafe = currentCafe.copy(
                        homeBannerPreview = currentPreview.copy(
                            title = updatedBanner.title,
                            period = "dashboard_banner_period_days:${updatedBanner.displayDays}",
                            statusLabel = statusLabel,
                            imageUrl = updatedBanner.imageUrl
                        )
                    )
                )
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
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastDomainEvent.Created -> if (event.cafeId == cafeId) {
                        refreshCastPreviews()
                    }
                    is CastDomainEvent.Updated -> if (event.cafeId == cafeId) {
                        _uiState.update { state ->
                            state.copy(
                                castPreviews = state.castPreviews.map { preview ->
                                    if (preview.id == event.cast.id) {
                                        preview.copy(
                                            name = event.cast.name,
                                            profileImage = event.cast.profileImage
                                        )
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

    private fun observeVisitEvent() {
        jobs[TaskKey.OBSERVE_VISIT_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_VISIT_EVENT] = viewModelScope.launch {
            visitEventPublisher.events.collectLatest { event ->
                when (event) {
                    is VisitEvent.Created -> if (event.cafeId == cafeId) {
                        _uiState.update { state ->
                            state.copy(cafe = state.cafe?.copy(todayCheckIns = state.cafe.todayCheckIns + 1))
                        }
                    }
                    is VisitEvent.Deleted -> Unit
                }
            }
        }
    }

    private fun observeCastClaimEvent() {
        jobs[TaskKey.OBSERVE_CAST_CLAIM_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_CLAIM_EVENT] = viewModelScope.launch {
            castClaimEventPublisher.events.collectLatest { event ->
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

    private fun startCastClaimPolling() {
        jobs[TaskKey.POLL_CAST_CLAIM]?.cancel()
        jobs[TaskKey.POLL_CAST_CLAIM] = viewModelScope.launch {
            while (isActive) {
                delay(CAST_CLAIM_POLLING_INTERVAL_MILLIS)

                if (isActive) {
                    refreshClaimData(resetMessage = false)
                }
            }
        }
    }

    fun onAction(action: CafeDashboardAction) {
        when (action) {
            CafeDashboardAction.ClickBack -> clickBack()
            is CafeDashboardAction.ClickShortcut -> clickShortcut(action.shortcut)
            CafeDashboardAction.ClickCreateBanner -> clickCreateBanner()
            CafeDashboardAction.DismissExternalLinkSheet -> dismissExternalLinkSheet()
            is CafeDashboardAction.ChangeExternalLinkTitle -> changeExternalLinkTitle(action.value)
            is CafeDashboardAction.ChangeExternalLinkUrl -> changeExternalLinkUrl(action.value)
            CafeDashboardAction.SubmitExternalLink -> submitExternalLink()
            is CafeDashboardAction.ClickExternalLinkItem -> clickExternalLinkItem(action.linkId)
            is CafeDashboardAction.ClickEditExternalLink -> clickEditExternalLink(action.linkId)
            is CafeDashboardAction.ClickDeleteExternalLink -> clickDeleteExternalLink(action.linkId)
            is CafeDashboardAction.ClickCastSchedule -> clickCastSchedule(action.castId)
            CafeDashboardAction.ClickDeleteCast -> clickDeleteCast()
            CafeDashboardAction.ConfirmDeleteCast -> confirmDeleteCast()
            CafeDashboardAction.DismissDeleteCastDialog -> dismissDeleteCastDialog()
            is CafeDashboardAction.ClickApproveCastClaim -> clickApproveCastClaim(action.claimId)
            is CafeDashboardAction.ClickRejectCastClaim -> clickRejectCastClaim(action.claimId)
            CafeDashboardAction.ClickLoadMoreCasts -> clickLoadMoreCasts()
            CafeDashboardAction.DismissInfoMessage -> dismissInfoMessage()
            is CafeDashboardAction.ChangeSocialMediaInstagram -> changeSocialMediaInstagram(action.value)
            is CafeDashboardAction.ChangeSocialMediaTwitter -> changeSocialMediaTwitter(action.value)
            is CafeDashboardAction.ChangeSocialMediaTiktok -> changeSocialMediaTiktok(action.value)
            is CafeDashboardAction.ChangeSocialMediaYoutube -> changeSocialMediaYoutube(action.value)
            CafeDashboardAction.SubmitSocialMedia -> submitSocialMedia()
            CafeDashboardAction.DismissSocialMediaSheet -> dismissSocialMediaSheet()
            CafeDashboardAction.DismissReservationSheet -> dismissReservationSheet()
            is CafeDashboardAction.ChangeReservationUrl -> changeReservationUrl(action.value)
            CafeDashboardAction.SubmitReservation -> submitReservation()
        }
    }

    init {
        observeBannerEvent()
        observeCafeDetailEvent()
        observeCastClaimEvent()
        observeCastEvent()
        observeVisitEvent()
        startCastClaimPolling()
        loadExternalLinks()
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
        OBSERVE_CAST_CLAIM_EVENT,
        OBSERVE_VISIT_EVENT,
        POLL_CAST_CLAIM
    }
}

private const val CAST_CLAIM_POLLING_INTERVAL_MILLIS = 30_000L
private const val PAGINATION_DELAY_MILLIS = 1_000L
