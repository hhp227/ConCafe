//
//  ExploreView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared
import UIKit

struct ExploreView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = ExploreViewModel()

    var body: some View {
        ExploreContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle("탐색")
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToCast(let id):
                onNavigationAction(.navigateToCast(id: id))
            case .navigateToCafe(let id):
                onNavigationAction(.navigateToCafe(id: id))
            }
        }
    }
}

private struct ExploreContentView: View {
    @FocusState private var isSearchFocused: Bool
    
    let uiState: ExploreUiState
    
    let onAction: (ExploreAction) -> Void
    
    private var cafeNameById: [String: String] {
        Dictionary(uniqueKeysWithValues: uiState.cafes.map { ($0.id, $0.name) })
    }
    
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12, pinnedViews: [.sectionHeaders]) {
                searchSection
                Section {
                    gridContent
                        .padding(.horizontal, 12)
                } header: {
                    tabHeader
                }
            }
            .padding(.vertical, 12)
        }
        .background(ScrollViewKeyboardDismissConfigurator())
        .background(Color(hex: "FFF9FC"))
        .modifier(ExploreKeyboardDismissModifier())
    }
    
    private var searchSection: some View {
        VStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)
                TextField("카페나 메이드를 검색하세요...", text: Binding(
                    get: { uiState.query },
                    set: { onAction(.queryChanged($0)) }
                ))
                .focused($isSearchFocused)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(
                        isSearchFocused ? Color(hex: "EF6797") : .clear,
                        lineWidth: isSearchFocused ? 1 : 0
                    )
            )
            HStack(spacing: 8) {
                Menu {
                    ForEach(ExploreUiState.RegionFilter.allCases, id: \.self) { region in
                        Button(region.label) {
                            onAction(.regionChanged(region))
                        }
                    }
                } label: {
                    CapsuleDropdownLabel(text: uiState.selectedRegion.label)
                }
                Menu {
                    ForEach(ExploreUiState.SortFilter.allCases, id: \.self) { sort in
                        Button(sort.label) {
                            onAction(.sortChanged(sort))
                        }
                    }
                } label: {
                    CapsuleDropdownLabel(text: uiState.selectedSort.label)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 12)
    }

    private var tabHeader: some View {
        ConCafeTabBar(
            labels: ExploreUiState.TabType.allCases.map { $0.rawValue },
            selectedIndex: ExploreUiState.TabType.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
            backgroundColor: Color(hex: "FFF9FC"),
            onSelect: { index in
                onAction(.tabChanged(ExploreUiState.TabType.allCases[index]))
            }
        )
        .zIndex(1)
    }

    @ViewBuilder
    private var gridContent: some View {
        if uiState.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
        } else if uiState.errorMessage != nil {
            Text("탐색 데이터를 불러오지 못했습니다.")
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
        } else {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                if uiState.selectedTab == .cafe {
                    ForEach(uiState.cafes, id: \.id) { cafe in
                        cafeCard(cafe)
                    }
                } else {
                    ForEach(uiState.maids, id: \.id) { maid in
                        maidCard(maid)
                    }
                }
            }
        }
    }

    private func cafeCard(_ cafe: Cafe) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack {
                if let urlString = cafe.thumbnailImage,
                   let url = URL(string: urlString) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            placeholderCafeImage
                        case .success(let image):
                            image.resizable().scaledToFill()
                        case .failure:
                            placeholderCafeImage
                        @unknown default:
                            placeholderCafeImage
                        }
                    }
                } else {
                    placeholderCafeImage
                }
            }
            .frame(height: 120)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text(cafe.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text("⭐ \(String(format: "%.1f", cafe.ratingAvg))")
                    .font(.caption)
                Text("📍 \(cafe.region.city)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 4)
        }
        .onTapGesture {
            onAction(.cafeTapped(id: cafe.id))
        }
    }

    private var placeholderCafeImage: some View {
        LinearGradient(
            colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")],
            startPoint: .top,
            endPoint: .bottom
        )
        .overlay(Image(systemName: "building.2.fill").foregroundStyle(Color.white.opacity(0.85)))
    }

    private func maidCard(_ maid: Cast) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack {
                if let urlString = maid.profileImage,
                   let url = URL(string: urlString) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            placeholderMaidImage
                        case .success(let image):
                            image.resizable().scaledToFill()
                        case .failure:
                            placeholderMaidImage
                        @unknown default:
                            placeholderMaidImage
                        }
                    }
                } else {
                    placeholderMaidImage
                }
            }
            .frame(height: 120)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text(maid.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text(cafeNameById[maid.cafeId] ?? maid.cafeId)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Text("👥 \(maid.followerCount)")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            .padding(.horizontal, 4)
        }
        .onTapGesture {
            onAction(.maidTapped(id: maid.id))
        }
    }

    private var placeholderMaidImage: some View {
        LinearGradient(
            colors: [Color(hex: "FFDFEA"), Color(hex: "FFBED5")],
            startPoint: .top,
            endPoint: .bottom
        )
        .overlay(Image(systemName: "person.fill").foregroundStyle(Color.white.opacity(0.85)))
    }
}

private struct ExploreKeyboardDismissModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.scrollDismissesKeyboard(.immediately)
        } else {
            content
        }
    }
}

private struct ScrollViewKeyboardDismissConfigurator: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)

        DispatchQueue.main.async {
            updateKeyboardDismissMode(from: view)
        }

        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            updateKeyboardDismissMode(from: uiView)
        }
    }

    private func updateKeyboardDismissMode(from view: UIView) {
        var currentView = view.superview

        while let currentView {
            if let scrollView = currentView as? UIScrollView {
                scrollView.keyboardDismissMode = .onDrag
                break
            } else {
                currentView = currentView.superview
            }
        }
    }
}

struct ExploreView_Previews: PreviewProvider {
    static var previews: some View {
        ExploreContentView(uiState: .empty, onAction: { _ in })
    }
}
