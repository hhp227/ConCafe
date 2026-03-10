# 🎀 ConCafe Platform -- 통합 기획서 (Extended Version)

------------------------------------------------------------------------

# 1. 프로젝트 개요

## 📌 서비스 정의

ConCafe는 메이드카페 정보를 공유하고  
카페 운영자, 캐스트, 팬 유저가 함께 사용하는 **컨셉 카페 플랫폼**이다.

단순한 카페 정보 제공 앱이 아니라,

**카페 운영 관리 + 캐스트 팬 커뮤니티 + 방문 기록 플랫폼**

을 목표로 한다.

------------------------------------------------------------------------

# 2. 서비스 목표

1. 전국 메이드카페 정보 통합
2. 캐스트 중심 팬 플랫폼 구축
3. 카페 운영자와 유저 연결
4. 방문 인증 기반 신뢰 리뷰 시스템
5. 글로벌 확장 가능 구조 설계 (한국 → 일본)
6. 카페 운영 관리 플랫폼 제공

------------------------------------------------------------------------

# 3. 유저 타입 정의 (Role 기반 설계)

ConCafe는 4가지 Role 기반 구조를 가진다.

| Role | 설명 |
|-----|-----|
| ADMIN | 플랫폼 관리자 |
| CAFE_OWNER | 카페 운영자 |
| CAST | 카페 캐스트 |
| VISITOR | 일반 사용자 |

------------------------------------------------------------------------

## 👑 1) Admin

관리자 권한

- 카페 승인 / 반려
- 카페 운영자 승인
- 캐스트 Claim 승인
- 신고 처리
- 리뷰 삭제
- 유저 밴 처리
- 데이터 관리

------------------------------------------------------------------------

## 🏠 2) Cafe Owner (카페 운영자)

가입 조건

- **휴대폰 인증 필수**

가입 시 입력

- 휴대폰 번호 인증
- 카페 등록 또는 기존 카페 선택

### 카페 등록 방식

#### 1️⃣ 기존 카페 Claim

초기 런칭 시  
관리자가 미리 카페 데이터를 등록한다.

운영자는

카페 검색 → 본인 카페 선택 → 운영자 신청

Admin 승인 후 운영 권한 부여.

운영 권한이 승인되면

- `users/{userId}.ownedCafeIds`에 해당 `cafeId` 추가
- `cafes/{cafeId}.ownerIds`에 해당 `userId` 추가

가입 직후 연결된 카페가 없는 운영자는 `카페관리` 탭에서 빈 상태 화면을 먼저 보게 되며,
여기서 `기존 카페 검색` 또는 `신규 카페 등록` 플로우로 진입한다.

앱 구현 계층에서는 카페관리 화면이 필요한 운영 카페 목록, 기존 카페 검색 목록, 운영자 신청 상태를
별도 화면 하드코딩이 아니라 `GetCafeManagementUseCase`를 통해 가져온다.
이 유스케이스는 `CafeManagementRepository`를 사용하며, Repository는 `ConCafeDataSource`를 원천 데이터로 사용한다.

#### 2️⃣ 신규 카페 등록

운영자가 직접 카페 등록 가능

입력 정보

- 카페 이름
- 주소
- 위치
- 소개
- 대표 이미지

Admin 승인 후 공개.

### Owner 권한

- 카페 정보 수정
- 캐스트 등록 및 관리
- 메뉴 관리
- 굿즈 관리
- 이벤트 등록
- 공지사항 작성
- 홈 배너 등록 및 예약
- 캐스트 출근표 관리
- 통계 확인

------------------------------------------------------------------------

## 🎀 3) Cast (캐스트)

카페에 소속된 메이드 / 집사 등 캐릭터 직원.

가입 시 입력

- 이름
- 생일
- 소속 카페 선택

### 캐스트 가입 흐름

가입 시 소속 카페 선택 후

1️⃣ 기존 캐스트 프로필 검색

존재할 경우

→ 해당 캐스트 프로필 Claim 요청

2️⃣ 캐스트 프로필이 없을 경우

→ 새 캐스트 생성

### Cast 권한

- 본인 프로필 수정
- 이미지 업로드
- 출근 일정 등록
- 팬 알림
- 공지 작성 (선택)

------------------------------------------------------------------------

## 👤 4) Visitor (일반 사용자)

가입 방식

- 이메일
- 소셜 로그인

**휴대폰 인증 없음**

### Visitor 기능

- 카페 조회
- 캐스트 조회
- 방문 기록
- 리뷰 작성
- 즐겨찾기
- 캐스트 팔로우
- 스탬프 적립
- 방문 히스토리 관리
- 설정 화면 진입

------------------------------------------------------------------------

# 4. 핵심 도메인 모델

## User

- role (ADMIN / CAFE_OWNER / CAST / VISITOR)
- ownedCafeIds[]
- profile
- phoneNumber
- phoneVerified
- visitHistory
- favorites
- followedCasts
- blurCredits

------------------------------------------------------------------------

## Cafe

- ownerIds[]
- name
- description
- region
- thumbnailImage
- images[]
- casts
- menus
- goods
- events
- reviews
- notices

------------------------------------------------------------------------

## Cast

- cafeId
- linkedUserId
- images[]
- schedule[]
- conceptRole
- birthday
- followerCount
- popularityScore

------------------------------------------------------------------------

## Review

- userId
- rating
- content
- images[]
- likeCount

------------------------------------------------------------------------

## Visit

- userId
- cafeId
- visitedAt
- verified

------------------------------------------------------------------------

## Banner

- ownerType (ADMIN / CAFE_OWNER)
- ownerId
- relatedCafeId
- title
- subtitle
- imageUrl
- linkType
- linkTarget
- priority
- startAt
- endAt
- status

------------------------------------------------------------------------

# 5. Firestore 컬렉션 설계

## users/{userId}

- role
- ownedCafeIds[]
- nickname
- profileImage
- phoneNumber
- phoneVerified
- banned
- blurCredits
- createdAt

------------------------------------------------------------------------

## cafeOwnerClaims/{claimId}

- userId
- cafeId
- status
    - PENDING
    - APPROVED
    - REJECTED
- message
- evidenceImageUrls[]
- reviewedBy
- reviewedAt
- createdAt

------------------------------------------------------------------------

## homeBanners/{bannerId}

- ownerType (ADMIN / CAFE_OWNER)
- ownerId
- relatedCafeId
- title
- subtitle
- imageUrl
- linkType
- linkTarget
- priority
- maxVisibleGroup
- startAt
- endAt
- status
    - DRAFT
    - SCHEDULED
    - ACTIVE
    - ENDED
    - PAUSED
- createdAt
- updatedAt

------------------------------------------------------------------------

## cafes/{cafeId}

- ownerIds[]
- name
- description
- region
    - country
    - city
    - address
    - location (GeoPoint)
- thumbnailImage
- images[]
- approved
- ratingAvg
- reviewCount
- conceptType (MAID / BUTLER / IDOL)
- createdAt

------------------------------------------------------------------------

## cafes/{cafeId}/casts/{castId}

- name
- profileImage
- images[]
- description
- birthday
- joinDate
- conceptRole (maid / butler / idol)
- followerCount
- linkedUserId

------------------------------------------------------------------------

## cafes/{cafeId}/casts/{castId}/externalLinks/{linkId}

- platform
- title
- url
- isVisible
- sortOrder
- createdAt
- updatedAt

------------------------------------------------------------------------

## castSchedules/{scheduleId}

- castId
- cafeId
- date
- startTime
- endTime

------------------------------------------------------------------------

## cafes/{cafeId}/menus/{menuId}

- name
- price
- description
- image
- category

------------------------------------------------------------------------

## cafes/{cafeId}/goods/{goodsId}

- name
- price
- image
- stock

------------------------------------------------------------------------

## cafes/{cafeId}/reviews/{reviewId}

- userId
- rating
- content
- images[]
- likeCount
- createdAt

------------------------------------------------------------------------

## cafes/{cafeId}/notices/{noticeId}

- title
- content
- createdAt

------------------------------------------------------------------------

## visits/{visitId}

- userId
- cafeId
- visitedAt
- verified

------------------------------------------------------------------------

## castFollowers/{castId}/users/{userId}

- followedAt

------------------------------------------------------------------------

## cafes/{cafeId}/externalLinks/{linkId}

- platform
- title
- url
- isVisible
- sortOrder
- createdAt
- updatedAt

------------------------------------------------------------------------

# 6. 핵심 기능 상세

## ⭐ 캐스트 팔로우 시스템

- 팔로우 시 출근 알림
- 생일 알림
- 이벤트 알림
- "마이 캐스트" 전용 화면 제공

------------------------------------------------------------------------

## 🏆 인기 캐스트 랭킹

- 팔로우 수
- 리뷰 언급 수
- 방문 인증 수
- 좋아요 수

기반 점수 계산.

------------------------------------------------------------------------

## 📅 출근표 UI

- 주간 캘린더
- 캐스트별 근무 시간 표시

------------------------------------------------------------------------

## 📍 위치 기반 방문 인증

- 카페 반경 100m 이내 체크인 가능
- 방문자만 리뷰 작성 가능

------------------------------------------------------------------------

## 🎀 홈 배너 운영 정책

- 홈 상단 배너는 관리자와 카페 운영자가 등록할 수 있다.
- 배너는 홈 메인에서 최대 3~5개까지만 동시 노출한다.
- 모든 배너는 노출 시작일과 종료일을 반드시 가진다.
- 현재 시간이 노출 기간 안에 있는 배너만 `ACTIVE` 상태로 노출된다.
- 활성 배너 수가 허용 개수에 도달하면 신규 배너는 즉시 노출할 수 없고 `SCHEDULED` 상태의 예약 등록만 가능하다.
- 예약 배너는 종료 예정 배너 이후 순번대로 활성화된다.
- 관리자는 전체 배너를 관리할 수 있고, 카페 운영자는 본인이 운영 권한을 가진 카페와 연결된 배너만 관리할 수 있다.
- 배너 클릭 시 카페 상세, 이벤트 상세, 공지, 외부 프로모션 링크 등으로 연결할 수 있다.

------------------------------------------------------------------------

## 🧭 홈 배너 관리 화면 기획

### 1) 카페 운영자 화면

- 진입 위치: `카페관리 > 홈 배너 관리`
- 탭 구성: `진행중`, `예약`, `종료`
- 제공 기능:
  - 배너 등록
  - 배너 수정
  - 예약 배너 일정 변경
  - 종료 배너 재등록
- 제한 규칙:
  - 본인이 운영 권한을 가진 카페와 연결된 배너만 조회/수정 가능
  - 활성 슬롯이 모두 찬 경우 새 배너는 자동으로 `SCHEDULED` 상태로 저장
  - 이미 종료된 배너는 직접 `ACTIVE`로 변경할 수 없고 재등록 플로우를 거쳐야 함

### 2) 관리자 화면

- 진입 위치: `운영관리 > 홈 배너 운영 관리`
- 탭 구성: `초안`, `진행중`, `예약`, `종료`, `중지`
- 제공 기능:
  - 전체 배너 조회
  - 등록 주체별 필터링
  - 강제 중지
  - 우선순위 조정
  - 슬롯 수 및 기간 충돌 확인
- 운영 원칙:
  - 플랫폼 공지성 배너는 관리자가 직접 등록 가능
  - 카페 운영자 등록 배너도 관리자가 수정 또는 중지 가능
  - 기간이 겹치는 경우 우선순위가 높은 배너를 먼저 활성화

### 3) 공통 배너 등록 플로우

1. 배너 유형 선택
2. 배너 이미지 업로드
3. 제목 / 서브 문구 입력
4. 연결 대상 설정
5. 노출 시작일 / 종료일 입력
6. 저장 후 상태 자동 계산

### 4) 저장 시 상태 결정 규칙

- 노출 시작일 전이면 `SCHEDULED`
- 현재 시각이 노출 기간 안이고 활성 슬롯 여유가 있으면 `ACTIVE`
- 현재 시각이 노출 기간 안이지만 활성 슬롯이 가득 차 있으면 `SCHEDULED`
- 운영자가 직접 중지하면 `PAUSED`
- 노출 종료일이 지나면 `ENDED`

------------------------------------------------------------------------

## ⚙️ 설정 화면

- 마이 페이지 하위 상세 화면으로 제공
- 현재 단계 범위는 `Settings` 스크린 UI 추가와 네비게이션 연결만 포함
- 포함 항목: 계정 관리, 알림 설정, 앱 버전 정보, SignOut
- 설정값 저장/서버 동기화는 후속 단계로 이관

------------------------------------------------------------------------

## 🏅 스탬프 시스템

- 방문 시 스탬프 적립
- 일정 횟수 달성 시 배지 지급
- 팬 활동 레벨 시스템

------------------------------------------------------------------------

## 📣 공지사항

- 카페 운영자 공지
- 캐스트 공지

------------------------------------------------------------------------

# 7. 이미지 블러 기능 (프리미엄)

카페 운영자 또는 캐스트는  
이미지 업로드 시 **블러 처리 기능** 사용 가능.

### 사용 예

- 체키 일부 가리기
- 얼굴 보호
- 굿즈 미리보기

### 이용 방식

------------------------------------------------------------------------

# 8. 🏪 Multi-Cafe Owner 구조 설계

ConCafe에서는 하나의 카페 운영자가 여러 카페를 운영할 수 있다.

예시

Owner A
- Maid Dream Tokyo
- Seoul Maid Cafe
- Akihabara Butler Cafe

따라서 Owner와 Cafe의 관계는 다음과 같다.

Owner (1) → Cafe (N)

### 설계 원칙

- 하나의 운영자 계정은 복수의 카페 운영 권한을 가질 수 있다.
- 하나의 카페는 복수의 운영자 계정을 가질 수 있다.
- 단일 대표 운영자만 가정하지 않고 공동 운영 및 지점 관리 시나리오를 지원한다.
- 카페 단위 권한 검사는 `ownerIds` 또는 `ownedCafeIds` 기준으로 수행한다.

### Firestore 구조

## users/{userId}

- role: VISITOR | CAST | CAFE_OWNER | ADMIN
- ownedCafeIds: [cafeId]

## cafes/{cafeId}

- name
- description
- ownerIds: [userId]
- createdAt
- approved

`ownerIds`는 여러 운영자를 지원하기 위해 배열 구조로 설계한다.

예시

## cafes/{cafeId}

- ownerIds:
  - ownerA
  - ownerB

이 구조는 공동 운영이나 지점 관리 상황에서도 유연하게 동작한다.

### 권한 해석 규칙

- `CAFE_OWNER` 권한 사용자가 앱에 로그인하면 `ownedCafeIds`에 포함된 카페 목록을 조회할 수 있다.
- 특정 카페 관리 화면 진입 가능 여부는 해당 `cafeId`가 `ownedCafeIds`에 포함되는지로 판단한다.
- 카페 문서의 `ownerIds`와 사용자 문서의 `ownedCafeIds`는 동일한 관계를 양방향으로 표현한다.
- 운영자 초대 또는 공동 운영자 추가 기능이 생기더라도 기본 관계 모델은 유지한다.
- 운영자 Claim은 `cafeOwnerClaims/{claimId}`를 통해 생성되며, 승인 전까지는 실제 운영 권한으로 간주하지 않는다.

------------------------------------------------------------------------

# 9. 📱 카페 운영자 전용 탭 UX 설계

ConCafe 하단 네비게이션 3번째 탭은 사용자 역할에 따라 다르게 표시된다.

일반 사용자

- 탭 이름: Check-in

카페 운영자

- 탭 이름: Cafe Manage

### 탭 전환 원칙

- 운영자 계정은 3번째 메인 탭에서 `Cafe Manage`를 사용한다.
- 운영자가 여러 카페를 운영하는 경우에도 탭은 하나만 노출하고, 탭 내부에서 관리 대상을 선택한다.
- 운영자 권한이 없는 사용자는 `Cafe Manage` 화면에 접근할 수 없다.

## Cafe Manage UX 구조

카페 운영자가 여러 카페를 운영할 수 있으므로 먼저 `내 카페 목록`을 보여준다.

### My Cafes 화면

My Cafes

- Maid Dream Tokyo
- Seoul Maid Cafe
- Akihabara Butler Cafe

카페를 선택하면 해당 카페의 관리 화면으로 이동한다.

### My Cafes 화면 구성

- 상단 제목: `내 카페`
- 운영 중인 카페 수 표시
- 카페 카드 항목:
  - 카페 이름
  - 대표 이미지
  - 지역
  - 승인 상태
  - 오늘 체크인 수 요약
- 카드 탭 시 선택한 카페의 `Cafe Dashboard`로 이동

### Firestore 조회 기준

## users/{userId}

- ownedCafeIds: [cafeId]

## cafes/{cafeId}

- name
- thumbnailImage
- region
- approved

------------------------------------------------------------------------

## Cafe Manage Empty State

카페 운영자로 로그인했지만 아직 연결된 운영 카페가 없으면 빈 상태 화면을 노출한다.

### Empty State 목적

- 카페 운영자가 `카페관리` 탭에서 막히지 않도록 다음 행동을 명확히 제시한다.
- 관리자가 미리 등록한 카페를 검색해 운영 권한을 신청할 수 있게 한다.
- 등록된 카페가 없을 경우 신규 카페 등록으로 자연스럽게 연결한다.

### 화면 구성

- 일러스트 또는 빈 상태 카드
- 제목: `아직 연결된 운영 카페가 없습니다`
- 설명: `기존 카페를 검색해 운영 권한을 신청하거나 새 카페를 등록하세요`
- CTA 1: `기존 카페 검색`
- CTA 2: `새 카페 등록`
- 하단 보조 문구: `운영자 신청은 관리자 승인 후 반영됩니다`

### Empty State 진입 조건

- 로그인 사용자의 역할이 `CAFE_OWNER`
- `users/{userId}.ownedCafeIds`가 비어 있음
- 진행 중인 운영자 Claim이 있어도 승인 전에는 Empty State를 유지하되 신청 상태 카드를 함께 노출할 수 있음

------------------------------------------------------------------------

## Existing Cafe Claim UX

관리자가 이미 등록해둔 카페가 있으면 운영자는 검색 후 해당 카페에 운영 권한을 신청할 수 있다.

### 검색 화면 구성

- 검색바: 카페명 / 지역 / 주소 검색
- 필터: 국가 / 도시 / 승인 상태
- 결과 카드:
  - 카페 이름
  - 대표 이미지
  - 지역
  - 승인 상태
  - `이 카페 운영자 신청` 버튼

### Claim 신청 플로우

1. 카페 검색
2. 카페 선택
3. 운영자 신청 폼 입력
4. 증빙 자료 업로드 선택
5. 신청 제출
6. 관리자 승인 대기

### 신청 폼 항목

- userId
- cafeId
- 메시지
- 증빙 이미지

### 신청 상태 화면

- 상태값: `PENDING`, `APPROVED`, `REJECTED`
- `PENDING`이면 `승인 대기 중` 배지와 예상 처리 안내 노출
- `APPROVED`이면 자동으로 내 카페 목록으로 이동 가능
- `REJECTED`이면 사유 노출 및 재신청 액션 제공 가능

### Firestore 연동 대상

## cafeOwnerClaims/{claimId}

- userId
- cafeId
- status
- message
- evidenceImageUrls[]
- reviewedBy
- reviewedAt
- createdAt

------------------------------------------------------------------------

## Cafe Dashboard

카페 운영자가 앱을 열면 먼저 운영 현황을 확인할 수 있다.

예시

- 오늘 방문자
- 오늘 체크인
- 오늘 리뷰
- 평점

예시 UI

- 오늘 방문 18
- 체크인 12
- 리뷰 3
- 평점 4.7

### Dashboard 구성

- 선택된 카페명과 대표 이미지
- 오늘 운영 요약 카드
- 빠른 이동 메뉴:
  - Cast Management
  - Cast Schedule
  - Event Management
  - Cafe Settings
  - 홈 배너 관리
- 최근 공지 또는 최근 리뷰 요약

### Dashboard 데이터 조합 기준

- 오늘 방문자: 방문 기록 수 기반 집계
- 오늘 체크인: 당일 인증 체크인 수
- 오늘 리뷰: 당일 생성 리뷰 수
- 평점: 카페 누적 평균 평점

### Firestore 조회 대상

## visits/{visitId}

- cafeId
- visitedAt
- verified

## cafes/{cafeId}/reviews/{reviewId}

- rating
- createdAt

## cafes/{cafeId}

- ratingAvg

------------------------------------------------------------------------

## Cast Management

카페에 소속된 캐스트 관리 기능

가능 기능

- 캐스트 추가
- 캐스트 프로필 수정
- 캐스트 사진 업로드
- 캐스트 스케줄 등록
- 캐스트 외부 SNS 링크 관리

### Firestore

## cafes/{cafeId}/casts/{castId}

- name
- profileImage
- description
- birthday
- joinDate

### UX 구성

- 캐스트 목록
- 검색 또는 정렬
- `캐스트 추가` 버튼
- 캐스트 카드 탭 시 상세 편집 화면 이동
- 캐스트별 최근 스케줄 및 팔로워 수 요약 노출 가능

------------------------------------------------------------------------

## Cast Schedule

캐스트 출근 스케줄 관리

Today's Cast

- Sakura 14:00 - 20:00
- Miku 12:00 - 18:00

### Firestore

## castSchedules/{scheduleId}

- cafeId
- castId
- date
- startTime
- endTime

### UX 구성

- 날짜 선택
- 해당 날짜 출근 캐스트 리스트
- 스케줄 추가 버튼
- 시간 수정 및 삭제 액션
- 캐스트별 주간 보기 확장 가능

------------------------------------------------------------------------

## Event Management

카페 이벤트 관리

예시

- Sakura Birthday Event
- Golden Week Event

가능 기능

- 이벤트 생성
- 이벤트 수정
- 이벤트 삭제

### Firestore

## cafes/{cafeId}/events/{eventId}

- eventType
- relatedCastId
- startDate
- endDate
- description

### UX 구성

- 진행중 / 예정 / 종료 이벤트 구분
- 이벤트 카드 목록
- `이벤트 생성` 버튼
- 캐스트 연계 이벤트와 카페 단독 이벤트를 모두 등록 가능

------------------------------------------------------------------------

## Cafe Settings

카페 기본 정보 관리

수정 가능 항목

- 카페 이름
- 카페 설명
- 주소
- 카페 이미지
- 메뉴
- 굿즈
- 외부 링크

### Firestore

## cafes/{cafeId}

- name
- description
- region
- images
- thumbnailImage

## cafes/{cafeId}/externalLinks/{linkId}

- platform
- title
- url
- isVisible
- sortOrder

### UX 구성

- 기본 정보 수정 폼
- 이미지 업로드 영역
- 메뉴 관리 바로가기
- 굿즈 관리 바로가기
- 외부 링크 관리 섹션
- 지원 플랫폼 예시: Instagram / X / TikTok / YouTube / Website
- 저장 후 카페 상세 화면과 운영 화면에 즉시 반영되는 구조를 목표로 한다

------------------------------------------------------------------------

## Cast External Links

캐스트는 본인 외부 SNS 계정을 등록할 수 있고, 등록된 링크는 캐스트 상세 화면에 노출한다.

### 지원 플랫폼 예시

- Instagram
- X
- TikTok
- YouTube

### 운영 원칙

- 캐스트 또는 권한을 가진 운영자는 해당 캐스트의 외부 링크를 등록/수정할 수 있다.
- 노출 여부가 `isVisible = true`인 링크만 상세 화면에 노출한다.
- 외부 링크는 팔로우 유도 및 공식 채널 안내 목적의 보조 정보로 취급한다.

### Firestore

## cafes/{cafeId}/casts/{castId}/externalLinks/{linkId}

- platform
- title
- url
- isVisible
- sortOrder

- 블러 영역 선택
- 이미지 저장

### 이용권 모델

1️⃣ 구독형

월 이용권

2️⃣ 크레딧형

블러 사용 시 크레딧 차감

------------------------------------------------------------------------

# 10. 보안 및 권한 설계 핵심

- CAFE_OWNER는 본인이 운영 권한을 가진 카페만 관리 가능
- CAST는 본인 프로필만 수정 가능
- ADMIN은 전체 수정 가능
- VISITOR는 조회 및 리뷰만 가능
- 리뷰는 방문 인증 기록 필요
- 카페 등록은 Admin 승인 필요
- 운영자 Claim은 Admin 승인 필요

------------------------------------------------------------------------

# 11. MVP 개발 로드맵

## 🟢 1단계

- 카페 목록
- 카페 상세
- 캐스트 목록
- 리뷰 작성
- 즐겨찾기

## 🟡 2단계

- 캐스트 팔로우
- 출근 캘린더
- 위치 인증 방문
- 랭킹 시스템
- 스탬프 시스템

## 🔴 3단계

- 관리자 콘솔
- 카페 운영자 관리 대시보드
- 캐스트 계정 연결
- 통계 기능
- 이미지 블러 기능

------------------------------------------------------------------------

# 12. 수익 모델

1. 프리미엄 노출 광고
2. 상단 고정 업체
3. 굿즈 판매 수수료
4. 이벤트 티켓 판매
5. 이미지 블러 이용권

------------------------------------------------------------------------

# 13. 기술 스택

## 모바일

Android: Jetpack Compose  
iOS: SwiftUI  
KMP Shared Domain

## Backend

Firebase Authentication  
Firestore  
Cloud Functions  
Cloud Storage

## 아키텍처

Clean Architecture  
MVVM  
Repository Pattern  
Role 기반 보안 규칙

------------------------------------------------------------------------

# 14. 프로젝트 비전

ConCafe는 단순한 카페 정보 앱이 아니라

> 🎀 컨셉 카페 문화 아카이빙 플랫폼 + 팬 커뮤니티 생태계 🎀

를 목표로 한다.

카페, 캐스트, 팬이 함께 성장하는  
**컨셉 카페 플랫폼 생태계 구축**을 지향한다.
