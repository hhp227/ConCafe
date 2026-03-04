# 🎀 Maid Cafe Platform -- 통합 기획서 (Full Version)

------------------------------------------------------------------------

# 1. 프로젝트 개요

## 📌 서비스 정의

메이드카페 정보 공유 + 메이드 중심 팬 커뮤니티 + 사업자 관리 플랫폼

단순 정보 제공 앱이 아닌,\
**메이드 중심 덕질 플랫폼 + 위치 기반 커뮤니티 서비스**를 목표로 한다.

------------------------------------------------------------------------

# 2. 서비스 목표

1.  전국 메이드카페 정보 통합
2.  메이드 중심 팬 플랫폼 구축
3.  사업 운영자와 유저 연결
4.  방문 인증 기반 신뢰 리뷰 시스템
5.  글로벌 확장 가능 구조 설계 (한국 → 일본)

------------------------------------------------------------------------

# 3. 유저 타입 정의 (Role 기반 설계)

## 👑 1) Admin

-   업체 승인 / 반려
-   신고 처리
-   리뷰 삭제
-   유저 밴 처리
-   사업자 인증 검수

## 🏠 2) 메이드카페 운영자 (Owner)

-   업체 등록 및 수정
-   메이드 등록 / 수정 / 삭제
-   메뉴 / 굿즈 관리
-   이벤트 일정 등록
-   출근표 관리
-   공지사항 작성
-   통계 확인 (조회수, 방문수 등)

## 👤 3) 일반 유저

-   업체 방문 기록
-   위치 기반 체크인
-   리뷰 작성 및 별점
-   좋아요
-   즐겨찾기
-   메이드 팔로우
-   스탬프 적립
-   방문 히스토리 관리

------------------------------------------------------------------------

# 4. 핵심 도메인 모델

## User

-   role (ADMIN / OWNER / USER)
-   profile
-   visitHistory
-   favorites
-   followedMaids

## Cafe

-   ownerId
-   casts
-   menus
-   goods
-   events
-   reviews
-   notices

## Cast

-   cafeId
-   images\[\]
-   schedule\[\]
-   conceptRole (maid / butler / idol)
-   birthday
-   popularityScore

## Review

-   userId
-   rating
-   content
-   images\[\]
-   likeCount

## Visit

-   userId
-   cafeId
-   visitedAt
-   verified (위치 인증 여부)

------------------------------------------------------------------------

# 5. Firestore 컬렉션 설계

## users/{userId}

-   role
-   nickname
-   profileImage
-   banned
-   createdAt

## cafes/{cafeId}

-   ownerId
-   name
-   description
-   region
-   address
-   location (GeoPoint)
-   thumbnailImage   // 대표 썸네일 (목록용)
-   images[]         // 상세 페이지용 다중 이미지
-   approved
-   ratingAvg
-   reviewCount
-   conceptType: "MAID" | "BUTLER" | "IDOL" | "COLLAB" | "POPUP"
-   createdAt

## cafes/{cafeId}/casts/{castId}

-   name
-   profileImage
-   images\[\]
-   description
-   birthday
-   joinDate
-   conceptRole (maid / butler / idol)
-   followerCount

## castSchedules/{scheduleId}

-   castId
-   cafeId
-   date
-   startTime
-   endTime

## cafes/{cafeId}/menus/{menuId}

-   name
-   price
-   description
-   image
-   category (food/drink)

## cafes/{cafeId}/goods/{goodsId}

-   name
-   price
-   image
-   stock

## cafes/{cafeId}/reviews/{reviewId}

-   userId
-   rating
-   content
-   images\[\]
-   likeCount
-   createdAt

## cafes/{cafeId}/notices/{noticeId}

-   title
-   content
-   createdAt

## visits/{visitId}

-   userId
-   cafeId
-   visitedAt
-   verified

## castFollowers/{castId}/users/{userId}

-   followedAt

------------------------------------------------------------------------

# 6. 핵심 기능 상세

## ⭐ 메이드 팔로우 시스템

-   팔로우 시 출근 알림
-   생일 알림
-   이벤트 알림
-   "마이 메이드" 전용 화면 제공

## 🏆 인기 메이드 랭킹

-   월간 좋아요 수
-   리뷰 언급 수
-   방문 인증 수 기반 점수 계산

## 📅 출근표 UI

-   주간 캘린더 형태 제공
-   날짜별 근무 시간 표시

## 📍 위치 기반 방문 인증

-   카페 반경 100m 이내 체크인 가능
-   방문자만 리뷰 작성 가능
-   리뷰 신뢰도 상승

## 🏅 스탬프 시스템

-   방문 시 스탬프 적립
-   일정 횟수 도달 시 배지 지급
-   팬 활동 시 레벨 상승

## 📣 공지사항 기능

-   운영자가 직접 공지 작성
-   오늘 휴무, 이벤트 안내 등

------------------------------------------------------------------------

# 7. 보안 및 권한 설계 핵심

-   OWNER는 본인 cafe만 수정 가능
-   ADMIN은 전체 수정 가능
-   USER는 리뷰/방문 기록만 작성 가능
-   리뷰는 방문 인증 기록이 있어야 작성 가능
-   사업자 등록 시 Admin 승인 필수

------------------------------------------------------------------------

# 8. MVP 개발 로드맵

## 🟢 1단계 (기본 플랫폼)

-   카페 목록
-   카페 상세
-   메이드 목록 및 상세
-   리뷰 작성
-   즐겨찾기

## 🟡 2단계 (팬 기능 강화)

-   메이드 팔로우
-   출근 캘린더
-   위치 인증 방문
-   랭킹 시스템
-   스탬프 시스템

## 🔴 3단계 (플랫폼 고도화)

-   관리자 웹 콘솔
-   Owner 대시보드
-   통계 기능
-   수익 모델 적용
-   글로벌 확장

------------------------------------------------------------------------

# 9. 수익 모델

1.  프리미엄 노출 광고
2.  상단 고정 업체
3.  굿즈 판매 수수료
4.  이벤트 티켓 판매 수수료
5.  배너 광고

------------------------------------------------------------------------

# 10. 기술 스택 제안

## 모바일

-   Android: Jetpack Compose
-   iOS: SwiftUI
-   KMP Shared Domain

## Backend

-   Firebase Authentication
-   Firestore
-   Cloud Functions
-   Cloud Storage

## 아키텍처

-   Clean Architecture
-   MVVM
-   Repository Pattern
-   Role 기반 보안 규칙

------------------------------------------------------------------------

# 11. 차별화 전략

-   단순 카페 정보 앱이 아닌 "메이드 중심 팬 플랫폼"
-   위치 인증 기반 신뢰 리뷰
-   출근 알림 기반 팬 유지
-   랭킹 + 스탬프 기반 Gamification

------------------------------------------------------------------------

# 12. 프로젝트 비전

이 플랫폼은 단순한 정보 공유 앱이 아니라,

> 🎀 메이드 문화 아카이빙 플랫폼 + 팬 커뮤니티 생태계 🎀

확장성, 수익성, 커뮤니티 지속성을 모두 고려한 구조로 설계한다.
