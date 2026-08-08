# Code Review 1.2 — Findings Report

**Diff reviewed:** working tree de la story 1.2 (12 archivos, +413/-7)
**Spec:** `_bmad-output/implementation-artifacts/1-2-aviso-de-uso-responsable.md`
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

### 1. `DataStoreSettingsRepository` se instancia dentro de `setContent` en cada recomposición
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/MainActivity.kt:40-48`
- **Detail:** `DataStoreSettingsRepository(applicationContext)` se crea en cada recomposición del composable. El ViewModel se cachea (factory), pero el repository se instancia innecesariamente en cada recomposición. Además, el delegado `preferencesDataStore` es singleton, así que no hay fuga, pero es un anti-patrón.
- **Fix:** Crear el repository una sola vez en `onCreate` (o como propiedad de la Activity) y pasarlo al factory.

### 2. `collectAsState()` en vez de `collectAsStateWithLifecycle()`
- **Source:** blind+edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/MainActivity.kt:53`
- **Detail:** `collectAsState()` sigue recolectando el flow cuando la activity está en background (STARTED/STOPPED), lo que es ineficiente y puede causar recomposiciones innecesarias. El patrón recomendado de AndroidX es `collectAsStateWithLifecycle()` (requiere `lifecycle-runtime-compose`).
- **Fix:** Usar `collectAsStateWithLifecycle()` y añadir la dependencia `androidx.lifecycle:lifecycle-runtime-compose`.

### 3. `acceptNotice()` no maneja errores de DataStore
- **Source:** edge
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/viewmodel/MainViewModel.kt:19-23`
- **Detail:** Si `setNoticeAccepted` lanza (IO, corrupción de DataStore), la excepción se propaga en `viewModelScope` y puede crashear la app sin feedback al usuario. El modal permanecería abierto (estado no actualizado), pero el crash es el problema.
- **Fix:** Envolver en try/catch (o `runCatching`) y loguear; el estado no cambia y el modal permanece — comportamiento degradado aceptable.

---

## Triage: defer

### 4. `SharingStarted.Eagerly` mantiene el flow activo sin suscriptores
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/ui/viewmodel/MainViewModel.kt:14`
- **Detail:** `Eagerly` inicia la recolección del DataStore flow al crear el ViewModel y la mantiene siempre. `WhileSubscribed(5000)` sería más eficiente, pero se eligió `Eagerly` para que `.value` refleje el estado real en tests sin suscriptores. Para una preferencia pequeña es aceptable. Defer: revisar si se puede usar `WhileSubscribed` con un test helper cuando se introduzca más configuración (story 1.4).

### 5. `SettingsKeys.TARGET_PACKAGE` y `TARGET_ACTIVITY` sin uso
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/main/java/com/stayalert/data/SettingsKeys.kt:5-6`
- **Detail:** Se definen claves que la story 1.2 no usa. Son parte del plan (story 1.4), pero YAGNI en esta story. Defer: se usarán en 1.4; no eliminarlas para evitar churn.

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Modal en primer inicio | ✅ | `MainScreen` muestra `ResponsibleUseNotice` si `!noticeAccepted` |
| 2. Texto vinculante | ✅ | Texto exacto del PRD, concatenado correctamente |
| 3. No dismissable | ✅ | `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)` |
| 4. Persistencia | ✅ | `setNoticeAccepted(true)` en DataStore; `noticeAccepted` flow lo refleja |
| 5. Botón deshabilitado | ✅ | `enabled = noticeAccepted` |
| 6. Visual | ✅ | Card `surface-raised`, `rounded/md` 16dp, `headlineSmall`/`bodyLarge`/`labelLarge`, `accent`/`accent-on` |
| 7. Tests | ✅ | `SettingsRepositoryTest` (2), `MainViewModelTest` (2), `MainScreenTest` (2) — todos verdes |

---

## Recommendation

**Approve with 3 minor patches.** Aplicar los `patch` findings (repository fuera del composable, `collectAsStateWithLifecycle`, manejo de error en `acceptNotice`), luego merge. Los `defer` son deuda consciente para stories futuras.
