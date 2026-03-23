//
//  NoticeEventView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit
import Shared

struct NoticeEventView: View {
    let cafeId: String

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: NoticeEventViewModel

    var body: some View {
        NoticeEventContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle("공지 및 이벤트 관리")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(
            text: Binding(
                get: { viewModel.uiState.query },
                set: { viewModel.onAction(.changeQuery($0)) }
            ),
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: viewModel.uiState.selectedTab == .notice ? "공지사항 검색" : "이벤트 검색"
        )
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.isFormSheetVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissFormSheet)
                    }
                }
            )
        ) {
            NoticeEventFormSheet(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
            .compatLargeSheetDetent()
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
    }

    init(
        cafeId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: NoticeEventViewModel(cafeId: cafeId))
    }
}

private struct NoticeEventContentView: View {
    let uiState: NoticeEventUiState

    let onAction: (NoticeEventAction) -> Void

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            LinearGradient(
                colors: [Color(hex: "F8F5F6"), Color(hex: "FFFBFD")],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            VStack(spacing: 0) {
                ConCafeTabBar(
                    labels: NoticeEventTab.allCases.map(\.rawValue),
                    selectedIndex: NoticeEventTab.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
                    backgroundColor: Color(hex: "F8F5F6"),
                    onSelect: { index in
                        onAction(.selectTab(NoticeEventTab.allCases[index]))
                    }
                )
                ScrollView {
                    VStack(spacing: 14) {
                        if let infoMessage = uiState.infoMessage {
                            infoBanner(message: infoMessage)
                                .padding(.horizontal, 16)
                        }
                        if uiState.isCurrentTabLoading && uiState.isCurrentTabEmpty {
                            loadingCard
                                .padding(.horizontal, 16)
                        } else if uiState.selectedTab == .notice && uiState.notices.isEmpty {
                            emptyStateCard(message: "등록된 공지사항이 없습니다.")
                                .padding(.horizontal, 16)
                        } else if uiState.selectedTab == .event && uiState.events.isEmpty {
                            emptyStateCard(message: "등록된 이벤트가 없습니다.")
                                .padding(.horizontal, 16)
                        } else if uiState.selectedTab == .notice {
                            ForEach(uiState.notices, id: \.id) { item in
                                noticeCard(item)
                                    .padding(.horizontal, 16)
                                    .onAppear {
                                        if item.id == uiState.notices.last?.id {
                                            onAction(.loadMoreNotices)
                                        }
                                    }
                            }
                        } else {
                            ForEach(uiState.events, id: \.id) { item in
                                eventCard(item)
                                    .padding(.horizontal, 16)
                                    .onAppear {
                                        if item.id == uiState.events.last?.id {
                                            onAction(.loadMoreEvents)
                                        }
                                    }
                            }
                        }
                        if uiState.isCurrentTabLoadingMore {
                            ProgressView()
                                .tint(Color(hex: "EF6797"))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                        }
                    }
                    .padding(.top, 12)
                    .padding(.bottom, 110)
                }
            }
            Button {
                onAction(.clickRegister)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "plus")
                    Text("공지/이벤트 등록")
                        .fontWeight(.bold)
                }
                .foregroundStyle(Color(hex: "2B2330"))
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                .background(Color(hex: "FFD1DC"))
                .clipShape(Capsule())
                .shadow(color: Color(hex: "FFD1DC").opacity(0.45), radius: 14, x: 0, y: 8)
            }
            .padding(.trailing, 16)
            .padding(.bottom, 20)
        }
    }

    private func noticeCard(_ item: CafeNoticeManagementItem) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                HStack(spacing: 6) {
                    if item.isPinned {
                        statusChip("PINNED", container: Color(hex: "FFD1DC"), content: Color(hex: "2B2330"))
                    }
                    switch item.statusAccent {
                    case .published:
                        statusChip(item.statusLabel, container: Color(hex: "E8F8EC"), content: Color(hex: "2E9E5B"))
                    case .draft:
                        statusChip(item.statusLabel, container: Color(hex: "F2F0F3"), content: Color(hex: "7A707A"))
                    case .ended:
                        statusChip(item.statusLabel, container: Color(hex: "F3E8E8"), content: Color(hex: "8C5A5A"))
                    default:
                        EmptyView()
                    }
                }
                Spacer()
                HStack(spacing: 6) {
                    iconButton("square.and.pencil") { onAction(.clickEditNotice(item.id)) }
                    iconButton("trash") { onAction(.clickDeleteNotice(item.id)) }
                }
            }
            Text(item.title)
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: "23161C"))
            Text(item.displayDate)
                .font(.caption)
                .foregroundStyle(Color(hex: "8F848F"))
        }
        .padding(18)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 4)
    }

    private func eventCard(_ item: CafeEventManagementItem) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                AsyncImage(url: URL(string: item.imageUrl)) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    LinearGradient(
                        colors: [Color(hex: "FFE7EF"), Color(hex: "F6D3E0")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                }
                .frame(height: 168)
                .frame(maxWidth: .infinity)
                .clipped()
                .saturation(item.isDimmed ? 0 : 1)
                .overlay(item.isDimmed ? Color.white.opacity(0.16) : Color.clear)
                statusChip(
                    item.statusLabel,
                    container: item.statusLabel == "진행 중" ? Color(hex: "FFD1DC") : Color(hex: "6E6570"),
                    content: item.statusLabel == "진행 중" ? Color(hex: "2B2330") : .white
                )
                .padding(12)
            }
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(item.title)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Color(hex: "23161C"))
                        .lineLimit(1)
                    Spacer()
                    iconButton("square.and.pencil") { onAction(.clickEditEvent(item.id)) }
                }
                HStack(spacing: 6) {
                    Image(systemName: "calendar")
                        .font(.caption)
                    Text(item.periodText)
                        .font(.caption)
                }
                .foregroundStyle(Color(hex: "8F848F"))
            }
            .padding(18)
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 4)
        .opacity(item.isDimmed ? 0.74 : 1)
    }

    private func statusChip(_ text: String, container: Color, content: Color) -> some View {
        Text(text)
            .font(.caption2.weight(.bold))
            .foregroundStyle(content)
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(container)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func iconButton(_ systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color(hex: "9A8D95"))
                .frame(width: 28, height: 28)
        }
        .buttonStyle(.plain)
    }

    private func formField(label: String, value: Binding<String>, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Color(hex: "665A63"))
                .padding(.leading, 4)
            ConCafeFormField(
                label: "",
                text: value,
                placeholder: placeholder
            )
        }
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "6B5320"))
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
        .background(Color(hex: "FFF2D8"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var loadingCard: some View {
        ProgressView()
            .tint(Color(hex: "EF6797"))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 32)
    }

    private func emptyStateCard(message: String) -> some View {
        HStack {
            Spacer()
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "8F848F"))
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(.vertical, 28)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct NoticeEventFormSheet: View {
    let uiState: NoticeEventUiState

    let onAction: (NoticeEventAction) -> Void

    @State private var isImagePickerPresented = false

    @State private var keyboardOverlap: CGFloat = 0

    var body: some View {
        GeometryReader { proxy in
            let safeAreaBottom = proxy.safeAreaInsets.bottom
            let keyboardBottomInset = calculateKeyboardBottomInset(overlap: keyboardOverlap, safeAreaBottom: safeAreaBottom)

            ZStack(alignment: .bottom) {
                Color.black.opacity(0.5)
                    .ignoresSafeArea()
                    .onTapGesture {
                        onAction(.dismissFormSheet)
                    }
                VStack(spacing: 0) {
                    Capsule()
                        .fill(Color(hex: "D6CED2"))
                        .frame(width: 48, height: 5)
                        .padding(.top, 12)
                        .padding(.bottom, 6)
                    HStack {
                        Text(uiState.formSheetTitle)
                            .font(.title3.weight(.bold))
                            .foregroundStyle(Color(hex: "23161C"))
                        Spacer()
                        Button {
                            onAction(.dismissFormSheet)
                        } label: {
                            Image(systemName: "xmark")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Color(hex: "9A8D95"))
                                .frame(width: 28, height: 28)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("제목")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(Color(hex: "665A63"))
                                    .padding(.leading, 4)
                                ConCafeFormField(
                                    label: "",
                                    text: Binding(
                                        get: { uiState.formTitle },
                                        set: { onAction(.changeFormTitle($0)) }
                                    ),
                                    placeholder: uiState.formTitlePlaceholder
                                )
                            }
                            VStack(alignment: .leading, spacing: 8) {
                                Text("내용")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(Color(hex: "665A63"))
                                    .padding(.leading, 4)
                                ConCafeFormEditor(
                                    label: "",
                                    text: Binding(
                                        get: { uiState.formContent },
                                        set: { onAction(.changeFormContent($0)) }
                                    ),
                                    placeholder: uiState.formContentPlaceholder
                                )
                            }
                            if uiState.showsImageSection {
                                representativeImageSection
                            }
                            if uiState.showsPinnedSection {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("중요 공지 (Pinned)")
                                            .font(.subheadline.weight(.bold))
                                            .foregroundStyle(Color(hex: "23161C"))
                                        Text("목록 상단에 고정됩니다.")
                                            .font(.caption)
                                            .foregroundStyle(Color(hex: "8F848F"))
                                    }
                                    Spacer()
                                    Toggle(
                                        "",
                                        isOn: Binding(
                                            get: { uiState.formPinned },
                                            set: { onAction(.changeFormPinned($0)) }
                                        )
                                    )
                                    .labelsHidden()
                                    .tint(Color(hex: "FFD1DC"))
                                }
                                .padding(16)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                            }
                            VStack(alignment: .leading, spacing: 8) {
                                Text(uiState.formScheduleLabel)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(Color(hex: "665A63"))
                                    .padding(.leading, 4)
                                Button {
                                    onAction(.clickReserveSchedule)
                                } label: {
                                    HStack {
                                        Text(uiState.formReservedAt.isEmpty ? uiState.formSchedulePlaceholder : uiState.formReservedAt)
                                            .foregroundStyle(Color(hex: "9A8D95"))
                                        Spacer()
                                        Image(systemName: "calendar")
                                            .foregroundStyle(Color(hex: "9A8D95"))
                                    }
                                    .padding(.horizontal, 16)
                                    .frame(height: 56)
                                    .background(Color.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                                            .stroke(Color(hex: "FFD1DC").opacity(0.3), style: StrokeStyle(lineWidth: 2, dash: [6]))
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 24)
                        .padding(.bottom, 24)
                    }
                }
                .safeAreaInset(edge: .bottom, spacing: 0) {
                    bottomSubmitBar(keyboardBottomInset: keyboardBottomInset)
                }
                .frame(maxWidth: .infinity)
                .frame(maxHeight: 720)
                .background(Color(hex: "F8F5F6"))
                .ignoresSafeArea(edges: .bottom)
            }
            .bindKeyboardOverlap($keyboardOverlap)
            .ignoresSafeArea(.keyboard, edges: .bottom)
        }
        .sheet(isPresented: $isImagePickerPresented) {
            CompatImagePicker(
                onImageSelected: { image in
                    isImagePickerPresented = false
                    if let imageUrl = saveImageToTemporaryFile(image) {
                        onAction(.changeFormImage(imageUrl))
                    }
                },
                onDismiss: {
                    isImagePickerPresented = false
                }
            )
        }
    }

    private func bottomSubmitBar(keyboardBottomInset: CGFloat) -> some View {
        LinearGradient(
            colors: [Color.clear, Color(hex: "F8F5F6"), Color(hex: "F8F5F6")],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(height: 24)
        .overlay(alignment: .bottom) {
            Button {
                onAction(.clickSubmitForm)
            } label: {
                Text(uiState.formSubmitLabel)
                    .font(.headline.weight(.bold))
                    .frame(maxWidth: .infinity)
                    .frame(height: 60)
                    .foregroundStyle(uiState.isFormSubmitEnabled ? Color(hex: "2B2330") : Color(hex: "7F7078"))
                    .background(uiState.isFormSubmitEnabled ? Color(hex: "FFD1DC") : Color(hex: "F0D9E0"))
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(!uiState.isFormSubmitEnabled)
            .padding(.horizontal, 24)
            .padding(.top, 10)
            .padding(.bottom, 24)
        }
        .padding(.bottom, keyboardBottomInset)
    }

    private var representativeImageSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("대표 이미지")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Color(hex: "665A63"))
                .padding(.leading, 4)
            GeometryReader { proxy in
                ZStack(alignment: .bottomTrailing) {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "FFD8E6"), Color(hex: "FFEFF5")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    if uiState.hasAttachedImage {
                        NoticeEventFormImageView(imageUrl: uiState.formImageUrl)
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .clipped()
                    } else {
                        VStack(spacing: 8) {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 32, weight: .semibold))
                                .foregroundStyle(Color(hex: "8B5164"))
                            Text(uiState.formImageTitle)
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(Color(hex: "5A4954"))
                        }
                        .frame(width: proxy.size.width, height: proxy.size.height, alignment: .center)
                    }
                    if uiState.hasAttachedImage {
                        Button("제거") {
                            onAction(.clickRemoveFormImage)
                        }
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color(hex: "8B5164"))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.white)
                        .clipShape(Capsule())
                        .padding(12)
                    }
                }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 200)
            .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .onTapGesture {
                isImagePickerPresented = true
            }
            Text(uiState.formImageDescription)
                .font(.caption)
                .foregroundStyle(Color(hex: "8A8088"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct NoticeEventFormImageView: View {
    let imageUrl: String

    var body: some View {
        if let fileUrl = URL(string: imageUrl),
           fileUrl.isFileURL,
           let uiImage = UIImage(contentsOfFile: fileUrl.path) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFill()
        } else if let remoteUrl = URL(string: imageUrl) {
            AsyncImage(url: remoteUrl) { phase in
                switch phase {
                case .empty:
                    ProgressView()
                        .tint(Color(hex: "9C7A88"))
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure:
                    placeholder
                @unknown default:
                    placeholder
                }
            }
        } else {
            placeholder
        }
    }

    private var placeholder: some View {
        LinearGradient(
            colors: [Color(hex: "FFE7EF"), Color(hex: "F6D3E0")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

private func saveImageToTemporaryFile(_ image: UIImage) -> String? {
    saveCompressedImageToTemporaryFile(image)
}

struct NoticeEventView_Previews: PreviewProvider {
    static var previews: some View {
        NoticeEventView(cafeId: "cafe-1")
    }
}
