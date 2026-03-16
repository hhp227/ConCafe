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
                        unreadCount = notifications.count { !it.isRead },
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
                types = setOf("CAFE_NOTICE")
            ),
            SectionSpec(
                id = "follow",
                title = "팔로우 업데이트",
                types = setOf("FOLLOW_UPDATE")
            )
        )
    }
}
