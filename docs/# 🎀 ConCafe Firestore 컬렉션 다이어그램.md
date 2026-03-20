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
├─ maxVisibleGroup: 5
├─ displayDays: 1..10
├─ activeFrom
├─ activeUntil
├─ status: DRAFT | SCHEDULED | ACTIVE | ENDED | PAUSED
├─ createdAt
└─ updatedAt

구현 메모
- 현재 mock/shared 구현은 `displayDays`를 저장하고, 활성 배너는 최대 5개까지만 홈에 노출한다.
- 홈 피드 조회 시 종료된 `ACTIVE` 배너를 `ENDED`로 정리하고, 빈 슬롯이 있으면 `SCHEDULED` 배너를 자동 승격한다.
- 외부 링크 타입 배너는 홈에서 탭 시 외부 브라우저가 아니라 앱 내부 외부 링크 화면(WebView)으로 이동한다.
- shared mock 데이터소스 계약에는 `homeBanners` 문서형 필드(`ownerType`, `ownerId`, `linkType`, `status`, `activeFrom/Until` 포함)가 반영되어 있으며, 기존 `HomeBanner` UI 모델과 병행 유지된다.

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

castScheduleStatuses/{castId}/days/{date}
├─ status: WORK | OFF | VACATION
├─ updatedAt
└─ updatedBy

구현 메모
- 현재 캐스트 프로필 편집 화면의 저장 범위는 `cafes/{cafeId}/casts/{castId}` 기본 정보와 `castSchedules`이다.
- 근무 요일 UI는 별도 `workingDays` 배열 필드가 아니라 `castSchedules` 문서 생성/수정 결과를 다시 읽어 계산한다.
- 현재 출근표 수정 구현은 `castSchedules`와 별도로 날짜별 상태(`WORK/OFF/VACATION`)를 함께 관리한다.
- `WORK`일 때만 `castSchedules` 문서에 실제 시작/종료 시간이 존재한다.
- `OFF`, `VACATION`은 상태 문서로 유지하고, UI는 `GetScheduleManagementDataUseCase`가 두 데이터를 조합해 만든다.
- 프로필 이미지, 갤러리 이미지, 외부 SNS 링크 저장은 후속 단계에서 연결한다.
- shared mock 데이터소스에는 `castScheduleStatuses/{castId}/days/{date}` 대응 문서형 필드(`status`, `updatedAt`, `updatedBy`)가 추가되어 상태 갱신 시 동기화된다.

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

구현 메모
- shared mock 데이터소스에는 역인덱스 필드(`favoriteUserIdsByCafeId`, `followerUserIdsByCastId`)가 추가되어 컬렉션 구조와 읽기 방향을 함께 유지한다.

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

구현 메모
- shared mock 데이터소스에 `stamps` 컬렉션 대응 필드가 추가되어 Firebase 연결 전에도 적립 데이터 모델을 유지한다.

castClaims/{claimId}
├─ userId
├─ cafeId
├─ castId
├─ status: PENDING | APPROVED | REJECTED
├─ message
├─ evidenceImageUrls: []
├─ reviewedBy
├─ reviewedAt
└─ createdAt

캐스트 Claim 정책 메모
- `castClaims`는 캐스트 회원가입 자체가 아니라 `팬관리에서 보내는 기존 캐스트 프로필 연결 요청`을 의미한다.
- 캐스트는 가입 시 선택한 `소속 카페` 기준으로 해당 카페의 캐스트 프로필에 연결 요청을 보낸다.
- 요청 생성은 캐스트가 `팬관리` 내부 전용 신청 UI에서 수행한다.
- 승인 / 반려는 소속 카페 운영자가 `카페 관리 대시보드 > 캐스트 관리 섹션`에서 처리한다.
- 승인되면 `cafes/{cafeId}/casts/{castId}.linkedUserId = userId`로 연결한다.
- 연결된 캐스트 프로필이 삭제되면 다시 미연결 상태가 되며 새 `castClaims` 생성이 가능하다.

cafeOwnerClaims/{claimId}
├─ userId
├─ cafeId
├─ status: PENDING | APPROVED | REJECTED
├─ message
├─ evidenceImageUrls: []
├─ reviewedBy
├─ reviewedAt
└─ createdAt

cafeRegistrationClaims/{claimId}
├─ userId
├─ draft
│  ├─ name
│  ├─ description
│  ├─ region
│  ├─ thumbnailImage
│  └─ businessHours
├─ status: PENDING | APPROVED | REJECTED
├─ reviewedBy
├─ reviewedAt
└─ createdAt

공지/이벤트 관리 메모
- `cafes/{cafeId}/notices`, `cafes/{cafeId}/events`는 카페별 페이지네이션 조회를 사용하며 현재 페이지 크기는 15개다.
- 공지 문서는 고정 여부(`isPinned`)와 게시 상태를 함께 관리한다.
- 이벤트 문서는 제목/설명 외에 대표 이미지와 기간을 함께 관리한다.
