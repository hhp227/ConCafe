# Remote Data Memory Cache Plan

## Current policy

- Cafe, cast, and CafeEvent remote data sources can be wrapped with a single `.withLocalCache(get())` line in DI.
- The current cache is process-local memory only. App restart, process death, or module recreation clears it.
- Reads use process-local cache-first behavior:
  1. return a memory cache hit inside the current app process,
  2. call remote only on cache miss,
  3. store the successful remote result in memory.
- Remote failures are not recovered with older cached data after a miss. This avoids explicit failure fallback hiding Firebase/server problems.
- Mutations invalidate related cache keys after the remote mutation succeeds. For CafeEvent, this includes cafe event pages, home event pages, and per-user like state touched by the mutation.

## If persistent cache is needed later

Use this prompt for the follow-up task:

```text
현재 카페/캐스트/CafeEvent 원격 데이터소스는 RemoteMemoryCache 기반의 프로세스 내 메모리 캐시를 사용한다.
이 구조를 유지하면서 영속 캐시를 추가해라.

요구사항:
- `.withLocalCache(get())` 적용/제거 방식은 유지한다.
- RemoteMemoryCache 인터페이스 또는 그 후속 인터페이스를 확장해서 persistent 구현만 교체 가능해야 한다.
- Android/iOS/JVM KMP 환경에서 사용할 저장소를 expect/actual 또는 플랫폼별 store로 분리한다.
- 기본 정책은 프로세스 내 cache-first다.
- 원격 실패 시 persistent cache fallback을 기본으로 하지 않는다.
- fallback이 필요한 화면은 명시적 정책을 별도 파라미터나 별도 wrapper로 opt-in하게 한다.
- 캐시 엔트리에는 저장 시각, schemaVersion, domain, operation, key를 포함한다.
- 모델 구조 변경 시 schemaVersion 불일치 엔트리는 폐기한다.
- 쓰기 성공 후 관련 카페/캐스트 키를 무효화한다.
- 앱 재실행 후에도 저장된 캐시를 사용할 수 있지만, 원격 불일치가 장애를 가리지 않도록 remote-first 원칙을 지킨다.
```

## Consistency rule

Remote data is the source of truth. Cache entries are snapshots of the last successful remote response in the current process. If local app mutations succeed, related cache keys are invalidated immediately so later reads cannot reuse known-stale data. If remote data changes outside this app process, the memory cache can stay stale until an explicit refresh/invalidation, process restart, or cache removal by commenting out `.withLocalCache(get())`.
