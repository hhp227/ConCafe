package com.hhp227.concafe.domain.model

enum class LocationPermissionStatus {
    GRANTED,

    /** The system permission dialog was shown; the caller should retry once the user answers. */
    REQUEST_PENDING,

    /** Denied, or only approximate location is allowed — the user has to change it in Settings. */
    PRECISE_LOCATION_REQUIRED,

    SERVICE_UNAVAILABLE,

    UNSUPPORTED;

    val isGranted: Boolean
        get() = this == GRANTED

    val requiresSettings: Boolean
        get() = this == PRECISE_LOCATION_REQUIRED || this == SERVICE_UNAVAILABLE
}
