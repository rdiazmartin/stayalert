# Story 1.1: Scaffolding del proyecto y tema visual

Status: done

## Story

As a desarrollador,
I want un proyecto Android inicializado con el stack y el tema visual definidos,
so that las stories siguientes se construyen sobre una base coherente.

## Acceptance Criteria

1. El proyecto usa Kotlin 2.4.x, Compose (BOM 2026.06.x), AGP 9.3.0, Gradle 9.5+, JDK 17, minSdk 26 y targetSdk 36.
2. La estructura de paquetes sigue el Structural Seed del spine: `ui/`, `domain/`, `data/`, `system/` bajo `com.stayalert`.
3. El tema Material 3 dark aplica los tokens del DESIGN.md: surface-base `#121212`, surface-raised `#1E1E1E`, ink-primary `#E6E6E6`, ink-secondary `#9E9E9E`, ink-disabled `#616161`, accent `#4CAF50`, accent-on `#0B3D0F`, error `#CF6679`, warning `#F0A020`, border-hairline `#2C2C2C`, surface-overlay `#000000`.
4. El manifest declara `<queries>` para la app objetivo (visibilidad de paquetes, API 30+).
5. El CI básico (GitHub Actions) ejecuta lint + unit tests en cada push.
6. El proyecto compila y los tests de humo pasan.

## Tasks / Subtasks

- [x] Task 1: Inicializar proyecto Android (AC: 1)
  - [x] 1.1 Crear proyecto con Android Studio / Gradle: Kotlin 2.4.x, Compose BOM 2026.06.x, AGP 9.3.0, Gradle 9.5+, JDK 17
  - [x] 1.2 Configurar minSdk 26 / targetSdk 36 en `app/build.gradle.kts`
  - [x] 1.3 Verificar compilación con `./gradlew assembleDebug`
- [x] Task 2: Estructura de paquetes (AC: 2)
  - [x] 2.1 Crear paquetes `ui/`, `domain/`, `data/`, `system/` bajo `com.stayalert`
  - [x] 2.2 Crear `MainActivity` (single-activity) en `ui/`
- [x] Task 3: Tema Material 3 dark (AC: 3)
  - [x] 3.1 Definir tokens de color en `ui/theme/Color.kt` (todos los del DESIGN.md)
  - [x] 3.2 Definir tipografía (Headline Small 24sp, Body Large 16sp, Body Small 12sp, Label Large 14sp medium) en `ui/theme/Type.kt`
  - [x] 3.3 Definir tema dark en `ui/theme/Theme.kt` (sin modo claro)
  - [x] 3.4 Definir shapes (sm 8dp, md 16dp, full 999dp) y spacing (4/8/12/16/24/32/48dp)
- [x] Task 4: Manifest con `<queries>` (AC: 4)
  - [x] 4.1 Declarar `<queries>` con el paquete `com.microsoft.teams` en `AndroidManifest.xml`
- [x] Task 5: CI básico (AC: 5)
  - [x] 5.1 Crear `.github/workflows/ci.yml` con jobs: lint + unit tests
  - [x] 5.2 Verificar que el workflow es válido (lint + test en cada push)
- [x] Task 6: Smoke test (AC: 6)
  - [x] 6.1 Test de humo: la app arranca y muestra la pantalla principal vacía con el tema dark
  - [x] 6.2 `./gradlew test` pasa

## Dev Notes

### Contexto del producto

stayAlert es una app Android que mantiene el estado "Disponible" en apps de comunicación corporativa (Teams) manteniendo la app objetivo en primer plano + overlay negro. Esta story es el cimiento: NO implementar lógica de sesión, overlay, permisos ni notificaciones — solo scaffolding + tema.

### Stack (verificado en TR 2026-08-08)

- Kotlin 2.4.x (K2 compiler estable)
- Jetpack Compose BOM 2026.06.x (UI recomendada oficialmente)
- AGP 9.3.0 (requiere Gradle 9.5+, JDK 17, SDK Build Tools 36.0.0)
- minSdk 26 (requerido por `TYPE_APPLICATION_OVERLAY`, API 26)
- targetSdk 36 (requisito Play ago 2026; aunque v1 es sideload, mantenerlo)
- Sin Hilt (DI manual en v1 — decisión AD-3)
- Sin base de datos (DataStore Preferences se añadirá en stories 1.2/1.4)

### Arquitectura (spine — ADs vinculantes)

- Paradigma: layered + single-activity + UDF
- Estructura: `ui/` (Compose), `domain/` (SessionController, etc.), `data/` (SettingsRepository, PermissionAuditor, Notifier), `system/` (OverlayController, FGS, Watchdog)
- AD-6: DataStore Preferences para config (no en esta story)
- AD-3: DI manual por constructor (sin Hilt)
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md`]

### Tokens de diseño (DESIGN.md — vinculantes)

- Material 3 dark, sin modo claro (la app es la antesala del overlay negro)
- Un único acento cromático: `#4CAF50` (presencia viva) — botón principal, estado, notificación
- Tipografía Android nativa: Headline Small 24sp, Body Large 16sp, Body Small 12sp, Label Large 14sp medium
- Shapes: sm 8dp, md 16dp, full 999dp (píldora solo para el botón principal)
- Spacing: 4/8/12/16/24/32/48dp
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/DESIGN.md`]

### Microcopy (EXPERIENCE.md)

- UI en español, corto y directo, sin exclamaciones ni jerga técnica
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md`]

### Testing

- JUnit4 + Robolectric 4.16 (unit tests JVM)
- Compose UI tests (`androidx.compose.ui:ui-test-junit4`)
- En esta story: solo smoke test de arranque + tema

### Project Structure Notes

- Proyecto greenfield: no hay código existente que preservar
- Repositorio sin commits previos (todo untracked)
- El repo raíz contiene `_bmad/`, `_bmad-output/`, `.agents/`, `.opencode/`, `docs/` — el proyecto Android se crea en la raíz (o en `app/` si se decide monorepo; mantener la raíz limpia de artefactos BMad)

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 1.1]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — Structural Seed, Stack, AD-3, AD-6]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/DESIGN.md` — Colors, Typography, Shapes, Spacing]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md` — Voice and Tone]
- [Source: `_bmad-output/planning-artifacts/research/technical-android-presence-keep-awake-research-2026-08-08.md` — Stack verificado (Kotlin 2.4.x, Compose BOM, AGP 9.3.0, minSdk 26)]

## Dev Agent Record

### Agent Model Used

deepseek-v4-flash:0731 (opencode)

### Debug Log References

- Emulador: `ice_test_avd` (android-34, google_apis), arrancado con `-no-window -no-audio -no-boot-anim -gpu swiftshader_indirect`
- Smoke test: `adb shell am start -n com.stayalert/.ui.MainActivity` → `topResumedActivity=com.stayalert/.ui.MainActivity`
- UI dump: `text="stayAlert"` visible, package `com.stayalert`
- Screencap: fondo `(0,0,0)` — tema dark aplicado
- Sin crashes en logcat

### Completion Notes List

- Proyecto Android inicializado: Kotlin 2.4.10 (integrado en AGP 9.3.0 — el plugin `org.jetbrains.kotlin.android` ya no se usa desde AGP 9.0), Compose BOM 2026.06.01, Gradle 9.5.0 (wrapper), JDK 17, minSdk 26, targetSdk 36, compileSdk 37
- Estructura de paquetes: `ui/`, `domain/`, `data/`, `system/` bajo `com.stayalert` (Structural Seed del spine)
- Tema Material 3 dark con todos los tokens del DESIGN.md (11 colores, tipografía, shapes, spacing)
- Manifest con `<queries>` para `com.microsoft.teams`
- CI: `.github/workflows/ci.yml` (lint + unit tests en push/PR)
- Icono adaptive (fondo negro + punto verde accent)
- Tests: `ThemeTokensTest` (2 tests, verificación de tokens contra DESIGN.md)
- Smoke test en emulador: app arranca, "stayAlert" visible, fondo negro, sin crashes
- Nota: `local.properties` con `sdk.dir=/home/roberto/Android/Sdk` (no commitear — gitignored)

### File List

- `settings.gradle.kts` (nuevo)
- `build.gradle.kts` (nuevo)
- `gradle/libs.versions.toml` (nuevo)
- `gradle/wrapper/gradle-wrapper.jar` (nuevo)
- `gradle/wrapper/gradle-wrapper.properties` (nuevo)
- `gradlew`, `gradlew.bat` (nuevos)
- `local.properties` (nuevo, gitignored)
- `app/build.gradle.kts` (nuevo)
- `app/proguard-rules.pro` (nuevo)
- `app/src/main/AndroidManifest.xml` (nuevo)
- `app/src/main/res/values/strings.xml` (nuevo)
- `app/src/main/res/values/themes.xml` (nuevo)
- `app/src/main/res/drawable/ic_launcher_background.xml` (nuevo)
- `app/src/main/res/drawable/ic_launcher_foreground.xml` (nuevo)
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (nuevo)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/theme/Color.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/theme/Type.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/theme/Shape.kt` (nuevo)
- `app/src/main/java/com/stayalert/ui/theme/Theme.kt` (nuevo)
- `app/src/test/java/com/stayalert/ThemeTokensTest.kt` (nuevo)
- `.github/workflows/ci.yml` (nuevo)

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `MainActivity` omits `enableEdgeToEdge()` [`app/src/main/java/com/stayalert/ui/MainActivity.kt:188`] — fixed
- [x] [Review][Patch] Activity theme declaration is redundant [`app/src/main/AndroidManifest.xml:153,158`] — fixed
- [x] [Review][Patch] `.gitignore` missing `.kotlin/` [`.gitignore`] — fixed

### defer

- [x] [Review][Defer] `SurfaceOverlay` mapped to `surfaceContainerLowest` is semantically risky [`app/src/main/java/com/stayalert/ui/theme/Theme.kt:286`] — deferred, pre-existing (scaffolding; resolve when overlay implemented)
- [x] [Review][Defer] `Warning` color token is not exposed in the theme [`app/src/main/java/com/stayalert/ui/theme/Color.kt:232`, `Theme.kt:265-287`] — deferred, pre-existing (no warning states yet)
- [x] [Review][Defer] `themes.xml` may show a light splash before Compose loads [`app/src/main/res/values/themes.xml:2`] — deferred, pre-existing (resolve when final launch theme polished)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-1.1/findings-report.md`
