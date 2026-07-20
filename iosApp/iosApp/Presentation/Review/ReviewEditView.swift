//
//  ReviewEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/12.
//

import SwiftUI
import UIKit

struct ReviewEditView: View {
    let cafeId: String?

    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: ReviewEditViewModel

    @State private var isPhotoPickerPresented = false

    var body: some View {
        ReviewEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction,
            onPickPhoto: {
                isPhotoPickerPresented = true
            }
        )
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
                    saveImageToTemporaryFileAsync(image) { imageUrl in
                        if let imageUrl {
                            viewModel.onAction(.selectPhoto(imageUrl))
                        }
                    }
                },
                onDismiss: {
                    isPhotoPickerPresented = false
                }
            )
        }
    }

    init(
        cafeId: String? = nil,
        reviewId: String? = nil,
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: ReviewEditViewModel(cafeId: cafeId, reviewId: reviewId))
    }
}

private struct ReviewEditContentView: View {
    let uiState: ReviewEditUiState

    let onAction: (ReviewEditAction) -> Void

    let onPickPhoto: () -> Void

    @State private var keyboardOverlap: CGFloat = 0

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(spacing: 0) {
                    if uiState.isLoading {
                        ProgressView()
                            .tint(ConCafeColors.primary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 32)
                    } else {
                        cafeInfoSection
                    }
                    ratingSection
                    reviewSection
                    photoSection
                    if let infoMessage = uiState.infoMessage {
                        infoBanner(message: infoMessage)
                            .padding(.horizontal, 16)
                            .padding(.top, 12)
                    }
                }
                .padding(.bottom, 12)
                .padding(.bottom, 60)
            }
            bottomBar()
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

    private var cafeInfoSection: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 68, height: 68)
                .overlay {
                    Text("Cafe")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(ConCafeColors.primary)
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(ConCafeColors.primaryContainer.opacity(0.3), lineWidth: 2)
                )
            VStack(alignment: .leading, spacing: 4) {
                if uiState.isVisitVerified {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.seal.fill")
                            .font(.caption)
                        Text(String(localized: String.LocalizationValue("reviewedit_verified_visit"), table: "Localizable"))
                            .font(.caption.weight(.bold))
                    }
                    .foregroundStyle(ConCafeColors.primary)
                }
                Text(uiState.cafeName)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.primary)
                Text(uiState.cafeAddress)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 20)
        .background(ConCafeColors.primaryContainer.opacity(0.1))
    }

    private var ratingSection: some View {
        VStack(spacing: 10) {
            Text(String(localized: String.LocalizationValue("reviewedit_rating_question"), table: "Localizable"))
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
            HStack(spacing: 6) {
                ForEach(1...ReviewEditUiState.maximumRating, id: \.self) { index in
                    let isSelected = index <= uiState.rating
                    Image(systemName: isSelected ? "star.fill" : "star")
                        .font(.system(size: 34))
                        .foregroundStyle(isSelected ? ConCafeColors.gold : ConCafeColors.primaryContainer)
                        .onTapGesture {
                            onAction(.selectRating(index))
                        }
                }
            }
            Text(uiState.ratingMessage)
                .font(.title3.weight(.semibold))
                .foregroundStyle(ConCafeColors.primary)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, 24)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
    }

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(String(localized: String.LocalizationValue("reviewedit_photo_section_title"), table: "Localizable"))
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
            GeometryReader { proxy in
                ZStack(alignment: .bottomTrailing) {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [ConCafeColors.primaryContainer, ConCafeColors.surfaceTint],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    if let photoImageUrl = uiState.photoImageUrl,
                       !photoImageUrl.isEmpty {
                        ReviewPhotoImageView(imageUrl: photoImageUrl)
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .clipped()
                    } else {
                        VStack(spacing: 8) {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 32, weight: .semibold))
                                .foregroundStyle(ConCafeColors.primary)
                            Text(String(localized: String.LocalizationValue("reviewedit_photo_add"), table: "Localizable"))
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(.secondary)
                        }
                        .frame(width: proxy.size.width, height: proxy.size.height, alignment: .center)
                    }
                    if uiState.photoImageUrl != nil {
                        Button(String(localized: String.LocalizationValue("reviewedit_photo_remove"), table: "Localizable")) {
                            onAction(.removePhoto)
                        }
                        .font(.caption.weight(.bold))
                        .foregroundStyle(ConCafeColors.primary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
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
                onAction(.clickAddPhoto)
                onPickPhoto()
            }
            Text(String(localized: String.LocalizationValue("reviewedit_photo_helper"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 24)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
    }

    private var reviewSection: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(String(localized: String.LocalizationValue("reviewedit_review_detail_title"), table: "Localizable"))
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
            ConCafeFormEditor(
                label: "",
                text: Binding(
                    get: { uiState.content },
                    set: { onAction(.changeReviewText($0)) }
                ),
                placeholder: String(localized: String.LocalizationValue("reviewedit_review_detail_hint"), table: "Localizable")
            )
            Text(
                String(
                    format: String(localized: String.LocalizationValue("reviewedit_review_length"), table: "Localizable"),
                    locale: Locale.current,
                    uiState.reviewLength,
                    ReviewEditUiState.minimumReviewLength
                )
            )
                .font(.caption)
                .foregroundStyle(uiState.reviewLength >= ReviewEditUiState.minimumReviewLength ? ConCafeColors.success : .secondary)
                .frame(maxWidth: .infinity, alignment: .trailing)
            if !uiState.availableCastTags.isEmpty {
                castTagSection
            }
            atmosphereCard
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 24)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
    }

    private var atmosphereCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Circle()
                    .fill(ConCafeColors.primaryContainer.opacity(0.1))
                    .frame(width: 34, height: 34)
                    .overlay {
                        Image(systemName: "face.smiling")
                            .foregroundStyle(ConCafeColors.primary)
                    }
                Text(String(localized: String.LocalizationValue("reviewedit_atmosphere_question"), table: "Localizable"))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.primary)
            }
            HStack(spacing: 8) {
                answerChip(
                    title: String(localized: String.LocalizationValue("reviewedit_atmosphere_positive"), table: "Localizable"),
                    isSelected: uiState.atmosphereAnswer == true,
                    action: { onAction(.selectAtmosphereAnswer(true)) }
                )
                answerChip(
                    title: String(localized: String.LocalizationValue("reviewedit_atmosphere_negative"), table: "Localizable"),
                    isSelected: uiState.atmosphereAnswer == false,
                    action: { onAction(.selectAtmosphereAnswer(false)) }
                )
            }
        }
        .padding(16)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var castTagSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: String.LocalizationValue("reviewedit_cast_tag_title"), table: "Localizable"))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(uiState.availableCastTags) { cast in
                        let selected = uiState.taggedCastIds.contains(cast.id)

                        Button {
                            onAction(.toggleCastTag(cast.id))
                        } label: {
                            Text(cast.name)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(selected ? .primary : .secondary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 9)
                                .background(selected ? ConCafeColors.primaryContainer : ConCafeColors.primaryContainer.opacity(0.1))
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(selected ? ConCafeColors.primaryContainer : ConCafeColors.primaryContainer.opacity(0.3), lineWidth: 1)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func bottomBar() -> some View {
        Group {
            if uiState.isLoggedIn {
                Button {
                    onAction(.clickSubmit)
                } label: {
                    HStack {
                        if uiState.isSubmitting {
                            ProgressView()
                                .tint(.primary)
                        } else {
                            Text(uiState.submitButtonLabel)
                                .fontWeight(.bold)
                        }
                    }
                    .foregroundStyle(uiState.isSubmitEnabled ? .primary : .secondary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(uiState.isSubmitEnabled ? ConCafeColors.primaryContainer : ConCafeColors.primaryContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(!uiState.isSubmitEnabled)
                .padding(.horizontal, 16)
                .padding(.top, 14)
                .padding(.bottom, 14)
                .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }).opacity(0.96))
            }
        }
    }

    private func answerChip(
        title: String,
        isSelected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(title)
                .font(.caption.weight(.bold))
                .foregroundStyle(isSelected ? .primary : .secondary)
                .padding(.horizontal, 16)
                .padding(.vertical, 9)
                .background(isSelected ? ConCafeColors.primaryContainer : Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(isSelected ? ConCafeColors.primaryContainer : ConCafeColors.outline, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(ConCafeColors.goldDeep)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.dismissInfoMessage)
            } label: {
                Text(String(localized: String.LocalizationValue("common_close"), table: "Localizable"))
                    .font(.caption.weight(.bold))
                    .foregroundStyle(ConCafeColors.goldDeep)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(ConCafeColors.goldContainer)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(ConCafeColors.gold, lineWidth: 1)
        )
    }
}

private struct ReviewPhotoImageView: View {
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
                        .tint(ConCafeColors.textMuted)
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure:
                    ConCafeColors.surfaceTint
                @unknown default:
                    ConCafeColors.surfaceTint
                }
            }
        } else {
            ConCafeColors.surfaceTint
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

struct ReviewEditView_Previews: PreviewProvider {
    static var previews: some View {
        ReviewEditView()
    }
}
