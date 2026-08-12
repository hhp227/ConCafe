package com.hhp227.concafe.presentation.main.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.data.model.NativeAdHandle
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.presentation.component.CommunityNativeAd
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ShimmerCardListSkeleton
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import com.hhp227.concafe.presentation.component.ConCafeColors

@Composable
fun CommunityScreen(
    onNavigationAction: (NavigationAction) -> Unit = {},
    showTopBar: Boolean = true,
    viewModel: CommunityViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CommunityViewModel>() }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CommunityEvent.NavigateToPostEdit -> onNavigationAction(NavigationAction.NavigateToPostEdit())
                is CommunityEvent.NavigateToPost -> onNavigationAction(NavigationAction.NavigateToPostDetail(event.postId))
            }
        }
    }
    CommunityContentScreen(
        uiState = uiState,
        showTopBar = showTopBar,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityContentScreen(
    uiState: CommunityUiState,
    showTopBar: Boolean,
    onAction: (CommunityAction) -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasNext && !uiState.isLoadingMore) {
            onAction(CommunityAction.LoadMore)
        }
    }
    Scaffold(
        containerColor = ConCafeColors.surfaceVariant,
        contentWindowInsets = if (showTopBar) {
            ScaffoldDefaults.contentWindowInsets
        } else {
            WindowInsets(0.dp)
        },
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.community_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = ConCafeColors.textPrimary
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(CommunityAction.ClickWritePost) },
                containerColor = ConCafeColors.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.community_write_post))
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    ShimmerCardListSkeleton(itemCount = 4, imageHeight = 96.dp)
                }
            }
            uiState.posts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.community_empty),
                        color = ConCafeColors.textMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val columnCount = if (maxWidth >= CommunityGridTwoColumnMinWidth) 2 else 1
                    val rows = uiState.posts.chunked(columnCount)

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rows.forEachIndexed { rowIndex, rowPosts ->
                            val rowStartIndex = rowIndex * columnCount

                            item(key = rowPosts.joinToString(prefix = "community-post-row-") { it.id }) {
                                CommunityPostRow(
                                    posts = rowPosts,
                                    columnCount = columnCount,
                                    onPostClick = { postId -> onAction(CommunityAction.ClickPost(postId)) }
                                )
                            }
                            nativeAdForRow(
                                rowStartIndex = rowStartIndex,
                                rowSize = rowPosts.size,
                                nativeAds = uiState.nativeAds
                            )?.let { nativeAd ->
                                item(key = "community-native-ad-$rowStartIndex") {
                                    CommunityNativeAdCard(nativeAdHandle = nativeAd)
                                }
                            }
                        }
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = ConCafeColors.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
        uiState.errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                action = {
                    TextButton(onClick = { onAction(CommunityAction.DismissError) }) {
                        Text(stringResource(Res.string.common_close))
                    }
                }
            ) {
                Text(message)
            }
        }
    }
}

@Composable
private fun CommunityPostRow(
    posts: List<CommunityPost>,
    columnCount: Int,
    onPostClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        posts.forEach { post ->
            CommunityPostCard(
                post = post,
                modifier = Modifier.weight(1f),
                onClick = { onPostClick(post.id) }
            )
        }
        repeat(columnCount - posts.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun nativeAdForRow(
    rowStartIndex: Int,
    rowSize: Int,
    nativeAds: Map<Int, NativeAdHandle?>
): NativeAdHandle? {
    return (0 until rowSize).firstNotNullOfOrNull { offset ->
        val postIndex = rowStartIndex + offset
        val pageIndex = postIndex / COMMUNITY_PAGE_SIZE
        val indexInPage = postIndex % COMMUNITY_PAGE_SIZE

        if (indexInPage == COMMUNITY_AD_INSERT_AFTER_INDEX) {
            nativeAds[COMMUNITY_NATIVE_AD_SLOT_START + pageIndex]
        } else {
            null
        }
    }
}

@Composable
private fun CommunityNativeAdCard(nativeAdHandle: NativeAdHandle?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        CommunityNativeAd(
            modifier = Modifier.fillMaxWidth(),
            nativeAdHandle = nativeAdHandle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityPostCard(
    modifier: Modifier = Modifier,
    post: CommunityPost,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ConCafeColors.surfaceTint, ConCafeColors.primaryContainer)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.userNickname.firstOrNull()?.toString() ?: "?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConCafeColors.primary
                        )
                    }
                    Text(
                        text = post.userNickname.ifBlank { "익명" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ConCafeColors.textSecondary
                    )
                }
                Text(
                    text = post.displayDate,
                    fontSize = 12.sp,
                    color = ConCafeColors.outlineStrong
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = post.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConCafeColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = post.content,
                    fontSize = 13.sp,
                    color = ConCafeColors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
            if (post.imageUrls.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.imageUrls.take(3).forEachIndexed { index, imageUrl ->
                        val isLast = index == 2 && post.imageUrls.size > 3
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            CompatImageDisplay(
                                imageUrl = imageUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isLast) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.42f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${post.imageUrls.size - 3}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Divider(color = ConCafeColors.primaryContainer.copy(alpha = 0.3f))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = ConCafeColors.outlineStrong,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(Res.string.community_post_like_count, post.likeCount),
                        fontSize = 12.sp,
                        color = ConCafeColors.textMuted
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = ConCafeColors.outlineStrong,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(Res.string.community_post_comment_count, post.commentCount),
                        fontSize = 12.sp,
                        color = ConCafeColors.textMuted
                    )
                }
            }
        }
    }
}

private const val COMMUNITY_PAGE_SIZE = 20
private const val COMMUNITY_AD_INSERT_AFTER_INDEX = 5
private const val COMMUNITY_NATIVE_AD_SLOT_START = 100
private val CommunityGridTwoColumnMinWidth = 700.dp
