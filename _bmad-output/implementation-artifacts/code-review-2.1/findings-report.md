# Code Review 2.1 — Findings Report

**Diff reviewed:** working tree de la story 2.1 (15 archivos, +846 líneas)
**Spec:** `_bmad-output/implementation-artifacts/2-1-nucleo-de-sesion.md`
**Layers:** Blind Hunter, Edge Case Hunter, Acceptance Auditor (single model; no subagents disponibles)

---

## Summary

| Bucket | Count |
|--------|-------|
| patch | 3 |
| decision_needed | 0 |
| defer | 2 |
| dismiss | 0 |
| **Total actionable** | **3** |

No blocking issues. La story cumple todos los ACs.

---

## Triage: patch

### 1. `SessionController.terminate()` no emite `SessionEvent` de confirmación de fin
- **Source:** blind
- **Severity:** medium
- **Location:** `app/src/main/java/com/stayalert/domain/SessionController.kt:190-198`
- **Detail:** `terminate()` pasa por `Deteniendo` y vuelve a `Inactiva` síncronamente dentro del mismo handler. El estado `Deteniendo` es transitorio e invisible para la UI (nunca se observa). Además, no hay evento de "sesión terminada" que la UI o el `Notifier` (story 2.4) puedan consumir para mostrar feedback. El `lastTerminationReason` existe pero la UI no lo observa.
- **Fix:** (a) Exponer `lastTerminationReason` en `MainViewModel` y mostrarlo en la UI (o al menos observarlo); (b) considerar mantener `Deteniendo` hasta que los componentes SO confirmen (handshake de terminación) — aunque para v1 el estado transitorio es aceptable, documentarlo.

### 2. `SessionController` no expone el `Channel` de eventos de forma segura — `trySend` puede fallar silenciosamente
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/domain/SessionController.kt:89-91`
- **Detail:** `events.trySend(event)` con `Channel.UNLIMITED` nunca falla en la práctica, pero si el consumidor muere (scope cancelado), los eventos se acumulan sin procesar y el estado queda congelado. No hay mecanismo de detección de consumidor muerto.
- **Fix:** Documentar la limitación o añadir un `onConsumptionFailed`/`isActive` check. Para v1, aceptable — el scope del controller vive con la Activity.

### 3. `MainScreen` muestra el error de validación solo tras pulsar el botón — no hay indicación preventiva
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/MainActivity.kt:412-419`
- **Detail:** UX-DR9/EXPERIENCE.md dice que el botón debe estar deshabilitado con mensaje "Falta: {permiso}" cuando hay permisos pendientes (estado "Permiso pendiente" en State Patterns). Actualmente el botón está habilitado (si aviso aceptado) y el error solo aparece tras pulsar. El patrón de EXPERIENCE.md es mostrar el motivo de forma preventiva.
- **Fix:** Evaluar la validación en `MainViewModel` al arrancar (o al volver de settings) y mostrar el motivo de bloqueo de forma preventiva, deshabilitando el botón si falta algo.

---

## Triage: defer

### 4. `SystemClock` importa `android.os.SystemClock` — acopla domain a Android
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/domain/SystemClock.kt:5`
- **Detail:** `domain/` debería ser puro Kotlin (testeable en JVM sin Robolectric). `SystemClock` usa `android.os.SystemClock.elapsedRealtime()`, lo que acopla la capa domain a Android. Defer: mover `SystemClock` a `system/` (capa de integración) cuando se refactorice la estructura.

### 5. `SessionController` no maneja `BatteryWarning` en `Lanzando`/`Deteniendo` con notificación
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/domain/SessionController.kt:157-159`
- **Detail:** `BatteryWarning` es no-op en `Aislada` (correcto: no termina), pero el aviso al 15% (FR-16) requiere una notificación que el `Notifier` (story 2.4) publicará. El controller no emite ningún comando/evento para ese aviso. Defer: cuando exista `Notifier` (story 2.4), el controller deberá emitir un evento de aviso o el watchdog lo hará directamente.

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Validación FR-5 | ✅ | `SessionValidator` con 4 motivos; UI muestra el motivo exacto |
| 2. Transición a `Lanzando` | ✅ | `startSession()` → `Lanzando` + `LaunchTarget` |
| 3. `SessionController` único propietario | ✅ | `SessionState` sealed + `StateFlow`; solo el controller muta |
| 4. Event loop consumidor único | ✅ | `Channel(UNLIMITED)` + `for (event in events)` en `init` |
| 5. Recuperación ante kill | ✅ | Sesión efímera (AD-6); estado `Inactiva` al reabrir (no se persiste) |
| 6. `Clock` inyectable | ✅ | `Clock` interfaz + `SystemClock` + fakes en tests |
| 7. Tests | ✅ | `SessionControllerTest` (6), `SessionValidatorTest` (5) — verdes |

---

## Recommendation

**Approve with 3 minor patches.** Aplicar los `patch` findings (feedback de terminación en UI, documentar limitación del channel, validación preventiva), luego merge. Los `defer` son deuda consciente para stories 2.4+.
