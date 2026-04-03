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

    private let cafeId: String

    @StateObject private var viewModel: CafeDashboardViewModel

    var body: some View {
        CafeDashboardContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(viewModel.uiState.cafe?.name ?? "카페 관리")
        .navigationBarTitleDisplayMode(.inline)
        .alert("캐스트 프로필 삭제", isPresented: Binding(
            get: { viewModel.uiState.isDeleteCastDialogVisible },
            set: { isPresented in
                if !isPresented {
                    viewModel.onAction(.dismissDeleteCastDialog)
                }
            }
        )) {
            Button("취소", role: .cancel) {
                viewModel.onAction(.dismissDeleteCastDialog)
            }
            Button("확인", role: .destructive) {
                viewModel.onAction(.confirmDeleteCast)
            }
        } message: {
            Text("캐스트 프로필을 삭제하시겠습니까?")
        }
        .fullScreenCover(isPresented: Binding(
            get: { viewModel.uiState.isExternalLinkSheetVisible },
            set: { isPresented in
                if !isPresented {
                    viewModel.onAction(.dismissExternalLinkSheet)
                }
            }
        )) {
            ExternalLinkInputSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToBanner:
                onNavigationAction(.navigateToBanner(cafeId: cafeId))
            case .navigateToBannerEdit:
                onNavigationAction(.navigateToBannerEdit(cafeId: cafeId))
            case .navigateToCafeInfoEdit(let cafeId):
                onNavigationAction(.navigateToCafeInfoEdit(id: cafeId))
            case .navigateToNoticeEvent(let cafeId):
                onNavigationAction(.navigateToNoticeEvent(id: cafeId))
            case .navigateToMenuGoods(let cafeId):
                onNavigationAction(.navigateToMenuGoods(id: cafeId))
            case .navigateToCastEdit(let cafeId, let castId):
                onNavigationAction(.navigateToCastEdit(cafeId: cafeId, castId: castId))
            case .navigateToSchedule(let castId):
                onNavigationAction(.navigateToSchedule(castId: castId))
            case .navigateToExternalLink(let title, let url):
                onNavigationAction(.navigateToExternalLink(title: title, url: url))
            }
        }
    }

    init(
        cafeId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CafeDashboardViewModel(cafeId: cafeId))
    }
}

private struct CafeDashboardContentView: View {
    let uiState: CafeDashboardUiState

    let onAction: (CafeDashboardAction) -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                VStack {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 48)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            } else {
                ScrollView {
                    VStack(spacing: 18) {
                        if uiState.cafe != nil {
                            heroCard
                        }
                        if let infoMessage = uiState.infoMessage {
                            infoBanner(
                                message: {
                                    switch infoMessage {
                                    case "dashboard_info_cast_list_load_failed",
                                         "dashboard_info_select_cast_for_schedule",
                                         "dashboard_info_external_link_input_required",
                                         "dashboard_info_external_link_updated",
                                         "dashboard_info_external_link_added",
                                         "dashboard_info_external_link_deleted",
                                         "dashboard_info_select_cast_for_delete",
                                         "dashboard_info_cast_deleted",
                                         "dashboard_info_cast_claim_approved",
                                         "dashboard_info_cast_claim_rejected":
                                        return String(localized: String.LocalizationValue(infoMessage), table: "Localizable")
                                    default:
                                        return infoMessage
                                    }
                                }()
                            )
                        }
                        if uiState.cafe != nil {
                            metricGrid
                            if !uiState.pendingCastClaims.isEmpty {
                                pendingCastClaimSection
                            }
                            shortcutGrid
                            castManagementSection
                            if !uiState.externalLinks.isEmpty {
                                externalLinkSection
                            }
                            homeBannerSection
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 20)
                }
            }
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

    private var pendingCastClaimSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("프로필 연결 요청")
                .font(.headline.weight(.bold))
            ForEach(Array(uiState.pendingCastClaims.prefix(3)), id: \.claimId) { claim in
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Text("\(claim.requesterNickname) → \(claim.castName)")
                            .font(.subheadline.weight(.bold))
                        Spacer()
                        Text(claim.requestedAtLabel)
                            .font(.caption2)
                            .foregroundStyle(Color(hex: "8A808A"))
                    }
                    if let message = claim.message {
                        Text(message)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "5C5760"))
                    }
                    HStack(spacing: 10) {
                        Button("승인") {
                            onAction(.clickApproveCastClaim(claim.claimId))
                        }
                        .font(.subheadline.weight(.bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color(hex: "FFD1DC"))
                        .foregroundStyle(Color(hex: "2B2330"))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        Button("반려") {
                            onAction(.clickRejectCastClaim(claim.claimId))
                        }
                        .font(.subheadline.weight(.bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.white)
                        .foregroundStyle(Color(hex: "6F6670"))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(Color(hex: "E4DDE5"), lineWidth: 1)
                        )
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(18)
                .background(Color.white.opacity(0.95))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
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
                Text(String(localized: String.LocalizationValue(shortcut.title), table: "Localizable"))
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
        return VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("소속 캐스트 관리")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "8C7A83"))
                Spacer()
                Button {
                    onAction(.clickDeleteCast)
                } label: {
                    Image(systemName: "trash")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color(hex: "EF6797"))
                        .frame(width: 34, height: 34)
                        .background(Color(hex: "FCE6EF"))
                        .clipShape(Circle())
                        .opacity(uiState.selectedCastId == nil ? 0.45 : 1)
                }
                .buttonStyle(.plain)
                .disabled(uiState.selectedCastId == nil)
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
                    ForEach(uiState.castPreviews, id: \.id) { cast in
                        castPreviewItem(cast: cast)
                    }
                    if uiState.hasMoreCasts {
                        Button {
                            onAction(.clickLoadMoreCasts)
                        } label: {
                            VStack(spacing: 8) {
                                ZStack {
                                    Circle()
                                        .fill(Color(hex: "F7F2F6"))
                                        .frame(width: 72, height: 72)
                                    if uiState.isLoadingMoreCasts {
                                        ProgressView()
                                            .tint(Color(hex: "B8AEB7"))
                                    } else {
                                        Image(systemName: "chevron.right")
                                            .foregroundStyle(Color(hex: "8F848F"))
                                    }
                                }
                                Text(uiState.isLoadingMoreCasts ? "불러오는 중" : "더 보기")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(Color(hex: "8F848F"))
                            }
                            .frame(width: 80)
                        }
                        .buttonStyle(.plain)
                    }
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

    private func castPreviewItem(cast: CafeCastPreview) -> some View {
        let isSelected = uiState.selectedCastId == cast.id
        return Button {
            onAction(.clickCastSchedule(cast.id))
        } label: {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .stroke(isSelected ? Color(hex: "EF6797") : .clear, lineWidth: 2)
                        .frame(width: 78, height: 78)
                    ZStack(alignment: .topTrailing) {
                        GeometryReader { proxy in
                            ZStack {
                                LinearGradient(
                                    colors: [Color(hex: "FFD7E3"), Color(hex: "FFF0F5")],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                                if let rawImageUrl = cast.profileImage?.trimmingCharacters(in: .whitespacesAndNewlines),
                                   !rawImageUrl.isEmpty,
                                   let imageUrl = URL(string: rawImageUrl) {
                                    CachedAsyncImage(
                                        url: imageUrl,
                                        placeholder: Color.clear
                                    )
                                }
                            }
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .clipShape(Circle())
                            .clipped()
                        }
                        .frame(width: 72, height: 72)
                        if isSelected {
                            Circle()
                                .fill(Color(hex: "EF6797"))
                                .frame(width: 22, height: 22)
                                .overlay {
                                    Image(systemName: "checkmark")
                                        .font(.caption2.weight(.bold))
                                        .foregroundStyle(.white)
                                }
                        }
                    }
                    Circle()
                        .fill(cast.isOnShift ? Color(hex: "35C26B") : Color(hex: "C7CBD3"))
                        .frame(width: 16, height: 16)
                        .offset(x: 24, y: 24)
                }
                .frame(width: 78, height: 78)
                Text(cast.name)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(isSelected ? Color(hex: "EF6797") : Color(hex: "2B2330"))
            }
            .frame(width: 80)
        }
        .buttonStyle(.plain)
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
                    GeometryReader { proxy in
                        let imageSize = proxy.size
                        let imageUrl = cafe.homeBannerPreview.imageUrl?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                        let resolvedUrl = imageUrl.isEmpty ? nil : URL(string: imageUrl)

                        ZStack {
                            LinearGradient(
                                colors: [Color(hex: "FFD1DC"), Color(hex: "FFE4EC")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                            if let resolvedUrl {
                                CachedAsyncImage(
                                    url: resolvedUrl,
                                    placeholder: Color.clear
                                )
                                .frame(width: imageSize.width, height: imageSize.height)
                                .clipped()
                            } else {
                                Image(systemName: "photo")
                                    .foregroundStyle(.white)
                            }
                        }
                    }
                    .frame(width: 96, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .clipped()
                    VStack(alignment: .leading, spacing: 6) {
                        Text(cafe.homeBannerPreview.title)
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Color(hex: "2B2330"))
                        Text(
                            {
                                let period = cafe.homeBannerPreview.period
                                if period.hasPrefix("dashboard_banner_period_days:") {
                                    let days = Int(period.split(separator: ":").last ?? "0") ?? 0
                                    return String(
                                        format: String(localized: String.LocalizationValue("dashboard_banner_period_days"), table: "Localizable"),
                                        days
                                    )
                                } else {
                                    return period
                                }
                            }()
                        )
                            .font(.caption)
                            .foregroundStyle(Color(hex: "7E7480"))
                        Text(
                            {
                                let statusLabel = cafe.homeBannerPreview.statusLabel
                                switch statusLabel {
                                case "dashboard_banner_status_active",
                                     "dashboard_banner_status_scheduled",
                                     "dashboard_banner_status_hidden":
                                    return String(localized: String.LocalizationValue(statusLabel), table: "Localizable")
                                default:
                                    return statusLabel
                                }
                            }()
                        )
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
                    onAction(.clickCreateBanner)
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

    private var externalLinkSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("외부 링크")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Color(hex: "8C7A83"))
                    Text("앱 외부로 연결할 링크를 관리합니다.")
                        .font(.caption)
                        .foregroundStyle(Color(hex: "7E7480"))
                }
                Spacer()
                Button("추가") {
                    onAction(.clickShortcut(.externalLinks))
                }
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color(hex: "EF6797"))
            }
            VStack(alignment: .leading, spacing: 12) {
                ForEach(uiState.externalLinks) { link in
                    HStack(spacing: 14) {
                        Button {
                            onAction(.clickExternalLinkItem(link.id))
                        } label: {
                            HStack(spacing: 14) {
                                ZStack {
                                    LinearGradient(
                                        colors: [Color(hex: "FFD1DC"), Color(hex: "FFE4EC")],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                    Image(systemName: "link")
                                        .foregroundStyle(.white)
                                }
                                .frame(width: 52, height: 52)
                                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                Text(link.title)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(Color(hex: "2B2330"))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                        Button {
                            onAction(.clickEditExternalLink(link.id))
                        } label: {
                            Image(systemName: "pencil")
                                .foregroundStyle(Color(hex: "8F848F"))
                        }
                        .buttonStyle(.plain)
                        Button {
                            onAction(.clickDeleteExternalLink(link.id))
                        } label: {
                            Image(systemName: "trash")
                                .foregroundStyle(Color(hex: "8F848F"))
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(Color(hex: "FFFBFD"))
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .stroke(Color(hex: "F0E6EC"), lineWidth: 1)
                    )
                }
                Button {
                    onAction(.clickShortcut(.externalLinks))
                } label: {
                    Text(String(localized: String.LocalizationValue("dashboard_external_link_add"), table: "Localizable"))
                        .fontWeight(.bold)
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
}

private struct ExternalLinkInputSheet: View {
    let uiState: CafeDashboardUiState

    let onAction: (CafeDashboardAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(
                uiState.editingExternalLinkId == nil
                ? String(localized: String.LocalizationValue("dashboard_external_link_add"), table: "Localizable")
                : String(localized: String.LocalizationValue("dashboard_external_link_edit"), table: "Localizable")
            )
                .font(.title3.weight(.bold))
            Text("홈이나 카페 화면에서 연결할 외부 링크를 간단히 등록합니다.")
                .font(.subheadline)
                .foregroundStyle(Color(hex: "7A707A"))
            ConCafeFormField(
                label: "제목",
                text: Binding(
                    get: { uiState.externalLinkTitle },
                    set: { onAction(.changeExternalLinkTitle($0)) }
                ),
                placeholder: "예: 공식 X 계정"
            )
            ConCafeFormField(
                label: "링크 URL",
                text: Binding(
                    get: { uiState.externalLinkUrl },
                    set: { onAction(.changeExternalLinkUrl($0)) }
                ),
                placeholder: "https://"
            )
            Button {
                onAction(.submitExternalLink)
            } label: {
                Text(
                    uiState.editingExternalLinkId == nil
                    ? String(localized: String.LocalizationValue("dashboard_external_link_add"), table: "Localizable")
                    : String(localized: String.LocalizationValue("dashboard_external_link_save"), table: "Localizable")
                )
                    .font(.headline.weight(.bold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(uiState.isExternalLinkSubmitEnabled ? Color(hex: "FFD1DC") : Color(hex: "F4D7DF"))
                    .foregroundStyle(uiState.isExternalLinkSubmitEnabled ? Color(hex: "2B2330") : Color(hex: "7F7078"))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(!uiState.isExternalLinkSubmitEnabled)
            Button("닫기") {
                onAction(.dismissExternalLinkSheet)
            }
            .font(.subheadline.weight(.semibold))
            .frame(maxWidth: .infinity)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Color(hex: "FFFBFD"))
    }
}
