package com.hhp227.concafe.presentation.main.community.edit

sealed interface PostEditAction {
    data object ClickBack : PostEditAction
    data class ChangeTitle(val value: String) : PostEditAction
    data class ChangeContent(val value: String) : PostEditAction
    data class AddImage(val imageUrl: String) : PostEditAction
    data class RemoveImage(val index: Int) : PostEditAction
    data object ClickAddImage : PostEditAction
    data object ClickSubmit : PostEditAction
    data object DismissInfoMessage : PostEditAction
}
