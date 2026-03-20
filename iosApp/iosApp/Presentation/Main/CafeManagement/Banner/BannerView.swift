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
        .navigationTitle(viewModel.uiState.screenTitle)
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
            "배너 삭제",
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
            Button("삭제", role: .destructive) {
                viewModel.onAction(.confirmDeleteBanner)
            }
            Button("취소", role: .cancel) {
                viewModel.onAction(.dismissDeleteBannerDialog)
            }
        } message: {
            Text("'\(viewModel.uiState.pendingDeleteBanner?.title ?? "")' 배너를 삭제하시겠습니까?")
        }
        .alert(
            "안내",
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
            Button("확인", role: .cancel) {
                alertMessage = nil
            }
        } message: { message in
            Text(message)
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
                labels: BannerTab.allCases.map(\.rawValue),
                selectedIndex: BannerTab.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
                backgroundColor: Color(hex: "F8F5F6"),
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
                    Text("최대 5개의 배너를 동시에 노출할 수 있습니다.")
                        .font(.caption)
                        .foregroundStyle(Color(hex: "9A8E97"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 16)
            }
        }
        .background(Color(hex: "F8F5F6"))
        .safeAreaInset(edge: .bottom) {
            Button {
                onAction(.createBannerTapped)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "plus.circle.fill")
                    Text("새 배너 등록")
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .foregroundStyle(Color(hex: "24161E"))
                .background(Color(hex: "FFD1DC"))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 14)
            .background(Color(hex: "F8F5F6"))
        }
    }

    private var headerRow: some View {
        HStack {
            Text(uiState.sectionCountLabel)
                .font(.caption.weight(.bold))
                .foregroundStyle(Color(hex: "7A707A"))
            Spacer()
            Text(uiState.locationLabel)
                .font(.caption.weight(.bold))
                .foregroundStyle(Color(hex: "EF6797"))
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
                    Text(banner.statusLabel)
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(Color(hex: "CE5E87"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color(hex: "FFD1DC").opacity(0.28))
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
                        .foregroundStyle(Color(hex: "24161E"))
                    Text(banner.description)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "7A707A"))
                }
                HStack(spacing: 6) {
                    Image(systemName: "calendar")
                        .font(.caption2)
                    Text(banner.periodText)
                        .font(.caption2)
                }
                .foregroundStyle(Color(hex: "9A8E97"))
            }
        }
        .padding(16)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(hex: "FFD1DC").opacity(0.16), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.03), radius: 8, x: 0, y: 4)
    }

    private var thumbnail: some View {
        LinearGradient(
            colors: [Color(hex: banner.accentHex), Color(hex: "FFE6ED")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .overlay(
            Image(systemName: banner.imageIcon)
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(.white)
        )
        .frame(width: 96, height: 96)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct IconCircleButton: View {
    let systemName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.caption.weight(.bold))
                .foregroundStyle(Color(hex: "8F848F"))
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
