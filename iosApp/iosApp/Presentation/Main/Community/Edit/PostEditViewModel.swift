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
    private let editPostId: String?

    @Published private(set) var uiState = PostEditUiState()

    let event = PassthroughSubject<PostEditEvent, Never>()

    private let createCommunityPostUseCase: CreateCommunityPostUseCase

    private let updateCommunityPostUseCase: UpdateCommunityPostUseCase

    private let getCommunityPostUseCase: GetCommunityPostUseCase

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

    private func loadPost(postId: String) {
        Task {
            do {
                let result = try await getCommunityPostUseCase.invoke(postId: postId)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let post = success.data as? CommunityPost {
                    uiState.title = post.title
                    uiState.content = post.content
                    uiState.imageUrls = post.imageUrls as? [String] ?? []
                }
            } catch { }
        }
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
                let result: Any
                let fallbackMessage: String

                if let postId = editPostId {
                    result = try await updateCommunityPostUseCase.invoke(
                        postId: postId,
                        title: title,
                        content: content,
                        imageUrls: uploadedUrls
                    )
                    fallbackMessage = "게시글을 수정하지 못했습니다."
                } else {
                    result = try await createCommunityPostUseCase.invoke(
                        title: title,
                        content: content,
                        imageUrls: uploadedUrls
                    )
                    fallbackMessage = "게시글을 등록하지 못했습니다."
                }

                uiState.isSubmitting = false
                if result is AppResultSuccess<AnyObject> {
                    event.send(.navigateBack)
                } else if let failure = result as? AppResultFailure {
                    uiState.infoMessage = errorMessage(from: failure, fallback: fallbackMessage)
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSubmitting = false
                uiState.infoMessage = editPostId != nil ? "게시글을 수정하지 못했습니다." : "게시글을 등록하지 못했습니다."
            }
        }
    }

    private func errorMessage(from failure: AppResultFailure, fallback: String) -> String {
        if failure.error is AppErrorUnauthorized {
            return "게시글 작성은 로그인 후 가능해요."
        } else if let validation = failure.error as? AppErrorValidationFailed {
            return Self.validationMessage(for: validation.reason)
        }
        return fallback
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
        editPostId: String? = nil,
        createCommunityPostUseCase: CreateCommunityPostUseCase = KoinInitializerKt.resolveCreateCommunityPostUseCase(),
        updateCommunityPostUseCase: UpdateCommunityPostUseCase = KoinInitializerKt.resolveUpdateCommunityPostUseCase(),
        getCommunityPostUseCase: GetCommunityPostUseCase = KoinInitializerKt.resolveGetCommunityPostUseCase(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase()
    ) {
        self.editPostId = editPostId
        self.createCommunityPostUseCase = createCommunityPostUseCase
        self.updateCommunityPostUseCase = updateCommunityPostUseCase
        self.getCommunityPostUseCase = getCommunityPostUseCase
        self.uploadImageUseCase = uploadImageUseCase
        self.uiState = PostEditUiState(isEditMode: editPostId != nil)

        if let postId = editPostId {
            loadPost(postId: postId)
        }
    }

    deinit {
        submitTask?.cancel()
    }
}
