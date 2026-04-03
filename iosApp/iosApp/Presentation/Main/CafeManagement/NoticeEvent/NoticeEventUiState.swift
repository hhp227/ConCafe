//
//  NoticeEventUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

struct NoticeEventUiState {
    var selectedTab: NoticeEventTab = .notice
    var query: String = ""
    var notices: [CafeNoticeManagementItem] = []
    var events: [CafeEventManagementItem] = []
    var isLoadingNotices: Bool = false
    var isLoadingMoreNotices: Bool = false
    var noticeNextCursor: String? = nil
    var canLoadMoreNotices: Bool = false
    var isLoadingEvents: Bool = false
    var isLoadingMoreEvents: Bool = false
    var eventNextCursor: String? = nil
    var canLoadMoreEvents: Bool = false
    var infoMessage: String? = nil
    var isFormSheetVisible: Bool = false
    var isSubmittingForm: Bool = false
    var formEditingId: String? = nil
    var formTitle: String = ""
    var formContent: String = ""
    var formImageUrl: String = ""
    var formPinned: Bool = false
    var formReservedAt: String = ""

    var formSheetTitle: String {
        if selectedTab == .notice {
            return formEditingId == nil ? String(localized: String.LocalizationValue("noticeevent_form_sheet_title_notice_create"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_sheet_title_notice_edit"), table: "Localizable")
        }
        return formEditingId == nil ? String(localized: String.LocalizationValue("noticeevent_form_sheet_title_event_create"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_sheet_title_event_edit"), table: "Localizable")
    }

    var formTitlePlaceholder: String {
        selectedTab == .notice ? String(localized: String.LocalizationValue("noticeevent_form_title_placeholder_notice"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_title_placeholder_event"), table: "Localizable")
    }

    var formContentPlaceholder: String {
        selectedTab == .notice ? String(localized: String.LocalizationValue("noticeevent_form_content_placeholder_notice"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_content_placeholder_event"), table: "Localizable")
    }

    var formSubmitLabel: String {
        if selectedTab == .notice {
            return formEditingId == nil ? String(localized: String.LocalizationValue("noticeevent_form_submit_notice_create"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_submit_notice_edit"), table: "Localizable")
        }
        return formEditingId == nil ? String(localized: String.LocalizationValue("noticeevent_form_submit_event_create"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_submit_event_edit"), table: "Localizable")
    }

    var formScheduleLabel: String {
        selectedTab == .notice ? String(localized: String.LocalizationValue("noticeevent_form_schedule_label_notice"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_schedule_label_event"), table: "Localizable")
    }

    var formSchedulePlaceholder: String {
        selectedTab == .notice ? String(localized: String.LocalizationValue("noticeevent_form_schedule_placeholder_notice"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_schedule_placeholder_event"), table: "Localizable")
    }

    var showsPinnedSection: Bool {
        selectedTab == .notice
    }

    var showsImageSection: Bool {
        selectedTab == .event
    }

    var hasAttachedImage: Bool {
        !formImageUrl.isEmpty
    }

    var formImageTitle: String {
        hasAttachedImage ? String(localized: String.LocalizationValue("noticeevent_form_image_title_attached"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_form_image_title_empty"), table: "Localizable")
    }

    var formImageDescription: String {
        String(localized: String.LocalizationValue("noticeevent_form_image_description"), table: "Localizable")
    }

    var isCurrentTabLoading: Bool {
        selectedTab == .notice ? isLoadingNotices : isLoadingEvents
    }

    var isCurrentTabLoadingMore: Bool {
        selectedTab == .notice ? isLoadingMoreNotices : isLoadingMoreEvents
    }

    var isCurrentTabEmpty: Bool {
        selectedTab == .notice ? notices.isEmpty : events.isEmpty
    }

    var isFormSubmitEnabled: Bool {
        !isSubmittingForm &&
        !formTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !formContent.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        (!showsImageSection || hasAttachedImage)
    }
}

enum NoticeEventTab: String, CaseIterable {
    case notice = "noticeevent_tab_notice"
    case event = "noticeevent_tab_event"
}

