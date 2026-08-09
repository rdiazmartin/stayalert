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

## Comandos útiles

```bash
./gradlew test            # unit tests (44 tests)
./gradlew lint            # lint (app + mock-teams)
./gradlew assembleDebug   # APK debug
./gradlew :mock-teams:assembleDebug  # mock APK
./gradlew connectedDebugAndroidTest  # tests instrumentados (requiere emulador)

# Emulador (con ventana SIEMPRE)
emulator -avd stayalert_avd -no-audio -no-boot-anim -gpu swiftshader_indirect

# E2E manual
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
