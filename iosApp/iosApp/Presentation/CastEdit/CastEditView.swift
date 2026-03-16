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
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("갤러리 사진")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color(hex: "665A63"))
                Spacer()
                Text(uiState.galleryLimitText)
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
                        label: "이미지 \(index + 1)",
                        imageUrl: imageUrl,
                        index: index
                    )
                }
                if uiState.galleryImages.count < 6 {
                    addGalleryItem
                }
            }
            Text("캐스트 갤러리에는 최대 6장까지 등록할 수 있습니다.")
                .font(.caption)
                .foregroundStyle(Color(hex: "8A8088"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func castGalleryItem(
        label: String,
        imageUrl: String,
        index: Int
    ) -> some View {
        let gradients = [
            ("FFE6EE", "F7C9D8"),
            ("FFD8E6", "FFEFF5"),
            ("FFD9CF", "FFF0EA")
        ]
        let colors = gradients[index % gradients.count]
        return GeometryReader { proxy in
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
        }
        .aspectRatio(1, contentMode: .fit)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
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
    guard let data = image.jpegData(compressionQuality: 0.88) else { return nil }
    let fileName = "\(UUID().uuidString).jpg"
    let fileURL = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)

    do {
        try data.write(to: fileURL, options: .atomic)
        return fileURL.absoluteString
    } catch {
        return nil
    }
}

struct CastEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            CastEditView(castId: nil, onNavigationAction: { _ in })
        }
    }
}
