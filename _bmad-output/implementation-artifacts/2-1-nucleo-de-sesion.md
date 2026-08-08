---
baseline_commit: ed8de8c
---

# Story 2.1: Núcleo de sesión

Status: done

## Story

As a usuario,
I want que la app valide las condiciones antes de iniciar y que no deje residuos si el proceso muere,
so that la sesión solo arranca cuando todo está listo y nunca queda en estado inconsistente.

## Acceptance Criteria

1. **Validación previa al inicio (FR-5)** — Al pulsar "Iniciar Jornada", el sistema valida: permisos concedidos (overlay, notificaciones), aviso aceptado y app objetivo instalada. Si falta algo, el botón no inicia la sesión y muestra el motivo exacto.
2. **Transición a `Lanzando`** — Si todo está listo, la sesión transita a `Lanzando` (FR-5).
3. **`SessionController` único propietario** — `SessionState` sealed class: `Inactiva`/`Lanzando`/`Aislada`/`Deteniendo`, expuesto como `StateFlow` (AD-1). Ningún otro componente muta el estado.
4. **Event loop de consumidor único** — Los eventos se procesan en un único dispatcher (Channel con consumidor único) (AD-9).
5. **Recuperación ante kill (FR-17)** — Tras un kill del proceso, no queda overlay huérfano ni servicio activo; al reabrir, el estado es `Inactiva`.
6. **`Clock` inyectable** — `Clock` (interfaz) con implementación de producción `SystemClock` y fake de test (AD-12).
7. **Tests** — Unit tests con Robolectric: validación FR-5 (todos los casos), transición a `Lanzando`, idempotencia de eventos, estado `Inactiva` tras kill.

## Tasks / Subtasks

- [x] Task 1: Modelos de dominio (AC: 3, 4)
  - [x] 1.1 Crear `domain/SessionState.kt` — sealed class `SessionState { Inactiva, Lanzando, Aislada, Deteniendo }`
  - [x] 1.2 Crear `domain/SessionEvent.kt` — sealed class con subclases fijas (AD-10): `PatternDetected`, `StopRequested`, `OverlayShown`, `OverlayFailed(cause)`, `LaunchFailed(cause)`, `ScreenOff`, `OverlayMissing`, `PermissionRevoked`, `TargetLeftForeground`, `TargetCrashed`, `HideOverlayWindows`, `BatteryWarning(level)`, `BatteryCritical(level)`
  - [x] 1.3 Crear `domain/SessionCommand.kt` — sealed class (AD-7): `ShowOverlay`, `HideOverlay`, `StartFgs`, `StopFgs`, `StartWatchdog`, `StopWatchdog`, `LaunchTarget`
  - [x] 1.4 Crear `domain/TerminationReason.kt` — enum (AD-10): `Pattern`, `ManualStop`, `ScreenOff`, `OverlayMissing`, `PermissionRevoked`, `TargetLeftForeground`, `TargetCrashed`, `HideOverlayWindows`, `BatteryCritical`, `LaunchFailed`, `OverlayFailed`
- [x] Task 2: `Clock` inyectable (AC: 6)
  - [x] 2.1 Crear `domain/Clock.kt` — interfaz con `now(): Long` (o `elapsedRealtime()`)
  - [x] 2.2 Crear `domain/SystemClock.kt` — implementación de producción
- [x] Task 3: `SessionController` (AC: 3, 4)
  - [x] 3.1 Crear `domain/SessionController.kt` — único propietario del estado, `StateFlow<SessionState>`
  - [x] 3.2 Event loop: `Channel<SessionEvent>` con consumidor único (AD-9)
  - [x] 3.3 Procesar eventos: transiciones de estado válidas; eventos no admitidos = no-ops (AD-9)
  - [x] 3.4 Terminación idempotente: eventos de terminación en `Deteniendo`/`Inactiva` son no-ops (AD-9)
  - [x] 3.5 Exponer `SessionCommand` a los componentes SO (órdenes fire-and-forget, AD-7)
- [x] Task 4: Validación FR-5 (AC: 1, 2)
  - [x] 4.1 Crear `domain/SessionValidator.kt` (o método en controller) que valida: permisos (vía `PermissionAuditor`), aviso aceptado (vía `SettingsRepository`), app objetivo instalada (vía `AppInstalledChecker`)
  - [x] 4.2 Devolver resultado con motivo exacto de bloqueo (enum `ValidationFailure { PERMISSION_OVERLAY, PERMISSION_NOTIFICATIONS, NOTICE_NOT_ACCEPTED, TARGET_NOT_INSTALLED }`)
  - [x] 4.3 `startSession()`: si validación OK → transición a `Lanzando` + emitir `LaunchTarget`; si falla → no transicionar, exponer motivo
- [x] Task 5: Integración con UI (AC: 1, 2)
  - [x] 5.1 `MainViewModel` expone `sessionState` y `validationFailure` (o `startError`)
  - [x] 5.2 Botón "Iniciar Jornada" llama a `startSession()`; si falla, muestra el motivo exacto
  - [x] 5.3 Estado `Lanzando` → botón deshabilitado con spinner "Abriendo app objetivo…" (UX-DR9)
- [x] Task 6: Tests (AC: 7)
  - [x] 6.1 Test `SessionController`: transición a `Lanzando` tras validación OK
  - [x] 6.2 Test validación FR-5: cada caso de fallo devuelve el motivo correcto
  - [x] 6.3 Test idempotencia: eventos de terminación en `Deteniendo`/`Inactiva` son no-ops
  - [x] 6.4 Test kill: estado `Inactiva` al reabrir (no persiste sesión, AD-6)
  - [x] 6.5 Verificar `./gradlew test` y `./gradlew lint`

## Dev Notes

### Contexto del producto

Esta story crea el núcleo de la sesión: el `SessionController` que será el cerebro de todo el Epic 2. Las stories 2.2-2.5 añadirán los componentes SO (overlay, FGS, watchdog, launcher) como clientes del controller. Aquí NO se implementan esos componentes — solo el controller, el contrato de eventos/comandos, la validación FR-5 y la integración con la UI.

### Stack

- Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36
- Coroutines + Flow (Channel para event loop)
- DI manual por constructor (sin Hilt)
- JUnit4 + Robolectric 4.16 + Compose UI tests

### Arquitectura (spine — ADs vinculantes)

- **AD-1**: `SessionController` único propietario del estado; `SessionState` sealed + `StateFlow`
- **AD-7**: `SessionCommand` sealed; órdenes fire-and-forget; handshake Lanzando→Aislada solo vía `OverlayShown` (en story 2.2)
- **AD-9**: event loop con Channel de consumidor único; terminación idempotente; eventos en estado no admitido = no-ops
- **AD-10**: `SessionEvent` sealed con subclases fijas; `TerminationReason` enum; mapeo motivo→texto en `Notifier` (story 2.4)
- **AD-12**: `Clock` inyectable (producción: `SystemClock`; test: fake)
- **AD-6**: sesión efímera — no se persiste; tras kill, estado `Inactiva`
- Convención: componentes SO reportan eventos, nunca mutan estado

### Estructura de archivos

- `domain/SessionState.kt`, `domain/SessionEvent.kt`, `domain/SessionCommand.kt`, `domain/TerminationReason.kt`
- `domain/Clock.kt`, `domain/SystemClock.kt`
- `domain/SessionController.kt`
- `domain/SessionValidator.kt` (o integrado en controller)
- `ui/viewmodel/MainViewModel.kt` (modificado)
- `ui/MainActivity.kt` (modificado — botón + spinner + mensaje de error)

### Testing

- Robolectric `@Config(sdk = [34])`
- Fakes: `PermissionAuditor`, `SettingsRepository`, `AppInstalledChecker`, `Clock`
- `Dispatchers.setMain(UnconfinedTestDispatcher())` para viewModelScope
- Test del event loop: enviar eventos y verificar transiciones con `advanceUntilIdle()`

### Project Structure Notes

- `domain/` está vacío hasta ahora — esta story lo puebla
- `MainViewModel` ya existe con `noticeAccepted`; se amplía con `sessionState` y `startError`
- `MainScreen` ya tiene el botón "Iniciar Jornada" (no-op); se conecta a `startSession()`

### Previous Story Intelligence

Stories 1.1-1.4 establecieron:
- `SettingsRepository` (interfaz) + `DataStoreSettingsRepository` — `noticeAccepted`, `targetPackage`, `targetActivity`
- `PermissionAuditor` (interfaz) + `SystemPermissionAuditor` — `canDrawOverlays()`, `areNotificationsEnabled()`
- `AppInstalledChecker` (interfaz) + `SystemAppInstalledChecker`
- `MainViewModel` con `noticeAccepted` (SharingStarted.Eagerly)
- `MainScreen` con botón "Iniciar Jornada" (enabled = noticeAccepted)
- Patrón de tests: fakes de interfaces, `Dispatchers.setMain(UnconfinedTestDispatcher())`

### Git Intelligence

Últimos commits:
- `ed8de8c docs: README con instrucciones de compilacion y guia de uso`
- `ceb2013 feat(1.4): configuracion de la app objetivo y exencion de bateria`
- `5e68a0e feat(1.3): auditoria y guiado de permisos con acceso a Ajustes`
- `07d4b2e feat(1.2): aviso de uso responsable con persistencia en DataStore`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.1]
- [Source: `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md` — §4.2 FR-5, FR-17]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — AD-1, AD-6, AD-7, AD-9, AD-10, AD-12]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md` — State Patterns, Flow 1]

## Dev Agent Record

### Agent Model Used

(opencode)

### Debug Log References

- `SessionController` usa `Channel(UNLIMITED)` con consumidor único en `init` (AD-9)
- Tests del controller requirieron `runTest(UnconfinedTestDispatcher())` para que el consumidor del channel procese inmediatamente; `backgroundScope` para evitar `UncompletedCoroutinesError`
- `MainViewModel` ahora recibe `SessionController` (constructor); fakes de tests actualizados
- Smoke test emulador: al pulsar "Iniciar Jornada" con permisos pendientes, muestra "Falta el permiso de overlay. Tócalo para abrir Ajustes." (FR-5 OK)
- **Repo remoto (usuario):** subir el repositorio a `https://github.com/rdiazmartin/stayalert` más tarde (aún no configurado como remote)

### Completion Notes List

- `domain/SessionState.kt` — sealed class `Inactiva/Lanzando/Aislada/Deteniendo`
- `domain/SessionEvent.kt` — 13 subclases fijas (AD-10)
- `domain/SessionCommand.kt` — 7 órdenes fire-and-forget (AD-7)
- `domain/TerminationReason.kt` — 11 motivos (AD-10)
- `domain/Clock.kt` + `domain/SystemClock.kt` (AD-12)
- `domain/ValidationFailure.kt` — 4 motivos de bloqueo FR-5
- `domain/SessionValidator.kt` — valida permisos + aviso + app objetivo
- `domain/SessionController.kt` — único propietario del estado; event loop Channel; terminación idempotente; handshake Lanzando→Aislada vía `OverlayShown`; `terminate()` emite HideOverlay/StopFgs/StopWatchdog
- `MainViewModel` ampliado: `sessionState`, `startError`, `startSession()`, `clearStartError()`
- `MainScreen`: botón conectado a `startSession()`, spinner "Abriendo app objetivo…" en Lanzando, mensaje de error con motivo exacto
- Tests: `SessionControllerTest` (6), `SessionValidatorTest` (5), `MainViewModelTest` (2, actualizado), `MainScreenTest` (2, actualizado)
- Build/test/lint verdes; smoke test en emulador OK

### File List

- `app/src/main/java/com/stayalert/domain/SessionState.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/SessionEvent.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/SessionCommand.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/TerminationReason.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/Clock.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/SystemClock.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/ValidationFailure.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/SessionValidator.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/SessionController.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/viewmodel/MainViewModel.kt` (modificado)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (modificado)
- `app/src/test/java/com/stayalert/domain/SessionControllerTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/domain/SessionValidatorTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/ui/MainViewModelTest.kt` (modificado)
- `app/src/test/java/com/stayalert/ui/MainScreenTest.kt` (modificado)

---

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `SessionController.terminate()` no expone feedback de fin de sesión a la UI [`app/src/main/java/com/stayalert/domain/SessionController.kt:190-198`] — fixed
- [x] [Review][Patch] `trySend` puede fallar silenciosamente si el consumidor muere [`app/src/main/java/com/stayalert/domain/SessionController.kt:89-91`] — fixed
- [x] [Review][Patch] `MainScreen` no muestra el motivo de bloqueo de forma preventiva [`app/src/main/java/com/stayalert/ui/MainActivity.kt:412-419`] — fixed

### defer

- [x] [Review][Defer] `SystemClock` acopla `domain/` a Android [`app/src/main/java/com/stayalert/domain/SystemClock.kt:5`] — deferred, pre-existing (mover a `system/` en refactor)
- [x] [Review][Defer] `BatteryWarning` no emite aviso de notificación [`app/src/main/java/com/stayalert/domain/SessionController.kt:157-159`] — deferred, pre-existing (requiere `Notifier` de story 2.4)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-2.1/findings-report.md`
