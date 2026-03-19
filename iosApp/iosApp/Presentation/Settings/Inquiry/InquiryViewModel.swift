//
//  InquiryViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine
import Shared

@MainActor
final class InquiryViewModel: ObservableObject {
    private let createInquiryUseCase: CreateInquiryUseCase

    @Published private(set) var uiState = InquiryUiState.empty

    let event = PassthroughSubject<InquiryEvent, Never>()

    private func submitInquiry() {
        if uiState.title.isEmpty {
            uiState.errorMessage = "문의 제목을 입력해 주세요."
            return
        }
        if uiState.message.isEmpty {
            uiState.errorMessage = "문의 내용을 입력해 주세요."
            return
        }

        let selectedType = uiState.inquiryType
        let title = uiState.title
        let message = uiState.message
        uiState.isSubmitting = true
        uiState.errorMessage = nil

        Task {
            do {
                let result = try await createInquiryUseCase.invoke(
                    input: InquiryCreate(
                        inquiryType: selectedType.title,
                        title: title,
                        content: message
                    )
                )

                if let failure = result as? AppResultFailure {
                    uiState.isSubmitting = false
                    uiState.errorMessage = failure.error.toUserMessage()
                    return
                }

                uiState = InquiryUiState(inquiryType: selectedType)
                event.send(.showMessage("문의가 접수되었습니다. 검토 후 안내드릴게요."))
            } catch {
                uiState.isSubmitting = false
                uiState.errorMessage = "문의 접수에 실패했습니다."
            }
        }
    }
    
    func onAction(_ action: InquiryAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .inquiryTypeChanged(let type):
            uiState.inquiryType = type
            uiState.errorMessage = nil
        case .titleChanged(let value):
            uiState.title = value
            uiState.errorMessage = nil
        case .messageChanged(let value):
            uiState.message = value
            uiState.errorMessage = nil
        case .submitTapped:
            submitInquiry()
        }
    }
    
    init(
        createInquiryUseCase: CreateInquiryUseCase = KoinInitializerKt.resolveCreateInquiryUseCase()
    ) {
        self.createInquiryUseCase = createInquiryUseCase
    }
}

private extension AppError {
    func toUserMessage() -> String {
        switch self {
        case let error as AppErrorValidationFailed:
            return error.reason
        case is AppErrorUnauthorized:
            return "로그인 후 문의를 접수해 주세요."
        case is AppErrorPermissionDenied:
            return "문의 작성 권한이 없습니다."
        case is AppErrorNotFound:
            return "문의 저장 대상을 찾지 못했습니다."
        case let error as AppErrorNetworkError:
            return error.message ?? "네트워크 오류가 발생했습니다."
        case let error as AppErrorUnknown:
            return error.cause ?? "문의 접수에 실패했습니다."
        default:
            return "문의 접수에 실패했습니다."
        }
    }
}
