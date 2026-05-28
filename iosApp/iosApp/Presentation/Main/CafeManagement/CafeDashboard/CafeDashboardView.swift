//
//  CafeDashboardView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import SwiftUI
import UIKit
import Foundation
import Photos
import Shared

struct CafeDashboardView: View {
    let onNavigationAction: (NavigationAction) -> Void

    private let cafeId: String

    @StateObject private var viewModel: CafeDashboardViewModel

    @State private var isQrSheetPresented = false

    var body: some View {
        CafeDashboardContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction,
            onQrMetricTap: { isQrSheetPresented = true }
        )
        .navigationTitle(viewModel.uiState.cafe?.name ?? String(localized: String.LocalizationValue("dashboard_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .alert(String(localized: String.LocalizationValue("dashboard_delete_cast_title"), table: "Localizable"), isPresented: Binding(
            get: { viewModel.uiState.isDeleteCastDialogVisible },
            set: { isPresented in
                if !isPresented {
                    viewModel.onAction(.dismissDeleteCastDialog)
                }
            }
        )) {
            Button(String(localized: String.LocalizationValue("common_cancel"), table: "Localizable"), role: .cancel) {
                viewModel.onAction(.dismissDeleteCastDialog)
            }
            Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"), role: .destructive) {
                viewModel.onAction(.confirmDeleteCast)
            }
        } message: {
            Text(String(localized: String.LocalizationValue("dashboard_delete_cast_message"), table: "Localizable"))
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
        .fullScreenCover(isPresented: Binding(
            get: { viewModel.uiState.isSocialMediaSheetVisible },
            set: { isPresented in
                if !isPresented {
                    viewModel.onAction(.dismissSocialMediaSheet)
                }
            }
        )) {
            SocialMediaInputSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
        }
        .fullScreenCover(isPresented: Binding(
            get: { viewModel.uiState.isReservationSheetVisible },
            set: { isPresented in
                if !isPresented {
                    viewModel.onAction(.dismissReservationSheet)
                }
            }
        )) {
            ReservationInputSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
        }
        .fullScreenCover(isPresented: Binding(
            get: { viewModel.uiState.isTableCountSheetVisible },
            set: { isPresented in
                if !isPresented {
                    viewModel.onAction(.dismissTableCountSheet)
                }
            }
        )) {
            TableCountInputSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
        }
        .fullScreenCover(isPresented: Binding(
            get: { viewModel.uiState.isGuestSheetVisible },
            set: { isPresented in
                if !isPresented {
                    viewModel.onAction(.dismissGuestSheet)
                }
            }
        )) {
            GuestScheduleInputSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
        }
        .sheet(isPresented: $isQrSheetPresented) {
            DashboardQrSheetView(
                payload: buildCafeCheckInQrPayload(cafeId: viewModel.uiState.cafe?.id ?? cafeId)
            )
            .compatFractionSheetDetent(0.58)
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

    let onQrMetricTap: () -> Void


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
                GeometryReader { geometry in
                    let contentWidth = max(0, geometry.size.width - 40)

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
                                             "dashboard_info_cast_claim_rejected",
                                             "dashboard_info_social_media_saved",
                                             "dashboard_info_reservation_saved",
                                        "dashboard_info_table_counts_saved":
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
                                shortcutGrid(contentWidth: contentWidth)
                                castManagementSection
                                guestManagementSection
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
        }
        .background(Color(hex: "FFF9FC"))
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
            Text(String(localized: String.LocalizationValue("dashboard_hero_subtitle"), table: "Localizable"))
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
            sectionHeader(
                title: String(localized: String.LocalizationValue("dashboard_section_metrics_title"), table: "Localizable"),
                subtitle: String(localized: String.LocalizationValue("dashboard_section_metrics_subtitle"), table: "Localizable")
            )
            HStack(spacing: 12) {
                dashboardMetricCard(title: String(localized: String.LocalizationValue("dashboard_metric_today_checkin"), table: "Localizable"), value: "\(cafe.todayCheckIns)", accent: Color(hex: "EF6797"))
                dashboardMetricCard(title: String(localized: String.LocalizationValue("dashboard_metric_follower"), table: "Localizable"), value: "\(cafe.followerCount)", accent: Color(hex: "47A88B"))
                dashboardMetricCard(
                    title: String(localized: String.LocalizationValue("dashboard_metric_table_count"), table: "Localizable"),
                    value: "\(cafe.tableCounts.current)/\(cafe.tableCounts.total)",
                    accent: Color(hex: "7C3AED"),
                    onTap: { onAction(.clickTableCountMetric) }
                )
            }
        }
    }

    private var pendingCastClaimSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("dashboard_pending_claim_title"), table: "Localizable"))
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
                        Button(String(localized: String.LocalizationValue("dashboard_action_approve"), table: "Localizable")) {
                            onAction(.clickApproveCastClaim(claim.claimId))
                        }
                        .font(.subheadline.weight(.bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color(hex: "FFD1DC"))
                        .foregroundStyle(.primary)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        Button(String(localized: String.LocalizationValue("dashboard_action_reject"), table: "Localizable")) {
                            onAction(.clickRejectCastClaim(claim.claimId))
                        }
                        .font(.subheadline.weight(.bold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                        .foregroundStyle(Color(hex: "6F6670"))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(Color(hex: "E4DDE5"), lineWidth: 1)
                        )
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(18)
                .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }).opacity(0.95))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
        }
    }

    private func dashboardMetricCard(title: String, value: String, accent: Color, onTap: (() -> Void)? = nil) -> some View {
        let content = VStack(alignment: .leading, spacing: 8) {
            Circle()
                .fill(accent)
                .frame(width: 10, height: 10)
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
        return Group {
            if let onTap {
                Button(action: onTap) { content }
                    .buttonStyle(.plain)
            } else {
                content
            }
        }
    }

    private func shortcutGrid(contentWidth: CGFloat) -> some View {
        let shortcuts: [CafeDashboardShortcut] = [
            .eventManagement,
            .cafeSettings,
            .menuGoods,
            .externalLinks,
            .socialMedia,
            .reservation
        ]
        return VStack(alignment: .leading, spacing: 14) {
            sectionHeader(
                title: String(localized: String.LocalizationValue("dashboard_section_menu_title"), table: "Localizable"),
                subtitle: String(localized: String.LocalizationValue("dashboard_section_menu_subtitle"), table: "Localizable")
            )
            dashboardQrMetricCard
            LazyVGrid(
                columns: dashboardMenuGridColumns(count: dashboardMenuColumnCount(for: contentWidth)),
                spacing: 12
            ) {
                ForEach(shortcuts) { shortcut in
                    shortcutCard(shortcut: shortcut)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func dashboardMenuGridColumns(count: Int) -> [GridItem] {
        Array(repeating: GridItem(.flexible(), spacing: 12), count: count)
    }

    private func dashboardMenuColumnCount(for width: CGFloat) -> Int {
        if width >= 1300 {
            return 5
        } else if width >= 1000 {
            return 4
        } else if width >= 700 {
            return 3
        } else {
            return 2
        }
    }

    private var dashboardQrMetricCard: some View {
        Button {
            onQrMetricTap()
        } label: {
            HStack {
                HStack(spacing: 10) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(Color(hex: "FCE6EF"))
                            .frame(width: 40, height: 40)
                        Image(systemName: "qrcode")
                            .foregroundStyle(Color(hex: "EF6797"))
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: String.LocalizationValue("dashboard_metric_checkin_qr"), table: "Localizable"))
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.primary)
                        Text(String(localized: String.LocalizationValue("dashboard_metric_checkin_qr_hint"), table: "Localizable"))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color(hex: "B8ACB4"))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
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
            case .socialMedia: return "square.and.arrow.up"
            case .reservation: return "bookmark.fill"
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
                    .foregroundStyle(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 110, alignment: .topLeading)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
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
                Text(String(localized: String.LocalizationValue("dashboard_section_cast_management"), table: "Localizable"))
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
                        Text(String(localized: String.LocalizationValue("dashboard_action_schedule_management"), table: "Localizable"))
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
                            Text(String(localized: String.LocalizationValue("dashboard_action_add"), table: "Localizable"))
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
                                Text(
                                    uiState.isLoadingMoreCasts
                                    ? String(localized: String.LocalizationValue("dashboard_action_loading"), table: "Localizable")
                                    : String(localized: String.LocalizationValue("dashboard_action_load_more"), table: "Localizable")
                                )
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
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
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
                    .foregroundStyle(isSelected ? Color(hex: "EF6797") : .primary)
            }
            .frame(width: 80)
        }
        .buttonStyle(.plain)
    }

    private var guestManagementSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("게스트 캐스트 관리")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Color(hex: "8C7A83"))
                    Text("소속 캐스트가 아닌 하루 출연자를 관리합니다")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button {
                    onAction(.clickAddGuest)
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                        Text("추가")
                            .fontWeight(.bold)
                    }
                    .font(.caption)
                    .foregroundStyle(Color(hex: "EF6797"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color(hex: "FCE6EF"))
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
            if uiState.guestSchedules.isEmpty {
                Text("등록된 게스트 캐스트가 없습니다")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(Color(hex: "F8F2F6"))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            } else {
                VStack(spacing: 8) {
                    ForEach(uiState.guestSchedules, id: \.id) { guest in
                        guestManagementRow(guest)
                    }
                }
            }
        }
        .padding(18)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }

    private func guestManagementRow(_ guest: GuestCastSchedule) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color(hex: "FFE2EC"))
                .frame(width: 46, height: 46)
                .overlay {
                    if let rawImageUrl = guest.profileImage,
                       let imageUrl = ImageUrlUtils.normalizedRemoteUrl(from: rawImageUrl) {
                        CachedAsyncImage(url: imageUrl, displaySize: .thumbnail)
                            .clipShape(Circle())
                    } else {
                        Text(String(guest.name.prefix(2)))
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Color(hex: "8B3154"))
                    }
                }
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(guest.name)
                        .font(.subheadline.weight(.bold))
                    Text("게스트")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(Color(hex: "EF6797"))
                        .padding(.horizontal, 7)
                        .padding(.vertical, 2)
                        .background(Color(hex: "FCE6EF"))
                        .clipShape(Capsule())
                }
                Text("\(guest.date) · \(guest.startTime) - \(guest.endTime)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if let memo = guest.memo, !memo.isEmpty {
                    Text(memo)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            Button {
                onAction(.deleteGuest(guest.id))
            } label: {
                Image(systemName: "trash")
                    .foregroundStyle(Color(hex: "C15A7B"))
                    .frame(width: 34, height: 34)
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .background(Color(hex: "FFF8FB"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color(hex: "F0D9E4"), lineWidth: 1)
        )
    }

    private var homeBannerSection: some View {
        let cafe = uiState.cafe!
        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(String(localized: String.LocalizationValue("dashboard_section_home_banner"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "8C7A83"))
                Spacer()
                Button(String(localized: String.LocalizationValue("dashboard_action_view_all"), table: "Localizable")) {
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
                            .foregroundStyle(.primary)
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
                        Text(String(localized: String.LocalizationValue("dashboard_action_create_banner"), table: "Localizable"))
                            .fontWeight(.bold)
                    }
                    .foregroundStyle(.primary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color(hex: "FFD1DC"))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(16)
            .background(Color(uiColor: .systemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color(hex: "F0E6EC"), lineWidth: 1)
            )
        }
        .padding(18)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }

    private func sectionHeader(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
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
        return RatingUtils.formatOneDecimalTruncated(rating)
    }

    private var externalLinkSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: String.LocalizationValue("dashboard_shortcut_external_links"), table: "Localizable"))
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Color(hex: "8C7A83"))
                    Text(String(localized: String.LocalizationValue("dashboard_external_link_section_subtitle"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(Color(hex: "7E7480"))
                }
                Spacer()
                Button(String(localized: String.LocalizationValue("dashboard_action_add"), table: "Localizable")) {
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
                                    .foregroundStyle(.primary)
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
                    .background(Color(uiColor: .systemGroupedBackground))
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
                        .foregroundStyle(.primary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color(hex: "FFD1DC"))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(16)
            .background(Color(uiColor: .systemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color(hex: "F0E6EC"), lineWidth: 1)
            )
        }
        .padding(18)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
        )
    }
}

private struct DashboardQrSheetView: View {
    let payload: String

    @State private var saveResultMessage: String?

    var body: some View {
        VStack(spacing: 12) {
            Text(String(localized: String.LocalizationValue("dashboard_qr_sheet_title"), table: "Localizable"))
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
            Text(String(localized: String.LocalizationValue("dashboard_qr_sheet_description"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            ZStack {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(Color(hex: "FFD1DC").opacity(0.1), lineWidth: 1)
                    )
                DashboardQrCodeImageView(payload: payload)
                    .frame(width: 240, height: 240)
                    .padding(14)
            }
            Button {
                saveQrImageToPhotos()
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "arrow.down.circle.fill")
                    Text(String(localized: String.LocalizationValue("dashboard_qr_sheet_save_button"), table: "Localizable"))
                        .fontWeight(.bold)
                }
                .foregroundStyle(.primary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Color(hex: "FFD1DC"))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            if let saveResultMessage {
                Text(saveResultMessage)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 12)
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
    }

    private func saveQrImageToPhotos() {
        guard let image = generateDashboardQrImage(from: payload) else {
            saveResultMessage = String(localized: String.LocalizationValue("dashboard_qr_sheet_save_failed"), table: "Localizable")
            return
        }
        saveDashboardQrImageToPhotoLibrary(image) { isSaved in
            saveResultMessage = String(localized: String.LocalizationValue(
                isSaved ? "dashboard_qr_sheet_save_success" : "dashboard_qr_sheet_save_failed"
            ), table: "Localizable")
        }
    }
}

private func buildCafeCheckInQrPayload(cafeId: String) -> String {
    "concafe://checkin?cafeId=\(cafeId)"
}

private struct ExternalLinkInputSheet: View {
    let uiState: CafeDashboardUiState

    let onAction: (CafeDashboardAction) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(String(localized: String.LocalizationValue("dashboard_external_link_guide"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                ConCafeFormField(
                    label: String(localized: String.LocalizationValue("dashboard_external_link_label_title"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.externalLinkTitle },
                        set: { onAction(.changeExternalLinkTitle($0)) }
                    ),
                    placeholder: String(localized: String.LocalizationValue("dashboard_external_link_placeholder_title"), table: "Localizable")
                )
                ConCafeFormField(
                    label: String(localized: String.LocalizationValue("dashboard_external_link_label_url"), table: "Localizable"),
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
                        .foregroundStyle(uiState.isExternalLinkSubmitEnabled ? .primary : .secondary)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(!uiState.isExternalLinkSubmitEnabled)
                Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
                    onAction(.dismissExternalLinkSheet)
                }
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .topLeading)
        }
        .background(Color(uiColor: .systemGroupedBackground))
    }
}

private struct SocialMediaInputSheet: View {
    let uiState: CafeDashboardUiState

    let onAction: (CafeDashboardAction) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(String(localized: String.LocalizationValue("dashboard_social_media_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                Text(String(localized: String.LocalizationValue("dashboard_social_media_section_subtitle"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                ConCafeFormField(
                    label: "Instagram",
                    text: Binding(
                        get: { uiState.instagramId },
                        set: { onAction(.changeSocialMediaInstagram($0)) }
                    ),
                    placeholder: "@account_id"
                )
                ConCafeFormField(
                    label: "X (Twitter)",
                    text: Binding(
                        get: { uiState.twitterId },
                        set: { onAction(.changeSocialMediaTwitter($0)) }
                    ),
                    placeholder: "@account_id"
                )
                ConCafeFormField(
                    label: "TikTok",
                    text: Binding(
                        get: { uiState.tiktokId },
                        set: { onAction(.changeSocialMediaTiktok($0)) }
                    ),
                    placeholder: "@account_id"
                )
                ConCafeFormField(
                    label: "YouTube",
                    text: Binding(
                        get: { uiState.youtubeId },
                        set: { onAction(.changeSocialMediaYoutube($0)) }
                    ),
                    placeholder: "@channel_id"
                )
                Button {
                    onAction(.submitSocialMedia)
                } label: {
                    Group {
                        if uiState.isSavingSocialMedia {
                            ProgressView()
                                .tint(.secondary)
                        } else {
                            Text(String(localized: String.LocalizationValue("dashboard_social_media_save"), table: "Localizable"))
                                .font(.headline.weight(.bold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(uiState.isSavingSocialMedia ? Color(hex: "F4D7DF") : Color(hex: "FFD1DC"))
                    .foregroundStyle(.primary)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(uiState.isSavingSocialMedia)
                Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
                    onAction(.dismissSocialMediaSheet)
                }
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .topLeading)
        }
        .background(Color(uiColor: .systemGroupedBackground))
    }
}

private struct ReservationInputSheet: View {
    let uiState: CafeDashboardUiState

    let onAction: (CafeDashboardAction) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(String(localized: String.LocalizationValue("dashboard_reservation_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                Text(String(localized: String.LocalizationValue("dashboard_reservation_guide"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                ConCafeFormField(
                    label: String(localized: String.LocalizationValue("dashboard_reservation_label_url"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.reservationUrl },
                        set: { onAction(.changeReservationUrl($0)) }
                    ),
                    placeholder: "https://"
                )
                Button {
                    onAction(.submitReservation)
                } label: {
                    Group {
                        if uiState.isSavingReservation {
                            ProgressView()
                                .tint(.secondary)
                        } else {
                            Text(String(localized: String.LocalizationValue("dashboard_reservation_save"), table: "Localizable"))
                                .font(.headline.weight(.bold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(uiState.isSavingReservation ? Color(hex: "F4D7DF") : Color(hex: "FFD1DC"))
                    .foregroundStyle(.primary)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(uiState.isSavingReservation)
                Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
                    onAction(.dismissReservationSheet)
                }
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .topLeading)
        }
        .background(Color(uiColor: .systemGroupedBackground))
    }
}

private struct TableCountInputSheet: View {
    let uiState: CafeDashboardUiState

    let onAction: (CafeDashboardAction) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(String(localized: String.LocalizationValue("dashboard_table_count_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                Text(String(localized: String.LocalizationValue("dashboard_table_count_guide"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                ConCafeFormField(
                    label: String(localized: String.LocalizationValue("dashboard_table_count_label_total"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.totalTableCountInput },
                        set: { onAction(.changeTotalTableCount($0)) }
                    ),
                    placeholder: "0",
                    keyboardType: .numberPad
                )
                ConCafeFormField(
                    label: String(localized: String.LocalizationValue("dashboard_table_count_label_current"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.currentTableCountInput },
                        set: { onAction(.changeCurrentTableCount($0)) }
                    ),
                    placeholder: "0",
                    keyboardType: .numberPad
                )
                Button {
                    onAction(.submitTableCounts)
                } label: {
                    Group {
                        if uiState.isSavingTableCounts {
                            ProgressView()
                                .tint(.secondary)
                        } else {
                            Text(String(localized: String.LocalizationValue("dashboard_table_count_save"), table: "Localizable"))
                                .font(.headline.weight(.bold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(!uiState.isTableCountSubmitEnabled || uiState.isSavingTableCounts ? Color(hex: "F4D7DF") : Color(hex: "FFD1DC"))
                    .foregroundStyle(.primary)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(!uiState.isTableCountSubmitEnabled || uiState.isSavingTableCounts)
                Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
                    onAction(.dismissTableCountSheet)
                }
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .topLeading)
        }
        .background(Color(uiColor: .systemGroupedBackground))
    }
}

private struct GuestScheduleInputSheet: View {
    let uiState: CafeDashboardUiState

    let onAction: (CafeDashboardAction) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("게스트 출연 추가")
                    .font(.headline.weight(.bold))
                Text("소속 캐스트가 아닌 하루 출연자를 카페 스케줄에 표시합니다.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                ConCafeFormField(
                    label: "게스트 이름",
                    text: Binding(get: { uiState.guestName }, set: { onAction(.changeGuestName($0)) }),
                    placeholder: "이름"
                )
                ConCafeFormField(
                    label: "프로필 이미지 URL",
                    text: Binding(get: { uiState.guestProfileImage }, set: { onAction(.changeGuestProfileImage($0)) }),
                    placeholder: "https://"
                )
                dashboardPickerField(
                    title: "출연일",
                    value: uiState.guestDate,
                    options: uiState.guestDateOptions,
                    onSelect: { onAction(.changeGuestDate($0)) }
                )
                HStack(spacing: 12) {
                    dashboardPickerField(
                        title: "시작",
                        value: uiState.guestStartTime,
                        options: uiState.guestTimeOptions,
                        onSelect: { onAction(.changeGuestStartTime($0)) }
                    )
                    dashboardPickerField(
                        title: "종료",
                        value: uiState.guestEndTime,
                        options: uiState.guestTimeOptions,
                        onSelect: { onAction(.changeGuestEndTime($0)) }
                    )
                }
                ConCafeFormField(
                    label: "메모",
                    text: Binding(get: { uiState.guestMemo }, set: { onAction(.changeGuestMemo($0)) }),
                    placeholder: "이벤트명, 참고사항"
                )
                Button {
                    onAction(.submitGuest)
                } label: {
                    Group {
                        if uiState.isGuestSaving {
                            ProgressView()
                                .tint(.secondary)
                        } else {
                            Text("게스트 출연 추가")
                                .font(.headline.weight(.bold))
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(!uiState.isGuestSubmitEnabled || uiState.isGuestSaving ? Color(hex: "F4D7DF") : Color(hex: "FFD1DC"))
                    .foregroundStyle(.primary)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(!uiState.isGuestSubmitEnabled || uiState.isGuestSaving)
                Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
                    onAction(.dismissGuestSheet)
                }
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .topLeading)
        }
        .background(Color(uiColor: .systemGroupedBackground))
    }

    private func dashboardPickerField(
        title: String,
        value: String,
        options: [String],
        onSelect: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption.weight(.bold))
                .foregroundStyle(.secondary)
            Menu {
                ForEach(options, id: \.self) { option in
                    Button(option) {
                        onSelect(option)
                    }
                }
            } label: {
                HStack {
                    Text(value)
                        .foregroundStyle(.primary)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color(uiColor: .secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
        }
    }
}
