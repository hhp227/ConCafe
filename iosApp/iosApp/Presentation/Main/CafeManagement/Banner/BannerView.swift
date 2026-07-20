//
//  BannerView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import SwiftUI

struct BannerView: View {
    let cafeId: String?
    
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: BannerViewModel
    
    @State private var alertMessage: String?

    var body: some View {
        BannerContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(String(localized: String.LocalizationValue("banner_screen_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                } label: {
                    Image(systemName: "ellipsis")
                }
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToBannerEdit(let cafeId, let bannerId):
                onNavigationAction(.navigateToBannerEdit(cafeId: cafeId, bannerId: bannerId))
            case .showMessage(let message):
                alertMessage = message
            }
        }
        .confirmationDialog(
            String(localized: String.LocalizationValue("banner_dialog_delete_title"), table: "Localizable"),
            isPresented: Binding(
                get: { viewModel.uiState.pendingDeleteBanner != nil },
                set: { isPresented in
                    if !isPresented {
                        viewModel.onAction(.dismissDeleteBannerDialog)
                    }
                }
            ),
            titleVisibility: .visible
        ) {
            Button(String(localized: String.LocalizationValue("banner_action_confirm"), table: "Localizable"), role: .destructive) {
                viewModel.onAction(.confirmDeleteBanner)
            }
            Button(String(localized: String.LocalizationValue("banner_action_cancel"), table: "Localizable"), role: .cancel) {
                viewModel.onAction(.dismissDeleteBannerDialog)
            }
        } message: {
            Text(String(format: String(localized: String.LocalizationValue("banner_dialog_delete_message"), table: "Localizable"), viewModel.uiState.pendingDeleteBanner?.title ?? ""))
        }
        .alert(
            String(localized: String.LocalizationValue("banner_alert_title"), table: "Localizable"),
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
            Button(String(localized: String.LocalizationValue("banner_action_ok"), table: "Localizable"), role: .cancel) {
                alertMessage = nil
            }
        } message: { message in
            Text(
                {
                    switch message {
                    case "banner_info_load_failed":
                        return String(localized: String.LocalizationValue("banner_info_load_failed"), table: "Localizable")
                    case "banner_info_deleted":
                        return String(localized: String.LocalizationValue("banner_info_deleted"), table: "Localizable")
                    case "banner_info_delete_failed":
                        return String(localized: String.LocalizationValue("banner_info_delete_failed"), table: "Localizable")
                    default:
                        return message
                    }
                }()
            )
        }
    }

    init(
        cafeId: String?,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: BannerViewModel(cafeId: cafeId))
    }
}

private struct BannerContentView: View {
    let uiState: BannerUiState
    
    let onAction: (BannerAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            ConCafeTabBar(
                labels: BannerTab.allCases.map { String(localized: String.LocalizationValue($0.rawValue), table: "Localizable") },
                selectedIndex: BannerTab.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
                backgroundColor: ConCafeColors.background,
                onSelect: { index in
                    onAction(.selectTab(BannerTab.allCases[index]))
                }
            )
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    headerRow
                    ForEach(uiState.filteredBanners) { banner in
                        BannerCardView(
                            banner: banner,
                            onEdit: { onAction(.editBannerTapped(id: banner.id)) },
                            onDelete: { onAction(.deleteBannerTapped(id: banner.id)) }
                        )
                    }
                    Text(String(localized: String.LocalizationValue("banner_info_max_five"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(ConCafeColors.textMuted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 16)
            }
        }
        .background(ConCafeColors.background)
        .safeAreaInset(edge: .bottom) {
            Button {
                onAction(.createBannerTapped)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "plus.circle.fill")
                    Text(String(localized: String.LocalizationValue("banner_action_create"), table: "Localizable"))
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .foregroundStyle(.primary)
                .background(ConCafeColors.primaryContainer)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 14)
            .background(ConCafeColors.background)
        }
    }

    private var headerRow: some View {
        HStack {
            Text(
                String(
                    format: String(localized: String.LocalizationValue("banner_section_count"), table: "Localizable"),
                    {
                        switch uiState.selectedTab {
                        case .active:
                            return String(localized: String.LocalizationValue("banner_section_active"), table: "Localizable")
                        case .scheduled:
                            return String(localized: String.LocalizationValue("banner_section_scheduled"), table: "Localizable")
                        case .ended:
                            return String(localized: String.LocalizationValue("banner_section_ended"), table: "Localizable")
                        }
                    }(),
                    uiState.filteredBanners.count
                )
            )
                .font(.caption.weight(.bold))
                .foregroundStyle(.secondary)
            Spacer()
            Text(String(localized: String.LocalizationValue("banner_location_home_top"), table: "Localizable"))
                .font(.caption.weight(.bold))
                .foregroundStyle(ConCafeColors.primary)
        }
    }
}

private struct BannerCardView: View {
    let banner: BannerItem

    let onEdit: () -> Void

    let onDelete: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            thumbnail
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top) {
                    Text(String(localized: String.LocalizationValue(banner.statusLabelKey), table: "Localizable"))
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(ConCafeColors.primary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(ConCafeColors.primaryContainer.opacity(0.28))
                        .clipShape(Capsule())
                    Spacer()
                    HStack(spacing: 6) {
                        IconCircleButton(systemName: "pencil", action: onEdit)
                        IconCircleButton(systemName: "trash", action: onDelete)
                    }
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(banner.title)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(.primary)
                    Text(banner.description)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                HStack(spacing: 6) {
                    Image(systemName: "calendar")
                        .font(.caption2)
                    Text(String(format: String(localized: String.LocalizationValue("banner_period_days"), table: "Localizable"), banner.periodDays))
                        .font(.caption2)
                }
                .foregroundStyle(ConCafeColors.textMuted)
            }
        }
        .padding(16)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(ConCafeColors.primaryContainer.opacity(0.16), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.03), radius: 8, x: 0, y: 4)
    }

    private var thumbnail: some View {
        GeometryReader { proxy in
            let imageSize = proxy.size
            let trimmedImageUrl = banner.imageUrl?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let resolvedImageUrl = trimmedImageUrl.isEmpty ? nil : URL(string: trimmedImageUrl)

            ZStack {
                LinearGradient(
                    colors: [Color(hex: banner.accentHex), ConCafeColors.surfaceTint],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                if let resolvedImageUrl {
                    CachedAsyncImage(
                        url: resolvedImageUrl,
                        placeholder: Color.clear,
                        displaySize: .medium
                    )
                    .frame(width: imageSize.width, height: imageSize.height)
                    .clipped()
                } else {
                    Image(systemName: banner.imageIcon)
                        .font(.system(size: 34, weight: .semibold))
                        .foregroundStyle(.white)
                }
            }
        }
        .frame(width: 96, height: 60)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .clipped()
    }
}

private struct IconCircleButton: View {
    let systemName: String

    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.caption.weight(.bold))
                .foregroundStyle(ConCafeColors.textMuted)
                .frame(width: 28, height: 28)
        }
        .buttonStyle(.plain)
    }
}

struct BannerView_Previews: PreviewProvider {
    static var previews: some View {
        CompatNavigationContainer {
            BannerView(cafeId: nil, onNavigationAction: { _ in })
        }
    }
}
