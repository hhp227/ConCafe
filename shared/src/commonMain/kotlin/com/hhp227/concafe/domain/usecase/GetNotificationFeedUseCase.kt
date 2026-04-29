package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.NotificationFeed
import com.hhp227.concafe.domain.model.NotificationListItem
import com.hhp227.concafe.domain.model.NotificationSection
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.NotificationRepository

class GetNotificationFeedUseCase(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(pageSize: Int = DEFAULT_PAGE_SIZE): AppResult<NotificationFeed> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Success(
                    NotificationFeed(
                        isLoggedIn = false,
                        unreadCount = 0,
                        sections = emptyList()
                    )
                )
            } else {
                val notifications = notificationRepository
                    .getNotifications(currentUser.id, cursor = null, pageSize = pageSize)
                    .items
                val unreadCount = notificationRepository.getUnreadNotificationCount(currentUser.id)
                val sections = SECTION_ORDER.mapNotNull { sectionSpec ->
                    val sectionItems = notifications
                        .filter { it.type in sectionSpec.types }
                        .map { notification ->
                            NotificationListItem(
                                id = notification.id,
                                title = notification.title,
                                message = notification.body,
                                type = notification.type,
                                targetId = notification.targetId,
                                isRead = notification.isRead,
                                relativeTime = notification.relativeTime
                            )
                        }

                    if (sectionItems.isEmpty()) {
                        null
                    } else {
                        NotificationSection(
                            id = sectionSpec.id,
                            title = sectionSpec.title,
                            items = sectionItems
                        )
                    }
                }

                AppResult.Success(
                    NotificationFeed(
                        isLoggedIn = true,
                        unreadCount = unreadCount,
                        sections = sections
                    )
                )
            }
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    private data class SectionSpec(
        val id: String,
        val title: String,
        val types: Set<String>
    )

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20

        private val SECTION_ORDER = listOf(
            SectionSpec(
                id = "approval_request",
                title = "승인 요청",
                types = setOf("CAFE_APPROVAL_REQUEST", "CAFE_OWNER_APPROVAL_REQUEST", "CAST_CLAIM_REQUEST")
            ),
            SectionSpec(
                id = "approval_result",
                title = "승인 결과",
                types = setOf(
                    "CAFE_APPROVED", "CAFE_REJECTED",
                    "CAFE_OWNER_APPROVED", "CAFE_OWNER_REJECTED",
                    "CAST_CLAIM_APPROVED", "CAST_CLAIM_REJECTED"
                )
            ),
            SectionSpec(
                id = "work",
                title = "출근 알림",
                types = setOf("CAST_SHIFT")
            ),
            SectionSpec(
                id = "birthday",
                title = "생일 알림",
                types = setOf("BIRTHDAY")
            ),
            SectionSpec(
                id = "notice",
                title = "카페 공지",
                types = setOf("CAFE_NOTICE", "CAFE_EVENT")
            ),
            SectionSpec(
                id = "check_in",
                title = "체크인 알림",
                types = setOf("CAFE_CHECK_IN")
            ),
            SectionSpec(
                id = "table_count",
                title = "테이블 알림",
                types = setOf("CAFE_TABLE_COUNT_UPDATE")
            ),
            SectionSpec(
                id = "follow",
                title = "팔로우 업데이트",
                types = setOf("FOLLOW_UPDATE")
            ),
            SectionSpec(
                id = "fan_announcement",
                title = "팬 공지",
                types = setOf("FAN_ANNOUNCEMENT")
            )
        )
    }
}
