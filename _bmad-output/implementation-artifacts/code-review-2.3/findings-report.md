# Code Review 2.3 — Findings Report

**Diff reviewed:** working tree de la story 2.3 (4 archivos, +220 líneas)
**Spec:** `_bmad-output/implementation-artifacts/2-3-patron-de-salida.md`
**Layers:** Blind Hunter, Edge Case Hunter, Acceptance Auditor (single model; no subagents disponibles)

---

## Summary

| Bucket | Count |
|--------|-------|
| patch | 2 |
| decision_needed | 0 |
| defer | 2 |
| dismiss | 0 |
| **Total actionable** | **2** |

No blocking issues. La story cumple todos los ACs.

---

## Triage: patch

### 1. `PatternDetector` no valida coordenadas negativas ni dimensiones cero
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/domain/PatternDetector.kt:59-63`
- **Detail:** Si `width` o `height` son 0 (o negativos), `regionWidth`/`regionHeight` son 0 y `x >= width - 0` puede ser true con `x` negativo (coordenadas fuera de pantalla). En la práctica el View siempre tiene dimensiones válidas, pero el detector es una clase pura que debería ser robusta.
- **Fix:** Guard clause: `if (width <= 0f || height <= 0f) return false` en `onTouch`.

### 2. `SystemOverlayController` no reinicia el detector al ocultar el overlay
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/SystemOverlayController.kt:73-86`
- **Detail:** Si el usuario hace 3 toques en la región y la sesión termina por otra vía (watchdog, notificación), el contador del detector queda en 3. En la siguiente sesión, un solo toque en la región completaría el patrón y terminaría la sesión inmediatamente — comportamiento inesperado.
- **Fix:** Llamar `patternDetector.reset()` en `hide()`.

---

## Triage: defer

### 3. `PatternDetector` no expone el estado del contador (solo reset)
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/domain/PatternDetector.kt`
- **Detail:** No hay forma de inspeccionar `tapCount` (p. ej. para debug o tests). Defer: añadir `tapCount` expuesto si se necesita en el futuro.

### 4. `MainActivity` crea dos `SystemClock()` separados (overlay y detector)
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/MainActivity.kt:97-100`
- **Detail:** `SystemClock()` se instancia dos veces (una para el overlay, otra para el detector). Funciona (ambos leen el mismo reloj del sistema), pero es duplicación. Defer: compartir una instancia única cuando se introduzca un `AppContainer`.

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Detección del patrón | ✅ | `PatternDetector` con región 15%×15%, ventana 500 ms, 4 toques |
| 2. Terminación completa | ✅ | `PatternDetected` → `terminate(Pattern)` → HideOverlay+StopFgs+StopWatchdog (2.1) |
| 3. Notificación | ⏳ | Evento emitido; notificación real en story 2.4 (Notifier) |
| 4. Reinicio del contador | ✅ | Fuera de región o fuera de ventana → `reset()` |
| 5. Idempotencia | ✅ | `terminate()` idempotente (2.1) |
| 6. Clock inyectable | ✅ | `PatternDetector(clock)` con fake en tests |
| 7. Tests | ✅ | `PatternDetectorTest` (5) — verdes |

---

## Recommendation

**Approve with 2 minor patches.** Aplicar los `patch` findings (guard clause de dimensiones, reset del detector en `hide()`), luego merge. Los `defer` son deuda consciente.
