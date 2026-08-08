# Code Review 1.4 — Findings Report

**Diff reviewed:** working tree de la story 1.4 (16 archivos, +606 líneas)
**Spec:** `_bmad-output/implementation-artifacts/1-4-configuracion-de-la-app-objetivo-y-exencion-de-bateria.md`
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

### 1. `setTargetPackage`/`setTargetActivity` escriben en DataStore en cada tecla
- **Source:** blind+edge
- **Severity:** medium
- **Location:** `app/src/main/java/com/stayalert/ui/settings/SettingsViewModel.kt:381-391`
- **Detail:** `onValueChange` se dispara por cada carácter tecleado; cada uno lanza una escritura a DataStore (I/O en disco). Escribir en cada tecla es ineficiente y puede causar jank en el campo de texto. Además, `setTargetPackage` re-valida la instalación en cada tecla (llamada a `PackageManager`).
- **Fix:** Debounce (p. ej. `debounce(500)` en un flow) o persistir al perder foco (`onFocusChanged`). La validación de instalación debería hacerse solo al persistir, no por tecla.

### 2. `SettingsScreen` no es scrollable — contenido puede desbordarse en pantallas pequeñas
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:71-76`
- **Detail:** Con 3 secciones (Permisos, App objetivo, Batería) + títulos, el contenido excede la altura en pantallas pequeñas o con dynamic type grande (UX-DR10 exige dynamic type). Sin `verticalScroll`, el contenido se corta y las secciones inferiores son inaccesibles.
- **Fix:** Envolver el `Column` en `Modifier.verticalScroll(rememberScrollState())`.

### 3. `SystemAppInstalledChecker` no usa `PackageManager.MATCH_*` flags — puede fallar con apps deshabilitadas
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/data/SystemAppInstalledChecker.kt:124`
- **Detail:** `getPackageInfo(packageName, 0)` devuelve la app aunque esté deshabilitada por el usuario. Para la validación de FR-4 ("app objetivo instalada"), una app deshabilitada no es lanzable. Considerar `PackageManager.MATCH_DISABLED_COMPONENTS` no aplica aquí; el punto es que `getPackageInfo` con flags 0 no distingue habilitada/deshabilitada.
- **Fix:** Verificar también `ApplicationInfo.enabled` (o usar `getApplicationInfo` + `enabled` flag) para reportar "no instalada" si está deshabilitada.

---

## Triage: defer

### 4. `DEFAULT_TARGET_ACTIVITY` hardcodeado como constante top-level
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/data/DataStoreSettingsRepository.kt:58`
- **Detail:** La actividad principal de Teams (`com.microsoft.teams.activities.MainActivity`) es un detalle de la app objetivo que podría cambiar. Defer: cuando se implemente `TargetAppLauncher` (AD-3, story 2.1), centralizar los defaults en un objeto de configuración.

### 5. `SettingsScreen` crece en complejidad — considerar extraer secciones
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt`
- **Detail:** La pantalla tiene 3 secciones en un solo archivo (177+ líneas). Defer: extraer `TargetAppSection` y `BatterySection` como componentes cuando se añada más configuración.

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Edición de app objetivo | ✅ | `OutlinedTextField` para paquete y actividad, defaults Teams |
| 2. Persistencia | ✅ | `setTargetPackage`/`setTargetActivity` en DataStore (claves `target_package`/`target_activity`) |
| 3. Validación de instalación | ⚠️ | `SystemAppInstalledChecker` + error en UI. Finding #3 (apps deshabilitadas) |
| 4. Exención de batería | ✅ | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + permiso en manifest |
| 5. Estado de exención | ✅ | `batteryExempt` StateFlow + texto/punto en UI |
| 6. UI | ⚠️ | UX-DR5/UX-DR8 OK. Finding #2 (scroll) |
| 7. Tests | ✅ | `SettingsRepositoryTest` (+3), `SettingsViewModelTest` (4), `SystemAppInstalledCheckerTest` (2), `SystemBatteryOptimizationCheckerTest` (1) — verdes |

---

## Recommendation

**Approve with 3 minor patches.** Aplicar los `patch` findings (debounce en persistencia, scroll en SettingsScreen, check de app deshabilitada), luego merge. Los `defer` son deuda consciente.
