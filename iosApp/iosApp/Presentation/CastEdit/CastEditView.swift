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

    @State private var imagePickerTarget: CastEditImagePickerTarget? = nil

    var body: some View {
        CastEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction,
            onPickProfileImage: {
                imagePickerTarget = .profile
            },
            onPickGalleryImage: {
                imagePickerTarget = .gallery
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
        .sheet(
            isPresented: Binding(
                get: { imagePickerTarget != nil },
                set: { isPresented in
                    if !isPresented {
                        imagePickerTarget = nil
                    }
                }
            )
        ) {
            CompatImagePicker(
                onImageSelected: { image in
                    let target = imagePickerTarget
                    imagePickerTarget = nil
                    saveImageToTemporaryFileAsync(image) { imageUrl in
                        guard let imageUrl else { return }
                        if target == .profile {
                            viewModel.onAction(.selectProfilePhoto(imageUrl))
                        } else if target == .gallery {
                            viewModel.onAction(.addGalleryImage(imageUrl))
                        }
                    }
                },
                onDismiss: {
                    imagePickerTarget = nil
                }
            )
        }
        .alert(
            String(localized: String.LocalizationValue("castedit_alert_image_required_title"), table: "Localizable"),
            isPresented: Binding(
                get: { viewModel.uiState.isImageRequiredAlertVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissImageRequiredAlert)
                    }
                }
            )
        ) {
            Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable")) {
                viewModel.onAction(.dismissImageRequiredAlert)
            }
        } message: {
            Text(String(localized: String.LocalizationValue("castedit_alert_image_required_desc"), table: "Localizable"))
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
    @Environment(\.colorScheme) private var colorScheme

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
                                label: String(localized: String.LocalizationValue("castedit_label_name"), table: "Localizable"),
                                text: Binding(
                                    get: { uiState.castName },
                                    set: { onAction(.changeCastName($0)) }
                                )
                            )
                            ConCafeFormField(
                                label: String(localized: String.LocalizationValue("castedit_label_concept_role"), table: "Localizable"),
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
                                label: String(localized: String.LocalizationValue("castedit_label_intro"), table: "Localizable"),
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
            Group {
                if colorScheme == .dark {
                    Color(hex: "FFF9FC")
                } else {
                    LinearGradient(
                        colors: [Color(hex: "F8F5F6"), Color(hex: "FFFBFD")],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
            }
        )
        .background(colorScheme == .dark ? Color(hex: "FFF9FC") : Color(hex: "F8F5F6"))
        .sheet(isPresented: $isBirthdayPickerPresented) {
            CompatNavigationContainer(title: String(localized: String.LocalizationValue("castedit_birthday_pick"), table: "Localizable")) {
                VStack {
                    DatePicker(
                        String(localized: String.LocalizationValue("castedit_birthday_pick"), table: "Localizable"),
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
                    Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable")) {
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
            Text(String(localized: String.LocalizationValue("castedit_profile_photo_title"), table: "Localizable"))
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text(String(localized: String.LocalizationValue("castedit_profile_photo_hint"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(Color(hex: "8C7E87"))
        }
        .frame(maxWidth: .infinity)
    }

    private var gallerySection: some View {
        let galleryLimitText = "\(uiState.galleryImages.count) / \(uiState.galleryMaxCount)"
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(String(localized: String.LocalizationValue("castedit_gallery_title"), table: "Localizable"))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color(hex: "665A63"))
                Spacer()
                Text(galleryLimitText)
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
                ForEach(Array(uiState.galleryImages.enumerated()), id: \.offset) { index, imageUrl in
                    castGalleryItem(
                        label: String(
                            format: String(localized: String.LocalizationValue("castedit_gallery_item_label"), table: "Localizable"),
                            locale: Locale.current,
                            index + 1
                        ),
                        imageUrl: imageUrl,
                        index: index,
                        onRemoveTap: {
                            onAction(.removeGalleryImage(index))
                        }
                    )
                }
                if uiState.galleryImages.count < uiState.galleryMaxCount {
                    addGalleryItem
                }
            }
            Text(
                String(
                    format: String(localized: String.LocalizationValue("castedit_gallery_guide"), table: "Localizable"),
                    locale: Locale.current,
                    uiState.galleryMaxCount
                )
            )
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
        .background(
            colorScheme == .dark
                ? Color(hex: "FFF9FC")
                : Color.white.opacity(0.92)
        )
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

private struct BirthdayInputField: View {
    @Environment(\.colorScheme) private var colorScheme

    @Binding var text: String

    let onTapCalendar: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: String.LocalizationValue("castedit_birthday_label"), table: "Localizable"))
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
            .background(
                colorScheme == .dark
                    ? Color(uiColor: .tertiarySystemBackground)
                    : Color(hex: "F8F5F6")
            )
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

private func saveImageToTemporaryFileAsync(
    _ image: UIImage,
    completion: @escaping (String?) -> Void
) {
    saveCompressedImageToTemporaryFileAsync(image, completion: completion)
}

struct CastEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            CastEditView(castId: nil, onNavigationAction: { _ in })
        }
    }
}

private enum CastEditImagePickerTarget {
    case profile
    case gallery
}
