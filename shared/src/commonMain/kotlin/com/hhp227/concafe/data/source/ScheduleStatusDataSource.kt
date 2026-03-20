package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.CastScheduleStatus

interface ScheduleStatusDataSource {
    val castScheduleStatusByCastId: MutableMap<String, MutableMap<String, CastScheduleStatus>>
}
