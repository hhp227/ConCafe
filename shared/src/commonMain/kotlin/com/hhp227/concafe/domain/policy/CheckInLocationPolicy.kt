package com.hhp227.concafe.domain.policy

/**
 * A device fix is usable for check-in only when it is both recent and precise enough to
 * place the user at a cafe. Platform location sources apply this before returning a fix.
 */
class CheckInLocationPolicy {
    fun isAcceptable(accuracyMeters: Double, ageMillis: Long): Boolean {
        return ageMillis in 0..MAX_LOCATION_AGE_MILLIS
            && accuracyMeters in 0.0..MAX_ALLOWED_ACCURACY_METERS
    }

    companion object {
        const val MAX_LOCATION_AGE_MILLIS = 30_000L
        const val MAX_ALLOWED_ACCURACY_METERS = 80.0
    }
}
