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

    var body: some View {
        CafeContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToCast(let id):
                onNavigationAction(.navigateToCast(id: id))
            case .navigateToSignIn:
                onNavigationAction(.navigateToSignIn)
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

private struct CafeContentView: View {
    let uiState: CafeUiState
    
    let onAction: (CafeAction) -> Void

    @State private var scrollOffset: CGFloat = 0
    
    var body: some View {
        ZStack(alignment: .top) {
            ScrollView {
                offsetReader
                content
            }
            .coordinateSpace(name: "cafeScroll")
            .background(Color(hex: "FFF9FC"))
            .ignoresSafeArea(edges: .top)
        }
        .background(Color(hex: "FFF9FC"))
        .onPreferenceChange(CafeScrollOffsetPreferenceKey.self) { value in
            scrollOffset = value
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
    }
    
    private var offsetReader: some View {
        GeometryReader { proxy in
            Color.clear
                .preference(
                    key: CafeScrollOffsetPreferenceKey.self,
                    value: proxy.frame(in: .named("cafeScroll")).minY
                )
        }
        .frame(height: 0)
    }
    
    @ViewBuilder
    private var content: some View {
        if let detail = uiState.detail {
            LazyVStack(spacing: 0, pinnedViews: [.sectionHeaders]) {
                heroSection(detail: detail)
                summarySection(detail: detail)
                Section {
                    tabContent(detail: detail)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 20)
                } header: {
                    tabHeader
                }
            }
        } else if uiState.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.top, 160)
        } else {
            VStack(spacing: 12) {
                Text(uiState.errorMessage ?? "카페 상세 데이터를 불러오지 못했습니다.")
                    .foregroundStyle(.red)
                Button("새로고침") {
                    onAction(.refresh)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(hex: "EF6797"))
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 160)
        }
    }
    
    private func heroSection(detail: CafeDetail) -> some View {
        TabView {
            ForEach(Array(detail.images.enumerated()), id: \.offset) { _, image in
                ZStack {
                    if let url = URL(string: image), !image.isEmpty {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .empty:
                                heroPlaceholder
                            case .success(let loadedImage):
                                loadedImage
                                    .resizable()
                                    .scaledToFill()
                            case .failure:
                                heroPlaceholder
                            @unknown default:
                                heroPlaceholder
                            }
                        }
                    } else {
                        heroPlaceholder
                    }
                }
                .frame(height: 280)
                .clipped()
            }
        }
        .frame(height: 280)
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
        VStack(alignment: .leading, spacing: 10) {
            Text(detail.cafe.name)
                .font(.title2.bold())
            HStack(spacing: 14) {
                HStack(spacing: 4) {
                    Image(systemName: "star.fill")
                        .foregroundStyle(Color.yellow)
                    Text(String(format: "%.1f", detail.cafe.ratingAvg))
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
        .background(Color.white)
    }
    
    private var tabHeader: some View {
        ScrollableConCafeTabBar(
            labels: CafeUiState.TabType.allCases.map { $0.rawValue },
            selectedIndex: CafeUiState.TabType.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
            backgroundColor: .white,
            onSelect: { index in
                onAction(.changeTab(CafeUiState.TabType.allCases[index]))
            }
        )
        .frame(maxWidth: .infinity)
        .background(Color.white)
        .zIndex(1)
    }
    
    @ViewBuilder
    private func tabContent(detail: CafeDetail) -> some View {
        switch uiState.selectedTab {
        case .info:
            CafeInfoView(cafeDetail: detail)
        case .maids:
            CafeCastView(maids: uiState.casts, onAction: onAction)
        case .menu:
            CafeMenuView(menus: detail.menus)
        case .reviews:
            CafeReviewView(detail: detail, reviews: uiState.reviews)
        case .notices:
            CafeNoticeView(notices: detail.notices)
        }
    }
}

private struct CafeScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    
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
