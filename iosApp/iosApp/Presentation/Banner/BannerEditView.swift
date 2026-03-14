//
//  BannerEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import SwiftUI

struct BannerEditView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = BannerEditViewModel()

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
                .fontWeight(.bold)
                .disabled(!viewModel.uiState.isSaveEnabled)
            }
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
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.onNavigationAction = onNavigationAction
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
                text: Binding(
                    get: { uiState.title },
                    set: { onAction(.changeTitle($0)) }
                ),
                placeholder: "배너 제목을 입력해주세요"
            )
            ConCafeFormField(
                label: "서브 문구",
                text: Binding(
                    get: { uiState.subtitle },
                    set: { onAction(.changeSubtitle($0)) }
                ),
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
            ConCafeFormField(
                label: "대상 값",
                text: Binding(
                    get: { uiState.targetValue },
                    set: { onAction(.changeTargetValue($0)) }
                ),
                placeholder: uiState.targetFieldPlaceholder
            )
        }
    }

    private var periodSection: some View {
        sectionCard(title: "노출 기간 설정") {
            VStack(spacing: 8) {
            Text(uiState.displayDaysLabel)
                .font(.caption.weight(.bold))
                .foregroundStyle(Color(hex: "EF6797"))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color(hex: "EF6797").opacity(0.08))
                .clipShape(Capsule())
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
                    ProgressView()
                        .tint(Color(hex: "2B2330"))
                } else {
                    Image(systemName: "square.and.arrow.down")
                }
                Text(uiState.submitButtonText)
                    .fontWeight(.bold)
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
        sectionCard(title: title, trailing: { EmptyView() }, content: content)
    }

    private func sectionCard<Content: View, Trailing: View>(
        title: String,
        @ViewBuilder trailing: () -> Trailing,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                HStack(spacing: 8) {
                    RoundedRectangle(cornerRadius: 999, style: .continuous)
                        .fill(Color(hex: "FFD1DC"))
                        .frame(width: 4, height: 18)
                    Text(title)
                        .font(.title3.weight(.bold))
                }
                Spacer()
                trailing()
            }
            content()
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
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

struct BannerEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            BannerEditView()
        }
    }
}
