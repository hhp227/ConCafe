package org.hhp227.concafe.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.hhp227.concafe.presentation.navigation.Route

class NavigationViewModel : ViewModel() {
    private val _event = MutableSharedFlow<NavigationEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: NavigationAction) {
        viewModelScope.launch {
            when (action) {
                is NavigationAction.NavigateToMain -> {
                    _event.emit(NavigationEvent.NavigateTo(Route.Main(action.initialTab)))
                }
                is NavigationAction.NavigateToDetail -> {
                    _event.emit(NavigationEvent.NavigateTo(Route.Detail(action.id)))
                }
                is NavigationAction.NavigateToNotification -> {
                    _event.emit(NavigationEvent.NavigateTo(Route.Notification))
                }
                is NavigationAction.NavigateBack -> {
                    _event.emit(NavigationEvent.NavigateBack)
                }
            }
        }
    }
}
