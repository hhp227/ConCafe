//
//  CafeEventView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/16.
//

import SwiftUI
import UIKit
import Shared

private let cafeEventHeroHeight: CGFloat = 260
private let cafeEventHeroTriggerOffset: CGFloat = 20

struct CafeEventView: View {
    let cafeId: String

    let eventId: String

    let showCafeButton: Bool

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CafeEventViewModel

    var body: some View {
        CafeEventContentView(
            uiState: viewModel.uiState,
            showCafeButton: showCafeButton,
            onAction: viewModel.onAction
        )
        .compatNavigationBarStyle(.transparentScrollEdge)
        .alert(
            String(localized: String.LocalizationValue("auth_login_required_title"), table: "Localizable"),
            isPresented: Binding(
                get: { viewModel.uiState.isLoginPromptVisible },
                set: { isPresented in
                    if !isPresented {
                        viewModel.onAction(.dismissLoginPrompt)
                    }
                }
            )
        ) {
            Button(String(localized: String.LocalizationValue("common_cancel"), table: "Localizable"), role: .cancel) {
                viewModel.onAction(.dismissLoginPrompt)
            }
            Button(String(localized: String.LocalizationValue("signin_submit"), table: "Localizable")) {
                viewModel.onAction(.loginPromptSignInTapped)
            }
        } message: {
            Text(String(localized: String.LocalizationValue("auth_login_required_message"), table: "Localizable"))
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToCafe:
                onNavigationAction(.replaceWithCafe(id: cafeId))
            case .navigateToCast(let id):
                onNavigationAction(.navigateToCast(id: id))
            case .navigateToSignIn:
                onNavigationAction(.navigateToSignIn)
            }
        }
    }

    init(
        cafeId: String,
        eventId: String,
        showCafeButton: Bool = false,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.eventId = eventId
        self.showCafeButton = showCafeButton
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CafeEventViewModel(cafeId: cafeId, eventId: eventId))
    }
}

private struct CafeEventContentView: View {
    let uiState: CafeEventUiState

    let showCafeButton: Bool

    let onAction: (CafeEventAction) -> Void

    @State private var scrollOffset: CGFloat = 0

    @State private var heroBottomMinY: CGFloat = .greatestFiniteMagnitude

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                mainContent(topSafeArea: proxy.safeAreaInsets.top)
                    .background(Color(uiColor: .systemBackground))
            }
            .ignoresSafeArea(edges: .top)
        }
    }

    @ViewBuilder
    private func mainContent(topSafeArea: CGFloat) -> some View {
        if let event = uiState.event {
            ScrollView {
                scrollOffsetReader
                LazyVStack(spacing: 0) {
                    CafeEventHeroSection(event: event, scrollOffset: scrollOffset, topSafeArea: topSafeArea)
                        .background(heroBottomReader)
                    CafeEventInfoSection(
                        event: event,
                        uiState: uiState,
                        showCafeButton: showCafeButton,
                        onAction: onAction
                    )
                    if !uiState.participantCasts.isEmpty {
                        CafeEventCastSection(
                            casts: uiState.participantCasts,
                            onCastTap: { id in onAction(.castTapped(id: id)) }
                        )
                    }
                    CafeEventContentSection(event: event)
                }
                .padding(.bottom, 32)
            }
            .coordinateSpace(name: "cafeEventScroll")
            .onPreferenceChange(CafeEventScrollOffsetPreferenceKey.self) { value in
                scrollOffset = value
            }
            .onPreferenceChange(CafeEventHeroBottomPreferenceKey.self) { value in
                heroBottomMinY = value
            }
        } else if uiState.isLoading {
            ProgressView()
                .tint(Color(hex: "EF6797"))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            CafeEventErrorContent(
                message: uiState.errorMessage == "Event not found."
                    ? String(localized: String.LocalizationValue("noticeevent_validation_event_required"), table: "Localizable")
                    : String(localized: String.LocalizationValue("noticeevent_info_event_load_failed"), table: "Localizable"),
                onRetry: { onAction(.retry) }
            )
        }
    }

    private var scrollOffsetReader: some View {
        GeometryReader { proxy in
            Color.clear
                .preference(
                    key: CafeEventScrollOffsetPreferenceKey.self,
                    value: proxy.frame(in: .named("cafeEventScroll")).minY
                )
        }
        .frame(height: 0)
        .onPreferenceChange(CafeEventScrollOffsetPreferenceKey.self) { value in
            scrollOffset = value
        }
    }

    private var heroBottomReader: some View {
        GeometryReader { proxy in
            Color.clear
                .preference(
                    key: CafeEventHeroBottomPreferenceKey.self,
                    value: proxy.frame(in: .named("cafeEventScroll")).maxY
                )
        }
    }
}

private struct CafeEventHeroSection: View {
    let event: CafeEventManagementItem

    let scrollOffset: CGFloat

    let topSafeArea: CGFloat

    var body: some View {
        let baseHeight = cafeEventHeroHeight + topSafeArea
        let pullDownOffset = scrollOffset > 0 ? scrollOffset : 0
        let dynamicHeight = baseHeight + pullDownOffset

        ZStack {
            if let imageUrl = resolvedImageUrl(event.imageUrl) {
                GeometryReader { geo in
                    CachedAsyncImage(url: imageUrl, placeholder: heroPlaceholder, displaySize: .full)
                        .frame(width: geo.size.width, height: geo.size.height)
                        .clipped()
                }
            } else {
                heroPlaceholder
            }
            LinearGradient(
                colors: [.clear, Color.black.opacity(0.35)],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .frame(height: dynamicHeight)
        .offset(y: pullDownOffset > 0 ? -pullDownOffset : 0)
        .clipped()
    }

    private var heroPlaceholder: some View {
        LinearGradient(
            colors: [Color(hex: "FDE7EF"), Color(hex: "FCCFDF")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private func resolvedImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? nil : URL(string: trimmed)
    }
}

private struct CafeEventInfoSection: View {
    let event: CafeEventManagementItem

    let uiState: CafeEventUiState

    let showCafeButton: Bool

    let onAction: (CafeEventAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Text(event.statusLabel)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "9E2E5C"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(Color(hex: "FDE7EF"), in: Capsule())
                if event.hasLivePerformance {
                    HStack(spacing: 4) {
                        Image(systemName: "music.note")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(Color(hex: "C25800"))
                        Text(String(localized: String.LocalizationValue("cafeevent_live_performance_badge"), table: "Localizable"))
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Color(hex: "C25800"))
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 7)
                    .background(Color(hex: "FFF0E0"), in: Capsule())
                }
            }
            Text(event.title)
                .font(.title2.weight(.bold))
                .foregroundStyle(.primary)
            HStack(spacing: 6) {
                Image(systemName: "calendar")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(event.periodText)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            HStack(spacing: 12) {
                Button {
                    onAction(.toggleLike)
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: uiState.isLikedByMe ? "heart.fill" : "heart")
                            .font(.system(size: 18))
                            .foregroundStyle(uiState.isLikedByMe ? Color(hex: "EF6797") : Color.secondary)
                        Text(String(format: String(localized: String.LocalizationValue("cafeevent_like_count"), table: "Localizable"), uiState.likeCount))
                            .font(.subheadline)
                            .foregroundStyle(uiState.isLikedByMe ? Color(hex: "EF6797") : Color.secondary)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 50)
                            .fill(uiState.isLikedByMe ? Color(hex: "EF6797").opacity(0.08) : Color(uiColor: .secondarySystemBackground))
                    )
                }
                .buttonStyle(.plain)
                .disabled(uiState.isTogglingLike)
                if showCafeButton {
                    Button {
                        onAction(.goToCafe)
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "storefront.fill")
                            Text(String(localized: String.LocalizationValue("cafeevent_go_to_cafe"), table: "Localizable"))
                                .fontWeight(.bold)
                        }
                        .font(.subheadline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color(hex: "EF6797"))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(uiColor: .systemBackground))
    }
}

private struct CafeEventCastSection: View {
    let casts: [Cast]

    let onCastTap: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: String.LocalizationValue("cafeevent_section_participating_cast"), table: "Localizable"))
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
                .padding(.horizontal, 20)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(casts, id: \.id) { cast in
                        VStack(spacing: 8) {
                            GeometryReader { proxy in
                                ZStack {
                                    Circle()
                                        .fill(LinearGradient(colors: [Color(hex: "FDE7EF"), Color(hex: "FCCFDF")], startPoint: .top, endPoint: .bottom))
                                    if let rawImageUrl = cast.profileImage?.trimmingCharacters(in: .whitespacesAndNewlines),
                                       !rawImageUrl.isEmpty,
                                       let imageUrl = URL(string: rawImageUrl) {
                                        CachedAsyncImage(url: imageUrl, displaySize: .thumbnail)
                                    }
                                }
                                .frame(width: proxy.size.width, height: proxy.size.height)
                                .clipShape(Circle())
                            }
                            .frame(width: 74, height: 74)
                            Text(cast.name)
                                .font(.caption)
                                .lineLimit(1)
                        }
                        .onTapGesture {
                            onCastTap(cast.id)
                        }
                    }
                }
                .padding(.horizontal, 20)
            }
        }
        .padding(.bottom, 8)
    }
}

private struct CafeEventContentSection: View {
    let event: CafeEventManagementItem

    var body: some View {
        Text(event.content)
            .font(.body)
            .foregroundStyle(.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 20)
    }
}

private struct CafeEventErrorContent: View {
    let message: String

    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button(String(localized: String.LocalizationValue("cafe_error_retry_prompt"), table: "Localizable")) {
                onRetry()
            }
            .font(.subheadline.weight(.semibold))
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct CafeEventScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct CafeEventHeroBottomPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = min(value, nextValue())
    }
}
