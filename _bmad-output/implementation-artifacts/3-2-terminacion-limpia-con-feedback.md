# Story 3.2: Terminación limpia con feedback

Status: review

---
baseline_commit: 2bbc0c8
---

## Story

As a usuario,
I want que al terminar la sesión el overlay se oculte siempre y recibir una notificación con el motivo,
so that nunca quede un overlay visible sin sesión y siempre sepa por qué terminó.

## Acceptance Criteria

1. **Transición atómica (A4):** Dado una sesión en `Aislada` o `Lanzando`, cuando se ejecuta `terminate(reason)`, entonces el estado pasa a `Deteniendo` y la transición a `Inactiva` ocurre SOLO al confirmar que el overlay se ocultó (evento `OverlayHidden`).
2. **Fallo de hide reflejado (A4):** Dado un fallo en `overlayController.hide()`, cuando se ejecuta `terminate`, entonces se emite `OverlayHideFailed(cause)`, el estado transita a `Inactiva` (sin deadlock) con log del fallo, y la sesión no queda congelada.
3. **Feedback de fin de sesión (FR-12):** Dado cualquier terminación (patrón, manual, watchdog, etc.), cuando el overlay se ocultó, entonces `notifier.showSessionEnded(reason)` se publica en el canal `stayalert_events` con el motivo correcto.
4. **Notificación de sesión activa desaparece (FR-11):** al terminar, el FGS se detiene (`StopFgs`) y la notificación de sesión activa se cancela (ya implementado vía `onDestroy` del servicio — mantener).
5. **Idempotencia (AD-9):** eventos de terminación en `Deteniendo`/`Inactiva` son no-ops.
6. **Tests unitarios (FR-12):** `terminate` espera el hide (fake overlay con hide suspendible), fallo de hide → estado consistente, `showSessionEnded` invocado con el motivo correcto.

## Tasks / Subtasks

- [x] Task 1: Contrato de overlay suspendible (AC: 1, 2)
  - [x] Subtask 1.1: `OverlayController.hide()` → `suspend fun hide()`
  - [x] Subtask 1.2: `SystemOverlayController.hide()` propagando el fallo (finally limpia refs)
  - [x] Subtask 1.3: `SessionCommandHandler.HideOverlay` → corrutina que emite `OverlayHidden`/`OverlayHideFailed`
- [x] Task 2: `SessionController` con notifier y confirmación de hide (AC: 1, 3)
  - [x] Subtask 2.1: Constructor recibe `Notifier` (AD-8: controller ordena publicar)
  - [x] Subtask 2.2: `terminate()` deja de transitar a `Inactiva`; emite `HideOverlay` + `StopFgs` + `StopWatchdog`
  - [x] Subtask 2.3: `handleDeteniendo` con `OverlayHidden` → `Inactiva` + `showSessionEnded`
  - [x] Subtask 2.4: `handleDeteniendo` con `OverlayHideFailed` → `Inactiva` + log + `showSessionEnded`
  - [x] Subtask 2.5: `OverlayHidden`/`OverlayHideFailed` como no-op en `Inactiva`/`Lanzando`/`Aislada`
- [x] Task 3: Wiring en `AppContainer` (AC: 3)
  - [x] Subtask 3.1: Pasar `notifier` a `SessionController`
- [x] Task 4: Tests (AC: 6)
  - [x] Subtask 4.1: Actualizar fakes (`FakeOverlay.hide` suspend, harness con fake notifier)
  - [x] Subtask 4.2: Test: `terminate` espera `OverlayHidden` antes de `Inactiva`
  - [x] Subtask 4.3: Test: `OverlayHideFailed` → `Inactiva` con motivo
  - [x] Subtask 4.4: Test: `showSessionEnded` con motivo correcto (Pattern/ManualStop)
  - [x] Subtask 4.5: Actualizar tests existentes que asumen `Inactiva` inmediata (SessionControllerTest, StopReceiverTest)
- [x] Task 5: Suite completa + lint (AC: todas)

## Dev Notes

### Estado actual

- `SessionController.terminate()` (`domain/SessionController.kt:150-158`) transita a `Inactiva` inmediatamente tras emitir `HideOverlay` (fire-and-forget) — hallazgo A4.
- `SystemOverlayController.hide()` (`system/SystemOverlayController.kt:115-129`) lanza una corrutina interna; no hay confirmación de que el overlay se ocultó.
- `Notifier.showSessionEnded(reason)` existe pero NUNCA se llama desde producción — FR-12 sin implementar (hallazgo del code review del commit 571b7ae).
- `SessionCommandHandler` (`system/SessionCommandHandler.kt:38`) ejecuta `HideOverlay → overlayController.hide()`.

### Arquitectura (guardrails)

- **AD-7**: órdenes fire-and-forget; la confirmación llega como `SessionEvent` — `OverlayHidden`/`OverlayHideFailed` siguen este contrato (el hide NO bloquea síncronamente).
- **AD-8**: `SessionController` es el único que ordena publicar vía `Notifier` — por eso el controller recibe `Notifier` en el constructor.
- **AD-9**: terminación idempotente — en `Deteniendo` los eventos de terminación siguen siendo no-ops; el nuevo estado de espera NO rompe la idempotencia.
- **AD-10**: `showSessionEnded(reason)` recibe el `TerminationReason` ya mapeado en `SystemNotifier`.
- **Decisión de diseño (documentada):** si `hide()` falla, transitar igualmente a `Inactiva` (evitar sesión congelada — peor que un overlay residual), con log del fallo. La AC "el estado refleja el fallo" se satisface con el log + el motivo ya registrado.

### Testing (lecciones project-context)

- `@Config(sdk = [34])` en todos los tests Robolectric.
- Fake overlay con `hide()` suspendible para simular éxito/fallo.
- Fake notifier registrando llamadas `showSessionEnded(reason)`.
- Verificar estado REAL del controller, nunca `assertTrue(true)`.

### Riesgo de regresión

- Tests de 3.1 (`StopReceiverTest`) y `SessionControllerTest` asumen `Inactiva` inmediata tras `StopRequested`/`PatternDetected` — requieren emitir `OverlayHidden` en el flujo (el handler real lo emite; los tests con `onCommand = {}` deben hacerlo manualmente).
- El E2E real: el flujo completo (overlay visible → patrón → hide) debe seguir funcionando; `OverlayHidden` llega vía `SessionCommandHandler` real en producción.

### References

- [Source: ARCHITECTURE-SPINE.md#AD-7] — órdenes fire-and-forget + confirmación vía evento
- [Source: ARCHITECTURE-SPINE.md#AD-8] — controller ordena publicar vía Notifier
- [Source: auditoria-pre-distribucion-2026-08-09.md#A4] — terminate no espera el hide
- [Source: code review 571b7ae] — showSessionEnded sin wiring (FR-12)

## Dev Agent Record

### Agent Model Used

deepseek-v4-flash:0731 (creación story 3.2)

### Debug Log References

- Build: `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (65 tests, 0 fallos)
- Lint + release: `./gradlew lintDebug assembleRelease` → BUILD SUCCESSFUL

### Completion Notes List

- `OverlayController.hide()` ahora es `suspend` y propaga fallos; `SessionCommandHandler` emite `OverlayHidden`/`OverlayHideFailed` (contrato AD-7).
- `SessionController` recibe `Notifier` (AD-8) y `terminate()` transita a `Deteniendo` esperando la confirmación del hide; `OverlayHidden`/`OverlayHideFailed` completan la terminación y publican `showSessionEnded(reason)` (FR-12 implementado).
- Hallazgo de debug: `android.util.Log` dentro del event loop lanza "Stub!" en tests JVM puros y el try-catch lo tragaba, dejando el estado en `Deteniendo`. Se envolvió el log en `runCatching` para mantener el log en producción sin romper tests.
- Decisión de diseño: si `hide()` falla se transita igualmente a `Inactiva` (sin sesión congelada) con log del fallo — documentado en Dev Notes.

### File List

- [M] `app/src/main/java/com/stayalert/domain/OverlayController.kt` — hide suspend
- [M] `app/src/main/java/com/stayalert/domain/SessionEvent.kt` — `OverlayHidden`/`OverlayHideFailed`
- [M] `app/src/main/java/com/stayalert/domain/SessionController.kt` — notifier, Deteniendo con confirmación, completeTermination
- [M] `app/src/main/java/com/stayalert/system/SystemOverlayController.kt` — hide suspend con propagación de fallo
- [M] `app/src/main/java/com/stayalert/system/SessionCommandHandler.kt` — hideOverlay con eventos de confirmación
- [M] `app/src/main/java/com/stayalert/AppContainer.kt` — notifier al SessionController
- [M] `app/src/test/java/com/stayalert/domain/SessionControllerTest.kt` — fakes y tests de confirmación
- [M] `app/src/test/java/com/stayalert/system/StopReceiverTest.kt` — contratos Deteniendo→Inactiva
- [M] `app/src/test/java/com/stayalert/system/SessionCommandHandlerTest.kt` — hide con éxito/fallo
- [M] `app/src/test/java/com/stayalert/system/SystemWatchdogTest.kt` — fake hide suspend
- [M] `app/src/test/java/com/stayalert/ui/MainScreenTest.kt` — fake notifier
- [M] `app/src/test/java/com/stayalert/ui/MainViewModelTest.kt` — fake notifier

## Change Log

- 2026-08-09: Implementación story 3.2 — terminación limpia con feedback (A4 + FR-12)
