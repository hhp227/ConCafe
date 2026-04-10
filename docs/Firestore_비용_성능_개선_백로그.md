# ConCafe Firestore 비용/성능 개선 백로그

기준 일자: 2026-04-10

기준 문서:
- [ConCafe_구현_실행작업_백로그.md](./ConCafe_%EA%B5%AC%ED%98%84_%EC%8B%A4%ED%96%89%EC%9E%91%EC%97%85_%EB%B0%B1%EB%A1%9C%EA%B7%B8.md)
- [# 🎀 ConCafe Firestore 컬렉션 다이어그램.md](./%23%20%F0%9F%8E%80%20ConCafe%20Firestore%20%EC%BB%AC%EB%A0%89%EC%85%98%20%EB%8B%A4%EC%9D%B4%EC%96%B4%EA%B7%B8%EB%9E%A8.md)

## 문서 목적
- Firestore 읽기/쓰기 비용이 큰 지점을 먼저 줄인다.
- 화면 진입 성능과 Cloud Functions 트리거 비용을 함께 개선한다.
- 작업자가 순서대로 체크하며 진행할 수 있는 실행형 백로그로 유지한다.

## 운영 규칙
- 우선순위: `P0(즉시)` / `P1(중요)` / `P2(후속)`
- 상태: `TODO` / `DOING` / `DONE` / `BLOCKED`
- 체크박스 규칙:
  - `[ ]` 미착수
  - `[-]` 진행중
  - `[x]` 완료
- 완료 처리 기준:
  - 코드 변경
  - 간단 검증 결과 기록
  - 관련 후속 작업 상태 갱신

## 이번 정리에서 확인된 핵심 문제
- 카페 상세가 카페 문서 1개가 아니라 `casts`, `notices`, `menus`, `goods` 전체를 매번 읽는다.
- 카페 상세와 내 정보 화면에서 캐스트 스케줄과 사용자 정보를 N+1 방식으로 다시 읽는다.
- 내 정보에서 최근 방문/즐겨찾기 카페를 카페 상세 재조회로 해결하고 있다.
- 카페관리에서 전체 카페를 먼저 읽고, 카페별로 캐스트/공지/방문 데이터를 다시 읽어 개수를 계산한다.
- 방문 목록 페이징이 서버 커서가 아니라 전체 조회 후 앱 메모리에서 잘라내는 구조다.
- 카페/캐스트 검색이 Firestore 쿼리에서 충분히 줄어들지 않고, 앱에서 후필터링한다.
- 캐스트 ID만 있을 때 `castId -> cafeId`를 빠르게 찾지 못해 전체 카페 순회 fallback이 있다.
- Cloud Functions 일부가 문서 1건 변경마다 관련 컬렉션 전체를 다시 읽어 재집계한다.

## 목표 지표

### 1차 목표
- 카페 상세 첫 진입 Firestore read 수 체감 감소
- 내 정보 첫 진입 Firestore read 수 체감 감소
- 카페관리 첫 진입 Firestore read 수 체감 감소
- 리뷰/방문/스탬프/방문인증 관련 Functions read 수 감소

### 2차 목표
- 목록 화면은 요약 문서만으로 그리기
- 개수 계산은 전체 문서 조회 대신 집계 필드 또는 aggregation query 사용
- 페이지네이션은 서버 커서 기반으로 일원화
- Functions는 전체 재집계보다 delta update 우선 적용

## 측정 체크리스트

### M-01. Firestore 사용량 기준선 수집
- 우선순위: P0
- 상태: DOING
- 체크:
  - [ ] Firebase Usage 탭에서 `reads`, `writes`, `storage` 최근 7일 스냅샷 기록
  - [ ] Cloud Functions 호출 수와 상위 비용 함수 목록 기록
  - [ ] 카페 상세/내 정보/카페관리 진입 시 네트워크 호출 개수 수동 측정
  - [ ] 개선 전 수치를 이 문서 하단 `측정 로그`에 기록
- 완료 기준:
  - 개선 전 기준선이 문서에 남아 있다.

### M-02. 개선 후 비교 측정 템플릿 준비
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] 화면별 측정 표 템플릿 작성
  - [x] Function별 read/write 감소 확인 표 작성
  - [x] 배포 후 3일, 7일 비교 항목 정의
- 완료 기준:
  - 개선 전후를 같은 형식으로 비교할 수 있다.

## A. 클라이언트 읽기 비용 개선

### A-01. 카페 상세 조회를 요약/상세 2단계로 분리
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] 카페 상세 진입 시 반드시 필요한 데이터와 탭 진입 후 필요한 데이터를 분리
  - [x] 첫 진입에서 `notices`, `menus`, `goods` 전체 조회 제거
  - [x] 탭별 lazy load 구조로 변경
  - [x] 상세 DTO와 탭 DTO를 분리
- 완료 기준:
  - 카페 상세 첫 진입이 전체 서브컬렉션 일괄 조회를 하지 않는다.
- 참고:
  - 현재 `fetchCafeDetailRemoteInternal()`가 `casts`, `notices`, `menus`, `goods`를 한 번에 읽고 있음

### A-02. 카페 상세의 캐스트 오늘 스케줄 N+1 제거
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] 카페 상세에서 캐스트별 `getCastSchedules()` 반복 호출 제거
  - [x] `cafeId + date` 기준 1회 조회 결과를 캐스트별로 매핑
  - [x] 이미 있는 `getWorkingCastIdsByCafeAndDate()`와 중복 조회 정리
- 완료 기준:
  - 캐스트 수에 비례한 스케줄 추가 read가 발생하지 않는다.

### A-03. 카페 상세 리뷰 작성자 닉네임 조회 최적화
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] 리뷰 문서의 `userNickname`을 우선 사용하고 미존재 시에만 fallback
  - [ ] fallback user 조회 결과를 화면 단위 캐시로 재사용
  - [ ] 같은 사용자 중복 조회 제거
- 완료 기준:
  - 리뷰 수에 비례한 사용자 문서 중복 조회가 줄어든다.

### A-04. 내 정보 화면의 카페 상세 재조회 제거
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] 최근 방문 카페 조회를 카페 상세가 아닌 카페 요약 조회로 변경
  - [x] 즐겨찾기 카페 조회를 카페 상세가 아닌 카페 요약 조회로 변경
  - [x] `getCafesByIds()` 내부에서 `fetchCafeDetail()` 의존 제거
  - [x] 카페 요약 조회용 API 또는 역인덱스 경로 설계
- 완료 기준:
  - 내 정보 화면이 카페 ID 목록 때문에 카페 상세를 반복 호출하지 않는다.

### A-05. 방문 목록 서버 커서 기반 페이징으로 전환
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] `fetchVisitsByUserPageRemote()`에서 전체 조회 후 `toPaged()` 하는 구조 제거
  - [x] Firestore 쿼리 `limit/startAfter` 기반으로 변경
  - [x] 첫 페이지와 다음 페이지 모두 실제 서버 페이징 동작 확인
- 완료 기준:
  - 방문 데이터가 많아져도 첫 페이지 비용이 선형 증가하지 않는다.

### A-06. 카페 검색의 후필터링 감소
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] `approved == true`를 쿼리에서 처리할지 구조 확정
  - [ ] 검색어 prefix 또는 검색 전용 필드 도입 여부 결정
  - [ ] 현재 앱단 `approved`, `name contains` 후필터링 루프 축소
- 완료 기준:
  - pageSize를 채우기 위해 과도한 batch 재조회가 줄어든다.

### A-07. 캐스트 검색의 지역 필터 비용 개선
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] `loadCafeIdsByRegionRemote()` 의존 구조 재검토
  - [ ] 캐스트 문서에 지역 검색용 denormalized 필드 도입 여부 결정
  - [ ] collectionGroup 전체 조회 후 앱 필터링 비중 축소
- 완료 기준:
  - 지역 필터 시 카페 목록 전체를 먼저 읽는 간접 비용이 줄어든다.

### A-08. castId -> cafeId 역탐색 제거
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] `castDirectory` 같은 역인덱스 문서 설계
  - [x] 캐스트 생성/수정/삭제 시 역인덱스 동기화 경로 설계
  - [x] `resolveCafeIdByCastId()`의 전체 카페 순회 fallback 제거
- 완료 기준:
  - 캐스트 상세/팔로우/Claim 경로에서 전체 카페 순회가 사라진다.

### A-09. 카페관리 화면의 전체 카페 선조회 제거
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] 운영자 기준 `ownedCafeIds`만으로 대상 카페를 직접 조회
  - [x] 관리자 화면은 별도 경량 리스트 쿼리 사용
  - [x] 카페별 `castCount`, `noticeCount`, `visitCount`를 전체 읽기 대신 집계 필드 또는 count query로 대체
- 완료 기준:
  - 운영 카페 수가 많아져도 진입 비용이 급증하지 않는다.

### A-10. 홈 화면 중복 조회 축소
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] 인기 캐스트의 카페명 해결 시 카페 상세 재조회 제거
  - [ ] nearby local/global fallback 쿼리 횟수 상한 재검토
  - [ ] 홈에 필요한 카페/캐스트 요약 필드만 사용하도록 정리
- 완료 기준:
  - 홈 화면 1회 진입 시 중복 상세 조회가 없다.

## B. 데이터 모델/집계 구조 개선

### B-01. 카페 요약 필드 정리
- 우선순위: P0
- 상태: TODO
- 체크:
  - [ ] 목록/카드/홈/탐색에 필요한 필드 정의
  - [ ] 카페 문서에 요약 필드와 집계 필드 위치 통일
  - [ ] `stats`와 top-level 중복 필드 정리 방향 확정
- 완료 기준:
  - 목록 화면에서 상세 서브컬렉션을 읽지 않아도 된다.

### B-02. 캐스트 요약 필드 정리
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] 지역/카페명/검색용 필드 denormalization 여부 결정
  - [ ] 캐스트 카드에 필요한 최소 필드 집합 정의
  - [ ] 팔로우/랭킹/탐색 공통 요약 구조 정의
- 완료 기준:
  - 캐스트 목록/랭킹/탐색이 상세 조회 없이 그려진다.

### B-03. count aggregation 도입 범위 확정
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] 방문 수
  - [x] 스탬프 수
  - [x] 공지 수
  - [x] 카페별 전체 방문 수
  - [x] 관리자 메트릭
- 완료 기준:
  - 전체 문서 읽기로 개수만 세는 경로가 줄어든다.

### B-04. 집계 필드 소유권 정리
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] 어떤 필드는 클라이언트 계산 금지인지 정리
  - [ ] 어떤 필드는 Function이 authoritative source인지 정리
  - [ ] 문서화 후 다이어그램/백로그에 반영
- 완료 기준:
  - 집계 불일치와 중복 계산 경로가 줄어든다.

## C. Cloud Functions 비용 개선

### C-01. 카페 리뷰 집계를 전체 재집계에서 delta 방식으로 전환
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] `onReviewWrittenSyncCafeAggregate`에서 전체 리뷰 재조회 제거
  - [x] create/update/delete별 delta 계산 설계
  - [x] 평균 평점 갱신 방식 결정
- 완료 기준:
  - 리뷰 1건 변경 시 해당 카페 리뷰 전체를 다시 읽지 않는다.

### C-02. 방문 인증 기반 캐스트 집계 최적화
- 우선순위: P0
- 상태: DONE
- 체크:
  - [x] `syncCastVisitCertificationAggregate()` 전체 재집계 제거
  - [x] 캐스트별 인증 유저 집계 소스 재설계
  - [x] review/visit write 당 필요한 최소 read만 남기기
- 완료 기준:
  - 리뷰 또는 방문 1건 변경 시 관련 컬렉션 전체 스캔이 발생하지 않는다.

### C-03. 유저 방문 수 재집계 최적화
- 우선순위: P1
- 상태: DONE
- 체크:
  - [x] `syncUserVisitCountAggregate()` 전체 verified visit 재조회 제거
  - [x] delta 기반 또는 stamp/visit authoritative source 확정
  - [x] level 계산 경로와 동기화 순서 정리
- 완료 기준:
  - 방문 문서 변경 시 유저별 전체 방문 재조회가 없다.

### C-04. 유저 스탬프 수 재집계 최적화
- 우선순위: P1
- 상태: DONE
- 체크:
  - [x] `syncUserStampCountAggregate()` 전체 stamp 재조회 제거
  - [x] stamp create/delete 기준 delta 반영 구조로 전환
- 완료 기준:
  - stamp write 시 전체 stamp 스캔이 없다.

### C-05. 알림 대상 fan-out 비용 관리
- 우선순위: P1
- 상태: DONE
- 체크:
  - [x] followers/favorites 대상 조회량이 큰 함수 목록 정리
  - [x] notificationSettings 조회 캐시 또는 배치 전략 검토
  - [x] 고비용 fan-out 함수에 상한/큐/배치 적용 여부 검토
- 완료 기준:
  - 인기 캐스트/카페의 fan-out 쓰기 비용이 통제된다.

### C-06. 랭킹 동기화 전체 스캔 최적화
- 우선순위: P2
- 상태: DONE
- 체크:
  - [x] 현재 전체 카페/전체 캐스트 스캔 주기 기록
  - [x] 증분 갱신 가능 범위 검토
  - [x] 랭킹 원천 데이터를 별도 경량 문서로 유지할지 결정
- 완료 기준:
  - 데이터가 증가해도 랭킹 작업 비용이 급증하지 않는다.

## D. 인덱스/쿼리 정비

### D-01. 현재 쿼리와 인덱스 매핑 점검
- 우선순위: P0
- 상태: TODO
- 체크:
  - [ ] 실제 사용 쿼리 목록 정리
  - [ ] 현재 `firestore.indexes.json`과 매핑
  - [ ] 클라이언트 후필터링을 유발하는 미비 인덱스 후보 정리
- 완료 기준:
  - 주요 쿼리의 인덱스 누락 여부를 파악했다.

### D-02. 검색/정렬 쿼리 재설계
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] 카페 정렬별 쿼리
  - [ ] 캐스트 정렬별 쿼리
  - [ ] 리뷰 페이지 쿼리
  - [ ] 방문 페이지 쿼리
- 완료 기준:
  - pageSize를 얻기 위해 과도한 우회 조회를 하지 않는다.

## E. 캐시/동기화 전략 개선

### E-01. 화면 단위 메모리 캐시 기준 수립
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] 카페 요약 캐시
  - [ ] 사용자 닉네임 캐시
  - [ ] 팔로우/즐겨찾기 여부 캐시
  - [ ] 캐스트-카페 매핑 캐시
- 완료 기준:
  - 동일 세션 내 중복 read가 줄어든다.

### E-02. refresh 계열 호출 정리
- 우선순위: P1
- 상태: TODO
- 체크:
  - [ ] 실제로 캐시를 쓰지 않는데 refresh만 호출하는 경로 파악
  - [ ] 삭제/수정 직후 불필요한 재조회 제거
  - [ ] 낙관적 업데이트 가능 경로 분리
- 완료 기준:
  - write 후 즉시 read-back 패턴이 필요한 곳만 남는다.

## F. 실행 순서 제안

### 1차 묶음
- [x] A-01 카페 상세 조회 분리
- [x] A-02 카페 상세 스케줄 N+1 제거
- [x] A-04 내 정보 카페 상세 재조회 제거
- [x] A-05 방문 목록 서버 페이징
- [x] A-09 카페관리 전체 카페 선조회 제거

### 2차 묶음
- [x] A-08 castId 역탐색 제거
- [x] B-03 count aggregation 도입 범위 적용
- [x] C-01 리뷰 집계 delta 전환
- [x] C-02 방문인증 캐스트 집계 최적화

### 3차 묶음
- [ ] A-06 카페 검색 최적화
- [ ] A-07 캐스트 검색 최적화
- [x] C-05 알림 fan-out 비용 관리
- [x] C-06 랭킹 동기화 최적화

## 작업 로그

### 2026-04-10
- [x] Firestore 비용/성능 개선 전용 백로그 문서 생성
- [x] 방문 목록 페이징을 `toPaged()` 기반 메모리 페이징에서 Firestore 서버 커서 기반 페이징으로 전환
- [x] 카페 상세/카페 캐스트 목록의 오늘 스케줄 조회를 `cafeId + date` 단건 쿼리로 통합
- [x] 내 정보의 최근 방문/즐겨찾기 카페 조회에서 카페 상세 재호출 제거
- [x] 카페관리에서 비관리자 대상 카페를 `ownedCafeIds` 직접 조회로 전환하고 `cast/notice/visit` 카운트를 aggregation query로 전환
- [x] 카페 상세 첫 진입에서 `notices/menus/goods` 일괄 조회를 제거하고 메뉴/굿즈 탭 lazy load 도입
- [x] `castDirectory` 역인덱스를 추가하고 `resolveCafeIdByCastId()`의 전체 카페 순회 fallback 제거
- [x] 리뷰 write 트리거를 전체 재집계에서 카페 문서 기반 delta 집계로 전환
- [x] 방문/리뷰 write 트리거의 캐스트 방문인증 집계를 `user-cafe-cast` 단위 delta 집계로 전환
- [x] 방문 수/스탬프 수/공지 수/카페별 전체 방문 수/관리자 메트릭의 count 경로를 aggregation 기준으로 정리
- [x] 방문 write 트리거의 유저 방문수/레벨 집계를 전체 재조회에서 delta 집계로 전환
- [x] stamp write 트리거의 유저 스탬프 수 집계를 전체 재조회에서 delta 집계로 전환
- [x] fan-out 알림 경로에 수신자 상한/배치 처리 적용 및 팬공지 중복 트리거 제거
- [x] 랭킹 동기화 dirty state를 cafe/cast로 분리하고 cafe 랭킹 소스 조회를 scope별 top-N 쿼리로 전환
- [x] 개선 전후 측정 템플릿 문서화
- [-] 기준선 수집 시작

## 측정 가이드

### 수집 원칙
- 기준선 측정은 같은 계정, 같은 기기, 같은 네트워크에서 진행한다.
- 각 화면은 앱 재실행 후 첫 진입 3회 측정하고 중앙값을 기록한다.
- Firebase 콘솔 수치는 스크린샷 기준 시각을 함께 남긴다.
- 배포 후 비교는 `배포 직전 7일 평균` 대비 `배포 후 3일`, `배포 후 7일`로 본다.

### Firebase 콘솔 수집 순서
1. Firebase Console > Firestore Database > Usage에서 최근 7일 `reads`, `writes`, `storage`를 기록한다.
2. Firebase Console > Functions > Usage 또는 GCP Metrics에서 호출 수 상위 함수를 기록한다.
3. 동일 날짜 기준으로 `onReviewWrittenSyncCafeAggregate`, `onReviewWrittenSyncCastVisitCertificationCount`, `onVisitWrittenSyncCastVisitCertificationCount`, `syncUserVisitCountAggregate`, `syncUserStampCountAggregate`를 우선 확인한다.

### 화면 수동 측정 순서
1. 앱 완전 종료 후 실행
2. 대상 화면 1회 진입
3. Firestore REST 호출 수와 문서 read 추정치를 메모
4. 동일 절차 3회 반복 후 중앙값 기록

## 측정 템플릿

### 화면별 측정 표
| 화면 | 시나리오 | 측정 전 호출 수 | 측정 후 호출 수 | 전 read 추정 | 후 read 추정 | 비고 |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| 카페 상세 | 첫 진입 후 정보 탭 유지 |  |  |  |  |  |
| 카페 상세 | 메뉴 탭 최초 진입 |  |  |  |  | lazy load 확인 |
| 카페 상세 | 공지 탭 최초 진입 |  |  |  |  |  |
| 내 정보 | 첫 진입 |  |  |  |  |  |
| 카페관리 | 운영자 첫 진입 |  |  |  |  |  |
| 방문 목록 | 첫 페이지 |  |  |  |  | server paging 확인 |

### Functions 비교 표
| 함수 | 변경 전 read 패턴 | 변경 후 read 패턴 | 배포 후 3일 호출 수 | 배포 후 7일 호출 수 | 비고 |
| --- | --- | --- | ---: | ---: | --- |
| `onReviewWrittenSyncCafeAggregate` | 카페별 전체 리뷰 재조회 | 카페 문서 delta update |  |  |  |
| `onReviewWrittenSyncCastVisitCertificationCount` | 리뷰/방문 연관 전체 재조회 | `user-cafe-cast` 단위 delta |  |  |  |
| `onVisitWrittenSyncCastVisitCertificationCount` | 리뷰/방문 연관 전체 재조회 | `user-cafe-cast` 단위 delta |  |  |  |
| `syncUserVisitCountAggregate` | 유저 verified visit 전체 재조회 | 방문 write delta 반영 |  |  |  |
| `onStampWrittenSyncUserStampStats` | 유저 stamp 전체 재조회 | stamp write delta 반영 |  |  |  |
| `onFanAnnouncementRequestWrittenSendPush` | 팔로워 대상 대량 동시 fan-out | 수신자 상한 + 배치 fan-out |  |  |  |
| `onScheduleSyncRankingSnapshots` | dirty 발생 시 카페/캐스트 전체 스캔 동시 실행 | dirty state별 분리 실행 + 카페 scope별 top-N 쿼리 |  |  |  |

### 배포 후 비교 항목
| 항목 | 기준선 | 배포 후 3일 | 배포 후 7일 | 목표 |
| --- | ---: | ---: | ---: | --- |
| Firestore reads / day |  |  |  | 감소 |
| Firestore writes / day |  |  |  | 유지 또는 소폭 감소 |
| 카페 상세 첫 진입 호출 수 |  |  |  | 감소 |
| 내 정보 첫 진입 호출 수 |  |  |  | 감소 |
| 카페관리 첫 진입 호출 수 |  |  |  | 감소 |
| 리뷰 집계 함수 평균 실행 시간 |  |  |  | 감소 |
| 방문인증 집계 함수 평균 실행 시간 |  |  |  | 감소 |

## 측정 로그

### 개선 전
- 수집 시각:
- Firestore reads:
- Firestore writes:
- Firestore storage:
- Cloud Functions 상위 비용 함수:
- 카페 상세 진입 호출 수:
- 카페 상세 메뉴 탭 최초 진입 호출 수:
- 내 정보 진입 호출 수:
- 카페관리 진입 호출 수:

### 개선 후
- 수집 시각:
- Firestore reads:
- Firestore writes:
- Firestore storage:
- Cloud Functions 상위 비용 함수:
- 카페 상세 진입 호출 수:
- 카페 상세 메뉴 탭 최초 진입 호출 수:
- 내 정보 진입 호출 수:
- 카페관리 진입 호출 수:

## 현재 기준선 수집 블로커
- Firebase Console 실사용 수치는 로컬 코드베이스만으로 확정할 수 없다.
- 다음 입력이 필요하다:
  - Firestore Usage 최근 7일 스크린샷 또는 수치
  - Functions 호출 수 상위 목록
  - 실제 기기/에뮬레이터에서 화면 진입 3회 측정값
