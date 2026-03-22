package com.hhp227.concafe.data.source.local

import kotlinx.serialization.Serializable

@Serializable
data class PersistedCafeExternalLink(
    val id: String,
    val title: String,
    val url: String,
    val createdAt: String,
    val updatedAt: String
)

interface CafeExternalLinkLocalStore {
    fun load(cafeId: String): List<PersistedCafeExternalLink>

    fun save(cafeId: String, links: List<PersistedCafeExternalLink>)
}
