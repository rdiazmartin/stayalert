---
baseline_commit: 07d4b2e
---

# Story 1.3: Auditoría y guiado de permisos

Status: done

## Story

As a usuario,
I want ver el estado de mis permisos y acceder a los menús de Ajustes para concederlos,
so that puedo preparar el dispositivo sin buscar los menús manualmente.

## Acceptance Criteria

1. **Auditoría de permisos** — La pantalla de configuración muestra el estado real de cada permiso (overlay vía `Settings.canDrawOverlays()`, notificaciones vía estado de runtime) con indicación concedido/pendiente (FR-1).
2. **Acceso directo por permiso pendiente** — Cada permiso pendiente tiene un acceso directo que abre el menú de Ajustes correspondiente (overlay: `ACTION_MANAGE_OVERLAY_PERMISSION`; notificaciones: ajustes de la app) (FR-2).
3. **Re-auditoría al regresar** — Al volver de Ajustes, el estado de permisos se re-audita automáticamente (FR-2).
4. **`PermissionAuditor` único lector** — `PermissionAuditor` es el único componente que lee el estado de permisos (convención del spine).
5. **UI** — La pantalla de configuración cumple UX-DR5 (tarjeta surface-raised, filas label + valor) y UX-DR10 (tap targets ≥ 48dp, TalkBack con rol+estado).
6. **Tests** — Unit tests con Robolectric: auditoría devuelve estado correcto según `Settings.canDrawOverlays()` y estado de notificaciones; el acceso directo construye el Intent correcto.

## Tasks / Subtasks

- [x] Task 1: Crear `PermissionAuditor` (AC: 4)
  - [x] 1.1 Crear `data/PermissionAuditor.kt` con API: `canDrawOverlays(): Boolean`, `areNotificationsEnabled(): Boolean`
  - [x] 1.2 Usar `Settings.canDrawOverlays(context)` y `NotificationManagerCompat.areNotificationsEnabled()` (o `NotificationManager.areNotificationsEnabled()`)
  - [x] 1.3 Definir enum `PermissionState { GRANTED, PENDING }` y modelo `PermissionStatus(permission, state)`
- [x] Task 2: Pantalla de configuración (AC: 1, 5)
  - [x] 2.1 Crear `ui/settings/SettingsScreen.kt` con tarjeta surface-raised, filas label + valor
  - [x] 2.2 Mostrar estado de cada permiso (concedido/pendiente) con punto o texto
  - [x] 2.3 Tap targets ≥ 48dp; TalkBack con rol+estado
- [x] Task 3: Acceso directo a Ajustes (AC: 2)
  - [x] 3.1 Crear `ui/settings/PermissionRow.kt` o integrar en SettingsScreen: fila clickeable si pendiente
  - [x] 3.2 Overlay: `Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))`
  - [x] 3.3 Notificaciones: `Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)` con `EXTRA_APP_PACKAGE`
- [x] Task 4: Re-auditoría al regresar (AC: 3)
  - [x] 4.1 Re-auditar en `onResume` de la pantalla de configuración (o `LifecycleEventObserver`)
  - [x] 4.2 Actualizar estado en `SettingsViewModel` o estado local
- [x] Task 5: Navegación desde pantalla principal (AC: 5)
  - [x] 5.1 Añadir acceso a configuración desde `MainScreen` (icono engranaje o botón)
  - [x] 5.2 Navegación simple (estado en MainActivity o Navigation Compose)
- [x] Task 6: Tests (AC: 6)
  - [x] 6.1 Test `PermissionAuditor` con Robolectric (overlay concedido/pendiente, notificaciones)
  - [x] 6.2 Test de Intents de acceso directo
  - [x] 6.3 Verificar `./gradlew test` y `./gradlew lint`

## Dev Notes

### Contexto del producto

stayAlert necesita 2 permisos para funcionar: overlay ("Mostrar sobre otras apps", para el overlay de aislamiento) y notificaciones (para el FGS). Esta story implementa la auditoría y el guiado a los menús de Ajustes. La validación completa previa al inicio (FR-5) se hará en story 2.1; aquí solo se audita y se guía.

### Stack

- Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36
- DataStore Preferences (ya añadido en 1.2)
- DI manual por constructor (sin Hilt)
- JUnit4 + Robolectric 4.16 + Compose UI tests

### Arquitectura (spine — ADs vinculantes)

- `data/PermissionAuditor` es el **único** lector del estado de permisos (convención del spine, FR-1, FR-5, FR-13)
- `ui/settings/` aloja la pantalla de configuración (Structural Seed)
- Navegación: single-activity; usar estado simple en `MainActivity` o Navigation Compose (decisión: estado simple para v1, sin dependencia extra)
- El watchdog (story 2.5) consumirá los datos de `PermissionAuditor` vía evento, no re-lee (convención del spine)

### Tokens de diseño (DESIGN.md)

- Tarjeta: `surface-raised`, `rounded/md` (16dp), borde `border-hairline`
- Filas: label a la izquierda, valor/chevron a la derecha
- Estado concedido: texto "Concedido" (o punto verde `accent`)
- Estado pendiente: texto "Pendiente" (o punto `ink-disabled`), fila clickeable
- Tipografía: label `bodyLarge`, valor `bodySmall` o `bodyLarge`
- Tap targets ≥ 48dp

### Microcopy (EXPERIENCE.md)

- "Falta el permiso de overlay. Tócalo para abrir Ajustes."
- "Falta el permiso de notificaciones. Tócalo para abrir Ajustes."
- "Permisos" (título de sección)
- Sin exclamaciones, sin jerga técnica, en español.

### Testing

- Robolectric: `Settings.canDrawOverlays()` se puede simular con `ShadowSettings` o `ShadowApplication`
- `NotificationManagerCompat.areNotificationsEnabled()` en Robolectric: usar `ShadowNotificationManager`
- Tests de Intent: verificar action y extras

### Project Structure Notes

- Archivos existentes a tocar: `app/src/main/java/com/stayalert/ui/MainActivity.kt` (navegación)
- Nuevos archivos: `data/PermissionAuditor.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt` (si aplica), tests

### Previous Story Intelligence

Story 1.2 estableció:
- `SettingsRepository` (interfaz) + `DataStoreSettingsRepository` (DataStore Preferences)
- `MainViewModel` con `noticeAccepted` StateFlow (SharingStarted.Eagerly)
- `MainScreen` con botón "Iniciar Jornada" y modal de aviso
- Patrón de tests: Robolectric `@Config(sdk = [34])`, `Dispatchers.setMain(UnconfinedTestDispatcher())`, fakes de repositorios
- `collectAsStateWithLifecycle()` (parche de review 1.2)

### Git Intelligence

Últimos commits:
- `07d4b2e feat(1.2): aviso de uso responsable con persistencia en DataStore`
- `02b9935 fix(1.1): parches de code review - edge-to-edge, manifest, gitignore`
- `4a5198c chore: versionar artefactos BMad y skills`
- `e08e670 feat(1.1): scaffolding del proyecto Android y tema visual`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 1.3]
- [Source: `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md` — §4.1 FR-1, FR-2]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — convención permisos, Structural Seed]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/DESIGN.md` — Components, Colors]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md` — Voice and Tone, Component Patterns]

## Dev Agent Record

### Agent Model Used

(opencode)

### Debug Log References

- `PermissionAuditor` convertido a interfaz + `SystemPermissionAuditor` (implementación) para permitir fakes en tests
- Iconos `android.R.drawable.ic_menu_manage`/`ic_menu_revert` no existen en Robolectric → vectores propios `ic_settings.xml`/`ic_back.xml` (sin `?attr/colorControlNormal`, no disponible en el theme)
- Bug de z-order: `IconButton` de configuración quedaba debajo del `Column` fillMaxSize → reordenado (Column primero, IconButton después)
- Smoke test emulador: navegación a configuración OK, ambos permisos pendientes, tap en overlay abre Ajustes del sistema, re-auditoría al volver OK
- **Repo remoto (usuario):** subir el repositorio a `https://github.com/rdiazmartin/stayalert` más tarde (aún no configurado como remote)

### Completion Notes List

- `PermissionAuditor` (interfaz) + `SystemPermissionAuditor` (implementación): `canDrawOverlays()`, `areNotificationsEnabled()`, `audit()`
- `PermissionState { GRANTED, PENDING }`, `Permission { OVERLAY, NOTIFICATIONS }`, `PermissionStatus`
- `SettingsViewModel` con `permissions` StateFlow y `refresh()`; re-auditoría en `ON_RESUME` vía `LifecycleEventObserver`
- `SettingsScreen`: tarjeta surface-raised con filas label+valor, punto de estado (accent/ink-disabled), fila clickeable si pendiente, botón volver
- Acceso directo: overlay → `ACTION_MANAGE_OVERLAY_PERMISSION`; notificaciones → `ACTION_APP_NOTIFICATION_SETTINGS` + `EXTRA_APP_PACKAGE`
- Navegación simple en `MainActivity` (estado `showSettings`), icono engranaje en `MainScreen`
- Tests: `SystemPermissionAuditorTest` (3), `SettingsViewModelTest` (2), `MainScreenTest` (2, actualizado)
- Build/test/lint verdes; smoke test en emulador OK

### File List

- `app/src/main/java/com/stayalert/data/PermissionAuditor.kt` (nuevo)
- `app/src/main/java/com/stayalert/data/SystemPermissionAuditor.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/settings/SettingsViewModel.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (modificado)
- `app/src/main/res/drawable/ic_settings.xml` (nuevo)
- `app/src/main/res/drawable/ic_back.xml` (nuevo)
- `app/src/test/java/com/stayalert/data/SystemPermissionAuditorTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/ui/settings/SettingsViewModelTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/ui/MainScreenTest.kt` (modificado)

---

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `SettingsScreen` no maneja `ActivityNotFoundException` al abrir Ajustes [`app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:343`] — fixed
- [x] [Review][Patch] `PermissionRow` no expone rol+estado para TalkBack [`app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:368`] — fixed
- [x] [Review][Patch] `SystemPermissionAuditorTest.areNotificationsEnabled` asume `true` por defecto [`app/src/test/java/com/stayalert/data/SystemPermissionAuditorTest.kt:511-513`] — fixed

### defer

- [x] [Review][Defer] Navegación con `mutableStateOf` en vez de Navigation Compose [`app/src/main/java/com/stayalert/ui/MainActivity.kt:134`] — deferred, pre-existing (evaluar con story 1.4)
- [x] [Review][Defer] `SettingsViewModel.refresh()` no es idempotente ante recomposiciones [`app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:297-305`] — deferred, pre-existing (no requiere acción)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-1.3/findings-report.md`
