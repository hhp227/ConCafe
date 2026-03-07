# ConCafe 코드 패턴 규칙

이 문서는 ConCafe 구현 시 내가 작성하는 코드의 고정 규칙이다.  
범위: `shared`, `composeApp`, (필요 시) `iosApp` 연동부

## 1. 공통 원칙
- 기능보다 일관성을 우선한다.
- 한 파일/클래스는 하나의 책임만 가진다.
- 예외(`throw`)를 UI까지 전파하지 않고 `AppResult`/`AppError`로 변환한다.
- 로그인/권한/검증 로직은 화면이 아닌 도메인 정책 또는 라우트 가드에 둔다.
- 하드코딩 문자열/매직넘버를 최소화하고 상수로 분리한다.

## 2. 모듈 경계 규칙
- `composeApp`은 `shared`의 도메인 타입과 유스케이스만 참조한다.
- `shared`는 UI 프레임워크 의존성을 갖지 않는다.
- 플랫폼별 구현(안드로이드/iOS)은 인터페이스 뒤로 숨기고 도메인 계약을 유지한다.

## 2-1. 플랫폼 네비게이션 규칙
- Android는 `Jetpack Navigation`(`NavHost`, `NavController`)을 사용한다.
- iOS는 `NavigationStack`(`NavigationPath`)을 사용한다.
- Desktop은 상태 기반 라우트 상태머신(`currentRoute`, `routeStack`)을 사용한다.
- 라우트 이름과 로그인 가드 규칙은 3플랫폼에서 동일하게 유지한다.
- 상세 화면은 모달이 아닌 route push 방식으로 이동한다.
- 인증이 필요한 라우트 진입 실패 시 `SignIn` 라우트로 이동 후 성공 시 `pendingRoute/pendingAction`을 재실행한다.
- 메인 네비게이션 3번째 탭은 역할별로 교체한다.

### 2-1-1. 공통 메인 라우트 규격
- 루트 라우트: `Entry`, `Main`, `Cast`, `Cafe`, `SignIn`, `Notification`, `Settings`
- 메인 탭 라우트: `home`, `explore`, `ranking`, `myinfo`
- 마이 하위 라우트: `settings`
- 역할별 3번째 탭 라우트:
  - 게스트/`VISITOR`: `checkin`
  - `CAST`: `fanManagement`
  - `CAFE_OWNER`: `cafeManagement`
  - `ADMIN`: `adminOperations`
- 메인 탭 순서는 항상 `홈 -> 탐색 -> 역할별 3번째 탭 -> 랭킹 -> 내 정보` 순서를 유지한다.
- 다중 역할 계정 우선순위는 `ADMIN > CAFE_OWNER > CAST > VISITOR`를 사용한다.
- `Settings`는 `MyInfo`에서 진입하는 상세 push 라우트로 취급한다.
- 현재 범위에서 `Settings`는 별도 도메인/데이터 연동 없이 `SettingsScreen`/`SettingsView` UI 추가만 우선 구현한다.

## 3. 패키지/파일 규칙
- 패키지 구조는 기능+레이어 기준으로 유지한다.
- 파일명은 타입명과 1:1 매칭한다.
- 하나의 파일에 public 타입은 1개를 기본으로 한다.
- 확장 함수/매퍼는 `*Mapper.kt`, `*Extensions.kt`로 분리한다.

## 4. 네이밍 규칙
- `UseCase`: 동사+대상 (`CreateReviewUseCase`, `GetCafeUseCase`)
- `Repository`: 도메인명+Repository (`CafeRepository`)
- `UiState`: 화면명+UiState (`HomeUiState`)
- `ViewModel`: 화면명+ViewModel (`ExploreViewModel`)
- `Action`: 화면명+Action (`HomeAction`)
- `Event`: 화면명+Event (`HomeEvent`)
- Boolean은 `is/has/can` 접두어를 사용한다.

## 5. 도메인 계층 규칙 (`shared/domain`)
- UseCase는 `operator fun invoke(...)`를 기본으로 한다.
- UseCase는 다른 UseCase를 직접 호출하지 않고 Repository/Policy 중심으로 구성한다.
- 도메인 모델은 불변(`data class` + `val`)을 기본으로 한다.
- 날짜/시간/좌표 등 검증이 필요한 값은 Policy 클래스에서 검증한다.

## 6. 데이터 계층 규칙 (`shared/data`)
- 외부 응답 DTO와 도메인 모델을 분리한다.
- DTO -> Domain 변환은 Mapper에서만 수행한다.
- Repository 구현은 I/O 담당, 비즈니스 규칙 판단은 최소화한다.
- 실패는 항상 `AppError`로 매핑해 반환한다.

## 7. 프레젠테이션 규칙 (`composeApp`)
- 화면은 상태 렌더링만 담당하고 비즈니스 판단을 하지 않는다.
- MVI 패턴을 사용한다.
- 화면 상태 관리는 아래 2개 프로퍼티와 1개 메소드를 고정으로 사용한다.
  - `uiState`: `StateFlow<ScreenUiState>`
  - `event`: `SharedFlow<ScreenEvent>`
  - `action`: `onAction(ScreenAction)`
- UI는 `action(...)`만 호출하고 상태 변화는 `uiState` 구독으로만 반영한다.
- 단발성 효과(토스트/네비게이션/로그인유도)는 `event`로만 전달한다.
- `event`는 `MutableSharedFlow(replay = 0)`를 기본값으로 사용한다.
- `onAction`(또는 `action`) 내부에서만 호출되는 처리 메서드는 반드시 `private`로 캡슐화한다.
- 외부(UI/다른 클래스)에서 직접 호출하면 안 되는 액션 핸들러는 public으로 노출하지 않는다.
- 로그인 필요 액션은 `pendingRoute/pendingAction` 규칙으로 처리한다.
- 게스트 차단 시 문구는 문서의 로그인 가이드 문구를 사용한다.

## 8. 인증/권한 패턴 규칙
- 앱 시작 기본 진입은 홈, 마이페이지는 라우트 가드 적용.
- 게스트 허용: 조회성 화면(홈/탐색/상세/랭킹)
- 로그인 필수: 마이/프로필 수정/즐겨찾기/팔로우/체크인/리뷰/좋아요/알림
- 서버 권한 검증(Firestore Rules/Functions)과 클라이언트 가드를 동시에 적용한다.

## 9. 비동기/상태 처리 규칙
- suspend 함수는 취소 가능성을 고려한다.
- 로딩/성공/실패 상태를 명시적으로 분리한다.
- 재시도 가능한 실패와 불가능한 실패를 분리한다.
- 페이징은 `PagedResult` 기준으로 `nextCursor/hasNext`를 사용한다.

## 10. 오류 처리 규칙
- 에러 타입은 `Unauthorized`, `PermissionDenied`, `ValidationFailed`, `NetworkError`, `NotFound`, `Unknown`을 기본으로 한다.
- 사용자 액션 에러는 사용자 문구로 변환 가능해야 한다.
- 로그에는 내부 원인(cause)을 남기고 UI에는 안전한 메시지만 노출한다.

## 11. UI 컴포넌트 규칙
- 공통 UI 컴포넌트(`ConCafeCard`, `ConCafeTopBar` 등)를 우선 재사용한다.
- 화면별 중복 컴포넌트는 공통 컴포넌트로 승격한다 (`component` 패키지).
- 빈 상태/에러 상태 컴포넌트를 항상 제공한다.
- 접근성(콘텐츠 설명, 클릭 영역, 색 대비)을 기본 준수한다.

## 12. 테스트 규칙
- UseCase 단위 테스트를 우선 작성한다.
- 권한 시나리오(`VISITOR`/`CAFE_OWNER`/`ADMIN`/`CAST`) 테스트를 필수로 포함한다.
- 로그인 가드 시나리오(게스트 -> 로그인 유도 -> 원복귀) 테스트를 포함한다.
- 핵심 플로우(홈->탐색->상세->체크인->리뷰)는 스모크 테스트 대상이다.

## 13. 금지 규칙
- ViewModel 또는 UI에서 Firestore 직접 호출 금지
- UI에서 권한 판단 하드코딩 금지
- domain/model에 mutable 상태(`var`) 남용 금지
- 에러를 `Exception` 문자열로만 처리하는 방식 금지
- 인증 필요 기능을 가드 없이 노출하는 구현 금지
- `event`를 상태처럼 재소비 가능한 구조로 저장하는 구현 금지
- `onAction` 전용 내부 메서드를 `public/internal`로 노출하는 구현 금지

## 14. 코드 리뷰 체크리스트
- 모듈 경계 위반이 없는가
- 로그인 가드 누락 기능이 없는가
- 에러 매핑이 `AppError`로 일관되는가
- 상태 모델(`UiState`)이 로딩/실패를 포함하는가
- `uiState`, `event`, `action` 3프로퍼티 패턴을 준수하는가
- `onAction` 전용 처리 메서드가 `private`로 캡슐화되어 있는가
- 중복 코드가 공통화 가능한 수준인지 검토했는가

## 15. 적용 우선순위
1. 인증/권한/라우트 가드 규칙
2. 도메인 반환 타입(`AppResult/AppError/PagedResult`) 일관성
3. 모듈 경계 및 레이어 분리
4. UI 상태/테스트 규칙

## 16. 코드 배치 규칙 (Kotlin/Swift 공통)
- 클래스/구조체 내부에서 프로퍼티 선언은 한 줄씩 선언하고 선언 사이를 개행한다.
- 메서드 내부에서는 변수 선언과 초기값 세팅을 상단에 모은다.
- 메서드 호출(비즈니스 실행/외부 호출)은 하단에 모은다.
- Kotlin 기준으로 `val name = 0` 같은 변수 선언 블록과 `invoke()` 호출 블록 사이에는 반드시 개행한다.
- `private` 메서드는 클래스 상단에 배치한다.
- `public` 메서드는 클래스 하단에 배치한다.
- `init` 블록은 클래스 하단(메서드 아래)에 배치한다.
- 지역변수 선언부는 줄 사이 개행 없이 연속 배치한다.
- 조건 분기는 `if-else`를 기본으로 사용한다.
- `if (condition) return` 형태의 조기 반환 패턴보다 `if-else`로 명시적으로 분기한다.
- Swift에서도 `guard ... else { return }` 남용보다 `if-else` 분기를 우선한다.
- 단, 중첩 인덴트 스코프가 3단계 이상 될 경우 가독성을 위해 가드(조기 종료)로 평탄화한다.

### 16-1. Kotlin 예시
```kotlin
class SampleViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase
) {
    private val _uiState = MutableStateFlow(HomeUiState())

    private val _event = MutableSharedFlow<HomeEvent>(replay = 0, extraBufferCapacity = 1)

    private fun buildRequest(userId: String?): HomeRequest {
        val resolvedUserId = userId ?: ""
        val timestamp = Clock.System.now().toString()
        return HomeRequest(resolvedUserId, timestamp)
    }

    private suspend fun executeLoad(request: HomeRequest) {
        val result = getHomeFeedUseCase.invoke(request.userId)

        _uiState.update { it.copy(isLoading = false) }
        _event.emit(HomeEvent.LoadCompleted(result))
    }

    fun action(action: HomeAction) {
        when (action) {
            is HomeAction.Enter -> onEnter(action.userId)
            is HomeAction.Refresh -> onEnter(action.userId)
        }
    }

    fun onEnter(userId: String?) {
        val request = buildRequest(userId)
        val isFirstLoad = _uiState.value.feed == null

        if (isFirstLoad) {
            _uiState.update { it.copy(isLoading = true) }
        }
        // 하단 호출부
        // launch { executeLoad(request) }
    }

    init {
        // 클래스 하단 init 블록
    }
}
```

#### Kotlin 조건문 예시
```kotlin
// 권장
if (isLoggedIn) {
    submitReview()
} else {
    showLoginRequired()
}

// 비권장
if (!isLoggedIn) return
submitReview()

// 예외 허용(3단계 이상 중첩 방지)
if (!isLoggedIn) return
if (!isVerifiedVisit) return
if (!hasReviewPermission) return
submitReview()
```

### 16-2. Swift 예시
```swift
final class SampleViewModel {
    private let useCase: HomeUseCase

    private(set) var uiState: HomeUiState

    private let event = PassthroughSubject<HomeEvent, Never>()

    private func buildRequest(userId: String?) -> HomeRequest {
        let resolvedUserId = userId ?? ""
        let timestamp = ISO8601DateFormatter().string(from: Date())
        return HomeRequest(userId: resolvedUserId, timestamp: timestamp)
    }

    private func executeLoad(request: HomeRequest) {
        let result = useCase.invoke(userId: request.userId)
        event.send(.loadCompleted(result))
    }

    func action(_ action: HomeAction) {
        switch action {
        case let .enter(userId):
            onEnter(userId: userId)
        case let .refresh(userId):
            onEnter(userId: userId)
        }
    }

    func onEnter(userId: String?) {
        let request = buildRequest(userId: userId)
        let isFirstLoad = uiState.feed == nil

        if isFirstLoad {
            uiState.isLoading = true
        }
        // 하단 호출부
        executeLoad(request: request)
    }

    init(useCase: HomeUseCase, initialState: HomeUiState) {
        self.useCase = useCase
        self.uiState = initialState
    }
}
```

#### Swift 조건문 예시
```swift
// 권장
if isLoggedIn {
    submitReview()
} else {
    showLoginRequired()
}

// 비권장
guard isLoggedIn else { return }
submitReview()

// 예외 허용(3단계 이상 중첩 방지)
guard isLoggedIn else { return }
guard isVerifiedVisit else { return }
guard hasReviewPermission else { return }
submitReview()
```
