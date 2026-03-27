//
//  CastEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit

struct CastEditView: View {
    let cafeId: String?

    let castId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CastEditViewModel

    @State private var isPhotoPickerPresented = false

    @State private var isGalleryPhotoPickerPresented = false

    var body: some View {
        CastEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction,
            onPickProfileImage: {
                isPhotoPickerPresented = true
            },
            onPickGalleryImage: {
                isGalleryPhotoPickerPresented = true
            }
        )
        .navigationTitle(viewModel.uiState.screenTitle)
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
        .sheet(isPresented: $isPhotoPickerPresented) {
            CompatImagePicker(
                onImageSelected: { image in
                    isPhotoPickerPresented = false
                    if let imageUrl = saveImageToTemporaryFile(image) {
                        viewModel.onAction(.selectProfilePhoto(imageUrl))
                    }
                },
                onDismiss: {
                    isPhotoPickerPresented = false
                }
            )
        }
        .sheet(isPresented: $isGalleryPhotoPickerPresented) {
            CompatImagePicker(
                onImageSelected: { image in
                    isGalleryPhotoPickerPresented = false
                    if let imageUrl = saveImageToTemporaryFile(image) {
                        viewModel.onAction(.addGalleryImage(imageUrl))
                    }
                },
                onDismiss: {
                    isGalleryPhotoPickerPresented = false
                }
            )
        }
        .alert(
            "이미지를 등록해주세요",
            isPresented: Binding(
                get: { viewModel.uiState.isImageRequiredAlertVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissImageRequiredAlert)
                    }
                }
            )
        ) {
            Button("확인") {
                viewModel.onAction(.dismissImageRequiredAlert)
            }
        } message: {
            Text("프로필 또는 갤러리 이미지 중 최소 1장은 필수입니다.")
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

    let onPickProfileImage: () -> Void

    let onPickGalleryImage: () -> Void

    @State private var isBirthdayPickerPresented = false

    @State private var selectedBirthdayDate = Date()

    var body: some View {
        ZStack(alignment: .bottom) {
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
                            BirthdayInputField(
                                text: Binding(
                                    get: { uiState.birthday },
                                    set: { onAction(.changeBirthday($0)) }
                                ),
                                onTapCalendar: {
                                    selectedBirthdayDate = TimeUtils.parseBirthdayDate(uiState.birthday) ?? Date()
                                    isBirthdayPickerPresented = true
                                }
                            )
                            ConCafeFormEditor(
                                label: "소개 및 바이오",
                                text: Binding(
                                    get: { uiState.introduction },
                                    set: { onAction(.changeIntroduction($0)) }
                                )
                            )
                            gallerySection
                        }
                        .padding(16)
                        .padding(.bottom, 24)
                        .padding(.bottom, 60)
                    }
                }
                bottomSaveBar()
            }
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "F8F5F6"), Color(hex: "FFFBFD")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .background(Color(hex: "F8F5F6"))
        .sheet(isPresented: $isBirthdayPickerPresented) {
            CompatNavigationContainer(title: "생일 선택") {
                VStack {
                    DatePicker(
                        "생일 선택",
                        selection: $selectedBirthdayDate,
                        displayedComponents: .date
                    )
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .padding()
                    Spacer()
                }
            }
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("확인") {
                        onAction(.changeBirthday(TimeUtils.formatBirthdayDate(selectedBirthdayDate)))
                        isBirthdayPickerPresented = false
                    }
                }
            }
            .compatFractionSheetDetent(0.45)
        }
    }

    private var profilePhotoSection: some View {
        VStack(spacing: 12) {
            Button {
                onAction(.clickProfilePhoto)
                onPickProfileImage()
            } label: {
                GeometryReader { proxy in
                    ZStack(alignment: .bottomTrailing) {
                        Circle()
                            .fill(
                                LinearGradient(
                                    colors: [Color(hex: "FFE3EC"), Color(hex: "F8C5D7")],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                        if let profileImageUrl = uiState.profileImageUrl,
                           !profileImageUrl.isEmpty {
                            CastEditProfileImageView(imageUrl: profileImageUrl)
                                .frame(width: proxy.size.width, height: proxy.size.height)
                                .clipShape(Circle())
                                .clipped()
                        }
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
                            .offset(x: 2, y: 2)
                    }
                    .frame(width: proxy.size.width, height: proxy.size.height)
                }
                .frame(width: 128, height: 128)
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

    private var gallerySection: some View {
        let editableGalleryItems = Array(uiState.galleryImages.enumerated().dropFirst()).map { entry in
            EditableGalleryItem(sourceIndex: entry.offset, imageUrl: entry.element)
        }
        let editableGalleryMaxCount = max(uiState.galleryMaxCount - 1, 0)
        let editableGalleryLimitText = "\(editableGalleryItems.count) / \(editableGalleryMaxCount)"
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("갤러리 사진")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color(hex: "665A63"))
                Spacer()
                Text(editableGalleryLimitText)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            LazyVGrid(
                columns: [
                    GridItem(.flexible(), spacing: 12),
                    GridItem(.flexible(), spacing: 12),
                    GridItem(.flexible(), spacing: 12)
                ],
                spacing: 12
            ) {
                ForEach(editableGalleryItems, id: \.sourceIndex) { item in
                    castGalleryItem(
                        label: "이미지 \(item.sourceIndex + 1)",
                        imageUrl: item.imageUrl,
                        index: item.sourceIndex,
                        onRemoveTap: {
                            onAction(.removeGalleryImage(item.sourceIndex))
                        }
                    )
                }
                if editableGalleryItems.count < editableGalleryMaxCount {
                    addGalleryItem
                }
            }
            Text("캐스트 갤러리에는 최대 \(max(uiState.galleryMaxCount - 1, 0))장까지 등록할 수 있습니다.")
                .font(.caption)
                .foregroundStyle(Color(hex: "8A8088"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func castGalleryItem(
        label: String,
        imageUrl: String,
        index: Int,
        onRemoveTap: @escaping () -> Void
    ) -> some View {
        let gradients = [
            ("FFE6EE", "F7C9D8"),
            ("FFD8E6", "FFEFF5"),
            ("FFD9CF", "FFF0EA")
        ]
        let colors = gradients[index % gradients.count]
        return GeometryReader { proxy in
            ZStack(alignment: .topTrailing) {
                ZStack(alignment: .bottomLeading) {
                    if !imageUrl.isEmpty {
                        CastEditImageView(
                            imageUrl: imageUrl,
                            placeholder: {
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .fill(
                                        LinearGradient(
                                            colors: [Color(hex: colors.0), Color(hex: colors.1)],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                            }
                        )
                        .frame(width: proxy.size.width, height: proxy.size.height)
                        .clipped()
                    } else {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(
                                LinearGradient(
                                    colors: [Color(hex: colors.0), Color(hex: colors.1)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                    }
                    Text(label)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(.black.opacity(0.32))
                        .clipShape(Capsule())
                        .padding(10)
                }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

                Button {
                    onRemoveTap()
                } label: {
                    Image(systemName: "xmark")
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.white)
                        .frame(width: 22, height: 22)
                        .background(.black.opacity(0.52))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .offset(x: 6, y: -6)
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private var addGalleryItem: some View {
        Button {
            onAction(.clickAddGalleryPhoto)
            onPickGalleryImage()
        } label: {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(hex: "FFD1DC").opacity(0.1))
                .overlay {
                    Circle()
                        .stroke(Color(hex: "FFD1DC").opacity(0.4), style: StrokeStyle(lineWidth: 2, dash: [5]))
                        .overlay {
                            Image(systemName: "plus")
                                .foregroundStyle(Color(hex: "EF6797"))
                        }
                        .padding(22)
                }
                .aspectRatio(1, contentMode: .fit)
        }
        .buttonStyle(.plain)
    }

    private func bottomSaveBar() -> some View {
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

private struct BirthdayInputField: View {
    @Binding var text: String

    let onTapCalendar: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("생일")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            HStack(spacing: 8) {
                MaskedBirthdayTextField(text: $text)
                    .frame(maxWidth: .infinity)
                Button(action: onTapCalendar) {
                    Image(systemName: "calendar")
                        .foregroundStyle(Color(hex: "B1A3AC"))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
            .frame(height: 52)
            .background(Color(hex: "F8F5F6"))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.3), lineWidth: 1)
            )
        }
    }
}

private struct MaskedBirthdayTextField: UIViewRepresentable {
    @Binding var text: String

    func makeCoordinator() -> Coordinator {
        Coordinator(text: $text)
    }

    func makeUIView(context: Context) -> UITextField {
        let textField = UITextField()
        textField.delegate = context.coordinator
        textField.keyboardType = .numberPad
        textField.placeholder = "MM/DD/YYYY"
        textField.autocapitalizationType = .none
        textField.autocorrectionType = .no
        textField.borderStyle = .none
        textField.backgroundColor = .clear
        return textField
    }

    func updateUIView(_ uiView: UITextField, context: Context) {
        if uiView.text != text {
            uiView.text = text
        }
        if uiView.isFirstResponder {
            moveCursorToEnd(uiView)
        }
    }

    private func moveCursorToEnd(_ textField: UITextField) {
        let endPosition = textField.endOfDocument
        textField.selectedTextRange = textField.textRange(from: endPosition, to: endPosition)
    }

    final class Coordinator: NSObject, UITextFieldDelegate {
        @Binding private var text: String

        func textField(
            _ textField: UITextField,
            shouldChangeCharactersIn range: NSRange,
            replacementString string: String
        ) -> Bool {
            let currentText = textField.text ?? ""
            guard let currentRange = Range(range, in: currentText) else { return false }
            let updatedText = currentText.replacingCharacters(in: currentRange, with: string)
            let normalized = TimeUtils.normalizeBirthdayInput(updatedText)
            text = normalized
            textField.text = normalized
            let endPosition = textField.endOfDocument
            textField.selectedTextRange = textField.textRange(from: endPosition, to: endPosition)
            return false
        }

        init(text: Binding<String>) {
            self._text = text
        }
    }
}

private struct CastEditProfileImageView: View {
    let imageUrl: String

    var body: some View {
        CastEditImageView(imageUrl: imageUrl, placeholder: { placeholder })
    }

    private var placeholder: some View {
        Circle()
            .fill(
                LinearGradient(
                    colors: [Color(hex: "FFE3EC"), Color(hex: "F8C5D7")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
    }
}

private struct CastEditImageView<Placeholder: View>: View {
    let imageUrl: String
    let placeholder: () -> Placeholder

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
                    placeholder()
                @unknown default:
                    placeholder()
                }
            }
        } else {
            placeholder()
        }
    }
}

private func saveImageToTemporaryFile(_ image: UIImage) -> String? {
    saveCompressedImageToTemporaryFile(image)
}

struct CastEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            CastEditView(castId: nil, onNavigationAction: { _ in })
        }
    }
}

private struct EditableGalleryItem {
    let sourceIndex: Int

    let imageUrl: String
}
