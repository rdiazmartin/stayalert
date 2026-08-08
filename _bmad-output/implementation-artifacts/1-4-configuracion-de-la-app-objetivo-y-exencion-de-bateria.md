---
baseline_commit: 5e68a0e
---

# Story 1.4: Configuración de la app objetivo y exención de batería

Status: done

## Story

As a usuario,
I want configurar la app objetivo (paquete y actividad) y completar la exención de batería del fabricante,
so that la sesión se lanza contra la app correcta y el servicio no es eliminado por el sistema.

## Acceptance Criteria

1. **Edición de app objetivo** — El usuario puede editar paquete y actividad principal, con default `com.microsoft.teams` y su actividad principal (FR-4).
2. **Persistencia** — El valor persiste en DataStore (claves `target_package`, `target_activity`) (FR-4, AD-6).
3. **Validación de instalación** — El sistema valida que el paquete esté instalado (vía `<queries>` del manifest) y muestra error si no lo está (FR-4).
4. **Exención de batería** — El onboarding incluye el paso de exención de optimización de batería con acceso directo a Ajustes (FR-15).
5. **Estado de exención** — El estado de exención se muestra en la pantalla de configuración (FR-15).
6. **UI** — La UI cumple UX-DR5 (tarjeta surface-raised, filas label + valor) y UX-DR8 (microcopy en español, directo).
7. **Tests** — Unit tests: persistencia de paquete/actividad en DataStore; validación de instalación (paquete instalado/no instalado); estado de exención de batería.

## Tasks / Subtasks

- [x] Task 1: Extender `SettingsRepository` (AC: 2)
  - [x] 1.1 Añadir a la interfaz: `targetPackage: Flow<String>`, `targetActivity: Flow<String>`, `setTargetPackage(value)`, `setTargetActivity(value)`
  - [x] 1.2 Implementar en `DataStoreSettingsRepository` con claves `TARGET_PACKAGE`/`TARGET_ACTIVITY` (defaults `com.microsoft.teams` y su actividad)
  - [x] 1.3 Actualizar fakes en tests existentes
- [x] Task 2: Validación de instalación (AC: 3)
  - [x] 2.1 Crear `data/AppInstalledChecker.kt` (interfaz) con `isInstalled(packageName): Boolean`
  - [x] 2.2 Implementación `SystemAppInstalledChecker` vía `PackageManager.getPackageInfo` (requiere `<queries>` ya declarado)
  - [x] 2.3 Mostrar error en UI si el paquete no está instalado
- [x] Task 3: Exención de batería (AC: 4, 5)
  - [x] 3.1 Crear `data/BatteryOptimizationChecker.kt` (interfaz) con `isIgnoringBatteryOptimizations(): Boolean`
  - [x] 3.2 Implementación `SystemBatteryOptimizationChecker` vía `PowerManager.isIgnoringBatteryOptimizations(packageName)`
  - [x] 3.3 Acceso directo: `Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))`
  - [x] 3.4 Mostrar estado (exento/no exento) en la pantalla de configuración
- [x] Task 4: UI de configuración de app objetivo (AC: 1, 6)
  - [x] 4.1 Añadir sección "App objetivo" en `SettingsScreen` con campos de texto para paquete y actividad
  - [x] 4.2 Defaults `com.microsoft.teams` y su actividad principal
  - [x] 4.3 Persistir al editar (debounce o al perder foco)
  - [x] 4.4 Mostrar error de validación si el paquete no está instalado
- [x] Task 5: Tests (AC: 7)
  - [x] 5.1 Test persistencia paquete/actividad en `SettingsRepositoryTest`
  - [x] 5.2 Test `AppInstalledChecker` (instalado/no instalado)
  - [x] 5.3 Test `BatteryOptimizationChecker`
  - [x] 5.4 Verificar `./gradlew test` y `./gradlew lint`

## Dev Notes

### Contexto del producto

stayAlert lanza la app objetivo (default Teams) al iniciar sesión. El usuario debe poder configurar paquete y actividad, y el sistema debe validar que la app esté instalada. Además, el servicio en primer plano puede ser eliminado por la optimización de batería del fabricante; el onboarding guía al usuario a la exención.

### Stack

- Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36
- DataStore Preferences (ya en proyecto)
- DI manual por constructor (sin Hilt)
- JUnit4 + Robolectric 4.16 + Compose UI tests

### Arquitectura (spine — ADs vinculantes)

- `data/SettingsRepository` (interfaz) + `DataStoreSettingsRepository` — claves `target_package`, `target_activity` (AD-6)
- `data/AppInstalledChecker` — validación de instalación vía `PackageManager` (requiere `<queries>` del manifest, ya declarado en 1.1)
- `data/BatteryOptimizationChecker` — estado de exención vía `PowerManager.isIgnoringBatteryOptimizations`
- `ui/settings/SettingsScreen` — sección "App objetivo" + sección "Batería"
- `TargetAppLauncher` (AD-3) se implementará en Epic 2; aquí solo validación de instalación

### Tokens de diseño (DESIGN.md)

- Tarjeta: `surface-raised`, `rounded/md` (16dp), borde `border-hairline`
- Filas: label a la izquierda, valor/chevron a la derecha
- Campos de texto: `OutlinedTextField` con tema dark
- Estado exento: texto "Exento" (o punto verde `accent`)
- Estado no exento: texto "No exento" (o punto `ink-disabled`), fila clickeable
- Tipografía: label `bodyLarge`, valor `bodySmall`

### Microcopy (EXPERIENCE.md)

- "App objetivo" (título de sección)
- "Paquete" / "Actividad principal" (labels)
- "La app objetivo no está instalada." (error)
- "Exención de batería" (título de sección)
- "Exento" / "No exento" (estado)
- "Tócalo para abrir Ajustes." (acceso directo)
- Sin exclamaciones, sin jerga técnica, en español.

### Testing

- Robolectric: `PackageManager` con `ShadowPackageManager` para simular instalado/no instalado
- `PowerManager.isIgnoringBatteryOptimizations` con `ShadowPowerManager`
- Tests de persistencia con DataStore temporal (patrón de 1.2)

### Project Structure Notes

- Archivos existentes a tocar: `data/SettingsRepository.kt`, `data/DataStoreSettingsRepository.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `ui/MainActivity.kt`
- Nuevos archivos: `data/AppInstalledChecker.kt`, `data/SystemAppInstalledChecker.kt`, `data/BatteryOptimizationChecker.kt`, `data/SystemBatteryOptimizationChecker.kt`, tests

### Previous Story Intelligence

Stories 1.1-1.3 establecieron:
- `SettingsRepository` (interfaz) + `DataStoreSettingsRepository` (DataStore Preferences)
- `PermissionAuditor` (interfaz) + `SystemPermissionAuditor`
- `SettingsScreen` con tarjeta de permisos, re-auditoría en ON_RESUME, acceso directo a Ajustes
- `SettingsViewModel` con `permissions` StateFlow
- Navegación simple con `mutableStateOf` en `MainActivity`
- Patrón de tests: Robolectric `@Config(sdk = [34])`, fakes de interfaces, DataStore temporal
- Deferred de 1.2: claves `TARGET_PACKAGE`/`TARGET_ACTIVITY` se usan en esta story

### Git Intelligence

Últimos commits:
- `5e68a0e feat(1.3): auditoria y guiado de permisos con acceso a Ajustes`
- `07d4b2e feat(1.2): aviso de uso responsable con persistencia en DataStore`
- `02b9935 fix(1.1): parches de code review - edge-to-edge, manifest, gitignore`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 1.4]
- [Source: `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md` — §4.1 FR-4, FR-15]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — AD-6, AD-3, Structural Seed]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/DESIGN.md` — Components, Colors]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md` — Voice and Tone]

## Dev Agent Record

### Agent Model Used

(opencode)

### Debug Log References

- `SettingsRepository` extendido con `targetPackage`/`targetActivity` (defaults Teams); fakes de tests actualizados
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` añadido al manifest — sin él, el intent de exención de batería no abría Ajustes (verificado en emulador)
- Smoke test emulador: configuración muestra permisos + app objetivo (defaults Teams) + error "no instalada" (correcto, Teams no está en el emulador) + batería "No exento"; tap en batería abre `RequestIgnoreBatteryOptimizations` de Ajustes
- **Repo remoto (usuario):** subir el repositorio a `https://github.com/rdiazmartin/stayalert` más tarde (aún no configurado como remote)

### Completion Notes List

- `SettingsRepository` (interfaz) extendido: `targetPackage`, `targetActivity`, `setTargetPackage`, `setTargetActivity`
- `DataStoreSettingsRepository` con defaults `DEFAULT_TARGET_PACKAGE`/`DEFAULT_TARGET_ACTIVITY` (Teams)
- `AppInstalledChecker` (interfaz) + `SystemAppInstalledChecker` (PackageManager, NameNotFoundException → false)
- `BatteryOptimizationChecker` (interfaz) + `SystemBatteryOptimizationChecker` (PowerManager)
- `SettingsViewModel` ampliado: `targetPackage`, `targetActivity`, `batteryExempt`, `targetInstalled` StateFlows; `refresh()` audita todo
- `SettingsScreen` con secciones: Permisos, App objetivo (OutlinedTextField paquete/actividad + error de instalación), Batería (estado + acceso directo)
- Manifest: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- Tests: `SettingsRepositoryTest` (+3), `SettingsViewModelTest` (4, reescrito), `SystemAppInstalledCheckerTest` (2), `SystemBatteryOptimizationCheckerTest` (1)
- Build/test/lint verdes; smoke test en emulador OK

### File List

- `app/src/main/AndroidManifest.xml` (modificado)
- `app/src/main/java/com/stayalert/data/SettingsRepository.kt` (modificado)
- `app/src/main/java/com/stayalert/data/DataStoreSettingsRepository.kt` (modificado)
- `app/src/main/java/com/stayalert/data/AppInstalledChecker.kt` (nuevo)
- `app/src/main/java/com/stayalert/data/SystemAppInstalledChecker.kt` (nuevo)
- `app/src/main/java/com/stayalert/data/BatteryOptimizationChecker.kt` (nuevo)
- `app/src/main/java/com/stayalert/data/SystemBatteryOptimizationChecker.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/settings/SettingsViewModel.kt` (modificado)
- `app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt` (modificado)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (modificado)
- `app/src/test/java/com/stayalert/data/SettingsRepositoryTest.kt` (modificado)
- `app/src/test/java/com/stayalert/data/SystemAppInstalledCheckerTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/data/SystemBatteryOptimizationCheckerTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/ui/settings/SettingsViewModelTest.kt` (modificado)
- `app/src/test/java/com/stayalert/ui/MainViewModelTest.kt` (modificado)
- `app/src/test/java/com/stayalert/ui/MainScreenTest.kt` (modificado)

---

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `setTargetPackage`/`setTargetActivity` escriben en DataStore en cada tecla [`app/src/main/java/com/stayalert/ui/settings/SettingsViewModel.kt:381-391`] — fixed
- [x] [Review][Patch] `SettingsScreen` no es scrollable — contenido puede desbordarse [`app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt:71-76`] — fixed
- [x] [Review][Patch] `SystemAppInstalledChecker` no distingue apps deshabilitadas [`app/src/main/java/com/stayalert/data/SystemAppInstalledChecker.kt:124`] — fixed

### defer

- [x] [Review][Defer] `DEFAULT_TARGET_ACTIVITY` hardcodeado como constante top-level [`app/src/main/java/com/stayalert/data/DataStoreSettingsRepository.kt:58`] — deferred, pre-existing (centralizar con `TargetAppLauncher` en story 2.1)
- [x] [Review][Defer] `SettingsScreen` crece en complejidad — extraer secciones [`app/src/main/java/com/stayalert/ui/settings/SettingsScreen.kt`] — deferred, pre-existing (cuando se añada más configuración)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-1.4/findings-report.md`
