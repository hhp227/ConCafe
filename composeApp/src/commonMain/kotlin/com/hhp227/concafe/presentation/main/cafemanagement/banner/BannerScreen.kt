package com.hhp227.concafe.presentation.main.cafemanagement.banner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.ConCafeTabBar
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerScreen(
    cafeId: String? = null,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: BannerViewModel = viewModel(
        key = "banner-${cafeId ?: "global"}",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<BannerViewModel> { parametersOf(cafeId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                BannerEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is BannerEvent.NavigateToBannerEdit -> {
                    onNavigationAction(NavigationAction.NavigateToBannerEdit(event.cafeId))
                }
                is BannerEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    BannerContentScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
    uiState.pendingDeleteBanner?.let { banner ->
        AlertDialog(
            onDismissRequest = { viewModel.onAction(BannerAction.DismissDeleteBannerDialog) },
            title = { Text("배너 삭제") },
            text = { Text("'${banner.title}' 배너를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(BannerAction.ConfirmDeleteBanner) }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(BannerAction.DismissDeleteBannerDialog) }) {
                    Text("취소")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerContentScreen(
    uiState: BannerUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (BannerAction) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF8F5F6),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(uiState.screenTitle, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { onAction(BannerAction.ClickBack) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                        }
                    }
                )
                ConCafeTabBar(
                    labels = BannerTab.values().map { it.label },
                    selectedIndex = BannerTab.values().indexOf(uiState.selectedTab),
                    modifier = Modifier.fillMaxWidth(),
                    onTabSelected = { index ->
                        onAction(BannerAction.SelectTab(BannerTab.values()[index]))
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFFF8F5F6),
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { onAction(BannerAction.ClickCreateBanner) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD1DC),
                        contentColor = Color(0xFF24161E)
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("새 배너 등록", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F5F6)),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.sectionCountLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7A707A)
                    )
                    Text(
                        text = uiState.locationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF6797)
                    )
                }
            }
            items(uiState.filteredBanners, key = { it.id }) { banner ->
                BannerCard(
                    banner = banner,
                    onEdit = { onAction(BannerAction.ClickEditBanner(banner.id)) },
                    onDelete = { onAction(BannerAction.ClickDeleteBanner(banner.id)) }
                )
            }
            item {
                Text(
                    text = "최대 5개의 배너를 동시에 노출할 수 있습니다.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A8E97)
                )
            }
        }
    }
}

@Composable
private fun BannerCard(
    banner: BannerItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0x1AFFD1DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BannerThumbnail(banner = banner)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x33FFD1DC)
                    ) {
                        Text(
                            text = banner.statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCE5E87)
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "배너 편집", tint = Color(0xFF8F848F))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "배너 삭제", tint = Color(0xFF8F848F))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF24161E)
                    )
                    Text(
                        text = banner.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A707A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFFB2A7AF),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = banner.periodText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9A8E97)
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerThumbnail(banner: BannerItem) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(colorFromHex(banner.accentColorHex), colorFromHex("FFE6ED"))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = banner.iconVector(),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
    }
}

private fun BannerItem.iconVector(): ImageVector = when (imageIcon) {
    "local_cafe" -> Icons.Default.LocalCafe
    "card_giftcard" -> Icons.Default.CardGiftcard
    "music_note" -> Icons.Default.MusicNote
    "cake" -> Icons.Default.Cake
    "restaurant" -> Icons.Default.Restaurant
    "redeem" -> Icons.Default.Redeem
    else -> Icons.Default.EventNote
}
