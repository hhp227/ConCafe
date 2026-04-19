package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.data.source.RankingDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.model.CastSort
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreRankingDataSource(
    private val config: FirestoreConfig,
    private val restApi: FirestoreRestApi,
    private val castRemoteDataSource: com.hhp227.concafe.data.source.CastRemoteDataSource
) : RankingDataSource {
    override suspend fun rankingItemsFromCasts(period: RankingPeriod, country: String?, city: String?): List<RankingItem> {
        return loadRankingItemsRemote(kind = RANKING_KIND_CAST, period = period, country = country, city = city)
    }

    override suspend fun rankingItemsFromCafes(period: RankingPeriod, country: String?, city: String?): List<RankingItem> {
        return loadRankingItemsRemote(kind = RANKING_KIND_CAFE, period = period, country = country, city = city)
    }

    override fun homePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast> {
        return runBlocking {
            castRemoteDataSource.searchCastsRemote(
                query = null,
                country = null,
                city = null,
                sort = CastSort.HOME_LINKED_FIRST,
                cursor = cursor,
                pageSize = pageSize
            )
        }
    }

    private suspend fun loadRankingItemsRemote(
        kind: String,
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        val rankingId = rankingDocumentId(kind = kind, period = period, country = country, city = city)
        val path = "${config.documentBasePath()}/${FirestorePaths.RANKINGS}/$rankingId"
        val response = runCatching {
            restApi.get(path, null)
        }.getOrElse { throwable ->
            if (throwable.isFirestoreNotFound()) return emptyList()
            throw IllegalStateException("failed to load ranking document: $rankingId", throwable)
        }
        val document = Json.parseToJsonElement(response).jsonObject
        return parseRankingItemsDocument(document).sortedBy { item -> item.rank }
    }

    private fun parseRankingItemsDocument(document: JsonObject): List<RankingItem> {
        val fields = document["fields"]?.jsonObject ?: return emptyList()
        val entries = fields["entries"]?.jsonObject?.get("arrayValue")?.jsonObject?.get("values")?.jsonArray.orEmpty()
        return entries.mapNotNull { element ->
            val entryFields = element.jsonObject["mapValue"]?.jsonObject?.get("fields")?.jsonObject ?: return@mapNotNull null
            val id = entryFields.getFirestoreString("id") ?: return@mapNotNull null
            RankingItem(
                id = id,
                name = entryFields.getFirestoreString("name").orEmpty(),
                subtitle = entryFields.getFirestoreString("subtitle").orEmpty(),
                score = entryFields.getFirestoreInt("score"),
                rank = entryFields.getFirestoreInt("rank"),
                change = entryFields.getFirestoreString("change") ?: "0",
                imageUrl = entryFields.getFirestoreString("imageUrl")
            )
        }
    }

    private fun rankingDocumentId(kind: String, period: RankingPeriod, country: String?, city: String?): String {
        val normalizedCountry = country?.trim().orEmpty()
        val normalizedCity = city?.trim().orEmpty()
        val regionToken = if (normalizedCountry.isEmpty() || normalizedCountry.equals("all", ignoreCase = true)) {
            "all_all"
        } else {
            val countryToken = sanitizeDocumentIdPart(normalizedCountry)
            val cityToken = if (normalizedCity.isEmpty() || normalizedCity.equals("all", ignoreCase = true)) {
                "all"
            } else {
                sanitizeDocumentIdPart(normalizedCity)
            }
            "${countryToken}_${cityToken}"
        }
        return "${kind}_${period.name.lowercase()}_$regionToken"
    }

    private fun sanitizeDocumentIdPart(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "all"
        val builder = StringBuilder(trimmed.length)
        for (ch in trimmed.lowercase()) {
            if (ch.isLetterOrDigit()) builder.append(ch) else builder.append('_')
        }
        return builder.toString().trim('_').ifEmpty { "all" }
    }

    private fun JsonObject.getFirestoreString(key: String): String? {
        val value = this[key]?.jsonObject ?: return null
        return value["stringValue"]?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.getFirestoreInt(key: String): Int {
        val value = this[key]?.jsonObject ?: return 0
        val longValue = value["integerValue"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        if (longValue != null) return longValue.toInt()
        val doubleValue = value["doubleValue"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        return doubleValue?.toInt() ?: 0
    }

    private fun Throwable.isFirestoreNotFound(): Boolean {
        val message = message.orEmpty()
        return message.contains("404") || message.contains("NOT_FOUND", ignoreCase = true)
    }

    private val JsonPrimitive.contentOrNull: String?
        get() = runCatching { content }.getOrNull()

    private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())

    private companion object {
        const val RANKING_KIND_CAST = "cast"
        const val RANKING_KIND_CAFE = "cafe"
    }
}
