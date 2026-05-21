//
//  PostEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/28/26.
//

import SwiftUI
import UIKit

struct PostEditView: View {
    let editPostId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: PostEditViewModel

    @State private var showImagePicker = false

    @State private var navigateBackTask: Task<Void, Never>?

    var body: some View {
        PostEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction,
            onPickImage: { showImagePicker = true }
        )
        .navigationTitle(
            viewModel.uiState.isEditMode
                ? String(localized: String.LocalizationValue("post_edit_screen_title_edit"), table: "Localizable")
                : String(localized: String.LocalizationValue("post_edit_screen_title"), table: "Localizable")
        )
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                navigateBackTask?.cancel()
                navigateBackTask = Task { @MainActor in
                    await dismissKeyboardAndWaitForHide()
                    guard !Task.isCancelled else { return }
                    onNavigationAction(.navigateBack)
                }
            }
        }
        .onDisappear {
            navigateBackTask?.cancel()
            dismissKeyboard()
        }
        .sheet(isPresented: $showImagePicker) {
            CompatImagePicker(
                onImageSelected: { image in
                    showImagePicker = false
                    saveCompressedImageToTemporaryFileAsync(image) { imageUrl in
                        guard let imageUrl else { return }
                        viewModel.onAction(.addImage(imageUrl))
                    }
                },
                onDismiss: { showImagePicker = false }
            )
        }
    }

    private func dismissKeyboard() {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .endEditing(true)
    }

    @MainActor
    private func dismissKeyboardAndWaitForHide() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                for await _ in NotificationCenter.default.notifications(named: UIResponder.keyboardDidHideNotification) {
                    break
                }
            }
            group.addTask {
                try? await Task.sleep(nanoseconds: 350_000_000)
            }
            dismissKeyboard()
            await group.next()
            group.cancelAll()
        }
    }

    init(editPostId: String? = nil, onNavigationAction: @escaping (NavigationAction) -> Void) {
        self.editPostId = editPostId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: PostEditViewModel(editPostId: editPostId))
    }
}

private struct PostEditContentView: View {
    let uiState: PostEditUiState

    let onAction: (PostEditAction) -> Void

    let onPickImage: () -> Void

    @State private var isPreparingSubmit = false

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(spacing: 20) {
                    if let infoMessage = uiState.infoMessage {
                        infoBanner(message: infoMessage)
                    }
                    ConCafeFormField(
                        label: String(localized: String.LocalizationValue("post_edit_title_label"), table: "Localizable"),
                        text: Binding(
                            get: { uiState.title },
                            set: { onAction(.changeTitle($0)) }
                        ),
                        placeholder: String(localized: String.LocalizationValue("post_edit_title_placeholder"), table: "Localizable")
                    )
                    ConCafeFormEditor(
                        label: String(localized: String.LocalizationValue("post_edit_content_label"), table: "Localizable"),
                        text: Binding(
                            get: { uiState.content },
                            set: { onAction(.changeContent($0)) }
                        ),
                        placeholder: String(localized: String.LocalizationValue("post_edit_content_placeholder"), table: "Localizable"),
                        minHeight: 160
                    )
                    imageSection
                }
                .padding(16)
                .padding(.bottom, 88)
            }
            bottomSubmitBar()
        }
        .background(
            LinearGradient(
                colors: [Color(uiColor: .systemGroupedBackground), Color(uiColor: .secondarySystemGroupedBackground)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .background(Color(uiColor: .systemGroupedBackground))
    }

    private var imageSection: some View {
        let limitText = "\(uiState.imageUrls.count) / \(uiState.imageMaxCount)"
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(String(localized: String.LocalizationValue("post_edit_image_label"), table: "Localizable"))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.secondary)
                Spacer()
                Text(limitText)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            LazyVGrid(
                columns: [
                    GridItem(.flexible(), spacing: 10),
                    GridItem(.flexible(), spacing: 10),
                    GridItem(.flexible(), spacing: 10)
                ],
                spacing: 10
            ) {
                ForEach(Array(uiState.imageUrls.enumerated()), id: \.offset) { index, imageUrl in
                    imageItem(imageUrl: imageUrl, index: index)
                }
                if uiState.imageUrls.count < uiState.imageMaxCount {
                    addImageItem
                }
            }
            Text(String(format: String(localized: String.LocalizationValue("post_edit_image_guide"), table: "Localizable"), locale: Locale.current, "\(uiState.imageMaxCount)"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func imageItem(imageUrl: String, index: Int) -> some View {
        GeometryReader { proxy in
            ZStack(alignment: .topTrailing) {
                Group {
                    if let fileUrl = URL(string: imageUrl), fileUrl.isFileURL,
                       let uiImage = UIImage(contentsOfFile: fileUrl.path) {
                        Image(uiImage: uiImage)
                            .resizable()
                            .scaledToFill()
                    } else if let remoteUrl = URL(string: imageUrl) {
                        AsyncImage(url: remoteUrl) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                            default:
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .fill(Color(hex: "FFE3EC"))
                            }
                        }
                    } else {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(Color(hex: "FFE3EC"))
                    }
                }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                Button {
                    onAction(.removeImage(index))
                } label: {
                    Image(systemName: "xmark")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.white)
                        .frame(width: 22, height: 22)
                        .background(Color.black.opacity(0.52))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .offset(x: 6, y: -6)
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private var addImageItem: some View {
        Button(action: onPickImage) {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color(hex: "FFD1DC").opacity(0.1))
                .overlay {
                    Circle()
                        .stroke(Color(hex: "FFD1DC").opacity(0.4), style: StrokeStyle(lineWidth: 2, dash: [5]))
                        .overlay {
                            Image(systemName: "plus")
                                .foregroundStyle(Color(hex: "EF6797"))
                        }
                        .padding(18)
                }
                .aspectRatio(1, contentMode: .fit)
        }
        .buttonStyle(.plain)
    }

    private func bottomSubmitBar() -> some View {
        Button {
            guard !isPreparingSubmit else { return }
            isPreparingSubmit = true
            Task { @MainActor in
                await dismissKeyboardAndWaitForHide()
                onAction(.clickSubmit)
                isPreparingSubmit = false
            }
        } label: {
            HStack(spacing: 8) {
                if uiState.isSubmitting || isPreparingSubmit {
                    ProgressView().tint(.primary)
                } else {
                    Image(systemName: uiState.isEditMode ? "checkmark.circle" : "square.and.pencil")
                }
                Text(
                    uiState.isEditMode
                        ? String(localized: String.LocalizationValue("post_edit_submit_edit_full"), table: "Localizable")
                        : String(localized: String.LocalizationValue("post_edit_submit"), table: "Localizable")
                )
                .fontWeight(.bold)
            }
            .foregroundStyle(.primary)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                uiState.canSubmit
                    ? Color(hex: "FFD1DC")
                    : Color(hex: "FFD1DC").opacity(0.5)
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!uiState.canSubmit || uiState.isSubmitting || isPreparingSubmit)
        .padding(16)
        .background(Color(uiColor: .secondarySystemBackground).opacity(0.96))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color(hex: "FFD1DC").opacity(0.2))
                .frame(height: 1)
        }
    }

    private func dismissKeyboard() {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .endEditing(true)
    }

    @MainActor
    private func dismissKeyboardAndWaitForHide() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask {
                for await _ in NotificationCenter.default.notifications(named: UIResponder.keyboardDidHideNotification) {
                    break
                }
            }
            group.addTask {
                try? await Task.sleep(nanoseconds: 350_000_000)
            }
            dismissKeyboard()
            await group.next()
            group.cancelAll()
        }
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
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

struct PostEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            PostEditView(editPostId: nil, onNavigationAction: { _ in })
        }
    }
}
