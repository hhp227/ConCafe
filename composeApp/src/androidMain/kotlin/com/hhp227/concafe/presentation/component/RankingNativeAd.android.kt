package com.hhp227.concafe.presentation.component

import android.content.pm.ApplicationInfo
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

private const val RANKING_NATIVE_AD_UNIT_ID = "ca-app-pub-6216021268300256/6596242282"

private const val RANKING_NATIVE_TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

@Composable
actual fun RankingNativeAd(
    modifier: Modifier
) {
    val context = LocalContext.current
    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            loadedNativeAd?.destroy()
        }
    }
    LaunchedEffect(Unit) {
        if (!isLoading) {
            isLoading = true

            val adLoader = AdLoader.Builder(
                context,
                if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    RANKING_NATIVE_TEST_AD_UNIT_ID
                } else {
                    RANKING_NATIVE_AD_UNIT_ID
                }
            )
                .forNativeAd { nativeAd ->
                    loadedNativeAd?.destroy()
                    loadedNativeAd = nativeAd
                    isLoading = false
                }
                .withNativeAdOptions(
                    NativeAdOptions.Builder().build()
                )
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isLoading = false
                        }
                    }
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }
    if (loadedNativeAd != null) {
        AndroidView(
            modifier = modifier,
            factory = { viewContext ->
                createRankingNativeAdView(viewContext)
            },
            update = { nativeAdView ->
                bindRankingNativeAd(nativeAdView, loadedNativeAd!!)
            }
        )
    } else {
        Column(
            modifier = modifier
                .background(Color.White)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.24f)
                    .height(20.dp)
                    .background(Color(0xFFFFE9F1), shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(22.dp)
                    .background(Color(0xFFF2EDF1), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(16.dp)
                    .background(Color(0xFFF2EDF1), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .height(16.dp)
                    .background(Color(0xFFF2EDF1), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.BottomStart) {
                Text(
                    text = "Ad",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9A8D95)
                )
            }
        }
    }
}

private fun createRankingNativeAdView(context: android.content.Context): NativeAdView {
    val root = NativeAdView(context)
    val container = LinearLayout(context)
    val adBadge = TextView(context)
    val headline = TextView(context)
    val body = TextView(context)
    val cta = Button(context)
    val media = MediaView(context)

    root.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    )

    container.orientation = LinearLayout.VERTICAL
    container.setPadding(24, 24, 24, 24)
    container.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    )

    adBadge.text = "Ad"
    adBadge.textSize = 12f
    adBadge.setTextColor("#B74D73".toColorInt())
    adBadge.setPadding(16, 8, 16, 8)
    adBadge.setBackgroundColor("#FFE9F1".toColorInt())
    adBadge.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    headline.textSize = 20f
    headline.setTextColor("#2B2330".toColorInt())
    headline.setPadding(0, 16, 0, 8)

    body.textSize = 13f
    body.setTextColor("#6F6670".toColorInt())

    media.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0
    ).apply {
        weight = 1f
        topMargin = 16
        bottomMargin = 16
    }

    cta.textSize = 14f
    cta.setTextColor("#2B2330".toColorInt())
    cta.setBackgroundColor("#FFD1DC".toColorInt())

    container.addView(adBadge)
    container.addView(headline)
    container.addView(body)
    container.addView(media)
    container.addView(cta)

    root.addView(container)
    root.headlineView = headline
    root.bodyView = body
    root.callToActionView = cta
    root.mediaView = media

    return root
}

private fun bindRankingNativeAd(
    nativeAdView: NativeAdView,
    nativeAd: NativeAd
) {
    val headlineView = nativeAdView.headlineView as? TextView
    val bodyView = nativeAdView.bodyView as? TextView
    val callToActionView = nativeAdView.callToActionView as? Button
    val mediaView = nativeAdView.mediaView

    headlineView?.text = nativeAd.headline
    bodyView?.text = nativeAd.body ?: ""

    if (nativeAd.body.isNullOrBlank()) {
        bodyView?.visibility = View.GONE
    } else {
        bodyView?.visibility = View.VISIBLE
    }

    callToActionView?.text = nativeAd.callToAction ?: "Detail"

    if (nativeAd.callToAction.isNullOrBlank()) {
        callToActionView?.visibility = View.GONE
    } else {
        callToActionView?.visibility = View.VISIBLE
    }

    mediaView?.mediaContent = nativeAd.mediaContent
    nativeAdView.setNativeAd(nativeAd)
}
