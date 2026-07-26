package com.hhp227.concafe.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ConCafe 로딩 스켈레톤 쉬머 이펙트.
 *
 * 콘텐츠 로딩 중 프로그레스 인디케이터 대신 화면 레이아웃을 모방한
 * 스켈레톤 위로 하이라이트 밴드가 흐르는 쉬머를 표시한다.
 * 색상은 ConCafeColors 시맨틱 토큰만 사용한다 (iOS Shimmer.swift와 1:1 유지).
 */
private const val SHIMMER_DURATION_MILLIS = 1200

private val ShimmerTravel = 600.dp

private val ShimmerBand = 240.dp

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    val density = LocalDensity.current
    val travelPx = with(density) { ShimmerTravel.toPx() }
    val bandPx = with(density) { ShimmerBand.toPx() }
    var originX by remember { mutableStateOf(0f) }
    // 루트 좌표 기준으로 밴드 시작점을 보정해 화면 전체 스켈레톤이 하나의 밴드로 이어져 보이게 한다.
    val startX = -bandPx + (travelPx + bandPx * 2f) * progress - originX
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            ConCafeColors.outlineStrong.copy(alpha = 0.4f),
            Color.Transparent
        ),
        start = Offset(startX, 0f),
        end = Offset(startX + bandPx, bandPx * 0.4f)
    )

    Box(
        modifier = modifier
            .onGloballyPositioned { originX = it.positionInRoot().x }
            .clip(shape)
            .background(ConCafeColors.outline)
            .background(brush)
    )
}

@Composable
fun ShimmerListItemSkeleton(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 48.dp,
    isAvatarCircular: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier.size(avatarSize),
            shape = if (isAvatarCircular) CircleShape else RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp)
            )
            Spacer(Modifier.height(8.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp)
            )
        }
    }
}

@Composable
fun ShimmerListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    avatarSize: Dp = 48.dp,
    isAvatarCircular: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(itemCount) {
            ShimmerListItemSkeleton(
                avatarSize = avatarSize,
                isAvatarCircular = isAvatarCircular
            )
        }
    }
}

@Composable
fun ShimmerCardListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3,
    imageHeight: Dp = 160.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        repeat(itemCount) {
            Column {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(12.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                )
                Spacer(Modifier.height(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                )
            }
        }
    }
}

@Composable
fun ShimmerCardGridSkeleton(
    modifier: Modifier = Modifier,
    columnCount: Int = 2,
    rowCount: Int = 3,
    imageHeight: Dp = 120.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(columnCount) {
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerFormSkeleton(
    modifier: Modifier = Modifier,
    fieldCount: Int = 6
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(fieldCount) {
            Column {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
