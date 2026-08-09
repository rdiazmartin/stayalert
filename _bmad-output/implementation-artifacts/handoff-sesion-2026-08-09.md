# Handoff de Sesión — stayAlert

**Fecha:** 2026-08-09
**Estado:** Proyecto completo (Epic 1 + Epic 2, 10 stories, todas `done`)

---

## Estado del proyecto

| Story | Status | Commit |
|-------|--------|--------|
| 1.1 Scaffolding y tema visual | done | `e08e670` + `02b9935` (parches) |
| 1.2 Aviso de uso responsable | done | `07d4b2e` |
| 1.3 Auditoría y guiado de permisos | done | `5e68a0e` |
| 1.4 App objetivo y exención de batería | done | `ceb2013` |
| 2.1 Núcleo de sesión | done | `1a15c6f` |
| 2.2 Despliegue secuencial y overlay | done | `b20e670` |
| 2.3 Patrón de salida | done | `db5c57f` |
| 2.4 FGS y notificaciones | done | `0f7446a` |
| 2.5 Watchdog de sesión | done | `4a61c5d` |
| 2.6 Mock de Teams y E2E | done | `0cbbc13` |

- **Epic 1:** done · **Epic 2:** done
- **Ramas:** `develop` (activa, pusheada) y `main` (creada al inicio, **DESACTUALIZADA** — apunta a `ed8de8c`)
- **Remote:** `origin` → `git@github.com:rdiazmartin/stayalert.git` (SSH)

## Pendientes para la próxima sesión

1. **Actualizar `main` en GitHub** — `git branch -f main develop && git push -f origin main` (o merge)
2. **Retrospectivas opcionales** — `epic-1-retrospective` y `epic-2-retrospective` (status `optional` en sprint-status.yaml)
3. **Validación manual SM-1a** — mock permanece resumed ≥ 8 h (procedimiento en README)
4. **`.opencode/` sin versionar** — config del entorno; decidir si se versiona

## Preferencias de sesión (usuario)

- **Emulador SIEMPRE con ventana gráfica** — NO usar `-no-window`; el usuario quiere ver la app
- **AVD dedicado:** `stayalert_avd` (Pixel 5, API 34, google_apis x86_64) — NO usar `ice_test_avd`
- **Repo remoto:** `https://github.com/rdiazmartin/stayalert` (SSH, usuario `rdiazmartin`)
- **Git identity local:** `Roberto Diaz <rdiazmartin@gmail.com>`
- **Commits separados:** código de la app vs artefactos BMad

## Arquitectura implementada

```
app/src/main/java/com/stayalert/
  ui/        Compose: MainActivity, MainScreen, SettingsScreen, ResponsibleUseNotice, viewmodels
  domain/    SessionController (único propietario del estado), SessionState/Event/Command,
             TerminationReason, ValidationFailure, SessionValidator, Clock, PatternDetector,
             TargetAppLauncher, ForegroundMonitor, OverlayController, Watchdog (interfaces)
  data/      SettingsRepository (DataStore), PermissionAuditor, AppInstalledChecker,
             BatteryOptimizationChecker, Notifier (interfaces + System* implementaciones)
  system/    IntentLauncher, UsageStatsForegroundMonitor, SystemOverlayController,
             SessionCommandHandler, PresenceForegroundService, StopReceiver, SystemWatchdog
mock-teams/  APK de prueba con applicationId com.microsoft.teams (solo debug)
```

### Flujo de sesión (ADs del spine)

1. `MainScreen` → `startSession()` → `SessionValidator` (FR-5: permisos + aviso + app instalada)
2. `SessionController` → `Lanzando` → `LaunchTarget` → `SessionCommandHandler`
3. Handler: `IntentLauncher.launch()` → delay 1 s → `ForegroundMonitor` confirma → `SystemOverlayController.show()`
4. Overlay: `TYPE_APPLICATION_OVERLAY`, flags NOT_FOCUSABLE+LAYOUT_IN_SCREEN+KEEP_SCREEN_ON+SECURE, negro, clickable (patrón 4-taps)
5. `OverlayShown` → `Aislada` → `StartFgs` (PresenceForegroundService specialUse) + `StartWatchdog`
6. Watchdog: polling 2 s; pantalla apagada/permiso revocado/overlay ausente/batería → eventos
7. Terminación: patrón 4-taps (esquina sup-der 15%×15%, ventana 500 ms), notificación Detener, o watchdog → `terminate(reason)` → HideOverlay+StopFgs+StopWatchdog → `Inactiva` + notificación "Sesión terminada: {motivo}"

### Decisiones técnicas clave (lecciones aprendidas)

- **Watchdog no comprueba foreground con overlay visible** — el sistema reporta la app objetivo como stopped cuando el overlay la cubre; comprobarlo causaba falsas terminaciones (fix en 2.6)
- **Overlay debe ser `isClickable = true`** — sin esto no recibe toques (fix en 2.6)
- **Mock usa `com.microsoft.teams.activities.MainActivity`** — alineado con el default de stayAlert (fix en 2.6)
- **`SharingStarted.Eagerly`** en StateFlows de ViewModels — `.value` refleja el estado real sin suscriptores (necesario para tests)
- **Tests Robolectric:** `@Config(sdk = [34])` (targetSdk 36 no soportado); `Dispatchers.setMain(UnconfinedTestDispatcher())`; `backgroundScope` para jobs del controller/watchdog
- **`ShadowSettings.setCanDrawOverlays(true)`** — Robolectric devuelve false por defecto
- **`ShadowBatteryManager.setIntProperty(BATTERY_PROPERTY_CAPACITY, ...)`** — no existe `setLevel`
- **Interfaces + implementaciones `System*`** — patrón para testabilidad (SettingsRepository, PermissionAuditor, OverlayController, Watchdog, Notifier, etc.)
- **`StopReceiver` reenvía a MainActivity vía intent** (no callback estático) — evita fugas de memoria
- **`START_NOT_STICKY`** en el FGS — evita reinicio sin sesión activa
- **Notificación única:** `SystemNotifier.buildSessionNotification()` compartida entre Notifier y Service
- **`LaunchError` extiende `Exception`** — para usarlo con `Result.failure`
- **Referencia circular en MainActivity** (controller ↔ handler) resuelta con `lateinit` + lazy auto-asignado

## Deferred work (ledger)

Ver `_bmad-output/implementation-artifacts/deferred-work.md` — 15+ entradas, las más relevantes:
- `SurfaceOverlay` mapeado a `surfaceContainerLowest` (tema) — resolver con overlay
- `Warning` token no expuesto en el tema
- `themes.xml` splash light antes de Compose
- `SystemClock` acopla `domain/` a Android — mover a `system/`
- `BatteryWarning` no emite aviso de notificación (FR-16) — requiere deduplicación
- Watchdog no distingue `TargetCrashed` de `TargetLeftForeground`
- Navegación con `mutableStateOf` — evaluar Navigation Compose
- `MainActivity` wiring frágil — considerar `AppContainer`

## Emulador (guía completa)

### Reglas de oro

1. **SIEMPRE con ventana gráfica** — el usuario quiere ver la app en el host. NUNCA usar `-no-window`.
2. **AVD dedicado `stayalert_avd`** (Pixel 5, API 34, google_apis x86_64). NO usar `ice_test_avd` (de otro propósito).
3. El emulador puede tardar 30-60 s en bootear; esperar `sys.boot_completed = 1`.

### Crear el AVD (una vez)

```bash
avdmanager create avd -n stayalert_avd -k "system-images;android-34;google_apis;x86_64" -d pixel_5
```

### Lanzar (con ventana)

```bash
nohup emulator -avd stayalert_avd -no-audio -no-boot-anim -gpu swiftshader_indirect > /tmp/opencode/emulator.log 2>&1 &
adb wait-for-device
# Esperar boot: adb shell getprop sys.boot_completed → "1"
```

### Matar el emulador

```bash
adb emu kill
```

### Instalar y preparar la app

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r mock-teams/build/outputs/apk/debug/mock-teams-debug.apk
adb shell appops set com.stayalert SYSTEM_ALERT_WINDOW allow
adb shell pm grant com.stayalert android.permission.POST_NOTIFICATIONS
adb shell am start -n com.stayalert/.ui.MainActivity
```

### E2E manual (flujo completo)

1. Abrir stayAlert → pulsar "Iniciar Jornada" (botón centrado, ~y=1275 en 1080×2400)
2. El mock de Teams se abre en primer plano (verificar `adb logcat -s MockTeams:I` → `lifecycle resumed`)
3. Tras 1 s, el overlay negro cubre la pantalla (verificar: `screencap` devuelve 0 bytes por FLAG_SECURE)
4. La sesión persiste (el watchdog NO la termina — fix 2.6)
5. **Patrón de salida:** 4 taps en esquina superior derecha (x≈1000, y≈200 — NO y=100, cae en la status bar)
6. El mock vuelve a primer plano (sesión terminada)

### Coordenadas útiles (Pixel 5, 1080×2400)

- Botón "Iniciar Jornada": (540, 1275)
- Icono configuración (engranaje): (992, 150)
- Región patrón de salida (15%×15% sup-der): x ≥ 918, y ≤ 360 → usar (1000, 200)
- Fila "Permiso de overlay" en configuración: (500, 300)

### Troubleshooting

- **Screencap vacío (0 bytes)** → el overlay FLAG_SECURE está visible (comportamiento esperado)
- **Mock pasa a `stopped`** → normal cuando el overlay lo cubre; el watchdog ya no lo interpreta como anomalía (fix 2.6)
- **Patrón no detectado** → verificar que el tap no cae en la status bar (y=100); usar y=200
- **"Sesión terminada: no se pudo abrir la app objetivo"** → el mock no está instalado o el paquete/actividad no coincide (`com.microsoft.teams.activities.MainActivity`)
- **Build falla transitoriamente** → memoria del daemon Gradle (512 MiB); reintentar o subir `org.gradle.jvmargs` en gradle.properties

## Comandos útiles

```bash
./gradlew test            # unit tests (44 tests)
./gradlew lint            # lint (app + mock-teams)
./gradlew assembleDebug   # APK debug
./gradlew :mock-teams:assembleDebug  # mock APK
./gradlew connectedDebugAndroidTest  # tests instrumentados (requiere emulador)

# E2E manual (ver sección "Emulador" arriba para la guía completa)
adb install -r mock-teams/build/outputs/apk/debug/mock-teams-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell appops set com.stayalert SYSTEM_ALERT_WINDOW allow
adb shell pm grant com.stayalert android.permission.POST_NOTIFICATIONS
adb shell am start -n com.stayalert/.ui.MainActivity
# Pulsar "Iniciar Jornada" → mock se abre → overlay negro → 4 taps esquina sup-der (x≈1000, y≈200)
```

## Notas de sesión

- El usuario pidió: "para las próximas sesiones recuerda que debes lanzar el emulador conectandose al entorno grafico del host de tal modo que yo pueda ver la aplicacion tambien"
- El usuario creó el repo en GitHub: `https://github.com/rdiazmartin/stayalert`
- Flujo BMad completo: create-story → dev-story → code-review (3 capas: Blind Hunter, Edge Case Hunter, Acceptance Auditor) → parches → commit → push
- Todas las stories pasaron code review con 2-3 parches cada una, todos aplicados
