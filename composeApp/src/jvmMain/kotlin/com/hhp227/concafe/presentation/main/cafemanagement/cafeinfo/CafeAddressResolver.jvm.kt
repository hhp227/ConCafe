package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

actual suspend fun resolveCafeAddress(query: String): CafeResolvedAddress? {
    val normalizedQuery = query.trim()

    if (!normalizedQuery.isBlank()) {
        val encodedQuery = URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8.name())
        val url = URL("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=$encodedQuery")
        return runCatching {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ConCafeDesktop/1.0")
                connectTimeout = 7000
                readTimeout = 7000
            }
            val body = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
            val first = Json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject ?: return@runCatching null
            val latitude = first["lat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@runCatching null
            val longitude = first["lon"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@runCatching null
            val displayName = first["display_name"]?.jsonPrimitive?.content?.trim().orEmpty()
            val fullAddress = if (displayName.isBlank()) normalizedQuery else displayName
            CafeResolvedAddress(
                latitude = latitude,
                longitude = longitude,
                fullAddress = fullAddress
            )
        }.getOrNull()
    } else {
        return null
    }
}
