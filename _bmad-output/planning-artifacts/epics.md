---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - prds/prd-stayAlert-2026-08-08/prd.md
  - architecture/architecture-stayAlert-2026-08-08/ARCHITECTURE-SPINE.md
  - ux-designs/ux-stayAlert-2026-08-08/DESIGN.md
  - ux-designs/ux-stayAlert-2026-08-08/EXPERIENCE.md
  - research/technical-android-presence-keep-awake-research-2026-08-08.md
---

# stayAlert - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for stayAlert, decomposing the requirements from the PRD, UX Design, Architecture, and Technical Research into implementable stories.

## Requirements Inventory

### Functional Requirements

- FR-1: Auditoría de permisos — el usuario puede ver el estado de cada permiso (overlay, notificaciones) en la pantalla de configuración, con indicación de concedido/pendiente.
- FR-2: Guiado a los menús de permisos — acceso directo por permiso pendiente que abre el menú de Ajustes correspondiente; re-auditoría al regresar.
- FR-3: Aviso de uso responsable — modal único en primer inicio; sin aceptación no se puede iniciar sesión.
- FR-4: Configuración de la app objetivo — paquete + actividad principal configurable (default `com.microsoft.teams`); validación de instalación (requiere `<queries>` en manifest); persistencia en DataStore.
- FR-5: Validación previa al inicio — sesión solo si permisos concedidos, aviso aceptado y app objetivo instalada; mensaje claro del motivo de bloqueo.
- FR-6: Despliegue secuencial — (a) lanzar app objetivo en primer plano; (b) 1 s después overlay; (c) iniciar FGS. Abortar limpiamente si la app objetivo no se confirma en primer plano. Overlay en < 500 ms. Pantalla encendida durante la sesión.
- FR-7: Patrón de salida — 4 toques consecutivos en esquina superior derecha (región 15% × 15%), ventana temporal 500 ms; termina sesión en < 500 ms tras el 4º toque; toque fuera de región/ventana reinicia contador.
- FR-8: Overlay negro a pantalla completa — `#000000` puro, sin elementos gráficos, topmost sobre la app objetivo, `FLAG_SECURE`; status bar/shade accesibles (comportamiento del sistema).
- FR-9: Bloqueo de toques accidentales — toques del usuario sobre el overlay no producen efecto sobre la app objetivo (salvo patrón de salida).
- FR-10: Retención de pantalla — pantalla encendida mientras el overlay es visible; liberada al terminar la sesión.
- FR-11: FGS con acción de detención — servicio en primer plano (tipo `specialUse`) con notificación visible y acción "Detener sesión" durante la sesión.
- FR-12: Feedback de fin de sesión — toda terminación produce notificación "Sesión terminada" con el motivo.
- FR-13: Watchdog de sesión — detecta pantalla apagada, overlay ausente, permiso revocado, salida de primer plano de la app objetivo, `HIDE_OVERLAY_WINDOWS`; termina sesión en < 5 s con notificación.
- FR-14: Manejo de crash de la app objetivo — si la app objetivo se cierra/crashea, terminar sesión con notificación en < 5 s.
- FR-15: Onboarding de exención de batería del fabricante — paso que guía a la exención de optimización de batería y autostart según fabricante.
- FR-16: Manejo de batería baja — aviso al 15%, terminación al 5% con notificación.
- FR-17: Recuperación ante muerte del proceso — tras kill, sin overlay huérfano ni servicio activo; estado `Inactiva` al reabrir.

### NonFunctional Requirements

- NFR-1: Overlay se despliega en < 500 ms tras el disparo de inicio (FR-6).
- NFR-2: Terminación de sesión en < 500 ms tras el 4º toque del patrón (FR-7).
- NFR-3: Watchdog detecta anomalías y termina sesión en < 5 s (FR-13, FR-14).
- NFR-4: Inyección de intervalos de inyección: no aplica (eliminada en v2 — sin orquestador).
- NFR-5: Contraste AA: ink-primary sobre surface-base ≈ 15:1; ink-secondary ≈ 7:1; acento con texto ≈ 4.6:1 (UX).
- NFR-6: Tap targets ≥ 48dp (UX).
- NFR-7: Dynamic type respetado en todos los niveles (UX).
- NFR-8: Sin wake locks activos fuera de sesión; retención liberada al terminar (SM-4).
- NFR-9: Sin red, sin almacenamiento, sin telemetría (Ethics §5).
- NFR-10: `FLAG_SECURE` en el overlay (no capturable por screenshot/grabación).

### Additional Requirements

- AD-1: SessionController único propietario del estado (sealed class `SessionState` + `StateFlow`).
- AD-2: Componentes SO son clientes; FGS se inicia tras overlay visible (regla Android 15).
- AD-3: `TargetAppLauncher` interfaz con contrato `Result<Unit>`; `IntentLauncher` + `FakeLauncher`; DI manual.
- AD-4: Overlay `TYPE_APPLICATION_OVERLAY` (minSdk 26) vía `createWindowContext`; flags NOT_FOCUSABLE + LAYOUT_IN_SCREEN + KEEP_SCREEN_ON + SECURE; sin NOT_TOUCHABLE; detector 4-taps (15% × 15%, 500 ms).
- AD-5: Watchdog activo solo en `Aislada`; detecta y emite eventos, nunca publica notificaciones; polling 2 s.
- AD-6: DataStore Preferences para config; sesión efímera (no persiste).
- AD-7: Contrato `SessionCommand` (sealed); fire-and-forget; handshake Lanzando→Aislada solo vía `OverlayShown`; aborto con `OverlayFailed`/`LaunchFailed`.
- AD-8: `Notifier` único publicador; canales fijos `stayalert_session` y `stayalert_events`; acción Detener vía `BroadcastReceiver` (`StopReceiver`).
- AD-9: Event loop de consumidor único; terminación idempotente; eventos en estado no admitido = no-ops.
- AD-10: `SessionEvent` sealed con subclases fijas; enum `TerminationReason`; mapeo motivo→texto en `Notifier`.
- AD-11: `ForegroundMonitor` interfaz (producción: `UsageStatsManager`; test: fake); `Unknown` si sin permiso de uso.
- AD-12: `Clock` inyectable (producción: `SystemClock`; test: fake).
- Stack: Kotlin 2.4.x, Compose BOM 2026.06.x, AGP 9.3.0, Gradle 9.5+, JDK 17, minSdk 26 / targetSdk 36.
- Testing: JUnit4 + Robolectric 4.16 + Compose UI tests + UI Automator; mock APK `com.microsoft.teams` excluido de release.
- Constantes de sesión en `SessionConstants` (1 s delay, 500 ms ventana, 15%/5% batería, 2 s polling, SLA 5 s).
- Validation Gate (PRD §9): GO si Teams real permanece Disponible ≥ 4 h foreground en ≥ 2 días; STOP/pivot si no.

### UX Design Requirements

- UX-DR1: Tema Material 3 dark (sin modo claro); tokens de color del DESIGN.md (surface-base `#121212`, surface-raised `#1E1E1E`, surface-overlay `#000000`, accent `#4CAF50`, etc.).
- UX-DR2: Tipografía Android (Headline Small 24sp, Body Large 16sp, Body Small 12sp, Label Large 14sp medium); dynamic type.
- UX-DR3: Botón principal "Iniciar Jornada" — píldora (`rounded/full`), fondo accent, texto accent-on; deshabilitado con ink-disabled.
- UX-DR4: Indicador de estado — texto + punto 8dp (accent activo / ink-disabled inactivo); "Sesión activa" / "Sin sesión activa".
- UX-DR5: Tarjeta de configuración — surface-raised, rounded/md, filas label + valor/chevron.
- UX-DR6: Diálogo de aviso de uso responsable — modal único, no dismissable por fuera, botón "Entiendo y acepto".
- UX-DR7: Notificaciones — canal FGS "Sesión activa — toca para detener" con acción Detener; canal fin de sesión "Sesión terminada: {motivo}".
- UX-DR8: Microcopy en español, corto y directo (tabla Do/Don't del EXPERIENCE.md).
- UX-DR9: Estados de sesión en UI: Inactiva / Lanzando (spinner "Abriendo app objetivo…") / Aislada / Deteniendo.
- UX-DR10: Accesibilidad — TalkBack etiquetas con rol+estado; tap targets ≥ 48dp; contraste AA; sin animaciones decorativas.
- UX-DR11: Overlay negro absoluto sin elementos gráficos (ni logo, ni texto, ni indicador).
- UX-DR12: Patrón de salida sin feedback visual (el feedback es la destrucción del overlay + notificación).

### FR Coverage Map

| FR | Epic | Story |
|---|---|---|
| FR-1, FR-2, FR-3, FR-15 | Epic 1 (Onboarding) | 1.1, 1.2, 1.3 |
| FR-4 | Epic 1 (Onboarding) | 1.4 |
| FR-5, FR-6 | Epic 2 (Sesión) | 2.1, 2.2 |
| FR-7 | Epic 2 (Sesión) | 2.3 |
| FR-8, FR-9, FR-10 | Epic 2 (Sesión) | 2.2 |
| FR-11, FR-12 | Epic 2 (Sesión) | 2.4 |
| FR-13, FR-14, FR-16 | Epic 2 (Sesión) | 2.5 |
| FR-17 | Epic 2 (Sesión) | 2.6 |
| NFR-1..NFR-10 | Transversal | En ACs de stories |
| AD-1..AD-12 | Transversal | En ACs de stories |
| UX-DR1..UX-DR12 | Transversal | En ACs de stories |

## Epic List

### Epic 1: Configuración y Preparación del Dispositivo
El usuario puede preparar stayAlert en su dispositivo: aceptar el aviso de uso responsable, conceder los permisos necesarios (overlay, notificaciones), configurar la app objetivo (paquete + actividad) y completar la exención de batería del fabricante. Sin este epic no se puede iniciar ninguna sesión.
**FRs covered:** FR-1, FR-2, FR-3, FR-4, FR-15

### Epic 2: Sesión de Presencia Activa
El usuario puede iniciar, mantener y terminar una sesión de presencia: despliegue secuencial (app objetivo en primer plano → overlay negro → FGS), patrón de salida (4-taps), kill switch en la notificación, watchdog de anomalías (pantalla apagada, overlay ausente, permiso revocado, salida de primer plano, batería baja) y recuperación ante fallos. Se apoya en los permisos del Epic 1.
**FRs covered:** FR-5, FR-6, FR-7, FR-8, FR-9, FR-10, FR-11, FR-12, FR-13, FR-14, FR-16, FR-17

### Story 2.1: Núcleo de sesión

As a usuario,
I want que la app valide las condiciones antes de iniciar y que no deje residuos si el proceso muere,
So that la sesión solo arranca cuando todo está listo y nunca queda en estado inconsistente.

**Acceptance Criteria:**

**Given** la app abierta en la pantalla principal
**When** se pulsa "Iniciar Jornada"
**Then** el sistema valida: permisos concedidos (overlay, notificaciones), aviso aceptado y app objetivo instalada (FR-5)
**And** si falta algo, el botón no inicia la sesión y muestra el motivo exacto (FR-5)
**And** si todo está listo, la sesión transita a `Lanzando` (FR-5)
**And** `SessionController` es el único propietario del estado (`SessionState` sealed: Inactiva/Lanzando/Aislada/Deteniendo, expuesto como `StateFlow`) (AD-1)
**And** los eventos se procesan en un único dispatcher (Channel con consumidor único) (AD-9)
**And** tras un kill del proceso, no queda overlay huérfano ni servicio activo y al reabrir el estado es `Inactiva` (FR-17)
**And** el `Clock` es inyectable (producción: SystemClock; test: fake) (AD-12)

### Story 2.2: Despliegue secuencial y overlay de aislamiento

As a usuario,
I want que al iniciar la jornada se abra la app objetivo, se cubra la pantalla con el overlay negro y la pantalla no se apague,
So that mi presencia se mantiene sin ver el contenido de la app objetivo.

**Acceptance Criteria:**

**Given** una sesión en estado `Lanzando`
**When** se ejecuta el despliegue secuencial
**Then** la app objetivo se lanza en primer plano vía `TargetAppLauncher` (contrato `Result<Unit>`, nunca excepciones) (AD-3)
**And** transcurrido 1 s (constante `SessionConstants.LAUNCH_DELAY_MS`, vía `Clock`), se despliega el overlay (FR-6)
**And** el overlay es `TYPE_APPLICATION_OVERLAY` (minSdk 26) vía `createWindowContext`, con flags NOT_FOCUSABLE + LAYOUT_IN_SCREEN + KEEP_SCREEN_ON + SECURE, fondo `#000000` opaco, sin NOT_TOUCHABLE (AD-4, FR-8)
**And** la transición a `Aislada` ocurre solo al recibir `SessionEvent.OverlayShown` (handshake AD-7); `ShowOverlay` es fire-and-forget
**And** si llega `LaunchFailed` o `OverlayFailed`, la sesión aborta a `Inactiva` con notificación (AD-7, FR-6)
**And** si `ForegroundMonitor` no confirma la app objetivo en primer plano, la sesión aborta limpiamente sin overlay huérfano (FR-6, AD-11)
**And** el FGS se inicia después de que el overlay esté visible (regla Android 15) (AD-2)
**And** la pantalla permanece encendida mientras el overlay es visible (FR-10) y se libera al terminar (FR-10)
**And** el overlay no muestra ningún elemento gráfico y los toques del usuario no llegan a la app objetivo (FR-8, FR-9)
**And** el overlay se despliega en < 500 ms tras el disparo (NFR-1)

### Story 2.3: Patrón de salida

As a usuario,
I want terminar la sesión con 4 toques en la esquina superior derecha,
So que puedo salir del modo de aislamiento al instante sin depender de la UI.

**Acceptance Criteria:**

**Given** una sesión en estado `Aislada` con el overlay visible
**When** el usuario pulsa 4 veces consecutivas en la esquina superior derecha (región 15% ancho × 15% alto, ventana temporal 500 ms entre toques)
**Then** la sesión termina en < 500 ms tras el 4º toque (FR-7, NFR-2)
**And** el overlay se destruye, el FGS se detiene y la retención de pantalla se libera (FR-7)
**And** se emite notificación "Sesión terminada: patrón de salida" (FR-12, AD-8, AD-10)
**And** un toque fuera de la región o fuera de la ventana temporal reinicia el contador sin terminar la sesión (FR-7)
**And** la terminación es idempotente: eventos de terminación en `Deteniendo`/`Inactiva` son no-ops (AD-9)
**And** el detector usa el `Clock` inyectable para la ventana temporal (AD-12)

### Story 2.4: Servicio en primer plano y notificaciones

As a usuario,
I want ver una notificación de sesión activa con acción para detenerla y recibir confirmación al terminar,
So que siempre tengo un kill switch visible y sé cuándo la sesión termina.

**Acceptance Criteria:**

**Given** una sesión activa
**When** el FGS está corriendo
**Then** el servicio en primer plano es tipo `specialUse` con permiso `FOREGROUND_SERVICE_SPECIAL_USE` y notificación visible (FR-11)
**And** la notificación usa el canal fijo `stayalert_session` (creado al iniciar la app) con texto "Sesión activa — toca para detener" y acción "Detener" (FR-11, AD-8, UX-DR7)
**And** la acción "Detener" enruta a `StopReceiver` (BroadcastReceiver) que emite `SessionEvent.StopRequested` (AD-8)
**And** al terminar la sesión, el FGS se detiene y la notificación desaparece (FR-11)
**And** toda terminación produce notificación "Sesión terminada: {motivo}" en el canal `stayalert_events` (FR-12, AD-8, AD-10)
**And** `Notifier` es el único componente que publica notificaciones (AD-8)
**And** el mapeo motivo→texto vive en `Notifier` (AD-10)

### Story 2.5: Watchdog de sesión

As a usuario,
I want que la sesión se termine automáticamente si algo falla (pantalla apagada, overlay ausente, permiso revocado, app objetivo cerrada, batería baja),
So que nunca quede una sesión muerta en silencio ni la app objetivo expuesta sin aviso.

**Acceptance Criteria:**

**Given** una sesión en estado `Aislada`
**When** el watchdog detecta una anomalía
**Then** la sesión termina en < 5 s con notificación "Sesión terminada: {motivo}" (FR-13, FR-14, NFR-3)
**And** el watchdog detecta: pantalla apagada (`PowerManager`), overlay ausente o permiso revocado (`Settings.canDrawOverlays()`), app objetivo fuera de primer plano (`ForegroundMonitor`/`UsageStatsManager` si el permiso de uso está concedido), `HIDE_OVERLAY_WINDOWS` (overlay no dibujado), y batería (aviso al 15%, terminación al 5%) (FR-13, FR-14, FR-16, AD-5)
**And** el watchdog solo está activo en estado `Aislada` (iniciado/detenido por órdenes) (AD-5)
**And** el watchdog solo detecta y emite eventos — nunca publica notificaciones (AD-5, AD-8)
**And** el polling es cada 2 s (constante `SessionConstants.WATCHDOG_POLL_MS`) (AD-5)
**And** si la app objetivo crashea, la sesión termina en < 5 s sin dejar el overlay sobre el launcher (FR-14)
**And** si el permiso de uso no está concedido, la detección de salida de primer plano se omite (limitación documentada) (AD-11)

### Story 2.6: Mock de Teams y validación E2E

As a desarrollador,
I want un mock de Teams instalable en el emulador y tests E2E que validen el flujo completo,
So que puedo verificar el comportamiento sin Teams real instalado.

**Acceptance Criteria:**

**Given** el emulador de Android Studio sin Teams instalado
**When** se instala el mock
**Then** el mock APK declara el paquete `com.microsoft.teams` y los componentes que stayAlert lanza (actividad principal)
**And** el mock registra en logcat los toques recibidos y expone su estado de lifecycle (resumed/stopped) para aserciones
**And** el mock está excluido de los builds de release (variant/build-type)
**And** los tests E2E (UI Automator) validan: lanzar mock → overlay visible → mock permanece resumed ≥ 8 h (proxy SM-1a) → 0 toques recibidos por el mock (SM-2) → patrón de salida termina la sesión
**And** los tests unitarios (JVM/Robolectric) cubren: handshake Lanzando→Aislada, idempotencia de terminación, watchdog con fake clock, patrón 4-taps con fake clock (AD-9, AD-12)
**And** el kill switch (BroadcastReceiver) se cubre en tests instrumentados

## Epic 1: Configuración y Preparación del Dispositivo

El usuario puede preparar stayAlert en su dispositivo: aceptar el aviso de uso responsable, conceder los permisos necesarios (overlay, notificaciones), configurar la app objetivo (paquete + actividad) y completar la exención de batería del fabricante. Sin este epic no se puede iniciar ninguna sesión.
**FRs covered:** FR-1, FR-2, FR-3, FR-4, FR-15

### Story 1.1: Scaffolding del proyecto y tema visual

As a desarrollador,
I want un proyecto Android inicializado con el stack y el tema visual definidos,
So that las stories siguientes se construyen sobre una base coherente.

**Acceptance Criteria:**

**Given** un repositorio vacío de stayAlert
**When** se inicializa el proyecto Android
**Then** el proyecto usa Kotlin 2.4.x, Compose (BOM 2026.06.x), AGP 9.3.0, Gradle 9.5+, JDK 17, minSdk 26 y targetSdk 36
**And** la estructura de paquetes sigue el Structural Seed del spine (`ui/`, `domain/`, `data/`, `system/`)
**And** el tema Material 3 dark aplica los tokens del DESIGN.md (surface-base `#121212`, surface-raised `#1E1E1E`, ink-primary `#E6E6E6`, ink-secondary `#9E9E9E`, accent `#4CAF50`, accent-on `#0B3D0F`, error `#CF6679`, warning `#F0A020`, border-hairline `#2C2C2C`)
**And** el manifest declara `<queries>` para la app objetivo (visibilidad de paquetes, API 30+)
**And** el CI básico (GitHub Actions) ejecuta lint + unit tests en cada push

### Story 1.2: Aviso de uso responsable

As a usuario,
I want ver un aviso claro sobre el uso de la app en el primer inicio y aceptarlo,
So that entiendo el propósito y los riesgos antes de usarla.

**Acceptance Criteria:**

**Given** el primer inicio de la app (sin aviso aceptado previamente)
**When** se abre la pantalla principal
**Then** se muestra el modal de aviso con el texto: "Esta app mantiene una señal de presencia activa sin interacción del usuario. Su uso puede no estar alineado con la política de tu empleador." y el botón "Entiendo y acepto"
**And** el modal no es dismissable por fuera (solo el botón lo cierra)
**And** la aceptación persiste en DataStore (clave `notice_accepted`)
**And** sin aceptación, el botón "Iniciar Jornada" está deshabilitado (FR-3, FR-5)
**And** el modal no se vuelve a mostrar en inicios posteriores
**And** el modal cumple UX-DR6 (surface-raised, rounded/md) y UX-DR8 (microcopy en español, directo)

### Story 1.3: Auditoría y guiado de permisos

As a usuario,
I want ver el estado de mis permisos y acceder a los menús de Ajustes para concederlos,
So that puedo preparar el dispositivo sin buscar los menús manualmente.

**Acceptance Criteria:**

**Given** la pantalla de configuración abierta
**When** se auditan los permisos
**Then** se muestra el estado real de cada permiso (overlay vía `Settings.canDrawOverlays()`, notificaciones vía estado de runtime) con indicación concedido/pendiente (FR-1)
**And** cada permiso pendiente tiene un acceso directo que abre el menú de Ajustes correspondiente (overlay: `ACTION_MANAGE_OVERLAY_PERMISSION`; notificaciones: ajustes de la app) (FR-2)
**And** al regresar de Ajustes, el estado se re-audita automáticamente (FR-2)
**And** `PermissionAuditor` es el único componente que lee el estado de permisos (convención del spine)
**And** la UI cumple UX-DR5 (tarjeta surface-raised, filas label + valor) y UX-DR10 (tap targets ≥ 48dp, TalkBack con rol+estado)

### Story 1.4: Configuración de la app objetivo y exención de batería

As a usuario,
I want configurar la app objetivo (paquete y actividad) y completar la exención de batería del fabricante,
So that la sesión se lanza contra la app correcta y el servicio no es eliminado por el sistema.

**Acceptance Criteria:**

**Given** la pantalla de configuración
**When** se configura la app objetivo
**Then** el usuario puede editar paquete y actividad principal, con default `com.microsoft.teams` y su actividad principal (FR-4)
**And** el valor persiste en DataStore (claves `target_package`, `target_activity`) (FR-4, AD-6)
**And** el sistema valida que el paquete esté instalado (vía `<queries>` del manifest) y muestra error si no lo está (FR-4)
**And** el onboarding incluye el paso de exención de optimización de batería con acceso directo a Ajustes (FR-15)
**And** el estado de exención se muestra en la pantalla de configuración (FR-15)
**And** la UI cumple UX-DR5 y UX-DR8

## Epic 2: Sesión de Presencia Activa

El usuario puede iniciar, mantener y terminar una sesión de presencia: despliegue secuencial (app objetivo en primer plano → overlay negro → FGS), patrón de salida (4-taps), kill switch en la notificación, watchdog de anomalías (pantalla apagada, overlay ausente, permiso revocado, salida de primer plano, batería baja) y recuperación ante fallos. Se apoya en los permisos del Epic 1.
**FRs covered:** FR-5, FR-6, FR-7, FR-8, FR-9, FR-10, FR-11, FR-12, FR-13, FR-14, FR-16, FR-17

<!-- End story repeat -->
