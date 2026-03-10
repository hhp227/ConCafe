//
//  CafeDashboardView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import SwiftUI
import Foundation
import Shared

struct CafeDashboardView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CafeDashboardViewModel

    var body: some View {
        CafeDashboardContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationBarBackButtonHidden()
        .navigationTitle(viewModel.uiState.cafe?.name ?? "카페 관리")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    viewModel.onAction(.clickBack)
                } label: {
                    Image(systemName: "chevron.left")
                }
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToCafeInfoEdit(let cafeId):
                onNavigationAction(.navigateToCafeInfoEdit(id: cafeId))
            }
        }
    }
    
    init(
        cafeId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CafeDashboardViewModel(cafeId: cafeId))
    }
}

private struct CafeDashboardContentView: View {
    let uiState: CafeDashboardUiState

    let onAction: (CafeDashboardAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                if uiState.cafe != nil {
                    heroCard
                }
                if let infoMessage = uiState.infoMessage {
                    infoBanner(message: infoMessage)
                }
                if uiState.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 48)
                } else if uiState.cafe != nil {
                    metricGrid
                    shortcutGrid
                    castManagementSection
                    homeBannerSection
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 20)
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "FFF7FB"), Color(hex: "FFEEF6"), Color(hex: "FFFBFD")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private var heroCard: some View {
        let cafe = uiState.cafe!
        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.18))
                        .frame(width: 44, height: 44)
                    Image(systemName: "storefront")
                        .foregroundStyle(.white)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(cafe.name)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.white)
                    Text(cafe.city)
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.85))
                }
            }
            Text("선택한 카페의 운영 수치와 관리 진입점을 한 화면에서 확인합니다.")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(22)
        .background(
            LinearGradient(
                colors: [Color(hex: "2F1B3A"), Color(hex: "7C3F67"), Color(hex: "F06A9D")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.dismissInfoMessage)
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "6B5320"))
            }
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

    private var metricGrid: some View {
        let cafe = uiState.cafe!
        return VStack(alignment: .leading, spacing: 12) {
            sectionHeader(title: "운영 대시보드", subtitle: "오늘 기준 핵심 수치")
            HStack(spacing: 12) {
                dashboardMetricCard(title: "오늘 체크인", value: "\(cafe.todayCheckIns)", accent: Color(hex: "EF6797"))
                dashboardMetricCard(title: "오늘 리뷰", value: "\(cafe.todayReviews)", accent: Color(hex: "47A88B"))
                dashboardMetricCard(title: "평점", value: formatRating(cafe.rating), accent: Color(hex: "F59E0B"))
            }
        }
    }

    private func dashboardMetricCard(title: String, value: String, accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Circle()
                .fill(accent)
                .frame(width: 10, height: 10)
            Text(title)
                .font(.caption)
                .foregroundStyle(Color(hex: "7A707A"))
            Text(value)
                .font(.title3.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }

    private var shortcutGrid: some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionHeader(title: "관리 메뉴", subtitle: "선택한 카페 컨텍스트로 이동")
            LazyVGrid(
                columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)],
                spacing: 12
            ) {
                ForEach([
                    CafeDashboardShortcut.eventManagement,
                    .cafeSettings,
                    .menuGoods,
                    .externalLinks
                ]) { shortcut in
                    shortcutCard(shortcut: shortcut)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func shortcutCard(shortcut: CafeDashboardShortcut) -> some View {
        let iconName: String = {
            switch shortcut {
            case .castManagement: return "person.3.fill"
            case .castSchedule: return "calendar"
            case .eventManagement: return "sparkles"
            case .cafeSettings: return "gearshape.fill"
            case .menuGoods: return "fork.knife"
            case .homeBanner: return "megaphone.fill"
            case .externalLinks: return "link"
            }
        }()
        return Button {
            onAction(.clickShortcut(shortcut))
        } label: {
            VStack(alignment: .leading, spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color(hex: "FCE6EF"))
                        .frame(width: 40, height: 40)
                    Image(systemName: iconName)
                        .foregroundStyle(Color(hex: "EF6797"))
                }
                Text(shortcut.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color(hex: "2B2330"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 110, alignment: .topLeading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var castManagementSection: some View {
        let cafe = uiState.cafe!
        return VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("소속 캐스트 관리")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "8C7A83"))
                Spacer()
                Button {
                    onAction(.clickShortcut(.castSchedule))
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "calendar")
                            .font(.caption)
                        Text("출근표 관리")
                            .font(.caption.weight(.bold))
                    }
                    .foregroundStyle(Color(hex: "EF6797"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color(hex: "FCE6EF"))
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(cafe.castPreviews, id: \.id) { cast in
                        castPreviewItem(cast: cast)
                    }
                    Button {
                        onAction(.clickShortcut(.castManagement))
                    } label: {
                        VStack(spacing: 8) {
                            ZStack {
                                Circle()
                                    .strokeBorder(Color(hex: "E3DCE3"), style: StrokeStyle(lineWidth: 2, dash: [5]))
                                    .frame(width: 72, height: 72)
                                Image(systemName: "plus")
                                    .foregroundStyle(Color(hex: "B8AEB7"))
                            }
                            Text("추가")
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(Color(hex: "8F848F"))
                        }
                        .frame(width: 80)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.vertical, 2)
            }
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }

    private func castPreviewItem(cast: CafeDashboardData.CastPreview) -> some View {
        VStack(spacing: 8) {
            ZStack(alignment: .bottomTrailing) {
                LinearGradient(
                    colors: [Color(hex: "FFD7E3"), Color(hex: "FFF0F5")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(width: 72, height: 72)
                .clipShape(Circle())
                Circle()
                    .fill(cast.isOnShift ? Color(hex: "35C26B") : Color(hex: "C7CBD3"))
                    .frame(width: 16, height: 16)
            }
            Text(cast.name)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
        }
        .frame(width: 80)
    }

    private var homeBannerSection: some View {
        let cafe = uiState.cafe!
        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("홈 배너 관리")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "8C7A83"))
                Spacer()
                Button("전체 보기") {
                    onAction(.clickShortcut(.homeBanner))
                }
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color(hex: "EF6797"))
            }
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 14) {
                    ZStack {
                        LinearGradient(
                            colors: [Color(hex: "FFD1DC"), Color(hex: "FFE4EC")],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        Image(systemName: "photo")
                            .foregroundStyle(.white)
                    }
                    .frame(width: 96, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    VStack(alignment: .leading, spacing: 6) {
                        Text(cafe.homeBannerPreview.title)
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Color(hex: "2B2330"))
                        Text(cafe.homeBannerPreview.period)
                            .font(.caption)
                            .foregroundStyle(Color(hex: "7E7480"))
                        Text(cafe.homeBannerPreview.statusLabel)
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(Color(hex: "2F8B57"))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(hex: "E8F7EE"))
                            .clipShape(Capsule())
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                Button {
                    onAction(.clickShortcut(.homeBanner))
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "photo.on.rectangle.angled")
                        Text("새 배너 등록하기")
                            .fontWeight(.bold)
                    }
                    .foregroundStyle(Color(hex: "2B2330"))
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color(hex: "FFD1DC"))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(16)
            .background(Color(hex: "FFFBFD"))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color(hex: "F0E6EC"), lineWidth: 1)
            )
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }

    private func sectionHeader(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.title3.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text(subtitle)
                .font(.caption)
                .foregroundStyle(Color(hex: "786E7A"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func formatRating(_ rating: Double) -> String {
        if rating <= 0 {
            return "-"
        }
        return String(format: "%.1f", floor(rating * 10) / 10.0)
    }
}
