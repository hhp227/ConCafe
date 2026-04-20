//
//  CafeCastView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared

struct CafeCastView: View {
    let maids: [CafeDetailCast]

    let canLoadMore: Bool

    let isLoadingMore: Bool

    let onAction: (CafeAction) -> Void

    @State private var contentWidth: CGFloat = 0

    var body: some View {
        if maids.isEmpty {
            emptyCard(String(localized: String.LocalizationValue("cafe_cast_empty"), table: "Localizable"))
        } else {
            VStack(spacing: 12) {
                LazyVGrid(columns: cafeCastGridColumns(for: contentWidth), spacing: 12) {
                    ForEach(Array(maids.enumerated()), id: \.element.cast.id) { _, maid in
                        let attendanceStatus = CastScheduleAttendanceUtils.attendanceStatus(schedule: maid.todaySchedule)

                        ConCafeCastCard(
                            name: maid.cast.name,
                            subtitle: maid.cast.desc,
                            imageUrl: maid.cast.profileImage,
                            subtitleLineLimit: 2,
                            attendanceStatusText: cafeCastAttendanceStatusText(attendanceStatus),
                            isWorking: maid.isWorking,
                            onTap: { onAction(.maidTapped(id: maid.cast.id)) }
                        )
                    }
                }
                if canLoadMore || isLoadingMore {
                    VStack(spacing: 0) {
                        Color.clear
                            .frame(height: 1)
                            .onAppear {
                                if canLoadMore, !isLoadingMore {
                                    onAction(.loadMoreCasts)
                                }
                            }
                        if isLoadingMore {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.top, 12)
                }
            }
            .background(
                GeometryReader { proxy in
                    Color.clear
                        .onAppear {
                            contentWidth = proxy.size.width
                        }
                        .onChange(of: proxy.size.width) { nextWidth in
                            contentWidth = nextWidth
                        }
                }
            )
        }
    }

    private func emptyCard(_ text: String) -> some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func cafeCastAttendanceStatusText(_ status: CastAttendanceStatus) -> String? {
        switch status {
        case .upcoming:
            return String(localized: String.LocalizationValue("cast_today_upcoming"), table: "Localizable")
        case .onShift:
            return String(localized: String.LocalizationValue("cast_today_working"), table: "Localizable")
        case .completed:
            return String(localized: String.LocalizationValue("cast_today_finished"), table: "Localizable")
        default:
            return nil
        }
    }

    private func cafeCastGridColumns(for contentWidth: CGFloat) -> [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: cafeCastGridItemSpacing),
            count: cafeCastGridColumnCount(for: contentWidth)
        )
    }

    private func cafeCastGridColumnCount(for contentWidth: CGFloat) -> Int {
        let availableWidth = contentWidth - cafeCastGridHorizontalPadding
        let minimumGridWidth = (cafeCastGridMinimumCellWidth * 2) + cafeCastGridItemSpacing
        let normalizedWidth = max(availableWidth, minimumGridWidth)
        let rawCount = Int((normalizedWidth + cafeCastGridItemSpacing) /
            (cafeCastGridMinimumCellWidth + cafeCastGridItemSpacing))
        return min(max(rawCount, cafeCastGridMinimumColumnCount), cafeCastGridMaximumColumnCount)
    }
}

struct CafeCastView_Previews: PreviewProvider {
    static var previews: some View {
        CafeCastView(maids: [], canLoadMore: false, isLoadingMore: false, onAction: { _ in })
    }
}

private let cafeCastGridMinimumColumnCount = 2
private let cafeCastGridMaximumColumnCount = 4
private let cafeCastGridHorizontalPadding: CGFloat = 24
private let cafeCastGridItemSpacing: CGFloat = 12
private let cafeCastGridMinimumCellWidth: CGFloat = 180
