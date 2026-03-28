package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.BannerDataSource
import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeBannerCreate
import com.hhp227.concafe.domain.repository.BannerRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class BannerRepositoryImpl(
    private val bannerDataSource: BannerDataSource,
    private val cafeDataSource: CafeDataSource,
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : BannerRepository {
    override suspend fun getAllHomeBanners(): List<HomeBanner> {
        normalizeBannerSlots()
        return bannerDataSource.banners.toList()
    }

    override suspend fun getHomeBanners(limit: Int): List<HomeBanner> {
        val firestoreDataSource = bannerDataSource as? FirestoreConCafeDataSource

        firestoreDataSource?.refreshHomeBanners()
        normalizeBannerSlots()
        return bannerDataSource.banners
            .asSequence()
            .filter { it.statusLabel == STATUS_ACTIVE }
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    override suspend fun createHomeBanner(input: HomeBannerCreate): HomeBanner {
        if (input.title.isBlank()) throw IllegalArgumentException("banner title is required")
        if (input.subtitle.isBlank()) throw IllegalArgumentException("banner subtitle is required")
        if (input.targetValue.isBlank()) throw IllegalArgumentException("banner target is required")
        if (input.displayDays !in 1..10) throw IllegalArgumentException("displayDays must be between 1 and 10")

        normalizeBannerSlots()
        val palette = resolvePalette(input.targetType)
        val now = Clock.System.now().toEpochMilliseconds()
        val activeCount = bannerDataSource.banners.count { it.statusLabel == STATUS_ACTIVE }
        val statusLabel = if (activeCount >= ACTIVE_BANNER_LIMIT) STATUS_SCHEDULED else STATUS_ACTIVE
        val created = HomeBanner(
            id = nextEntityId("banner"),
            title = input.title.trim(),
            subtitle = input.subtitle.trim(),
            startColorHex = palette.first,
            endColorHex = palette.second,
            cafeId = input.cafeId,
            imageUrl = input.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            targetType = input.targetType,
            targetValue = input.targetValue.trim(),
            displayDays = input.displayDays,
            statusLabel = statusLabel,
            createdAtEpochMillis = now,
            activatedAtEpochMillis = if (statusLabel == STATUS_ACTIVE) now else 0L
        )
        val firestoreDataSource = bannerDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            firestoreSyncDataSource.pushHomeBanner(created)
            firestoreSyncDataSource.refreshHomeBanners()
            rebuildCafeHomeBannerPreviewCache()
            return bannerDataSource.banners.firstOrNull { banner -> banner.id == created.id } ?: created
        } else {
            bannerDataSource.banners.add(0, created)
            rebuildCafeHomeBannerPreviewCache()
        }
        return created
    }

    override suspend fun updateHomeBanner(bannerId: String, input: HomeBannerCreate): HomeBanner {
        if (input.title.isBlank()) throw IllegalArgumentException("banner title is required")
        if (input.subtitle.isBlank()) throw IllegalArgumentException("banner subtitle is required")
        if (input.targetValue.isBlank()) throw IllegalArgumentException("banner target is required")
        if (input.displayDays !in 1..10) throw IllegalArgumentException("displayDays must be between 1 and 10")

        normalizeBannerSlots()
        val index = bannerDataSource.banners.indexOfFirst { it.id == bannerId }
        if (index == -1) {
            throw NoSuchElementException("banner not found")
        }

        val existing = bannerDataSource.banners[index]
        val palette = resolvePalette(input.targetType)
        val updated = existing.copy(
            title = input.title.trim(),
            subtitle = input.subtitle.trim(),
            startColorHex = palette.first,
            endColorHex = palette.second,
            cafeId = input.cafeId,
            imageUrl = input.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            targetType = input.targetType,
            targetValue = input.targetValue.trim(),
            displayDays = input.displayDays
        )
        val firestoreDataSource = bannerDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            firestoreSyncDataSource.pushHomeBanner(updated)
            firestoreSyncDataSource.refreshHomeBanners()
            rebuildCafeHomeBannerPreviewCache()
            return bannerDataSource.banners.firstOrNull { banner -> banner.id == bannerId } ?: updated
        } else {
            bannerDataSource.banners[index] = updated
            rebuildCafeHomeBannerPreviewCache()
        }
        return updated
    }

    override suspend fun deleteHomeBanner(bannerId: String): HomeBanner {
        normalizeBannerSlots()
        val index = bannerDataSource.banners.indexOfFirst { it.id == bannerId }
        if (index == -1) {
            throw NoSuchElementException("banner not found")
        }

        val deleted = bannerDataSource.banners[index]
        val firestoreDataSource = bannerDataSource as? FirestoreConCafeDataSource

        if (firestoreDataSource != null) {
            firestoreSyncDataSource.deleteHomeBanner(bannerId)
            firestoreSyncDataSource.refreshHomeBanners()
            rebuildCafeHomeBannerPreviewCache()
            return deleted
        }

        bannerDataSource.banners.removeAt(index)
        normalizeBannerSlots()
        rebuildCafeHomeBannerPreviewCache()
        return deleted
    }

    private fun normalizeBannerSlots() {
        val now = Clock.System.now().toEpochMilliseconds()
        val expiredIds = bannerDataSource.banners
            .filter { it.statusLabel == STATUS_ACTIVE && it.isExpired(now) }
            .map { it.id }
            .toSet()

        if (expiredIds.isNotEmpty()) {
            bannerDataSource.banners.indices.forEach { index ->
                val banner = bannerDataSource.banners[index]
                if (banner.id in expiredIds) {
                    bannerDataSource.banners[index] = banner.copy(statusLabel = STATUS_ENDED)
                }
            }
        }

        val activeCount = bannerDataSource.banners.count { it.statusLabel == STATUS_ACTIVE }
        if (activeCount >= ACTIVE_BANNER_LIMIT) return

        val scheduledBanners = bannerDataSource.banners
            .filter { it.statusLabel == STATUS_SCHEDULED }
            .sortedBy { it.createdAtEpochMillis }

        val activateIds = scheduledBanners
            .take((ACTIVE_BANNER_LIMIT - activeCount).coerceAtLeast(0))
            .map { it.id }
            .toSet()

        if (activateIds.isEmpty()) return

        bannerDataSource.banners.indices.forEach { index ->
            val banner = bannerDataSource.banners[index]
            if (banner.id in activateIds) {
                bannerDataSource.banners[index] = banner.copy(
                    statusLabel = STATUS_ACTIVE,
                    activatedAtEpochMillis = now
                )
            }
        }
    }

    private fun rebuildCafeHomeBannerPreviewCache() {
        val previewByCafeId = bannerDataSource.banners
            .asSequence()
            .mapNotNull { banner ->
                val cafeId = banner.cafeId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                cafeId to banner
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { entry ->
                entry.value
                    .sortedWith(
                        compareByDescending<HomeBanner> { banner -> banner.statusLabel.uppercase() == STATUS_ACTIVE }
                            .thenByDescending { banner -> banner.createdAtEpochMillis }
                    )
                    .first()
            }

        cafeDataSource.cafeHomeBannerPreviewByCafeId.clear()
        previewByCafeId.forEach { entry ->
            val statusLabel = when (entry.value.statusLabel.uppercase()) {
                STATUS_ACTIVE -> "노출 중"
                STATUS_SCHEDULED -> "예약 중"
                else -> "미노출"
            }
            cafeDataSource.cafeHomeBannerPreviewByCafeId[entry.key] = CafeDashboardData.HomeBannerPreview(
                title = entry.value.title,
                period = resolvePeriodLabel(entry.value.displayDays),
                statusLabel = statusLabel,
                imageUrl = entry.value.imageUrl
            )
        }
    }
}

private const val ACTIVE_BANNER_LIMIT = 5
private const val STATUS_ACTIVE = "ACTIVE"
private const val STATUS_SCHEDULED = "SCHEDULED"
private const val STATUS_ENDED = "ENDED"

private fun resolvePalette(targetType: BannerLinkTargetType): Pair<String, String> {
    return when (targetType) {
        BannerLinkTargetType.CAFE_DETAIL -> "FFD1DC" to "F58FB2"
        BannerLinkTargetType.EVENT_DETAIL -> "FFC2A7" to "FF8F7A"
        BannerLinkTargetType.NOTICE -> "B6A5FF" to "7E88FF"
        BannerLinkTargetType.EXTERNAL_LINK -> "A8E6CF" to "56C596"
    }
}

private fun resolvePeriodLabel(displayDays: Int): String {
    val startDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val endDate = startDate.plus(DatePeriod(days = displayDays - 1))
    return "${startDate.toPeriodText()} - ${endDate.toPeriodText()}"
}

private fun HomeBanner.isExpired(nowEpochMillis: Long): Boolean {
    if (activatedAtEpochMillis <= 0L) return false
    val endInstant = Instant.fromEpochMilliseconds(activatedAtEpochMillis)
        .plus(DatePeriod(days = displayDays), TimeZone.currentSystemDefault())
    return nowEpochMillis >= endInstant.toEpochMilliseconds()
}

private fun LocalDate.toPeriodText(): String {
    val monthText = monthNumber.toString().padStart(2, '0')
    val dayText = dayOfMonth.toString().padStart(2, '0')
    return "$year.$monthText.$dayText"
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}
