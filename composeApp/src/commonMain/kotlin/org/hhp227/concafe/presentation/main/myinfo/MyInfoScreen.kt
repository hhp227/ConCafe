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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.Cast
import org.hhp227.concafe.presentation.navigation.NavigationAction
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
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is MyInfoEvent.NavigateToCafeDetail -> onNavigate(NavigationAction.NavigateToCafeDetail(event.id))
                is MyInfoEvent.NavigateToCastDetail -> onNavigate(NavigationAction.NavigateToCastDetail(event.id))
            }
        }
    }

    when {
        state.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        !state.isLoggedIn -> {
            GuestMyInfoScreen(
                cafes = state.popularCafes,
                onCafeClick = { id -> viewModel.onAction(MyInfoAction.ClickCafe(id)) }
            )
        }

        else -> {
            ProfileMyInfoScreen(
                state = state,
                onLogout = { viewModel.onAction(MyInfoAction.ClickLogout) },
                onCafeClick = { id -> viewModel.onAction(MyInfoAction.ClickCafe(id)) },
                onMaidClick = { id -> viewModel.onAction(MyInfoAction.ClickMaid(id)) }
            )
        }
    }
}

@Composable
private fun GuestMyInfoScreen(
    cafes: List<Cafe>,
    onCafeClick: (String) -> Unit
) {
    val features = listOf(
        Triple("📍", "체크인 기록", "방문한 카페를 기록해보세요"),
        Triple("❤️", "즐겨찾기", "좋아하는 카페와 메이드를 저장"),
        Triple("⭐", "배지 수집", "다양한 활동으로 배지 획득"),
        Triple("🎁", "멤버십 혜택", "이벤트와 할인 혜택 받기")
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
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("로그인하기", color = Color(0xFFEF6797), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("로그인 후 이용 가능한 기능", fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .height(220.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(features) { feature ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(feature.first)
                            Text(feature.second, fontWeight = FontWeight.SemiBold)
                            Text(feature.third, style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777))
                        }
                    }
                }
            }
        }

        item {
            Text("인기 카페 둘러보기", fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                cafes.forEach { cafe ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCafeClick(cafe.id) },
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
    }
}

@Composable
private fun ProfileMyInfoScreen(
    state: MyInfoUiState,
    onLogout: () -> Unit,
    onCafeClick: (String) -> Unit,
    onMaidClick: (String) -> Unit
) {
    val summary = state.summary
    val user = state.user

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
                        .padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(user?.nickname ?: "메이드러버", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("로그아웃", color = Color.White, modifier = Modifier.clickable { onLogout() })
                    }
                    Text("레벨 ${summary?.level ?: 1}", color = Color.White.copy(alpha = 0.9f))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Text("📍 ${summary?.totalVisits ?: 0}", color = Color.White)
                        Text("❤️ ${summary?.favoritesCount ?: 0}", color = Color.White)
                        Text("👥 ${summary?.followedCastsCount ?: 0}", color = Color.White)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("방문 횟수", summary?.totalVisits ?: 0)
                StatCard("즐겨찾기", summary?.favoritesCount ?: 0)
                StatCard("팔로우", summary?.followedCastsCount ?: 0)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("획득 배지", fontWeight = FontWeight.Bold)
                Text("${state.badges.count { it.unlocked }} / ${state.badges.size}", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.badges.forEach { badge ->
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
                items(state.recentVisits) { cafe ->
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .clickable { onCafeClick(cafe.id) }
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
                items(state.favorites.take(4)) { cafe ->
                    Card(
                        modifier = Modifier.clickable { onCafeClick(cafe.id) },
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
            Text("팔로우한 메이드", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                items(state.followedMaids.take(6)) { maid ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onMaidClick(maid.id) }) {
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

@Composable
private fun RowScope.StatCard(title: String, value: Int) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = Color(0xFFEF6797), fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777))
        }
    }
}
