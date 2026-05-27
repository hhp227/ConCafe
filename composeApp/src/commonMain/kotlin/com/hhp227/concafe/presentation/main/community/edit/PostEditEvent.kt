package com.hhp227.concafe.presentation.main.community.edit

sealed interface PostEditEvent {
    data object NavigateBack : PostEditEvent
}
