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

    var body: some View {
        if maids.isEmpty {
            emptyCard(String(localized: String.LocalizationValue("cafe_cast_empty"), table: "Localizable"))
        } else {
            VStack(spacing: 12) {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(Array(maids.enumerated()), id: \.element.cast.id) { index, maid in
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
                        .onAppear {
                            guard index == maids.indices.last,
                                  canLoadMore,
                                  !isLoadingMore else { return }
                            onAction(.loadMoreCasts)
                        }
                    }
                }
                if canLoadMore {
                    Group {
                        if isLoadingMore {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.top, 12)
                }
            }
        }
    }

    private func emptyCard(_ text: String) -> some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .background(Color.white)
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
}

struct CafeCastView_Previews: PreviewProvider {
    static var previews: some View {
        CafeCastView(maids: [], canLoadMore: false, isLoadingMore: false, onAction: { _ in })
    }
}
