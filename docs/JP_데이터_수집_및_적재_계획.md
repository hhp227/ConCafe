# ConCafe 일본 데이터 수집 및 적재 계획

## 1. 목적
- ConCafe 초기 운영에서 데이터 공백 이탈을 방지하기 위해 일본(도쿄/오사카) 데이터를 먼저 제공한다.
- 외부 API 데이터를 ConCafe Firestore 스키마(`cafes`, `cafes/{cafeId}/casts`)에 맞게 가공 저장한다.
- 일본어 원문을 보존하면서 한국어 표시값을 함께 저장한다.

## 2. 수집 대상 API
- 도쿄 목록:
  - `https://con-cafe.jp/api/shop?displayed=1&request=1&front_displayed=1&region=area02&sort=priority&order=desc&page={n}`
- 오사카 목록:
  - `https://con-cafe.jp/api/shop?displayed=1&request=1&front_displayed=1&region=area06&prefecture=pre25&sort=priority&order=desc&page={n}`
- 카페 상세:
  - `https://con-cafe.jp/api/shop/{shopId}`
- 캐스트 상세:
  - `https://con-cafe.jp/api/cast/{castId}`

메모:
- 목록 API는 페이지당 20개이며 `page` 파라미터로 페이징한다.

## 3. 실행 아키텍처
- 트리거: Firebase `onSchedule` (Cloud Scheduler)
- 주기: 30일(월 1회)
- 파이프라인:
  1. 목록 수집(도쿄, 오사카)
  2. 카페 상세 병렬 수집
  3. 캐스트 상세 병렬 수집
  4. 일본어 -> 한국어 번역
  5. 정규화/검증
  6. Firestore upsert(batch)
- 원칙:
  - idempotent(동일 입력 재실행 안전)
  - 변경 감지(`source.updatedAt` + `sourceHash`) 기반 최소 쓰기

## 4. Firestore 저장 매핑

### 4.1 카페 문서
- 문서 경로: `cafes/{cafeId}`
- 문서 ID: `jp_shop_{shop.id}`

필드 매핑:
- `name`: 한국어 번역 이름
- `desc`: 한국어 번역 소개 (`description` 우선, 없으면 `subtitle`)
- `region.country`: `"JP"`
- `region.city`:
  - `pre08` -> `Tokyo`
  - `pre25` -> `Osaka`
- `region.address`: 한국어 번역 주소
- `region.location`: `GeoPoint(lat, lng)` (누락 시 `0.0, 0.0`)
- `thumbnailImage`: `https://img.con-cafe.jp/upload/{logo_filename}`
- `galleryImages`: 배경/커버/갤러리 이미지 URL 정규화 후 병합, 중복 제거
- `approved`: `true`
- `ratingAvg`: `0.0`
- `reviewCount`: `0`
- `conceptType`: 기본 `MAID` (룰 기반 확장 가능)
- `ownerIds`: `[]`
- `socialMedia`: 플랫폼별 계정 ID만 저장
- `reservationUrl`: 예약 URL이 유효한 경우 저장

원문/출처 보존(권장):
- `source.provider`: `con-cafe.jp`
- `source.shopId`: number
- `source.nameJa`, `source.descJa`, `source.addressJa`
- `source.updatedAt`
- `sourceHash`

### 4.2 캐스트 문서
- 문서 경로: `cafes/{cafeId}/casts/{castId}`
- 문서 ID: `jp_cast_{cast.id}`

필드 매핑:
- `name`: 캐스트명(원문 유지 또는 정책에 따라 번역명)
- `desc`: 한국어 번역 소개(`comment`)
- `profileImage`: `https://img.con-cafe.jp/upload/{profile_filename}`
- `galleryImages`: `cast_gallaries` URL 정규화 후 중복 제거
- `birthday`:
  - `birth_month`, `birth_day` 있으면 `2000-MM-DD`
  - 없으면 `null`
- `birthdayKey`: 생일 존재 시 `MM-DD`
- `conceptRole`: 기본 `maid`
- `linkedUserId`: `null`
- `followerCount`: `0`
- `rating`: `0.0`
- `visitCertificationCount`: `0`

원문/출처 보존(권장):
- `source.castId`
- `source.nameJa`, `source.descJa`
- `source.updatedAt`
- `sourceHash`

## 5. socialMedia 저장 규칙(중요)
- 저장 목표: URL 전체가 아니라 계정 아이디만 저장
- 대상 필드: `socialMedia.twitter`, `socialMedia.instagram`, `socialMedia.tiktok`, `socialMedia.youtube`

정규화 공통:
1. trim
2. query/hash 제거
3. trailing slash 제거
4. `@` 제거
5. 소문자화
6. 추출 실패 시 미저장(빈 문자열 저장 금지)

플랫폼별:
- Twitter/X:
  - 입력 예: `https://x.com/nijigenkanojo`
  - 저장: `nijigenkanojo`
- Instagram:
  - 입력 예: `https://instagram.com/maid_cafe_tokyo/`
  - 저장: `maid_cafe_tokyo`
- TikTok:
  - 입력 예: `https://www.tiktok.com/@maidtokyo?lang=ja`
  - 저장: `maidtokyo`
- YouTube:
  - `youtube.com/@handle` -> `handle`
  - `youtube.com/channel/UCxxx` -> `UCxxx`

## 6. 번역 정책
- 앱 표시 필드: 한국어 저장
- 원문 필드: `source.*Ja`에 보존
- 번역 실패 시:
  - 원문 fallback 저장
  - 상태 필드(`translationStatus`)로 추적 가능하도록 설계

## 7. 품질/안정성 기준
- 필수값 검증: `id`, `name`, `region.country`, `region.city`
- 좌표 검증: 위도/경도 범위 체크
- URL 검증: http/https만 허용
- 중복 방지: 문서 ID 고정 + 해시 비교
- 장애 대응: 배치 단위 커밋, 실패 시 재시도(backoff), 요약 로그 남김

## 8. 배포 순서
1. 도쿄/오사카 수집 함수 개발
2. Emulator 검증(소량)
3. 운영 프로젝트 dry-run
4. Scheduler 연결
5. 1차 운영 반영
6. 모니터링/보정

## 9. 연계 문서
- 실행 백로그: `docs/JP_데이터_적재_실행_백로그.md`
- 체크리스트: `docs/JP_데이터_적재_체크리스트.md`
- 구현 후 업데이트 대상:
  - `docs/Firestore Mock데이터 관련.md`

## 10. 구현 상태 (2026-04-13)
- 완료:
  - `syncJpCrawledConCafeData` (수동 실행 HTTP Function)
  - `onScheduleSyncJpCrawledConCafeData` (2개월 간격, 1일: Tokyo)
  - `onScheduleSyncJpCrawledConCafeDataOsaka` (2개월 간격, 15일: Osaka)
  - 일본 API 목록/상세 수집, Firestore upsert, sourceHash 기반 변경 감지
  - 타임아웃 완화를 위해 페이지/카페 단위 즉시 저장 방식으로 처리 구조 변경
  - 수동 함수 파라미터 확장: `source(tokyo|osaka|all)`, `maxPages`, `maxShops`, `dryRun`
  - socialMedia 계정 ID 추출 저장
  - 일본어->한국어 번역(키 기반) + 실패 fallback + 원문 보존
- 미완료:
  - 운영 배포/실행 검증
  - 재시도 고도화(backoff 정책 확장)
  - Mock 데이터 문서 업데이트
