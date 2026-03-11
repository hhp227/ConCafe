//
//  CastEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI

struct CastEditView: View {
    let cafeId: String?

    let castId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CastEditViewModel

    var body: some View {
        CastEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(viewModel.uiState.screenTitle)
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
    }

    init(
        cafeId: String? = nil,
        castId: String? = nil,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.cafeId = cafeId
        self.castId = castId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CastEditViewModel(cafeId: cafeId, castId: castId))
    }
}

private struct CastEditContentView: View {
    let uiState: CastEditUiState

    let onAction: (CastEditAction) -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                VStack {
                    Spacer()
                    ProgressView()
                        .tint(Color(hex: "EF6797"))
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 20) {
                        profilePhotoSection
                        if let infoMessage = uiState.infoMessage {
                            infoBanner(message: infoMessage)
                        }
                        ConCafeFormField(
                            label: "캐스트 이름",
                            text: Binding(
                                get: { uiState.castName },
                                set: { onAction(.changeCastName($0)) }
                            )
                        )
                        ConCafeFormField(
                            label: "컨셉 역할",
                            text: Binding(
                                get: { uiState.conceptRole },
                                set: { onAction(.changeConceptRole($0)) }
                            )
                        )
                        ConCafeFormField(
                            label: "생일",
                            text: Binding(
                                get: { uiState.birthday },
                                set: { onAction(.changeBirthday($0)) }
                            ),
                            trailingContent: {
                                Image(systemName: "calendar")
                                    .foregroundStyle(Color(hex: "B1A3AC"))
                            }
                        )
                        ConCafeFormEditor(
                            label: "소개 및 바이오",
                            text: Binding(
                                get: { uiState.introduction },
                                set: { onAction(.changeIntroduction($0)) }
                            )
                        )
                        workingDaysSection
                        gallerySection
                    }
                    .padding(16)
                    .padding(.bottom, 24)
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            bottomSaveBar
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "F8F5F6"), Color(hex: "FFFBFD")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .background(Color(hex: "F8F5F6"))
    }

    private var profilePhotoSection: some View {
        VStack(spacing: 12) {
            Button {
                onAction(.clickProfilePhoto)
            } label: {
                ZStack(alignment: .bottomTrailing) {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "FFE3EC"), Color(hex: "F8C5D7")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 128, height: 128)
                    Circle()
                        .fill(Color(hex: "FFD1DC"))
                        .frame(width: 34, height: 34)
                        .overlay {
                            Image(systemName: "camera.fill")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Color(hex: "2B2330"))
                        }
                        .overlay(
                            Circle()
                                .stroke(Color.white, lineWidth: 2)
                        )
                }
            }
            .buttonStyle(.plain)
            Text("캐스트 프로필 사진")
                .font(.title3.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text("탭해서 사진을 변경하세요")
                .font(.caption)
                .foregroundStyle(Color(hex: "8C7E87"))
        }
        .frame(maxWidth: .infinity)
    }

    private var workingDaysSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("근무 요일")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            FlexibleChipLayout(
                items: CastEditUiState.WorkingDay.allCases,
                spacing: 8
            ) { day in
                let isSelected = uiState.selectedWorkingDays.contains(day)
                Button {
                    onAction(.toggleWorkingDay(day))
                } label: {
                    Text(day.shortLabel)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(isSelected ? Color(hex: "2B2330") : Color(hex: "6E6169"))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(isSelected ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.08))
                        .clipShape(Capsule())
                        .overlay(
                            Capsule()
                                .stroke(
                                    isSelected ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.3),
                                    lineWidth: 1
                                )
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var gallerySection: some View {
        let visibleItems = Array(uiState.galleryItems.prefix(3))
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("갤러리 사진")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color(hex: "665A63"))
                Spacer()
                Text("사진 추가")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            GeometryReader { proxy in
                let itemSize = (proxy.size.width - 36) / 4

                HStack(spacing: 12) {
                    Button {
                        onAction(.clickAddGalleryPhoto)
                    } label: {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(Color(hex: "FFD1DC").opacity(0.1))
                            .frame(width: itemSize, height: itemSize)
                            .overlay {
                                Image(systemName: "camera.badge.plus")
                                    .foregroundStyle(Color(hex: "EF6797"))
                            }
                    }
                    .buttonStyle(.plain)
                    ForEach(visibleItems) { item in
                        ZStack {
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(
                                    LinearGradient(
                                        colors: [Color(hex: "FFE6EE"), Color(hex: "F7C9D8")],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .frame(width: itemSize, height: itemSize)
                            Text(item.overlayCount.map { "+\($0)" } ?? item.label)
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Color(hex: "5E4C57"))
                        }
                    }
                }
            }
            .frame(height: max(0, UIScreen.main.bounds.width - 68) / 4)
        }
    }

    private var bottomSaveBar: some View {
        Button {
            onAction(.clickSave)
        } label: {
            HStack(spacing: 8) {
                if uiState.isSaving {
                    ProgressView()
                        .tint(Color(hex: "2B2330"))
                } else {
                    Image(systemName: "person.crop.circle.badge.checkmark")
                }
                Text(uiState.saveButtonLabel)
                    .fontWeight(.bold)
            }
            .foregroundStyle(Color(hex: "2B2330"))
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(Color(hex: "FFD1DC"))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
        .padding(16)
        .background(Color.white.opacity(0.92))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color(hex: "FFD1DC").opacity(0.2))
                .frame(height: 1)
        }
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("닫기") {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "6B5320"))
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
}

private struct FlexibleChipLayout<Item: Identifiable & Hashable, Content: View>: View {
    let items: [Item]

    let spacing: CGFloat

    @ViewBuilder let content: (Item) -> Content

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 64), spacing: spacing, alignment: .leading)],
            alignment: .leading,
            spacing: spacing
        ) {
            ForEach(items, id: \.self) { item in
                content(item)
            }
        }
    }
}

struct CastEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            CastEditView(castId: nil, onNavigationAction: { _ in })
        }
    }
}
