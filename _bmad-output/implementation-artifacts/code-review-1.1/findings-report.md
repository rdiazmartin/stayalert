# Code Review 1.1 — Findings Report

**Commit reviewed:** `e08e670` — `feat(1.1): scaffolding del proyecto Android y tema visual`
**Spec:** `_bmad-output/implementation-artifacts/1-1-scaffolding-del-proyecto-y-tema-visual.md`
**Layers:** Blind Hunter, Edge Case Hunter, Acceptance Auditor (single model; no subagents available)

---

## Summary

| Bucket | Count |
|--------|-------|
| patch | 3 |
| decision_needed | 0 |
| defer | 3 |
| dismiss | 0 |
| **Total actionable** | **3** |

All findings are low-to-medium severity. No blocking issues for a scaffolding story.

---

## Triage: patch

### 1. `MainActivity` omits `enableEdgeToEdge()`
- **Source:** blind
- **Severity:** medium
- **Location:** `app/src/main/java/com/stayalert/ui/MainActivity.kt:188`
- **Detail:** A Material 3 + Compose app should call `enableEdgeToEdge()` before `setContent()` to ensure the system bars are handled correctly. Without it, the app may show non-transparent system bars, which is especially visible for a dark-themed app.
- **Fix:** Add `enableEdgeToEdge()` as the first statement inside `onCreate`.

### 2. Activity theme declaration is redundant
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/AndroidManifest.xml:153,158`
- **Detail:** Both `<application>` and `<activity>` declare the same `android:theme="@style/Theme.StayAlert"`. Since the activity inherits the application theme, the activity-level declaration is redundant. Removing it reduces the chance of future inconsistencies if a different activity theme is introduced.
- **Fix:** Remove `android:theme="@style/Theme.StayAlert"` from the `<activity>` tag.

### 3. `.gitignore` missing `.kotlin/`
- **Source:** blind+edge
- **Severity:** low
- **Location:** `.gitignore`
- **Detail:** AGP 8+/Kotlin 2.x generates a `.kotlin/` directory with K2 compiler metadata. It should be ignored like `.gradle/` and `build/`.
- **Fix:** Add `.kotlin/` to `.gitignore`.

---

## Triage: defer

### 4. `SurfaceOverlay` mapped to `surfaceContainerLowest` is semantically risky
- **Source:** edge+auditor
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/theme/Theme.kt:286`
- **Detail:** The DESIGN.md says `SurfaceOverlay` (#000000) is "the product" (the isolation overlay), not a Material 3 surface container. Mapping it to `surfaceContainerLowest` is a forced fit. Currently harmless because nothing uses `surfaceContainerLowest`, but if a future component accidentally uses that slot, it will render pure black. Defer: decide the proper slot or create a dedicated overlay color token when implementing the overlay in a later story.

### 5. `Warning` color token is not exposed in the theme
- **Source:** auditor
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/theme/Color.kt:232`, `Theme.kt:265-287`
- **Detail:** AC-3 says the theme must apply the tokens from DESIGN.md. `Warning` is defined in `Color.kt` but is not part of `StayAlertDarkColorScheme`. It is currently unused. Defer: wire it into a custom color slot or use it locally when warning states are implemented.

### 6. `themes.xml` may show a light splash before Compose loads
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/res/values/themes.xml:2`
- **Detail:** `Theme.Material.NoActionBar` follows the system default theme. On devices configured for light mode, the initial activity background could be light until Compose applies the dark color scheme. This contradicts the DESIGN.md's "dark and quiet" intent. Defer: force a dark theme in `themes.xml` (or add `values-night/themes.xml`) when the final launch experience is polished; out of scope for pure scaffolding.

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Stack versions | ✅ | Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36, compileSdk 37 |
| 2. Package structure | ✅ | `ui/`, `domain/`, `data/`, `system/` created under `com.stayalert` |
| 3. Theme tokens | ⚠️ | All 11 tokens defined in `Color.kt`. Two mapping concerns deferred (#4, #5). No functional impact in this story. |
| 4. `<queries>` | ✅ | Declared for `com.microsoft.teams` |
| 5. CI | ✅ | GitHub Actions workflow runs lint + unit tests on push to `main` and PRs |
| 6. Build + smoke tests | ✅ | `assembleDebug`, `test`, `lint` pass; emulator smoke test passes |

---

## Recommendation

**Approve with 3 minor patches.** Apply the `patch` findings (enable edge-to-edge, remove redundant theme, add `.kotlin/` to `.gitignore`), then merge. The deferred items are intentional scaffolding gaps that should be resolved when the overlay, warning states, and launch theme are implemented in subsequent stories.
