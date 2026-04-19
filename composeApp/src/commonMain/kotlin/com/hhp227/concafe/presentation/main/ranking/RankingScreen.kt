package com.hhp227.concafe.presentation.main.ranking

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.data.model.NativeAdHandle
import com.hhp227.concafe.domain.model.RankingFeedEntry
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.model.RankingPromoAd
import com.hhp227.concafe.presentation.component.CapsuleDropdown
import com.hhp227.concafe.presentation.component.ConCafeTabBar
import com.hhp227.concafe.presentation.component.RankingNativeAd
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@Composable
fun RankingScreen(
    viewModel: RankingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<RankingViewModel>() }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(RankingAction.LoadNativeAdIfNeeded)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is RankingEvent.NavigateToCafe -> onNavigate(NavigationAction.NavigateToCafe(event.id))
                is RankingEvent.NavigateToCast -> onNavigate(NavigationAction.NavigateToCast(event.id))
                RankingEvent.NavigateToSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
            }
        }
    }
    LaunchedEffect(uiState.ads.size, uiState.selectedAdIndex) {
        if (uiState.ads.size <= 1) return@LaunchedEffect
        delay(if (uiState.selectedAdIndex == 1) 15_000 else 5_000)
        viewModel.onAction(
            RankingAction.SelectAd((uiState.selectedAdIndex + 1) % uiState.ads.size)
        )
    }
    RankingContent(uiState = uiState, onAction = viewModel::onAction)
    if (uiState.isLoginPromptVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(RankingAction.DismissLoginPrompt) },
            title = { Text(stringResource(Res.string.auth_login_required_title)) },
            text = { Text(stringResource(Res.string.auth_login_required_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(RankingAction.ClickLoginPromptSignIn) }) {
                    Text(stringResource(Res.string.signin_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(RankingAction.DismissLoginPrompt) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankingContent(
    uiState: RankingUiState,
    onAction: (RankingAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFromHex("FFFBFD")),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RankingHeaderSection(
                uiState = uiState,
                onPeriodSelected = { onAction(RankingAction.ChangePeriod(it)) },
                onRegionSelected = { onAction(RankingAction.ChangeRegion(it)) }
            )
        }
        stickyHeader {
            RankingTabBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { onAction(RankingAction.ChangeTab(it)) }
            )
        }
        item(key = "promo_banner") {
            RankingPromoBanner(
                ad = uiState.currentAd,
                selectedIndex = uiState.selectedAdIndex,
                size = uiState.ads.size,
                nativeAdHandle = uiState.nativeAd,
                bannerHeightPx = uiState.bannerHeightPx,
                onHeightMeasured = { height ->
                    onAction(RankingAction.UpdateBannerHeight(height))
                },
                onSelect = { index -> onAction(RankingAction.SelectAd(index)) }
            )
        }
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.rankingEntries.isNotEmpty()) {
            items(uiState.rankingEntries) { item ->
                RankingEntryCard(
                    item = item,
                    isMaid = uiState.selectedTab == RankingUiState.TabType.MAIDS,
                    onClick = {
                        if (uiState.selectedTab == RankingUiState.TabType.MAIDS) {
                            onAction(RankingAction.ClickMaid(item.id))
                        } else {
                            onAction(RankingAction.ClickCafe(item.id))
                        }
                    }
                )
            }
        } else {
            item {
                RankingEmptyPlaceholder()
            }
        }
    }
}

@Composable
fun RankingHeaderSection(
    uiState: RankingUiState,
    onPeriodSelected: (RankingPeriod) -> Unit,
    onRegionSelected: (RankingUiState.RegionFilter) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = colorFromHex("EF6797")
            )
            Text(
                text = stringResource(Res.string.ranking_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CapsuleDropdown(
                selected = periodLabel(uiState.selectedPeriod),
                options = listOf(RankingPeriod.WEEKLY, RankingPeriod.MONTHLY).map { periodLabel(it) to it },
                onSelected = onPeriodSelected
            )
            CapsuleDropdown(
                selected = uiState.selectedRegion.label,
                options = RankingUiState.RegionFilter.entries.map { it.label to it },
                onSelected = onRegionSelected
            )
        }
    }
}

@Composable
private fun periodLabel(period: RankingPeriod): String {
    return when (period) {
        RankingPeriod.WEEKLY -> stringResource(Res.string.ranking_period_weekly)
        RankingPeriod.MONTHLY -> stringResource(Res.string.ranking_period_monthly)
    }
}

@Composable
fun RankingTabBar(
    selectedTab: RankingUiState.TabType,
    onTabSelected: (RankingUiState.TabType) -> Unit
) {
    ConCafeTabBar(
        labels = RankingUiState.TabType.entries.map { it.label },
        selectedIndex = RankingUiState.TabType.entries.indexOf(selectedTab),
        modifier = Modifier.fillMaxWidth(),
        onTabSelected = { index ->
            onTabSelected(RankingUiState.TabType.entries[index])
        }
    )
}

@Composable
fun RankingPromoBanner(
    ad: RankingPromoAd,
    selectedIndex: Int,
    size: Int,
    nativeAdHandle: NativeAdHandle?,
    bannerHeightPx: Int,
    onHeightMeasured: (Int) -> Unit,
    onSelect: (Int) -> Unit
) {
    val bannerHeightDp = with(LocalDensity.current) {
        bannerHeightPx.toDp()
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Crossfade(targetState = selectedIndex) { index ->
            if (index == 1) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(colorFromHex(ad.startColorHex), colorFromHex(ad.endColorHex))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = if (bannerHeightPx > 0) Modifier.height(bannerHeightDp) else Modifier,
                        verticalArrangement = if (bannerHeightPx > 0) Arrangement.SpaceBetween else Arrangement.spacedBy(16.dp)
                    ) {
                        RankingNativeAd(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (bannerHeightPx > 0) {
                                        Modifier.height(bannerHeightDp)
                                    } else {
                                        Modifier.heightIn(min = 120.dp)
                                    }
                                ),
                            nativeAdHandle = nativeAdHandle
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (index == selectedIndex) colorFromHex("EF6797") else colorFromHex("E3D9E0"))
                                        .clickable { onSelect(index) }
                                        .size(width = if (index == selectedIndex) 22.dp else 8.dp, height = 8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(colorFromHex(ad.startColorHex), colorFromHex(ad.endColorHex))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(min = 120.dp)
                            .onSizeChanged { sizeInfo ->
                                onHeightMeasured(sizeInfo.height)
                            },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.White.copy(alpha = 0.22f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = ad.icon(),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = ad.badge,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(ad.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(ad.subtitle, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(ad.desc, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = colorFromHex("262626")
                                ),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(stringResource(Res.string.ranking_detail), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (index == selectedIndex) Color.White else Color.White.copy(alpha = 0.5f))
                                        .clickable { onSelect(index) }
                                        .size(width = if (index == selectedIndex) 22.dp else 8.dp, height = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingEmptyPlaceholder() {
    val isDarkMode = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(Res.string.ranking_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.ranking_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkMode) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingEntryCard(
    item: RankingFeedEntry,
    isMaid: Boolean,
    onClick: () -> Unit
) {
    val isDarkMode = isSystemInDarkTheme()

    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.rank <= 3) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = rankColor(item.rank),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = item.rank.toString(),
                        color = rankColor(item.rank),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 64.dp)
                    .clip(if (isMaid) CircleShape else RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(colorFromHex(item.startColorHex), colorFromHex(item.endColorHex))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.symbol,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    item.name,
                    color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.78f) else colorFromHex("7E7E7E"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${item.score} pt",
                        color = colorFromHex("EF6797"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    RankingChangeIndicator(item.change)
                }
            }
        }
    }
}

@Composable
fun RankingChangeIndicator(change: String) {
    val icon = when {
        change.startsWith("+") -> Icons.Filled.ArrowUpward
        change.startsWith("-") -> Icons.Filled.ArrowDownward
        else -> null
    }
    val tint = when {
        change.startsWith("+") -> colorFromHex("34A853")
        change.startsWith("-") -> colorFromHex("E24B62")
        else -> colorFromHex("8A8A8A")
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Text("-", color = tint, style = MaterialTheme.typography.bodySmall)
        }
        Text(change, color = colorFromHex("8A8A8A"), style = MaterialTheme.typography.labelSmall)
    }
}

private fun rankColor(rank: Int): Color {
    return when (rank) {
        1 -> colorFromHex("E2B11E")
        2 -> colorFromHex("A2A7B1")
        3 -> colorFromHex("B8753B")
        else -> colorFromHex("8A8A8A")
    }
}

private fun RankingPromoAd.icon(): ImageVector {
    return when (symbol) {
        "✨" -> Icons.Default.AutoAwesome
        "🎁" -> Icons.Default.Redeem
        else -> Icons.Default.LocalOffer
    }
}
