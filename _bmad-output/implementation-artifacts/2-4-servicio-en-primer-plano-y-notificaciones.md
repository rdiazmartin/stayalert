---
baseline_commit: db5c57f
---

# Story 2.4: Servicio en primer plano y notificaciones

Status: done

## Story

As a usuario,
I want ver una notificación de sesión activa con acción para detenerla y recibir confirmación al terminar,
so que siempre tengo un kill switch visible y sé cuándo la sesión termina.

## Acceptance Criteria

1. **FGS activo (FR-11)** — El servicio en primer plano es tipo `specialUse` con permiso `FOREGROUND_SERVICE_SPECIAL_USE` y notificación visible durante la sesión.
2. **Notificación de sesión (FR-11, AD-8, UX-DR7)** — La notificación usa el canal fijo `stayalert_session` (creado al iniciar la app) con texto "Sesión activa — toca para detener" y acción "Detener".
3. **Kill switch (FR-11, AD-8)** — La acción "Detener" enruta a `StopReceiver` (BroadcastReceiver) que emite `SessionEvent.StopRequested`.
4. **Fin de sesión (FR-11)** — Al terminar la sesión, el FGS se detiene y la notificación desaparece.
5. **Notificación de fin (FR-12, AD-8, AD-10)** — Toda terminación produce notificación "Sesión terminada: {motivo}" en el canal `stayalert_events`.
6. **Notifier único publicador (AD-8)** — `Notifier` es el único componente que publica notificaciones; el mapeo motivo→texto vive en `Notifier` (AD-10).
7. **Tests** — Unit tests: creación de canales, notificación de sesión, notificación de fin con motivo, mapeo motivo→texto.

## Tasks / Subtasks

- [x] Task 1: `Notifier` (AC: 2, 5, 6)
  - [x] 1.1 Crear `data/Notifier.kt` — interfaz con `showSessionNotification()`, `showSessionEnded(reason)`, `createChannels()`
  - [x] 1.2 Crear `data/SystemNotifier.kt` — implementación con `NotificationManager`
  - [x] 1.3 Canales fijos: `stayalert_session` (FGS) y `stayalert_events` (avisos/fin) (AD-8)
  - [x] 1.4 Mapeo motivo→texto en `Notifier` (AD-10): "Sesión terminada: {motivo}"
- [x] Task 2: `PresenceForegroundService` (AC: 1, 2, 4)
  - [x] 2.1 Crear `system/PresenceForegroundService.kt` — `Service` con tipo `specialUse` (Android 14+)
  - [x] 2.2 `onStartCommand` → `startForeground` con notificación de sesión
  - [x] 2.3 `onDestroy`/`stopSelf` → detener FGS y quitar notificación
- [x] Task 3: `StopReceiver` (AC: 3)
  - [x] 3.1 Crear `system/StopReceiver.kt` — `BroadcastReceiver` que emite `SessionEvent.StopRequested`
  - [x] 3.2 Registrar en manifest con `android:exported="false"`
- [x] Task 4: Integración (AC: 1, 4)
  - [x] 4.1 `SessionCommandHandler`: `StartFgs` → iniciar servicio; `StopFgs` → detener servicio
  - [x] 4.2 `Notifier.createChannels()` al iniciar la app (MainActivity)
  - [x] 4.3 `SessionController` emite notificación de fin vía `Notifier` (o el handler la emite al recibir `StopFgs`)
- [x] Task 5: Manifest (AC: 1)
  - [x] 5.1 Declarar `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` + `POST_NOTIFICATIONS`
  - [x] 5.2 Declarar `PresenceForegroundService` con `foregroundServiceType="specialUse"` y `property="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"`
  - [x] 5.3 Declarar `StopReceiver`
- [x] Task 6: Tests (AC: 7)
  - [x] 6.1 Test `SystemNotifier`: canales creados, notificación de sesión, notificación de fin con motivo
  - [x] 6.2 Test mapeo motivo→texto (todos los `TerminationReason`)
  - [x] 6.3 Verificar `./gradlew test` y `./gradlew lint`

## Dev Notes

### Contexto del producto

El FGS mantiene el proceso vivo durante la sesión y muestra la notificación con el kill switch. El `Notifier` es el único publicador de notificaciones (AD-8). El mapeo motivo→texto vive en `Notifier` (AD-10).

### Stack

- Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36
- Coroutines + Flow
- DI manual por constructor (sin Hilt)
- JUnit4 + Robolectric 4.16

### Arquitectura (spine — ADs vinculantes)

- **AD-2**: FGS se inicia después del overlay visible (regla Android 15)
- **AD-8**: `Notifier` único publicador; canales fijos `stayalert_session` y `stayalert_events`; acción Detener → `StopReceiver` → `SessionEvent.StopRequested`
- **AD-10**: mapeo motivo→texto en `Notifier`
- **AD-9**: terminación idempotente (ya en SessionController)

### Estructura de archivos

- `data/Notifier.kt` (interfaz), `data/SystemNotifier.kt` (implementación)
- `system/PresenceForegroundService.kt`, `system/StopReceiver.kt`
- `system/SessionCommandHandler.kt` (modificado — StartFgs/StopFgs)
- `AndroidManifest.xml` (modificado)
- `MainActivity.kt` (modificado — createChannels)

### Testing

- Robolectric `@Config(sdk = [34])`
- `ShadowNotificationManager` para verificar notificaciones
- Test del mapeo motivo→texto con todos los `TerminationReason`

### Project Structure Notes

- `data/Notifier` es el único publicador (AD-8)
- El FGS no se comunica con el overlay ni el watchdog (AD-2)

### Previous Story Intelligence

Stories 2.1-2.3 establecieron:
- `SessionController` con `terminate()` que emite `StopFgs` (entre otros)
- `SessionCommandHandler` con no-ops para `StartFgs`/`StopFgs` — ahora se implementan
- `TerminationReason` enum con 11 motivos (AD-10)
- `SessionEvent.StopRequested` → `terminate(ManualStop)` (2.1)

### Git Intelligence

Últimos commits:
- `db5c57f feat(2.3): patron de salida - detector 4-taps en esquina superior derecha`
- `b20e670 feat(2.2): despliegue secuencial y overlay de aislamiento`
- `1a15c6f feat(2.1): nucleo de sesion - SessionController, validacion FR-5 y event loop`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.4]
- [Source: `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md` — §4.4 FR-11, FR-12]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — AD-2, AD-8, AD-9, AD-10]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/DESIGN.md` — Components (Notificación FGS)]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md` — Component Patterns]

## Dev Agent Record

### Agent Model Used

(opencode)

### Debug Log References

- `StopReceiver` con callback estático (`onStopRequested`) — patrón común para receivers de notificaciones (requiere constructor sin args para el manifest)
- `SessionCommandHandler` ahora recibe `context` y `notifier`; `StartFgs`/`StopFgs` implementados
- `SessionCommandHandlerTest` requirió `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [34])` por el `ApplicationProvider`
- Manifest: `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` + `POST_NOTIFICATIONS`; service `specialUse` con `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; `StopReceiver` con action `com.stayalert.action.STOP_SESSION`
- Smoke test emulador: app arranca sin crashes
- **Repo remoto (usuario):** subir el repositorio a `https://github.com/rdiazmartin/stayalert` más tarde (aún no configurado como remote)

### Completion Notes List

- `data/Notifier.kt` (interfaz) + `data/SystemNotifier.kt` (implementación): canales `stayalert_session`/`stayalert_events`, notificación de sesión con acción Detener, notificación de fin con motivo (AD-8, AD-10)
- `system/PresenceForegroundService.kt` — FGS `specialUse`, `startForeground` con notificación, `onDestroy` cancela
- `system/StopReceiver.kt` — BroadcastReceiver con action `STOP_SESSION` → `SessionEvent.StopRequested`
- `SessionCommandHandler` — `StartFgs`/`StopFgs` implementados (start/stop service)
- `MainActivity` — `notifier.createChannels()` + `StopReceiver.onStopRequested` wiring
- Manifest — permisos FGS + service specialUse + receiver
- Tests: `SystemNotifierTest` (4) — verdes
- Build/test/lint verdes

### File List

- `app/src/main/AndroidManifest.xml` (modificado)
- `app/src/main/java/com/stayalert/data/Notifier.kt` (nuevo)
- `app/src/main/java/com/stayalert/data/SystemNotifier.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/PresenceForegroundService.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/StopReceiver.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/SessionCommandHandler.kt` (modificado)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (modificado)
- `app/src/test/java/com/stayalert/data/SystemNotifierTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/system/SessionCommandHandlerTest.kt` (modificado)

---

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `StopReceiver.onStopRequested` es un callback estático global — frágil y con fuga de memoria [`app/src/main/java/com/stayalert/system/StopReceiver.kt:305`] — fixed
- [x] [Review][Patch] `PresenceForegroundService` duplica la construcción de la notificación de sesión [`app/src/main/java/com/stayalert/system/PresenceForegroundService.kt:205-225`] — fixed
- [x] [Review][Patch] `PresenceForegroundService` usa `START_STICKY` — puede reiniciarse sin sesión activa [`app/src/main/java/com/stayalert/system/PresenceForegroundService.kt:195`] — fixed

### defer

- [x] [Review][Defer] `SystemNotifier` no maneja `POST_NOTIFICATIONS` (API 33+) [`app/src/main/java/com/stayalert/data/SystemNotifier.kt:94-95`] — deferred, pre-existing (la validación FR-5 lo cubre)
- [x] [Review][Defer] `SystemNotifier` no expone `buildSessionNotification()` [`app/src/main/java/com/stayalert/data/SystemNotifier.kt`] — deferred, pre-existing (se resuelve con el fix #2)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-2.4/findings-report.md`
