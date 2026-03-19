package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

sealed interface CafeInfoEditAction {
    data object ClickBack : CafeInfoEditAction
    data class ChangeCafeName(val value: String) : CafeInfoEditAction
    data class ChangeCafeDescription(val value: String) : CafeInfoEditAction
    data class ChangeAddress(val value: String) : CafeInfoEditAction
    data class ChangeContactNumber(val value: String) : CafeInfoEditAction
    data class ChangeWeekdayOpen(val value: String) : CafeInfoEditAction
    data class ChangeWeekdayClose(val value: String) : CafeInfoEditAction
    data class ChangeWeekendOpen(val value: String) : CafeInfoEditAction
    data class ChangeWeekendClose(val value: String) : CafeInfoEditAction
    data object ClickRepresentativeImage : CafeInfoEditAction
    data class SelectRepresentativeImage(val imageUrl: String) : CafeInfoEditAction
    data object ClickAddGalleryImage : CafeInfoEditAction
    data class AddGalleryImage(val imageUrl: String) : CafeInfoEditAction
    data object ClickPinLocation : CafeInfoEditAction
    data object ClickManageExceptionDates : CafeInfoEditAction
    data object DismissImageRequiredAlert : CafeInfoEditAction
    data object ClickSave : CafeInfoEditAction
    data object DismissInfoMessage : CafeInfoEditAction
}
