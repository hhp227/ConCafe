//
//  PostEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
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

    private var submitTask: Task<Void, Never>?

    private func addImage(_ imageUrl: String) {
        guard uiState.imageUrls.count < uiState.imageMaxCount else {
            uiState.infoMessage = "사진은 최대 \(uiState.imageMaxCount)장까지 첨부할 수 있습니다."
            return
        }
        guard !imageUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        uiState.imageUrls.append(imageUrl)
        uiState.infoMessage = nil
    }

    private func removeImage(_ index: Int) {
        guard uiState.imageUrls.indices.contains(index) else { return }
        uiState.imageUrls.remove(at: index)
    }

    private func submit() {
        let title = uiState.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let content = uiState.content.trimmingCharacters(in: .whitespacesAndNewlines)

        if title.isEmpty {
            uiState.infoMessage = "제목을 입력해주세요."
            return
        }
        if content.isEmpty {
            uiState.infoMessage = "내용을 입력해주세요."
            return
        }
        uiState.isSubmitting = true
        uiState.infoMessage = nil

        submitTask?.cancel()
        submitTask = Task {
            do {
                let uploadedUrls = try await uploadImages(uiState.imageUrls)
                let result = try await createCommunityPostUseCase.invoke(
                    title: title,
                    content: content,
                    imageUrls: uploadedUrls
                )
                uiState.isSubmitting = false
                if result is AppResultSuccess<AnyObject> {
                    event.send(.navigateBack)
                } else if let failure = result as? AppResultFailure {
                    if failure.error is AppErrorUnauthorized {
                        uiState.infoMessage = "게시글 작성은 로그인 후 가능해요."
                    } else if let validation = failure.error as? AppErrorValidationFailed {
                        uiState.infoMessage = Self.validationMessage(for: validation.reason)
                    } else {
                        uiState.infoMessage = "게시글을 등록하지 못했습니다."
                    }
                } else {
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

        for path in localPaths {
            guard !path.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { continue }
            let result = try await uploadImageUseCase.invoke(localPath: path, folder: "community")

            if let success = result as? AppResultSuccess<AnyObject>, let url = success.data as? String {
                uploaded.append(url)
            } else {
                throw NSError(domain: "PostEdit", code: 1, userInfo: [NSLocalizedDescriptionKey: "이미지를 업로드하지 못했습니다."])
            }
        }
        return uploaded
    }

    private static func validationMessage(for reason: String) -> String {
        switch reason {
        case "title is required": return "제목을 입력해주세요."
        case "content is required": return "내용을 입력해주세요."
        default: return reason
        }
    }

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
            removeImage(index)
        case .clickSubmit:
            submit()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        createCommunityPostUseCase: CreateCommunityPostUseCase = KoinInitializerKt.resolveCreateCommunityPostUseCase(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase()
    ) {
        self.createCommunityPostUseCase = createCommunityPostUseCase
        self.uploadImageUseCase = uploadImageUseCase
    }

    deinit {
        submitTask?.cancel()
    }
}
