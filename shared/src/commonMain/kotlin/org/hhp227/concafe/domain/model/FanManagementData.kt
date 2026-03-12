package org.hhp227.concafe.domain.model

data class FanManagementData(
    val user: User,
    val detail: CastDetail,
    val followers: List<User>
)
