# Fixes aplicados — stayAlert v0.1.1 (validación en dispositivo real)

**Fecha:** 2026-08-09
**Dispositivo:** Xiaomi 14T Pro (duchamp_global, HyperOS, Android 14)
**Contexto:** validación en móvil personal tras la release v0.1.0; los fixes se aplicaron y verificaron en el dispositivo vía adb.

---

## F1. Launcher de app objetivo: fallback a `getLaunchIntentForPackage`

**Síntoma:** "no se pudo abrir la app objetivo" con Teams real instalado y abierto.

**Causa raíz (diagnosticada con logcat):**
- La actividad hardcodeada `com.microsoft.teams.activities.MainActivity` no existe en Teams real (la real es `com.microsoft.skype.teams.Launcher`).
- El fallback a intent implícito (`ACTION_MAIN` + `CATEGORY_LAUNCHER` + `setPackage`) resolvía pero fallaba al ejecutar con `ActivityNotFoundException` — restricción de visibilidad de paquetes de Android 11+.

**Fix:** `IntentLauncher` (`app/src/main/java/com/stayalert/system/IntentLauncher.kt`) ahora:
1. Intenta la actividad explícita configurada.
2. Si no resuelve, usa `PackageManager.getLaunchIntentForPackage()` — la API oficial que devuelve el intent launcher real de cualquier paquete visible.

**Verificado:** sesión inicia correctamente con Teams real en el dispositivo.

## F2. Overlay: cobertura total de pantalla (barras del sistema)

**Síntoma:** la barra de estado (hora) y la barra de navegación (raya inferior) quedaban visibles sobre el overlay negro.

**Causa raíz:** en Android 11+ las ventanas overlay se recortan al área de la app; no pueden cubrir las barras del sistema por sí solas.

**Fix:** `SystemOverlayController` (`app/src/main/java/com/stayalert/system/SystemOverlayController.kt`):
- Quitado `FLAG_NOT_FOCUSABLE` (impedía ocultar las barras) y añadidos `FLAG_FULLSCREEN`, `FLAG_LAYOUT_NO_LIMITS`.
- Ocultación de barras vía `WindowInsetsController.hide(statusBars | navigationBars)` (API 30+) con fallback `SYSTEM_UI_FLAG_HIDE_NAVIGATION | IMMERSIVE_STICKY | FULLSCREEN` (API < 30).
- `setOnKeyListener` consume el botón back para que no cierre el overlay.

**Verificado:** pantalla completamente negra en el dispositivo.

## F3. Overlay: zona del notch/cutout

**Síntoma:** franja superior (zona de la cámara/notch) quedaba con luz tenue, no negra.

**Causa raíz:** el overlay no se extendía sobre el área del cutout.

**Fix:** `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` (API 28+) y re-ocultación de barras tras `addView`.

**Verificado:** franja del notch completamente negra en el dispositivo.

## F4. Patrón de salida: 1 tap en cualquier parte

**Síntoma:** el patrón 4-taps en la esquina superior derecha no terminaba la sesión.

**Causa raíz:** la lupa de accesibilidad de triple tap de Xiaomi (activada en el dispositivo) interfería con la secuencia de 4 taps.

**Decisión del usuario:** cambiar el patrón a **1 tap en cualquier parte de la pantalla** (simplicidad sobre seguridad anti-accidentes).

**Fix:** `PatternDetector` (`app/src/main/java/com/stayalert/domain/PatternDetector.kt`): `requiredTaps = 1`, `regionFraction = 1.0f` (toda la pantalla). Tests actualizados al nuevo contrato.

**Verificado:** un toque termina la sesión en el dispositivo.

## F5. Logging de diagnóstico (mantenido)

`SessionCommandHandler.launchTarget()` registra en logcat (tag `SessionCommandHandler`) el objetivo lanzado, el motivo de fallo y el estado de foreground — útil para diagnóstico futuro en dispositivo.

---

## Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `app/src/main/java/com/stayalert/system/IntentLauncher.kt` | F1: fallback a `getLaunchIntentForPackage` |
| `app/src/main/java/com/stayalert/system/SystemOverlayController.kt` | F2, F3: cobertura total + cutout |
| `app/src/main/java/com/stayalert/domain/PatternDetector.kt` | F4: 1 tap, toda la pantalla |
| `app/src/main/java/com/stayalert/system/SessionCommandHandler.kt` | F5: logging de diagnóstico |
| `app/src/test/java/com/stayalert/domain/PatternDetectorTest.kt` | F4: tests del nuevo contrato |
| `app/src/test/java/com/stayalert/system/IntentLauncherTest.kt` | F1: test de fallback |
| `app/build.gradle.kts` | versión 0.1.1 (versionCode 2) |
| `README.md` | documentación del nuevo patrón de salida |

## Pendiente

- Auditoría pre-distribución (`auditoria-pre-distribucion-2026-08-09.md`): los 3 hallazgos críticos (C1 overlay huérfano, C2 FLAG_SECURE, C3 KEEP_SCREEN_ON) siguen abiertos.
- Validación manual SM-1a (mock ≥ 8 h) y SM-2 (0 toques).
