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
        .navigationBarBackButtonHidden(true)
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
            
            if let detail = uiState.detail {
                if isCollapsedTopBar {
                    collapsedTopBar(detail: detail)
                } else {
                    floatingTopButtons
                }
            }
        }
        .background(Color(hex: "FFF9FC"))
        .onPreferenceChange(CafeScrollOffsetPreferenceKey.self) { value in
            scrollOffset = value
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
    
    private var isCollapsedTopBar: Bool {
        scrollOffset < -160
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
    }
    
    @ViewBuilder
    private func tabContent(detail: CafeDetail) -> some View {
        switch uiState.selectedTab {
        case .info:
            VStack(spacing: 14) {
                infoCard(detail: detail)
                descriptionCard(detail: detail)
            }
        case .maids:
            maidGrid(uiState.casts)
        case .menu:
            menuList(detail.menus)
        case .reviews:
            reviewList(detail: detail, reviews: uiState.reviews)
        case .notices:
            noticeList(detail.notices)
        }
    }
    
    private func infoCard(detail: CafeDetail) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            infoRow(icon: "mappin.and.ellipse", title: "주소", value: detail.cafe.region.address)
            infoRow(icon: "clock.fill", title: "영업시간", value: detail.businessHours)
            infoRow(icon: "phone.fill", title: "전화번호", value: detail.phoneNumber)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
    
    private func infoRow(icon: String, title: String, value: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(Color(hex: "EF6797"))
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                Text(value)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }
    
    private func descriptionCard(detail: CafeDetail) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("소개")
                .font(.subheadline.weight(.semibold))
            Text(detail.cafe.description)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "666666"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
    
    @ViewBuilder
    private func maidGrid(_ maids: [CafeDetailCast]) -> some View {
        if maids.isEmpty {
            emptyCard("등록된 메이드가 없습니다.")
        } else {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(maids, id: \.cast.id) { maid in
                    VStack(alignment: .leading, spacing: 0) {
                        LinearGradient(
                            colors: [Color(hex: "FFDFEA"), Color(hex: "FFBED5")],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: 160)
                        .overlay(alignment: .topTrailing) {
                            HStack(spacing: 6) {
                                if maid.isWorking {
                                    Text("출근중")
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.green)
                                        .clipShape(Capsule())
                                }
                                Text(maid.cast.conceptRole.uppercased())
                                    .font(.caption2.weight(.bold))
                                    .foregroundStyle(Color.white.opacity(0.9))
                            }
                            .padding(12)
                        }
                        VStack(alignment: .leading, spacing: 4) {
                            Text(maid.cast.name)
                                .font(.subheadline.weight(.semibold))
                            Text(maid.cast.description)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                        }
                        .padding(12)
                    }
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                    .onTapGesture {
                        onAction(.maidTapped(id: maid.cast.id))
                    }
                }
            }
        }
    }
    
    @ViewBuilder
    private func menuList(_ menus: [CafeMenu]) -> some View {
        if menus.isEmpty {
            emptyCard("등록된 메뉴가 없습니다.")
        } else {
            VStack(spacing: 12) {
                ForEach(menus, id: \.id) { menu in
                    HStack(spacing: 12) {
                        LinearGradient(
                            colors: menu.image == nil ? [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")] : [Color(hex: "FFD8E8"), Color(hex: "F5AFCC")],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(width: 84, height: 84)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        VStack(alignment: .leading, spacing: 6) {
                            Text(menu.name)
                                .font(.subheadline.weight(.semibold))
                            Text("\(menu.price)원")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Color(hex: "EF6797"))
                            Text(menu.description)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(12)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                }
            }
        }
    }
    
    @ViewBuilder
    private func reviewList(detail: CafeDetail, reviews: [CafeDetailReview]) -> some View {
        VStack(spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: "star.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(Color.yellow)
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(format: "%.1f", detail.cafe.ratingAvg))
                        .font(.title2.bold())
                    Text("\(detail.cafe.reviewCount)개 리뷰")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            
            if reviews.isEmpty {
                emptyCard("아직 등록된 리뷰가 없습니다.")
            } else {
                ForEach(reviews, id: \.id) { review in
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            HStack(spacing: 8) {
                                Text(review.userNickname)
                                    .font(.subheadline.weight(.semibold))
                                if review.verified {
                                    Text("방문인증")
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color(hex: "EF6797"))
                                        .clipShape(Capsule())
                                }
                            }
                            Spacer()
                            Text(review.createdDate)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        HStack(spacing: 2) {
                            ForEach(0..<5, id: \.self) { index in
                                Image(systemName: "star.fill")
                                    .font(.caption)
                                    .foregroundStyle(index < Int(review.rating) ? Color.yellow : Color(hex: "E1E1E1"))
                            }
                        }
                        Text(review.content)
                            .font(.subheadline)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                }
            }
        }
    }
    
    @ViewBuilder
    private func noticeList(_ notices: [Notice]) -> some View {
        if notices.isEmpty {
            emptyCard("등록된 공지가 없습니다.")
        } else {
            VStack(spacing: 12) {
                ForEach(notices, id: \.id) { notice in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text(notice.title)
                                .font(.subheadline.weight(.semibold))
                            Spacer()
                            Text(String(notice.createdAt.prefix(10)))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text(notice.content)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "666666"))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
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
    
    private func collapsedTopBar(detail: CafeDetail) -> some View {
        HStack(spacing: 12) {
            Button(action: { onAction(.backTapped) }) {
                Image(systemName: "arrow.left")
                    .font(.headline)
                    .foregroundStyle(Color(hex: "333333"))
                    .frame(width: 36, height: 36)
            }
            Text(detail.cafe.name)
                .font(.headline.weight(.bold))
                .lineLimit(1)
            Spacer()
            Button {
                onAction(.favoriteTapped)
            } label: {
                Image(systemName: uiState.isFavorite ? "heart.fill" : "heart")
                    .font(.headline)
                    .foregroundStyle(uiState.isFavorite ? Color(hex: "EF6797") : Color(hex: "333333"))
                    .frame(width: 36, height: 36)
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 8)
        .padding(.bottom, 10)
        .background(Color.white)
    }
    
    private var floatingTopButtons: some View {
        HStack {
            Button(action: { onAction(.backTapped) }) {
                Image(systemName: "arrow.left")
                    .font(.headline)
                    .foregroundStyle(Color(hex: "333333"))
                    .frame(width: 40, height: 40)
                    .background(Color.white.opacity(0.92))
                    .clipShape(Circle())
            }
            Spacer()
            Button {
                onAction(.favoriteTapped)
            } label: {
                Image(systemName: uiState.isFavorite ? "heart.fill" : "heart")
                    .font(.headline)
                    .foregroundStyle(uiState.isFavorite ? Color(hex: "EF6797") : Color(hex: "333333"))
                    .frame(width: 40, height: 40)
                    .background(Color.white.opacity(0.92))
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 12)
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
