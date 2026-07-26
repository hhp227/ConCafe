# ConCafe MVP WBS 체크리스트

체크 기준:
- [x] 현재 코드베이스에 실제 구현이 확인된 항목만 표시
- mock/fake 데이터 기반 구현도 UI/흐름이 동작하면 구현으로 간주
- 플레이스홀더 텍스트만 있는 화면, 정책만 있고 라우팅/기능이 없는 항목은 미체크 유지

참고 문서:
- [UX plan.md](./UX%20plan.md)
- [# 🎀 ConCafe Firestore 컬렉션 다이어그램.md](./%23%20%F0%9F%8E%80%20ConCafe%20Firestore%20%EC%BB%AC%EB%A0%89%EC%85%98%20%EB%8B%A4%EC%9D%B4%EC%96%B4%EA%B7%B8%EB%9E%A8.md)
- [Maid_Cafe_Platform_Full_Project_Plan.md](./Maid_Cafe_Platform_Full_Project_Plan.md)

## 최근 정합성 반영 (2026-03-20)
- [x] Firestore 컬렉션 다이어그램 기준으로 `castSchedules`(상태 포함), `stamps`, `cafeOwnerClaims`, `cafeRegistrationClaims`, `castClaims` 책임을 MVP 범위 문서에 반영
- [x] 홈 배너 정책(`ACTIVE` 최대 5개, `SCHEDULED` 자동 승격, 링크 타입별 라우팅) 문서 기준 통일
- [x] Claim 흐름(캐스트: 팬관리 신청 -> 운영자 승인, 운영자/신규 카페: Admin 승인) 기획서 기준 통일
- [x] `users/{userId}` 중심 역할/소유 카페 연결(`ownedCafeIds`) 정책을 운영/권한 항목에 반영
- [x] Repository + DataSource 패턴에서 사용자 목록은 데이터소스 구현 내부 상태로 관리하고, Repository는 인터페이스 메서드 기반 조회/갱신을 사용하도록 정리

## 최근 정합성 반영 (2026-03-22)
- [x] 앱 런타임 데이터소스가 Firestore 경로를 사용하고, `MockConCafeDataSource`는 테스트 코드로만 분리됨
- [x] Firebase Auth 기반 세션 복원이 Android/iOS/Desktop 공통 흐름(`restoreSession` + `observeCurrentUser`)으로 정렬됨
- [x] Admin 운영관리 승인 대기 목록 조회가 Firestore pending claim 조회 경로로 연결됨
- [x] Firestore Rules 초안에 Admin의 claim 조회(read) 허용 규칙이 반영됨
- [x] 운영관리 승인 대기 카드 이미지 노출이 Compose/iOS에 공통 반영됨
- [x] 캐스트 상세 조회 시 캐시 우선 + Firestore 동기화(카페 상세/스케줄/visits 재동기화 후 재조회) 경로가 공통 shared 로직으로 반영됨
- [x] 캐스트 상세 `방문 인증` 수치가 임시 계산식(팔로워/스케줄)에서 `visits` 기반 집계로 변경됨

## 최근 정합성 반영 (2026-03-23)
- [x] 캐스트 팔로우/언팔로우가 Firestore 실데이터 경로(`castFollows`)로 연결됨
- [x] 팔로우 변경 시 캐스트 `followerCount`가 캐시/화면 이벤트 경로로 즉시 반영되도록 보강됨
- [x] 카페 리뷰 CRUD가 Firestore 경로 기준으로 동작하도록 정리됨
- [x] 리뷰 변경 시 카페 집계(`reviewCount`, `ratingAvg`)를 Cloud Functions 트리거로 동기화하도록 반영됨
- [x] MyInfo 팔로우 캐스트 섹션에 이미지 노출이 반영되고, iOS는 이미지가 플레이스홀더 영역을 벗어나지 않도록 clip 처리됨

## 최근 정합성 반영 (2026-03-26)
- [x] 카페 상세 집계에서 캐스트별 상세 반복 조회(N+1) 제거
- [x] 카페 상세/리뷰 리스트의 방문인증 상태를 리뷰 문서 필드(`visitVerified`) 기반으로 사용하도록 정리
- [x] 리뷰/공지 탭 재진입 시 전체 원격 재동기화를 줄이고 최초 미캐시 진입 중심으로 최적화
- [x] Cloud Functions에 리뷰 방문인증 동기화 트리거 추가(`onReviewWrittenSyncReviewVisitVerified`, `onVisitWrittenSyncReviewVisitVerified`)
- [x] Functions 소스 로딩 타임아웃 개선을 위한 lazy Firestore 초기화 반영

## 0. 목표/범위
- [ ] MVP 목표 확정: 메이드 중심 팬 플랫폼 + 위치 인증 기반 신뢰 리뷰
- [ ] MVP 화면 범위 확정: 홈/탐색/역할별 3번째 탭(체크인/팬관리/카페관리/운영관리)/랭킹/마이 + 카페/메이드 상세
- [ ] 제외 범위 확정: Admin 웹 콘솔, 수익화 기능(2~3차로 이관)

## 0-1. 인증 UX 고정 정책 (기획 정합)
- [x] 앱 첫 실행 기본 진입 화면은 로그인 화면이 아닌 홈 화면으로 고정
- [x] 비로그인 상태(게스트)에서도 홈/탐색/상세 조회는 가능
- [x] 마이 페이지 진입 시 비로그인 상태면 게스트 화면을 노출하고, 로그인 버튼 탭 시 로그인 화면으로 이동
- [x] 로그인 성공 후 앱 재실행 시 세션 유지(자동 로그인)
- [ ] 로그아웃 시에만 세션 초기화 및 마이 페이지 재진입 시 로그인 요구
- [x] 메인 네비게이션 3번째 탭은 역할별로 교체됨: 게스트/`VISITOR`=`체크인`, `CAST`=`팬관리`, `CAFE_OWNER`=`카페관리`, `ADMIN`=`운영관리`
- [ ] 다중 역할 계정은 메인 네비게이션 우선순위 `ADMIN > CAFE_OWNER > CAST > VISITOR` 적용

## 0-2. 로그인 필요 기능 매트릭스 (전면 반영)
- [x] 게스트 가능: 홈/탐색/카페 상세/메이드 상세/랭킹 조회
- [ ] 로그인 필요: 프로필 수정
- [x] 로그인 필요: 즐겨찾기 토글, 메이드 팔로우/언팔로우
- [ ] 로그인 필요: 체크인 생성, 리뷰 작성/삭제, 리뷰 좋아요
- [ ] 로그인 필요: 알림 조회/읽음 처리
- [ ] 로그인 필요 기능에서 비로그인 접근 시 로그인 화면으로 라우팅
- [ ] 로그인 성공 시 원래 시도한 기능/화면으로 복귀

## 1. 사전 의사결정(필수)
- [x] `conceptType` enum 확정: `MAID | BUTLER | IDOL`
- [x] `events` 저장 위치 확정: `cafes/{cafeId}/events/{eventId}`
- [x] `favorites`/`visitHistory` 저장 방식 확정: `users/{userId}/favorites`, `users/{userId}/visits` 서브컬렉션
- [x] 참고 문서 정합성 반영: `events` 컬렉션 위치/집계 필드(`stats.*`) 표기 통일
- [ ] 랭킹 점수식 확정 (팔로우/리뷰 언급/인증 방문 가중치)
- [ ] 홈 섹션 우선순위 데이터 기준 확정 (인기/근처/생일/최신 공지)
- [x] 리뷰/프로필 이미지 업로드 용량·해상도 정책 확정
- [x] `CAST` 프로필 연결 claim / `CAFE_OWNER` 운영자 claim 승인 흐름 확정

## 2. 아키텍처 WBS
- [ ] 모듈 책임 고정: `composeApp`(UI), `shared`(도메인/유스케이스)
- [ ] 레이어 구조 정의: Presentation / Domain / Data
- [ ] Repository 인터페이스 목록 정의
- [ ] 에러 모델/결과 모델 통일 (`Success/Failure` 정책)
- [ ] 공통 상태 관리 패턴(MVI) 정의 (`uiState`, `event`, `action`)

## 2-1. 플랫폼별 네비게이션 아키텍처 (확정)
- [x] Android 네비게이션: Jetpack Navigation 적용
- [x] iOS 네비게이션: SwiftUI `NavigationStack` 적용
- [x] Desktop 네비게이션: 상태 기반 UI 확장(라우트 상태 전환)
- [ ] 공통 라우트 규격 정의: `Home/Explore/CheckIn/Ranking/MyInfo/Cafe/Cast/SignIn/Notification/Settings`
- [x] 메인 탭 규격 정의: 3번째 탭은 역할별 `CheckIn | FanManagement | CafeManagement | AdminOperations`
- [ ] 로그인 가드 동작 통일: 플랫폼별 구현체는 달라도 라우트 규칙은 동일
- [ ] 뒤로가기 규칙 통일: 상세 -> 이전, 로그인 -> 이전, 탭은 홈 복귀 우선
- [ ] 딥링크/외부 진입 시 로그인 가드 + pendingRoute 복귀 규칙 통일

## 3. 데이터/백엔드 WBS
- [x] Firestore 컬렉션 생성 기준 문서 확정
- [x] 핵심 컬렉션 책임 분리 확정: 루트 컬렉션 vs `users/*`, `cafes/*` 서브컬렉션
- [ ] `users.stats`, `cafes.stats` 집계 필드 소유자/갱신 주체 확정
- [ ] 핵심 인덱스 생성
- [ ] 인증/권한 룰 초안 작성
- [ ] Cloud Functions 목록 확정
- [ ] Storage 경로 규칙 정의 (카페/메이드/리뷰 이미지)
- [x] 컬렉션 범위 확정: `menus`, `goods`, `notices`, `events`, `favorites`, `followers`, `stamps`, `claims`

### 3-1. 인덱스 체크리스트
- [ ] 카페 탐색: `approved + region + ratingAvg`
- [ ] 카페 최신순: `approved + createdAt`
- [ ] 메이드 탐색: `cafeId + popularityScore` 또는 `birthday`
- [ ] 리뷰 정렬: `cafes/{cafeId}/reviews(createdAt desc)`
- [ ] 홈 공지 섹션: `cafes/{cafeId}/notices(createdAt desc)`
- [ ] 홈 생일 메이드 섹션: `cafes/{cafeId}/casts(birthday)`
- [ ] 출근표: `castSchedules(cafeId + date)`
- [ ] 출근표: `castSchedules(castId + date)`
- [ ] 즐겨찾기/팔로우 조회: `users/{userId}/favorites(createdAt desc)`, `castFollowers/{castId}/users(followedAt desc)`

### 3-2. 서버 검증 체크리스트
- [ ] 리뷰 작성 로그인 검증
- [ ] 체크인 시 반경 100m 위치 검증
- [ ] 평점 평균/리뷰 수 집계 원자적 업데이트
- [ ] 팔로워 수/랭킹 점수 집계 업데이트
- [ ] `banned` 사용자 쓰기 차단 검증
- [ ] `CAFE_OWNER` claim 승인 전 운영 쓰기 차단 / `CAST` 프로필 연결 전 캐스트 전용 쓰기 차단 검증
- [ ] 스탬프 적립 중복 방지 검증 (`visitId` 단위)

## 4. 보안/권한 WBS
- [ ] `ADMIN`, `CAFE_OWNER`, `VISITOR`, `CAST` 역할 정책 문서화
- [ ] `VISITOR` 쓰기 범위 제한 검증 (본인 데이터만)
- [ ] CAST 쓰기 범위 제한 검증 (본인 데이터만)
- [ ] `CAFE_OWNER` 수정 범위 제한 검증 (본인 cafe만)
- [ ] ADMIN 승인/반려/밴 권한 검증
- [ ] 승인되지 않은 카페 노출 제한 검증
- [x] `castClaims`, `cafeOwnerClaims` 승인 상태별 접근 권한 검증
- [ ] 공지/이벤트 작성 주체 검증 (`CAFE_OWNER` 기본, `CAST` 선택 허용 범위 명시)

## 5. 기능 구현 WBS (MVP)

### 5-1. 인증/프로필
- [x] 게스트 세션 + 로그인 플로우
- [ ] 프로필 조회/수정
- [x] 로그인 사용자 초기 데이터 생성
- [x] 세션 복원(앱 재실행 자동 로그인)
- [x] `AuthRepository.observeCurrentUser()` 기반 세션 변경 스트림 연결
- [x] 메인 네비게이션이 세션 변경 스트림을 구독해 로그인/로그아웃 시 즉시 재계산

### 5-2. 홈 탭
- [x] 인기 캐스트 섹션
- [x] 근처 카페 섹션
- [x] 생일 메이드 섹션
- [x] 최신 공지 3개 섹션
- [x] 홈 배너 등록/반영이 shared 데이터 기준으로 동작
- [x] 활성 배너 최대 5개 / 초과 시 `SCHEDULED` 정책 반영
- [x] 예약 배너가 빈 슬롯 발생 시 자동 승격
- [x] 외부 링크 배너 클릭 시 인앱 `ExternalLink` WebView 화면으로 이동
- [x] 인기 캐스트 / 근처 카페 페이지네이션 연결
- [ ] 섹션별 fallback 규칙 확정 (데이터 부족 시 숨김/대체 카드)

### 5-3. 탐색 탭
- [x] 검색바
- [x] 지역 필터(KR/JP/도시)
- [x] 정렬(인기/최신/평점)
- [x] 내부 탭(카페/메이드)
- [x] 카페/메이드 탭 무한 스크롤 페이지네이션
- [x] 카페/메이드 전용 page use case 분리

### 5-4. 카페 상세
- [x] 상단 이미지 슬라이더/기본 정보
- [x] 탭: 정보/메이드/메뉴/리뷰/공지
- [x] 즐겨찾기 토글(비로그인 시 로그인 라우팅)
- [x] 상세 진입은 플랫폼 네비게이션 스택 push 방식으로 처리 (모달 금지)
- [x] 메뉴/굿즈/이벤트 데이터 노출 범위 확정
- [x] 공지/이벤트 작성자 표기 규칙 반영

### 5-5. 메이드 상세
- [x] 프로필/소개/소속 카페
- [x] 팔로우 버튼(비로그인 시 로그인 라우팅)
- [x] 출근 캘린더 영역(읽기 전용 최소 구현)
- [x] 상세 진입은 플랫폼 네비게이션 스택 push 방식으로 처리 (모달 금지)
- [ ] 캐스트 공식 SNS 링크 노출(Instagram/X/TikTok 등)

### 5-6. 체크인
- [x] 체크인 탭 상단 지도는 로그인 여부와 관계없이 공통 노출
- [x] 카페 선택
- [x] 날짜 선택
- [ ] 위치 인증
- [x] 메모 작성
- [x] 방문 등록 및 타임라인 반영(공통 유스케이스 기반 임시 저장 + 즉시 반영, 비로그인 시 로그인 유도)
- [x] 게스트/`VISITOR` 역할에서만 메인 탭 3번째 위치에 노출

### 5-6-1. 역할별 3번째 메인 탭
- [x] `CAST` 로그인 시 `팬관리` 탭 노출
- [x] `팬관리` 메인 화면 UI를 Compose/iOS 공통 상태 구조로 구현
- [x] `CAFE_OWNER` 로그인 시 `카페관리` 탭 노출
- [x] `ADMIN` 로그인 시 `운영관리` 탭 노출
- [x] 역할 변경/로그아웃 시 메인 탭 구성이 즉시 갱신
- [ ] 다중 역할 계정은 우선순위 규칙에 따라 1개 탭만 노출
- [x] `CAFE_OWNER`이지만 연결된 운영 카페가 없으면 `카페관리 Empty State` 노출
- [x] Empty State에서 `기존 카페 검색`과 `새 카페 등록` CTA 제공
- [x] 다중 카페 운영 전제를 반영한 `내 카페 목록` / `운영 대시보드` UI 제공
- [x] `GetCafeManagementUseCase` 기반 카페관리 데이터 로딩 연결
- [x] `CafeManagementRepository` 추가 및 운영 카페/검색 카페/신청 상태 조합
- [x] `CafeManagementRepository`가 `ConCafeDataSource`를 원천 데이터로 사용하도록 정리
- [x] 기존 카페 검색 결과에서 `이 카페 운영자 신청` 진입 가능
- [x] 운영자 Claim `PENDING/APPROVED/REJECTED` 상태 카드 노출
- [x] 카페 대시보드 외부 링크 섹션 UI 제공
- [ ] 카페 외부 링크 CRUD(`cafes/{cafeId}/externalLinks`)
- [ ] 캐스트 외부 링크 CRUD(`cafes/{cafeId}/casts/{castId}/externalLinks`)
- [x] 카페 대시보드에서 `카페 정보 관리` 화면으로 진입 가능
- [x] 카페 정보 수정 화면 초기값은 `GetCafeDetailUseCase`로 로드
- [x] 카페 정보 수정 저장은 `UpdateCafeInfoUseCase`를 통해 처리
- [x] 저장 성공 시 `MockConCafeDataSource` 기준 카페 정보가 즉시 갱신됨
- [x] 저장 성공 피드백은 Compose 스낵바 / iOS alert로 노출
- [x] 카페 대시보드에서 `메뉴&굿즈` 화면으로 진입 가능
- [x] `메뉴&굿즈 관리` 화면 UI를 Compose/iOS에 공통 상태 구조로 구현
- [x] `GetCafeDetailUseCase` 기반으로 메뉴/굿즈 목록, 카테고리 필터, 검색, 판매 상태 토글 UI 연결
- [x] `메뉴&굿즈 편집` 화면에서 메뉴/굿즈 추가 및 수정 저장 가능
- [x] `메뉴&굿즈 관리` 목록에서 삭제 확인 UX 후 실제 삭제 가능
- [x] 메뉴/굿즈 목록은 공용 `observeCafeDetail` 스트림으로 즉시 동기화
- [x] 카페 대시보드 캐스트 `+` 버튼에서 `캐스트 프로필 추가` 화면으로 진입 가능
- [x] `캐스트 프로필 추가/수정` 화면 UI를 Compose/iOS에 공통 상태 구조로 구현
- [x] 수정 모드 초기값은 `GetCastDetailUseCase`로 실제 캐스트 상세를 로드
- [x] `캐스트 프로필 추가/수정` 저장은 `UpsertCastUseCase`를 통해 처리
- [x] 캐스트 이름/컨셉 역할/생일/소개/근무 요일 수정 결과가 KMP shared mock 데이터에 반영됨
- [x] 팬관리에서 캐스트 프로필 연결 신청 UI 제공
- [x] 카페 대시보드 캐스트 관리 섹션에서 캐스트 연결 요청 승인/반려 가능
- [x] 카페 대시보드에서 선택된 캐스트 프로필 삭제 가능
- [x] 카페 대시보드 외부 링크 추가/삭제/인앱 WebView 이동 UI 구현
- [x] 새 카페 등록은 카페 정보 입력 폼 재사용 + `cafeRegistrationClaims` 생성
- [x] Admin 운영관리에서 기존 카페 운영자 신청 / 신규 카페 등록 신청 승인·반려 가능

### 5-6-2. 출근표 관리
- [x] 출근표 수정 바텀시트/모달 UI
- [x] `근무 / 휴무 / 휴가` 상태 수정
- [x] 출근표 수정 저장이 shared KMP 데이터에 실제 반영
- [x] `ScheduleManagementEvent` explicit event로 화면 재조회 갱신
- [ ] 출근표 전체 주간 일괄 저장 플로우

### 5-7. 리뷰
- [x] 로그인 사용자 리뷰 작성 가능
- [x] 별점/내용/이미지 등록
- [x] 카페 리뷰 작성 시 같은 카페 소속 캐스트 태그 선택 가능
- [x] 최신순 목록/좋아요 수 표시
- [x] 리뷰 CRUD가 Firestore 실데이터 경로로 반영
- [ ] 캐스트 상세에는 `함께 언급된 후기`로 태그된 카페 리뷰만 노출
- [ ] 리뷰 작성/삭제/좋아요는 로그인 필요(비로그인 시 로그인 라우팅)
- [x] 방문 인증 사용자는 리뷰 작성 화면/리뷰 아이템에 방문 인증 마크 표시
- [x] 리뷰 이미지 업로드 실제 연결

### 5-8. 마이 페이지
- [x] 프로필/레벨/총 방문 수
- [x] 방문 기록
- [x] 즐겨찾기 카페
- [x] 팔로우 메이드/배지 섹션 자리 확보
- [x] 팔로우한 캐스트 섹션 이미지 노출(Compose/iOS)
- [x] 캐스트 상세에서 팔로우 상태 변경 시 MyInfo 목록/카운트 즉시 동기화
- [x] 설정 진입 버튼 추가
- [x] `Settings` 스크린 추가
- [x] 설정 화면에서 `SignOut` 진입점 제공
- [x] 설정 화면에서 `계정 관리` / `알림 설정` / `개인정보 처리방침` 상세 진입 제공
- [x] 비로그인 마이페이지 게스트 화면 노출 + 로그인 버튼으로 로그인 화면 진입
- [ ] 스탬프/배지 데이터 연결을 위한 placeholder 상태 정의

### 5-9. Presentation 정리
- [ ] 공통 로그인 가드 + `pendingRoute/pendingAction` 복귀 UX 통일
- [ ] 설정 계정 관리 저장을 실제 서버 저장 플로우로 연결
- [ ] 비밀번호 변경 화면을 실제 변경 API에 연결
- [x] 리뷰 사진 업로드 placeholder 제거 후 실제 업로드 연결
- [x] 캐스트 프로필 이미지/갤러리 업로드 실제 연결
- [x] 카페 대표/갤러리 이미지 업로드 실제 연결
- [ ] 카페관리/팬관리/배너/출근표의 `다음 단계` placeholder 액션 정리

## 6. Phase 2 WBS (팬 기능)
- [x] 메이드 팔로우/언팔로우
- [ ] 출근/생일/이벤트 알림
- [ ] 주간 출근 캘린더
- [x] 메이드/카페 랭킹(주간/월간, 지역 필터)
- [ ] 스탬프/배지/레벨 시스템
- [ ] 리뷰 언급 수/좋아요 수 기반 랭킹 집계 반영
- [ ] 홈/랭킹/알림 공통 집계 필드 재사용 구조 확정

## 7. Phase 3 WBS (운영 고도화)
- [ ] Admin 콘솔(승인/신고/밴)
- [ ] Owner 대시보드(카페/메이드/공지/출근표/통계)
- [ ] Owner Claim / Cast Claim 승인 UI 및 이력 관리
- [ ] 다중 카페 운영자 연결 관리(`ownedCafeIds` / `ownerIds`) 반영
- [x] Owner `메뉴&굿즈 관리` 운영 화면(Android/iOS)
- [x] 메뉴/굿즈 운영 CRUD(mock shared 데이터 기준)
- [x] Owner/CAST 공용 `캐스트 프로필 추가/수정` 화면(Android/iOS)
- [x] 캐스트 프로필 기본 정보 CRUD(mock shared 데이터 기준, 이미지 업로드 제외)
- [x] 이벤트 운영 CRUD(mock shared 데이터 기준)
- [ ] 광고/상단 고정/수수료 기능
- [ ] 글로벌 확장(한국→일본) 다국가 운영 정책

## 8. 테스트/QA 체크리스트
- [ ] (후순위) 도메인 유스케이스 단위 테스트
- [ ] (후순위) Repository 계약 테스트
- [ ] (후순위) 화면 스모크 테스트(탭 이동/상세 진입/체크인)
- [ ] (후순위) 권한 룰 시나리오 테스트(`VISITOR`/`CAFE_OWNER`/`ADMIN`/`CAST`)
- [ ] (후순위) 성능 점검(목록 페이지네이션, 이미지 로딩)

## 9. 일정 체크리스트(8주)
- [ ] 1주차: 요구사항/스키마/권한 정책 확정
- [ ] 2주차: 인증/도메인/리포지토리 뼈대
- [ ] 3주차: 홈/탐색/카페상세
- [ ] 4주차: 메이드상세/리뷰/즐겨찾기
- [ ] 5주차: 체크인/위치 인증/방문 기록
- [ ] 6주차: 팔로우/알림/출근표
- [ ] 7주차: 랭킹/스탬프/배지
- [ ] 8주차: MVP 구현 마무리/배포 준비
- [ ] 9주차 이후(후순위): 테스트/QA/성능 점검

## 10. 완료 정의(Definition of Done)
- [ ] MVP 핵심 플로우가 Android/iOS/Desktop에서 동일 동작
- [ ] 플랫폼별 네비게이션 구현체(Android/iOS/Desktop)가 공통 라우트 규격과 동일 동작을 보장
- [ ] 앱 첫 실행 시 홈 진입, 마이 페이지는 게스트/로그인 상태에 맞는 화면을 노출
- [x] 역할별 메인 네비게이션 3번째 탭이 로그인 역할에 따라 정확히 교체됨
- [ ] 로그인 후 앱 재실행 시 로그인 상태가 유지됨
- [ ] 로그인 필요 기능(즐겨찾기/팔로우/체크인/리뷰/좋아요/알림)은 모두 라우트 가드가 적용됨
- [ ] 리뷰 작성은 로그인 조건을 반드시 충족
- [ ] 방문 인증 배지는 실제 방문 인증 데이터와 일치
- [ ] 역할 기반 권한 위반 요청이 모두 차단
- [ ] 컬렉션/인덱스/집계 정책이 기획서와 다이어그램 기준으로 충돌 없이 문서화됨
- [ ] 주요 화면 로딩/전환이 목표 성능 기준 충족
- [ ] 운영 문서(스키마/룰/릴리즈 노트) 최신화

## 11. 구현 전 최종 점검 (추가 반영)
- [x] 환경 분리 정책 확정: `dev + prod` Firebase 프로젝트 분리
- [ ] 시크릿 관리 정책 확정: API 키/서비스 계정 키 저장소 및 노출 방지 규칙
- [ ] Firestore/Storage 보안 규칙 테스트 자동화 방식 확정
- [ ] Cloud Functions 배포/롤백 절차 확정
- [ ] 개인정보/운영 정책 확정: 계정 삭제, 데이터 보존 기간, 신고 처리 정책
- [x] 관측 지표 확정: Crash 리포팅 + 최소 이벤트(`sign_in_success`, `checkin_success`, `checkin_fail`, `review_create`, `favorite_toggle`, `follow_toggle`)
- [ ] 성능 기준 수치화: 초기 로딩 시간, 목록 스크롤 지표, 이미지 용량 기준
- [ ] 딥링크/푸시 진입 시 로그인 가드 동작 규칙 검증
- [ ] 오프라인/네트워크 불안정 시 UX 정책 확정(재시도, 캐시, 메시지)
- [ ] 운영 데이터 시드 정책 확정: 초기 카페 등록/승인/운영자 claim 부트스트랩 절차
- [x] 브랜치/릴리즈 정책 확정: `develop -> main` 머지 후 태그/릴리즈 노트 관리
