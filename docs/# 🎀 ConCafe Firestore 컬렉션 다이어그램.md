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
├─ createdAt
└─ updatedAt

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
├─ taggedCastIds[]
├─ likeCount
└─ createdAt

리뷰 정책 메모
- 현재 단계의 리뷰는 `카페 리뷰`만 작성한다.
- 리뷰 작성 시 같은 카페에 소속된 캐스트를 선택적으로 태그할 수 있다.
- `taggedCastIds[]`는 캐스트 전용 리뷰를 의미하지 않고, 카페 리뷰 안에서 함께 언급된 캐스트 연결 정보로만 사용한다.
- 캐스트 상세 화면에서는 `taggedCastIds[]`에 현재 캐스트 id가 포함된 카페 리뷰만 `함께 언급된 후기`로 노출한다.

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

구현 메모
- 현재 캐스트 프로필 편집 화면의 저장 범위는 `cafes/{cafeId}/casts/{castId}` 기본 정보와 `castSchedules`이다.
- 근무 요일 UI는 별도 `workingDays` 배열 필드가 아니라 `castSchedules` 문서 생성/수정 결과를 다시 읽어 계산한다.
- 프로필 이미지, 갤러리 이미지, 외부 SNS 링크 저장은 후속 단계에서 연결한다.

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
