package com.hhp227.concafe.domain.policy

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class DormantAccountPolicy {
    fun dormantCutoffIso(now: Instant = Clock.System.now()): String {
        return (now - DORMANT_THRESHOLD_DAYS.days).toString()
    }

    fun shouldRefreshLastLogin(lastLoginAt: String?, now: Instant = Clock.System.now()): Boolean {
        val lastLoginInstant = lastLoginAt?.let { value ->
            runCatching { Instant.parse(value) }.getOrNull()
        }
        return if (lastLoginInstant == null) {
            true
        } else {
            now - lastLoginInstant >= LAST_LOGIN_REFRESH_HOURS.hours
        }
    }

    fun isDormantCandidate(lastLoginAt: String?, now: Instant = Clock.System.now()): Boolean {
        val lastLoginInstant = lastLoginAt?.let { value ->
            runCatching { Instant.parse(value) }.getOrNull()
        }
        return if (lastLoginInstant == null) {
            false
        } else {
            now - lastLoginInstant >= DORMANT_THRESHOLD_DAYS.days
        }
    }

    companion object {
        const val DORMANT_THRESHOLD_DAYS = 365

        const val LAST_LOGIN_REFRESH_HOURS = 24
    }
}
