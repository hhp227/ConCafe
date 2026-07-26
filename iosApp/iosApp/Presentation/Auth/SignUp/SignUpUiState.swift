//
//  SignUpUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

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
                return String(localized: String.LocalizationValue("signup_user_type_visitor_title"), table: "Localizable")
            case .cast:
                return String(localized: String.LocalizationValue("signup_user_type_cast_title"), table: "Localizable")
            case .cafeOwner:
                return String(localized: String.LocalizationValue("signup_user_type_owner_title"), table: "Localizable")
            }
        }

        var subtitle: String {
            switch self {
            case .visitor:
                return String(localized: String.LocalizationValue("signup_user_type_visitor_subtitle"), table: "Localizable")
            case .cast:
                return String(localized: String.LocalizationValue("signup_user_type_cast_subtitle"), table: "Localizable")
            case .cafeOwner:
                return String(localized: String.LocalizationValue("signup_user_type_owner_subtitle"), table: "Localizable")
            }
        }

        var badge: String {
            switch self {
            case .visitor:
                return String(localized: String.LocalizationValue("signup_user_type_visitor_badge"), table: "Localizable")
            case .cast:
                return String(localized: String.LocalizationValue("signup_user_type_cast_badge"), table: "Localizable")
            case .cafeOwner:
                return String(localized: String.LocalizationValue("signup_user_type_owner_badge"), table: "Localizable")
            }
        }

        var submitLabel: String {
            switch self {
            case .cast:
                return String(localized: String.LocalizationValue("signup_submit_cast"), table: "Localizable")
            case .visitor, .cafeOwner:
                return String(localized: String.LocalizationValue("signup_submit"), table: "Localizable")
            }
        }
    }

    var step: Step = .selectType

    var selectedUserType: UserType?

    var email: String = ""

    var password: String = ""

    var confirmPassword: String = ""

    var nickname: String = ""

    var name: String = ""

    var isCafeOwner: Bool = false

    var phone: String = ""

    var phoneVerificationId: String?

    var verificationCode: String = ""

    var hasRequestedVerification: Bool = false

    var isPhoneVerified: Bool = false

    var signupCompleted: Bool = false

    var isSocialFlow: Bool = false

    var socialProvider: SignUpProvider?

    var hasAuthenticatedSocialAccount: Bool = false

    var selectedCafe: Cafe?

    var cafeSearchQuery: String = ""

    var isCafeSearchVisible: Bool = false

    var cafes: [Cafe] = []

    var isLoading: Bool = false

    var errorMessage: String?

    var infoMessage: String?

    static let empty = SignUpUiState()
}
