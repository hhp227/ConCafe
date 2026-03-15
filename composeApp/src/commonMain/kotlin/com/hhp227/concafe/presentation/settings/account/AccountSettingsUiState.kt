package com.hhp227.concafe.presentation.settings.account

import com.hhp227.concafe.domain.model.UserRole

data class AccountSettingsUiState(
    val isLoading: Boolean,
    val errorMessage: String?,
    val nickname: String,
    val email: String,
    val role: UserRole?,
    val memberSince: String,
    val roleSummary: String,
    val linkedCafeName: String?,
    val ownedCafeCount: Int,
    val castId: String?,
    val castCafeId: String?,
    val castName: String,
    val castConceptRole: String,
    val castDescription: String,
    val isDeleteDialogVisible: Boolean,
    val deleteConfirmation: String,
    val isDeleteRequested: Boolean
) {
    companion object {
        fun empty(): AccountSettingsUiState {
            return AccountSettingsUiState(
                isLoading = true,
                errorMessage = null,
                nickname = "",
                email = "",
                role = null,
                memberSince = "",
                roleSummary = "",
                linkedCafeName = null,
                ownedCafeCount = 0,
                castId = null,
                castCafeId = null,
                castName = "",
                castConceptRole = "",
                castDescription = "",
                isDeleteDialogVisible = false,
                deleteConfirmation = "",
                isDeleteRequested = false
            )
        }
    }
}
