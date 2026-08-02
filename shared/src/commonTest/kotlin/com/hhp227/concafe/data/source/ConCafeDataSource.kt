package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.User

interface ConCafeDataSource :
    AuthDataSource,
    CafeDataSource,
    CastDataSource,
    CastClaimDataSource,
    BannerDataSource,
    InquiryDataSource,
    NoticeDataSource,
    ReviewDataSource,
    VisitDataSource,
    ExternalLinkDataSource,
    StampDataSource,
    ScheduleStatusDataSource,
    NotificationDataSource,
    SocialDataSource,
    MyInfoDataSource,
    RankingDataSource,
    PagingDataSource {
    val users: MutableList<User>

    val notifications: MutableList<AppNotification>

    fun findUserById(userId: String): User?

    fun findUserByEmail(email: String): User?

    fun isEmailTaken(email: String): Boolean

    fun addUser(user: User)

    fun removeUser(userId: String): Boolean

    fun replaceUser(user: User): Boolean

    fun replaceAllUsers(users: List<User>)
}
