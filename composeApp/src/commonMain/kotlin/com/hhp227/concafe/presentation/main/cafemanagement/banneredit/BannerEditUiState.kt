package com.hhp227.concafe.presentation.main.cafemanagement.banneredit

import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem

data class BannerEditUiState(
    val editingBannerId: String? = null,
    val selectedImageLabel: String? = null,
    val originalImageUrl: String? = null,
    val title: String = "",
    val subtitle: String = "",
    val selectedTarget: BannerTargetType = BannerTargetType.CAFE_DETAIL,
    val targetValue: String = "",
    val displayDays: Int = 5,
    val ownedCafeOptions: List<CafeManagementData.OwnedCafeSummary> = emptyList(),
    val selectedCafeId: String? = null,
    val selectedNoticeId: String? = null,
    val selectedEventId: String? = null,
    val selectorType: BannerSelectorType? = null,
    val selectorQuery: String = "",
    val noticeSelectorOptions: List<CafeNoticeManagementItem> = emptyList(),
    val eventSelectorOptions: List<CafeEventManagementItem> = emptyList(),
    val isSelectorLoading: Boolean = false,
    val isAdmin: Boolean = false,
    val isImageRequiredAlertVisible: Boolean = false,
    val isSaving: Boolean = false,
    val infoMessage: String? = null
) {
    val isEditMode: Boolean
        get() = !editingBannerId.isNullOrBlank()

    val displayDaysLabelValue: Int
        get() = displayDays

    val targetFieldPlaceholderKey: String
        get() = selectedTarget.placeholder

    val selectorTitleKey: String
        get() = selectorType?.title.orEmpty()

    val selectorSearchPlaceholderKey: String
        get() = selectorType?.searchPlaceholder.orEmpty()

    val selectedCafeOption: CafeManagementData.OwnedCafeSummary?
        get() = selectedCafeId?.let { id -> ownedCafeOptions.firstOrNull { it.id == id } }

    val selectedContentTitle: String?
        get() = when (selectedTarget) {
            BannerTargetType.NOTICE -> {
                selectedNoticeId?.let { id -> noticeSelectorOptions.firstOrNull { it.id == id }?.title }
            }
            BannerTargetType.EVENT_DETAIL -> {
                selectedEventId?.let { id -> eventSelectorOptions.firstOrNull { it.id == id }?.title }
            }
            else -> null
        }

    val selectedContentSubtitle: String?
        get() = when (selectedTarget) {
            BannerTargetType.NOTICE -> {
                selectedNoticeId?.let { id -> noticeSelectorOptions.firstOrNull { it.id == id }?.displayDate }
            }
            BannerTargetType.EVENT_DETAIL -> {
                selectedEventId?.let { id -> eventSelectorOptions.firstOrNull { it.id == id }?.periodText }
            }
            else -> null
        }

    val filteredCafeSelectorOptions: List<CafeManagementData.OwnedCafeSummary>
        get() = if (selectorQuery.isBlank()) {
            ownedCafeOptions
        } else {
            ownedCafeOptions.filter {
                it.name.contains(selectorQuery, ignoreCase = true) ||
                    it.city.contains(selectorQuery, ignoreCase = true)
            }
        }

    val activeSelectorItemCount: Int
        get() = when (selectorType) {
            BannerSelectorType.CAFE -> filteredCafeSelectorOptions.size
            BannerSelectorType.NOTICE -> noticeSelectorOptions.size
            BannerSelectorType.EVENT -> eventSelectorOptions.size
            null -> 0
        }

    val isSaveEnabled: Boolean
        get() = title.isNotBlank() &&
            subtitle.isNotBlank() &&
            !selectedImageLabel.isNullOrBlank() &&
            targetValue.isNotBlank() &&
            !isSaving

    val targetSelectionLabelKey: String
        get() = when (selectedTarget) {
            BannerTargetType.NOTICE -> "banneredit_target_notice_select_label"
            BannerTargetType.EVENT_DETAIL -> "banneredit_target_event_select_label"
            else -> ""
        }

    val targetSelectionPlaceholderKey: String
        get() = when (selectedTarget) {
            BannerTargetType.NOTICE -> "banneredit_target_notice_select_placeholder"
            BannerTargetType.EVENT_DETAIL -> "banneredit_target_event_select_placeholder"
            else -> ""
        }
}

enum class BannerTargetType(
    val label: String,
    val placeholder: String
) {
    CAFE_DETAIL("banneredit_target_cafe_detail", "banneredit_target_placeholder_cafe"),
    EVENT_DETAIL("banneredit_target_event_detail", "banneredit_target_placeholder_event"),
    NOTICE("banneredit_target_notice", "banneredit_target_placeholder_notice"),
    EXTERNAL_LINK("banneredit_target_external_link", "banneredit_target_placeholder_external")
}

enum class BannerSelectorType(
    val title: String,
    val searchPlaceholder: String
) {
    CAFE("banneredit_selector_title_cafe", "banneredit_selector_search_cafe"),
    NOTICE("banneredit_selector_title_notice", "banneredit_selector_search_notice"),
    EVENT("banneredit_selector_title_event", "banneredit_selector_search_event")
}
