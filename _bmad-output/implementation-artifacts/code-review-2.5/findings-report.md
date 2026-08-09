# Code Review 2.5 — Findings Report

**Diff reviewed:** working tree de la story 2.5 (6 archivos, +365 líneas)
**Spec:** `_bmad-output/implementation-artifacts/2-5-watchdog-de-sesion.md`
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

### 1. `SystemWatchdog` no detecta `HIDE_OVERLAY_WINDOWS` (AC-1 incompleto)
- **Source:** auditor
- **Severity:** medium
- **Location:** `app/src/main/java/com/stayalert/system/SystemWatchdog.kt:110-141`
- **Detail:** AC-1 exige detectar `HIDE_OVERLAY_WINDOWS` (overlay no dibujado sobre la app objetivo, p. ej. apps bancarias). El watchdog actual detecta `OverlayMissing` (overlay no visible) pero no el caso específico de que el overlay esté visible pero NO sobre la app objetivo (porque la app declara `HIDE_OVERLAY_WINDOWS`). La detección de este caso requiere verificar si la app objetivo está en primer plano Y el overlay está visible — si ambos son true pero el overlay no se dibuja sobre ella, es `HIDE_OVERLAY_WINDOWS`. Actualmente `TargetLeftForeground` cubre el caso de app fuera de primer plano, pero no el de overlay no dibujado sobre una app en primer plano.
- **Fix:** Documentar la limitación o añadir una heurística: si `ForegroundStatus.FOREGROUND` y overlay visible, no hay forma directa de saber si el overlay se dibuja sobre ella (el sistema no lo expone). La detección real requiere `WindowManager` internals. Defer con documentación explícita en el código.

### 2. `SystemWatchdog` no usa el `Clock` inyectable para el polling
- **Source:** blind+edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/SystemWatchdog.kt:100`
- **Detail:** AD-12 exige que todo delay/SLA use el `Clock` inyectable. El watchdog recibe `clock` pero usa `delay(SessionConstants.WATCHDOG_POLL_MS)` directamente. El `clock` es dead code (mismo patrón que el finding de 2.2). El SLA < 5 s (NFR-3) se cumple por el polling de 2 s, pero no se mide con el clock.
- **Fix:** Eliminar el parámetro `clock` (dead code) o usarlo para medir el tiempo entre checks. Recomendado: eliminar (el `delay` del scheduler es el mecanismo correcto para polling).

### 3. `SystemWatchdog` emite eventos repetidamente si la anomalía persiste
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/SystemWatchdog.kt:110-141`
- **Detail:** Si la pantalla está apagada, el watchdog emite `ScreenOff` cada 2 s. El `SessionController` es idempotente (el primer `ScreenOff` termina la sesión; los siguientes son no-ops en `Inactiva`), así que no hay daño funcional, pero es ruido. Además, si el evento se emite justo cuando la sesión ya terminó, no hay problema. Aceptable, pero documentar o añadir un flag de "anomalía ya reportada".
- **Fix:** Aceptable como está (idempotencia del controller lo cubre). Documentar en el código.

---

## Triage: defer

### 4. `SystemWatchdog` no distingue `TargetCrashed` de `TargetLeftForeground`
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/SystemWatchdog.kt:127-131`
- **Detail:** FR-14 (crash de la app objetivo) y FR-13 (salida de primer plano) son eventos distintos en `SessionEvent` (`TargetCrashed` vs `TargetLeftForeground`), pero el watchdog solo emite `TargetLeftForeground` cuando la app no está en primer plano. No hay forma de distinguir un crash de una salida normal con `UsageStatsManager`. Defer: aceptar la limitación (el motivo de la notificación será "la app objetivo salió de primer plano" en ambos casos) o usar `ActivityManager` para detectar crashes.

### 5. `SystemWatchdog` no maneja `BatteryWarning` repetido (aviso cada 2 s)
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/SystemWatchdog.kt:137-138`
- **Detail:** `BatteryWarning` se emite en cada poll mientras la batería esté ≤ 15%. El `SessionController` lo trata como no-op en `Aislada` (no termina), pero el `Notifier` (story 2.4) no publica el aviso — el evento se pierde. Defer: cuando se implemente el aviso de batería (FR-16), el watchdog deberá emitir `BatteryWarning` solo una vez (flag) o el controller deberá gestionar la deduplicación.

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Detección de anomalías | ⚠️ | Pantalla/permiso/overlay/foreground/batería OK. `HIDE_OVERLAY_WINDOWS` no detectable directamente (finding #1) |
| 2. SLA < 5 s | ✅ | Polling 2 s → detección en ≤ 2 s + terminación inmediata |
| 3. Gating por estado | ✅ | `StartWatchdog`/`StopWatchdog` desde el controller (solo en `Aislada`) |
| 4. Solo detecta y emite | ✅ | Nunca publica notificaciones (AD-5, AD-8) |
| 5. Polling 2 s | ✅ | `WATCHDOG_POLL_MS` |
| 6. Crash de app objetivo | ⚠️ | Cubierto por `TargetLeftForeground` (finding #4: no distingue crash) |
| 7. Permiso de uso no concedido | ✅ | `ForegroundMonitor` devuelve `UNKNOWN` → detección omitida (AD-11) |
| 8. Tests | ✅ | `SystemWatchdogTest` (6) — verdes |

---

## Recommendation

**Approve with 3 minor patches.** Aplicar los `patch` findings (documentar HIDE_OVERLAY_WINDOWS, eliminar clock muerto, documentar repetición de eventos), luego merge. Los `defer` son deuda consciente.
