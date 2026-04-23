package com.hhp227.concafe.presentation.main.checkin.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.usecase.GetCheckInGuestFeedUseCase
import com.hhp227.concafe.presentation.main.checkin.CheckInLocationProvider
import com.hhp227.concafe.presentation.main.checkin.CheckInLocationResult
import com.hhp227.concafe.presentation.main.explore.ExploreUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val checkInLocationProvider: CheckInLocationProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MapEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadMapFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        jobs[TaskKey.LOAD_MAP_FEED]?.cancel()
        jobs[TaskKey.LOAD_MAP_FEED] = viewModelScope.launch {
            when (val result = getCheckInGuestFeedUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            currentLocationLabel = result.data.currentLocationLabel,
                            mapCafes = result.data.mapCafes
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun detectUserCity() {
        jobs[TaskKey.DETECT_CITY]?.cancel()
        jobs[TaskKey.DETECT_CITY] = viewModelScope.launch {
            if (_uiState.value.userCityKey != null) return@launch

            when (val result = checkInLocationProvider.getCurrentLocation()) {
                is CheckInLocationResult.Success -> {
                    val cityKey = cityKeyFromCoordinates(
                        lat = result.location.latitude,
                        lng = result.location.longitude
                    )
                    _uiState.update { it.copy(userCityKey = cityKey) }
                }
                is CheckInLocationResult.Failure -> Unit
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.events.collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafe(event.cafe)
                }
            }
        }
    }

    private fun patchCafe(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                mapCafes = state.mapCafes.map { item ->
                    if (item.id == cafe.id) {
                        item.copy(name = cafe.name, locationLabel = cafe.region.city, rating = cafe.ratingAvg)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun initializeRegion(regionKey: String?) {
        val region = regionKey
            ?.let { key -> ExploreUiState.RegionFilter.entries.firstOrNull { it.name == key } }
            ?: return

        _uiState.update { state ->
            if (state.selectedRegion == ExploreUiState.RegionFilter.ALL) {
                state.copy(selectedRegion = region)
            } else {
                state
            }
        }
    }

    fun onAction(action: MapAction) {
        viewModelScope.launch {
            when (action) {
                MapAction.ClickBack -> _event.emit(MapEvent.NavigateBack)
                is MapAction.ClickCafe -> _event.emit(MapEvent.NavigateToCafe(action.id))
                is MapAction.UpdateRegion -> _uiState.update { it.copy(selectedRegion = action.region) }
                is MapAction.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = action.query) }
            }
        }
    }

    init {
        observeCafeDetailEvent()
        detectUserCity()
        loadMapFeed()
    }

    override fun onCleared() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        LOAD_MAP_FEED,
        OBSERVE_CAFE_DETAIL_EVENT,
        DETECT_CITY
    }

    private fun cityKeyFromCoordinates(lat: Double, lng: Double): String? {
        return when {
            lat in 37.4..37.7 && lng in 126.7..127.2 -> "seoul"
            lat in 35.0..35.4 && lng in 128.8..129.3 -> "busan"
            lat in 35.7..36.0 && lng in 128.4..128.8 -> "daegu"
            lat in 35.35..35.60 && lng in 139.50..139.75 -> "yokohama"
            lat in 35.5..35.9 && lng in 139.3..139.9 -> "tokyo"
            lat in 34.5..34.9 && lng in 135.3..135.7 -> "osaka"
            else -> null
        }
    }
}
