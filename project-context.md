# Project Context — stayAlert

Contexto y lecciones aprendidas para agentes que trabajen en este proyecto.

## Stack

- Android (Kotlin, Compose, Material 3), minSdk 26, targetSdk 36, compileSdk 37
- AGP 9.3.0, Gradle 9.5.0, Kotlin 2.4.10
- DataStore Preferences, coroutines, ViewModel + StateFlow
- Tests: JUnit4, Robolectric, Compose UI Test, UI Automator (E2E)

## Convenciones de build

- `gradle.properties` con `org.gradle.jvmargs=-Xmx4g` — sin esto el daemon OOM en dex merging (512 MB por defecto)
- APK release: `stayAlert-<version>-release.apk` (configurado en `app/build.gradle.kts` vía `androidComponents`)
- `mock-teams` es un módulo aparte (paquete `com.microsoft.teams`) para validación E2E

## Lecciones aprendidas (no repetir errores)

### Tests Robolectric con targetSdk 36+
- Robolectric no soporta targetSdk 36: usar `@Config(sdk = [34])` en TODOS los tests Robolectric.
- Los iconos de `android.R.drawable` no existen en Robolectric: usar vectores propios.
- Los Shadows tienen defaults incorrectos (`canDrawOverlays` = false, batería): configurarlos explícitamente.

### Manifest
- Verificar el manifest ANTES de implementar: permisos y `<queries>` faltantes rompen funcionalidad silenciosamente (ej: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` faltante hacía que el intent no abriera Ajustes).

### Wiring y DI
- El wiring manual en `MainActivity` es frágil (lateinit circular, SystemClock duplicado, callback estático). Señalado 3 veces en reviews: introducir `AppContainer`/ServiceLocator cuando crezca.

### Clock inyectable (AD-12)
- Usar SIEMPRE el `Clock` inyectable en implementaciones: nunca `delay()` real ni `SystemClock` directo (violado en 2.2 y 2.5).

### Testing
- Escribir tests E2E instrumentados temprano: el E2E de 2.6 encontró 3 bugs que los unitarios no podían ver (watchdog mataba sesión con overlay visible, overlay no clickable, paquete del mock desalineado).
- Evitar aserciones débiles: `assertTrue(true)` vacío y tests que verifican el caso equivocado (test "3 toques" que verificaba el 4º).

## Arquitectura (resumen)

- `SessionController` es el único propietario del estado de sesión (AD-1)
- `Notifier` es el único publicador de notificaciones (AD-8)
- Handshake Lanzando→Aislada vía `SessionEvent.OverlayShown` (AD-7)
- Watchdog solo detecta y emite eventos, nunca publica notificaciones (AD-5)
- `SystemClock` en `domain/` es deuda conocida — mover a `system/` en refactor

## Deuda técnica conocida

Ledger completo: `_bmad-output/implementation-artifacts/deferred-work.md` (10 entradas).
Las más relevantes: `SystemClock` en `domain/`, `BatteryWarning` sin notificación (FR-16), navegación con `mutableStateOf`, `SettingsScreen` monolítico, `DEFAULT_TARGET_ACTIVITY` hardcodeado.

## Validación pendiente (success metrics del PRD)

- SM-1a: mock resumed ≥ 8 h (validación manual en emulador)
- SM-1b: Teams real ≥ 2 días (dispositivo real)
- SM-2: 0 toques al mock (lectura de logcat en E2E completo)
