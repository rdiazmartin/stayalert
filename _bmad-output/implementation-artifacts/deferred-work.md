# Deferred Work Ledger

## Deferred from: code review of 1-1-scaffolding-del-proyecto-y-tema-visual.md (2026-08-09)

- `SurfaceOverlay` mapped to `surfaceContainerLowest` is semantically risky [`app/src/main/java/com/stayalert/ui/theme/Theme.kt:286`] — deferred; resolve when overlay implemented. Decide proper slot or dedicated overlay color token.
- `Warning` color token is not exposed in the theme [`app/src/main/java/com/stayalert/ui/theme/Color.kt:232`, `Theme.kt:265-287`] — deferred; no warning states yet. Wire into custom color slot or local usage when warning states are implemented.
- `themes.xml` may show a light splash before Compose loads [`app/src/main/res/values/themes.xml:2`] — deferred; resolve when final launch theme is polished. Force dark theme or add `values-night/themes.xml`.
