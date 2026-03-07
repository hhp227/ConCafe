# 🎀 ConCafe Firestore 컬렉션 다이어그램

users/{userId}
├─ role: ADMIN | CAFE_OWNER | CAST | VISITOR
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

cafes/{cafeId}
├─ ownerId
├─ name
├─ description
├─ conceptType: MAID | BUTLER | IDOL | THEME
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

cafes/{cafeId}/casts/{castId}
├─ name
├─ profileImage
├─ images[]
├─ description
├─ birthday
├─ joinDate
├─ conceptRole: maid | butler | idol
├─ linkedUserId
├─ followerCount
├─ popularityScore
└─ createdAt

cafes/{cafeId}/menus/{menuId}
├─ name
├─ price
├─ description
├─ image
├─ category: food | drink | dessert
└─ createdAt

cafes/{cafeId}/goods/{goodsId}
├─ name
├─ price
├─ image
├─ stock
└─ createdAt

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

events/{eventId}
├─ eventType: BIRTHDAY | ANNIVERSARY | COLLAB | SPECIAL_GUEST
├─ relatedCafeId
├─ relatedCastId
├─ startDate
├─ endDate
└─ description

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
├─ status
│  PENDING
│  APPROVED
│  REJECTED
└─ createdAt