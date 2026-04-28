package com.hhp227.concafe.presentation.community.edit

sealed interface PostEditEvent {
    data object NavigateBack : PostEditEvent
}
