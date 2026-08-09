# Story 3.1: Kill switch fiable

Status: review

---
baseline_commit: a10e8aa40bdd83a477c07910421176d5909ed254
---

## Story

As a usuario,
I want que la acción "Detener" de la notificación termine la sesión siempre, esté la app en primer plano o en background,
so that nunca me quede atrapado en una sesión activa con un kill switch muerto.

## Acceptance Criteria

1. **Stop en background sin abrir la app (A2):** Dado una sesión activa con la notificación "Sesión activa — toca para detener" visible, cuando el usuario pulsa "Detener" con la app en background (MainActivity destruida o en pausa), entonces la sesión termina limpiamente (overlay oculto, FGS detenido, watchdog parado) y NO se abre `MainActivity`.
   - `StopReceiver` debe emitir `SessionEvent.StopRequested` directamente vía `AppContainer` (`(context.applicationContext as StayAlertApplication).container.sessionController.emit(StopRequested)`), sin `startActivity`.
   - `StopReceiver.onReceive` ya no referencia `MainActivity` (eliminar import y lógica de forward).
2. **Servicio muerto con sesión activa (A1):** Dado una sesión en `Aislada`, cuando el sistema o la app detiene `PresenceForegroundService`, entonces la sesión se termina (overlay oculto + watchdog parado + notificación cancelada) en lugar de dejar un kill switch fantasma.
   - En `PresenceForegroundService.onDestroy()`: si `container.sessionController.state.value` NO es `Inactiva` ni `Deteniendo`, emitir `SessionEvent.StopRequested` antes de cancelar la notificación.
   - La terminación sigue siendo idempotente (AD-9): en `Deteniendo`/`Inactiva` no se re-emite.
3. **Kill switch funciona con servicio muerto:** Dado un servicio eliminado con la sesión activa, cuando el usuario pulsa "Detener" en la notificación residual (si aún se ve), entonces la sesión termina (vía A2, el receiver no depende del servicio).
4. **Notificación de sesión activa no queda colgada:** Dado el fin de sesión (cualquier motivo), cuando la sesión transita a `Inactiva`, entonces la notificación de sesión activa desaparece (`cancelSessionNotification` en `onDestroy` del servicio, ya existente — mantener).
5. **AD-8 se mantiene:** `StopReceiver` sigue siendo el destino de la acción "Detener" de la notificación (PendingIntent a BroadcastReceiver) y emite `StopRequested`; `Notifier` sigue siendo el único publicador.
6. **Tests instrumentados (JVM/Robolectric + androidTest):**
   - Robolectric (`@Config(sdk = [34])` — lección de project-context): `StopReceiver.onReceive` con sesión activa → estado `Inactiva`; sin sesión → no-op.
   - Robolectric: `PresenceForegroundService.onDestroy` con sesión en `Aislada` → `StopRequested` emitido (estado `Inactiva`); con sesión `Inactiva` → sin re-emisión (idempotencia).
   - androidTest: `StopReceiver` con la app en background termina la sesión sin abrir la activity (adaptar/ampliar `StopReceiverTest.kt` existente — hoy solo verifica que no lanza excepción).

## Tasks / Subtasks

- [x] Task 1: `StopReceiver` emite `StopRequested` vía AppContainer (AC: 1, 3)
  - [x] Subtask 1.1: Obtener container de `context.applicationContext as StayAlertApplication`
  - [x] Subtask 1.2: `sessionController.emit(SessionEvent.StopRequested)` en lugar de `startActivity`
  - [x] Subtask 1.3: Eliminar import y forward a `MainActivity`
- [x] Task 2: `PresenceForegroundService.onDestroy` termina sesión activa huérfana (AC: 2, 4)
  - [x] Subtask 2.1: Acceder al container (mismo patrón que Task 1)
  - [x] Subtask 2.2: Si `state.value !is Inactiva && !is Deteniendo` → `emit(StopRequested)`
  - [x] Subtask 2.3: Mantener `cancelSessionNotification()` existente
- [x] Task 3: Tests Robolectric (AC: 6)
  - [x] Subtask 3.1: Test `StopReceiver` con sesión activa → Inactiva
  - [x] Subtask 3.2: Test `StopReceiver` sin sesión → no-op
  - [x] Subtask 3.3: Test `onDestroy` con sesión Aislada → StopRequested
  - [x] Subtask 3.4: Test `onDestroy` con sesión Inactiva → idempotente
- [x] Task 4: Ampliar test instrumentado `StopReceiverTest` (AC: 6)
  - [x] Subtask 4.1: Verificar que NO se lanza la activity (solo emisión de evento)
- [x] Task 5: Verificar suite completa (`./gradlew testDebugUnitTest` + lint)

## Dev Notes

### Estado actual del código (leer antes de modificar)

- `app/src/main/java/com/stayalert/system/StopReceiver.kt` — hoy hace `startActivity(MainActivity)` con `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP`; depende del ciclo de vida de `MainActivity` (A2). **Eliminar este comportamiento.**
- `app/src/main/java/com/stayalert/system/PresenceForegroundService.kt` — `onStartCommand` devuelve `START_NOT_STICKY` (A1: si el sistema mata el servicio no se reinicia; la notificación puede quedar colgada sin servicio detrás). `onDestroy` ya hace `cancelSessionNotification()`. **Añadir terminación de sesión huérfana en `onDestroy`.**
- `app/src/main/java/com/stayalert/AppContainer.kt` — `sessionController` es singleton de proceso accesible desde cualquier contexto vía `(applicationContext as StayAlertApplication).container` (patrón ya usado en `MainActivity`).
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` — `onNewIntent` maneja `ACTION_STOP_SESSION` (se mantiene, sin cambios; cubre el caso de activity ya abierta).
- `app/src/main/java/com/stayalert/domain/SessionController.kt` — event loop con try-catch (fix de review anterior); `emit(StopRequested)` en `Aislada` → `terminate(ManualStop)`, idempotente.

### Arquitectura (guardrails obligatorios)

- **AD-8** (`ARCHITECTURE-SPINE.md#AD-8`): la acción "Detener" de la notificación FGS enruta a un `BroadcastReceiver` que emite `SessionEvent.StopRequested`. El receiver es `StopReceiver` — mantenerlo como destino del PendingIntent (`SystemNotifier.buildSessionNotification`).
- **AD-2**: el FGS reporta el estado de la sesión; la sesión vive en el proceso (AppContainer), NO en el servicio. El servicio es un accesorio: si muere, la sesión debe limpiarse o la notificación reaparecer — en esta story elegimos **terminar la sesión** (simplicidad, decisión alineada con C1 de la auditoría: sin kill switch fantasma).
- **AD-9**: terminación idempotente — `emit(StopRequested)` en `Deteniendo`/`Inactiva` es no-op (el guard de Task 2 es defensivo, la idempotencia ya cubre).
- **AD-10**: `StopRequested` → `TerminationReason.ManualStop` → texto "detención manual" (sin cambios).
- **Convención errores**: fallos de componentes SO → `SessionEvent`, nunca excepciones cruzando capas.
- **Notificaciones**: único publicador `Notifier` (AD-8); el FGS usa `SystemNotifier` directamente hoy (deuda conocida, no ampliar en esta story).

### Testing (lecciones de project-context.md)

- `@Config(sdk = [34])` en TODOS los tests Robolectric (targetSdk 36 no soportado).
- Usar fakes para el container cuando haga falta (o instanciar `AppContainer` con contexto real en Robolectric — verificar que `DataStore` funciona en Robolectric; si falla, inyectar el controller vía constructor o `@VisibleForTesting`).
- No usar `SystemClock` real en tests; el `Clock` inyectable (AD-12) no aplica a esta story (no hay delays).
- Evitar aserciones débiles (`assertTrue(true)`); verificar el ESTADO real (`sessionController.state.value`).

### Riesgo de regresión

- `MainActivity.onNewIntent` sigue existiendo: NO romper la terminación por acción de la notificación cuando la activity está en primer plano (ambas vías deben coexistir; `StopRequested` es idempotente).
- `StopReceiverTest.kt` existente (androidTest) verifica el reenvío a MainActivity — debe actualizarse al nuevo contrato.
- El E2E (`SessionE2ETest`) no cubre el kill switch de notificación — no es necesario ampliarlo en esta story (la AC 6 cubre instrumentados).

### Project Structure Notes

- Sin archivos nuevos de producción; solo se modifican `system/StopReceiver.kt` y `system/PresenceForegroundService.kt` (+ tests).
- Patrón de acceso al container: `(context.applicationContext as StayAlertApplication).container` (ya establecido en `MainActivity`).

### References

- [Source: ARCHITECTURE-SPINE.md#AD-8] — acción Detener → BroadcastReceiver → `StopRequested`
- [Source: ARCHITECTURE-SPINE.md#AD-9] — terminación idempotente
- [Source: auditoria-pre-distribucion-2026-08-09.md#A1] — FGS `START_NOT_STICKY` con notificación colgada
- [Source: auditoria-pre-distribucion-2026-08-09.md#A2] — `StopReceiver` con callback estático frágil
- [Source: project-context.md#Testing] — `@Config(sdk = [34])`, fakes, sin aserciones débiles

## Dev Agent Record

### Agent Model Used

deepseek-v4-flash:0731 (implementación story 3.1)

### Debug Log References

- Build: `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (60 tests, 0 fallos)
- Lint: `./gradlew lintDebug` → BUILD SUCCESSFUL (21 warnings preexistentes, 0 en archivos modificados)
- Release: `./gradlew assembleRelease` → BUILD SUCCESSFUL

### Completion Notes List

- `StopReceiver` ya no reenvía a `MainActivity` (eliminado `startActivity`): ahora emite `SessionEvent.StopRequested` directamente vía `AppContainer` (A2 resuelto).
- `PresenceForegroundService.onDestroy` termina la sesión activa huérfana: si el estado es `Aislada`/`Lanzando`, emite `SessionEvent.ServiceKilled` antes de cancelar la notificación (A1 resuelto: sin kill switch fantasma).
- Seam de test: `sessionControllerProvider` (internal var) en ambos componentes para inyectar fakes sin depender del Application real.
- Hallazgos de code review aplicados: `TerminationReason.ServiceKilled` + evento `SessionEvent.ServiceKilled` (D1), handlers huérfanos de `ACTION_STOP_SESSION` eliminados de MainActivity (P1), androidTest sin tautología (P2), logs cuando el provider es null (P3), guard en whitelist (P4), tests de Lanzando/sesión ya terminada (P5/P6), imports limpios (P7).
- Hallazgo preexistente registrado en deferred-work: `SessionE2ETest` no compila en compileSdk 37 (`AppOpsManager.setMode` eliminado en API 37).

### File List

- [M] `app/src/main/java/com/stayalert/system/StopReceiver.kt`
- [M] `app/src/main/java/com/stayalert/system/PresenceForegroundService.kt`
- [M] `app/src/main/java/com/stayalert/domain/SessionEvent.kt` — `ServiceKilled`
- [M] `app/src/main/java/com/stayalert/domain/TerminationReason.kt` — `ServiceKilled`
- [M] `app/src/main/java/com/stayalert/domain/SessionController.kt` — manejo de `ServiceKilled`
- [M] `app/src/main/java/com/stayalert/data/SystemNotifier.kt` — texto "servicio eliminado por el sistema"
- [M] `app/src/main/java/com/stayalert/ui/MainActivity.kt` — handlers huérfanos eliminados
- [A] `app/src/test/java/com/stayalert/system/StopReceiverTest.kt` (nuevo, Robolectric, 8 tests)
- [M] `app/src/androidTest/java/com/stayalert/StopReceiverTest.kt`

## Change Log

- 2026-08-09: Implementación story 3.1 — kill switch fiable (A1 + A2)
- 2026-08-09: Hallazgos de code review aplicados (D1 ServiceKilled, P1-P7)
