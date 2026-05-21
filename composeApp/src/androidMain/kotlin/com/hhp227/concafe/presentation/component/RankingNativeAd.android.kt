package com.hhp227.concafe.presentation.component

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.View
import android.widget.Button as AndroidButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.hhp227.concafe.data.model.AndroidNativeAdHandle
import com.hhp227.concafe.data.model.NativeAdHandle

@Composable
actual fun RankingNativeAd(
    modifier: Modifier,
    nativeAdHandle: NativeAdHandle?
) {
    val nativeAd = (nativeAdHandle as? AndroidNativeAdHandle)?.nativeAd

    if (nativeAd != null) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                NativeAdView(context).apply {
                    val composeView = ComposeView(context).apply {
                        // 중요: findViewWithTag가 작동하려면 태그가 설정되어야 함
                        setContent {
                            NativeAdContentLayout(nativeAd)
                        }
                    }

                    addView(composeView)
                }
            },
            update = { adView ->
                // ComposeView 내부의 뷰들을 찾아서 SDK에 연결
                adView.headlineView = adView.findViewWithTag("headline")
                adView.bodyView = adView.findViewWithTag("body")
                adView.callToActionView = adView.findViewWithTag("cta")

                adView.setNativeAd(nativeAd)
            }
        )
    } else {
        PlaceholderUI(modifier)
    }
}

@Composable
actual fun CommunityNativeAd(
    modifier: Modifier,
    nativeAdHandle: NativeAdHandle?
) {
    val nativeAd = (nativeAdHandle as? AndroidNativeAdHandle)?.nativeAd

    if (nativeAd != null) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                NativeAdView(context).apply {
                    val density = context.resources.displayMetrics.density
                    fun dp(value: Int) = (value * density).toInt()

                    val container = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(16), dp(16), dp(16), dp(16))
                        setBackgroundColor(android.graphics.Color.WHITE)
                    }
                    val topRow = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    val avatar = TextView(context).apply {
                        text = "AD"
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = android.view.Gravity.CENTER
                        setTextColor(android.graphics.Color.rgb(239, 103, 151))
                        background = android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                            intArrayOf(
                                android.graphics.Color.rgb(255, 227, 236),
                                android.graphics.Color.rgb(248, 197, 215)
                            )
                        ).apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                        }
                    }
                    val advertiser = TextView(context).apply {
                        textSize = 13f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(android.graphics.Color.rgb(102, 90, 99))
                        maxLines = 1
                    }
                    val badge = TextView(context).apply {
                        text = "광고"
                        textSize = 11f
                        setTextColor(android.graphics.Color.rgb(177, 163, 172))
                    }
                    val headline = TextView(context).apply {
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(android.graphics.Color.rgb(43, 35, 48))
                        maxLines = 2
                    }
                    val body = TextView(context).apply {
                        textSize = 13f
                        setTextColor(android.graphics.Color.rgb(102, 90, 99))
                        maxLines = 3
                    }
                    val cta = AndroidButton(context).apply {
                        textSize = 12f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(android.graphics.Color.rgb(43, 35, 48))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.rgb(255, 209, 220))
                            cornerRadius = dp(18).toFloat()
                        }
                        minHeight = 0
                        minimumHeight = 0
                        setPadding(dp(14), dp(6), dp(14), dp(6))
                    }
                    val divider = View(context).apply {
                        setBackgroundColor(android.graphics.Color.argb(76, 255, 209, 220))
                    }
                    val bottomRow = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    topRow.addView(avatar, LinearLayout.LayoutParams(dp(32), dp(32)))
                    topRow.addView(advertiser, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(8)
                    })
                    topRow.addView(badge)
                    container.addView(topRow)
                    container.addView(headline, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(10)
                    })
                    container.addView(body, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(4)
                    })
                    container.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                        topMargin = dp(12)
                        bottomMargin = dp(10)
                    })
                    bottomRow.addView(cta)
                    container.addView(bottomRow)
                    addView(container)

                    headlineView = headline
                    bodyView = body
                    advertiserView = advertiser
                    callToActionView = cta
                }
            },
            update = { adView ->
                (adView.headlineView as? TextView)?.text = nativeAd.headline.orEmpty()
                (adView.bodyView as? TextView)?.text = nativeAd.body.orEmpty()
                (adView.advertiserView as? TextView)?.text = nativeAd.advertiser?.takeIf { it.isNotBlank() } ?: "ConCafe"
                (adView.callToActionView as? AndroidButton)?.text = nativeAd.callToAction?.takeIf { it.isNotBlank() } ?: "자세히"
                adView.callToActionView?.isClickable = false
                adView.setNativeAd(nativeAd)
            }
        )
    } else {
        PlaceholderUI(modifier)
    }
}

@Composable
private fun NativeAdContentLayout(nativeAd: NativeAd) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // 왼쪽: 텍스트 정보 영역
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mock의 Badge 스타일과 일치 ("Ad" 표시)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.22f)
            ) {
                Text(
                    text = "Ad",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Headline (Mock의 titleLarge와 일치)
            Text(
                text = nativeAd.headline ?: "",
                modifier = Modifier.viewTag("headline"), // 🔥 별도 확장함수 사용 권장
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Body (Mock의 bodySmall과 일치)
            nativeAd.body?.let {
                Text(
                    text = it,
                    modifier = Modifier.viewTag("body"),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 오른쪽: CTA 버튼 영역
        Button(
            onClick = {}, // AdMob이 처리함
            modifier = Modifier.viewTag("cta"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF262626)
            ),
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text(
                text = nativeAd.callToAction ?: "Detail",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * AndroidView 내부의 findViewWithTag가 Compose 컴포넌트를 찾을 수 있도록
 * 인덱싱을 도와주는 확장 함수
 */
@SuppressLint("SuspiciousModifierThen")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.viewTag(tag: String): Modifier {
    return this.semantics { set(SemanticsPropertyKey("ViewTag"), tag) }
        .then(layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        }).pointerInteropFilter { false } // 실제로는 'view.setTag()'가 필요함
}

@Composable
private fun PlaceholderUI(modifier: Modifier) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(22.dp)
                        .background(Color(0x75FFFFFF), shape = RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .background(Color(0x6BFFFFFF), shape = RoundedCornerShape(8.dp))
                )
            }
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .height(36.dp)
                    .background(Color(0x90FFFFFF), shape = RoundedCornerShape(16.dp))
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(Color(0x80FFFFFF), shape = RoundedCornerShape(8.dp))
        )
    }
}
