# KR Crawling Functions Deploy

KR 크롤링 관련 Cloud Functions만 배포할 때 사용하는 명령어입니다.

## 배포 대상

- `syncKrCrawledConCafeData`: KR Supabase `cafe_maids` 데이터에서 캐스트와 스케줄만 동기화
- `clearKrCrawledConCafeData`: KR 크롤링으로 추가된 `kr_cast_00000` 형식 캐스트, 스케줄, `castDirectory` 데이터만 삭제

## 최초 설정

Supabase REST API 키를 Secret으로 등록합니다.

```bash
firebase functions:secrets:set KR_CRAWL_SUPABASE_API_KEY
```

호출 토큰은 기존 시드 토큰을 재사용하거나, KR 전용 토큰을 Functions 런타임 환경변수로 설정합니다.

```dotenv
KR_CRAWL_TOKEN=YOUR_TOKEN
CRAWLED_DATA_CLEAR_TOKEN=YOUR_CLEAR_TOKEN
```

동기화 함수는 `KR_CRAWL_TOKEN` 또는 `MOCK_SEED_TOKEN`을 확인합니다. 삭제 함수는 `CRAWLED_DATA_CLEAR_TOKEN` 또는 `MOCK_SEED_TOKEN`을 확인합니다.

## 빌드 확인

```bash
cd functions
npm run build
```

## 함수만 배포

```bash
firebase deploy --only functions:syncKrCrawledConCafeData,functions:clearKrCrawledConCafeData
```

## 수동 실행

동기화:

```bash
curl -X POST "https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/syncKrCrawledConCafeData" \
  -H "X-Seed-Token: YOUR_TOKEN"
```

드라이런:

```bash
curl -X POST "https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/syncKrCrawledConCafeData?dryRun=1" \
  -H "X-Seed-Token: YOUR_TOKEN"
```

KR 크롤링 데이터 삭제:

```bash
curl -X POST "https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/clearKrCrawledConCafeData" \
  -H "X-Seed-Token: YOUR_CLEAR_TOKEN"
```

삭제 함수는 기존 카페 문서는 삭제하지 않고, 지정된 KR 카페 하위의 `kr_cast_` 캐스트와 `kr_cast_` 문서 ID를 가진 `castSchedules`, `castDirectory`만 삭제합니다.
