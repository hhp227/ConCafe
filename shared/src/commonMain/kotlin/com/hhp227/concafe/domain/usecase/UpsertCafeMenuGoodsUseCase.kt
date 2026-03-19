package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.repository.CafeRepository

class UpsertCafeMenuGoodsUseCase(
    private val cafeRepository: CafeRepository,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher
) {
    suspend operator fun invoke(update: CafeMenuGoodsUpsert): AppResult<CafeDetail> {
        return try {
            val updatedDetail = cafeRepository.upsertCafeMenuGoods(update)
            val existingItemId = update.itemId
            val isCreate = existingItemId.isNullOrBlank()
            val updatedMenu = updatedDetail.menus.firstOrNull { it.id == existingItemId }
                ?: updatedDetail.menus.lastOrNull()?.takeIf { update.category.lowercase() != "goods" }
            val updatedGoods = updatedDetail.goods.firstOrNull { it.id == existingItemId }
                ?: updatedDetail.goods.lastOrNull()?.takeIf { update.category.lowercase() == "goods" }
            val event = when {
                updatedMenu != null && isCreate -> CafeDetailEvent.MenuCreated(update.cafeId, updatedMenu)
                updatedMenu != null -> CafeDetailEvent.MenuUpdated(update.cafeId, updatedMenu)
                updatedGoods != null && isCreate -> CafeDetailEvent.GoodsCreated(update.cafeId, updatedGoods)
                updatedGoods != null -> CafeDetailEvent.GoodsUpdated(update.cafeId, updatedGoods)
                else -> throw NoSuchElementException("menu goods item not found")
            }

            cafeDetailEventPublisher.publish(event)
            AppResult.Success(updatedDetail)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
