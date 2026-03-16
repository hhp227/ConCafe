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
                    if let imageUrl = saveImageToTemporaryFile(image) {
                        viewModel.onAction(.selectPhoto(imageUrl))
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
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.cafeId = cafeId
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: ReviewEditViewModel(cafeId: cafeId))
    }
}

private struct ReviewEditContentView: View {
    let uiState: ReviewEditUiState

    let onAction: (ReviewEditAction) -> Void

    let onPickPhoto: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    if uiState.isLoading {
                        ProgressView()
                            .tint(Color(hex: "EF6797"))
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
            }
            bottomBar
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

    private var cafeInfoSection: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "FFE5EE"), Color(hex: "F4C6D5")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 68, height: 68)
                .overlay {
                    Text("Cafe")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color(hex: "8A5C71"))
                }
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(Color(hex: "FFD1DC").opacity(0.3), lineWidth: 2)
                )
            VStack(alignment: .leading, spacing: 4) {
                if uiState.isVisitVerified {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.seal.fill")
                            .font(.caption)
                        Text("방문 인증됨")
                            .font(.caption.weight(.bold))
                    }
                    .foregroundStyle(Color(hex: "EF6797"))
                }
                Text(uiState.cafeName)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(Color(hex: "24161E"))
                Text(uiState.cafeAddress)
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "7A707A"))
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 20)
        .background(Color(hex: "FFD1DC").opacity(0.1))
    }

    private var ratingSection: some View {
        VStack(spacing: 10) {
            Text("카페 경험은 어떠셨나요?")
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            HStack(spacing: 6) {
                ForEach(1...ReviewEditUiState.maximumRating, id: \.self) { index in
                    let isSelected = index <= uiState.rating
                    Image(systemName: isSelected ? "star.fill" : "star")
                        .font(.system(size: 34))
                        .foregroundStyle(isSelected ? Color(hex: "FFC94D") : Color(hex: "E9DDE1"))
                        .onTapGesture {
                            onAction(.selectRating(index))
                        }
                }
            }
            Text(uiState.ratingMessage)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Color(hex: "EF6797"))
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, 24)
        .background(Color.white)
    }

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("사진 등록 (선택)")
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
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
                    if let photoImageUrl = uiState.photoImageUrl,
                       !photoImageUrl.isEmpty {
                        ReviewPhotoImageView(imageUrl: photoImageUrl)
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .clipped()
                    } else {
                        VStack(spacing: 8) {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 32, weight: .semibold))
                                .foregroundStyle(Color(hex: "8B5164"))
                            Text("리뷰 사진 추가")
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(Color(hex: "5A4954"))
                        }
                        .frame(width: proxy.size.width, height: proxy.size.height, alignment: .center)
                    }
                    if uiState.photoImageUrl != nil {
                        Button("제거") {
                            onAction(.removePhoto)
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
                onAction(.clickAddPhoto)
                onPickPhoto()
            }
            Text("리뷰 사진은 선택사항이며 최대 1장만 등록할 수 있습니다.")
                .font(.caption)
                .foregroundStyle(Color(hex: "8A8088"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 24)
        .background(Color.white)
    }

    private var reviewSection: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("상세 리뷰")
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            ConCafeFormEditor(
                label: "",
                text: Binding(
                    get: { uiState.content },
                    set: { onAction(.changeReviewText($0)) }
                ),
                placeholder: "카페 분위기, 맛, 서비스 등에 대한 솔직한 경험을 남겨주세요 (최소 10자 이상)"
            )
            Text("\(uiState.reviewLength)/\(ReviewEditUiState.minimumReviewLength)자 이상")
                .font(.caption)
                .foregroundStyle(uiState.reviewLength >= ReviewEditUiState.minimumReviewLength ? Color(hex: "2E9E5B") : Color(hex: "9A8D95"))
                .frame(maxWidth: .infinity, alignment: .trailing)
            if !uiState.availableCastTags.isEmpty {
                castTagSection
            }
            atmosphereCard
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 24)
        .background(Color.white)
    }

    private var atmosphereCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Circle()
                    .fill(Color(hex: "FFD1DC").opacity(0.1))
                    .frame(width: 34, height: 34)
                    .overlay {
                        Image(systemName: "face.smiling")
                            .foregroundStyle(Color(hex: "EF6797"))
                    }
                Text("분위기가 좋았나요?")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color(hex: "2B2330"))
            }
            HStack(spacing: 8) {
                answerChip(
                    title: "네",
                    isSelected: uiState.atmosphereAnswer == true,
                    action: { onAction(.selectAtmosphereAnswer(true)) }
                )
                answerChip(
                    title: "아니요",
                    isSelected: uiState.atmosphereAnswer == false,
                    action: { onAction(.selectAtmosphereAnswer(false)) }
                )
            }
        }
        .padding(16)
        .background(Color(hex: "F8F5F6"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var castTagSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("함께 언급한 캐스트")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(uiState.availableCastTags) { cast in
                        let selected = uiState.taggedCastIds.contains(cast.id)

                        Button {
                            onAction(.toggleCastTag(cast.id))
                        } label: {
                            Text(cast.name)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(selected ? Color(hex: "2B2330") : Color(hex: "6E6169"))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 9)
                                .background(selected ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.1))
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(selected ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.3), lineWidth: 1)
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private var bottomBar: some View {
        Group {
            if uiState.isLoggedIn {
                Button {
                    onAction(.clickSubmit)
                } label: {
                    HStack {
                        if uiState.isSubmitting {
                            ProgressView()
                                .tint(Color(hex: "2B2330"))
                        } else {
                            Text(uiState.submitButtonLabel)
                                .fontWeight(.bold)
                        }
                    }
                    .foregroundStyle(uiState.isSubmitEnabled ? Color(hex: "2B2330") : Color(hex: "7F7078"))
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(uiState.isSubmitEnabled ? Color(hex: "FFD1DC") : Color(hex: "F0D9E0"))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(!uiState.isSubmitEnabled)
                .padding(.horizontal, 16)
                .padding(.top, 14)
                .padding(.bottom, 14)
                .background(Color.white.opacity(0.96))
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
                .foregroundStyle(isSelected ? Color(hex: "2B2330") : Color(hex: "8E7F88"))
                .padding(.horizontal, 16)
                .padding(.vertical, 9)
                .background(isSelected ? Color(hex: "FFD1DC") : Color.white)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(isSelected ? Color(hex: "FFD1DC") : Color(hex: "D9CFD5"), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.dismissInfoMessage)
            } label: {
                Text("닫기")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "6B5320"))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(hex: "FFF6D7"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: "F1D88D"), lineWidth: 1)
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
                        .tint(Color(hex: "9C7A88"))
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure:
                    Color(hex: "F4EFF2")
                @unknown default:
                    Color(hex: "F4EFF2")
                }
            }
        } else {
            Color(hex: "F4EFF2")
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

struct ReviewEditView_Previews: PreviewProvider {
    static var previews: some View {
        ReviewEditView()
    }
}
