# 🎀 ConCafe Firestore 컬렉션 다이어그램

users/{userId}
├─ role: ADMIN | CAFE_OWNER | CAST | VISITOR
├─ ownedCafeIds: []
├─ nickname
├─ profileImage
├─ phoneNumber
├─ phoneVerified
├─ banned
├─ blurCredits
├─ createdAt
└─ stats
   ├─ visitCount
   ├─ reviewCount
   └─ stampCount

homeBanners/{bannerId}
├─ ownerType: ADMIN | CAFE_OWNER
├─ ownerId
├─ relatedCafeId
├─ title
├─ subtitle
├─ imageUrl
├─ linkType: CAFE | EVENT | NOTICE | EXTERNAL
├─ linkTarget
├─ priority
├─ maxVisibleGroup: 3 | 4 | 5
├─ startAt
├─ endAt
├─ status: DRAFT | SCHEDULED | ACTIVE | ENDED | PAUSED
├─ createdAt
└─ updatedAt

cafes/{cafeId}
├─ ownerIds: []
├─ name
├─ desc
├─ conceptType: MAID | BUTLER | IDOL
├─ region
│  ├─ country: KR | JP
│  ├─ city: Seoul | Tokyo
│  ├─ address
│  └─ location (GeoPoint)
├─ thumbnailImage
├─ images: []
├─ approved
├─ stats
│  ├─ ratingAvg
│  ├─ reviewCount
│  ├─ visitCount
│  └─ favoriteCount
├─ createdAt
└─ updatedAt

cafes/{cafeId}/externalLinks/{linkId}
├─ platform: INSTAGRAM | X | TIKTOK | YOUTUBE | WEBSITE
├─ title
├─ url
├─ isVisible
├─ sortOrder
├─ createdAt
└─ updatedAt

cafes/{cafeId}/casts/{castId}
├─ name
├─ profileImage
├─ images[]
├─ desc
├─ birthday
├─ joinDate
├─ conceptRole: maid | butler | idol
├─ linkedUserId
├─ followerCount
├─ rating
├─ popularityScore
└─ createdAt

cafes/{cafeId}/casts/{castId}/externalLinks/{linkId}
├─ platform: INSTAGRAM | X | TIKTOK | YOUTUBE | WEBSITE
├─ title
├─ url
├─ isVisible
├─ sortOrder
├─ createdAt
└─ updatedAt

cafes/{cafeId}/menus/{menuId}
├─ name
├─ price
├─ desc
├─ image
├─ category: food | drink | dessert
├─ isAvailable
├─ createdAt
└─ updatedAt

cafes/{cafeId}/goods/{goodsId}
├─ name
├─ price
├─ image
├─ stock
├─ createdAt
└─ updatedAt

cafes/{cafeId}/reviews/{reviewId}
├─ userId
├─ visitId
├─ rating
├─ content
├─ images[]
├─ likeCount
└─ createdAt

cafes/{cafeId}/notices/{noticeId}
├─ title
├─ content
├─ createdBy
└─ createdAt

castSchedules/{scheduleId}
├─ castId
├─ cafeId
├─ date
├─ startTime
├─ endTime
└─ createdAt

visits/{visitId}
├─ userId
├─ cafeId
├─ location (GeoPoint)
├─ visitedAt
└─ verified

castFollowers/{castId}/users/{userId}
└─ followedAt

cafeFavorites/{cafeId}/users/{userId}
└─ createdAt

cafes/{cafeId}/events/{eventId}
├─ eventType: BIRTHDAY | ANNIVERSARY | COLLAB | SPECIAL_GUEST
├─ relatedCastId
├─ startDate
├─ endDate
└─ desc

stamps/{stampId}
├─ userId
├─ cafeId
├─ visitId
└─ earnedAt

castClaims/{claimId}
├─ userId
├─ cafeId
├─ castId
├─ status
│  PENDING
│  APPROVED
│  REJECTED
└─ createdAt

cafeOwnerClaims/{claimId}
├─ userId
├─ cafeId
├─ status: PENDING | APPROVED | REJECTED
├─ message
├─ evidenceImageUrls: []
├─ reviewedBy
├─ reviewedAt
└─ createdAt
