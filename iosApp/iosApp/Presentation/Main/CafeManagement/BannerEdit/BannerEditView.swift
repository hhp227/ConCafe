//
//  BannerEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import SwiftUI

struct BannerEditView: View {
    let initialCafeId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: BannerEditViewModel

    var body: some View {
        BannerEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle(viewModel.uiState.screenTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("저장") {
                    viewModel.onAction(.clickSave)
                }
                .font(.system(size: 16, weight: .bold))
                .disabled(!viewModel.uiState.isSaveEnabled)
            }
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.selectorType != nil },
                set: { isPresented in
                    if !isPresented {
                        viewModel.onAction(.dismissSelector)
                    }
                }
            )
        ) {
            BannerSelectorSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
            .compatLargeSheetDetent()
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showSaveSuccessAlert:
                break
            }
        }
    }

    init(
        initialCafeId: String? = nil,
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.initialCafeId = initialCafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: BannerEditViewModel(initialCafeId: initialCafeId))
    }
}

private struct BannerEditContentView: View {
    let uiState: BannerEditUiState

    let onAction: (BannerEditAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                bannerImageCard
                basicInformationSection
                targetSection
                periodSection
                if let infoMessage = uiState.infoMessage {
                    infoBanner(message: infoMessage)
                }
            }
            .padding(16)
            .padding(.bottom, 100)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            bottomSaveBar
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "F8F5F6"), Color(hex: "FFFBFD")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private var bannerImageCard: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color(hex: "EF6797").opacity(0.08))
                    .frame(width: 72, height: 72)
                Image(systemName: "photo.badge.plus")
                    .font(.system(size: 30, weight: .semibold))
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            VStack(spacing: 4) {
                Text(uiState.imageSectionTitle)
                    .font(.title3.weight(.bold))
                    .multilineTextAlignment(.center)
                Text(uiState.imageGuideText)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "8F848F"))
                if let selectedImageLabel = uiState.selectedImageLabel {
                    Text(selectedImageLabel)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color(hex: "EF6797"))
                        .padding(.top, 4)
                }
            }
            Button {
                onAction(.clickImagePicker)
            } label: {
                Text(uiState.imageButtonText)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color(hex: "2B2330"))
                    .padding(.horizontal, 22)
                    .padding(.vertical, 10)
                    .background(Color(hex: "FFD1DC"))
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 20)
        .padding(.vertical, 24)
        .background(Color.white.opacity(0.72))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(style: StrokeStyle(lineWidth: 2, dash: [8, 6]))
                .foregroundStyle(Color(hex: "FFD1DC").opacity(0.45))
        )
        .onTapGesture {
            onAction(.clickImagePicker)
        }
    }

    private var basicInformationSection: some View {
        sectionCard(title: "배너 기본 정보") {
            ConCafeFormField(
                label: "배너 제목",
                text: Binding(get: { uiState.title }, set: { onAction(.changeTitle($0)) }),
                placeholder: "배너 제목을 입력해주세요"
            )
            ConCafeFormField(
                label: "서브 문구",
                text: Binding(get: { uiState.subtitle }, set: { onAction(.changeSubtitle($0)) }),
                placeholder: "서브 문구를 입력해주세요"
            )
        }
    }

    private var targetSection: some View {
        sectionCard(title: "연결 대상 설정 (Target)") {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 2), spacing: 8) {
                ForEach(BannerTargetType.allCases) { target in
                    Button {
                        onAction(.selectTarget(target))
                    } label: {
                        Text(target.rawValue)
                            .font(.subheadline.weight(uiState.selectedTarget == target ? .bold : .medium))
                            .foregroundStyle(uiState.selectedTarget == target ? Color(hex: "23161C") : Color(hex: "7A707A"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(uiState.selectedTarget == target ? Color(hex: "FFD1DC").opacity(0.12) : Color(hex: "F8F5F6"))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .stroke(uiState.selectedTarget == target ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.2), lineWidth: uiState.selectedTarget == target ? 2 : 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            switch uiState.selectedTarget {
            case .externalLink:
                ConCafeFormField(
                    label: "외부 링크",
                    text: Binding(get: { uiState.targetValue }, set: { onAction(.changeTargetValue($0)) }),
                    placeholder: uiState.targetFieldPlaceholder
                )
            case .cafeDetail:
                if uiState.isAdmin {
                    selectionField(
                        label: "운영 카페",
                        selectedItem: uiState.selectedCafeOption,
                        placeholder: "운영 카페를 선택해주세요"
                    ) {
                        onAction(.clickCafeSelector)
                    }
                } else {
                    fixedSelectionField(
                        label: "적용 카페",
                        selectedItem: uiState.selectedCafeOption,
                        placeholder: "연결할 운영 카페가 없습니다."
                    )
                }
            case .notice, .eventDetail:
                if uiState.isAdmin {
                    selectionField(
                        label: "운영 카페",
                        selectedItem: uiState.selectedCafeOption,
                        placeholder: "운영 카페를 선택해주세요"
                    ) {
                        onAction(.clickCafeSelector)
                    }
                } else {
                    fixedSelectionField(
                        label: "적용 카페",
                        selectedItem: uiState.selectedCafeOption,
                        placeholder: "연결할 운영 카페가 없습니다."
                    )
                }
                selectionField(
                    label: uiState.targetSelectionLabel,
                    selectedItem: uiState.selectedContentOption,
                    placeholder: uiState.targetSelectionPlaceholder
                ) {
                    onAction(.clickTargetSelector)
                }
            }
        }
    }

    private var periodSection: some View {
        sectionCard(title: "노출 기간 설정") {
            VStack(spacing: 8) {
                badgeText(uiState.displayDaysLabel)
                Slider(
                    value: Binding(
                        get: { Double(uiState.displayDays) },
                        set: { onAction(.changeDisplayDays(Int($0.rounded()))) }
                    ),
                    in: 1...10,
                    step: 1
                )
                .tint(Color(hex: "EF6797"))
                HStack {
                    Text("1일")
                    Spacer()
                    Text("10일")
                }
                .font(.caption2)
                .foregroundStyle(Color(hex: "9A8D95"))
            }
        }
    }

    private var bottomSaveBar: some View {
        Button {
            onAction(.clickSave)
        } label: {
            HStack(spacing: 8) {
                if uiState.isSaving {
                    ProgressView().tint(Color(hex: "2B2330"))
                } else {
                    Image(systemName: "square.and.arrow.down")
                }
                Text(uiState.submitButtonText).fontWeight(.bold)
            }
            .foregroundStyle(Color(hex: "2B2330"))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(uiState.isSaveEnabled ? Color(hex: "FFD1DC") : Color(hex: "F4D7DF"))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 16)
            .background(Color.white.opacity(0.94))
        }
        .buttonStyle(.plain)
        .disabled(!uiState.isSaveEnabled)
    }

    private func sectionCard<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                RoundedRectangle(cornerRadius: 999, style: .continuous)
                    .fill(Color(hex: "FFD1DC"))
                    .frame(width: 4, height: 18)
                Text(title).font(.title3.weight(.bold))
            }
            content()
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func selectionField(
        label: String,
        selectedItem: BannerSelectableItem?,
        placeholder: String,
        onTap: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            Button(action: onTap) {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(selectedItem?.title ?? placeholder)
                            .font(.subheadline.weight(selectedItem == nil ? .regular : .semibold))
                            .foregroundStyle(selectedItem == nil ? Color(hex: "AA98A4") : Color(hex: "23161C"))
                        if let subtitle = selectedItem?.subtitle, !subtitle.isEmpty {
                            Text(subtitle)
                                .font(.caption)
                                .foregroundStyle(Color(hex: "8F848F"))
                        }
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(Color(hex: "8F848F"))
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .background(Color(hex: "F8F5F6"))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(Color(hex: "FFD1DC").opacity(0.2), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
    }

    private func fixedSelectionField(
        label: String,
        selectedItem: BannerSelectableItem?,
        placeholder: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            VStack(alignment: .leading, spacing: 4) {
                Text(selectedItem?.title ?? placeholder)
                    .font(.subheadline.weight(selectedItem == nil ? .regular : .semibold))
                    .foregroundStyle(selectedItem == nil ? Color(hex: "AA98A4") : Color(hex: "23161C"))
                if let subtitle = selectedItem?.subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "8F848F"))
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color(hex: "F8F5F6"))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.2), lineWidth: 1)
            )
        }
    }

    private func badgeText(_ text: String) -> some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "EF6797"))
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color(hex: "EF6797").opacity(0.08))
            .clipShape(Capsule())
    }

    private func infoBanner(message: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "info.circle.fill")
                .foregroundStyle(Color(hex: "EF6797"))
                .padding(.top, 1)
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "7A707A"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("닫기") {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "6B5320"))
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(hex: "FFD1DC").opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct BannerSelectorSheet: View {
    let uiState: BannerEditUiState

    let onAction: (BannerEditAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(uiState.selectorTitle)
                .font(.title3.weight(.bold))
                .padding(.horizontal, 24)
            ConCafeFormField(
                label: "검색",
                text: Binding(get: { uiState.selectorQuery }, set: { onAction(.changeSelectorQuery($0)) }),
                placeholder: uiState.selectorSearchPlaceholder
            )
            .padding(.horizontal, 24)
            if uiState.isSelectorLoading {
                ProgressView()
                    .tint(Color(hex: "EF6797"))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
            } else if uiState.selectorOptions.isEmpty {
                Text("선택 가능한 항목이 없습니다.")
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "8F848F"))
                    .padding(.horizontal, 24)
                    .padding(.vertical, 24)
            } else {
                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(uiState.selectorOptions) { item in
                            Button {
                                onAction(.selectSelectorItem(item.id))
                            } label: {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(item.title)
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(Color(hex: "23161C"))
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    Text(item.subtitle)
                                        .font(.caption)
                                        .foregroundStyle(Color(hex: "8F848F"))
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                }
                                .padding(16)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 24)
                }
            }
        }
        .padding(.top, 16)
        .background(Color(hex: "F8F5F6"))
    }
}

struct BannerEditView_Previews: PreviewProvider {
    static var previews: some View {
        CompatNavigationContainer {
            BannerEditView()
        }
    }
}
