# ConCafe 디자인 시스템

이 문서는 ConCafe 앱의 색상 디자인 시스템 규칙이다.

## 0. 브랜드 테마

라이트 모드는 사용자가 선택 가능한 브랜드 테마 2종을 제공한다 (설정 > 테마 스타일):

- **메이드카페(MAID_CAFE, 기본)**: 핑크 팔레트 — 기존 브랜드 색상
- **맨즈콘카페(MENS_CON_CAFE)**: Purple Dream 팔레트 (`design/palette1.scss`)

다크 모드는 브랜드 테마와 무관하게 Purple Dream 다크 값을 사용한다.
저장/전파: `BrandTheme`(shared domain) → `Observe/SetBrandThemeUseCase` → 플랫폼 설정 화면. 토큰이 자체적으로 해석하므로 화면 코드는 테마를 분기하지 않는다.

## 1. 원칙

- 화면 코드에 hex 색상을 하드코딩하지 않는다. 반드시 `ConCafeColors` 시맨틱 토큰을 사용한다.
- 토큰 이름·값은 Compose와 iOS(SwiftUI)에서 1:1로 동일하게 유지한다.
  - Compose: `composeApp/src/commonMain/kotlin/com/hhp227/concafe/presentation/component/ConCafeColors.kt`
  - iOS: `iosApp/iosApp/Presentation/Component/ConCafeColors.swift`
- 토큰은 `themedColor(light:, dark:, maidLight:)`로 값을 명시적으로 정의한다. 기존 `colorFromHex`/`Color(hex:)`의 휘도 기반 자동 다크 보정에 의존하지 않는다.
- 서드파티 브랜드 색상만 예외로 hex를 직접 사용한다: 카카오 `FEE500`/`222222`, 애플·X `111111`, 틱톡 `010101`, 인스타그램 `E1306C`.
- 사진 위 스크림/글래스 오버레이는 순수 검정/흰색 알파를 유지한다 (`0x66000000`, `0x80FFFFFF` 등).

## 2. 토큰 정의

라이트(맨즈콘카페) / 메이드 / 다크 순. 상태·골드 토큰은 브랜드 테마 공통.

| 토큰 | 라이트(맨즈콘) | 메이드 | 다크 | 용도 |
|------|--------|--------|------|------|
| primary | `5E548E` | `EF6797` | `9F86C0` | 주요 버튼, 활성 탭, 강조 텍스트 |
| onPrimary | `FFFFFF` | `FFFFFF` | `231942` | primary 위 콘텐츠 |
| primaryContainer | `EFE9F6` | `FFD1DC` | `3A2F55` | 칩/뱃지/카드 강조 배경 |
| onPrimaryContainer | `4A4174` | `7C3F67` | `D6C8F0` | primaryContainer 위 텍스트 |
| secondary | `9F86C0` | `F7A0C1` | `BE95C4` | 보조 강조, 그라데이션 페어 |
| onSecondary | `FFFFFF` | `FFFFFF` | `2E2347` | secondary 위 콘텐츠 |
| secondaryContainer | `E7DDF3` | `F8C5D7` | `453963` | 보조 칩/태그 배경 |
| onSecondaryContainer | `514578` | `8B5164` | `DFD3F0` | secondaryContainer 위 텍스트 |
| tertiary | `C97BA0` | `D1436F` | `E0B1CB` | 포인트 — 하트/좋아요/찜 |
| onTertiary | `FFFFFF` | `FFFFFF` | `43202F` | tertiary 위 콘텐츠 |
| tertiaryContainer | `F9E4EE` | `FDE7EF` | `4E3040` | 포인트 배경 |
| onTertiaryContainer | `8B4A68` | `9E2E5C` | `F0CCDE` | tertiaryContainer 위 텍스트 |
| background | `FBFAFD` | `FFFBFD` | `171225` | 화면 배경 |
| surface | `FFFFFF` | `FFFFFF` | `221B36` | 카드/시트 표면 |
| surfaceVariant | `F4F1F8` | `F8F5F6` | `2A2244` | 구분 패널/입력 필드 배경 |
| surfaceTint | `F8F6FB` | `FCE6EF` | `1D1730` | 은은한 틴트 배경(섹션 구분) |
| textPrimary | `231942` | `2B2330` | `EDE8F5` | 본문/제목 텍스트 |
| textSecondary | `6E6590` | `7A707A` | `A79CC4` | 보조 텍스트 |
| textMuted | `9C94B3` | `8F848F` | `7A6FA0` | 비활성/플레이스홀더 |
| outline | `E9E3F2` | `E8DFE7` | `332A4E` | 구분선/테두리 |
| outlineStrong | `CFC7E0` | `B3ACB7` | `453B66` | 강조 테두리 |
| success | `2E9E5B` | 공통 | `5BC98A` | 성공/출근중/증가 |
| successContainer | `E4F5EB` | 공통 | `1E3B2C` | 성공 배경 |
| error | `E53935` | 공통 | `F28B84` | 오류/삭제/감소 |
| errorContainer | `FDE9E7` | 공통 | `45211E` | 오류 배경 |
| warning | `D9822B` | 공통 | `F0BE6B` | 주의/대기 |
| warningContainer | `FBEEDC` | 공통 | `43351A` | 주의 배경 |
| info | `4A79E8` | 공통 | `8FB0FF` | 정보/링크성 강조 |
| infoContainer | `E8F0FF` | 공통 | `202C4D` | 정보 배경 |
| gold | `E2A81E` | 공통 | `F1D88D` | 별점/랭킹 아이콘 |
| goldDeep | `6B5320` | 공통 | `E8D290` | 골드 컨테이너 위 텍스트 |
| goldContainer | `FFF6D7` | 공통 | `46390F` | 랭킹/별점 배경 |

## 3. 사용 패턴

Compose:

```kotlin
Text(color = ConCafeColors.textSecondary, ...)
Button(colors = ButtonDefaults.buttonColors(containerColor = ConCafeColors.primary)) { ... }
// 알파는 copy로
Box(Modifier.background(ConCafeColors.primary.copy(alpha = 0.1f)))
```

SwiftUI:

```swift
Text("...").foregroundStyle(ConCafeColors.textSecondary)
.background(ConCafeColors.surface)
// 알파는 opacity로
ConCafeColors.primary.opacity(0.1)
```

- Material 테마 슬롯(`MaterialTheme.colorScheme.*`)도 동일 토큰으로 매핑되어 있다 (`ConCafeTheme.kt`). 컴포넌트 기본값을 우선 쓰고, 명시 색이 필요할 때 `ConCafeColors`를 쓴다.
- 그라데이션은 `primary → secondary` 또는 `surfaceTint → primaryContainer` 조합을 기본으로 한다.
- 다크 모드 분기(`isDarkMode ? A : B`)를 화면 코드에 두지 않는다. 토큰이 자체적으로 해결한다.

## 4. 새 색이 필요할 때

1. 기존 토큰 중 시맨틱하게 맞는 것이 있는지 먼저 찾는다.
2. 없으면 `ConCafeColors.kt`와 `ConCafeColors.swift` **두 곳에 동시에** 같은 이름으로 추가한다.
3. 라이트(맨즈콘)/메이드/다크 값을 모두 정의하고(브랜드 공통이면 maidLight 생략), 이 문서의 토큰 표를 갱신한다.
