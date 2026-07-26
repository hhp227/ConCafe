package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.data.source.local.CafeExternalLinkLocalStore
import com.hhp227.concafe.data.source.local.PersistedCafeExternalLink
import kotlinx.datetime.Clock

class CafeExternalLinkLocalUseCase(
    private val store: CafeExternalLinkLocalStore
) {
    fun load(cafeId: String): List<PersistedCafeExternalLink> {
        if (cafeId.isBlank()) return emptyList()
        return store.load(cafeId).sortedByDescending { it.updatedAt }
    }

    fun upsert(cafeId: String, linkId: String?, title: String, url: String): List<PersistedCafeExternalLink> {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        require(title.isNotBlank()) { "title is required" }
        require(url.isNotBlank()) { "url is required" }

        val now = Clock.System.now().toString()
        val current = store.load(cafeId).toMutableList()
        val trimmedTitle = title.trim()
        val trimmedUrl = url.trim()
        val targetId = linkId?.trim()?.takeIf { it.isNotEmpty() }
        val updated = if (targetId == null) {
            val created = PersistedCafeExternalLink(
                id = "external-link-${Clock.System.now().toEpochMilliseconds()}",
                title = trimmedTitle,
                url = trimmedUrl,
                createdAt = now,
                updatedAt = now
            )
            mutableListOf(created).apply { addAll(current.filterNot { it.id == created.id }) }
        } else {
            val existing = current.firstOrNull { it.id == targetId }
            val merged = PersistedCafeExternalLink(
                id = targetId,
                title = trimmedTitle,
                url = trimmedUrl,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            mutableListOf(merged).apply { addAll(current.filterNot { it.id == targetId }) }
        }
        val normalized = updated.sortedByDescending { it.updatedAt }

        store.save(cafeId, normalized)
        return normalized
    }

    fun delete(cafeId: String, linkId: String): List<PersistedCafeExternalLink> {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        require(linkId.isNotBlank()) { "linkId is required" }

        val next = store.load(cafeId).filterNot { it.id == linkId }.sortedByDescending { it.updatedAt }

        store.save(cafeId, next)
        return next
    }
}
