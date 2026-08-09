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

## Deferred from: code review of 2-1-nucleo-de-sesion.md (2026-08-09)

- `SystemClock` acopla `domain/` a Android [`app/src/main/java/com/stayalert/domain/SystemClock.kt:5`] — deferred; mover `SystemClock` a `system/` (capa de integración) en refactor.
- `BatteryWarning` no emite aviso de notificación [`app/src/main/java/com/stayalert/domain/SessionController.kt:157-159`] — deferred; requiere `Notifier` (story 2.4) para publicar el aviso al 15% (FR-16).

## Deferred from: code review of 2-2-despliegue-secuencial-y-overlay-de-aislamiento.md (2026-08-09)

- `SessionCommandHandler` no usa `clock` (parámetro muerto) [`app/src/main/java/com/stayalert/system/SessionCommandHandler.kt:176`] — deferred; se resuelve con el fix del delay vía clock o se elimina en refactor.
- `MainActivity` con `lateinit` + lazy circular [`app/src/main/java/com/stayalert/ui/MainActivity.kt:78-110`] — deferred; considerar `AppContainer`/`ServiceLocator` simple cuando crezca el wiring (stories 2.4/2.5).

## Deferred from: code review of 2-3-patron-de-salida.md (2026-08-09)

- `PatternDetector` no expone el estado del contador [`app/src/main/java/com/stayalert/domain/PatternDetector.kt`] — deferred; añadir `tapCount` expuesto si se necesita debug en el futuro.
- `MainActivity` crea dos `SystemClock()` separados [`app/src/main/java/com/stayalert/ui/MainActivity.kt:97-100`] — deferred; compartir instancia única con `AppContainer`.

## Deferred from: code review of 2-4-servicio-en-primer-plano-y-notificaciones.md (2026-08-09)

- `SystemNotifier` no maneja `POST_NOTIFICATIONS` (API 33+) [`app/src/main/java/com/stayalert/data/SystemNotifier.kt:94-95`] — deferred; la validación FR-5 ya cubre `areNotificationsEnabled()`.
- `SystemNotifier` no expone `buildSessionNotification()` [`app/src/main/java/com/stayalert/data/SystemNotifier.kt`] — deferred; se resuelve con el fix de notificación única.

## Deferred from: code review of 2-5-watchdog-de-sesion.md (2026-08-09)

- `SystemWatchdog` no distingue `TargetCrashed` de `TargetLeftForeground` [`app/src/main/java/com/stayalert/system/SystemWatchdog.kt:127-131`] — deferred; limitación de `UsageStatsManager`; el motivo de la notificación será "la app objetivo salió de primer plano" en ambos casos.
- `SystemWatchdog` no maneja `BatteryWarning` repetido [`app/src/main/java/com/stayalert/system/SystemWatchdog.kt:137-138`] — deferred; requiere deduplicación (flag) cuando se implemente el aviso de batería (FR-16).

## Deferred from: code review of 2-6-mock-de-teams-y-validacion-e2e.md (2026-08-09)

- `SessionE2ETest` no verifica el proxy SM-1a (mock resumed ≥ 8 h) [`app/src/androidTest/java/com/stayalert/SessionE2ETest.kt`] — deferred; validación manual en emulador; documentar el procedimiento en el README.
- `SessionE2ETest` no verifica "0 toques recibidos por el mock" (SM-2) [`app/src/androidTest/java/com/stayalert/SessionE2ETest.kt`] — deferred; requiere lectura de logcat en el E2E completo.

## Deferred from: code review of auditoria-pre-distribucion-2026-08-09.md (2026-08-09)

- `terminate()` transiciona a `Inactiva` antes de que el overlay se oculte efectivamente (hide es async vía scope.launch) [`app/src/main/java/com/stayalert/domain/SessionController.kt:150-158`] — deferred; ya registrado como hallazgo A4 de la auditoría; requiere hide suspend o confirmación de `isVisible() == false`.
- `SystemOverlayController` no usa `createWindowContext` (API 31+) como exige AD-4 [`app/src/main/java/com/stayalert/system/SystemOverlayController.kt:43,97`] — deferred; mejora de arquitectura, funciona el enfoque legacy.
- `PresenceForegroundService` crea su propio `SystemNotifier` y no garantiza `createChannels()` antes de `startForeground()` [`app/src/main/java/com/stayalert/system/PresenceForegroundService.kt:10-14`] — deferred; no alcanzable hoy (MainActivity.onCreate siempre corre primero), pero defensa: crear canales en `StayAlertApplication.onCreate()`.
- `SystemNotifier.showSessionEnded(reason)` nunca se invoca desde código de producción [`app/src/main/java/com/stayalert/data/SystemNotifier.kt:70-78`] — deferred; requiere wiring con `SessionController.terminate()` (relacionado con FR-16).

## Deferred from: code review of story 3-1-kill-switch-fiable (2026-08-09)

- `SessionE2ETest` no compila en compileSdk 37: `AppOpsManager.setMode` fue eliminado en API 37 [`app/src/androidTest/java/com/stayalert/SessionE2ETest.kt:36`] — deferred; preexistente (story 2.6), el CI solo corre unit tests y nunca lo detectó; requiere reemplazo de la concesión de overlay vía appops (p.ej. `UiAutomation.executeShellCommand("appops set ...")` o shadow en el E2E).
