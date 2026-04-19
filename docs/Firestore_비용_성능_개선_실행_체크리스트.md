# Firestore 비용/성능 개선 실행 체크리스트

기준 일자: 2026-04-19
연계 문서: `docs/Firestore_비용_성능_개선_백로그.md`

## 0) 이번 턴 완료 항목
- [x] 전수조사 결과 문서 반영
- [x] 오늘 체크인/리뷰 카운트의 aggregation query 전환
- [x] 리뷰 존재 여부 조회 `limit = 1` 적용

## 1) 즉시 진행(P0/P1)
- [-] 카페 검색 후필터링 축소(A-06)
- [-] 캐스트 검색 후필터링/지역 간접 필터 축소(A-07)
- [-] 알림 fan-out 사용자별 read 증폭 완화(C-07)

## 2) 검색 최적화 구현 체크리스트
- [x] 카페 검색(`LATEST` 제외 정렬) `approved=true` 서버 필터 푸시다운
- [x] 캐스트 검색 일부 케이스(`targetCafeIds <= 10` + `POPULAR/FOLLOWERS`) `cafeId IN` 서버 필터 푸시다운
- [ ] 카페 검색어용 정규화 필드(`searchName` 또는 토큰 필드) 설계
- [ ] 캐스트 검색어/지역 denormalized 필드 설계
- [ ] `firestore.indexes.json`에 필요한 인덱스 반영
- [ ] `searchCafesRemote()` 후필터링 루프 축소
- [ ] `searchCastsRemote()` collectionGroup 과조회 축소
- [ ] 회귀 점검: 검색 결과 품질/정렬/페이지네이션

## 3) 알림 fan-out 최적화 체크리스트
- [ ] fan-out 함수별 read/write 산식 문서화
- [-] 사용자 설정/토큰 조회 캐시 전략 확정
- [x] `syncBirthdayNotifications()` 동일 invocation 내 `notificationSettings` 캐시 적용
- [ ] 고트래픽 이벤트 임계치에서 큐 기반 분산 처리 검토
- [ ] 실패/재시도 정책 정리
- [ ] 운영 모니터링 지표(대상 수, sent/skipped/failed, droppedByCap) 대시보드화

## 4) 측정 및 검증
- [ ] Firestore Usage 최근 7일 기준선 기록
- [ ] 화면별 호출 수(카페 상세/내 정보/카페관리) 3회 측정
- [ ] 개선 후 3일/7일 비교 수치 업데이트
- [ ] Functions 상위 비용 함수 변동 기록

## 5) 완료 기준
- [ ] 검색 화면에서 pageSize 확보를 위한 과도한 재조회가 재현되지 않음
- [ ] 알림 fan-out 이벤트 1건당 read/write 비용 상한이 운영 기준 내로 확인됨
- [ ] 백로그/체크리스트/측정 로그가 최신 상태로 일치함
