package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastUpsert

interface CastDataSource {
    val casts: List<Cast>
    val affiliatedCafeIdByUser: MutableMap<String, String>
    val castTodayVisitCountById: Map<String, Int>
    fun castDetail(castId: String): CastDetail?
    fun castSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule>
    fun castScheduleStatuses(castId: String, fromDate: String, toDate: String): Map<String, CastScheduleStatus>
    fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule?
    fun upsertCast(update: CastUpsert): CastDetail
    fun deleteCast(castId: String): Cast
}