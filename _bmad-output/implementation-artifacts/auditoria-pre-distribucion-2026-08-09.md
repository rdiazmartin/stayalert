# Auditoría de riesgos — stayAlert v0.1.0 (pre-distribución)

**Fecha:** 2026-08-09
**Alcance:** `app/src/main` completo (manifest, overlay, watchdog, FGS, notifier, controller, launcher, monitor, settings, viewmodels)
**Motivo:** mitigar riesgos antes de instalar en móviles personales
**Método:** revisión adversarial del código

---

## Resumen ejecutivo

stayAlert no puede "brikear" un móvil moderno (no toca bootloader/firmware), pero tenía **1 riesgo crítico** que podía dejar el móvil inutilizable hasta reiniciar/desinstalar: **overlay huérfano si el proceso muere durante una sesión**. Los 3 hallazgos críticos (C1, C2, C3) fueron **resueltos y verificados en emulador** (2026-08-09). Quedan pendientes los hallazgos altos (A1–A4) y medios (M1–M5).

## Estado de los hallazgos

| ID | Hallazgo | Estado |
|----|----------|--------|
| C1 | Overlay huérfano si el proceso muere (FR-17) | ✅ **RESUELTO** — AppContainer + scope de aplicación + limpieza defensiva en onCreate; verificado con force-stop y rotación en emulador |
| C2 | `FLAG_SECURE` sin aviso al usuario | ✅ **RESUELTO** — aviso en ResponsibleUseNotice y en la notificación de sesión activa |
| C3 | `KEEP_SCREEN_ON` sin liberación garantizada | ✅ **RESUELTO** — cubierto por WMS (libera al destruir la vista) + limpieza defensiva de C1 |
| A1–A4 | Hallazgos altos | ⏳ Pendiente |
| M1–M5 | Hallazgos medios | ⏳ Pendiente |
| B1–B2 | Hallazgos bajos | ⏳ Pendiente (B1 resuelto parcialmente con AppContainer) |

## Hallazgos

### CRÍTICO

**C1. Overlay huérfano si el proceso muere (FR-17 no garantizado)**
- `SystemOverlayController.show()` añade la vista al `WindowManager` y guarda la referencia solo en memoria (`SystemOverlayController.kt:76-78`).
- Si el proceso de stayAlert es matado por el sistema o crashea con la sesión activa, el overlay negro a pantalla completa **permanece en pantalla**: el usuario no puede tocar nada debajo y el patrón 4-taps no responde (proceso muerto).
- Único remedio: reiniciar el móvil o desinstalar la app.
- El PRD FR-17 exige "no queda overlay huérfano tras kill", pero no hay limpieza en `onTaskRemoved`, ni watchdog de proceso, ni mecanismo de auto-recuperación.
- **Impacto:** móvil inutilizable hasta reinicio/desinstalación.

**C2. `FLAG_SECURE` en el overlay sin aviso al usuario**
- El overlay usa `FLAG_SECURE` (`SystemOverlayController.kt:71`): bloquea capturas de pantalla y grabaciones mientras la sesión está activa.
- No hay aviso de ello en el onboarding ni en la notificación.
- **Impacto:** el usuario no puede capturar/grabar durante la sesión sin saber por qué.

**C3. `KEEP_SCREEN_ON` sin liberación garantizada**
- `FLAG_KEEP_SCREEN_ON` se libera al ocultar el overlay, pero si el proceso muere con el overlay visible, la pantalla queda encendida indefinidamente.
- **Impacto:** drenaje agresivo de batería (combinado con C1, el móvil se queda con pantalla encendida + overlay).

### ALTO

**A1. FGS `START_NOT_STICKY` con notificación colgada**
- `PresenceForegroundService` devuelve `START_NOT_STICKY` (`PresenceForegroundService.kt:22`). Si el sistema mata el servicio, no se reinicia, pero la notificación "Sesión activa" puede quedar colgada sin servicio detrás.
- **Impacto:** kill switch fantasma (toca "Detener" y no pasa nada).

**A2. `StopReceiver` con callback estático frágil**
- `StopReceiver` reenvía a `MainActivity` vía `startActivity` (`StopReceiver.kt:12-16`). Si la app está en background, puede fallar silenciosamente o abrir la app sin querer.
- El kill switch de la notificación depende de que `MainActivity` esté viva.
- **Impacto:** kill switch poco fiable.

**A3. `UsageStatsForegroundMonitor` con ventana de 60 s**
- `queryEvents(now - 60_000, now)` (`UsageStatsForegroundMonitor.kt:16`): si la app objetivo no tiene eventos en los últimos 60 s (sesión larga sin interacción), devuelve `UNKNOWN`.
- `SessionCommandHandler.launchTarget()` aborta la sesión si el estado no es `FOREGROUND` (`SessionCommandHandler.kt:79-83`).
- **Impacto:** sesiones legítimas abortadas con "la app objetivo no está en primer plano".

**A4. `terminate()` no espera a que el overlay se oculte**
- `SessionController.terminate()` emite `HideOverlay` y pasa a `Inactiva` inmediatamente (`SessionController.kt:150-158`). Si `hide()` falla, el overlay queda visible con la sesión "terminada".
- **Impacto:** estado inconsistente (overlay visible sin sesión).

### MEDIO

**M1. `BatteryWarning` re-emitido cada 2 s**
- El watchdog emite `BatteryWarning` en cada poll (`SystemWatchdog.kt:81-83`) mientras la batería esté ≤ 15%. Hoy es no-op en `Aislada`, pero si se añade notificación al 15% (FR-16 pendiente), habrá spam.
- **Impacto:** futuro spam de notificaciones.

**M2. Paquete/actividad arbitrarios en Settings**
- El usuario puede escribir cualquier paquete/actividad. `IntentLauncher` falla limpio con `resolveActivity`, pero si el usuario escribe una app del sistema (p.ej. `com.android.settings`), stayAlert la lanzará y la cubrirá con el overlay.
- **Impacto:** comportamiento inesperado.

**M3. `allowBackup="true"`**
- El manifest tiene `allowBackup="true"` (`AndroidManifest.xml:17`). La configuración (incluida la app objetivo) se respalda en la nube de Google.
- **Impacto:** datos de configuración fuera del dispositivo; para una app de este tipo conviene `allowBackup="false"` o `dataExtractionRules`.

**M4. Región 4-taps fija (15% × 15%)**
- `PatternDetector` usa `regionFraction = 0.15f` (`PatternDetector.kt:6`). En pantallas grandes (tablets), 15% puede ser demasiado grande y provocar terminaciones accidentales.
- **Impacto:** sesiones terminadas por toques accidentales.

**M5. Watchdog no detecta crash de la app objetivo con overlay visible**
- Limitación documentada (`SystemWatchdog.kt:66-72`): con el overlay visible no se detecta `TargetCrashed` ni `TargetLeftForeground`. El PRD FR-14 exige terminación en < 5 s.
- **Impacto:** sesión activa con overlay sobre el launcher si la app objetivo crashea.

### BAJO

**B1. `delay()` real en `launchTarget()`**
- `delay(SessionConstants.LAUNCH_DELAY_MS)` (`SessionCommandHandler.kt:77`) en vez del `Clock` inyectable (AD-12). Lección de la retro (action item 4) no aplicada aquí.
- **Impacto:** tests menos deterministas.

**B2. `lateinit` + lazy circular en `MainActivity`**
- `sessionCommandHandler` es `lateinit` inicializado en `commandHandler` lazy (`MainActivity.kt:85, 111-123`). Si `sessionController` se accede antes que `commandHandler`, NPE.
- **Impacto:** NPE potencial por orden de inicialización.

---

## Recomendación de priorización

| Prioridad | Hallazgos | Acción |
|-----------|-----------|--------|
| **Bloquea distribución** | ~~C1, C2, C3~~ | ✅ Resueltos y verificados (2026-08-09) |
| **Alta** | A1, A2, A3, A4 | Arreglar en la próxima iteración |
| **Media** | M1–M5 | Planificar (algunos son mejoras de UX) |
| **Baja** | B1, B2 | Deuda técnica, resuelta parcialmente con AppContainer |

## Nota

- La validación manual SM-1a (mock ≥ 8 h) y SM-2 (0 toques) sigue pendiente y es independiente de estos hallazgos.
- El keystore de release (`app/keystore/`) no se versiona; respaldo necesario para futuras actualizaciones.
