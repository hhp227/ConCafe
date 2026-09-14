package com.hhp227.concafe.domain.policy

import com.hhp227.concafe.domain.model.GeoPoint

/** Maps a coordinate to the region key used by the check-in map, or null outside known cities. */
class CityRegionPolicy {
    fun regionKeyOf(point: GeoPoint): String? {
        val lat = point.latitude
        val lng = point.longitude
        return when {
            lat in 37.4..37.7 && lng in 126.7..127.2 -> "seoul"
            lat in 35.0..35.4 && lng in 128.8..129.3 -> "busan"
            lat in 35.7..36.0 && lng in 128.4..128.8 -> "daegu"
            lat in 35.35..35.60 && lng in 139.50..139.75 -> "etc"
            lat in 35.5..35.9 && lng in 139.3..139.9 -> "tokyo"
            lat in 34.5..34.9 && lng in 135.3..135.7 -> "osaka"
            else -> null
        }
    }
}
