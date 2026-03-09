package org.hhp227.concafe.presentation.main.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CheckInScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFD)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { CheckInSectionTitle("오늘의 방문", "3월 9일") }
        item { TodayVisitsRow() }
        item { CheckInButton() }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { CheckInSectionTitle("최근 타임라인", "🕘") }
        item { TimelineList() }
    }
}

@Composable
fun CheckInSectionTitle(
    title: String,
    trailing: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(trailing)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun TodayVisitsRow() {
    val todayVisits = listOf(
        CheckInVisitCardUi(
            id = "today-1",
            name = "모닝 브루",
            time = "오전 8:30"
        ),
        CheckInVisitCardUi(
            id = "today-2",
            name = "스위트 빈즈",
            time = "오후 1:15"
        ),
        CheckInVisitCardUi(
            id = "today-3",
            name = "블루 리본 카페",
            time = "오후 6:40"
        ),
        CheckInVisitCardUi(
            id = "today-4",
            name = "체리 블룸",
            time = "오후 8:10"
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (todayVisits.size) {
            0 -> Unit
            1 -> {
                VisitCard(
                    name = todayVisits[0].name,
                    time = todayVisits[0].time,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            2 -> {
                todayVisits.forEach { visit ->
                    VisitCard(
                        name = visit.name,
                        time = visit.time,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            3 -> {
                todayVisits.forEach { visit ->
                    VisitCard(
                        name = visit.name,
                        time = visit.time,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            else -> {
                VisitCard(
                    name = todayVisits[0].name,
                    time = todayVisits[0].time,
                    modifier = Modifier.weight(1f)
                )
                VisitCard(
                    name = todayVisits[1].name,
                    time = todayVisits[1].time,
                    modifier = Modifier.weight(1f)
                )
                MoreVisitCard(
                    remainingCount = todayVisits.size - 2,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VisitCard(
    name: String,
    time: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .height(200.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFE2D2), Color(0xFFFFC9A9))
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(time, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MoreVisitCard(
    remainingCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF1F6), Color(0xFFFFE1EC))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+$remainingCount",
                    color = Color(0xFFEF6797),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "더 방문했어요",
                    color = Color(0xFF7C7480),
                    fontSize = 13.sp
                )
            }
        }
    }
}

private data class CheckInVisitCardUi(
    val id: String,
    val name: String,
    val time: String
)

@Composable
fun CheckInButton() {
    Button(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 32.dp)
            .height(70.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8A0C2).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.DarkGray)
        Spacer(modifier = Modifier.width(8.dp))
        Text("새 방문 체크인", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun TimelineList() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        TimelineItem("어반 로스트", "어제", "분위기가 좋았고 바닐라 라떼가 정말 훌륭했어요.", 4)
        TimelineItem("더 케이크 팔러", "2일 전", "레드벨벳 케이크가 정말 맛있었어요!", 5)
    }
}

@Composable
fun TimelineItem(title: String, time: String, review: String, stars: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF8A0C2)),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = Color.DarkGray
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(100.dp)
                    .background(Color(0xFFF8A0C2).copy(alpha = 0.3f))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp)) {
                        Text(time, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(review, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    repeat(5) { index ->
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < stars) Color(0xFFFFD700) else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
