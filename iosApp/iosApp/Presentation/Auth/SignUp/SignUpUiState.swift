//
//  SignUpUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

struct SignUpUiState {
    enum Step {
        case selectType
        case form
    }

    enum UserType: CaseIterable {
        case visitor
        case cast
        case cafeOwner

        var title: String {
            switch self {
            case .visitor:
                return "일반 회원"
            case .cast:
                return "캐스트 (메이드)"
            case .cafeOwner:
                return "카페 운영자"
            }
        }

        var subtitle: String {
            switch self {
            case .visitor:
                return "메이드카페를 방문하고 즐기는 팬"
            case .cast:
                return "카페에서 근무하는 메이드/캐스트"
            case .cafeOwner:
                return "메이드카페를 운영하는 사업자"
            }
        }

        var badge: String {
            switch self {
            case .visitor:
                return "간편 가입 · 소셜 로그인"
            case .cast:
                return "프로필 관리 · 소속 카페 등록"
            case .cafeOwner:
                return "카페 관리 · 휴대폰 인증 필수"
            }
        }

        var submitLabel: String {
            switch self {
            case .cast:
                return "가입 신청하기"
            case .visitor, .cafeOwner:
                return "가입하기"
            }
        }
    }

    struct CafeOption: Equatable {
        let id: String
        let name: String
        let location: String
        let isVerified: Bool
    }

    var step: Step = .selectType

    var selectedUserType: UserType?

    var email: String = ""

    var password: String = ""

    var confirmPassword: String = ""

    var nickname: String = ""

    var name: String = ""

    var phone: String = ""

    var verificationCode: String = ""

    var isPhoneVerified: Bool = false

    var selectedCafe: CafeOption?

    var cafeSearchQuery: String = ""

    var isCafeSearchVisible: Bool = false

    var cafes: [CafeOption] = []

    var isLoading: Bool = false

    var errorMessage: String?

    var infoMessage: String?

    static let empty = SignUpUiState()
}
