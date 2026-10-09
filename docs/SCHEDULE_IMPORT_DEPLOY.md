# Schedule Image Import Functions Deploy

관리자 웹 페이지(`/admin` > 출근표 이미지로 스케줄 등록)에서 사용하는 Cloud Functions 배포 방법입니다.

## 흐름

1. 관리자 페이지가 출근표 이미지를 Storage `scheduleImports/{cafeId}/`에 업로드 (5MB 초과·미지원 형식은 브라우저에서 JPEG로 다시 인코딩)
2. `extractScheduleImage`: OpenAI `gpt-5.5`(Responses API, JSON 스키마 strict, reasoning effort `high`)로 날짜·시간·캐스트 이름을 추출하고, 소속 캐스트와 이름을 매칭해 초안을 반환. 초안은 `scheduleImports/{importId}`에 `DRAFT`로 저장
3. 관리자가 웹에서 확인·수정 (캐스트 연결/새 캐스트 등록/건너뛰기, 시간, 기존 일정 덮어쓰기, 출근표에 없는 캐스트 분리)
4. `applyScheduleImport`: 새 캐스트 생성 + `castSchedules` 일괄 반영 + 선택한 캐스트를 현재 없는 캐스트(`rosterStatus: "INACTIVE"`)로 분리, 다시 출근표에 나온 분리 캐스트는 `ACTIVE`로 복원. 초안은 `APPLIED`로 변경
5. `onCastScheduleWrittenCreateShiftNotifications`: 같은 `requestBatchId`의 스케줄 알림을 캐스트당 한 건으로 묶음

## 대상 함수

- `extractScheduleImage`
- `applyScheduleImport`
- `onCastScheduleWrittenCreateShiftNotifications`

두 HTTP 함수 모두 `Authorization: Bearer <Firebase ID 토큰>`을 요구하며, `ADMIN` 또는 해당 카페 소유자(`cafes.ownerIds` / `users.ownedCafeIds`)만 호출할 수 있습니다. CORS는 `concafe-5f7fd.web.app`, `concafe-5f7fd.firebaseapp.com`, `localhost`만 허용합니다.

## 최초 설정

```bash
npm --prefix functions install
firebase functions:secrets:set OPENAI_API_KEY
```

## 배포

프로젝트 루트(`firebase/`)에서 실행합니다. WSL에서 `/mnt/c` 경로로 배포하면 코드 분석이 10초 제한을 넘기 쉬우므로 `FUNCTIONS_DISCOVERY_TIMEOUT`을 늘립니다. 다른 함수의 미배포 변경이 섞이지 않도록 함수 이름을 지정합니다.

```bash
FUNCTIONS_DISCOVERY_TIMEOUT=120 firebase deploy --only "functions:extractScheduleImage,functions:applyScheduleImport,functions:onCastScheduleWrittenCreateShiftNotifications"
firebase deploy --only hosting
```
