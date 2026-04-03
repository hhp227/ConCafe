package com.hhp227.concafe.presentation.auth.signup

import com.hhp227.concafe.domain.model.Cafe

data class SignUpUiState(
    val step: Step = Step.SELECT_TYPE,
    val selectedUserType: UserType? = null,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nickname: String = "",
    val name: String = "",
    val isCafeOwner: Boolean = false,
    val phone: String = "",
    val phoneVerificationId: String? = null,
    val verificationCode: String = "",
    val hasRequestedVerification: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val signupCompleted: Boolean = false,
    val selectedCafe: Cafe? = null,
    val cafeSearchQuery: String = "",
    val isCafeSearchVisible: Boolean = false,
    val cafes: List<Cafe> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    enum class Step {
        SELECT_TYPE,
        FORM
    }

    enum class UserType(
        val title: String,
        val subtitle: String,
        val badge: String,
        val submitLabel: String
    ) {
        VISITOR("일반 회원", "컨셉카페를 방문하고 즐기는 팬", "간편 가입 · 소셜 로그인", "가입하기"),
        CAST("캐스트 (메이드)", "카페에서 근무하는 캐스트/메이드", "프로필 관리 · 소속 카페 등록", "가입 신청하기"),
        CAFE_OWNER("카페 운영자", "컨셉카페를 운영하는 사업자", "카페 관리 · 휴대폰 인증 필수", "가입하기")
    }

    companion object {
        fun empty() = SignUpUiState()
    }
}
