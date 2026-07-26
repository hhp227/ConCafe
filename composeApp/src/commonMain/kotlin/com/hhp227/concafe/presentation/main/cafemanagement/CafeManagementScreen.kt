package com.hhp227.concafe.presentation.main.cafemanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ShimmerCardGridSkeleton
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cafemgmt_add_cafe_desc
import concafe.composeapp.generated.resources.cafemgmt_add_cafe_title
import concafe.composeapp.generated.resources.cafemgmt_cafe_detail_move_content_description
import concafe.composeapp.generated.resources.cafemgmt_claim_pending_content_description
import concafe.composeapp.generated.resources.cafemgmt_empty_desc
import concafe.composeapp.generated.resources.cafemgmt_empty_title
import concafe.composeapp.generated.resources.cafemgmt_fold_cafe_list
import concafe.composeapp.generated.resources.cafemgmt_hero_count
import concafe.composeapp.generated.resources.cafemgmt_hero_desc_empty
import concafe.composeapp.generated.resources.cafemgmt_hero_desc_with_cafe
import concafe.composeapp.generated.resources.cafemgmt_hero_title
import concafe.composeapp.generated.resources.cafemgmt_info_owner_claim_registered
import concafe.composeapp.generated.resources.cafemgmt_more_cafe_list
import concafe.composeapp.generated.resources.cafemgmt_register
import concafe.composeapp.generated.resources.cafemgmt_register_new_cafe
import concafe.composeapp.generated.resources.cafemgmt_search_existing_subtitle
import concafe.composeapp.generated.resources.cafemgmt_search_existing_title
import concafe.composeapp.generated.resources.cafemgmt_search_no_result
import concafe.composeapp.generated.resources.cafemgmt_search_placeholder
import concafe.composeapp.generated.resources.cafemgmt_section_claim_status_subtitle
import concafe.composeapp.generated.resources.cafemgmt_section_claim_status_title
import concafe.composeapp.generated.resources.cafemgmt_section_my_cafe_subtitle
import concafe.composeapp.generated.resources.cafemgmt_section_my_cafe_title
import concafe.composeapp.generated.resources.common_close
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import com.hhp227.concafe.presentation.component.ConCafeColors

@Composable
fun CafeManagementScreen(
    onNavigate: (NavigationAction) -> Unit,
    viewModel: CafeManagementViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CafeManagementViewModel>() }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is CafeManagementEvent.NavigateToCafeDashboard -> {
                    onNavigate(NavigationAction.NavigateToCafeDashboard(event.cafeId))
                }
                is CafeManagementEvent.NavigateToCafe -> {
                    onNavigate(NavigationAction.NavigateToCafe(event.cafeId))
                }
                CafeManagementEvent.NavigateToCafeInfoRegistration -> {
                    onNavigate(NavigationAction.NavigateToCafeInfoEdit(isRegistrationMode = true))
                }
            }
        }
    }
    CafeManagementContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
private fun CafeManagementContentScreen(
    uiState: CafeManagementUiState,
    onAction: (CafeManagementAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ConCafeColors.background)
        ) {
            if (!uiState.isLoading) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        CafeManagementHeroCard(
                            cafeCount = uiState.ownedCafes.size,
                            featuredCafe = uiState.featuredCafe
                        )
                    }
                    uiState.infoMessage?.let { message ->
                        item {
                            InfoBanner(
                                message = when (message) {
                                    "cafemgmt_info_owner_claim_registered" -> stringResource(Res.string.cafemgmt_info_owner_claim_registered)
                                    else -> message
                                },
                                onDismiss = { onAction(CafeManagementAction.DismissInfoMessage) }
                            )
                        }
                    }
                    if (uiState.hasOwnedCafes) {
                        item {
                            SectionHeader(
                                title = stringResource(Res.string.cafemgmt_section_my_cafe_title),
                                subtitle = stringResource(Res.string.cafemgmt_section_my_cafe_subtitle)
                            )
                        }
                        item {
                            OwnedCafeGrid(
                                cafes = uiState.visibleOwnedCafes,
                                onCafeClick = { cafeId -> onAction(CafeManagementAction.ClickCafe(cafeId)) },
                                onCafeDetailClick = { cafeId -> onAction(CafeManagementAction.ClickCafeDetail(cafeId)) }
                            )
                        }
                        if (uiState.hasHiddenOwnedCafes) {
                            item {
                                ExpandOwnedCafeButton(
                                    isExpanded = uiState.isShowingAllCafes,
                                    hiddenCount = (uiState.ownedCafes.size - uiState.visibleOwnedCafes.size).coerceAtLeast(0),
                                    onClick = { onAction(CafeManagementAction.ToggleCafeListExpanded) }
                                )
                            }
                        }
                        if (uiState.pendingClaims.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = stringResource(Res.string.cafemgmt_section_claim_status_title),
                                    subtitle = stringResource(Res.string.cafemgmt_section_claim_status_subtitle)
                                )
                            }
                            items(uiState.pendingClaims, key = { it.cafeName + it.requestedAt }) { claim ->
                                PendingClaimCard(claim = claim)
                            }
                        }
                        item {
                            SearchCafeSection(
                                searchQuery = uiState.cafeSearchQuery,
                                searchResults = uiState.filteredSearchableCafes,
                                excludedCafeIds = uiState.ownedCafes.map { it.id }.toSet(),
                                onSearchQueryChange = { onAction(CafeManagementAction.ChangeCafeSearchQuery(it)) },
                                onClaimCafe = { onAction(CafeManagementAction.ClickClaimCafe(it)) }
                            )
                        }
                        item {
                            AddCafeCard(
                                onCreateCafe = { onAction(CafeManagementAction.ClickCreateCafe) }
                            )
                        }
                    } else {
                        item {
                            SearchCafeSection(
                                searchQuery = uiState.cafeSearchQuery,
                                searchResults = uiState.filteredSearchableCafes,
                                excludedCafeIds = uiState.ownedCafes.map { it.id }.toSet(),
                                onSearchQueryChange = { onAction(CafeManagementAction.ChangeCafeSearchQuery(it)) },
                                onClaimCafe = { onAction(CafeManagementAction.ClickClaimCafe(it)) }
                            )
                        }
                        item {
                            EmptyStateCard(
                                pendingClaims = uiState.pendingClaims,
                                onCreateCafe = { onAction(CafeManagementAction.ClickCreateCafe) }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ShimmerCardGridSkeleton()
                }
            }
        }
    }
}

@Composable
private fun OwnedCafeGrid(
    cafes: List<CafeManagementData.OwnedCafeSummary>,
    onCafeClick: (String) -> Unit,
    onCafeDetailClick: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnCount = if (maxWidth >= OwnedCafeGridTwoColumnMinWidth) 2 else 1
        val rows = cafes.chunked(columnCount)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { cafe ->
                        CompactOwnedCafeCard(
                            cafe = cafe,
                            modifier = Modifier.weight(1f),
                            onClick = { onCafeClick(cafe.id) },
                            onArrowClick = { onCafeDetailClick(cafe.id) }
                        )
                    }
                    repeat(columnCount - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCafeCard(
    onCreateCafe: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ConCafeColors.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.cafemgmt_add_cafe_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.cafemgmt_add_cafe_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = ConCafeColors.textSecondary
            )
            Button(
                onClick = onCreateCafe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConCafeColors.surfaceTint,
                    contentColor = ConCafeColors.textSecondary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = stringResource(Res.string.cafemgmt_register_new_cafe), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CafeManagementHeroCard(
    cafeCount: Int,
    featuredCafe: CafeManagementData.OwnedCafeSummary?
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(ConCafeColors.textPrimary, ConCafeColors.primary, ConCafeColors.primary)
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(Res.string.cafemgmt_hero_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (featuredCafe != null) {
                        stringResource(Res.string.cafemgmt_hero_desc_with_cafe)
                    } else {
                        stringResource(Res.string.cafemgmt_hero_desc_empty)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = stringResource(Res.string.cafemgmt_hero_count, cafeCount),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ConCafeColors.goldContainer,
        border = BorderStroke(1.dp, ConCafeColors.gold)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = ConCafeColors.goldDeep
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.common_close), tint = ConCafeColors.goldDeep)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = ConCafeColors.textSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactOwnedCafeCard(
    cafe: CafeManagementData.OwnedCafeSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onArrowClick: () -> Unit
) {
    val resolvedThumbnail = cafe.thumbnailImage?.trim().orEmpty()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val dynamicHeight = (maxWidth / 1.8f).coerceIn(220.dp, 500.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dynamicHeight)
                    .clip(RoundedCornerShape(24.dp))
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = if (cafe.isApproved) {
                                listOf(ConCafeColors.textPrimary, ConCafeColors.primary, ConCafeColors.primary)
                            } else {
                                listOf(ConCafeColors.textPrimary, ConCafeColors.textSecondary, ConCafeColors.outlineStrong)
                            }
                        )
                    )
            )
            if (resolvedThumbnail.isNotBlank()) {
                CompatImageDisplay(
                    imageUrl = resolvedThumbnail,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.52f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = cafe.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cafe.city,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onArrowClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(Res.string.cafemgmt_cafe_detail_move_content_description),
                    tint = Color.White
                )
            }
            }
        }
    }
}

private val OwnedCafeGridTwoColumnMinWidth = 700.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandOwnedCafeButton(
    isExpanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ConCafeColors.surfaceTint),
        border = BorderStroke(1.dp, ConCafeColors.outline),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) {
                    stringResource(Res.string.cafemgmt_fold_cafe_list)
                } else {
                    stringResource(Res.string.cafemgmt_more_cafe_list, hiddenCount)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ConCafeColors.textSecondary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = ConCafeColors.textSecondary
            )
        }
    }
}

@Composable
private fun SearchCafeSection(
    searchQuery: String,
    searchResults: List<CafeManagementData.SearchableCafeSummary>,
    excludedCafeIds: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onClaimCafe: (String) -> Unit
) {
    val visibleSearchResults = searchResults.filterNot { excludedCafeIds.contains(it.id) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(Res.string.cafemgmt_search_existing_title),
            subtitle = stringResource(Res.string.cafemgmt_search_existing_subtitle)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, ConCafeColors.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ConCafeColors.textMuted
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorFromHex("222222")),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isBlank()) {
                            Text(
                                text = stringResource(Res.string.cafemgmt_search_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = ConCafeColors.textMuted
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
        if (searchQuery.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ConCafeColors.outline)
            ) {
                Column {
                    if (visibleSearchResults.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.cafemgmt_search_no_result),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                            color = ConCafeColors.textMuted
                        )
                    } else {
                        visibleSearchResults.forEachIndexed { index, cafe ->
                            SearchCafeItem(
                                cafe = cafe,
                                onClaimClick = { onClaimCafe(cafe.id) }
                            )
                            if (index < visibleSearchResults.lastIndex) {
                                Divider(color = ConCafeColors.surfaceTint)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    pendingClaims: List<CafeManagementData.PendingClaimSummary>,
    onCreateCafe: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ConCafeColors.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ConCafeColors.surfaceTint
            ) {
                Icon(
                    imageVector = Icons.Default.AddBusiness,
                    contentDescription = null,
                    tint = ConCafeColors.primary,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Text(
                text = stringResource(Res.string.cafemgmt_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.cafemgmt_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = ConCafeColors.textSecondary
            )
            Button(
                onClick = onCreateCafe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConCafeColors.surfaceTint,
                    contentColor = ConCafeColors.textSecondary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(Res.string.cafemgmt_register_new_cafe))
            }
            if (pendingClaims.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.cafemgmt_section_claim_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                pendingClaims.forEach { claim ->
                    PendingClaimCard(claim = claim)
                }
            }
        }
    }
}

@Composable
private fun SearchCafeItem(
    cafe: CafeManagementData.SearchableCafeSummary,
    onClaimClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = cafe.name,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = cafe.location,
                style = MaterialTheme.typography.bodySmall,
                color = ConCafeColors.textMuted
            )
        }
        Button(
            onClick = onClaimClick,
            colors = ButtonDefaults.buttonColors(containerColor = ConCafeColors.primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(Res.string.cafemgmt_register))
        }
    }
}

@Composable
private fun PendingClaimCard(
    claim: CafeManagementData.PendingClaimSummary
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ConCafeColors.warningContainer,
        border = BorderStroke(1.dp, ConCafeColors.warningContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = claim.cafeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = ConCafeColors.warningContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(Res.string.cafemgmt_claim_pending_content_description),
                        tint = ConCafeColors.goldDeep,
                        modifier = Modifier.padding(7.dp)
                    )
                }
            }
            Text(
                text = "${claim.status} · ${claim.requestedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = ConCafeColors.goldDeep
            )
            Text(
                text = claim.message,
                style = MaterialTheme.typography.bodySmall,
                color = ConCafeColors.goldDeep
            )
        }
    }
}
