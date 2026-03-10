package org.hhp227.concafe.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.hhp227.concafe.presentation.navigation.NavigationEvent.NavigateTo
import org.hhp227.concafe.presentation.navigation.Route.Cafe
import org.hhp227.concafe.presentation.navigation.Route.Cast
import org.hhp227.concafe.presentation.navigation.Route.Main

class NavigationViewModel : ViewModel() {
    private val _event = MutableSharedFlow<NavigationEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: NavigationAction) {
        viewModelScope.launch {
            when (action) {
                is NavigationAction.NavigateToMain -> {
                    _event.emit(NavigateTo(Main(action.initialTab)))
                }
                is NavigationAction.NavigateToCast -> {
                    _event.emit(NavigateTo(Cast(action.id)))
                }
                is NavigationAction.NavigateToCafe -> {
                    _event.emit(NavigateTo(Cafe(action.id)))
                }
                NavigationAction.NavigateToSignIn -> {
                    _event.emit(NavigateTo(Route.SignIn))
                }
                NavigationAction.NavigateToSignUp -> {
                    _event.emit(NavigateTo(Route.SignUp))
                }
                is NavigationAction.NavigateToNotification -> {
                    _event.emit(NavigateTo(Route.Notification))
                }
                NavigationAction.NavigateToSettings -> {
                    _event.emit(NavigateTo(Route.Settings))
                }
                is NavigationAction.NavigateBack -> {
                    _event.emit(NavigationEvent.NavigateBack)
                }
            }
        }
    }
}
