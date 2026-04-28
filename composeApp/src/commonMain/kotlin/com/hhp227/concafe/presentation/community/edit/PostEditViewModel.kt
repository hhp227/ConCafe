package com.hhp227.concafe.presentation.community.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.CreateCommunityPostUseCase
import com.hhp227.concafe.domain.usecase.GetCommunityPostUseCase
import com.hhp227.concafe.domain.usecase.UpdateCommunityPostUseCase
import com.hhp227.concafe.domain.usecase.UploadImageUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostEditViewModel(
    private val editPostId: String? = null,
    private val createCommunityPostUseCase: CreateCommunityPostUseCase,
    private val updateCommunityPostUseCase: UpdateCommunityPostUseCase,
    private val getCommunityPostUseCase: GetCommunityPostUseCase,
    private val uploadImageUseCase: UploadImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostEditUiState(isEditMode = editPostId != null))
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<PostEditEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun addImage(imageUrl: String) {
        val state = _uiState.value

        if (state.imageUrls.size >= state.imageMaxCount) {
            _uiState.update { it.copy(infoMessage = "사진은 최대 ${state.imageMaxCount}장까지 첨부할 수 있습니다.") }
            return
        }
        if (imageUrl.isBlank()) return
        _uiState.update { it.copy(imageUrls = it.imageUrls + imageUrl, infoMessage = null) }
    }

    private fun removeImage(index: Int) {
        _uiState.update { state ->
            if (state.imageUrls.indices.contains(index)) {
                state.copy(imageUrls = state.imageUrls.toMutableList().also { it.removeAt(index) })
            } else {
                state
            }
        }
    }

    private fun loadPost(postId: String) {
        viewModelScope.launch {
            when (val result = getCommunityPostUseCase(postId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        title = result.data.title,
                        content = result.data.content,
                        imageUrls = result.data.imageUrls
                    )
                }
                is AppResult.Failure -> _uiState.update { it.copy(infoMessage = "게시글을 불러오지 못했습니다.") }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value

        if (state.title.isBlank()) {
            _uiState.update { it.copy(infoMessage = "제목을 입력해주세요.") }
            return
        }
        if (state.content.isBlank()) {
            _uiState.update { it.copy(infoMessage = "내용을 입력해주세요.") }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, infoMessage = null) }
        viewModelScope.launch {
            val uploadedImageUrls = uploadImages(state.imageUrls)

            if (uploadedImageUrls == null) {
                _uiState.update { it.copy(isSubmitting = false, infoMessage = "이미지를 업로드하지 못했습니다.") }
                return@launch
            }
            val result = if (editPostId != null) {
                updateCommunityPostUseCase(editPostId, state.title, state.content, uploadedImageUrls)
            } else {
                createCommunityPostUseCase(state.title, state.content, uploadedImageUrls)
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(PostEditEvent.NavigateBack)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            infoMessage = when (result.error) {
                                is AppError.Unauthorized -> "게시글 작성은 로그인 후 가능해요."
                                is AppError.ValidationFailed -> (result.error as AppError.ValidationFailed).reason.toPostValidationMessage()
                                else -> if (editPostId != null) "게시글을 수정하지 못했습니다." else "게시글을 등록하지 못했습니다."
                            }
                        )
                    }
                }
            }
        }
    }

    private suspend fun uploadImages(localPaths: List<String>): List<String>? {
        val uploaded = mutableListOf<String>()

        for (path in localPaths) {
            if (path.isBlank()) continue
            when (val result = uploadImageUseCase.invoke(path, "community")) {
                is AppResult.Success -> uploaded.add(result.data)
                is AppResult.Failure -> return null
            }
        }
        return uploaded
    }

    fun onAction(action: PostEditAction) {
        when (action) {
            PostEditAction.ClickBack -> viewModelScope.launch { _event.emit(PostEditEvent.NavigateBack) }
            is PostEditAction.ChangeTitle -> _uiState.update { it.copy(title = action.value, infoMessage = null) }
            is PostEditAction.ChangeContent -> _uiState.update { it.copy(content = action.value, infoMessage = null) }
            PostEditAction.ClickAddImage -> _uiState.update { it.copy(infoMessage = null) }
            is PostEditAction.AddImage -> addImage(action.imageUrl)
            is PostEditAction.RemoveImage -> removeImage(action.index)
            PostEditAction.ClickSubmit -> submit()
            PostEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    init {
        editPostId?.let { loadPost(it) }
    }
}

private fun String.toPostValidationMessage(): String = when (this) {
    "title is required" -> "제목을 입력해주세요."
    "content is required" -> "내용을 입력해주세요."
    else -> this
}
