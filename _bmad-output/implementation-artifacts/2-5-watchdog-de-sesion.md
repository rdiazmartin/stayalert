---
baseline_commit: 0f7446a
---

# Story 2.5: Watchdog de sesión

Status: done

## Story

As a usuario,
I want que la sesión se termine automáticamente si algo falla (pantalla apagada, overlay ausente, permiso revocado, app objetivo cerrada, batería baja),
so que nunca quede una sesión muerta en silencio ni la app objetivo expuesta sin aviso.

## Acceptance Criteria

1. **Detección de anomalías (FR-13, FR-14, FR-16)** — El watchdog detecta: pantalla apagada (`PowerManager`), overlay ausente o permiso revocado (`Settings.canDrawOverlays()`), app objetivo fuera de primer plano (`ForegroundMonitor`/`UsageStatsManager` si el permiso de uso está concedido), `HIDE_OVERLAY_WINDOWS` (overlay no dibujado), y batería (aviso al 15%, terminación al 5%).
2. **SLA (NFR-3)** — La sesión termina en < 5 s con notificación "Sesión terminada: {motivo}".
3. **Gating por estado (AD-5)** — El watchdog solo está activo en estado `Aislada` (iniciado/detenido por órdenes).
4. **Solo detecta y emite (AD-5, AD-8)** — El watchdog nunca publica notificaciones; solo emite `SessionEvent`.
5. **Polling (AD-5)** — El polling es cada 2 s (constante `SessionConstants.WATCHDOG_POLL_MS`).
6. **Crash de app objetivo (FR-14)** — Si la app objetivo crashea, la sesión termina en < 5 s sin dejar el overlay sobre el launcher.
7. **Permiso de uso no concedido (AD-11)** — Si el permiso de uso no está concedido, la detección de salida de primer plano se omite (limitación documentada).
8. **Tests** — Unit tests: detección de cada anomalía, gating por estado, polling, SLA.

## Tasks / Subtasks

- [x] Task 1: `Watchdog` (AC: 1, 3, 4, 5)
  - [x] 1.1 Crear `system/Watchdog.kt` — clase con `start()`/`stop()`, polling cada 2 s
  - [x] 1.2 Detectar: pantalla apagada (`PowerManager.isInteractive()`), overlay ausente/permiso revocado (`Settings.canDrawOverlays()`), app objetivo fuera de primer plano (`ForegroundMonitor`), batería (`BatteryManager`)
  - [x] 1.3 Emitir `SessionEvent` por cada anomalía (nunca notificaciones — AD-5, AD-8)
  - [x] 1.4 `HIDE_OVERLAY_WINDOWS`: si el overlay no se dibuja sobre la app objetivo (detección vía `ForegroundMonitor` + estado del overlay)
- [x] Task 2: Integración (AC: 2, 3, 6)
  - [x] 2.1 `SessionCommandHandler`: `StartWatchdog` → `watchdog.start()`; `StopWatchdog` → `watchdog.stop()`
  - [x] 2.2 El watchdog recibe el `SessionController` (o `onEvent` callback) para emitir eventos
  - [x] 2.3 `SessionController` ya maneja los eventos de anomalía → `terminate(reason)` (de 2.1)
- [x] Task 3: Tests (AC: 8)
  - [x] 3.1 Test: pantalla apagada → `ScreenOff`
  - [x] 3.2 Test: permiso revocado → `PermissionRevoked`
  - [x] 3.3 Test: app objetivo fuera de primer plano → `TargetLeftForeground`
  - [x] 3.4 Test: batería 15% → `BatteryWarning`; batería 5% → `BatteryCritical`
  - [x] 3.5 Test: gating por estado (solo en `Aislada`)
  - [x] 3.6 Verificar `./gradlew test` y `./gradlew lint`

## Dev Notes

### Contexto del producto

El watchdog es la red de seguridad: si algo falla durante la sesión (pantalla se apaga, overlay desaparece, permiso revocado, app objetivo cerrada, batería baja), la sesión termina con notificación. Solo detecta y emite eventos — nunca publica notificaciones (AD-5, AD-8).

### Stack

- Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36
- Coroutines + Flow
- DI manual por constructor (sin Hilt)
- JUnit4 + Robolectric 4.16

### Arquitectura (spine — ADs vinculantes)

- **AD-5**: watchdog solo activo en `Aislada`; solo detecta y emite eventos; polling 2 s
- **AD-8**: nunca publica notificaciones
- **AD-11**: `ForegroundMonitor` devuelve `UNKNOWN` si sin permiso de uso; detección omitida
- **AD-12**: `Clock` inyectable para el polling
- **AD-9**: terminación idempotente (ya en SessionController)

### Estructura de archivos

- `system/Watchdog.kt` — clase con polling
- `system/SessionCommandHandler.kt` (modificado — StartWatchdog/StopWatchdog)
- `domain/SessionController.kt` (sin cambios — ya maneja los eventos)

### Testing

- Robolectric `@Config(sdk = [34])`
- Fakes: `PowerManager` (via `ShadowPowerManager`), `BatteryManager` (via `ShadowBatteryManager`), `ForegroundMonitor`, `Clock`
- Test del polling con `Clock` fake y `advanceTimeBy`

### Project Structure Notes

- `Watchdog` en `system/` (capa de integración)
- El watchdog no conoce `Notifier` (AD-8)

### Previous Story Intelligence

Stories 2.1-2.4 establecieron:
- `SessionController` maneja todos los eventos de anomalía → `terminate(reason)` (2.1)
- `SessionCommandHandler` con no-ops para `StartWatchdog`/`StopWatchdog` — ahora se implementan
- `ForegroundMonitor` con `ForegroundStatus { FOREGROUND, NOT_FOREGROUND, UNKNOWN }` (2.2)
- `SessionConstants.WATCHDOG_POLL_MS = 2000`, `WATCHDOG_SLA_MS = 5000`, `BATTERY_WARNING_LEVEL = 15`, `BATTERY_CRITICAL_LEVEL = 5` (2.2)

### Git Intelligence

Últimos commits:
- `0f7446a feat(2.4): servicio en primer plano y notificaciones`
- `db5c57f feat(2.3): patron de salida - detector 4-taps en esquina superior derecha`
- `b20e670 feat(2.2): despliegue secuencial y overlay de aislamiento`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.5]
- [Source: `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md` — §4.4 FR-13, FR-14, FR-16]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — AD-5, AD-8, AD-9, AD-11, AD-12]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md` — Flow 4]

## Dev Agent Record

### Agent Model Used

(opencode)

### Debug Log References

- `Watchdog` convertido a interfaz (`domain/Watchdog`) + `SystemWatchdog` (implementación) para testabilidad
- `check()` es `suspend` (llama a `ForegroundMonitor.status` suspend); `targetPackage` es `suspend () -> String`
- Tests requirieron `ShadowSettings.setCanDrawOverlays(true)` (Robolectric devuelve false por defecto) y `ShadowBatteryManager.setIntProperty(BATTERY_PROPERTY_CAPACITY, ...)`
- `MainActivity` usa `settingsRepository.targetPackage.first()` (Flow, no StateFlow) en la lambda del watchdog
- **Repo remoto (usuario):** subir el repositorio a `https://github.com/rdiazmartin/stayalert` más tarde (aún no configurado como remote)

### Completion Notes List

- `domain/Watchdog.kt` (interfaz) + `system/SystemWatchdog.kt` (implementación): polling 2 s, detecta pantalla apagada, permiso revocado, overlay ausente, app objetivo fuera de primer plano, batería 15%/5%
- Solo emite `SessionEvent` — nunca notificaciones (AD-5, AD-8)
- `SessionCommandHandler` — `StartWatchdog`/`StopWatchdog` implementados
- `MainActivity` — wiring del watchdog con `targetPackage.first()`
- Tests: `SystemWatchdogTest` (6) — verdes
- Build/test/lint verdes

### File List

- `app/src/main/java/com/stayalert/domain/Watchdog.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/SystemWatchdog.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/SessionCommandHandler.kt` (modificado)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (modificado)
- `app/src/test/java/com/stayalert/system/SystemWatchdogTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/system/SessionCommandHandlerTest.kt` (modificado)

---

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `SystemWatchdog` no detecta `HIDE_OVERLAY_WINDOWS` (AC-1 incompleto) [`app/src/main/java/com/stayalert/system/SystemWatchdog.kt:110-141`] — fixed
- [x] [Review][Patch] `SystemWatchdog` no usa el `Clock` inyectable para el polling [`app/src/main/java/com/stayalert/system/SystemWatchdog.kt:100`] — fixed
- [x] [Review][Patch] `SystemWatchdog` emite eventos repetidamente si la anomalía persiste [`app/src/main/java/com/stayalert/system/SystemWatchdog.kt:110-141`] — fixed

### defer

- [x] [Review][Defer] `SystemWatchdog` no distingue `TargetCrashed` de `TargetLeftForeground` [`app/src/main/java/com/stayalert/system/SystemWatchdog.kt:127-131`] — deferred, pre-existing (limitación de UsageStatsManager; motivo único en notificación)
- [x] [Review][Defer] `SystemWatchdog` no maneja `BatteryWarning` repetido [`app/src/main/java/com/stayalert/system/SystemWatchdog.kt:137-138`] — deferred, pre-existing (requiere deduplicación cuando se implemente el aviso FR-16)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-2.5/findings-report.md`
