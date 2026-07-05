//
//  CastListView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/07/05.
//

import SwiftUI
import Shared

struct CastListView: View {
    let onNavigationAction: (NavigationAction) -> Void

    private let cafeId: String

    @StateObject private var viewModel: CastListViewModel

    var body: some View {
        CastListContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(String(localized: String.LocalizationValue("castlist_screen_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    viewModel.onAction(.clickAddCast)
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "person.badge.plus")
                            .font(.caption.weight(.bold))
                        Text(String(localized: String.LocalizationValue("castlist_action_add"), table: "Localizable"))
                            .font(.subheadline.weight(.bold))
                    }
                    .foregroundStyle(Color(hex: "EF6797"))
                }
            }
        }
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
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToCastEdit(let cafeId, let castId):
                onNavigationAction(.navigateToCastEdit(cafeId: cafeId, castId: castId))
            case .navigateToSchedule(let castId):
                onNavigationAction(.navigateToSchedule(castId: castId))
            }
        }
    }

    init(
        cafeId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CastListViewModel(cafeId: cafeId))
    }
}

private struct CastListContentView: View {
    let uiState: CastListUiState

    let onAction: (CastListAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            searchField
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            if let infoMessage = uiState.infoMessage {
                infoBanner(
                    message: {
                        switch infoMessage {
                        case "castlist_info_load_failed",
                             "castlist_info_cast_deleted":
                            return String(localized: String.LocalizationValue(infoMessage), table: "Localizable")
                        default:
                            return infoMessage
                        }
                    }()
                )
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
            if uiState.isLoading {
                Spacer()
                ProgressView()
                    .tint(Color(hex: "EF6797"))
                Spacer()
            } else if uiState.filteredCasts.isEmpty {
                Spacer()
                Text(
                    uiState.isSearching
                    ? String(localized: String.LocalizationValue("castlist_empty_search"), table: "Localizable")
                    : String(localized: String.LocalizationValue("castlist_empty"), table: "Localizable")
                )
                .font(.subheadline)
                .foregroundStyle(Color(hex: "8F848F"))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(uiState.filteredCasts, id: \.id) { cast in
                            castListItemCard(cast: cast)
                        }
                        if uiState.hasMoreCasts && !uiState.isSearching {
                            loadMoreRow
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
            }
        }
        .background(Color(hex: "FFF9FC"))
    }

    private var searchField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: String.LocalizationValue("castlist_search_label"), table: "Localizable"))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Color(hex: "8F848F"))
                TextField(
                    String(localized: String.LocalizationValue("castlist_search_placeholder"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.searchQuery },
                        set: { onAction(.changeSearchQuery($0)) }
                    )
                )
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.7), lineWidth: 1)
            )
        }
    }

    private func castListItemCard(cast: CafeCastPreview) -> some View {
        Button {
            onAction(.clickCast(cast.id))
        } label: {
            HStack(spacing: 12) {
                ZStack(alignment: .bottomTrailing) {
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
                    .frame(width: 52, height: 52)
                    Circle()
                        .fill(cast.isOnShift ? Color(hex: "35C26B") : Color(hex: "C7CBD3"))
                        .frame(width: 14, height: 14)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(cast.name)
                        .font(.body.weight(.bold))
                        .foregroundStyle(.primary)
                    shiftBadge(isOnShift: cast.isOnShift)
                }
                Spacer()
                Button {
                    onAction(.clickCastSchedule(cast.id))
                } label: {
                    Image(systemName: "calendar")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color(hex: "EF6797"))
                        .frame(width: 34, height: 34)
                        .background(Color(hex: "FCE6EF"))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                Button {
                    onAction(.clickDeleteCast(cast.id))
                } label: {
                    Image(systemName: "trash")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color(hex: "EF6797"))
                        .frame(width: 34, height: 34)
                        .background(Color(hex: "F6EEF2"))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color(hex: "E8DFE7"), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func shiftBadge(isOnShift: Bool) -> some View {
        Text(
            isOnShift
            ? String(localized: String.LocalizationValue("castlist_on_shift"), table: "Localizable")
            : String(localized: String.LocalizationValue("castlist_off_shift"), table: "Localizable")
        )
        .font(.caption2.weight(.bold))
        .foregroundStyle(isOnShift ? Color(hex: "1F8B5F") : Color(hex: "8F848F"))
        .padding(.horizontal, 8)
        .padding(.vertical, 2)
        .background(isOnShift ? Color(hex: "ECFFF5") : Color(hex: "F4F1F4"))
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(isOnShift ? Color(hex: "C8EFD9") : Color(hex: "E3DCE3"), lineWidth: 1)
        )
    }

    private var loadMoreRow: some View {
        Button {
            onAction(.clickLoadMoreCasts)
        } label: {
            Group {
                if uiState.isLoadingMore {
                    ProgressView()
                        .tint(Color(hex: "EF6797"))
                } else {
                    Text(String(localized: String.LocalizationValue("castlist_action_load_more"), table: "Localizable"))
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color(hex: "8C7A83"))
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color(hex: "F7F2F6"))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color(hex: "E3DCE3"), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "info.circle.fill")
                .font(.subheadline)
                .foregroundStyle(Color(hex: "EF6797"))
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.dismissInfoMessage)
            } label: {
                Text(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"))
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color(hex: "FFD1DC").opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}
