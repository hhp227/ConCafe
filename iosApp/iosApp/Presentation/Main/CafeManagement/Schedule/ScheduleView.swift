//
//  ScheduleView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
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
        .navigationTitle("주간 출근표 관리")
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
        .sheet(isPresented: Binding(
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
            .background(Color.clear)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showMessage(let message):
                alertMessage = message
            }
        }
        .alert(
            "안내",
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
            Button("확인", role: .cancel) {
                alertMessage = nil
            }
        } message: { message in
            Text(message)
        }
        .safeAreaInset(edge: .bottom) {
            Button {
                viewModel.onAction(.clickSave)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "square.and.arrow.down")
                    Text(viewModel.uiState.isSaving ? "저장 중..." : "주간 시간표 저장하기")
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .foregroundStyle(Color(hex: "24161E"))
                .background(Color(hex: "FFD1DC"))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(!viewModel.uiState.hasPendingChanges || viewModel.uiState.isSaving)
            .opacity((!viewModel.uiState.hasPendingChanges || viewModel.uiState.isSaving) ? 0.55 : 1)
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 14)
            .background(Color.white)
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
                    .fill(Color(hex: "E5DDE2"))
                    .frame(width: 48, height: 5)
                    .padding(.top, 12)
                    .padding(.bottom, 8)
                VStack(spacing: 16) {
                    VStack(spacing: 4) {
                        Text("근무 시간 수정")
                            .font(.title3.weight(.bold))
                        Text(uiState.editingScheduleTitle)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "7A707A"))
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
                                    .background(uiState.editStatus == status ? Color.white : Color.clear)
                                    .foregroundStyle(uiState.editStatus == status ? Color(hex: "24161E") : Color(hex: "7A707A"))
                                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(6)
                    .background(Color(hex: "F8F5F6"))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    HStack(spacing: 12) {
                        TimePickerField(
                            title: "시작 시간",
                            value: uiState.editStartTime,
                            isEnabled: uiState.isEditingWorking,
                            options: uiState.timeOptions,
                            onSelect: { onAction(.changeEditStartTime($0)) }
                        )
                        TimePickerField(
                            title: "종료 시간",
                            value: uiState.editEndTime,
                            isEnabled: uiState.isEditingWorking,
                            options: uiState.timeOptions,
                            onSelect: { onAction(.changeEditEndTime($0)) }
                        )
                    }
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "info.circle.fill")
                            .foregroundStyle(Color(hex: "EF6797"))
                            .font(.caption)
                        Text("휴게 시간 1시간(12:00 - 13:00)이 자동으로 포함되어 총 근무 시간에서 제외됩니다.")
                            .font(.caption)
                            .foregroundStyle(Color(hex: "6B5A63"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(12)
                    .background(Color(hex: "FFD1DC").opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(Color(hex: "FFD1DC").opacity(0.2), lineWidth: 1)
                    )
                    HStack {
                        Text("실제 근무 합계")
                            .foregroundStyle(Color(hex: "7A707A"))
                        Spacer()
                        HStack(alignment: .bottom, spacing: 4) {
                            Text("총")
                                .font(.caption)
                                .foregroundStyle(Color(hex: "7A707A"))
                            Text(uiState.totalWorkDurationLabel)
                                .font(.title2.weight(.bold))
                        }
                    }
                    Button {
                        onAction(.submitEditDay)
                    } label: {
                        Text("편집 내용 반영하기")
                            .font(.headline.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color(hex: "FFD1DC"))
                            .foregroundStyle(Color(hex: "24161E"))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 20)
            }
            .frame(maxWidth: .infinity)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .ignoresSafeArea(edges: .bottom)
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
                .foregroundStyle(Color(hex: "7A707A"))
            Menu {
                ForEach(options, id: \.self) { option in
                    Button(option) {
                        onSelect(option)
                    }
                }
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "clock")
                        .foregroundStyle(Color(hex: "EF6797"))
                    Text(value)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(isEnabled ? Color(hex: "24161E") : Color(hex: "B0A3AC"))
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color(hex: "8F848F"))
                }
                .padding(.horizontal, 14)
                .frame(height: 52)
                .background(isEnabled ? Color(hex: "F8F5F6") : Color(hex: "F2EDF0"))
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

    var body: some View {
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
        .background(
            LinearGradient(
                colors: [Color(hex: "F8F5F6"), Color(hex: "FFF8FB"), Color(hex: "FFEFF5")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private var castSummaryCard: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text(uiState.castSummary.badge)
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(Color(hex: "EF6797"))
                Text(uiState.castSummary.title)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(Color(hex: "24161E"))
                Text(uiState.castSummary.subtitle)
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "7A707A"))
            }
            Spacer()
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "FFD7E5"), Color(hex: "F2ADC2")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 80, height: 80)
                .overlay {
                    Text(uiState.castSummary.initials)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(Color(hex: "7C3F67"))
                }
        }
        .padding(16)
        .background(Color.white.opacity(0.94))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: "FFD1DC").opacity(0.10), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 10, x: 0, y: 4)
    }

    private var weekSelectorSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(uiState.weekRangeLabel)
                    .font(.headline)
                    .foregroundStyle(Color(hex: "24161E"))
                Spacer()
                Button {
                    onAction(.clickCalendar)
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "calendar")
                        Text("달력보기")
                            .fontWeight(.bold)
                    }
                    .font(.caption)
                    .foregroundStyle(Color(hex: "EF6797"))
                }
                .buttonStyle(.plain)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(uiState.weekDays, id: \.id) { day in
                        let isSelected = day.id == uiState.selectedDayId
                        Button {
                            onAction(.selectDay(id: day.id))
                        } label: {
                            VStack(spacing: 4) {
                                Text(day.label)
                                    .font(.caption2.weight(.bold))
                                    .foregroundStyle(isSelected ? Color(hex: "24161E").opacity(0.6) : Color(hex: "9C8C98"))
                                Text(day.number)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(Color(hex: "24161E"))
                            }
                            .frame(width: 56)
                            .padding(.vertical, 10)
                            .background(isSelected ? Color(hex: "FFD1DC") : Color.white.opacity(0.92))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(isSelected ? .clear : Color(hex: "FFD1DC").opacity(0.10), lineWidth: 1)
                            )
                            .shadow(color: isSelected ? Color(hex: "FFD1DC").opacity(0.5) : .clear, radius: 4, x: 0, y: 2)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("닫기") {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "6B5320"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(hex: "FFF6D7"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: "F1D88D"), lineWidth: 1)
        )
    }

    private var scheduleListSection: some View {
        VStack(spacing: 12) {
            ForEach(uiState.schedules, id: \.id) { schedule in
                HStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(schedule.isWorking ? Color(hex: "FFD1DC").opacity(0.14) : Color(hex: "F2EDF0"))
                        .frame(width: 48, height: 48)
                        .overlay {
                            Image(systemName: schedule.isWorking ? "clock" : "bed.double")
                                .foregroundStyle(schedule.isWorking ? Color(hex: "EF6797") : Color(hex: "B0A3AC"))
                        }
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 8) {
                            Text(schedule.title)
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(Color(hex: "24161E"))
                            Text(schedule.statusLabel)
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(schedule.isWorking ? Color(hex: "5B4A57") : Color(hex: "9C8C98"))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(schedule.isWorking ? Color(hex: "FFD1DC").opacity(0.30) : Color(hex: "F2EDF0"))
                                .clipShape(Capsule())
                        }
                        Text(schedule.timeLabel)
                            .font(.subheadline)
                            .foregroundStyle(schedule.isWorking ? Color(hex: "7A707A") : Color(hex: "B0A3AC"))
                    }
                    Spacer()
                    Button {
                        onAction(.clickEditDay(id: schedule.id))
                    } label: {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(Color(hex: "F8F5F6"))
                            .frame(width: 40, height: 40)
                            .overlay {
                                Image(systemName: "pencil")
                                    .foregroundStyle(Color(hex: "7A707A"))
                            }
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 14)
                .background(Color.white.opacity(schedule.isWorking ? 0.96 : 0.88))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(schedule.isWorking ? Color(hex: "FFD1DC") : Color(hex: "E9E0E5"))
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

private extension CastScheduleStatus {
    var label: String {
        switch self {
        case .work:
            return "근무"
        case .off:
            return "휴무"
        default:
            return "휴가"
        }
    }
}
