package org.hhp227.concafe

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GreetingUiState(
    val showContent: Boolean = false,
    val greeting: String = ""
)

class GreetingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GreetingUiState())
    val uiState: StateFlow<GreetingUiState> = _uiState.asStateFlow()

    fun toggleContent() {
        val nextShowContent = !_uiState.value.showContent
        _uiState.value = GreetingUiState(
            showContent = nextShowContent,
            greeting = if (nextShowContent) Greeting().greet() else ""
        )
    }
}
