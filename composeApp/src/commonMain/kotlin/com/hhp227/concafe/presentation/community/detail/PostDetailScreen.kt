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
import androidx.compose.ui.text.style.TextAlign
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
    val textColor = colorFromHex("2B2330")

    if (uiState.isDeleteConfirmVisible) {
        AlertDialog(
            onDismissRequest = { onAction(PostDetailAction.DismissDeleteConfirm) },
            title = { Text("게시글 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("이 게시글을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { onAction(PostDetailAction.ConfirmDelete) }) {
                    Text("삭제", color = colorFromHex("E53935"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(PostDetailAction.DismissDeleteConfirm) }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        containerColor = colorFromHex("F8F5F6"),
        topBar = {
            TopAppBar(
                title = { Text("게시글", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor) },
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
                                        text = { Text("수정") },
                                        onClick = { onAction(PostDetailAction.ClickEdit) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("삭제", color = colorFromHex("E53935")) },
                                        onClick = { onAction(PostDetailAction.ClickDelete) }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("신고하기") },
                                        onClick = { onAction(PostDetailAction.ClickReport) }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                            Text("게시글을 불러오지 못했습니다.", color = colorFromHex("8C7E87"), textAlign = TextAlign.Center)
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
                                        tint = colorFromHex("B1A3AC"),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "댓글 ${uiState.commentCount}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorFromHex("665A63")
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
                                items(uiState.comments, key = { it.id }) { comment ->
                                    CommentItem(comment = comment)
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
                                Text("닫기")
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

    Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
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
                        text = post.userNickname.ifBlank { "익명" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorFromHex("2B2330")
                    )
                    Text(text = post.displayDate, fontSize = 12.sp, color = colorFromHex("B1A3AC"))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(text = post.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorFromHex("2B2330"))
        if (post.content.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(text = post.content, fontSize = 15.sp, color = colorFromHex("665A63"), lineHeight = 24.sp)
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
                    tint = if (isLiked) pink else colorFromHex("B1A3AC"),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "좋아요 ${post.likeCount}",
                    fontSize = 13.sp,
                    color = if (isLiked) pink else colorFromHex("8C7E87")
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = colorFromHex("B1A3AC"),
                    modifier = Modifier.size(18.dp)
                )
                Text(text = "댓글 ${post.commentCount}", fontSize = 13.sp, color = colorFromHex("8C7E87"))
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
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
                text = comment.userNickname.ifBlank { "익명" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorFromHex("2B2330")
            )
            Spacer(Modifier.weight(1f))
            Text(text = comment.displayDate, fontSize = 11.sp, color = colorFromHex("B1A3AC"))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = comment.content,
            fontSize = 14.sp,
            color = colorFromHex("665A63"),
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
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("댓글을 입력하세요", fontSize = 14.sp) },
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
