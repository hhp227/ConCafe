# 🎀 ConCafe Firestore 컬렉션 다이어그램

users/{userId}
├─ role: ADMIN | OWNER | USER
├─ nickname
├─ profileImage
├─ banned
├─ createdAt
├─ visitHistory: [visitId]
└─ favorites: [cafeId]

cafes/{cafeId}
├─ ownerId
├─ name
├─ description
├─ conceptType: MAID | BUTLER
├─ region
│  ├─ country: KR | JP
│  ├─ city: Seoul | Tokyo
│  ├─ address
│  └─ location (GeoPoint)
├─ thumbnailImage
├─ images: []
├─ approved
├─ ratingAvg
├─ reviewCount
├─ createdAt
├─ casts/{castId}
│  ├─ name
│  ├─ profileImage
│  ├─ images: []
│  ├─ description
│  ├─ birthday
│  ├─ joinDate
│  ├─ conceptRole: maid / butler / idol
│  └─ followerCount
├─ menus/{menuId}
│  ├─ name
│  ├─ price
│  ├─ description
│  ├─ image
│  └─ category: food / drink
├─ goods/{goodsId}
│  ├─ name
│  ├─ price
│  ├─ image
│  └─ stock
├─ reviews/{reviewId}
│  ├─ userId
│  ├─ rating
│  ├─ content
│  ├─ images: []
│  ├─ likeCount
│  └─ createdAt
└─ notices/{noticeId}
   ├─ title
   ├─ content
   └─ createdAt

castSchedules/{scheduleId}
├─ castId
├─ cafeId
├─ date
├─ startTime
└─ endTime

visits/{visitId}
├─ userId
├─ cafeId
├─ visitedAt
└─ verified

castFollowers/{castId}/users/{userId}
└─ followedAt

events/{eventId}
├─ eventType: BIRTHDAY | ANNIVERSARY | COLLAB | SPECIAL_GUEST
├─ relatedCafeId
├─ relatedCastId
├─ startDate
├─ endDate
└─ description