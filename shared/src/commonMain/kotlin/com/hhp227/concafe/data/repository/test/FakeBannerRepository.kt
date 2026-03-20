package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.data.source.BannerLinkType
import com.hhp227.concafe.data.source.BannerOwnerType
import com.hhp227.concafe.data.source.BannerStatus
import com.hhp227.concafe.data.source.HomeBannerDocument
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeBannerCreate
import com.hhp227.concafe.domain.repository.BannerRepository
import kotlinx.datetime.*

class FakeBannerRepository(
    private val dataSource: ConCafeDataSource
) : BannerRepository {
    override suspend fun getAllHomeBanners(): List<HomeBanner> {
        normalizeBannerSlots()
        return dataSource.banners.toList()
    }

    override suspend fun getHomeBanners(limit: Int): List<HomeBanner> {
        normalizeBannerSlots()
        return dataSource.banners
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
        val nextIndex = dataSource.banners.size + 1
        val palette = resolvePalette(input.targetType)
        val now = Clock.System.now().toEpochMilliseconds()
        val activeCount = dataSource.banners.count { it.statusLabel == STATUS_ACTIVE }
        val statusLabel = if (activeCount >= ACTIVE_BANNER_LIMIT) STATUS_SCHEDULED else STATUS_ACTIVE
        val created = HomeBanner(
            id = "banner-$nextIndex",
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
        dataSource.banners.add(0, created)
        syncBannerDocuments()

        input.cafeId?.let { cafeId ->
            dataSource.cafeHomeBannerPreviewByCafeId[cafeId] = CafeDashboardData.HomeBannerPreview(
                title = created.title,
                period = resolvePeriodLabel(input.displayDays),
                statusLabel = if (statusLabel == STATUS_ACTIVE) "노출 중" else "예약 중"
            )
        }
        return created
    }

    override suspend fun updateHomeBanner(bannerId: String, input: HomeBannerCreate): HomeBanner {
        if (input.title.isBlank()) throw IllegalArgumentException("banner title is required")
        if (input.subtitle.isBlank()) throw IllegalArgumentException("banner subtitle is required")
        if (input.targetValue.isBlank()) throw IllegalArgumentException("banner target is required")
        if (input.displayDays !in 1..10) throw IllegalArgumentException("displayDays must be between 1 and 10")

        normalizeBannerSlots()
        val index = dataSource.banners.indexOfFirst { it.id == bannerId }
        if (index == -1) {
            throw NoSuchElementException("banner not found")
        }

        val existing = dataSource.banners[index]
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
        dataSource.banners[index] = updated
        syncBannerDocuments()

        val changedCafeIds = linkedSetOf<String>()
        existing.cafeId?.takeIf { it.isNotBlank() }?.let { changedCafeIds.add(it) }
        updated.cafeId?.takeIf { it.isNotBlank() }?.let { changedCafeIds.add(it) }
        changedCafeIds.forEach { changedCafeId ->
            updateCafeHomeBannerPreview(changedCafeId)
        }
        return updated
    }

    override suspend fun deleteHomeBanner(bannerId: String): HomeBanner {
        normalizeBannerSlots()
        val index = dataSource.banners.indexOfFirst { it.id == bannerId }
        if (index == -1) {
            throw NoSuchElementException("banner not found")
        }

        val deleted = dataSource.banners.removeAt(index)
        syncBannerDocuments()
        updateCafeHomeBannerPreview(deleted.cafeId)
        normalizeBannerSlots()
        return deleted
    }

    private fun normalizeBannerSlots() {
        val now = Clock.System.now().toEpochMilliseconds()
        val expiredIds = dataSource.banners
            .filter { it.statusLabel == STATUS_ACTIVE && it.isExpired(now) }
            .map { it.id }
            .toSet()

        if (expiredIds.isNotEmpty()) {
            dataSource.banners.indices.forEach { index ->
                val banner = dataSource.banners[index]
                if (banner.id in expiredIds) {
                    dataSource.banners[index] = banner.copy(statusLabel = STATUS_ENDED)
                }
            }
        }

        var activeCount = dataSource.banners.count { it.statusLabel == STATUS_ACTIVE }
        if (activeCount >= ACTIVE_BANNER_LIMIT) {
            syncBannerDocuments()
            return
        }

        val scheduledBanners = dataSource.banners
            .filter { it.statusLabel == STATUS_SCHEDULED }
            .sortedBy { it.createdAtEpochMillis }

        val activateIds = scheduledBanners
            .take((ACTIVE_BANNER_LIMIT - activeCount).coerceAtLeast(0))
            .map { it.id }
            .toSet()

        if (activateIds.isEmpty()) {
            syncBannerDocuments()
            return
        }

        dataSource.banners.indices.forEach { index ->
            val banner = dataSource.banners[index]
            if (banner.id in activateIds) {
                dataSource.banners[index] = banner.copy(
                    statusLabel = STATUS_ACTIVE,
                    activatedAtEpochMillis = now
                )
            }
        }
        syncBannerDocuments()
    }

    private fun syncBannerDocuments() {
        val indexedDocuments = dataSource.homeBannerDocuments
            .associateBy { it.id }
            .toMutableMap()
        val nowDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val nowMonth = nowDateTime.monthNumber.toString().padStart(2, '0')
        val nowDay = nowDateTime.dayOfMonth.toString().padStart(2, '0')
        val nowLabel = "${nowDateTime.year}-$nowMonth-$nowDay"

        val syncedDocuments = dataSource.banners.mapIndexed { index, banner ->
            val existing = indexedDocuments.remove(banner.id)
            HomeBannerDocument(
                id = banner.id,
                ownerType = existing?.ownerType ?: resolveOwnerType(banner),
                ownerId = existing?.ownerId ?: resolveOwnerId(banner),
                relatedCafeId = banner.cafeId,
                title = banner.title,
                subtitle = banner.subtitle,
                imageUrl = banner.imageUrl,
                linkType = banner.targetType.toBannerLinkType(),
                linkTarget = banner.targetValue,
                priority = index + 1,
                maxVisibleGroup = existing?.maxVisibleGroup ?: 5,
                displayDays = banner.displayDays,
                activeFrom = existing?.activeFrom,
                activeUntil = existing?.activeUntil,
                status = banner.statusLabel.toBannerStatus(),
                createdAt = existing?.createdAt ?: "${nowLabel}T00:00:00Z",
                updatedAt = "${nowLabel}T00:00:00Z"
            )
        }
        dataSource.homeBannerDocuments.clear()
        dataSource.homeBannerDocuments.addAll(syncedDocuments)
    }

    private fun updateCafeHomeBannerPreview(cafeId: String?) {
        if (cafeId.isNullOrBlank()) {
            return
        }

        val representative = dataSource.banners
            .asSequence()
            .filter { it.cafeId == cafeId }
            .sortedBy { it.createdAtEpochMillis }
            .firstOrNull()
        if (representative == null) {
            dataSource.cafeHomeBannerPreviewByCafeId.remove(cafeId)
            return
        }

        val statusLabel = if (representative.statusLabel == STATUS_ACTIVE) "노출 중" else "예약 중"
        dataSource.cafeHomeBannerPreviewByCafeId[cafeId] = CafeDashboardData.HomeBannerPreview(
            title = representative.title,
            period = resolvePeriodLabel(representative.displayDays),
            statusLabel = statusLabel
        )
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

private fun resolveOwnerType(banner: HomeBanner): BannerOwnerType {
    return if (banner.cafeId.isNullOrBlank()) BannerOwnerType.ADMIN else BannerOwnerType.CAFE_OWNER
}

private fun resolveOwnerId(banner: HomeBanner): String {
    return if (banner.cafeId.isNullOrBlank()) "user-4" else "user-3"
}

private fun BannerLinkTargetType.toBannerLinkType(): BannerLinkType {
    return when (this) {
        BannerLinkTargetType.CAFE_DETAIL -> BannerLinkType.CAFE
        BannerLinkTargetType.EVENT_DETAIL -> BannerLinkType.EVENT
        BannerLinkTargetType.NOTICE -> BannerLinkType.NOTICE
        BannerLinkTargetType.EXTERNAL_LINK -> BannerLinkType.EXTERNAL
    }
}

private fun String.toBannerStatus(): BannerStatus {
    return when (this.uppercase()) {
        "DRAFT" -> BannerStatus.DRAFT
        "SCHEDULED" -> BannerStatus.SCHEDULED
        "ACTIVE" -> BannerStatus.ACTIVE
        "ENDED" -> BannerStatus.ENDED
        "PAUSED" -> BannerStatus.PAUSED
        else -> BannerStatus.ACTIVE
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
