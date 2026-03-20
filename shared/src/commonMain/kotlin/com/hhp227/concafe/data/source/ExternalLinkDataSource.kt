package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.ExternalLink

interface ExternalLinkDataSource {
    val cafeExternalLinksByCafeId: MutableMap<String, MutableList<ExternalLink>>
    val castExternalLinksByCastId: MutableMap<String, MutableList<ExternalLink>>
}
