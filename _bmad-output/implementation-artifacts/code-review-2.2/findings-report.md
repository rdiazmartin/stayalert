# Code Review 2.2 — Findings Report

**Diff reviewed:** working tree de la story 2.2 (14 archivos, +597 líneas)
**Spec:** `_bmad-output/implementation-artifacts/2-2-despliegue-secuencial-y-overlay-de-aislamiento.md`
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

### 1. `SessionCommandHandler` usa `delay()` real en vez del `Clock` inyectable
- **Source:** blind+edge
- **Severity:** medium
- **Location:** `app/src/main/java/com/stayalert/system/SessionCommandHandler.kt:217`
- **Detail:** AD-12 exige que todo delay/SLA use el `Clock` inyectable. El handler recibe `clock` pero usa `delay(SessionConstants.LAUNCH_DELAY_MS)` directamente. En producción funciona, pero el `clock` inyectado es dead code y el delay no es testeable con el fake clock (los tests usan `advanceTimeBy` del scheduler, no el clock). Inconsistencia con AD-12.
- **Fix:** Usar `clock` para medir el tiempo transcurrido (p. ej. `while (clock.now() - start < LAUNCH_DELAY_MS) delay(50)`) o eliminar el parámetro `clock` si no se usa (documentando que el delay usa el scheduler). Recomendado: usar el clock para la medición.

### 2. `SystemOverlayController.show()` no verifica el permiso `SYSTEM_ALERT_WINDOW` antes de `addView`
- **Source:** edge
- **Severity:** medium
- **Location:** `app/src/main/java/com/stayalert/system/SystemOverlayController.kt:285`
- **Detail:** Si el permiso de overlay se revoca entre la validación FR-5 y el despliegue, `wm.addView` lanza `BadTokenException`/`SecurityException` — capturado por el catch genérico y emitido como `OverlayFailed`. Funciona, pero el mensaje de error es genérico. Además, el catch captura `Exception` pero `SecurityException` es `RuntimeException` (sí, es Exception). OK, pero el SLA se mide con `System.currentTimeMillis()` en vez del `Clock` inyectable (AD-12).
- **Fix:** (a) Verificar `Settings.canDrawOverlays(context)` antes de `addView` y emitir `OverlayFailed("permiso de overlay revocado")`; (b) usar el `Clock` inyectable para medir el SLA.

### 3. `UsageStatsForegroundMonitor` no maneja el caso de `queryEvents` con permiso denegado
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/UsageStatsForegroundMonitor.kt:337`
- **Detail:** Si el permiso `PACKAGE_USAGE_STATS` no está concedido, `queryEvents` puede lanzar `SecurityException` (no devolver null). El código solo maneja el null. Un `SecurityException` crashearía la corrutina del handler.
- **Fix:** Envolver `queryEvents` en try/catch y devolver `UNKNOWN` ante `SecurityException`.

---

## Triage: defer

### 4. `SessionCommandHandler` no usa `clock` en absoluto (parámetro muerto)
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/SessionCommandHandler.kt:176`
- **Detail:** El parámetro `clock` se inyecta pero nunca se usa. Defer: si se adopta el fix #1 (usar clock para el delay), el parámetro cobra vida; si no, eliminarlo en refactor.

### 5. `MainActivity` con `lateinit` + lazy circular — patrón frágil
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/MainActivity.kt:78-110`
- **Detail:** La referencia circular `sessionController` ↔ `commandHandler` se resuelve con `lateinit` + `commandHandler` lazy que se auto-asigna. Funciona pero es frágil: si `commandHandler` se accede antes de `onCreate`, `lateinit` lanza. Defer: considerar un `AppContainer`/`ServiceLocator` simple cuando crezca el wiring (stories 2.4/2.5 añadirán FGS y watchdog).

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Lanzamiento vía `TargetAppLauncher` | ✅ | `IntentLauncher` con `Result<Unit>` (AD-3) |
| 2. Delay 1 s | ⚠️ | `delay(LAUNCH_DELAY_MS)` — funciona, pero no vía `Clock` (finding #1) |
| 3. Overlay flags | ✅ | `TYPE_APPLICATION_OVERLAY`, NOT_FOCUSABLE+LAYOUT_IN_SCREEN+KEEP_SCREEN_ON+SECURE, sin NOT_TOUCHABLE, fondo negro |
| 4. Handshake Lanzando→Aislada | ✅ | `OverlayShown` → transición (ya en 2.1) |
| 5. Aborto limpio | ✅ | `LaunchFailed`/`OverlayFailed` → `Inactiva` (ya en 2.1) |
| 6. Confirmación de primer plano | ✅ | `ForegroundMonitor` + aborto si `NOT_FOREGROUND` |
| 7. FGS después del overlay | ✅ | `StartFgs` emitido tras `OverlayShown` (2.1); FGS real en 2.4 |
| 8. Retención de pantalla | ✅ | `FLAG_KEEP_SCREEN_ON` en el overlay |
| 9. Sin elementos gráficos | ✅ | `View` con fondo negro, sin contenido |
| 10. SLA < 500 ms | ⚠️ | Medido con `System.currentTimeMillis()` (finding #2b) |
| 11. Tests | ✅ | `IntentLauncherTest` (3), `SessionCommandHandlerTest` (3) — verdes |

---

## Recommendation

**Approve with 3 minor patches.** Aplicar los `patch` findings (clock en delay, verificación de permiso + clock en SLA, try/catch en UsageStats), luego merge. Los `defer` son deuda consciente.
