package com.hhp227.concafe.presentation.component

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(22.dp)
                            .background(Color(0x75FFFFFF), shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .background(Color(0x6BFFFFFF), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    )
                }
                Box(
                    modifier = Modifier
                        .width(74.dp)
                        .height(36.dp)
                        .background(Color(0x90FFFFFF), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(Color(0x80FFFFFF), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(16.dp)
                    .background(Color(0x73FFFFFF), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .background(Color(0x73FFFFFF), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
                Text(
                    text = "Ad",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7E5A6E)
                )
            }
        }
    }
}

private fun createRankingNativeAdView(context: Context): NativeAdView {
    val root = NativeAdView(context)
    val container = LinearLayout(context)
    val topRow = LinearLayout(context)
    val metaStack = LinearLayout(context)
    val adBadge = TextView(context)
    val sponsor = TextView(context)
    val headline = TextView(context)
    val body = TextView(context)
    val cta = Button(context)

    root.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    container.orientation = LinearLayout.VERTICAL
    container.setPadding(20.dp(context), 20.dp(context), 20.dp(context), 20.dp(context))
    container.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    root.addView(container)

    topRow.orientation = LinearLayout.HORIZONTAL
    topRow.gravity = Gravity.TOP
    topRow.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    metaStack.orientation = LinearLayout.VERTICAL
    metaStack.layoutParams = LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        1f
    )

    adBadge.text = "Ad"
    adBadge.textSize = 12f
    adBadge.setTextColor("#B74D73".toColorInt())
    adBadge.setPadding(8.dp(context), 4.dp(context), 8.dp(context), 4.dp(context))
    adBadge.background = roundedDrawable("#FFE9F1", 12f, context)
    adBadge.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    sponsor.text = "Ad provided by ConCafe"
    sponsor.textSize = 11f
    sponsor.setTextColor("#927D8A".toColorInt())
    sponsor.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = 6.dp(context)
    }

    headline.textSize = 20f
    headline.setTextColor("#2B2330".toColorInt())
    headline.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = 12.dp(context)
    }

    body.textSize = 13f
    body.setTextColor("#6F6670".toColorInt())
    body.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = 4.dp(context)
    }

    cta.textSize = 14f
    cta.isAllCaps = false
    cta.setTextColor("#2B2330".toColorInt())
    cta.background = roundedDrawable("#FFD1DC", 16f, context)
    cta.setPadding(16.dp(context), 8.dp(context), 16.dp(context), 8.dp(context))
    cta.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    metaStack.addView(adBadge)
    metaStack.addView(sponsor)
    topRow.addView(metaStack)
    topRow.addView(cta)

    container.addView(topRow)
    container.addView(headline)
    container.addView(body)
    root.headlineView = headline
    root.bodyView = body
    root.advertiserView = sponsor
    root.callToActionView = cta

    return root
}

private fun bindRankingNativeAd(
    nativeAdView: NativeAdView,
    nativeAd: NativeAd
) {
    val headlineView = nativeAdView.headlineView as? TextView
    val bodyView = nativeAdView.bodyView as? TextView
    val advertiserView = nativeAdView.advertiserView as? TextView
    val callToActionView = nativeAdView.callToActionView as? Button

    headlineView?.text = nativeAd.headline
    bodyView?.text = nativeAd.body ?: ""
    advertiserView?.text = nativeAd.advertiser?.takeIf { it.isNotBlank() } ?: "Ad provided by ConCafe"

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
    callToActionView?.isEnabled = false
    nativeAdView.setNativeAd(nativeAd)
}

private fun Int.dp(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

private fun roundedDrawable(colorHex: String, radiusDp: Float, context: Context): GradientDrawable {
    return GradientDrawable().apply {
        setColor(colorHex.toColorInt())
        cornerRadius = radiusDp * context.resources.displayMetrics.density
    }
}
