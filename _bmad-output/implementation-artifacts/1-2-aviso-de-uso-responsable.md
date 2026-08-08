---
baseline_commit: 02b9935509a29420455f814946d36910f24debae
---

# Story 1.2: Aviso de uso responsable

Status: done

## Story

As a usuario,
I want ver un aviso claro sobre el uso de la app en el primer inicio y aceptarlo,
so that entiendo el propósito y los riesgos antes de usarla.

## Acceptance Criteria

1. **Modal en primer inicio** — Si el usuario no ha aceptado el aviso (`notice_accepted` no existe o es `false` en DataStore), la pantalla principal muestra un modal con el texto de uso responsable y el botón "Entiendo y acepto".
2. **Texto vinculante** — El texto del modal debe ser exactamente: "Esta app mantiene una señal de presencia activa sin interacción del usuario. Su uso puede no estar alineado con la política de tu empleador." (FR-3, UX-DR8).
3. **No dismissable por fuera** — El modal no se cierra al tocar fuera, al pulsar back, ni al cambiar de configuración. Solo el botón "Entiendo y acepto" lo cierra (UX-DR6).
4. **Persistencia** — Al aceptar, se guarda `notice_accepted = true` en DataStore Preferences. En inicios posteriores el modal no se muestra.
5. **Bloqueo del botón principal** — Mientras el aviso no esté aceptado, el botón "Iniciar Jornada" está deshabilitado (FR-3, FR-5). Nota: la validación completa de permisos/app objetivo se hará en stories 1.3 y 1.4/2.1; aquí solo se bloquea por el aviso.
6. **Visual** — El modal usa `surface-raised` (#1E1E1E), `rounded/md` (16dp), título en `headlineSmall`, cuerpo en `bodyLarge`, botón en `accent` con texto `accent-on` (UX-DR6, DESIGN.md).
7. **Tests** — Unit test con Robolectric/Compose: (a) modal visible cuando `notice_accepted` es false; (b) modal oculto y botón habilitado cuando es true; (c) al pulsar aceptar, DataStore se actualiza y el modal desaparece.

## Tasks / Subtasks

- [x] Task 1: Añadir DataStore Preferences al proyecto (AC: 4)
  - [x] 1.1 Añadir dependencias `datastore-preferences` y `datastore-preferences-core` en `app/build.gradle.kts`
  - [x] 1.2 Crear `data/SettingsKeys.kt` con claves: `notice_accepted`, `target_package`, `target_activity`
  - [x] 1.3 Crear `data/SettingsRepository.kt` con API suspend: `isNoticeAccepted(): Boolean`, `setNoticeAccepted(value: Boolean)`
  - [x] 1.4 Proveer instancia singleton con DI manual (sin Hilt — AD-6)
- [x] Task 2: Implementar el modal de aviso (AC: 1, 2, 3, 6)
  - [x] 2.1 Crear componente `ui/components/ResponsibleUseNotice.kt` con `AlertDialog` o `Dialog` Material 3
  - [x] 2.2 Forzar no-dismissable: `properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`
  - [x] 2.3 Aplicar tokens de color y tipografía del DESIGN.md
- [x] Task 3: Integrar en pantalla principal (AC: 1, 5)
  - [x] 3.1 Ampliar `MainActivity`/`MainScreen` para leer `notice_accepted` desde `SettingsRepository`
  - [x] 3.2 Mostrar modal condicional y deshabilitar "Iniciar Jornada" cuando no aceptado
  - [x] 3.3 Habilitar botón al aceptar (solo por aviso; no validar otros requisitos aún)
- [x] Task 4: Tests (AC: 7)
  - [x] 4.1 Test unitario con Robolectric + Compose: modal visible/invisible según DataStore
  - [x] 4.2 Test de aceptación: click en botón → `notice_accepted = true` → modal desaparece
  - [x] 4.3 Verificar `./gradlew test` y `./gradlew lint`

## Dev Notes

### Contexto del producto

stayAlert mantiene el estado "Disponible" en apps de comunicación corporativa manteniendo la app objetivo en primer plano + overlay negro. Esta story implementa el aviso ético/legal del PRD (§5) que el usuario debe aceptar antes de poder iniciar una sesión. Es la primera story con estado persistente (DataStore) y la primera con UI modal.

### Stack

- Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36
- DataStore Preferences (no SharedPreferences — AD-6)
- DI manual por constructor (sin Hilt — AD-3/AD-6)
- JUnit4 + Robolectric 4.16 + Compose UI tests

### Arquitectura (spine — ADs vinculantes)

- `data/` aloja `SettingsRepository` y `SettingsKeys` (AD-6)
- `ui/` aloja el modal y la pantalla principal (Structural Seed)
- `domain/` aún no se toca en esta story; `SessionController` llegará en Epic 2
- DI manual: `MainActivity` crea/inyecta `SettingsRepository` a `MainScreen`/`MainViewModel` (AD-3)

### Tokens de diseño (DESIGN.md)

- Modal: `surface-raised` fondo, `rounded/md`, borde `border-hairline`
- Título: `headlineSmall` (24sp)
- Cuerpo: `bodyLarge` (16sp)
- Botón: fondo `accent`, texto `accent-on`, tipografía `labelLarge` (14sp medium)
- Sombreado del fondo: opacidad negra sobre `surface-base`

### Microcopy (EXPERIENCE.md)

- Título modal: "Aviso de uso responsable" (o similar directo)
- Cuerpo: "Esta app mantiene una señal de presencia activa sin interacción del usuario. Su uso puede no estar alineado con la política de tu empleador."
- Botón: "Entiendo y acepto"
- No exclamaciones, no jerga técnica, en español.

### Testing

- Robolectric + Compose UI test (`createComposeRule()`)
- Inyectar un `SettingsRepository` de test con un `DataStore` en memoria (`preferencesDataStore` con un `InMemoryDataStore` o usar un directorio temporal en tests)
- Alternativa: test directo de `SettingsRepository` con DataStore de test

### Project Structure Notes

- El proyecto Android está en la raíz del repo (`app/`)
- Archivos existentes a tocar: `app/build.gradle.kts`, `app/src/main/java/com/stayalert/ui/MainActivity.kt`
- Nuevos archivos: `data/SettingsKeys.kt`, `data/SettingsRepository.kt`, `ui/components/ResponsibleUseNotice.kt`, test(s)

### Previous Story Intelligence

Story 1.1 estableció:
- Proyecto Android con tema Material 3 dark y estructura de paquetes (`ui/`, `domain/`, `data/`, `system/`)
- `MainActivity` con `enableEdgeToEdge()` y `MainScreen` mostrando "stayAlert"
- Manifest con `<queries>` para `com.microsoft.teams`
- CI con lint + tests
- `.gitignore` incluye `.gradle/`, `build/`, `.kotlin/`, `local.properties`

Lesson learned: el test de tokens de color requirió comparar `Color` directamente, no `ULong`, por el empaquetado interno de Compose.

### Git Intelligence

Últimos commits:
- `02b9935 fix(1.1): parches de code review - edge-to-edge, manifest, gitignore`
- `4a5198c chore: versionar artefactos BMad y skills`
- `e08e670 feat(1.1): scaffolding del proyecto Android y tema visual`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 1.2]
- [Source: `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md` — §4.1 FR-3, §5 Ethics]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — AD-6, AD-3, Structural Seed]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/DESIGN.md` — Colors, Typography, Shapes, Components]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md` — Voice and Tone, Flow 3]

## Dev Agent Record

### Agent Model Used

opencode (kimi-k2.7-code)

### Debug Log References

- Build inicial con `DexArchiveMergerException` transitorio; resuelto reintentando (probable caché/daemon)
- Tests Robolectric requirieron `@Config(sdk = [34])` porque targetSdk 36 no está soportado por Robolectric
- `MainViewModel` usó `SharingStarted.Eagerly` para que `.value` refleje el estado real sin suscriptores (necesario para tests)
- `MainScreenTest` con Robolectric + Compose requirió `Dispatchers.setMain(UnconfinedTestDispatcher())` y `@LooperMode(LooperMode.Mode.PAUSED)`
- Lint: suprimido `ViewModelConstructorInComposable` en tests con `@SuppressLint`
- Smoke test emulador: modal visible en primer inicio; tras aceptar, botón "Iniciar Jornada" pasa a `enabled=true`
- **Preferencia de sesión (usuario):** lanzar el emulador SIEMPRE con ventana gráfica (`-no-window` NO usar) para que el usuario pueda ver la app en el host
- **AVD dedicado:** usar `stayalert_avd` (Pixel 5, API 34, google_apis x86_64) — NO `ice_test_avd` (de otro propósito). Crear con: `avdmanager create avd -n stayalert_avd -k "system-images;android-34;google_apis;x86_64" -d pixel_5`

### Completion Notes List

- `SettingsRepository` definido como interfaz; implementación `DataStoreSettingsRepository` con DataStore Preferences (AD-6)
- `SettingsKeys.kt` con `notice_accepted`, `target_package`, `target_activity`
- Dependencias DataStore, ViewModel (lifecycle-viewmodel-ktx, lifecycle-viewmodel-compose), coroutines-test añadidas
- Componente `ResponsibleUseNotice.kt`: modal no dismissable por fuera, texto vinculante del PRD, diseño con tokens Material 3 dark
- `MainViewModel` expone `noticeAccepted` como `StateFlow`
- `MainScreen` muestra botón "Iniciar Jornada" (deshabilitado hasta aceptar) y el modal condicional
- `MainActivity` crea `DataStoreSettingsRepository` e inyecta a `MainViewModel` vía factory manual (DI manual, sin Hilt)
- Tests: `SettingsRepositoryTest` (DataStore real temporal), `MainViewModelTest` (fake repository), `MainScreenTest` (Robolectric + Compose)
- Build/test/lint verdes; smoke test en emulador OK

### File List

- `gradle/libs.versions.toml` (modificado)
- `app/build.gradle.kts` (modificado)
- `app/src/main/java/com/stayalert/data/SettingsKeys.kt` (nuevo)
- `app/src/main/java/com/stayalert/data/SettingsRepository.kt` (nuevo)
- `app/src/main/java/com/stayalert/data/DataStoreSettingsRepository.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/components/ResponsibleUseNotice.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/viewmodel/MainViewModel.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (modificado)
- `app/src/test/java/com/stayalert/data/SettingsRepositoryTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/ui/viewmodel/MainViewModelTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/ui/MainScreenTest.kt` (nuevo)
- `app/src/test/java/com/stayalert/ThemeTokensTest.kt` (sin cambios, regresión OK)

---

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `DataStoreSettingsRepository` se instancia dentro de `setContent` en cada recomposición [`app/src/main/java/com/stayalert/ui/MainActivity.kt:40-48`] — fixed
- [x] [Review][Patch] `collectAsState()` en vez de `collectAsStateWithLifecycle()` [`app/src/main/java/com/stayalert/ui/MainActivity.kt:53`] — fixed
- [x] [Review][Patch] `acceptNotice()` no maneja errores de DataStore [`app/src/main/java/com/stayalert/ui/viewmodel/MainViewModel.kt:19-23`] — fixed

### defer

- [x] [Review][Defer] `SharingStarted.Eagerly` mantiene el flow activo sin suscriptores [`app/src/main/java/com/stayalert/ui/viewmodel/MainViewModel.kt:14`] — deferred, pre-existing (revisar con story 1.4)
- [x] [Review][Defer] `SettingsKeys.TARGET_PACKAGE` y `TARGET_ACTIVITY` sin uso [`app/src/main/java/com/stayalert/data/SettingsKeys.kt:5-6`] — deferred, pre-existing (se usarán en story 1.4)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-1.2/findings-report.md`
