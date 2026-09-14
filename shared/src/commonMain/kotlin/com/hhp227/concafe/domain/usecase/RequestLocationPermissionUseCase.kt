package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.LocationPermissionStatus
import com.hhp227.concafe.domain.repository.LocationRepository

class RequestLocationPermissionUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): AppResult<LocationPermissionStatus> {
        return try {
            AppResult.Success(locationRepository.requestPermission())
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
