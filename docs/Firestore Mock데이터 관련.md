Mock 데이터 추가하는 방법:

아래 명령어로 firebase emulator 실행후

FIREBASE_SKIP_UPDATE_CHECK=1 FUNCTIONS_DISCOVERY_TIMEOUT=60 firebase emulators:start --only functions --project concafe-5f7fd

다른 터미널에서 아래 명령어 실행

Mock 데이터 추가: curl -v -m 600 -X POST "http://127.0.0.1:5001/concafe-5f7fd/us-central1/seedMockConCafeData"

Mock 데이터 삭제: curl -v -m 600 -X POST "https://us-central1-concafe-5f7fd.cloudfunctions.net/clearMockConCafeData"


Mock 데이터 함수 배포 명령어:
firebase deploy --only functions:seedMockConCafeData,functions:clearMockConCafeData

삭제 함수 실행 기준:
• 지금 삭제 기준은 문서 내용이 아니라 문서 ID(prefix) 입니다.

기준 상세:

- cafes: 문서 ID가 mock_cafe_로 시작하면 삭제 + 하위 전부 재귀 삭제
    - firebase/functions/src/index.ts:3443
- users: 문서 ID가 mock_user_로 시작하면 삭제 + 하위 전부 재귀 삭제
    - firebase/functions/src/index.ts:3454
- reviews: mock_review_ prefix
- visits: mock_visit_ prefix
- homeBanners: mock_home_banner_ prefix
    - firebase/functions/src/index.ts:3465
- prefix 삭제 로직 자체: documentId 범위쿼리(>= prefix, <= prefix\uf8ff)
    - firebase/functions/src/index.ts:3080

질문하신 경우:

- Mock 생성 후 이미지 URL 교체, 내용 수정, 필드 업데이트를 해도
  문서 ID가 그대로 mock_*면 삭제 함수 호출 시 그대로 삭제됩니다.
- 반대로 문서 ID를 mock_*가 아닌 값으로 바꾸거나(복사/이동) 새로 만들면, 그 문서는 현재 삭제 대상이 아닙니다.