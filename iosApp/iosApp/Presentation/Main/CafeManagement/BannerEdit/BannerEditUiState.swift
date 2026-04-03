//
//  BannerEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Shared

struct BannerEditUiState {
    var editingBannerId: String? = nil
    var selectedImageLabel: String? = nil
    var originalImageUrl: String? = nil
    var title = ""
    var subtitle = ""
    var selectedTarget: BannerTargetType = .cafeDetail
    var targetValue = ""
    var displayDays = 5
    var ownedCafeOptions: [CafeManagementData.OwnedCafeSummary] = []
    var selectedCafeId: String? = nil
    var selectedNoticeId: String? = nil
    var selectedEventId: String? = nil
    var selectorType: BannerSelectorType? = nil
    var selectorQuery = ""
    var noticeSelectorOptions: [CafeNoticeManagementItem] = []
    var eventSelectorOptions: [CafeEventManagementItem] = []
    var isSelectorLoading = false
    var isAdmin = false
    var isImageRequiredAlertVisible = false
    var isSaving = false
    var infoMessage: String? = nil

    var isEditMode: Bool {
        !(editingBannerId?.isEmpty ?? true)
    }

    var displayDaysLabelValue: Int {
        displayDays
    }

    var targetFieldPlaceholderKey: String {
        selectedTarget.placeholder
    }

    var selectorTitleKey: String {
        selectorType?.title ?? ""
    }

    var selectorSearchPlaceholderKey: String {
        selectorType?.searchPlaceholder ?? ""
    }

    var selectedCafeOption: CafeManagementData.OwnedCafeSummary? {
        guard let selectedCafeId else { return nil }
        return ownedCafeOptions.first(where: { $0.id == selectedCafeId })
    }

    var selectedContentTitle: String? {
        switch selectedTarget {
        case .notice:
            guard let selectedNoticeId else { return nil }
            return noticeSelectorOptions.first(where: { $0.id == selectedNoticeId })?.title
        case .eventDetail:
            guard let selectedEventId else { return nil }
            return eventSelectorOptions.first(where: { $0.id == selectedEventId })?.title
        default:
            return nil
        }
    }

    var selectedContentSubtitle: String? {
        switch selectedTarget {
        case .notice:
            guard let selectedNoticeId else { return nil }
            return noticeSelectorOptions.first(where: { $0.id == selectedNoticeId })?.displayDate
        case .eventDetail:
            guard let selectedEventId else { return nil }
            return eventSelectorOptions.first(where: { $0.id == selectedEventId })?.periodText
        default:
            return nil
        }
    }

    var filteredCafeSelectorOptions: [CafeManagementData.OwnedCafeSummary] {
        if selectorQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return ownedCafeOptions
        }
        return ownedCafeOptions.filter { option in
            option.name.localizedCaseInsensitiveContains(selectorQuery) ||
            option.city.localizedCaseInsensitiveContains(selectorQuery)
        }
    }

    var activeSelectorItemCount: Int {
        switch selectorType {
        case .cafe:
            return filteredCafeSelectorOptions.count
        case .notice:
            return noticeSelectorOptions.count
        case .event:
            return eventSelectorOptions.count
        case .none:
            return 0
        }
    }

    var targetSelectionLabelKey: String {
        switch selectedTarget {
        case .notice:
            return "banneredit_target_notice_select_label"
        case .eventDetail:
            return "banneredit_target_event_select_label"
        default:
            return ""
        }
    }

    var targetSelectionPlaceholderKey: String {
        switch selectedTarget {
        case .notice:
            return "banneredit_target_notice_select_placeholder"
        case .eventDetail:
            return "banneredit_target_event_select_placeholder"
        default:
            return ""
        }
    }

    var isSaveEnabled: Bool {
        !(selectedImageLabel?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true) &&
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !subtitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !isSaving
    }
}

enum BannerTargetType: String, CaseIterable, Identifiable {
    case cafeDetail = "banneredit_target_cafe_detail"
    case eventDetail = "banneredit_target_event_detail"
    case notice = "banneredit_target_notice"
    case externalLink = "banneredit_target_external_link"

    var id: String { rawValue }

    var placeholder: String {
        switch self {
        case .cafeDetail:
            return "banneredit_target_placeholder_cafe"
        case .eventDetail:
            return "banneredit_target_placeholder_event"
        case .notice:
            return "banneredit_target_placeholder_notice"
        case .externalLink:
            return "banneredit_target_placeholder_external"
        }
    }
}

enum BannerSelectorType {
    case cafe
    case notice
    case event

    var title: String {
        switch self {
        case .cafe: return "banneredit_selector_title_cafe"
        case .notice: return "banneredit_selector_title_notice"
        case .event: return "banneredit_selector_title_event"
        }
    }

    var searchPlaceholder: String {
        switch self {
        case .cafe: return "banneredit_selector_search_cafe"
        case .notice: return "banneredit_selector_search_notice"
        case .event: return "banneredit_selector_search_event"
        }
    }
}
