package com.hhp227.concafe.presentation.main.cafemanagement.castlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.CastEvent as CastDomainEvent
import com.hhp227.concafe.domain.usecase.DeleteCastUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastPageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CastListViewModel(
    private val cafeId: String,
    private val getCafeCastPageUseCase: GetCafeCastPageUseCase,
    private val deleteCastUseCase: DeleteCastUseCase,
    private val castEventPublisher: CastEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(CastListUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CastListEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private var observeCastEventJob: Job? = null

    private fun loadCastPage(cursor: String?, append: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !append && it.casts.isEmpty(),
                    isLoadingMore = append,
                    infoMessage = if (append) it.infoMessage else null
                )
            }
            when (val result = getCafeCastPageUseCase.invoke(cafeId, cursor, getCafeCastPageUseCase.defaultPageSize())) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        val mergedItems = if (append) state.casts + result.data.items else result.data.items

                        state.copy(
                            casts = mergedItems,
                            nextCursor = result.data.nextCursor,
                            hasMoreCasts = result.data.hasNext,
                            isLoading = false,
                            isLoadingMore = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            infoMessage = "castlist_info_load_failed"
                        )
                    }
                }
            }
        }
    }

    private fun refreshCasts() {
        loadCastPage(cursor = null, append = false)
    }

    private fun clickLoadMoreCasts() {
        val currentState = _uiState.value

        if (currentState.isLoadingMore || !currentState.hasMoreCasts) {
            return
        }
        loadCastPage(cursor = currentState.nextCursor, append = true)
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(CastListEvent.NavigateBack)
        }
    }

    private fun clickAddCast() {
        viewModelScope.launch {
            _event.emit(CastListEvent.NavigateToCastEdit(cafeId = cafeId))
        }
    }

    private fun changeSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    private fun clickCast(castId: String) {
        viewModelScope.launch {
            _event.emit(CastListEvent.NavigateToCastEdit(cafeId = cafeId, castId = castId))
        }
    }

    private fun clickCastSchedule(castId: String) {
        viewModelScope.launch {
            _event.emit(CastListEvent.NavigateToSchedule(castId))
        }
    }

    private fun clickDeleteCast(castId: String) {
        _uiState.update { it.copy(deleteTargetCastId = castId, infoMessage = null) }
    }

    private fun dismissDeleteCastDialog() {
        _uiState.update { it.copy(deleteTargetCastId = null) }
    }

    private fun confirmDeleteCast() {
        val deleteTargetCastId = _uiState.value.deleteTargetCastId

        if (deleteTargetCastId == null) {
            _uiState.update { it.copy(deleteTargetCastId = null) }
        } else {
            viewModelScope.launch {
                when (val result = deleteCastUseCase.invoke(deleteTargetCastId)) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                deleteTargetCastId = null,
                                infoMessage = "castlist_info_cast_deleted"
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                deleteTargetCastId = null,
                                infoMessage = result.error.toString()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun dismissInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun observeCastEvent() {
        observeCastEventJob?.cancel()
        observeCastEventJob = viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastDomainEvent.Created -> if (event.cafeId == cafeId) {
                        refreshCasts()
                    }
                    is CastDomainEvent.Updated -> if (event.cafeId == cafeId) {
                        _uiState.update { state ->
                            state.copy(
                                casts = state.casts.map { preview ->
                                    if (preview.id == event.cast.id) {
                                        preview.copy(
                                            name = event.cast.name,
                                            profileImage = event.cast.profileImage
                                        )
                                    } else {
                                        preview
                                    }
                                }
                            )
                        }
                    }
                    is CastDomainEvent.Deleted -> if (event.cafeId == cafeId) {
                        _uiState.update { state ->
                            state.copy(
                                casts = state.casts.filterNot { it.id == event.castId },
                                deleteTargetCastId = state.deleteTargetCastId?.takeUnless { it == event.castId }
                            )
                        }
                    }
                }
            }
        }
    }

    fun onAction(action: CastListAction) {
        when (action) {
            CastListAction.ClickBack -> clickBack()
            CastListAction.ClickAddCast -> clickAddCast()
            is CastListAction.ChangeSearchQuery -> changeSearchQuery(action.value)
            is CastListAction.ClickCast -> clickCast(action.castId)
            is CastListAction.ClickCastSchedule -> clickCastSchedule(action.castId)
            is CastListAction.ClickDeleteCast -> clickDeleteCast(action.castId)
            CastListAction.ConfirmDeleteCast -> confirmDeleteCast()
            CastListAction.DismissDeleteCastDialog -> dismissDeleteCastDialog()
            CastListAction.ClickLoadMoreCasts -> clickLoadMoreCasts()
            CastListAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }

    init {
        observeCastEvent()
        refreshCasts()
    }

    override fun onCleared() {
        observeCastEventJob?.cancel()
        observeCastEventJob = null
        super.onCleared()
    }
}
