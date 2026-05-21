package com.hhp227.concafe.presentation.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@Composable
fun CommunityScreen(
    onNavigationAction: (NavigationAction) -> Unit = {},
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
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityContentScreen(
    uiState: CommunityUiState,
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
        containerColor = colorFromHex("F8F5F6"),
        topBar = {
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
                    titleContentColor = colorFromHex("2B2330")
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(CommunityAction.ClickWritePost) },
                containerColor = colorFromHex("EF6797"),
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
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorFromHex("EF6797"))
                }
            }
            uiState.posts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.community_empty),
                        color = colorFromHex("8C7E87"),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.posts.forEachIndexed { index, post ->
                        item(key = post.id) {
                            CommunityPostCard(
                                post = post,
                                onClick = { onAction(CommunityAction.ClickPost(post.id)) }
                            )
                        }
                        val pageIndex = index / COMMUNITY_PAGE_SIZE
                        val indexInPage = index % COMMUNITY_PAGE_SIZE
                        val adSlot = COMMUNITY_NATIVE_AD_SLOT_START + pageIndex
                        val nativeAd = uiState.nativeAds[adSlot]
                        if (indexInPage == COMMUNITY_AD_INSERT_AFTER_INDEX && nativeAd != null) {
                            item(key = "community-native-ad-$adSlot") {
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
                                    color = colorFromHex("EF6797"),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
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
    post: CommunityPost,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                                    colors = listOf(colorFromHex("FFE3EC"), colorFromHex("F8C5D7"))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.userNickname.firstOrNull()?.toString() ?: "?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorFromHex("EF6797")
                        )
                    }
                    Text(
                        text = post.userNickname.ifBlank { "익명" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorFromHex("665A63")
                    )
                }
                Text(
                    text = post.displayDate,
                    fontSize = 12.sp,
                    color = colorFromHex("B1A3AC")
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = post.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorFromHex("2B2330"),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = post.content,
                    fontSize = 13.sp,
                    color = colorFromHex("665A63"),
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
            Divider(color = colorFromHex("FFD1DC").copy(alpha = 0.3f))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = colorFromHex("B1A3AC"),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(Res.string.community_post_like_count, post.likeCount),
                        fontSize = 12.sp,
                        color = colorFromHex("8C7E87")
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
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(Res.string.community_post_comment_count, post.commentCount),
                        fontSize = 12.sp,
                        color = colorFromHex("8C7E87")
                    )
                }
            }
        }
    }
}

private const val COMMUNITY_PAGE_SIZE = 20
private const val COMMUNITY_AD_INSERT_AFTER_INDEX = 5
private const val COMMUNITY_NATIVE_AD_SLOT_START = 100
