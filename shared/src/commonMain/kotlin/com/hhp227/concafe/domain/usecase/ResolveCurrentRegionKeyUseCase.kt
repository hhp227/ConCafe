package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CurrentLocation
import com.hhp227.concafe.domain.policy.CityRegionPolicy
import com.hhp227.concafe.domain.repository.LocationRepository

/**
 * Resolves the region key of the user's current city for the check-in map. A cached fix is
 * used when it already maps to a known city so the map can populate without waiting for
 * hardware; null means the user is outside every known region.
 */
class ResolveCurrentRegionKeyUseCase(
    private val locationRepository: LocationRepository
) {
    private val cityRegionPolicy = CityRegionPolicy()

    suspend operator fun invoke(): AppResult<String?> {
        return try {
            val cachedRegionKey = locationRepository.getLastKnownLocation()
                ?.let { point -> cityRegionPolicy.regionKeyOf(point) }

            if (cachedRegionKey != null) {
                AppResult.Success(cachedRegionKey)
            } else {
                when (val location = locationRepository.getCurrentLocation()) {
                    is CurrentLocation.Available -> {
                        AppResult.Success(cityRegionPolicy.regionKeyOf(location.point))
                    }
                    is CurrentLocation.Unavailable -> {
                        AppResult.Failure(AppError.ValidationFailed(location.reason.name))
                    }
                }
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
