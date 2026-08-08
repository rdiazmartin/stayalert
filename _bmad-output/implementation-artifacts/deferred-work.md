# Deferred Work Ledger

## Deferred from: code review of 1-1-scaffolding-del-proyecto-y-tema-visual.md (2026-08-09)

- `SurfaceOverlay` mapped to `surfaceContainerLowest` is semantically risky [`app/src/main/java/com/stayalert/ui/theme/Theme.kt:286`] — deferred; resolve when overlay implemented. Decide proper slot or dedicated overlay color token.
- `Warning` color token is not exposed in the theme [`app/src/main/java/com/stayalert/ui/theme/Color.kt:232`, `Theme.kt:265-287`] — deferred; no warning states yet. Wire into custom color slot or local usage when warning states are implemented.
- `themes.xml` may show a light splash before Compose loads [`app/src/main/res/values/themes.xml:2`] — deferred; resolve when final launch theme is polished. Force dark theme or add `values-night/themes.xml`.

## Deferred from: code review of 1-2-aviso-de-uso-responsable.md (2026-08-09)

- `SharingStarted.Eagerly` mantiene el flow activo sin suscriptores [`app/src/main/java/com/stayalert/ui/viewmodel/MainViewModel.kt:14`] — deferred; revisar si se puede usar `WhileSubscribed` con test helper cuando se introduzca más configuración (story 1.4).
- `SettingsKeys.TARGET_PACKAGE` y `TARGET_ACTIVITY` sin uso [`app/src/main/java/com/stayalert/data/SettingsKeys.kt:5-6`] — deferred; se usarán en story 1.4.

## Deferred from: code review of 1-3-auditoria-y-guiado-de-permisos.md (2026-08-09)

- Navegación con `mutableStateOf` en vez de Navigation Compose [`app/src/main/java/com/stayalert/ui/MainActivity.kt:134`] — deferred; evaluar Navigation Compose cuando haya más pantallas (story 1.4 añadirá más configuración).
- `SettingsViewModel.refresh()` no es idempotente ante recomposiciones [`app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:297-305`] — deferred; no requiere acción (LocalLifecycleOwner es estable).

## Deferred from: code review of 1-4-configuracion-de-la-app-objetivo-y-exencion-de-bateria.md (2026-08-09)

- `DEFAULT_TARGET_ACTIVITY` hardcodeado como constante top-level [`app/src/main/java/com/stayalert/data/DataStoreSettingsRepository.kt:58`] — deferred; centralizar defaults con `TargetAppLauncher` (AD-3) en story 2.1.
- `SettingsScreen` crece en complejidad [`app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt`] — deferred; extraer `TargetAppSection` y `BatterySection` como componentes cuando se añada más configuración.
