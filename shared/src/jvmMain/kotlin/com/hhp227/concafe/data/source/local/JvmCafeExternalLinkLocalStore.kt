package com.hhp227.concafe.data.source.local

import java.util.prefs.Preferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class JvmCafeExternalLinkLocalStore : CafeExternalLinkLocalStore {
    private val preferences: Preferences = Preferences.userRoot().node(PREF_NODE)

    override fun load(cafeId: String): List<PersistedCafeExternalLink> {
        val raw = preferences.get(key(cafeId), null) ?: return emptyList()
        return runCatching<List<PersistedCafeExternalLink>> {
            Json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                PersistedCafeExternalLink(
                    id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    title = obj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    url = obj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    createdAt = obj["createdAt"]?.jsonPrimitive?.content ?: "",
                    updatedAt = obj["updatedAt"]?.jsonPrimitive?.content ?: ""
                )
            }
        }.getOrElse { emptyList() }
    }

    override fun save(cafeId: String, links: List<PersistedCafeExternalLink>) {
        val raw = buildJsonArray {
            links.forEach { link ->
                add(
                    buildJsonObject {
                        put("id", JsonPrimitive(link.id))
                        put("title", JsonPrimitive(link.title))
                        put("url", JsonPrimitive(link.url))
                        put("createdAt", JsonPrimitive(link.createdAt))
                        put("updatedAt", JsonPrimitive(link.updatedAt))
                    }
                )
            }
        }.toString()
        preferences.put(key(cafeId), raw)
    }

    private fun key(cafeId: String): String {
        return "$KEY_PREFIX$cafeId"
    }
}

private const val PREF_NODE = "com.hhp227.concafe.external.links"
private const val KEY_PREFIX = "concafe.external.links."
