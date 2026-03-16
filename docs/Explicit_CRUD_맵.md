# Explicit CRUD Map

## 목적

CRUD 성공 후 UI 갱신을 `observeCafeDetail` / `observeCastVersion` 같은 간접 신호에 기대지 않고,
repository explicit event를 기준으로 맞춘다.

기준 패턴은 다음과 같다.

1. Repository CRUD 성공
2. Repository가 domain event emit
3. 필요한 ViewModel이 event를 observe
4. ViewModel이 reload 또는 1회성 UI event 처리

## Domain Event

### Review

- `ReviewEvent.Created(cafeId)`
- `ReviewEvent.Deleted(cafeId, reviewId)`

### CafeDetail

- `CafeDetailEvent.CafeInfoUpdated(cafeId)`
- `CafeDetailEvent.MenuGoodsCreated(cafeId, itemId)`
- `CafeDetailEvent.MenuGoodsUpdated(cafeId, itemId)`
- `CafeDetailEvent.MenuGoodsDeleted(cafeId, itemId)`

### Cast

- `CastEvent.Created(cafeId, castId)`
- `CastEvent.Updated(cafeId, castId)`
- `CastEvent.Deleted(cafeId, castId)`

## Producer Map

| Entity | CRUD 진입점 | Repository Event |
| --- | --- | --- |
| Review | `FakeReviewRepository.createReview/deleteReview` | `ReviewEvent.Created/Deleted` |
| CafeInfo | `FakeCafeRepository.updateCafeInfo` | `CafeDetailEvent.CafeInfoUpdated` |
| Menu/Goods | `FakeCafeRepository.upsertCafeMenuGoods/deleteCafeMenuGoods` | `CafeDetailEvent.MenuGoodsCreated/Updated/Deleted` |
| Cast | `FakeCastRepository.upsertCast` | `CastEvent.Created/Updated` |

## Consumer Map

| 화면 | 이전 갱신 방식 | 현재 explicit event |
| --- | --- | --- |
| Cafe Review | 카페 상세 observe + 리뷰 refresh | `ReviewEvent` |
| MenuGoods 관리 | `ObserveCafeDetailUseCase` | `CafeDetailEvent` |
| CafeDashboard | `ObserveCafeCastVersionUseCase` | `CafeDetailEvent`, `CastEvent` |
| Cast 상세 | `ObserveCastVersionUseCase` | `CastEvent` |
| FanManagement | `ObserveCastVersionUseCase` | `CastEvent` |
| Schedule 관리 | `ObserveCastVersionUseCase` | `CastEvent` |

## 유지하는 observe 의존

### CafeViewModel

- `ObserveCafeDetailUseCase`는 유지한다.
- 이유:
  카페 상세 화면은 CRUD invalidation 전용 화면이 아니라, 화면 전체를 구성하는 canonical detail stream 역할이 더 크다.
- 적용 방식:
  리뷰는 explicit `ReviewEvent`로 스크롤/리뷰 페이지 갱신을 처리하고,
  카페 상세 본문은 기존 detail observe를 그대로 사용한다.

## 정리 대상

### 제거 후보

- `ObserveCastVersionUseCase`
- `ObserveCafeCastVersionUseCase`
- `CastRepository.observeCastVersion`
- `CastRepository.observeCafeCastVersion`

현재 app screen 소비자는 explicit `CastEvent`로 치환됐다.
shared layer의 version observe API는 후속 정리에서 제거 가능하다.

### 향후 확장 규칙

- 새 CRUD 추가 시 먼저 domain event를 정의한다.
- ViewModel은 성공 callback이나 간접 observe 대신 explicit event를 구독한다.
- 스크롤, toast, navigate는 UiState가 아니라 1회성 event로 처리한다.
