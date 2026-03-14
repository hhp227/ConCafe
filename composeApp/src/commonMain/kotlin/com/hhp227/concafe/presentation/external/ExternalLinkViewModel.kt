package com.hhp227.concafe.presentation.external

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ExternalLinkViewModel(
    title: String,
    url: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ExternalLinkUiState(
            title = title,
            url = url
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ExternalLinkEvent>(extraBufferCapacity = 1)
    val event = _event.asSharedFlow()

    fun onAction(action: ExternalLinkAction) {
        when (action) {
            ExternalLinkAction.ClickBack -> _event.tryEmit(ExternalLinkEvent.NavigateBack)
        }
    }
}
