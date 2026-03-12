package org.hhp227.concafe.presentation.main.myinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.hhp227.concafe.presentation.navigation.NavigationAction
import org.hhp227.concafe.presentation.navigation.NavigationAction.*
import org.hhp227.concafe.domain.model.UserRole
import org.koin.core.context.GlobalContext

@Composable
fun MyInfoScreen(
    viewModel: MyInfoViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<MyInfoViewModel>()
            }
        }
    ),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is MyInfoEvent.NavigateToCafe -> onNavigate(NavigateToCafe(event.id))
                is MyInfoEvent.NavigateToCast -> onNavigate(NavigateToCast(event.id))
                MyInfoEvent.NavigateToSignIn -> onNavigate(NavigateToSignIn)
            }
        }
    }
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        !uiState.isLoggedIn -> {
            GuestMyInfoScreen(
                uiState, viewModel::onAction
            )
        }
        else -> {
            ProfileMyInfoScreen(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }
}

@Composable
private fun GuestMyInfoScreen(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit
) {
    val features = listOf(
        GuestFeatureItem(Icons.Filled.Place, "체크인 기록", "방문한 카페를 기록하고\n추억을 남겨보세요", Color(0xFFEF6797), Color(0xFFF57AA8)),
        GuestFeatureItem(Icons.Filled.Favorite, "즐겨찾기", "좋아하는 카페와 메이드를\n저장하세요", Color(0xFF9C6ADE), Color(0xFFB388EB)),
        GuestFeatureItem(Icons.Filled.Star, "배지 수집", "다양한 활동으로\n특별한 배지를 모아보세요", Color(0xFFF0B429), Color(0xFFF5C857)),
        GuestFeatureItem(Icons.Filled.CardGiftcard, "멤버십 혜택", "특별한 이벤트와\n할인 혜택을 받으세요", Color(0xFF4C8BF5), Color(0xFF71A7FF))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFEF6797), Color(0xFFF8A0C2))))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💗", style = MaterialTheme.typography.headlineLarge)
                    Text("ConCafe에 오신 것을\n환영합니다!", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("로그인하고 메이드카페의 모든 것을 즐겨보세요", color = Color.White.copy(alpha = 0.9f))
                    Button(
                        onClick = { onAction(MyInfoAction.ClickSignIn) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFFEF6797),
                            modifier = Modifier.size(18.dp)
                        )
                        Box(modifier = Modifier.width(6.dp))
                        Text(
                            "로그인하기",
                            color = Color(0xFFEF6797),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            Text("로그인 후 이용 가능한 기능", fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .height(296.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(features) { feature ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier
                                .height(140.dp)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(feature.startColor, feature.endColor))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(feature.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                feature.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF777777),
                                maxLines = 3,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("인기 카페 둘러보기", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.clickable { },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("더보기", color = Color(0xFFEF6797), style = MaterialTheme.typography.bodySmall)
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFFEF6797),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                uiState.popularCafes.forEach { cafe ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(MyInfoAction.ClickCafe(cafe.id)) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE2D2), Color(0xFFFFC9A9))))
                            )
                            Column {
                                Text(cafe.name, fontWeight = FontWeight.SemiBold)
                                Text("⭐ ${cafe.ratingAvg}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFFFEAF2), Color(0xFFFDE3F0))))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✨", style = MaterialTheme.typography.headlineMedium)
                    Text("지금 바로 시작하세요!", fontWeight = FontWeight.Bold)
                    Text(
                        "ConCafe 회원만의 특별한 혜택을 누려보세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7E7E7E)
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier.padding(top = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6797))
                    ) {
                        Text("회원가입하기", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class GuestFeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val startColor: Color,
    val endColor: Color
)

@Composable
private fun ProfileMyInfoScreen(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileSummaryCard(uiState = uiState)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val metricCards = myInfoMetricCards(uiState)
                metricCards.forEach { card ->
                    MyInfoMetricCard(
                        title = card.title,
                        value = card.value,
                        highlight = card.highlight
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("획득 배지", fontWeight = FontWeight.Bold)
                Text("${uiState.badges.count { it.unlocked }} / ${uiState.badges.size}", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.badges.forEach { badge ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (badge.unlocked) Color(0xFFEF6797) else Color(0xFFDADADA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(badge.icon)
                        }
                        Text(badge.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
        item {
            Text("최근 방문", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                items(uiState.recentVisits) { cafe ->
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .clickable { onAction(MyInfoAction.ClickCafe(cafe.id)) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFE2D2), Color(0xFFFFC9A9))))
                        )
                        Text(cafe.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            Text("즐겨찾기", fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .height(190.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                items(uiState.favorites.take(4)) { cafe ->
                    Card(
                        modifier = Modifier.clickable { onAction(MyInfoAction.ClickCafe(cafe.id)) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE2D2), Color(0xFFFFC9A9))))
                            )
                            Text(
                                cafe.name,
                                modifier = Modifier.padding(10.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        item {
            if (uiState.user?.role != UserRole.CAST) {
                Text("팔로우한 메이드", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(uiState.followedMaids.take(6)) { maid ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAction(MyInfoAction.ClickMaid(maid.id)) }) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFDFEA), Color(0xFFFFBED5))))
                            )
                            Text(maid.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(uiState: MyInfoUiState) {
    val user = uiState.user ?: return
    val castDetail = uiState.castDetail
    val ownerCafe = uiState.ownedCafes.firstOrNull()
    val title = when (user.role) {
        UserRole.CAST -> castDetail?.cast?.name ?: user.nickname
        else -> user.nickname
    }
    val subtitle = when (user.role) {
        UserRole.CAST -> castDetail?.let { detail ->
            val accountName = user.nickname.takeIf { it != detail.cast.name }.orEmpty()
            accountName
        }.orEmpty()
        UserRole.CAFE_OWNER -> "카페 운영자"
        UserRole.ADMIN -> "관리자 계정"
        UserRole.VISITOR -> "레벨 ${uiState.summary?.level ?: 1} · 열정적인 팬"
    }
    val accentText = when (user.role) {
        UserRole.CAST -> castDetail?.cafe?.name ?: "소속 카페 없음"
        UserRole.CAFE_OWNER -> ownerCafe?.name ?: "운영 카페 없음"
        UserRole.ADMIN -> "ConCafe 운영"
        UserRole.VISITOR -> "내 활동 요약"
    }
    val profileAccent = title.take(2).uppercase()
    val isHighlighted = when (user.role) {
        UserRole.CAST -> castDetail?.schedule?.isNotEmpty() == true
        UserRole.CAFE_OWNER -> ownerCafe != null
        UserRole.ADMIN -> true
        UserRole.VISITOR -> true
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD7E5), Color(0xFFF2ADC2))
                            )
                        )
                        .background(Color(0xFFFFD7E5))
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profileAccent,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3F67)
                    )
                }
                if (isHighlighted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF37B26C))
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF24161E)
                    )
                    if (subtitle.isNotBlank()) {
                        Box(modifier = Modifier.width(8.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A707A)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = Color(0xFFEF6797),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = accentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A57)
                    )
                }
            }
        }
    }
}

private data class MyInfoMetricCardModel(
    val title: String,
    val value: String,
    val highlight: Boolean
)

private fun myInfoMetricCards(uiState: MyInfoUiState): List<MyInfoMetricCardModel> {
    val user = uiState.user ?: return emptyList()

    return when (user.role) {
        UserRole.CAST -> {
            val cast = uiState.castDetail?.cast
            val scheduleCount = uiState.castDetail?.schedule?.size ?: 0
            listOf(
                MyInfoMetricCardModel("전체 팔로워", (cast?.followerCount ?: 0).toString(), false),
                MyInfoMetricCardModel("근무 일정", scheduleCount.toString(), true),
                MyInfoMetricCardModel("평점", ((cast?.rating ?: 0.0) * 10).toInt().div(10.0).toString(), false)
            )
        }
        UserRole.CAFE_OWNER -> {
            val cafeCount = uiState.ownedCafes.size
            val castCount = uiState.ownedCafes.sumOf { it.castCount }
            val rating = if (uiState.ownedCafes.isEmpty()) 0.0 else uiState.ownedCafes.map { it.rating }.average()
            listOf(
                MyInfoMetricCardModel("운영 카페", cafeCount.toString(), false),
                MyInfoMetricCardModel("소속 캐스트", castCount.toString(), true),
                MyInfoMetricCardModel("평균 평점", ((rating * 10).toInt() / 10.0).toString(), false)
            )
        }
        else -> {
            listOf(
                MyInfoMetricCardModel("방문 횟수", (uiState.summary?.totalVisits ?: 0).toString(), false),
                MyInfoMetricCardModel("즐겨찾기", (uiState.summary?.favoritesCount ?: 0).toString(), true),
                MyInfoMetricCardModel("팔로우", (uiState.summary?.followedCastsCount ?: 0).toString(), false)
            )
        }
    }
}

@Composable
private fun RowScope.MyInfoMetricCard(
    title: String,
    value: String,
    highlight: Boolean
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(20.dp),
        color = if (highlight) Color(0x1AFFD1DC) else Color.White.copy(alpha = 0.92f),
        tonalElevation = if (highlight) 0.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (highlight) Color(0x33FFB3C6) else Color(0x1AFFD1DC)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7A707A),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (highlight) Color(0xFFD94A82) else Color(0xFF24161E),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
