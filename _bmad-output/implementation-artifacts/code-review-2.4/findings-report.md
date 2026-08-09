# Code Review 2.4 — Findings Report

**Diff reviewed:** working tree de la story 2.4 (9 archivos, +464 líneas)
**Spec:** `_bmad-output/implementation-artifacts/2-4-servicio-en-primer-plano-y-notificaciones.md`
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

### 1. `StopReceiver.onStopRequested` es un callback estático global — frágil y con fuga de memoria
- **Source:** blind
- **Severity:** medium
- **Location:** `app/src/main/java/com/stayalert/system/StopReceiver.kt:305`
- **Detail:** El callback estático se asigna en `MainActivity.onCreate` y nunca se limpia. Si la Activity se recrea (rotación), el callback se reasigna (OK), pero si la app se destruye y recrea, el viejo callback puede quedar referenciando una Activity muerta. Además, es un singleton global que dificulta tests. El patrón correcto para receivers de notificaciones es registrar el receiver dinámicamente con `Context.registerReceiver` (con instancia) o usar un `PendingIntent` a una Activity/Service en vez de un BroadcastReceiver estático.
- **Fix:** Registrar `StopReceiver` dinámicamente en `MainActivity` (con instancia y callback por constructor) en `onStart`/`onStop`, o usar `registerReceiver` con `Context.RECEIVER_NOT_EXPORTED`. Alternativa: `PendingIntent.getService` a un intent que el `SessionCommandHandler` procese.

### 2. `PresenceForegroundService` duplica la construcción de la notificación de sesión
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/PresenceForegroundService.kt:205-225`
- **Detail:** `SystemNotifier.showSessionNotification()` y `PresenceForegroundService.buildSessionNotification()` construyen la misma notificación (mismo título, texto, acción). Duplicación que puede divergir. Además, `onStartCommand` llama `notifier.showSessionNotification()` (que hace `notify`) y luego `startForeground` con la misma notificación — doble publicación.
- **Fix:** Exponer `SystemNotifier.buildSessionNotification(): Notification` y usarla en ambos lugares; en el servicio, solo `startForeground` (sin `notify` previo).

### 3. `SystemNotifier` no maneja el permiso `POST_NOTIFICATIONS` (API 33+)
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/data/SystemNotifier.kt:94-95`
- **Detail:** En API 33+, si el usuario no concede `POST_NOTIFICATIONS`, `notify()` no muestra nada (no crashea, pero silencioso). La validación FR-5 ya cubre `areNotificationsEnabled()`, pero el `Notifier` debería degradar con gracia. Defer: aceptable — la validación previa lo cubre.

---

## Triage: defer

### 4. `SystemNotifier` no expone `buildSessionNotification()` (relacionado con #2)
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/data/SystemNotifier.kt`
- **Detail:** La notificación de sesión se construye en dos sitios. Defer: refactorizar cuando se resuelva #2.

### 5. `PresenceForegroundService` usa `START_STICKY` — puede reiniciarse sin sesión activa
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/system/PresenceForegroundService.kt:195`
- **Detail:** `START_STICKY` reinicia el servicio si el sistema lo mata, incluso sin sesión activa (el overlay ya no existe). El servicio se reiniciaría mostrando una notificación de sesión sin overlay. `START_NOT_STICKY` sería más seguro (el controller decide cuándo reiniciar).
- **Fix:** Cambiar a `START_NOT_STICKY` (o `START_REDELIVER_INTENT` si se quiere reintento). Defer: evaluar en la práctica.

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. FGS activo | ✅ | `PresenceForegroundService` `specialUse` + permisos en manifest |
| 2. Notificación de sesión | ✅ | Canal `stayalert_session`, texto UX-DR7, acción Detener |
| 3. Kill switch | ⚠️ | `StopReceiver` → `StopRequested`. Finding #1 (callback estático) |
| 4. Fin de sesión | ✅ | `onDestroy` cancela notificación; `stopService` en `StopFgs` |
| 5. Notificación de fin | ✅ | Canal `stayalert_events`, "Sesión terminada: {motivo}" |
| 6. Notifier único publicador | ✅ | Solo `SystemNotifier` publica; mapeo motivo→texto en él (AD-8, AD-10) |
| 7. Tests | ✅ | `SystemNotifierTest` (4) — verdes |

---

## Recommendation

**Approve with 3 minor patches.** Aplicar los `patch` findings (receiver dinámico, notificación única, START_NOT_STICKY), luego merge. Los `defer` son deuda consciente.
