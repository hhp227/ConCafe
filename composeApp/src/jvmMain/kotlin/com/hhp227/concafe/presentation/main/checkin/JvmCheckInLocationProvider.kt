package com.hhp227.concafe.presentation.main.checkin

class JvmCheckInLocationProvider : CheckInLocationProvider {
    override suspend fun requestPermissionIfNeeded(): CheckInLocationPermissionResult {
        return CheckInLocationPermissionResult.Failure("현재 플랫폼에서는 위치 기반 체크인을 지원하지 않습니다.")
    }

    override suspend fun getCurrentLocation(): CheckInLocationResult {
        return CheckInLocationResult.Failure("현재 플랫폼에서는 위치 기반 체크인을 지원하지 않습니다.")
    }
}
