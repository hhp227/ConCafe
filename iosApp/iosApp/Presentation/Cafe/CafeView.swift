//
//  CafeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import SwiftUI
import Shared

struct CafeView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CafeViewModel

    @State private var alertMessage: String?

    @State private var countBeforeLoad = (casts: 0, notices: 0, reviews: 0)

    @State private var castPagingAnchorId: String?

    private let topAnchorId = "CAFE_TOP"

    var body: some View {
        ScrollViewReader { proxy in
            CafeContentView(
                uiState: viewModel.uiState,
                onAction: { action in
                    switch action {
                    case .loadMoreCasts:
                        let count = viewModel.uiState.casts.count
                        countBeforeLoad.casts = count
                        castPagingAnchorId = viewModel.uiState.casts.last?.cast.id
                    case .loadMoreNotices:
                        let count = viewModel.uiState.notices.count
                        countBeforeLoad.notices = count
                    case .loadMoreReviews:
                        let count = viewModel.uiState.reviews.count
                        countBeforeLoad.reviews = count
                    case .pagingTriggerDisappeared(let tab):
                        switch tab {
                        case .casts:
                            countBeforeLoad.casts = -1
                            castPagingAnchorId = nil
                        case .notices:
                            countBeforeLoad.notices = -1
                        case .reviews:
                            countBeforeLoad.reviews = -1
                        default:
                            break
                        }
                    default:
                        break
                    }
                    viewModel.onAction(action)
                },
                topAnchorId: topAnchorId
            )
            .navigationBarTitleDisplayMode(.inline)
            .compatNavigationBarStyle(.transparentScrollEdge)
            .onReceive(viewModel.event) { event in
                switch event {
                case .navigateBack:
                    onNavigationAction(.navigateBack)
                case .navigateToCast(let id):
                    onNavigationAction(.navigateToCast(id: id))
                case .navigateToCafeEvent(let cafeId, let eventId):
                    onNavigationAction(.navigateToCafeEvent(cafeId: cafeId, eventId: eventId))
                case .navigateToReviewEdit(let cafeId, let reviewId):
                    onNavigationAction(.navigateToReviewEdit(cafeId: cafeId, reviewId: reviewId))
                case .navigateToPicture(let imageUrl):
                    onNavigationAction(.navigateToPicture(imageUrl: imageUrl))
                case .navigateToSignIn:
                    onNavigationAction(.navigateToSignIn)
                case .showReviewDeleteFailedMessage:
                    alertMessage = String(localized: String.LocalizationValue("cafe_message_review_delete_failed"), table: "Localizable")
                case .showReviewReportedMessage:
                    alertMessage = String(localized: String.LocalizationValue("cafe_message_report_received"), table: "Localizable")
                }
            }
            .alert(String(localized: String.LocalizationValue("cafe_alert_title"), table: "Localizable"), isPresented: Binding(
                get: { alertMessage != nil },
                set: { if !$0 { alertMessage = nil } }
            )) {
                Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"), role: .cancel) { alertMessage = nil }
            } message: {
                Text(alertMessage ?? "")
            }
            .onChange(of: viewModel.uiState.shouldScrollToTopOnReturn) { shouldScroll in
                guard shouldScroll else { return }
                viewModel.onAction(.consumeScrollToTopOnReturn)
                DispatchQueue.main.async {
                    withAnimation {
                        proxy.scrollTo(topAnchorId, anchor: .top)
                    }
                }
            }
            .onChange(of: viewModel.uiState.casts.count) { newCount in
                guard countBeforeLoad.casts != 0, countBeforeLoad.casts != -1 else { return }
                let preCount = countBeforeLoad.casts
                let targetId = castPagingAnchorId
                countBeforeLoad.casts = -1
                castPagingAnchorId = nil
                guard newCount > preCount, preCount > 0 else { return }
                guard viewModel.uiState.selectedTab == .casts else { return }
                guard let targetId else { return }
                DispatchQueue.main.async {
                    DispatchQueue.main.async {
                        var transaction = Transaction()
                        transaction.disablesAnimations = true
                        withTransaction(transaction) {
                            proxy.scrollTo(targetId, anchor: cafeCastPagingRestoreAnchor)
                        }
                    }
                }
            }
            .onChange(of: viewModel.uiState.notices.count) { newCount in
                guard countBeforeLoad.notices != 0, countBeforeLoad.notices != -1 else { return }
                let preCount = abs(countBeforeLoad.notices)
                let wasSubsequent = countBeforeLoad.notices > 0
                countBeforeLoad.notices = -1
                guard newCount > preCount, wasSubsequent, preCount > 0 else { return }
                guard viewModel.uiState.selectedTab == .notices else { return }
                let targetId = viewModel.uiState.notices[preCount - 1].id
                DispatchQueue.main.async {
                    proxy.scrollTo(targetId, anchor: .bottom)
                }
            }
            .onChange(of: viewModel.uiState.reviews.count) { newCount in
                guard countBeforeLoad.reviews != 0, countBeforeLoad.reviews != -1 else { return }
                let preCount = abs(countBeforeLoad.reviews)
                let wasSubsequent = countBeforeLoad.reviews > 0
                countBeforeLoad.reviews = -1
                guard newCount > preCount, wasSubsequent, preCount > 0 else { return }
                guard viewModel.uiState.selectedTab == .reviews else { return }
                let targetId = viewModel.uiState.reviews[preCount - 1].id
                DispatchQueue.main.async {
                    proxy.scrollTo(targetId, anchor: .bottom)
                }
            }
        }
    }

    init(
        cafeId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CafeViewModel(cafeId: cafeId))
    }
}

private let cafeCastPagingRestoreAnchor = UnitPoint(x: 0.5, y: 0.88)
private let cafeTabPinnedResetScrollOffset: CGFloat = -8
private let cafeCastPagingLayoutLockDelay: TimeInterval = 0.45
private let detailTooltipDurationNanoseconds: UInt64 = 5_000_000_000

private struct CafeContentView: View {
    let uiState: CafeUiState

    let onAction: (CafeAction) -> Void

    let topAnchorId: String

    @State private var scrollOffset: CGFloat = 0

    @State private var isTabPinned = false

    @State private var isCastPagingLayoutLocked = false

    @State private var castPagingLayoutLockGeneration = 0

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    offsetReader
                    content(topSafeArea: proxy.safeAreaInsets.top)
                }
                .coordinateSpace(name: "cafeScroll")
                .ignoresSafeArea(edges: .top)
                .compatScrollContentInsetAdjustmentNever()
                .background(Color(hex: "FFF9FC"))
                .onPreferenceChange(CafeScrollOffsetPreferenceKey.self) { value in
                    scrollOffset = value
                }
                .onPreferenceChange(CafeTabHeaderOffsetPreferenceKey.self) { value in
                    guard value != .greatestFiniteMagnitude, value.isFinite else { return }
                    let shouldPin = value <= proxy.safeAreaInsets.top
                    if shouldPin {
                        isTabPinned = true
                    } else if !isCastPagingLayoutLocked, scrollOffset >= cafeTabPinnedResetScrollOffset {
                        isTabPinned = false
                    }
                }
                if uiState.detail != nil, isTabPinned {
                    pinnedTabHeader()
                        .padding(.top, pinnedTabTopPadding(in: proxy))
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                        .zIndex(2)
                }
                if uiState.selectedTab == .reviews, uiState.detail != nil, uiState.isLoggedIn {
                    writeReviewButton
                    .padding(.trailing, 20)
                    .padding(.bottom, 24)
                    .zIndex(3)
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        onAction(.favoriteTapped)
                    } label: {
                        Image(systemName: uiState.isFavorite ? "heart.fill" : "heart")
                        .font(.headline)
                        .frame(width: 36, height: 36)
                    }
                }
            }
            .safeAreaInset(edge: .top) {
                if uiState.shouldShowFavoriteTooltip {
                    HStack {
                        Spacer()
                        DetailTooltipBubble(
                            text: String(localized: String.LocalizationValue("cafe_favorite_tooltip"), table: "Localizable")
                        )
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                            .zIndex(4)
                            .onAppear {
                                onAction(.favoriteTooltipShown)
                            }
                            .task(id: uiState.shouldShowFavoriteTooltip) {
                                try? await Task.sleep(nanoseconds: detailTooltipDurationNanoseconds)
                                if !Task.isCancelled {
                                    onAction(.dismissFavoriteTooltip)
                                }
                            }
                            .padding(.trailing, 8)
                    }
                }
            }
            .onChange(of: uiState.isLoadingMoreCasts) { isLoading in
                guard uiState.selectedTab == .casts else { return }
                if isLoading {
                    castPagingLayoutLockGeneration += 1
                    isCastPagingLayoutLocked = true
                } else {
                    unlockCastPagingLayoutAfterDelay()
                }
            }
            .onChange(of: uiState.casts.count) { _ in
                guard uiState.selectedTab == .casts, isCastPagingLayoutLocked else { return }
                unlockCastPagingLayoutAfterDelay()
            }
        }
    }

    private func unlockCastPagingLayoutAfterDelay() {
        let generation = castPagingLayoutLockGeneration
        DispatchQueue.main.asyncAfter(deadline: .now() + cafeCastPagingLayoutLockDelay) {
            guard generation == castPagingLayoutLockGeneration else { return }
            isCastPagingLayoutLocked = false
        }
    }

    private var writeReviewButton: some View {
        Button {
            onAction(.writeReviewTapped)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                Text(String(localized: String.LocalizationValue("cafe_action_write_review"), table: "Localizable"))
                .font(.subheadline.weight(.bold))
            }
            .foregroundStyle(Color(hex: "2B2330"))
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .background(Color(hex: "FFD1DC"))
            .clipShape(Capsule())
            .shadow(color: Color(hex: "FFD1DC").opacity(0.45), radius: 12, x: 0, y: 6)
        }
        .buttonStyle(.plain)
    }

    private var offsetReader: some View {
        GeometryReader { proxy in
            Color.clear
            .id(topAnchorId)
            .preference(
                key: CafeScrollOffsetPreferenceKey.self,
                value: proxy.frame(in: .named("cafeScroll")).minY
            )
        }
        .frame(height: 0)
    }

    @ViewBuilder
    private func content(topSafeArea: CGFloat) -> some View {
        if let detail = uiState.detail {
            LazyVStack(spacing: 0) {
                heroSection(detail: detail, topSafeArea: topSafeArea)
                summarySection(detail: detail)
                tabHeader()
                    .overlay {
                        GeometryReader { proxy in
                            Color.clear.preference(
                                key: CafeTabHeaderOffsetPreferenceKey.self,
                                value: proxy.frame(in: .named("cafeScroll")).minY
                            )
                        }
                        .allowsHitTesting(false)
                }
                tabContent(detail: detail)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 20)
                }
        } else if uiState.isLoading {
            ProgressView()
            .frame(maxWidth: .infinity)
            .padding(.top, 160)
        } else {
            VStack(spacing: 12) {
                Text(String(localized: String.LocalizationValue("cafe_error_detail_load_failed"), table: "Localizable"))
                .foregroundStyle(.red)
                Button(String(localized: String.LocalizationValue("cafe_action_refresh"), table: "Localizable")) {
                    onAction(.refresh)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(hex: "EF6797"))
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 160)
        }
    }

    private func heroSection(detail: CafeDetail, topSafeArea: CGFloat) -> some View {
        let heroHeight = 230 + topSafeArea
        let pullDownOffset = scrollOffset > 0 ? scrollOffset : 0
        let dynamicHeroHeight = heroHeight + pullDownOffset
        let heroImages: [String] = {
            let normalized = detail.images
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
            if !normalized.isEmpty {
                return normalized
            }
            let fallback = detail.cafe.thumbnailImage?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            return fallback.isEmpty ? [""] : [fallback]
        }()
        return TabView {
            ForEach(Array(heroImages.enumerated()), id: \.offset) { _, image in
                ZStack {
                    let trimmed = image.trimmingCharacters(in: .whitespacesAndNewlines)

                    if let url = ImageUrlUtils.normalizedRemoteUrl(from: trimmed), !trimmed.isEmpty {
                        GeometryReader { geometry in
                            CachedAsyncImage(
                                url: url,
                                placeholder: heroPlaceholder,
                                displaySize: .medium
                            )
                            .frame(width: geometry.size.width, height: geometry.size.height)
                            .clipped()
                        }
                    } else {
                        heroPlaceholder
                    }
                }
                .frame(height: dynamicHeroHeight)
                .clipped()
            }
        }
        .frame(height: dynamicHeroHeight)
        .offset(y: pullDownOffset > 0 ? -pullDownOffset : 0)
        .frame(height: dynamicHeroHeight, alignment: .top)
        .clipShape(Rectangle())
        .tabViewStyle(.page(indexDisplayMode: .always))
    }

    private var heroPlaceholder: some View {
        LinearGradient(
            colors: [Color(hex: "FFD2E4"), Color(hex: "F7A6C5")],
            startPoint: .top,
            endPoint: .bottom
        )
        .overlay(
            Image(systemName: "cup.and.saucer.fill")
            .font(.system(size: 54))
            .foregroundStyle(Color.white.opacity(0.9))
        )
    }

    private func summarySection(detail: CafeDetail) -> some View {
        let conceptLabel = localizedCafeConceptType(detail.cafe.conceptType)
        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Text(detail.cafe.name)
                    .font(.title2.bold())
                if !detail.cafe.ownerIds.isEmpty {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundStyle(Color(hex: "2563EB"))
                        .font(.title2)
                }
            }
            if !conceptLabel.isEmpty {
                Text(conceptLabel)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(hex: "9E2E5C"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color(hex: "FDE7EF"), in: Capsule())
            }
            HStack(spacing: 14) {
                HStack(spacing: 4) {
                    Image(systemName: "star.fill")
                    .foregroundStyle(Color.yellow)
                    Text(RatingUtils.formatOneDecimal(detail.cafe.ratingAvg))
                    .fontWeight(.semibold)
                    Text("(\(detail.cafe.reviewCount))")
                    .foregroundStyle(.secondary)
                }
                HStack(spacing: 4) {
                    Image(systemName: "mappin.and.ellipse")
                    .foregroundStyle(.secondary)
                    Text(detail.cafe.region.city)
                    .foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 18)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
    }

    private func localizedCafeConceptType(_ rawConceptType: String) -> String {
        let normalized = rawConceptType.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else {
            return ""
        }
        switch normalized.uppercased() {
        case "MAID":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_maid"), table: "Localizable")
        case "BUTLER":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_butler"), table: "Localizable")
        case "IDOL":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_idol"), table: "Localizable")
        case "DEVIL":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_devil"), table: "Localizable")
        case "DOLL":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_doll"), table: "Localizable")
        case "COSPLAY":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_cosplay"), table: "Localizable")
        case "NAMJANG":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_namjang"), table: "Localizable")
        case "YOKAI":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_yokai"), table: "Localizable")
        case "CAT":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_cat"), table: "Localizable")
        case "OTHER":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_other"), table: "Localizable")
        default:
            return normalized
        }
    }

    private func tabHeader() -> some View {
        ScrollableConCafeTabBar(
            labels: CafeUiState.TabType.allCases.map { tab in
                if tab == .info {
                    return String(localized: String.LocalizationValue("cafe_tab_info"), table: "Localizable")
                } else if tab == .casts {
                    return String(localized: String.LocalizationValue("cafe_tab_casts"), table: "Localizable")
                } else if tab == .menu {
                    return String(localized: String.LocalizationValue("cafe_tab_menu"), table: "Localizable")
                } else if tab == .reviews {
                    return String(localized: String.LocalizationValue("cafe_tab_reviews"), table: "Localizable")
                } else {
                    let noticeLabel = String(localized: String.LocalizationValue("cafe_tab_notices"), table: "Localizable")
                    let eventLabel = String(localized: String.LocalizationValue("noticeevent_tab_event"), table: "Localizable")
                    return "\(noticeLabel)/\(eventLabel)"
                }
            },
            selectedIndex: CafeUiState.TabType.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
            backgroundColor: Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }),
            onSelect: { index in
                onAction(.changeTab(CafeUiState.TabType.allCases[index]))
            }
        )
        .frame(maxWidth: .infinity)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .zIndex(1)
    }

    private func pinnedTabHeader() -> some View {
        VStack(spacing: 0) {
            tabHeader()
        }
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
    }

    private func pinnedTabTopPadding(in proxy: GeometryProxy) -> CGFloat {
        max(0, proxy.safeAreaInsets.top - proxy.frame(in: .global).minY)
    }

    @ViewBuilder
    private func tabContent(detail: CafeDetail) -> some View {
        switch uiState.selectedTab {
        case .info:
            CafeInfoView(cafeDetail: detail)
        case .casts:
            CafeCastView(
                maids: uiState.casts,
                canLoadMore: uiState.canLoadMoreCasts,
                isLoadingMore: uiState.isLoadingMoreCasts,
                onPagingTriggerDisappear: {
                    onAction(.pagingTriggerDisappeared(.casts))
                },
                onAction: onAction
            )
        case .menu:
            CafeMenuView(
                menus: detail.menus,
                goods: detail.goods,
                isLoading: uiState.isLoadingMenuGoods
            )
        case .reviews:
            CafeReviewView(
                detail: detail,
                reviews: uiState.reviews,
                canLoadMore: uiState.canLoadMoreReviews,
                isLoadingMore: uiState.isLoadingMoreReviews,
                currentUserId: uiState.currentUserId,
                onLoadMore: { onAction(.loadMoreReviews) },
                onPagingTriggerDisappear: {
                    onAction(.pagingTriggerDisappeared(.reviews))
                },
                onAction: onAction
            )
        case .notices:
            CafeNoticeView(
                events: uiState.events,
                notices: uiState.notices,
                canLoadMore: uiState.canLoadMoreNotices,
                isLoadingMore: uiState.isLoadingMoreNotices,
                onLoadMore: { onAction(.loadMoreNotices) },
                onPagingTriggerDisappear: {
                    onAction(.pagingTriggerDisappeared(.notices))
                },
                onEventTap: { onAction(.eventTapped(eventId: $0)) }
            )
        }
    }

}

private struct CafeScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct CafeTabHeaderOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct CafeView_Previews: PreviewProvider {
    static var previews: some View {
        CafeView(
            cafeId: "cafe-1",
            onNavigationAction: { _ in }
        )
    }
}

struct DetailTooltipBubble: View {
    let text: String

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
            DetailTooltipTriangle()
                .fill(Color(uiColor: .label))
                .frame(width: 14, height: 8)
                .padding(.trailing, 18)
            Text(text)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color(uiColor: .systemBackground))
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .frame(maxWidth: 280, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color(uiColor: .label))
                )
                .shadow(color: .black.opacity(0.18), radius: 8, x: 0, y: 4)
        }
    }
}

private struct DetailTooltipTriangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}
