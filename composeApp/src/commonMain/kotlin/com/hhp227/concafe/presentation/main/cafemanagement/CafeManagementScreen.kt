package com.hhp227.concafe.presentation.main.cafemanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF7FB), Color(0xFFFFEEF6), Color(0xFFFFFBFD))
                    )
                )
        ) {
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
                            message = message,
                            onDismiss = { onAction(CafeManagementAction.DismissInfoMessage) }
                        )
                    }
                }
                if (uiState.hasOwnedCafes) {
                    item {
                        SectionHeader(
                            title = "내 카페",
                            subtitle = "카페를 탭하면 운영 대시보드 상세 화면으로 이동합니다"
                        )
                    }
                    items(uiState.visibleOwnedCafes, key = { it.id }) { cafe ->
                        CompactOwnedCafeCard(
                            cafe = cafe,
                            onClick = { onAction(CafeManagementAction.ClickCafe(cafe.id)) },
                            onArrowClick = { onAction(CafeManagementAction.ClickCafeDetail(cafe.id)) }
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
                                title = "운영자 신청 상태",
                                subtitle = "기존 카페 연결 요청 현황"
                            )
                        }
                        items(uiState.pendingClaims, key = { it.cafeName + it.requestedAt }) { claim ->
                            PendingClaimCard(claim = claim)
                        }
                    }
                } else {
                    item {
                        SearchCafeSection(
                            searchQuery = uiState.cafeSearchQuery,
                            searchResults = uiState.filteredSearchableCafes,
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
                        colors = listOf(Color(0xFF2F1B3A), Color(0xFF7C3F67), Color(0xFFF06A9D))
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Cafe Manage",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (featuredCafe != null) {
                        "운영 중인 카페를 확인하고 각 카페의 관리 화면으로 이동할 수 있습니다."
                    } else {
                        "운영 카페 연결 상태를 확인하고 기존 카페 검색 또는 새 카페 등록을 시작하세요."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "운영 카페 ${cafeCount}개",
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
        color = Color(0xFFFFF6D7),
        border = BorderStroke(1.dp, Color(0xFFF1D88D))
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
                color = Color(0xFF6B5320)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "안내 닫기", tint = Color(0xFF6B5320))
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B2330)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF786E7A)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactOwnedCafeCard(
    cafe: CafeManagementData.OwnedCafeSummary,
    onClick: () -> Unit,
    onArrowClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
                .background(
                    Brush.linearGradient(
                        colors = if (cafe.isApproved) {
                            listOf(Color(0xFF2F1B3A), Color(0xFF7C3F67), Color(0xFFF06A9D))
                        } else {
                            listOf(Color(0xFF3A3240), Color(0xFF6F6272), Color(0xFFB8A8B2))
                        }
                    )
                )
        ) {
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
                    contentDescription = "카페 상세로 이동",
                    tint = Color.White
                )
            }
        }
    }
}

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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2F6)),
        border = BorderStroke(1.dp, Color(0xFFE5DCE5)),
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
                    "카페 목록 접기"
                } else {
                    "나머지 카페 ${hiddenCount}개 더 보기"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5E4F5D)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF7C6B79)
            )
        }
    }
}

@Composable
private fun SearchCafeSection(
    searchQuery: String,
    searchResults: List<CafeManagementData.SearchableCafeSummary>,
    onSearchQueryChange: (String) -> Unit,
    onClaimCafe: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "기존 카페 검색",
            subtitle = "기등록되어있는 카페를 검색해서 등록할수 있습니다."
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE4DDE5))
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
                    tint = Color(0xFF8E8794)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF222222)),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "카페 이름 또는 지역 검색",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF8E8794)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE4DDE5))
            ) {
                Column {
                    if (searchResults.isEmpty()) {
                        Text(
                            text = "검색 결과가 없습니다",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                            color = Color(0xFF8E8794)
                        )
                    } else {
                        searchResults.forEachIndexed { index, cafe ->
                            SearchCafeItem(
                                cafe = cafe,
                                onClaimClick = { onClaimCafe(cafe.id) }
                            )
                            if (index < searchResults.lastIndex) {
                                Divider(color = Color(0xFFF1EAF1))
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8DFE7))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFCE6EF)
            ) {
                Icon(
                    imageVector = Icons.Default.AddBusiness,
                    contentDescription = null,
                    tint = Color(0xFFEF6797),
                    modifier = Modifier.padding(14.dp)
                )
            }
            Text(
                text = "아직 연결된 운영 카페가 없습니다",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B2330)
            )
            Text(
                text = "검색으로 기존 카페를 찾거나 새 카페를 등록해 운영 권한을 연결하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF786E7A)
            )
            Button(
                onClick = onCreateCafe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6EDF4),
                    contentColor = Color(0xFF6A5666)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("새 카페 등록")
            }
            if (pendingClaims.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "운영자 신청 상태",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2330)
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
                color = Color(0xFF2B2330)
            )
            Text(
                text = cafe.location,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8794)
            )
        }
        Button(
            onClick = onClaimClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6797)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("등록")
        }
    }
}

@Composable
private fun PendingClaimCard(
    claim: CafeManagementData.PendingClaimSummary
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF8EA),
        border = BorderStroke(1.dp, Color(0xFFF0DEB1))
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
                    color = Color(0xFF2B2330)
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFE8B8)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "승인 대기",
                        tint = Color(0xFF9A6A11),
                        modifier = Modifier.padding(7.dp)
                    )
                }
            }
            Text(
                text = "${claim.status} · ${claim.requestedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B774C)
            )
            Text(
                text = claim.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6E6248)
            )
        }
    }
}
