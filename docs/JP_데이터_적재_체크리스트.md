# JP 데이터 적재 체크리스트

기준 문서:
- `docs/JP_데이터_수집_및_적재_계획.md`
- `docs/JP_데이터_적재_실행_백로그.md`

## 0. 구현 완료(코드 기준)
- [x] `syncJpCrawledConCafeData` 수동 실행 Function 추가
- [x] `onScheduleSyncJpCrawledConCafeData` 2개월 간격 Scheduler 추가(Tokyo: 1일)
- [x] `onScheduleSyncJpCrawledConCafeDataOsaka` 2개월 간격 Scheduler 추가(Osaka: 15일)
- [x] 일본 API 수집 + 상세 수집 + 매핑 + Firestore upsert 구현
- [x] 페이지/카페 단위 즉시 저장 구조로 타임아웃 리스크 완화
- [x] socialMedia 계정 아이디 추출 로직 구현
- [x] 번역 모듈(키 기반) + fallback + 원문 보존 필드 구현

## 1. 사전 준비
- [ ] 외부 API 이용약관/호출 제한 확인
- [ ] 운영 프로젝트/에뮬레이터 환경 변수 준비
- [ ] 번역 API 키 및 사용량 한도 확인
- [ ] Firestore 권한 경로(Functions Admin SDK) 사용 경로 확인

## 2. 수집 정확성
- [ ] 도쿄 API 전체 페이지 수집 확인
- [ ] 오사카 API 전체 페이지 수집 확인
- [ ] 카페 상세 API 누락 없이 호출되는지 확인
- [ ] 캐스트 상세 API 누락 없이 호출되는지 확인
- [ ] API 응답 오류(4xx/5xx) 재시도 동작 확인

## 3. 매핑 정확성
- [ ] `cafes/{cafeId}` 필수 필드 저장 확인
- [ ] `cafes/{cafeId}/casts/{castId}` 필수 필드 저장 확인
- [ ] `region.country == JP` 확인
- [ ] `region.city`가 `Tokyo/Osaka`로 정규화되는지 확인
- [ ] `birthday`와 `birthdayKey` 생성 규칙 확인
- [ ] `approved=true`, `ratingAvg=0`, `reviewCount=0` 기본값 확인

## 4. 이미지/소셜 정규화
- [ ] 이미지 경로가 `https://img.con-cafe.jp/upload/...`로 저장되는지 확인
- [ ] `socialMedia.twitter`가 계정 ID만 저장되는지 확인
- [ ] `socialMedia.instagram`가 계정 ID만 저장되는지 확인
- [ ] `socialMedia.tiktok`이 `@` 없는 ID로 저장되는지 확인
- [ ] `socialMedia.youtube`가 handle/channel ID로 저장되는지 확인
- [ ] 추출 실패 시 빈 문자열이 저장되지 않는지 확인

## 5. 번역 품질
- [ ] `name/desc/region.address` 한국어 저장 확인
- [ ] `source.*Ja` 원문 보존 확인
- [ ] 번역 실패 시 fallback 동작 확인
- [ ] 고유명사(상호/인명) 변환 품질 샘플 점검

## 6. 중복/업데이트 안정성
- [ ] 동일 데이터 재실행 시 중복 문서 미생성 확인
- [ ] `source.updatedAt/sourceHash` 변경분만 업데이트 확인
- [ ] 배치 실패 재시도 및 부분 실패 로그 확인
- [ ] 스케줄 동시 실행 방지(락/중복 실행 방지) 확인

## 7. 앱 동작 검증
- [ ] 홈 피드 일본 데이터 노출 확인
- [ ] 탐색(도쿄/오사카 필터) 결과 확인
- [ ] 카페 상세/캐스트 상세 렌더링 확인
- [ ] 앱 크래시/파싱 오류 없음 확인

## 8. 운영 전환
- [ ] Scheduler(30일 주기) 배포 확인
- [ ] 첫 운영 실행 결과(수집/업데이트 건수) 확인
- [ ] 실패 알림/로그 조회 경로 확인
- [ ] 롤백 절차(스냅샷/삭제 기준) 준비

## 9. 문서 반영
- [ ] 구현 결과를 계획서에 반영
- [ ] `docs/Firestore Mock데이터 관련.md` 업데이트
- [ ] 백로그 완료 항목 체크 및 이력 기록
