# JP 데이터 적재 실행 백로그

기준 문서:
- `docs/JP_데이터_수집_및_적재_계획.md`

## A. 수집/매핑 기반 작업
- [x] A1. API 클라이언트 작성: 도쿄/오사카 목록 + 카페 상세 + 캐스트 상세
- [x] A2. 페이지네이션 구현: `page` 증가 기반 전체 수집
- [x] A3. 이미지 URL 정규화 함수 구현
- [x] A4. socialMedia 계정 아이디 추출 함수 구현
- [x] A5. 카페 매퍼 구현(`shop detail` -> `cafes/{cafeId}`)
- [x] A6. 캐스트 매퍼 구현(`cast detail` -> `cafes/{cafeId}/casts/{castId}`)
- [x] A7. birthday/birthdayKey 변환 유틸 구현
- [x] A8. region.city 규칙 구현(`pre08=Tokyo`, `pre25=Osaka`)

## B. 번역/원문 보존
- [x] B1. 일본어 -> 한국어 번역 모듈 연동
- [x] B2. 번역 실패 fallback(원문 사용) 구현
- [x] B3. 원문 저장 필드(`source.*Ja`) 매핑 적용
- [x] B4. 번역 상태 필드(예: `translationStatus`) 반영

## C. Firestore 적재
- [x] C1. 문서 ID 규칙 구현(`jp_shop_`, `jp_cast_`)
- [x] C2. upsert(batch) 구현
- [x] C3. 변경 감지(`source.updatedAt`, `sourceHash`) 구현
- [ ] C4. 삭제/비활성 정책 정의 및 반영
- [ ] C5. 트랜잭션/배치 실패 재시도(backoff) 처리

## D. 스케줄러/운영
- [x] D1. Cloud Function `onSchedule` 엔트리 추가
- [x] D2. 30일 주기 스케줄 설정
- [x] D3. 실행 로그 구조화(수집/번역/업데이트/실패 건수)
- [ ] D4. 장애 알림 채널 연동(로그 기반)
- [x] D5. 수동 실행(onRequest) 엔드포인트 추가(운영 점검용)

## E. 검증/QA
- [ ] E1. Emulator 소량 데이터 적재 검증
- [ ] E2. 필드 검증: `Cafe`, `Cast` 도메인 모델 역직렬화 확인
- [ ] E3. 탐색 필터 검증: `JP/Tokyo`, `JP/Osaka` 노출 확인
- [ ] E4. 앱 홈/탐색/상세에서 렌더링 점검
- [ ] E5. socialMedia 아이디 저장 결과 샘플 검증
- [ ] E6. 재실행 시 중복 미발생 검증

## F. 문서/운영 인수인계
- [ ] F1. 실행/롤백 가이드 문서화
- [ ] F2. `docs/Firestore Mock데이터 관련.md` 업데이트(구현 완료 후)
- [ ] F3. 운영 체크리스트 최신화

## 우선순위(권장)
1. A1~A8
2. B1~B4
3. C1~C5
4. D1~D5
5. E1~E6
6. F1~F3
