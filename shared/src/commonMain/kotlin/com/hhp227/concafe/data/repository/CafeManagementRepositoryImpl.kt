package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.data.source.CastRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.CafeManagementRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CafeManagementRepositoryImpl(
    private val cafeRemoteDataSource: CafeRemoteDataSource,
    private val castRemoteDataSource: CastRemoteDataSource,
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : CafeManagementRepository {
    override suspend fun getOwnedCafes(userId: String): List<CafeManagementData.OwnedCafeSummary> {
        val manageableCafes = loadManageableCafes(userId)
        return buildOwnedCafeSummaries(manageableCafes)
    }

    override suspend fun getCafeManagementData(userId: String): CafeManagementData {
        cafeRemoteDataSource.refreshCafeManagementData(userId)
        val manageableCafes = loadManageableCafes(userId)
        val (ownedCafes, searchableCafes, pendingCafeOwnerClaims, pendingCafeRegistrationClaims) = coroutineScope {
            val ownedCafesDeferred = async { buildOwnedCafeSummaries(manageableCafes) }
            val searchableCafesDeferred = async {
                cafeRemoteDataSource.fetchAllCafes()
                    .sortedBy { cafe -> cafe.name.lowercase() }
                    .map { cafe ->
                        CafeManagementData.SearchableCafeSummary(
                            id = cafe.id,
                            name = cafe.name,
                            location = "${cafe.region.city} ${cafe.region.address}".trim()
                        )
                    }
            }
            val pendingCafeOwnerClaimsDeferred = async {
                cafeRemoteDataSource.fetchPendingCafeOwnerClaims(userId)
            }
            val pendingCafeRegistrationClaimsDeferred = async {
                cafeRemoteDataSource.fetchPendingCafeRegistrationClaims(userId)
                    .map { claim ->
                        CafeManagementData.PendingClaimSummary(
                            claimId = claim.claimId,
                            cafeId = "",
                            cafeName = claim.cafeName,
                            requestedAt = claim.requestedAt,
                            status = claim.status,
                            message = claim.message
                        )
                    }
            }
            Quadruple(
                ownedCafesDeferred.await(),
                searchableCafesDeferred.await(),
                pendingCafeOwnerClaimsDeferred.await(),
                pendingCafeRegistrationClaimsDeferred.await()
            )
        }
        val pendingClaims = (pendingCafeOwnerClaims + pendingCafeRegistrationClaims)
            .sortedByDescending { it.requestedAt }
        return CafeManagementData(
            ownedCafes = ownedCafes,
            searchableCafes = searchableCafes,
            pendingClaims = pendingClaims
        )
    }

    private suspend fun loadManageableCafes(userId: String): List<Cafe> {
        val currentUser = firestoreSyncDataSource.fetchUser(userId)
        return if (currentUser?.role == UserRole.ADMIN) {
            cafeRemoteDataSource.fetchAllCafes()
        } else {
            val ownedCafeIds = cafeRemoteDataSource.fetchOwnedCafeIds(userId)
            ownedCafeIds.mapNotNull { cafeId ->
                cafeRemoteDataSource.fetchCafeById(cafeId)
            }.sortedBy { cafe -> cafe.name }
        }
    }

    private suspend fun buildOwnedCafeSummaries(
        cafes: List<Cafe>
    ): List<CafeManagementData.OwnedCafeSummary> = coroutineScope {
        cafes.map { cafe ->
            async {
                val castCountDeferred = async {
                    castRemoteDataSource.fetchCafeCastCount(cafe.id)
                }
                val noticeCountDeferred = async {
                    cafeRemoteDataSource.fetchNoticeCount(cafe.id)
                }
                val cafeCheckInCountDeferred = async {
                    cafeRemoteDataSource.fetchCafeCheckInCount(cafe.id)
                }
                val cafeCheckInCount = cafeCheckInCountDeferred.await()

                CafeManagementData.OwnedCafeSummary(
                    id = cafe.id,
                    name = cafe.name,
                    city = cafe.region.city,
                    isApproved = cafe.approved,
                    todayVisitors = cafeCheckInCount,
                    todayCheckIns = cafeCheckInCount / 4,
                    todayReviews = (cafe.reviewCount / 50).coerceAtLeast(0),
                    rating = cafe.ratingAvg,
                    castCount = castCountDeferred.await(),
                    noticeCount = noticeCountDeferred.await(),
                    externalLinkCount = 3,
                    thumbnailImage = cafe.thumbnailImage
                )
            }
        }.awaitAll()
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
