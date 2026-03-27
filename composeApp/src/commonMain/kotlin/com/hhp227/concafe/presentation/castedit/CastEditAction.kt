package com.hhp227.concafe.presentation.castedit

sealed interface CastEditAction {
    data object ClickBack : CastEditAction
    data object ClickProfilePhoto : CastEditAction
    data class SelectProfilePhoto(val imageUrl: String) : CastEditAction
    data class AddGalleryImage(val imageUrl: String) : CastEditAction
    data class RemoveGalleryImage(val index: Int) : CastEditAction
    data class ChangeCastName(val value: String) : CastEditAction
    data class ChangeConceptRole(val value: String) : CastEditAction
    data class ChangeBirthday(val value: String) : CastEditAction
    data class ChangeIntroduction(val value: String) : CastEditAction
    data class ToggleWorkingDay(val day: CastEditUiState.WorkingDay) : CastEditAction
    data object ClickAddGalleryPhoto : CastEditAction
    data object DismissImageRequiredAlert : CastEditAction
    data object ClickSave : CastEditAction
    data object DismissInfoMessage : CastEditAction
}
