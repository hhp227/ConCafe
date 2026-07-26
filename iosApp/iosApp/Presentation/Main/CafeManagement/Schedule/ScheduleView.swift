//
//  ScheduleView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit
import Shared

struct ScheduleView: View {
    let castId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: ScheduleViewModel

    @State private var alertMessage: String?

    var body: some View {
        ScheduleContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(String(localized: String.LocalizationValue("schedule_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    viewModel.onAction(.clickMore)
                } label: {
                    Image(systemName: "ellipsis")
                }
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { viewModel.uiState.isEditSheetVisible },
            set: { isPresented in
                if !isPresented {
                    viewModel.onAction(.dismissEditSheet)
                }
            }
        )) {
            ScheduleEditModal(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
            .background(TransparentPresentationBackground())
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showMessage(let message):
                alertMessage = message
            case .navigateToCastManagement(let cafeId, let cafeName):
                onNavigationAction(.navigateToCastManagement(cafeId: cafeId, cafeName: cafeName))
            }
        }
        .alert(
            String(localized: String.LocalizationValue("schedule_alert_title_info"), table: "Localizable"),
            isPresented: Binding(
                get: { alertMessage != nil },
                set: { isPresented in
                    if !isPresented {
                        alertMessage = nil
                    }
                }
            ),
            presenting: alertMessage
        ) { _ in
            Button(String(localized: String.LocalizationValue("schedule_action_confirm"), table: "Localizable"), role: .cancel) {
                alertMessage = nil
            }
        } message: { message in
            Text(
                {
                    switch message {
                    case "schedule_info_saved_work",
                         "schedule_info_saved_off",
                         "schedule_info_saved_vacation",
                         "schedule_info_load_failed",
                         "schedule_info_more_next_step",
                         "schedule_info_calendar_next_step",
                         "schedule_error_end_after_start",
                         "schedule_info_edit_applied",
                         "schedule_info_no_changes",
                         "schedule_error_start_required",
                         "schedule_error_end_required",
                         "schedule_error_save_failed",
                         "schedule_error_week_save_failed",
                         "schedule_event_week_saved":
                        return String(localized: String.LocalizationValue(message), table: "Localizable")
                    default:
                        return message
                    }
                }()
            )
        }
        .safeAreaInset(edge: .bottom) {
            Button {
                viewModel.onAction(.clickSave)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "square.and.arrow.down")
                    Text(viewModel.uiState.isSaving ? String(localized: String.LocalizationValue("schedule_save_in_progress"), table: "Localizable") : String(localized: String.LocalizationValue("schedule_save"), table: "Localizable"))
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .foregroundStyle(.primary)
                .background(ConCafeColors.primaryContainer)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(!viewModel.uiState.hasPendingChanges || viewModel.uiState.isSaving)
            .opacity((!viewModel.uiState.hasPendingChanges || viewModel.uiState.isSaving) ? 0.55 : 1)
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 14)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        }
    }

    init(
        castId: String? = nil,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.castId = castId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: ScheduleViewModel(castId: castId))
    }
}

private struct ScheduleEditModal: View {
    let uiState: ScheduleUiState

    let onAction: (ScheduleAction) -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.35)
                .ignoresSafeArea()
                .onTapGesture {
                    onAction(.dismissEditSheet)
                }
            VStack(spacing: 0) {
                Capsule()
                    .fill(ConCafeColors.outline)
                    .frame(width: 48, height: 5)
                    .padding(.top, 12)
                    .padding(.bottom, 8)
                VStack(spacing: 16) {
                    VStack(spacing: 4) {
                        Text(String(localized: String.LocalizationValue("schedule_edit_title"), table: "Localizable"))
                            .font(.title3.weight(.bold))
                        Text(uiState.editingScheduleTitle)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    HStack(spacing: 8) {
                        ForEach(["work", "off", "vacation"], id: \.self) { statusId in
                            let status: CastScheduleStatus = {
                                switch statusId {
                                case "off": return .off
                                case "vacation": return .vacation
                                default: return .work
                                }
                            }()
                            Button {
                                onAction(.changeEditStatus(status))
                            } label: {
                                Text(status.label)
                                    .font(.subheadline.weight(.medium))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(uiState.editStatus == status ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.clear)
                                    .foregroundStyle(uiState.editStatus == status ? .primary : .secondary)
                                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(6)
                    .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    HStack(spacing: 12) {
                        TimePickerField(
                            title: String(localized: String.LocalizationValue("schedule_label_start_time"), table: "Localizable"),
                            value: uiState.editStartTime,
                            isEnabled: uiState.isEditingWorking,
                            options: uiState.timeOptions,
                            onSelect: { onAction(.changeEditStartTime($0)) }
                        )
                        TimePickerField(
                            title: String(localized: String.LocalizationValue("schedule_label_end_time"), table: "Localizable"),
                            value: uiState.editEndTime,
                            isEnabled: uiState.isEditingWorking,
                            options: uiState.timeOptions,
                            onSelect: { onAction(.changeEditEndTime($0)) }
                        )
                    }
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "info.circle.fill")
                            .foregroundStyle(ConCafeColors.primary)
                            .font(.caption)
                        Text(String(localized: String.LocalizationValue("schedule_break_notice"), table: "Localizable"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(12)
                    .background(ConCafeColors.primaryContainer.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(ConCafeColors.primaryContainer.opacity(0.2), lineWidth: 1)
                    )
                    HStack {
                        Text(String(localized: String.LocalizationValue("schedule_total_work"), table: "Localizable"))
                            .foregroundStyle(.secondary)
                        Spacer()
                        HStack(alignment: .bottom, spacing: 4) {
                            Text(String(localized: String.LocalizationValue("schedule_total_prefix"), table: "Localizable"))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(resolveScheduleDurationLabel(uiState.totalWorkDurationLabel))
                                .font(.title2.weight(.bold))
                        }
                    }
                    Button {
                        onAction(.submitEditDay)
                    } label: {
                        Text(String(localized: String.LocalizationValue("schedule_apply_edit"), table: "Localizable"))
                            .font(.headline.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(ConCafeColors.primaryContainer)
                            .foregroundStyle(.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 20)
                .compatSafeAreaBottomPadding()
            }
            .frame(maxWidth: .infinity)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(TopRoundedRectangle(radius: 24))
            .ignoresSafeArea(edges: .bottom)
        }
        .background(TransparentPresentationBackground())
    }
}

private struct TopRoundedRectangle: Shape {
    let radius: CGFloat

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [.topLeft, .topRight],
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

private struct TransparentPresentationBackground: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.backgroundColor = .clear
        DispatchQueue.main.async {
            clearPresentationBackground(from: view)
        }
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            clearPresentationBackground(from: uiView)
        }
    }

    private func clearPresentationBackground(from view: UIView) {
        var currentView: UIView? = view
        while let parent = currentView?.superview, !(parent is UIWindow) {
            parent.backgroundColor = .clear
            currentView = parent
        }
    }
}

private struct TimePickerField: View {
    let title: String

    let value: String

    let isEnabled: Bool

    let options: [String]

    let onSelect: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(.secondary)
            Menu {
                ForEach(options, id: \.self) { option in
                    Button(option) {
                        onSelect(option)
                    }
                }
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "clock")
                        .foregroundStyle(ConCafeColors.primary)
                    Text(value)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(isEnabled ? .primary : .secondary)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 14)
                .frame(height: 52)
                .background(isEnabled ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }) : Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .disabled(!isEnabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct ScheduleContentView: View {
    let uiState: ScheduleUiState
    
    let onAction: (ScheduleAction) -> Void
    
    @State private var scheduleScrollTargetId: String?
    
    @State private var scheduleScrollRequestToken = 0
    
    @State private var isScheduleScrollRequestPending = false
    
    var body: some View {
        ZStack {
            Group {
                if UITraitCollection.current.userInterfaceStyle == .dark {
                    ConCafeColors.background
                } else {
                    LinearGradient(
                        colors: [Color(uiColor: .systemGroupedBackground), Color(uiColor: .secondarySystemGroupedBackground), Color(uiColor: .systemGroupedBackground)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
            }
            .ignoresSafeArea()
            if !uiState.isLoading {
                ScrollViewReader { scrollProxy in
                    ScrollView(.vertical, showsIndicators: true) {
                        VStack(spacing: 12) {
                            castSummaryCard
                            weekSelectorSection
                            if let errorMessage = uiState.errorMessage {
                                infoBanner(message: errorMessage)
                            }
                            if let infoMessage = uiState.infoMessage {
                                infoBanner(message: infoMessage)
                            }
                            scheduleListSection
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 16)
                        .padding(.bottom, 24)
                        .frame(maxWidth: .infinity, alignment: .top)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                    .onChange(of: scheduleScrollRequestToken) { _ in
                        guard isScheduleScrollRequestPending else { return }
                        isScheduleScrollRequestPending = false
                        guard let dayId = scheduleScrollTargetId,
                              let targetId = resolveScheduleScrollTargetId(for: dayId) else { return }
                        DispatchQueue.main.async {
                            withAnimation(.easeInOut(duration: 0.28)) {
                                scrollProxy.scrollTo(targetId, anchor: .top)
                            }
                        }
                    }
                }
            } else {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(ConCafeColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            }
        }
    }
    
    private var castSummaryCard: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text(String(localized: String.LocalizationValue(uiState.castSummary.badge), table: "Localizable"))
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(ConCafeColors.primary)
                Text(uiState.castSummary.title)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.primary)
                Text(resolveScheduleCastSubtitle(uiState.castSummary.subtitle))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [ConCafeColors.primaryContainer, ConCafeColors.secondaryContainer],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 80, height: 80)
                .overlay {
                    let trimmedImageUrl = uiState.castSummary.profileImageUrl?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                    if let imageUrl = ImageUrlUtils.normalizedRemoteUrl(from: trimmedImageUrl) {
                        CachedAsyncImage(url: imageUrl, displaySize: .thumbnail)
                            .frame(width: 80, height: 80)
                            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    } else {
                        Text(uiState.castSummary.initials)
                            .font(.title3.weight(.bold))
                            .foregroundStyle(ConCafeColors.primary)
                    }
                }
        }
        .padding(16)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }).opacity(0.94))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(ConCafeColors.primaryContainer.opacity(0.10), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 10, x: 0, y: 4)
    }
    
    private var weekSelectorSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(uiState.weekRangeLabel)
                    .font(.headline)
                    .foregroundStyle(.primary)
                Spacer()
                Button {
                    onAction(.clickCalendar)
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "calendar")
                        Text(String(localized: String.LocalizationValue("schedule_calendar"), table: "Localizable"))
                            .fontWeight(.bold)
                    }
                    .font(.caption)
                    .foregroundStyle(ConCafeColors.primary)
                }
                .buttonStyle(.plain)
            }
            schedulePeriodTabs
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(uiState.weekDays, id: \.id) { day in
                        let isSelected = day.id == uiState.selectedDayId
                        Button {
                            onAction(.selectDay(id: day.id))
                            requestScheduleScroll(to: day.id)
                        } label: {
                            VStack(spacing: 4) {
                                Text(day.label)
                                    .font(.caption2.weight(.bold))
                                    .foregroundStyle(isSelected ? Color.white.opacity(0.82) : Color.secondary)
                                Text(day.number)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(isSelected ? Color.white : Color.primary)
                            }
                            .frame(width: 56)
                            .padding(.vertical, 10)
                            .background(isSelected ? ConCafeColors.primary : Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }).opacity(0.92))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(isSelected ? .clear : ConCafeColors.primaryContainer.opacity(0.10), lineWidth: 1)
                            )
                            .shadow(color: isSelected ? ConCafeColors.primary.opacity(0.35) : .clear, radius: 4, x: 0, y: 2)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
    
    private func requestScheduleScroll(to dayId: String) {
        scheduleScrollTargetId = dayId
        isScheduleScrollRequestPending = true
        scheduleScrollRequestToken += 1
    }
    
    private func resolveScheduleScrollTargetId(for dayId: String) -> String? {
        if uiState.schedules.contains(where: { $0.id == dayId }) {
            return dayId
        }
        let normalizedDayId = normalizedScheduleDateId(dayId)
        return uiState.schedules.first { schedule in
            normalizedScheduleDateId(schedule.id) == normalizedDayId
        }?.id
    }
    
    private func normalizedScheduleDateId(_ value: String) -> String {
        String(value.prefix(10))
    }
    
    private var schedulePeriodTabs: some View {
        HStack(spacing: 4) {
            ForEach(SchedulePeriod.allCases, id: \.self) { period in
                let isSelected = uiState.schedulePeriod == period
                
                Button {
                    onAction(.selectPeriod(period))
                } label: {
                    Text(String(localized: String.LocalizationValue(period.localizationKey), table: "Localizable"))
                        .font(.caption.weight(isSelected ? .bold : .medium))
                        .foregroundStyle(isSelected ? ConCafeColors.primary : .secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(isSelected ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .secondarySystemGroupedBackground }).opacity(0.72))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
    
    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(
                {
                    switch message {
                    case "schedule_info_saved_work",
                        "schedule_info_saved_off",
                        "schedule_info_saved_vacation",
                        "schedule_info_load_failed",
                        "schedule_info_more_next_step",
                        "schedule_info_calendar_next_step",
                        "schedule_error_end_after_start",
                        "schedule_info_edit_applied",
                        "schedule_info_no_changes",
                        "schedule_error_start_required",
                        "schedule_error_end_required",
                        "schedule_error_save_failed",
                        "schedule_error_week_save_failed",
                        "schedule_event_week_saved":
                        return String(localized: String.LocalizationValue(message), table: "Localizable")
                    default:
                        return message
                    }
                }()
            )
            .font(.caption)
            .foregroundStyle(ConCafeColors.goldDeep)
            .frame(maxWidth: .infinity, alignment: .leading)
            Button(String(localized: String.LocalizationValue("schedule_action_close"), table: "Localizable")) {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(ConCafeColors.goldDeep)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(ConCafeColors.goldContainer)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(ConCafeColors.gold, lineWidth: 1)
        )
    }
    
    private var scheduleListSection: some View {
        VStack(spacing: 12) {
            ForEach(uiState.schedules, id: \.id) { schedule in
                HStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(schedule.isWorking ? Color.white.opacity(0.18) : ConCafeColors.surfaceVariant)
                        .frame(width: 48, height: 48)
                        .overlay {
                            Image(systemName: schedule.isWorking ? "clock" : "bed.double")
                                .foregroundStyle(schedule.isWorking ? Color.white : .secondary)
                        }
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 8) {
                            Text(schedule.title)
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(schedule.isWorking ? Color.white : .primary)
                            Text(resolveScheduleStatusLabel(schedule.statusLabel, status: schedule.status))
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(schedule.isWorking ? Color.white.opacity(0.86) : .secondary)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(schedule.isWorking ? Color.white.opacity(0.22) : ConCafeColors.surfaceVariant)
                                .clipShape(Capsule())
                        }
                        Text(resolveScheduleTimeLabel(schedule.timeLabel, status: schedule.status))
                            .font(.subheadline)
                            .foregroundStyle(schedule.isWorking ? Color.white.opacity(0.82) : .secondary)
                    }
                    Spacer()
                    Button {
                        onAction(.clickEditDay(id: schedule.id))
                    } label: {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(schedule.isWorking ? Color.white.opacity(0.18) : Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }))
                            .frame(width: 40, height: 40)
                            .overlay {
                                Image(systemName: "pencil")
                                    .foregroundStyle(schedule.isWorking ? Color.white : .secondary)
                            }
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 14)
                .id(schedule.id)
                .background(schedule.isWorking ? ConCafeColors.primary : Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }).opacity(0.88))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(schedule.isWorking ? Color.white.opacity(0.38) : ConCafeColors.outline)
                        .frame(width: 4)
                }
                .shadow(color: Color.black.opacity(0.03), radius: 6, x: 0, y: 2)
            }
        }
    }
}

struct ScheduleView_Previews: PreviewProvider {
    static var previews: some View {
        ScheduleView(onNavigationAction: { _ in })
    }
}

private func resolveScheduleDurationLabel(_ value: String) -> String {
    if value.hasPrefix("schedule_duration_hours_only:") {
        let hours = Int(value.split(separator: ":").last ?? "0") ?? 0
        return String(format: String(localized: String.LocalizationValue("schedule_duration_hours_only"), table: "Localizable"), hours)
    }
    if value.hasPrefix("schedule_duration_hours_minutes:") {
        let components = value.split(separator: ":")
        let hours = Int(components.indices.contains(1) ? components[1] : "0") ?? 0
        let minutes = Int(components.indices.contains(2) ? components[2] : "0") ?? 0
        return String(format: String(localized: String.LocalizationValue("schedule_duration_hours_minutes"), table: "Localizable"), hours, minutes)
    }
    return value
}

private func resolveScheduleStatusLabel(_ statusLabel: String, status: CastScheduleStatus) -> String {
    switch statusLabel.lowercased() {
    case "schedule_status_work", "근무", "work":
        return String(localized: String.LocalizationValue("schedule_status_work"), table: "Localizable")
    case "schedule_status_off", "휴무", "off":
        return String(localized: String.LocalizationValue("schedule_status_off"), table: "Localizable")
    case "schedule_status_vacation", "휴가", "vacation":
        return String(localized: String.LocalizationValue("schedule_status_vacation"), table: "Localizable")
    default:
        switch status {
        case .work:
            return String(localized: String.LocalizationValue("schedule_status_work"), table: "Localizable")
        case .off:
            return String(localized: String.LocalizationValue("schedule_status_off"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("schedule_status_vacation"), table: "Localizable")
        }
    }
}

private func resolveScheduleTimeLabel(_ timeLabel: String, status: CastScheduleStatus) -> String {
    switch timeLabel.lowercased() {
    case "schedule_status_off", "휴무", "off":
        return String(localized: String.LocalizationValue("schedule_status_off"), table: "Localizable")
    case "schedule_status_vacation", "휴가", "vacation":
        return String(localized: String.LocalizationValue("schedule_status_vacation"), table: "Localizable")
    default:
        switch status {
        case .off:
            return String(localized: String.LocalizationValue("schedule_status_off"), table: "Localizable")
        case .vacation:
            return String(localized: String.LocalizationValue("schedule_status_vacation"), table: "Localizable")
        default:
            return timeLabel
        }
    }
}

private func resolveScheduleCastSubtitle(_ subtitle: String) -> String {
    let separator = " / "
    guard subtitle.contains(separator) else { return subtitle }
    let components = subtitle.components(separatedBy: separator)
    guard let conceptRaw = components.first, let cafeName = components.last else { return subtitle }
    let concept: String
    switch conceptRaw {
    case "schedule_concept_maid":
        concept = String(localized: String.LocalizationValue("schedule_concept_maid"), table: "Localizable")
    case "schedule_concept_butler":
        concept = String(localized: String.LocalizationValue("schedule_concept_butler"), table: "Localizable")
    case "schedule_concept_idol":
        concept = String(localized: String.LocalizationValue("schedule_concept_idol"), table: "Localizable")
    default:
        concept = conceptRaw
    }
    return "\(concept)\(separator)\(cafeName)"
}

private extension CastScheduleStatus {
    var label: String {
        switch self {
        case .work:
            return String(localized: String.LocalizationValue("schedule_status_work"), table: "Localizable")
        case .off:
            return String(localized: String.LocalizationValue("schedule_status_off"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("schedule_status_vacation"), table: "Localizable")
        }
    }
}
