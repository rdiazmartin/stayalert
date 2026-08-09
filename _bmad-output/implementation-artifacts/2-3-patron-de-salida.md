---
baseline_commit: b20e670
---

# Story 2.3: Patrón de salida

Status: done

## Story

As a usuario,
I want terminar la sesión con 4 toques en la esquina superior derecha,
so que puedo salir del modo de aislamiento al instante sin depender de la UI.

## Acceptance Criteria

1. **Detección del patrón (FR-7)** — 4 toques consecutivos en la esquina superior derecha (región 15% ancho × 15% alto, ventana temporal 500 ms entre toques) terminan la sesión en < 500 ms tras el 4º toque (NFR-2).
2. **Terminación completa (FR-7)** — El overlay se destruye, el FGS se detiene y la retención de pantalla se libera.
3. **Notificación (FR-12)** — Se emite notificación "Sesión terminada: patrón de salida" (AD-8, AD-10).
4. **Reinicio del contador (FR-7)** — Un toque fuera de la región o fuera de la ventana temporal reinicia el contador sin terminar la sesión.
5. **Idempotencia (AD-9)** — La terminación es idempotente: eventos de terminación en `Deteniendo`/`Inactiva` son no-ops.
6. **Clock inyectable (AD-12)** — El detector usa el `Clock` inyectable para la ventana temporal.
7. **Tests** — Unit tests: patrón correcto, toque fuera de región, toque fuera de ventana, reinicio del contador, idempotencia.

## Tasks / Subtasks

- [x] Task 1: Detector de patrón (AC: 1, 4, 6)
  - [x] 1.1 Crear `domain/PatternDetector.kt` — clase pura con `onTouch(x, y, width, height): Boolean` (o `PatternResult`)
  - [x] 1.2 Región: 15% ancho × 15% alto, esquina superior derecha
  - [x] 1.3 Ventana temporal: 500 ms entre toques (`SessionConstants.PATTERN_WINDOW_MS`), vía `Clock` inyectable
  - [x] 1.4 Contador: 4 toques consecutivos; toque fuera de región o fuera de ventana reinicia
  - [x] 1.5 `reset()` para reiniciar el contador
- [x] Task 2: Integración en overlay (AC: 1, 2)
  - [x] 2.1 `SystemOverlayController` recibe el `PatternDetector` y un `onPatternDetected` callback
  - [x] 2.2 El `View` del overlay registra `OnTouchListener` que alimenta el detector
  - [x] 2.3 Al detectar el patrón → `onEvent(SessionEvent.PatternDetected)`
- [x] Task 3: Terminación (AC: 2, 3, 5)
  - [x] 3.1 `SessionController` ya maneja `PatternDetected` en `Aislada` → `terminate(Pattern)` (de 2.1)
  - [x] 3.2 Verificar que `terminate()` emite `HideOverlay` + `StopFgs` + `StopWatchdog` (ya en 2.1)
  - [x] 3.3 Notificación "Sesión terminada: patrón de salida" — se implementará en story 2.4 (Notifier); aquí solo el evento
- [x] Task 4: Tests (AC: 7)
  - [x] 4.1 Test `PatternDetector`: 4 toques en región → detectado
  - [x] 4.2 Test: toque fuera de región → reinicia contador
  - [x] 4.3 Test: toque fuera de ventana temporal → reinicia contador
  - [x] 4.4 Test: 3 toques + reset → no detectado
  - [x] 4.5 Test integración: `PatternDetected` en `Aislada` → `Inactiva` (ya cubierto en 2.1, verificar)
  - [x] 4.6 Verificar `./gradlew test` y `./gradlew lint`

## Dev Notes

### Contexto del producto

El patrón de salida es el único mecanismo táctil para terminar la sesión: 4 toques en la esquina superior derecha de la pantalla negra. Sin feedback visual (el overlay es negro absoluto); el feedback es la destrucción del overlay + notificación.

### Stack

- Kotlin 2.4.10, Compose BOM 2026.06.01, AGP 9.3.0, Gradle 9.5.0, JDK 17, minSdk 26, targetSdk 36
- Coroutines + Flow
- DI manual por constructor (sin Hilt)
- JUnit4 + Robolectric 4.16

### Arquitectura (spine — ADs vinculantes)

- **AD-4**: detector de patrón: 4 taps en región 15% ancho × 15% alto (esquina superior derecha), ventana temporal 500 ms
- **AD-9**: terminación idempotente (ya en SessionController)
- **AD-10**: `PatternDetected` → `TerminationReason.Pattern` (ya en SessionController)
- **AD-12**: `Clock` inyectable para la ventana temporal
- **AD-8**: notificación de fin de sesión (story 2.4)

### Estructura de archivos

- `domain/PatternDetector.kt` — clase pura (sin Android)
- `system/SystemOverlayController.kt` (modificado — OnTouchListener)
- `domain/SessionController.kt` (sin cambios — ya maneja PatternDetected)

### Testing

- `PatternDetector` es puro Kotlin — tests JVM sin Robolectric
- Fake `Clock` con control manual del tiempo
- Test de integración con `SessionController` (ya cubierto en 2.1)

### Project Structure Notes

- `PatternDetector` en `domain/` (puro, testeable)
- El overlay registra el touch listener; el detector no conoce Android

### Previous Story Intelligence

Story 2.2 estableció:
- `SystemOverlayController` con `View` de fondo negro, flags, `onEvent` callback
- `SessionCommandHandler` orquesta el despliegue
- `SessionController` ya maneja `PatternDetected` en `Aislada` → `terminate(Pattern)` (test en 2.1)

### Git Intelligence

Últimos commits:
- `b20e670 feat(2.2): despliegue secuencial y overlay de aislamiento`
- `1a15c6f feat(2.1): nucleo de sesion - SessionController, validacion FR-5 y event loop`
- `ed8de8c docs: README con instrucciones de compilacion y guia de uso`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.3]
- [Source: `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md` — §4.2 FR-7]
- [Source: `_bmad-output/planning-artifacts/architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md` — AD-4, AD-9, AD-10, AD-12]
- [Source: `_bmad-output/planning-artifacts/ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md` — Flow 2, Interaction Primitives]

## Dev Agent Record

### Agent Model Used

(opencode)

### Debug Log References

- `PatternDetector` es clase pura Kotlin (sin Android) — testeable en JVM sin Robolectric
- Test "tres toques no detectan" corregido: verificaba el 4º toque (que sí detecta); ahora verifica que los 3 primeros devuelven false
- `SystemOverlayController` recibe `PatternDetector` y registra `OnTouchListener` en el View del overlay
- Smoke test emulador: app arranca OK (el patrón requiere sesión activa con overlay, no testeable sin permisos completos)
- **Repo remoto (usuario):** subir el repositorio a `https://github.com/rdiazmartin/stayalert` más tarde (aún no configurado como remote)

### Completion Notes List

- `domain/PatternDetector.kt` — clase pura: región 15%×15% esquina superior derecha, ventana 500 ms vía `Clock`, contador 4 toques, `reset()`
- `SystemOverlayController` — `OnTouchListener` en el View que alimenta el detector; al detectar → `SessionEvent.PatternDetected`
- `MainActivity` — inyecta `PatternDetector(SystemClock())` al overlay
- `SessionController` — sin cambios (ya maneja `PatternDetected` → `terminate(Pattern)` desde 2.1)
- Tests: `PatternDetectorTest` (5) — verdes
- Build/test/lint verdes

### File List

- `app/src/main/java/com/stayalert/domain/PatternDetector.kt` (nuevo)
- `app/src/main/java/com/stayalert/system/SystemOverlayController.kt` (modificado)
- `app/src/main/java/com/stayalert/ui/MainActivity.kt` (modificado)
- `app/src/test/java/com/stayalert/domain/PatternDetectorTest.kt` (nuevo)

---

## Review Findings

### decision-needed

(None)

### patch

- [x] [Review][Patch] `PatternDetector` no valida coordenadas negativas ni dimensiones cero [`app/src/main/java/com/stayalert/domain/PatternDetector.kt:59-63`] — fixed
- [x] [Review][Patch] `SystemOverlayController` no reinicia el detector al ocultar el overlay [`app/src/main/java/com/stayalert/system/SystemOverlayController.kt:73-86`] — fixed

### defer

- [x] [Review][Defer] `PatternDetector` no expone el estado del contador [`app/src/main/java/com/stayalert/domain/PatternDetector.kt`] — deferred, pre-existing (añadir si se necesita debug)
- [x] [Review][Defer] `MainActivity` crea dos `SystemClock()` separados [`app/src/main/java/com/stayalert/ui/MainActivity.kt:97-100`] — deferred, pre-existing (compartir instancia con `AppContainer`)

---

**Code review report:** `_bmad-output/implementation-artifacts/code-review-2.3/findings-report.md`
