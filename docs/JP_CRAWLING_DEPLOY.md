# JP Crawling Functions Deploy

JP 크롤링 관련 Cloud Functions만 배포할 때 사용하는 명령어입니다.

## 대상 함수

- `syncJpCrawledConCafeData`
- `onScheduleSyncJpCrawledConCafeData`
- `onScheduleSyncJpCrawledConCafeDataOsaka`
- `onScheduleSyncJpCrawledConCafeDataYokohama`
- `clearJpCrawledConCafeData`

## 배포

프로젝트 루트(`firebase/`)에서 실행합니다.

```bash
npm --prefix functions run build
firebase deploy --only "functions:syncJpCrawledConCafeData,functions:onScheduleSyncJpCrawledConCafeData,functions:onScheduleSyncJpCrawledConCafeDataOsaka,functions:onScheduleSyncJpCrawledConCafeDataYokohama,functions:clearJpCrawledConCafeData"
```

## 참고

크롤링 동기화 함수는 `GOOGLE_TRANSLATE_API_KEY` Secret을 사용합니다. 최초 배포 전 Secret이 없다면 Firebase CLI에서 먼저 등록해야 합니다.

```bash
firebase functions:secrets:set GOOGLE_TRANSLATE_API_KEY
```
