package com.hhp227.concafe.presentation.community.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.Comment
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.common_close
import concafe.composeapp.generated.resources.community_action_block
import concafe.composeapp.generated.resources.community_action_delete
import concafe.composeapp.generated.resources.community_action_edit
import concafe.composeapp.generated.resources.community_action_report
import concafe.composeapp.generated.resources.community_anonymous
import concafe.composeapp.generated.resources.community_post_comment_count
import concafe.composeapp.generated.resources.community_post_like_count
import concafe.composeapp.generated.resources.community_report_sheet_title
import concafe.composeapp.generated.resources.community_report_submit
import concafe.composeapp.generated.resources.community_report_type_abuse
import concafe.composeapp.generated.resources.community_report_type_other
import concafe.composeapp.generated.resources.community_report_type_privacy
import concafe.composeapp.generated.resources.community_report_type_sexual
import concafe.composeapp.generated.resources.community_report_type_spam
import concafe.composeapp.generated.resources.post_detail_comment_edit_placeholder
import concafe.composeapp.generated.resources.post_detail_comment_edit_title
import concafe.composeapp.generated.resources.post_detail_comment_placeholder
import concafe.composeapp.generated.resources.post_detail_delete_message
import concafe.composeapp.generated.resources.post_detail_delete_title
import concafe.composeapp.generated.resources.post_detail_load_failed
import concafe.composeapp.generated.resources.post_detail_load_more_comments
import concafe.composeapp.generated.resources.post_detail_screen_title
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
fun PostDetailScreen(
    postId: String,
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: PostDetailViewModel = viewModel(
        key = "post-detail-$postId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<PostDetailViewModel> { parametersOf(postId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                PostDetailEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is PostDetailEvent.NavigateToPicture -> onNavigationAction(NavigationAction.NavigateToPicture(event.imageUrl))
                is PostDetailEvent.NavigateToPostEdit -> onNavigationAction(NavigationAction.NavigateToPostEdit(event.postId))
            }
        }
    }
    PostDetailContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailContentScreen(
    uiState: PostDetailUiState,
    onAction: (PostDetailAction) -> Unit
) {
    val pink = colorFromHex("EF6797")
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    var localEditText by remember(uiState.editingCommentId) { mutableStateOf(uiState.editCommentText) }

    if (uiState.editingCommentId != null) {
        ModalBottomSheet(
            onDismissRequest = { onAction(PostDetailAction.DismissEditComment) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Text(
                    text = stringResource(Res.string.post_detail_comment_edit_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ImeSafeOutlinedTextField(
                    value = localEditText,
                    onValueChange = { localEditText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.post_detail_comment_edit_placeholder), fontSize = 14.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorFromHex("EF6797"),
                        unfocusedBorderColor = colorFromHex("FFD1DC")
                    ),
                    maxLines = 5,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = { onAction(PostDetailAction.DismissEditComment) }) {
                        Text(stringResource(Res.string.common_cancel), color = secondaryTextColor)
                    }
                    Button(
                        onClick = { onAction(PostDetailAction.ConfirmEditComment(localEditText)) },
                        enabled = localEditText.isNotBlank() && !uiState.isUpdatingComment,
                        colors = ButtonDefaults.buttonColors(containerColor = colorFromHex("EF6797"))
                    ) {
                        if (uiState.isUpdatingComment) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(Res.string.community_action_edit), color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (uiState.isReportSheetVisible) {
        val reportTypes = listOf(
            stringResource(Res.string.community_report_type_spam),
            stringResource(Res.string.community_report_type_abuse),
            stringResource(Res.string.community_report_type_sexual),
            stringResource(Res.string.community_report_type_privacy),
            stringResource(Res.string.community_report_type_other)
        )
        ModalBottomSheet(onDismissRequest = { onAction(PostDetailAction.DismissReportSheet) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(stringResource(Res.string.community_report_sheet_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                Spacer(Modifier.height(12.dp))
                reportTypes.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(PostDetailAction.SelectReportType(type)) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedReportType == type,
                            onClick = { onAction(PostDetailAction.SelectReportType(type)) }
                        )
                        Text(type, color = textColor)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onAction(PostDetailAction.SubmitReport) },
                    enabled = uiState.selectedReportType != null && !uiState.isSubmittingReport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorFromHex("EF6797"))
                ) {
                    if (uiState.isSubmittingReport) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.community_report_submit), color = Color.White)
                    }
                }
            }
        }
    }

    if (uiState.isDeleteConfirmVisible) {
        AlertDialog(
            onDismissRequest = { onAction(PostDetailAction.DismissDeleteConfirm) },
            title = { Text(stringResource(Res.string.post_detail_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.post_detail_delete_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(PostDetailAction.ConfirmDelete) }) {
                    Text(stringResource(Res.string.community_action_delete), color = colorFromHex("E53935"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(PostDetailAction.DismissDeleteConfirm) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.post_detail_screen_title), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { onAction(PostDetailAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = textColor
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading && uiState.post != null) {
                        Box {
                            IconButton(onClick = { onAction(PostDetailAction.ClickMoreMenu) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = textColor)
                            }
                            DropdownMenu(
                                expanded = uiState.isMenuVisible,
                                onDismissRequest = { onAction(PostDetailAction.DismissMoreMenu) }
                            ) {
                                if (uiState.isOwner) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.community_action_edit)) },
                                        onClick = { onAction(PostDetailAction.ClickEdit) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.community_action_delete), color = colorFromHex("E53935")) },
                                        onClick = { onAction(PostDetailAction.ClickDelete) }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.community_action_report)) },
                                        onClick = { onAction(PostDetailAction.ClickReport) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.community_action_block), color = colorFromHex("E53935")) },
                                        onClick = { onAction(PostDetailAction.ClickBlock) }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection)
                )
                .navigationBarsPadding()
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading || uiState.isDeleting -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = pink)
                        }
                    }
                    uiState.post == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(Res.string.post_detail_load_failed), color = secondaryTextColor, textAlign = TextAlign.Center)
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = rememberLazyListState(),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            item {
                                PostBody(
                                    post = uiState.post,
                                    isLiked = uiState.isLiked,
                                    onLike = { onAction(PostDetailAction.ClickLike) },
                                    onImageClick = { url -> onAction(PostDetailAction.ClickImage(url)) }
                                )
                            }
                            item {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = colorFromHex("FFD1DC").copy(alpha = 0.4f)
                                )
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.community_post_comment_count, uiState.commentCount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryTextColor
                                    )
                                }
                            }
                            if (uiState.isLoadingComments) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = pink, strokeWidth = 2.dp)
                                    }
                                }
                            } else {
                                if (uiState.isLoadingMoreComments) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = pink, strokeWidth = 2.dp)
                                        }
                                    }
                                } else if (uiState.hasMoreComments) {
                                    item {
                                        TextButton(
                                            onClick = { onAction(PostDetailAction.LoadMoreComments) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(Res.string.post_detail_load_more_comments), color = secondaryTextColor, fontSize = 13.sp)
                                        }
                                    }
                                }
                                items(uiState.comments, key = { it.id }) { comment ->
                                    CommentItem(
                                        comment = comment,
                                        isMine = comment.userId == uiState.currentUserId,
                                        onEdit = { onAction(PostDetailAction.ClickEditComment(comment.id)) },
                                        onDelete = { onAction(PostDetailAction.ClickDeleteComment(comment.id)) },
                                        onReport = { onAction(PostDetailAction.ClickReportComment(comment.id)) },
                                        onBlock = { onAction(PostDetailAction.ClickBlockComment(comment.id)) }
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
                uiState.errorMessage?.let { message ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { onAction(PostDetailAction.DismissError) }) {
                                Text(stringResource(Res.string.common_close))
                            }
                        }
                    ) { Text(message) }
                }
            }
            CommentInputBar(
                text = uiState.commentText,
                isSending = uiState.isSendingComment,
                canSend = uiState.canSendComment,
                onTextChange = { onAction(PostDetailAction.ChangeCommentText(it)) },
                onSend = { onAction(PostDetailAction.ClickSendComment) }
            )
        }
    }
}

@Composable
private fun PostBody(
    post: CommunityPost,
    isLiked: Boolean,
    onLike: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val pink = colorFromHex("EF6797")

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(
                        Brush.linearGradient(colors = listOf(colorFromHex("FFE3EC"), colorFromHex("F8C5D7")))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.userNickname.firstOrNull()?.toString() ?: "?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = pink
                    )
                }
                Column {
                    Text(
                        text = post.userNickname.ifBlank { stringResource(Res.string.community_anonymous) },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(text = post.displayDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(text = post.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (post.content.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(text = post.content, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
        }
        if (post.imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                post.imageUrls.forEach { imageUrl ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(imageUrl) }
                    ) {
                        CompatImageDisplay(imageUrl = imageUrl, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable(onClick = onLike)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isLiked) pink else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(Res.string.community_post_like_count, post.likeCount),
                    fontSize = 13.sp,
                    color = if (isLiked) pink else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(Res.string.community_post_comment_count, post.commentCount),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    isMine: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
    var isMenuVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(
                    Brush.linearGradient(colors = listOf(colorFromHex("FFE3EC"), colorFromHex("F8C5D7")))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.userNickname.firstOrNull()?.toString() ?: "?",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorFromHex("EF6797")
                )
            }
            Text(
                text = comment.userNickname.ifBlank { stringResource(Res.string.community_anonymous) },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(text = comment.displayDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                IconButton(
                    onClick = { isMenuVisible = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = isMenuVisible,
                    onDismissRequest = { isMenuVisible = false }
                ) {
                    if (isMine) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.community_action_edit)) },
                            onClick = {
                                isMenuVisible = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.community_action_delete), color = colorFromHex("E53935")) },
                            onClick = {
                                isMenuVisible = false
                                onDelete()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.community_action_report)) },
                            onClick = {
                                isMenuVisible = false
                                onReport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.community_action_block), color = colorFromHex("E53935")) },
                            onClick = {
                                isMenuVisible = false
                                onBlock()
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = comment.content,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 21.sp,
            modifier = Modifier.padding(start = 36.dp)
        )
    }
}

@Composable
private fun CommentInputBar(
    text: String,
    isSending: Boolean,
    canSend: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ImeSafeOutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(Res.string.post_detail_comment_placeholder), fontSize = 14.sp) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorFromHex("EF6797"),
                    unfocusedBorderColor = colorFromHex("FFD1DC")
                ),
                maxLines = 3,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (canSend) colorFromHex("EF6797") else colorFromHex("FFD1DC"),
                        shape = CircleShape
                    )
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImeSafeOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    maxLines: Int = Int.MAX_VALUE,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    LaunchedEffect(value) {
        if (textFieldValue.text != value && textFieldValue.composition == null) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { nextValue ->
            textFieldValue = nextValue
            if (nextValue.text != value) {
                onValueChange(nextValue.text)
            }
        },
        modifier = modifier,
        placeholder = placeholder,
        shape = shape,
        colors = colors,
        maxLines = maxLines,
        textStyle = textStyle
    )
}
