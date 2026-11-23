package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.maid_logo
import org.jetbrains.compose.resources.painterResource

/**
 * 캐스트 목록용 프로필 이미지.
 * 프로필 이미지 → 소속 카페 썸네일 → 기본 로고(maid_logo) 순서로 표시한다.
 */
@Composable
fun CastProfileImage(
    profileImageUrl: String?,
    cafeImageUrl: String?,
    modifier: Modifier = Modifier,
    displaySize: ImageDisplaySize = ImageDisplaySize.THUMBNAIL
) {
    val imageUrl = resolveCastListImageUrl(profileImageUrl, cafeImageUrl)

    if (imageUrl != null) {
        CompatImageDisplay(
            imageUrl = imageUrl,
            modifier = modifier,
            applyRoundedClip = false,
            displaySize = displaySize
        )
    } else {
        Image(
            painter = painterResource(Res.drawable.maid_logo),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

fun resolveCastListImageUrl(profileImageUrl: String?, cafeImageUrl: String?): String? {
    return profileImageUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: cafeImageUrl?.trim()?.takeIf { it.isNotEmpty() }
}
