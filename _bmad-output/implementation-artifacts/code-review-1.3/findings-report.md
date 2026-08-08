# Code Review 1.3 — Findings Report

**Diff reviewed:** working tree de la story 1.3 (10 archivos, +598 líneas)
**Spec:** `_bmad-output/implementation-artifacts/1-3-auditoria-y-guiado-de-permisos.md`
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

### 1. `SettingsScreen` no maneja `ActivityNotFoundException` al abrir Ajustes
- **Source:** edge
- **Severity:** medium
- **Location:** `app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:343`
- **Detail:** `context.startActivity(permissionSettingsIntent(...))` puede lanzar `ActivityNotFoundException` en dispositivos/OEMs donde el intent no tenga handler (p. ej. `ACTION_APP_NOTIFICATION_SETTINGS` en algunos fabricantes). Crashing la app en un flujo de onboarding es un fallo real.
- **Fix:** Envolver en try/catch (`runCatching`) y loguear; el usuario permanece en la pantalla.

### 2. `PermissionRow` usa `clickable(enabled = pending)` — filas concedidas no son accesibles por TalkBack
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:368`
- **Detail:** AC-5 exige "TalkBack con rol+estado". Una fila con `clickable(enabled=false)` no se anuncia como interactiva, pero tampoco expone el estado (concedido/pendiente) como `contentDescription` o `stateDescription`. El texto secundario lo dice, pero TalkBack lo lee como texto plano, no como estado.
- **Fix:** Añadir `semantics { stateDescription = ... }` o `contentDescription` con rol+estado en la fila.

### 3. `SystemPermissionAuditorTest.areNotificationsEnabled` asume `true` por defecto
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/test/java/com/stayalert/data/SystemPermissionAuditorTest.kt:511-513`
- **Detail:** El test asume que Robolectric devuelve `true` para `areNotificationsEnabled()`. Esto es un test de humo frágil: si Robolectric cambia el default, el test falla sin que el código haya cambiado. Mejor: verificar contra el valor real del sistema (leer `NotificationManager.areNotificationsEnabled()` en el test y comparar).
- **Fix:** Comparar con el valor real: `assertEquals(manager.areNotificationsEnabled(), auditor.areNotificationsEnabled())`.

---

## Triage: defer

### 4. Navegación con `mutableStateOf` en vez de Navigation Compose
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/MainActivity.kt:134`
- **Detail:** La navegación simple con estado funciona para 2 pantallas, pero no maneja back gesture del sistema (el botón back del dispositivo no vuelve de configuración a principal; solo el botón "Volver" de la UI). Defer: evaluar Navigation Compose cuando haya más pantallas (story 1.4 añadirá más configuración).

### 5. `SettingsViewModel.refresh()` no es idempotente ante recomposiciones
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:297-305`
- **Detail:** El `DisposableEffect` se re-ejecuta en cada recomposición del composable (si el `lifecycleOwner` cambia). En la práctica `LocalLifecycleOwner` es estable, así que no hay problema real. Defer: no requiere acción.

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Auditoría de permisos | ✅ | `SystemPermissionAuditor.audit()` con `Settings.canDrawOverlays()` y `NotificationManager.areNotificationsEnabled()` |
| 2. Acceso directo | ✅ | Overlay → `ACTION_MANAGE_OVERLAY_PERMISSION`; notificaciones → `ACTION_APP_NOTIFICATION_SETTINGS` + `EXTRA_APP_PACKAGE` |
| 3. Re-auditoría al regresar | ✅ | `LifecycleEventObserver` en `ON_RESUME` → `viewModel.refresh()` |
| 4. `PermissionAuditor` único lector | ✅ | Interfaz `PermissionAuditor` + `SystemPermissionAuditor`; solo él lee permisos |
| 5. UI | ⚠️ | Tarjeta surface-raised, filas label+valor, tap targets ≥ 48dp OK. TalkBack rol+estado parcial (finding #2) |
| 6. Tests | ✅ | `SystemPermissionAuditorTest` (3), `SettingsViewModelTest` (2), `MainScreenTest` (2) — verdes |

---

## Recommendation

**Approve with 3 minor patches.** Aplicar los `patch` findings (try/catch en startActivity, semantics de estado para TalkBack, test robusto de notificaciones), luego merge. Los `defer` son deuda consciente.
