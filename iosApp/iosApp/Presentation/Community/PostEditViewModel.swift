//
//  PostEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 4/28/26.
//

import Foundation
import Combine
import Shared

@MainActor
final class PostEditViewModel: ObservableObject {
    @Published private(set) var uiState = PostEditUiState()

    let event = PassthroughSubject<PostEditEvent, Never>()

    private let createCommunityPostUseCase: CreateCommunityPostUseCase

    private let uploadImageUseCase: UploadImageUseCase

    func onAction(_ action: PostEditAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .changeTitle(let value):
            uiState.title = value
            uiState.infoMessage = nil
        case .changeContent(let value):
            uiState.content = value
            uiState.infoMessage = nil
        case .clickAddImage:
            uiState.infoMessage = nil
        case .addImage(let imageUrl):
            addImage(imageUrl)
        case .removeImage(let index):
            if uiState.imageUrls.indices.contains(index) {
                uiState.imageUrls.remove(at: index)
            }
        case .clickSubmit:
            submit()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    private func addImage(_ imageUrl: String) {
        guard uiState.imageUrls.count < uiState.imageMaxCount else {
            uiState.infoMessage = "사진은 최대 \(uiState.imageMaxCount)장까지 첨부할 수 있습니다."
            return
        }
        if imageUrl.isEmpty { return }
        uiState.imageUrls.append(imageUrl)
        uiState.infoMessage = nil
    }

    private func submit() {
        let title = uiState.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let content = uiState.content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else {
            uiState.infoMessage = "제목을 입력해주세요."
            return
        }
        guard !content.isEmpty else {
            uiState.infoMessage = "내용을 입력해주세요."
            return
        }
        uiState.isSubmitting = true
        uiState.infoMessage = nil

        Task {
            do {
                let uploadedImageUrls = try await uploadImages(uiState.imageUrls)
                let result = try await createCommunityPostUseCase.invoke(
                    title: title,
                    content: content,
                    imageUrls: uploadedImageUrls
                )
                if result is AppResultSuccess<AnyObject> {
                    uiState.isSubmitting = false
                    event.send(.navigateBack)
                } else {
                    uiState.isSubmitting = false
                    uiState.infoMessage = "게시글을 등록하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSubmitting = false
                uiState.infoMessage = "게시글을 등록하지 못했습니다."
            }
        }
    }

    private func uploadImages(_ localPaths: [String]) async throws -> [String] {
        var uploaded: [String] = []
        for path in localPaths where !path.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let result = try await uploadImageUseCase.invoke(localPath: path, folder: "community")
            guard let success = result as? AppResultSuccess<AnyObject>,
                  let url = success.data as? String else {
                throw NSError(domain: "PostEditUpload", code: 1)
            }
            uploaded.append(url)
        }
        return uploaded
    }

    init(
        createCommunityPostUseCase: CreateCommunityPostUseCase = KoinInitializerKt.resolveCreateCommunityPostUseCase(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase()
    ) {
        self.createCommunityPostUseCase = createCommunityPostUseCase
        self.uploadImageUseCase = uploadImageUseCase
    }
}
