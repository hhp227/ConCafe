package com.hhp227.concafe.data.source

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
}
