package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.Stamp

interface StampDataSource {
    val stamps: MutableList<Stamp>
}
