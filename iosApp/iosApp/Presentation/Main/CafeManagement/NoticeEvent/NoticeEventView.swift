//
//  NoticeEventView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit
import Shared

struct NoticeEventView: View {
    let cafeId: String

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: NoticeEventViewModel

    var body: some View {
        NoticeEventContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(String(localized: String.LocalizationValue("noticeevent_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .searchable(
            text: Binding(
                get: { viewModel.uiState.query },
                set: { viewModel.onAction(.changeQuery($0)) }
            ),
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: viewModel.uiState.selectedTab == .notice ? String(localized: String.LocalizationValue("noticeevent_search_placeholder_notice"), table: "Localizable") : String(localized: String.LocalizationValue("noticeevent_search_placeholder_event"), table: "Localizable")
        )
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.isFormSheetVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissFormSheet)
                    }
                }
            )
        ) {
            NoticeEventFormSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
            .compatLargeSheetDetent()
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
    }

    init(
        cafeId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: NoticeEventViewModel(cafeId: cafeId))
    }
}

private struct NoticeEventContentView: View {
    let uiState: NoticeEventUiState

    let onAction: (NoticeEventAction) -> Void
    
    @State private var countBeforeLoad = (notice: 0, event: 0)

    var body: some View {
        ScrollViewReader { proxy in
            ZStack(alignment: .bottomTrailing) {
                Group {
                    if UITraitCollection.current.userInterfaceStyle == .dark {
                        ConCafeColors.background
                    } else {
                        LinearGradient(
                            colors: [Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }), Color(uiColor: .systemGroupedBackground)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
                }
                .ignoresSafeArea()
                VStack(spacing: 0) {
                    ConCafeTabBar(
                        labels: NoticeEventTab.allCases.map { String(localized: String.LocalizationValue($0.rawValue), table: "Localizable") },
                        selectedIndex: NoticeEventTab.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
                        backgroundColor: UITraitCollection.current.userInterfaceStyle == .dark ? ConCafeColors.background : Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }),
                        onSelect: { index in
                            onAction(.selectTab(NoticeEventTab.allCases[index]))
                        }
                    )
                    ScrollView {
                        VStack(spacing: 14) {
                            if let infoMessage = uiState.infoMessage {
                                infoBanner(
                                    message: {
                                        switch infoMessage {
                                        case "noticeevent_validation_cafe_required":
                                            return String(localized: String.LocalizationValue("noticeevent_validation_cafe_required"), table: "Localizable")
                                        case "noticeevent_validation_notice_required":
                                            return String(localized: String.LocalizationValue("noticeevent_validation_notice_required"), table: "Localizable")
                                        case "noticeevent_validation_event_required":
                                            return String(localized: String.LocalizationValue("noticeevent_validation_event_required"), table: "Localizable")
                                        case "noticeevent_validation_title_required":
                                            return String(localized: String.LocalizationValue("noticeevent_validation_title_required"), table: "Localizable")
                                        case "noticeevent_validation_content_required":
                                            return String(localized: String.LocalizationValue("noticeevent_validation_content_required"), table: "Localizable")
                                        case "noticeevent_validation_event_image_required":
                                            return String(localized: String.LocalizationValue("noticeevent_validation_event_image_required"), table: "Localizable")
                                        case "noticeevent_info_notice_edit_target_not_found":
                                            return String(localized: String.LocalizationValue("noticeevent_info_notice_edit_target_not_found"), table: "Localizable")
                                        case "noticeevent_info_event_edit_target_not_found":
                                            return String(localized: String.LocalizationValue("noticeevent_info_event_edit_target_not_found"), table: "Localizable")
                                        case "noticeevent_info_notice_load_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_notice_load_failed"), table: "Localizable")
                                        case "noticeevent_info_event_load_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_event_load_failed"), table: "Localizable")
                                        case "noticeevent_info_notice_created":
                                            return String(localized: String.LocalizationValue("noticeevent_info_notice_created"), table: "Localizable")
                                        case "noticeevent_info_notice_updated":
                                            return String(localized: String.LocalizationValue("noticeevent_info_notice_updated"), table: "Localizable")
                                        case "noticeevent_info_event_created":
                                            return String(localized: String.LocalizationValue("noticeevent_info_event_created"), table: "Localizable")
                                        case "noticeevent_info_event_updated":
                                            return String(localized: String.LocalizationValue("noticeevent_info_event_updated"), table: "Localizable")
                                        case "noticeevent_info_notice_create_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_notice_create_failed"), table: "Localizable")
                                        case "noticeevent_info_notice_update_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_notice_update_failed"), table: "Localizable")
                                        case "noticeevent_info_event_create_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_event_create_failed"), table: "Localizable")
                                        case "noticeevent_info_event_update_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_event_update_failed"), table: "Localizable")
                                        case "noticeevent_info_notice_delete_success":
                                            return String(localized: String.LocalizationValue("noticeevent_info_notice_delete_success"), table: "Localizable")
                                        case "noticeevent_info_notice_delete_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_notice_delete_failed"), table: "Localizable")
                                        case "noticeevent_info_event_delete_success":
                                            return String(localized: String.LocalizationValue("noticeevent_info_event_delete_success"), table: "Localizable")
                                        case "noticeevent_info_event_delete_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_event_delete_failed"), table: "Localizable")
                                        case "noticeevent_info_image_upload_failed":
                                            return String(localized: String.LocalizationValue("noticeevent_info_image_upload_failed"), table: "Localizable")
                                        case "noticeevent_info_more_events_next_step":
                                            return String(localized: String.LocalizationValue("noticeevent_info_more_events_next_step"), table: "Localizable")
                                        case "noticeevent_info_image_pick_required":
                                            return String(localized: String.LocalizationValue("noticeevent_info_image_pick_required"), table: "Localizable")
                                        case "noticeevent_info_reserve_schedule_next_step":
                                            return String(localized: String.LocalizationValue("noticeevent_info_reserve_schedule_next_step"), table: "Localizable")
                                        default:
                                            return infoMessage
                                        }
                                    }()
                                )
                                .padding(.horizontal, 16)
                            }
                            if uiState.isCurrentTabLoading && uiState.isCurrentTabEmpty {
                                loadingCard
                                    .padding(.horizontal, 16)
                            } else if uiState.selectedTab == .notice && uiState.notices.isEmpty {
                                emptyStateCard(message: String(localized: String.LocalizationValue("noticeevent_empty_notice"), table: "Localizable"))
                                    .padding(.horizontal, 16)
                            } else if uiState.selectedTab == .event && uiState.events.isEmpty {
                                emptyStateCard(message: String(localized: String.LocalizationValue("noticeevent_empty_event"), table: "Localizable"))
                                    .padding(.horizontal, 16)
                            } else if uiState.selectedTab == .notice {
                                ForEach(uiState.notices, id: \.id) { item in
                                    noticeCard(item)
                                        .id(item.id)
                                        .padding(.horizontal, 16)
                                }
                            } else {
                                ForEach(uiState.events, id: \.id) { item in
                                    eventCard(item)
                                        .id(item.id)
                                        .padding(.horizontal, 16)
                                }
                            }
                            let canLoadMoreCurrentTab = uiState.selectedTab == .notice ? uiState.canLoadMoreNotices : uiState.canLoadMoreEvents

                            if canLoadMoreCurrentTab || uiState.isCurrentTabLoadingMore {
                                Color.clear
                                    .frame(height: 1)
                                    .onAppear {
                                        if uiState.selectedTab == .notice {
                                            guard canLoadMoreCurrentTab, !uiState.isCurrentTabLoadingMore else { return }
                                            let count = uiState.notices.count
                                            countBeforeLoad.notice = count
                                            onAction(.loadMoreNotices)
                                        } else {
                                            guard canLoadMoreCurrentTab, !uiState.isCurrentTabLoadingMore else { return }
                                            let count = uiState.events.count
                                            countBeforeLoad.event = count
                                            onAction(.loadMoreEvents)
                                        }
                                    }
                                    .onDisappear {
                                        if uiState.selectedTab == .notice {
                                            countBeforeLoad.notice = -1
                                        } else {
                                            countBeforeLoad.event = -1
                                        }
                                    }
                            }
                            if uiState.isCurrentTabLoadingMore {
                                ProgressView()
                                    .tint(ConCafeColors.primary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 8)
                            }
                        }
                        .padding(.top, 12)
                        .padding(.bottom, 110)
                    }
                }
                .onChange(of: uiState.notices.count) { newCount in
                    guard countBeforeLoad.notice != 0, countBeforeLoad.notice != -1 else { return }
                    let preCount = abs(countBeforeLoad.notice)
                    let wasSubsequent = countBeforeLoad.notice > 0
                    countBeforeLoad.notice = -1
                    guard newCount > preCount, wasSubsequent, preCount > 0 else { return }
                    guard uiState.selectedTab == .notice else { return }
                    proxy.scrollTo(uiState.notices[preCount - 1].id, anchor: .bottom)
                }
                .onChange(of: uiState.events.count) { newCount in
                    guard countBeforeLoad.event != 0, countBeforeLoad.event != -1 else { return }
                    let preCount = abs(countBeforeLoad.event)
                    let wasSubsequent = countBeforeLoad.event > 0
                    countBeforeLoad.event = -1
                    guard newCount > preCount, wasSubsequent, preCount > 0 else { return }
                    guard uiState.selectedTab == .event else { return }
                    proxy.scrollTo(uiState.events[preCount - 1].id, anchor: .bottom)
                }
                Button {
                    onAction(.clickRegister)
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "plus")
                        Text(String(localized: String.LocalizationValue("noticeevent_register_cta"), table: "Localizable"))
                            .fontWeight(.bold)
                    }
                    .foregroundStyle(.primary)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                    .background(ConCafeColors.primaryContainer)
                    .clipShape(Capsule())
                    .shadow(color: ConCafeColors.primaryContainer.opacity(0.45), radius: 14, x: 0, y: 8)
                }
                .padding(.trailing, 16)
                .padding(.bottom, 20)
            }
        }
    }

    private func noticeCard(_ item: CafeNoticeManagementItem) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                HStack(spacing: 6) {
                    if item.isPinned {
                        statusChip("PINNED", container: ConCafeColors.primaryContainer, content: .primary)
                    }
                    switch item.statusAccent {
                    case .published:
                        statusChip(item.statusLabel, container: ConCafeColors.successContainer, content: ConCafeColors.success)
                    case .draft:
                        statusChip(item.statusLabel, container: ConCafeColors.surfaceVariant, content: .secondary)
                    case .ended:
                        statusChip(item.statusLabel, container: ConCafeColors.errorContainer, content: ConCafeColors.textSecondary)
                    default:
                        EmptyView()
                    }
                }
                Spacer()
                HStack(spacing: 6) {
                    iconButton("square.and.pencil") { onAction(.clickEditNotice(item.id)) }
                    iconButton("trash") { onAction(.clickDeleteNotice(item.id)) }
                }
            }
            Text(item.title)
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
            Text(item.displayDate)
                .font(.caption)
                .foregroundStyle(ConCafeColors.textMuted)
        }
        .padding(18)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 4)
    }

    private func eventCard(_ item: CafeEventManagementItem) -> some View {
        let imageUrl = item.imageUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        return VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                if let url = URL(string: imageUrl), !imageUrl.isEmpty {
                    CachedAsyncImage(
                        url: url,
                        placeholder: eventImagePlaceholder,
                        displaySize: .medium
                    )
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    eventImagePlaceholder
                }
            }
            .frame(height: 168)
            .frame(maxWidth: .infinity)
            .clipped()
            .saturation(item.isDimmed ? 0 : 1)
            .overlay(item.isDimmed ? Color.white.opacity(0.16) : Color.clear)
            .overlay(alignment: .topLeading) {
                statusChip(
                    item.statusLabel,
                    container: item.isDimmed ? ConCafeColors.textSecondary : ConCafeColors.primaryContainer,
                    content: item.isDimmed ? .white : .primary
                )
                .padding(12)
            }
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(item.title)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                    Spacer()
                    iconButton("square.and.pencil") { onAction(.clickEditEvent(item.id)) }
                }
                HStack(spacing: 6) {
                    Image(systemName: "calendar")
                        .font(.caption)
                    Text(item.periodText)
                        .font(.caption)
                }
                .foregroundStyle(ConCafeColors.textMuted)
            }
            .padding(18)
        }
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 4)
        .opacity(item.isDimmed ? 0.74 : 1)
    }

    private var eventImagePlaceholder: some View {
        LinearGradient(
            colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private func statusChip(_ text: String, container: Color, content: Color) -> some View {
        Text(text)
            .font(.caption2.weight(.bold))
            .foregroundStyle(content)
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(container)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func iconButton(_ systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
                .frame(width: 28, height: 28)
        }
        .buttonStyle(.plain)
    }

    private func formField(label: String, value: Binding<String>, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.secondary)
                .padding(.leading, 4)
            ConCafeFormField(
                label: "",
                text: value,
                placeholder: placeholder
            )
        }
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(ConCafeColors.goldDeep)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(ConCafeColors.goldDeep)
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(ConCafeColors.warningContainer)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var loadingCard: some View {
        ShimmerCardListSkeleton(itemCount: 2, imageHeight: 120)
            .frame(maxWidth: .infinity)
    }

    private func emptyStateCard(message: String) -> some View {
        HStack {
            Spacer()
            Text(message)
                .font(.subheadline)
                .foregroundStyle(ConCafeColors.textMuted)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.vertical, 28)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct NoticeEventFormSheet: View {
    let uiState: NoticeEventUiState

    let onAction: (NoticeEventAction) -> Void

    @State private var isImagePickerPresented = false
    @State private var isEventPeriodEditorVisible = false
    @State private var eventStartDate = Date()
    @State private var eventEndDate = Date()

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                Capsule()
                    .fill(ConCafeColors.outline)
                    .frame(width: 48, height: 5)
                    .padding(.top, 12)
                    .padding(.bottom, 6)
                HStack {
                    Text(uiState.formSheetTitle)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.primary)
                    Spacer()
                    Button {
                        onAction(.dismissFormSheet)
                    } label: {
                        Image(systemName: "xmark")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.secondary)
                            .frame(width: 28, height: 28)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(String(localized: String.LocalizationValue("noticeevent_form_label_title"), table: "Localizable"))
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(.secondary)
                                .padding(.leading, 4)
                            ConCafeFormField(
                                label: "",
                                text: Binding(
                                    get: { uiState.formTitle },
                                    set: { onAction(.changeFormTitle($0)) }
                                ),
                                placeholder: uiState.formTitlePlaceholder
                            )
                        }
                        VStack(alignment: .leading, spacing: 8) {
                            Text(String(localized: String.LocalizationValue("noticeevent_form_label_content"), table: "Localizable"))
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(.secondary)
                                .padding(.leading, 4)
                            ConCafeFormEditor(
                                label: "",
                                text: Binding(
                                    get: { uiState.formContent },
                                    set: { onAction(.changeFormContent($0)) }
                                ),
                                placeholder: uiState.formContentPlaceholder
                            )
                        }
                        if uiState.showsImageSection {
                            representativeImageSection
                        }
                        if uiState.selectedTab == .event {
                            eventOptionsSection
                        }
                        if uiState.showsPinnedSection {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(String(localized: String.LocalizationValue("noticeevent_pinned_title"), table: "Localizable"))
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(.primary)
                                    Text(String(localized: String.LocalizationValue("noticeevent_pinned_desc"), table: "Localizable"))
                                        .font(.caption)
                                        .foregroundStyle(ConCafeColors.textMuted)
                                }
                                Spacer()
                                Toggle(
                                    "",
                                    isOn: Binding(
                                        get: { uiState.formPinned },
                                        set: { onAction(.changeFormPinned($0)) }
                                    )
                                )
                                .labelsHidden()
                                .tint(ConCafeColors.primaryContainer)
                            }
                            .padding(16)
                            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        }
                        VStack(alignment: .leading, spacing: 8) {
                            Text(uiState.formScheduleLabel)
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(.secondary)
                                .padding(.leading, 4)
                            Button {
                                if uiState.selectedTab == .event {
                                    if let parsed = parseEventPeriod(uiState.formReservedAt) {
                                        eventStartDate = parsed.start
                                        eventEndDate = parsed.end
                                    } else {
                                        let now = Date()
                                        eventStartDate = now
                                        eventEndDate = now
                                    }
                                    withAnimation(.easeInOut(duration: 0.2)) {
                                        isEventPeriodEditorVisible.toggle()
                                    }
                                } else {
                                    onAction(.clickReserveSchedule)
                                }
                            } label: {
                                HStack {
                                    Text(uiState.formReservedAt.isEmpty ? uiState.formSchedulePlaceholder : uiState.formReservedAt)
                                        .foregroundStyle(.secondary)
                                    Spacer()
                                    Image(systemName: "calendar")
                                        .foregroundStyle(.secondary)
                                }
                                .padding(.horizontal, 16)
                                .frame(height: 56)
                                .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                                        .stroke(ConCafeColors.primaryContainer.opacity(0.3), style: StrokeStyle(lineWidth: 2, dash: [6]))
                                )
                            }
                            .buttonStyle(.plain)

                            if uiState.selectedTab == .event && isEventPeriodEditorVisible {
                                VStack(spacing: 10) {
                                    VStack(alignment: .leading, spacing: 6) {
                                        Text(String(localized: String.LocalizationValue("schedule_label_start_time"), table: "Localizable"))
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(.secondary)
                                        DatePicker(
                                            "",
                                            selection: $eventStartDate,
                                            displayedComponents: .date
                                        )
                                        .labelsHidden()
                                        .datePickerStyle(.compact)
                                    }
                                    VStack(alignment: .leading, spacing: 6) {
                                        Text(String(localized: String.LocalizationValue("schedule_label_end_time"), table: "Localizable"))
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(.secondary)
                                        DatePicker(
                                            "",
                                            selection: $eventEndDate,
                                            in: eventStartDate...Date.distantFuture,
                                            displayedComponents: .date
                                        )
                                        .labelsHidden()
                                        .datePickerStyle(.compact)
                                    }
                                    HStack(spacing: 8) {
                                        Button {
                                            onAction(.changeFormReservedAt(""))
                                            withAnimation(.easeInOut(duration: 0.2)) {
                                                isEventPeriodEditorVisible = false
                                            }
                                        } label: {
                                            Text(String(localized: String.LocalizationValue("noticeevent_remove"), table: "Localizable"))
                                                .frame(maxWidth: .infinity)
                                        }
                                        .buttonStyle(.bordered)

                                        Button {
                                            let start = min(eventStartDate, eventEndDate)
                                            let end = max(eventStartDate, eventEndDate)
                                            onAction(.changeFormReservedAt(formatEventPeriod(start: start, end: end)))
                                            withAnimation(.easeInOut(duration: 0.2)) {
                                                isEventPeriodEditorVisible = false
                                            }
                                        } label: {
                                            Text(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"))
                                                .frame(maxWidth: .infinity)
                                        }
                                        .buttonStyle(.borderedProminent)
                                        .tint(ConCafeColors.primaryContainer)
                                        .foregroundStyle(.primary)
                                    }
                                }
                                .padding(12)
                                .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
                    .padding(.bottom, 60)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(maxHeight: 720)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }))
            .ignoresSafeArea(edges: .bottom)
            bottomSubmitBar()
        }
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }))
        .sheet(isPresented: $isImagePickerPresented) {
            CompatImagePicker(
                onImageSelected: { image in
                    isImagePickerPresented = false
                    saveImageToTemporaryFileAsync(image) { imageUrl in
                        if let imageUrl {
                            onAction(.changeFormImage(imageUrl))
                        }
                    }
                },
                onDismiss: {
                    isImagePickerPresented = false
                }
            )
        }
    }

    private func bottomSubmitBar() -> some View {
        LinearGradient(
            colors: [Color.clear, Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }), Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white })],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(height: 24)
        .overlay(alignment: .bottom) {
            Button {
                onAction(.clickSubmitForm)
            } label: {
                Text(uiState.formSubmitLabel)
                    .font(.headline.weight(.bold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 60)
                    .foregroundStyle(uiState.isFormSubmitEnabled ? .primary : .secondary)
                    .background(uiState.isFormSubmitEnabled ? ConCafeColors.primaryContainer : ConCafeColors.primaryContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(!uiState.isFormSubmitEnabled)
            .padding(.horizontal, 24)
            .padding(.top, 10)
            .padding(.bottom, 24)
        }
    }

    private var eventOptionsSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: String.LocalizationValue("noticeevent_form_live_performance_title"), table: "Localizable"))
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.primary)
                    Text(String(localized: String.LocalizationValue("noticeevent_form_live_performance_desc"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(ConCafeColors.textMuted)
                }
                Spacer()
                Toggle(
                    "",
                    isOn: Binding(
                        get: { uiState.formHasLivePerformance },
                        set: { onAction(.changeFormHasLivePerformance($0)) }
                    )
                )
                .labelsHidden()
                .tint(ConCafeColors.primaryContainer)
            }
            .padding(16)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

            VStack(alignment: .leading, spacing: 8) {
                Text(String(localized: String.LocalizationValue("noticeevent_form_participant_cast_label"), table: "Localizable"))
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.secondary)
                    .padding(.leading, 4)
                if uiState.cafeCasts.isEmpty {
                    Text(String(localized: String.LocalizationValue("noticeevent_form_participant_cast_empty"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(ConCafeColors.textMuted)
                        .padding(.leading, 4)
                } else {
                    VStack(spacing: 8) {
                        ForEach(Array(uiState.cafeCasts.chunked(into: 2).enumerated()), id: \.offset) { _, rowItems in
                            HStack(spacing: 8) {
                                ForEach(rowItems, id: \.id) { cast in
                                    participantCastChip(cast)
                                }
                                if rowItems.count == 1 {
                                    Spacer()
                                        .frame(maxWidth: .infinity)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private func participantCastChip(_ cast: CafeCastPreview) -> some View {
        let isSelected = uiState.formParticipantCastIds.contains(cast.id)
        return Button {
            onAction(.toggleFormParticipantCast(cast.id))
        } label: {
            Text(cast.name)
                .font(.caption.weight(.semibold))
                .foregroundStyle(isSelected ? ConCafeColors.textPrimary : ConCafeColors.textSecondary)
                .lineLimit(1)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .background(isSelected ? ConCafeColors.primaryContainer : Color(uiColor: .secondarySystemGroupedBackground))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private var representativeImageSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: String.LocalizationValue("noticeevent_form_image_label"), table: "Localizable"))
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.secondary)
                .padding(.leading, 4)
            GeometryReader { proxy in
                ZStack(alignment: .bottomTrailing) {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [ConCafeColors.primaryContainer, Color(uiColor: .secondarySystemGroupedBackground)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    if uiState.hasAttachedImage {
                        NoticeEventFormImageView(imageUrl: uiState.formImageUrl)
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .clipped()
                    } else {
                        VStack(spacing: 8) {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 32, weight: .semibold))
                                .foregroundStyle(ConCafeColors.primary)
                            Text(uiState.formImageTitle)
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(ConCafeColors.textSecondary)
                        }
                        .frame(width: proxy.size.width, height: proxy.size.height, alignment: .center)
                    }
                    if uiState.hasAttachedImage {
                        Button(String(localized: String.LocalizationValue("noticeevent_remove"), table: "Localizable")) {
                            onAction(.clickRemoveFormImage)
                        }
                        .font(.caption.weight(.bold))
                        .foregroundStyle(ConCafeColors.primary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                        .clipShape(Capsule())
                        .padding(12)
                    }
                }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 200)
            .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .onTapGesture {
                isImagePickerPresented = true
            }
            Text(uiState.formImageDescription)
                .font(.caption)
                .foregroundStyle(ConCafeColors.textMuted)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func formatEventPeriod(start: Date, end: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyy.MM.dd"
        return "\(formatter.string(from: start)) - \(formatter.string(from: end))"
    }

    private func parseEventPeriod(_ value: String) -> (start: Date, end: Date)? {
        let pattern = #"(\d{4})\.(\d{2})\.(\d{2})\s*(?:~|-)\s*(\d{4})\.(\d{2})\.(\d{2})"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return nil }
        let range = NSRange(value.startIndex..<value.endIndex, in: value)
        guard let match = regex.firstMatch(in: value, options: [], range: range), match.numberOfRanges == 7 else {
            return nil
        }
        let parts = (1...6).compactMap { index -> Int? in
            guard let range = Range(match.range(at: index), in: value) else { return nil }
            return Int(value[range])
        }
        guard parts.count == 6 else { return nil }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        let start = DateComponents(year: parts[0], month: parts[1], day: parts[2])
        let end = DateComponents(year: parts[3], month: parts[4], day: parts[5])
        guard let startDate = calendar.date(from: start), let endDate = calendar.date(from: end) else {
            return nil
        }
        return (start: startDate, end: endDate)
    }
}

private struct NoticeEventFormImageView: View {
    let imageUrl: String

    var body: some View {
        if let fileUrl = URL(string: imageUrl),
           fileUrl.isFileURL,
           let uiImage = UIImage(contentsOfFile: fileUrl.path) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFill()
        } else if let remoteUrl = URL(string: imageUrl) {
            AsyncImage(url: remoteUrl) { phase in
                switch phase {
                case .empty:
                    ProgressView()
                        .tint(ConCafeColors.textMuted)
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure:
                    placeholder
                @unknown default:
                    placeholder
                }
            }
        } else {
            placeholder
        }
    }

    private var placeholder: some View {
        LinearGradient(
            colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

private extension Array {
    func chunked(into size: Int) -> [[Element]] {
        guard size > 0 else { return [] }
        return stride(from: 0, to: count, by: size).map {
            Array(self[$0..<Swift.min($0 + size, count)])
        }
    }
}

private func saveImageToTemporaryFile(_ image: UIImage) -> String? {
    saveCompressedImageToTemporaryFile(image)
}

private func saveImageToTemporaryFileAsync(
    _ image: UIImage,
    completion: @escaping (String?) -> Void
) {
    saveCompressedImageToTemporaryFileAsync(image, completion: completion)
}

struct NoticeEventView_Previews: PreviewProvider {
    static var previews: some View {
        NoticeEventView(cafeId: "cafe-1")
    }
}
