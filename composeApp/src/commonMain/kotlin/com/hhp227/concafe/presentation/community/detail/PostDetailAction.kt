package com.hhp227.concafe.presentation.community.detail

sealed interface PostDetailAction {
    data object ClickBack : PostDetailAction
    data object ClickLike : PostDetailAction
    data object ClickMoreMenu : PostDetailAction
    data object DismissMoreMenu : PostDetailAction
    data object ClickEdit : PostDetailAction
    data object ClickDelete : PostDetailAction
    data object ConfirmDelete : PostDetailAction
    data object DismissDeleteConfirm : PostDetailAction
    data object ClickReport : PostDetailAction
    data class ClickEditComment(val commentId: String) : PostDetailAction
    data class ConfirmEditComment(val content: String) : PostDetailAction
    data object DismissEditComment : PostDetailAction
    data class ClickDeleteComment(val commentId: String) : PostDetailAction
    data class ClickReportComment(val commentId: String) : PostDetailAction
    data class ChangeCommentText(val text: String) : PostDetailAction
    data object ClickSendComment : PostDetailAction
    data object DismissError : PostDetailAction
    data class ClickImage(val imageUrl: String) : PostDetailAction
    data object LoadMoreComments : PostDetailAction
}
