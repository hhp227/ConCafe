package com.hhp227.concafe.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.ContentLayout

/**
 * 홈 배너, 체크인 지도처럼 화면을 크게 차지하는 표시 영역의 배치 방식.
 * 두 화면이 같은 값을 공유하므로 여백/모서리 규칙도 여기에 함께 둔다.
 */
enum class AppContentLayout {
    FULL_BLEED,
    LEGACY
}

/**
 * 풀블리드는 화면 폭을 가득 채우고, 레거시는 좌우 여백을 둔 카드 형태를 유지한다.
 */
val AppContentLayout.horizontalPadding: Dp
    get() = when (this) {
        AppContentLayout.FULL_BLEED -> 0.dp
        AppContentLayout.LEGACY -> 16.dp
    }

/**
 * 풀블리드 표시 영역은 상단 바에 붙여야 하므로 화면 상단 여백을 없앤다.
 * 레거시 여백은 화면마다 다르므로 호출부에서 넘긴다.
 */
fun AppContentLayout.topPadding(legacyTopPadding: Dp): Dp {
    return when (this) {
        AppContentLayout.FULL_BLEED -> 0.dp
        AppContentLayout.LEGACY -> legacyTopPadding
    }
}

/**
 * 레거시에서만 [legacyCornerRadius]만큼 둥글게 자르고, 풀블리드는 각지게 둔다.
 */
fun AppContentLayout.shape(legacyCornerRadius: Dp): Shape {
    return when (this) {
        AppContentLayout.FULL_BLEED -> RectangleShape
        AppContentLayout.LEGACY -> RoundedCornerShape(legacyCornerRadius)
    }
}

fun ContentLayout.toPresentationContentLayout(): AppContentLayout {
    return when (this) {
        ContentLayout.FULL_BLEED -> AppContentLayout.FULL_BLEED
        ContentLayout.LEGACY -> AppContentLayout.LEGACY
    }
}

fun AppContentLayout.toDomainContentLayout(): ContentLayout {
    return when (this) {
        AppContentLayout.FULL_BLEED -> ContentLayout.FULL_BLEED
        AppContentLayout.LEGACY -> ContentLayout.LEGACY
    }
}
