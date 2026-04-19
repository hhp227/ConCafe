//
//  CastManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI

struct CastManagementView: View {
    let cafeId: String

    let cafeName: String

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CastManagementViewModel

    var body: some View {
        CastManagementContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(viewModel.uiState.cafeName.isEmpty
            ? String(localized: String.LocalizationValue("cast_management_screen_title"), table: "Localizable")
            : viewModel.uiState.cafeName)
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
    }

    init(
        cafeId: String,
        cafeName: String,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.cafeName = cafeName
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CastManagementViewModel(cafeId: cafeId, cafeName: cafeName))
    }
}

private struct CastManagementContentView: View {
    let uiState: CastManagementUiState

    let onAction: (CastManagementAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            ViewModeSelector(
                selected: uiState.viewMode,
                onSelect: { onAction(.changeViewMode($0)) }
            )
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            if let fromDate = uiState.periodStart, let toDate = uiState.periodEnd {
                let periodLabel = formatPeriodLabel(viewMode: uiState.viewMode, from: fromDate, to: toDate)
                let scheduleTitle = String(format: String(localized: String.LocalizationValue("cast_management_period_schedule"), table: "Localizable"), periodLabel, uiState.cafeName)
                Text(scheduleTitle)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
            }
            if uiState.isLoading {
                Spacer()
                ProgressView()
                Spacer()
            } else if let error = uiState.errorMessage {
                Spacer()
                Text(String(localized: String.LocalizationValue(error), table: "Localizable"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                Spacer()
            } else {
                switch uiState.viewMode {
                case .week:
                    ScrollView {
                        WeekScheduleView(columns: uiState.weekColumns)
                            .padding(.top, 4)
                            .padding(.bottom, 16)
                    }
                case .month:
                    MonthScheduleView(offset: uiState.monthOffset, cells: uiState.monthCells)
                }
            }
        }
        .background(Color(hex: "FFF9FC"))
    }

    private func formatPeriodLabel(viewMode: CastScheduleViewMode, from: Date, to: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        switch viewMode {
        case .week:
            formatter.setLocalizedDateFormatFromTemplate("Md")
            return "\(formatter.string(from: from)) - \(formatter.string(from: to))"
        case .month:
            formatter.setLocalizedDateFormatFromTemplate("yyyyMMMM")
            return formatter.string(from: from)
        }
    }
}

private struct ViewModeSelector: View {
    let selected: CastScheduleViewMode

    let onSelect: (CastScheduleViewMode) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(CastScheduleViewMode.allCases, id: \.self) { mode in
                let isSelected = selected == mode

                Button {
                    onSelect(mode)
                } label: {
                    Text(String(localized: String.LocalizationValue(mode.rawValue), table: "Localizable"))
                        .font(.subheadline.weight(isSelected ? .bold : .regular))
                        .foregroundStyle(isSelected ? Color(hex: "EF6797") : .secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(isSelected ? Color(hex: "FFD1DC").opacity(0.3) : Color.clear)
                }
                .buttonStyle(.plain)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(Color(hex: "FFD1DC").opacity(0.5), lineWidth: 1)
        )
    }
}

// MARK: - Week View

private struct WeekScheduleView: View {
    let columns: [CastManagementUiState.WeekColumn]

    var body: some View {
        VStack(spacing: 8) {
            ForEach(columns) { col in
                WeekDayCard(column: col)
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }
}

private struct WeekDayCard: View {
    let column: CastManagementUiState.WeekColumn

    var body: some View {
        let isWorking = !column.castNames.isEmpty

        HStack(alignment: .center, spacing: 14) {
            VStack(spacing: 2) {
                Text(column.dayLabel)
                    .font(.system(size: 15, weight: .heavy))
                    .foregroundStyle(isWorking ? Color(hex: "EF6797") : .secondary)
                Text(column.dateLabel)
                    .font(.system(size: 10))
                    .foregroundStyle(.secondary)
            }
            .frame(width: 52, height: 52)
            .background(isWorking ? Color(hex: "FFF0F4") : Color(uiColor: UIColor.secondarySystemFill))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            if !isWorking {
                Text("-")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
            } else {
                FlowLayout(spacing: 6) {
                    ForEach(column.castNames, id: \.self) { name in
                        CastNameChip(name: name)
                    }
                }
                Spacer(minLength: 0)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 6, x: 0, y: 2)
    }
}

private struct FlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var lineHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += lineHeight + spacing
                lineHeight = 0
            }
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
        return CGSize(width: maxWidth, height: y + lineHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var lineHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)

            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += lineHeight + spacing
                lineHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
    }
}

private struct CastNameChip: View {
    let name: String

    var body: some View {
        Text(name)
            .font(.caption2.weight(.medium))
            .foregroundStyle(Color(hex: "EF6797"))
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(Color(hex: "FFD1DC").opacity(0.3))
            .clipShape(Capsule())
            .lineLimit(1)
    }
}

// MARK: - Month View

private struct MonthScheduleView: View {
    let offset: Int

    let cells: [CastManagementUiState.MonthCell]

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 1), count: 7)

    // Locale-aware Mon–Sun short weekday symbols
    private var dayHeaders: [String] {
        var cal = Calendar.current
        cal.locale = Locale.current
        let symbols = cal.veryShortWeekdaySymbols // Sun=0, Mon=1, ..., Sat=6
        return Array(symbols[1...]) + [symbols[0]]
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    ForEach(dayHeaders, id: \.self) { label in
                        Text(label)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Color(hex: "EF6797"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                    }
                }
                .background(Color(hex: "FFD1DC").opacity(0.12))
                LazyVGrid(columns: columns, spacing: 1) {
                    ForEach(0..<offset, id: \.self) { _ in
                        Color.clear
                            .frame(minHeight: 64)
                    }
                    ForEach(cells) { cell in
                        MonthDayCell(cell: cell)
                    }
                }
                .background(Color(hex: "FFD1DC").opacity(0.08))
            }
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.2), lineWidth: 1)
            )
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
    }
}

private struct MonthDayCell: View {
    let cell: CastManagementUiState.MonthCell

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text("\(cell.dayNumber)")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.primary)
            if cell.castNames.isEmpty {
                Spacer()
            } else {
                ForEach(cell.castNames.prefix(3), id: \.self) { name in
                    Text(name)
                        .font(.system(size: 9))
                        .foregroundStyle(Color(hex: "EF6797"))
                        .lineLimit(1)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                        .background(Color(hex: "FFD1DC").opacity(0.3))
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                }
                if cell.castNames.count > 3 {
                    Text("+\(cell.castNames.count - 3)")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity, minHeight: 64, alignment: .topLeading)
        .padding(5)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
    }
}

struct CastManagementView_Previews: PreviewProvider {
    static var previews: some View {
        CompatNavigationContainer {
            CastManagementView(cafeId: "", cafeName: "ConCafe", onNavigationAction: { _ in })
        }
    }
}
