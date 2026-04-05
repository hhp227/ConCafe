package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import android.content.Context
import android.location.Geocoder
import org.koin.core.context.GlobalContext
import java.util.Locale

actual suspend fun resolveCafeAddress(query: String): CafeResolvedAddress? {
    val normalizedQuery = query.trim()

    if (normalizedQuery.isBlank()) {
        return null
    }

    val context = runCatching {
        GlobalContext.get().get<Context>()
    }.getOrNull() ?: return null
    return runCatching {
        val geocoder = Geocoder(context, Locale.KOREA)
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocationName(normalizedQuery, 1)
        val first = addresses?.firstOrNull() ?: return@runCatching null
        val fullAddress = first.getAddressLine(0)
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }
            ?: normalizedQuery

        CafeResolvedAddress(
            latitude = first.latitude,
            longitude = first.longitude,
            fullAddress = fullAddress
        )
    }.getOrNull()
}
