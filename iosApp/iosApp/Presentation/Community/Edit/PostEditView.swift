//
//  PostEditView.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
//

import SwiftUI
import UIKit

struct PostEditView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = PostEditViewModel()

    @State private var showImagePicker = false

    var body: some View {
        PostEditContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction,
            onPickImage: { showImagePicker = true }
        )
        .navigationTitle("게시글 작성")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    viewModel.onAction(.clickSubmit)
                } label: {
                    if viewModel.uiState.isSubmitting {
                        ProgressView()
                            .tint(Color(hex: "EF6797"))
                    } else {
                        Text("등록")
                            .fontWeight(.bold)
                            .foregroundStyle(
                                viewModel.uiState.canSubmit
                                    ? Color(hex: "EF6797")
                                    : Color(hex: "B1A3AC")
                            )
                    }
                }
                .disabled(!viewModel.uiState.canSubmit || viewModel.uiState.isSubmitting)
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
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
}

private struct PostEditContentView: View {
    let uiState: PostEditUiState

    let onAction: (PostEditAction) -> Void

    let onPickImage: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if let infoMessage = uiState.infoMessage {
                    infoBanner(message: infoMessage)
                }
                ConCafeFormField(
                    label: "제목",
                    text: Binding(
                        get: { uiState.title },
                        set: { onAction(.changeTitle($0)) }
                    ),
                    placeholder: "제목을 입력하세요"
                )
                ConCafeFormEditor(
                    label: "내용",
                    text: Binding(
                        get: { uiState.content },
                        set: { onAction(.changeContent($0)) }
                    ),
                    placeholder: "내용을 입력하세요"
                )
                imageSection
            }
            .padding(16)
            .padding(.bottom, 40)
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "F8F5F6"), Color(hex: "FFFBFD")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private var imageSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("사진 첨부")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color(hex: "665A63"))
                Spacer()
                Text("\(uiState.imageUrls.count) / \(uiState.imageMaxCount)")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            HStack(spacing: 10) {
                ForEach(Array(uiState.imageUrls.enumerated()), id: \.offset) { index, imageUrl in
                    imageItem(imageUrl: imageUrl, index: index)
                        .frame(width: 88, height: 88)
                }
                if uiState.imageUrls.count < uiState.imageMaxCount {
                    addImageItem
                        .frame(width: 88, height: 88)
                }
            }
            Text("사진은 최대 \(uiState.imageMaxCount)장까지 첨부할 수 있습니다.")
                .font(.caption)
                .foregroundStyle(Color(hex: "8A8088"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func imageItem(imageUrl: String, index: Int) -> some View {
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
    }

    private var addImageItem: some View {
        Button(action: onPickImage) {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(Color(hex: "FFD1DC").opacity(0.15))
                .overlay {
                    Image(systemName: "plus")
                        .font(.title3.weight(.medium))
                        .foregroundStyle(Color(hex: "EF6797"))
                }
        }
        .buttonStyle(.plain)
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
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color(hex: "F1D88D"), lineWidth: 1)
        )
    }
}

struct PostEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            PostEditView(onNavigationAction: { _ in })
        }
    }
}
