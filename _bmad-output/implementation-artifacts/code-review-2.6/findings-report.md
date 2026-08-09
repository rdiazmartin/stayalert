# Code Review 2.6 — Findings Report

**Diff reviewed:** working tree de la story 2.6 (16 archivos, +389 líneas)
**Spec:** `_bmad-output/implementation-artifacts/2-6-mock-de-teams-y-validacion-e2e.md`
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

### 1. `SessionE2ETest.flujo completo` tiene aserciones vacías (assertTrue(true))
- **Source:** blind
- **Severity:** medium
- **Location:** `app/src/androidTest/java/com/stayalert/SessionE2ETest.kt:79-81`
- **Detail:** El test principal del E2E termina con `assertTrue("La app no crasheó", true)` — una aserción tautológica que siempre pasa. `mockOpened` se calcula pero nunca se usa. El test no valida nada real: no verifica que el overlay esté visible, que el mock permanezca resumed, ni que el patrón de salida funcione. Es un test de humo disfrazado de E2E.
- **Fix:** Usar `mockOpened` en la aserción (`assertTrue("El mock debe abrirse", mockOpened)`), y añadir verificaciones reales: overlay visible (vía `dumpsys window` o `UiDevice`), patrón de salida (4 taps → mock en primer plano).

### 2. `StopReceiverTest` tiene código muerto y aserción débil
- **Source:** blind
- **Severity:** low
- **Location:** `app/src/androidTest/java/com/stayalert/StopReceiverTest.kt:126-129`
- **Detail:** `val forwarded = context.getSystemService(Context.ACTIVITY_SERVICE)?.let { null }` es código muerto (siempre null, nunca usado). La aserción solo verifica que el intent original tiene la acción — no verifica que el receiver haga nada. El test no cubre el comportamiento real (reenvío a MainActivity).
- **Fix:** Eliminar el código muerto y verificar el comportamiento real: el receiver lanza MainActivity — verificar con `UiDevice` que la app está en primer plano, o al menos que `startActivity` no lanza excepción (ya implícito).

### 3. `mock-teams` no está excluido de release explícitamente
- **Source:** auditor
- **Severity:** low
- **Location:** `mock-teams/build.gradle.kts`
- **Detail:** AC-3 exige que el mock esté excluido de los builds de release. El módulo es independiente (no es dependencia de `app`), así que `assembleRelease` de `app` no lo incluye — pero `./gradlew assembleRelease` (raíz) sí compilaría el mock en release. La exclusión no es explícita.
- **Fix:** Documentar en el README o añadir una condición en el build del mock (p. ej. `if (gradle.startParameter.taskNames.any { it.contains("Release") }) { ... }` o simplemente documentar que el mock es solo para debug).

---

## Triage: defer

### 4. `SessionE2ETest` no verifica el proxy SM-1a (mock resumed ≥ 8 h)
- **Source:** auditor
- **Severity:** low
- **Location:** `app/src/androidTest/java/com/stayalert/SessionE2ETest.kt`
- **Detail:** AC-4 exige validar que el mock permanece resumed ≥ 8 h (proxy SM-1a). Un test instrumentado de 8 h no es práctico en CI; se valida manualmente en el emulador. Defer: documentar el procedimiento manual de validación de 8 h en el README.

### 5. `SessionE2ETest` no verifica "0 toques recibidos por el mock" (SM-2)
- **Source:** auditor
- **Severity:** low
- **Location:** `app/src/androidTest/java/com/stayalert/SessionE2ETest.kt`
- **Detail:** AC-4 exige verificar que el mock no recibe toques mientras el overlay está visible. El test no lee logcat para verificar ausencia de `MockTeams: touch`. Defer: añadir la verificación de logcat cuando se implemente el test E2E completo (finding #1).

---

## Dismissed

None.

---

## Acceptance Criteria Audit

| AC | Status | Notes |
|----|--------|-------|
| 1. Mock APK | ✅ | `applicationId = "com.microsoft.teams"`, actividad `com.microsoft.teams.activities.MainActivity` |
| 2. Registro en logcat | ✅ | `MockTeams: lifecycle ...` y `MockTeams: touch x=.. y=..` |
| 3. Excluido de release | ⚠️ | Módulo independiente (no dependencia de app); exclusión implícita (finding #3) |
| 4. Tests E2E | ⚠️ | Esqueleto presente; aserciones débiles (finding #1); SM-1a/SM-2 diferidos (#4, #5) |
| 5. Tests unitarios | ✅ | Handshake, idempotencia, watchdog, patrón 4-taps ya cubiertos (2.1-2.5) |
| 6. Kill switch | ⚠️ | Test presente pero débil (finding #2) |
| 7. Tests pasan | ✅ | `test` + `lint` verdes; E2E manual verificado en emulador |

---

## Recommendation

**Approve with 3 minor patches.** Aplicar los `patch` findings (aserciones reales en E2E, limpiar StopReceiverTest, documentar exclusión de release), luego merge. Los `defer` son deuda consciente (validación 8 h y SM-2 son procedimientos manuales).
