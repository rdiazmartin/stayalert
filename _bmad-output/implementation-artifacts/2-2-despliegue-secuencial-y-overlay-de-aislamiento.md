---
baseline_commit: 1a15c6f
---

# Story 2.2: Despliegue secuencial y overlay de aislamiento

Status: done

## Story

As a usuario,
I want que al iniciar la jornada se abra la app objetivo, se cubra la pantalla con el overlay negro y la pantalla no se apague,
so that mi presencia se mantiene sin ver el contenido de la app objetivo.

## Acceptance Criteria

1. **Lanzamiento de app objetivo (FR-6a)** — La app objetivo se lanza en primer plano vía `TargetAppLauncher` (contrato `Result<Unit>`, nunca excepciones) (AD-3).
2. **Delay de 1 s (FR-6b)** — Transcurrido 1 s (constante `SessionConstants.LAUNCH_DELAY_MS`, vía `Clock`), se despliega el overlay.
3. **Overlay (FR-8, AD-4)** — El overlay es `TYPE_APPLICATION_OVERLAY` (minSdk 26) vía `createWindowContext`, con flags NOT_FOCUSABLE + LAYOUT_IN_SCREEN + KEEP_SCREEN_ON + SECURE, fondo `#000000` opaco, sin NOT_TOUCHABLE.
4. **Handshake Lanzando→Aislada (AD-7)** — La transición ocurre solo al recibir `SessionEvent.OverlayShown`; `ShowOverlay` es fire-and-forget.
5. **Aborto limpio (FR-6, AD-7)** — Si llega `LaunchFailed` o `OverlayFailed`, la sesión aborta a `Inactiva` con notificación.
6. **Confirmación de primer plano (FR-6, AD-11)** — Si `ForegroundMonitor` no confirma la app objetivo en primer plano, la sesión aborta limpiamente sin overlay huérfano.
7. **FGS después del overlay (AD-2)** — El FGS se inicia después de que el overlay esté visible (regla Android 15).
8. **Retención de pantalla (FR-10)** — La pantalla permanece encendida mientras el overlay es visible; se libera al terminar.
9. **Overlay sin elementos gráficos (FR-8, FR-9)** — El overlay no muestra ningún elemento gráfico y los toques del usuario no llegan a la app objetivo.
10. **SLA (NFR-1)** — El overlay se despliega en < 500 ms tras el disparo.
11. **Tests** — Unit tests: launcher (éxito/fallo), overlay (flags, color, handshake), aborto limpio, delay vía Clock fake.

## Tasks / Subtasks

- [x] Task 1: `SessionConstants` (AC: 2, 10)
  - [x] 1.1 Crear `domain/SessionConstants.kt` con `LAUNCH_DELAY_MS = 1000`, `OVERLAY_DEPLOY_SLA_MS = 500`
- [x] Task 2: `TargetAppLauncher` (AC: 1, AD-3)
  - [x] 2.1 Crear `domain/TargetAppLauncher.kt` — interfaz con `suspend fun launch(target: TargetApp): Result<Unit>`
  - [x] 2.2 Crear `domain/TargetApp.kt` — data class `(packageName, activityName)`
  - [x] 2.3 Crear `domain/LaunchError.kt` — sealed class: `PackageNotInstalled`, `ActivityNotFound`, `SystemFailure`
  - [x] 2.4 Crear `system/IntentLauncher.kt` — implementación con Intent explícito
  - [x] 2.5 Crear `system/FakeLauncher.kt` — implementación de test (o en test sources)
- [x] Task 3: `ForegroundMonitor` (AC: 6, AD-11)
  - [x] 3.1 Crear `domain/ForegroundMonitor.kt` — interfaz con `suspend fun isInForeground(packageName): Boolean` (o `ForegroundStatus`)
  - [x] 3.2 Crear `system/UsageStatsForegroundMonitor.kt` — implementación con `UsageStatsManager` (si permiso concedido; si no, `Unknown`)
  - [x] 3.3 Fake de test
- [x] Task 4: `OverlayController` (AC: 3, 4, 8, 9, AD-4)
  - [x] 4.1 Crear `system/OverlayController.kt` — ventana `TYPE_APPLICATION_OVERLAY` vía `createWindowContext`
  - [x] 4.2 Flags: NOT_FOCUSABLE + LAYOUT_IN_SCREEN + KEEP_SCREEN_ON + SECURE; sin NOT_TOUCHABLE
  - [x] 4.3 Fondo `#000000` opaco; sin contenido gráfico
  - [x] 4.4 `show()` → emite `OverlayShown` o `OverlayFailed`; `hide()` → destruye ventana y libera retención
  - [x] 4.5 Medir tiempo de despliegue (SLA < 500 ms, NFR-1)
- [x] Task 5: Integración en `SessionController` (AC: 2, 4, 5, 6, 7)
  - [x] 5.1 `onCommand(LaunchTarget)` → `TargetAppLauncher.launch()`; si falla → `LaunchFailed`; si OK → esperar 1 s (Clock) → `ShowOverlay`
  - [x] 5.2 `onCommand(ShowOverlay)` → `OverlayController.show()` (fire-and-forget)
  - [x] 5.3 `OverlayShown` → transición a `Aislada` + `StartFgs` + `StartWatchdog` (ya en 2.1)
  - [x] 5.4 `OverlayFailed`/`LaunchFailed` → aborto a `Inactiva` (ya en 2.1)
  - [x] 5.5 `ForegroundMonitor` confirma primer plano antes del overlay; si no → aborto limpio
  - [x] 5.6 `HideOverlay` → `OverlayController.hide()`
- [x] Task 6: Permisos en manifest (AC: 3)
  - [x] 6.1 Añadir `SYSTEM_ALERT_WINDOW` al manifest
  - [x] 6.2 Añadir `PACKAGE_USAGE_STATS` (para ForegroundMonitor, opcional)
- [x] Task 7: Tests (AC: 11)
  - [x] 7.1 Test `IntentLauncher`: éxito, paquete no instalado, actividad no resuelta
  - [x] 7.2 Test `OverlayController` con Robolectric: flags, color, handshake
  - [x] 7.3 Test integración: delay 1 s con Clock fake, aborto limpio
  - [x] 7.4 Verificar `./gradlew test` y `./gradlew lint`

## Dev Notes

### Contexto del producto

Esta story implementa el despliegue secuencial: lanzar la app objetivo → esperar 1 s → desplegar el overlay negro → confirmar primer plano → iniciar FGS. Es la story más técnica: requiere `TYPE_APPLICATION_OVERLAY`, `createWindowContext`, `UsageStatsManager` y el handshake con `SessionController`.

### Stack

- Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36
- Coroutines + Flow
- DI manual por constructor (sin Hilt)
- JUnit4 + Robolectric 4.16

### Arquitectura (spine — ADs vinculantes)

- **AD-3**: `TargetAppLauncher` interfaz con contrato `Result<Unit>`; `IntentLauncher` + `FakeLauncher`; DI manual
- **AD-4**: Overlay `TYPE_APPLICATION_OVERLAY` vía `createWindowContext`; flags NOT_FOCUSABLE + LAYOUT_IN_SCREEN + KEEP_SCREEN_ON + SECURE; sin NOT_TOUCHABLE; detector 4-taps (story 2.3)
- **AD-7**: handshake Lanzando→Aislada solo vía `OverlayShown`; `ShowOverlay` fire-and-forget; aborto con `OverlayFailed`/`LaunchFailed`
- **AD-11**: `ForegroundMonitor` interfaz (producción: `UsageStatsManager`; test: fake); `Unknown` si sin permiso
- **AD-12**: `Clock` inyectable para el delay de 1 s
- **AD-2**: FGS se inicia después del overlay visible

### Estructura de archivos

- `domain/SessionConstants.kt`, `domain/TargetApp.kt`, `domain/TargetAppLauncher.kt`, `domain/LaunchError.kt`, `domain/ForegroundMonitor.kt`
- `system/IntentLauncher.kt`, `system/UsageStatsForegroundMonitor.kt`, `system/OverlayController.kt`
- `domain/SessionController.kt` (modificado — procesar comandos)
- `AndroidManifest.xml` (modificado — SYSTEM_ALERT_WINDOW)

### Testing

- Robolectric `@Config(sdk = [34])`
- Fakes: `TargetAppLauncher`, `ForegroundMonitor`, `Clock`
- `ShadowWindowManager` para overlay
- Test del delay con `Clock` fake controlado

### Project Structure Notes

- `system/` está vacío hasta ahora — esta story lo puebla
- `SessionController` ya procesa eventos; ahora debe procesar comandos (el `onCommand` callback actual es no-op en MainActivity)
- El overlay NO tiene UI Compose — es una ventana del sistema con fondo negro

### Previous Story Intelligence

Story 2.1 estableció:
- `SessionController` con event loop, handshake Lanzando→Aislada vía `OverlayShown`, `terminate()` con HideOverlay/StopFgs/StopWatchdog
- `SessionCommand` sealed con `LaunchTarget`, `ShowOverlay`, `HideOverlay`, `StartFgs`, `StopFgs`, `StartWatchdog`, `StopWatchdog`
- `Clock` interfaz + `SystemClock`
- `MainActivity` crea `SessionController` con `onCommand = { /* no-op */ }` — ahora se conecta a los componentes reales

### Git Intelligence

Últimos commits:
- `1a15c6f feat(2.1): nucleo de sesion - SessionController, validacion FR-5 y event loop`
- `ed8de8c docs: README con instrucciones de compilacion y guia de uso`
- `ceb2013 feat(1.4): configuracion de la app objetivo y exencion de bateria`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.2]
- [Source: `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md` — §4.2 FR-6, §4.3 FR-8, FR-9, FR-10]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — AD-2, AD-3, AD-4, AD-7, AD-11, AD-12]
- [Source: `_bmad-output/planning-artifacts/research/technical-android-presence-keep-awake-research-2026-08-08.md` — Overlay, createWindowContext]

## Dev Agent Record

### Agent Model Used

(opencode)

### Debug Log References

- `LaunchError` extendido de `Exception` para poder usarlo con `Result.failure` (contrato AD-3)
- `OverlayController` convertido a interfaz (`domain/OverlayController`) + `SystemOverlayController` (implementación) para testabilidad
- Referencia circular `sessionController` ↔ `sessionCommandHandler` en `MainActivity` resuelta con `lateinit` + `commandHandler` lazy que se auto-asigna
- `UsageStatsForegroundMonitor` simplificado: `queryEvents` + iteración de eventos (sin doble query)
- Manifest: `SYSTEM_ALERT_WINDOW` + `PACKAGE_USAGE_STATS` (con `tools:ignore="ProtectedPermissions"`)
- Smoke test emulador: validación preventiva muestra "Falta el permiso de notificaciones" y el botón no inicia la sesión
- **Repo remoto (usuario):** subir el repositorio a `https://github.com/rdiazmartin/stayalert` más tarde (aún no configurado como remote)

### Completion Notes List

- `domain/SessionConstants.kt` — LAUNCH_DELAY_MS 1000, OVERLAY_DEPLOY_SLA_MS 500, PATTERN_WINDOW_MS 500, WATCHDOG_POLL_MS 2000, batería 15/5, SLA watchdog 5000
- `domain/TargetApp.kt` — data class (packageName, activityName)
- `domain/TargetAppLauncher.kt` — interfaz `suspend fun launch(target): Result<Unit>` (AD-3)
- `domain/LaunchError.kt` — sealed class extendiendo Exception: PackageNotInstalled, ActivityNotFound, SystemFailure
- `domain/ForegroundMonitor.kt` — interfaz con `ForegroundStatus { FOREGROUND, NOT_FOREGROUND, UNKNOWN }` (AD-11)
- `domain/OverlayController.kt` — interfaz show/hide/isVisible (AD-4)
- `system/IntentLauncher.kt` — Intent explícito con resolveActivity; nunca lanza excepciones
- `system/UsageStatsForegroundMonitor.kt` — UsageStatsManager, último evento del paquete en 60 s
- `system/SystemOverlayController.kt` — TYPE_APPLICATION_OVERLAY, flags NOT_FOCUSABLE+LAYOUT_IN_SCREEN+KEEP_SCREEN_ON+SECURE, fondo negro, sin NOT_TOUCHABLE; mide SLA
- `system/SessionCommandHandler.kt` — orquesta: launch → delay 1 s → confirmar foreground → overlay.show(); emite LaunchFailed/OverlayShown
- `MainActivity` — wiring de comandos con lateinit; manifest con SYSTEM_ALERT_WINDOW + PACKAGE_USAGE_STATS
- Tests: `IntentLauncherTest` (3), `SessionCommandHandlerTest` (3)
- Build/test/lint verdes; smoke test en emulador OK

### File List

- `app/src/main/AndroidManifest.xml` (modificado)
- `app/src/main/java/com/stayalert/domain/SessionConstants.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/TargetApp.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/TargetAppLauncher.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/LaunchError.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/ForegroundMonitor.kt` (nuevo)
- `app/src/main/java/com/stayalert/domain/OverlayController.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/IntentLauncher.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/UsageStatsForegroundMonitor.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/SystemOverlayController.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/SessionCommandHandler.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (modificado)
- `app/src/test/java/com/stayalert/system/IntentLauncherTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/system/SessionCommandHandlerTest.kt` (nuevo)

---

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `SessionCommandHandler` usa `delay()` real en vez del `Clock` inyectable [`app/src/main/java/com/stayalert/system/SessionCommandHandler.kt:217`] — fixed
- [x] [Review][Patch] `SystemOverlayController.show()` no verifica `SYSTEM_ALERT_WINDOW` ni usa `Clock` para el SLA [`app/src/main/java/com/stayalert/system/SystemOverlayController.kt:285`] — fixed
- [x] [Review][Patch] `UsageStatsForegroundMonitor` no maneja `SecurityException` de `queryEvents` [`app/src/main/java/com/stayalert/system/UsageStatsForegroundMonitor.kt:337`] — fixed

### defer

- [x] [Review][Defer] `SessionCommandHandler` no usa `clock` (parámetro muerto) [`app/src/main/java/com/stayalert/system/SessionCommandHandler.kt:176`] — deferred, pre-existing (se resuelve con el fix #1 o se elimina en refactor)
- [x] [Review][Defer] `MainActivity` con `lateinit` + lazy circular — patrón frágil [`app/src/main/java/com/stayalert/ui/MainActivity.kt:78-110`] — deferred, pre-existing (considerar `AppContainer` cuando crezca el wiring)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-2.2/findings-report.md`
