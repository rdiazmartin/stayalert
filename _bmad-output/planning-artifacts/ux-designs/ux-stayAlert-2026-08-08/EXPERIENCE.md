---
name: stayAlert
status: draft
sources:
  - {planning_artifacts}/prds/prd-stayAlert-2026-08-08/prd.md
  - {planning_artifacts}/research/technical-android-presence-keep-awake-research-2026-08-08.md
updated: 2026-08-08
---

# stayAlert — Experience Spine

> Herramienta internal de operador único. Mobile Android nativo (Compose, Material 3). Postura: silenciosa, de una sola función. El producto es el overlay negro; la app es su antesala. Paired con `DESIGN.md` (identidad visual).

## Foundation

- **Form-factor:** mobile Android nativo (Compose, Material 3 dark). Sin modo claro — la app es oscura por defecto y única (contexto de uso: pantalla negra).
- **UI system:** Material 3 (Compose). `DESIGN.md` es la referencia de identidad visual; este spine es la experiencia.
- **Superficies:** MainActivity (pantalla principal + onboarding), overlay de aislamiento (ventana del sistema, no una superficie de la app), notificaciones (FGS + fin de sesión).

## Information Architecture

| Surface | Reached from | Purpose |
|---|---|---|
| Pantalla principal | App open (cold) | Botón "Iniciar Jornada" + estado de sesión + acceso a configuración |
| Configuración | Pantalla principal (icono engranaje) | Permisos, app objetivo, exención de batería |
| Aviso de uso responsable | Primer inicio (modal sobre la pantalla principal) | Aceptación única (FR-3) |
| Overlay de aislamiento | Inicio de sesión (ventana del sistema) | Pantalla negra + patrón de salida |
| Notificaciones | Sistema | FGS (sesión activa) + fin de sesión |

Sin navegación compleja: la pantalla principal es el hub; configuración es un nivel de profundidad; el overlay es una ventana del sistema, no una pantalla de la app. Modal de aviso: un solo nivel de profundidad.

→ Composición de referencia: `mockups/` (pendiente). Spine wins on conflict.

## Voice and Tone

Microcopy en español. La voz de marca vive en `DESIGN.md.Brand & Style`; aquí el tono de los textos.

| Do | Don't |
|---|---|
| "Iniciar Jornada" | "¡Empieza tu jornada!" |
| "Sesión activa" | "¡Estás disponible!" |
| "Sesión terminada: pantalla apagada" | "Error: screen off" |
| "Falta el permiso de overlay. Tócalo para abrir Ajustes." | "Permission denied" |
| "La app objetivo no está instalada." | "Package not found" |
| Corto, directo, sin exclamaciones | Jerga técnica, tono de marketing, emojis |

## Component Patterns

Comportamiento. Los specs visuales viven en `DESIGN.md.Components`.

| Component | Use | Behavioral rules |
|---|---|---|
| Botón principal | Pantalla principal | Tap → inicia sesión (si validación OK) o muestra motivo de bloqueo. Deshabilitado si falta algo. |
| Indicador de estado | Pantalla principal | Texto + punto. Refleja el estado real de la sesión (StateFlow). |
| Tarjeta de configuración | Configuración | Filas con label + valor. Tap → detalle o toggle. |
| Diálogo de aviso | Primer inicio | Modal único, no dismissable por fuera. Botón "Entiendo y acepto" → persiste en DataStore. |
| Notificación FGS | Sistema | Acción "Detener" → termina sesión (kill switch, FR-11). |
| Notificación fin de sesión | Sistema | Texto "Sesión terminada: {motivo}" (FR-12). |

## State Patterns

| State | Surface | Treatment |
|---|---|---|
| Inactiva | Pantalla principal | Botón habilitado, punto gris, "Sin sesión activa" |
| Lanzando | Pantalla principal | Botón deshabilitado con spinner, "Abriendo app objetivo…" |
| Aislada | Overlay + notificación | Overlay negro, notificación "Sesión activa", punto verde |
| Deteniendo | Overlay → pantalla principal | Overlay se destruye, notificación "Sesión terminada: {motivo}" |
| Permiso pendiente | Pantalla principal | Botón deshabilitado + mensaje "Falta: {permiso}" con acceso directo |
| App objetivo no instalada | Pantalla principal | Botón deshabilitado + mensaje "La app objetivo no está instalada" |
| Batería baja (15%) | Notificación | Aviso "Batería baja — la sesión terminará al 5%" |
| Batería crítica (5%) | Notificación | "Sesión terminada: batería baja" |
| Watchdog (pantalla apagada / overlay ausente / permiso revocado / app objetivo fuera de primer plano) | Notificación | "Sesión terminada: {motivo}" |

## Interaction Primitives

- Tap para actuar. Sin gestos custom fuera del patrón de salida.
- **Patrón de salida** (overlay): 4 toques consecutivos en la esquina superior derecha (región 15% × 15%), ventana temporal 500 ms entre toques. Sin feedback visual (el overlay es negro absoluto); el feedback es la destrucción del overlay + notificación de fin de sesión.
- **Banned:** carousels, animaciones hero, badges, gamificación, push de re-engagement, gestos de swipe en la app.

## Accessibility Floor

Comportamiento. El contraste visual vive en `DESIGN.md`.

- **TalkBack:** cada elemento interactivo etiquetado con rol + estado. El botón principal anuncia "Iniciar Jornada" / "Sesión activa". El overlay de aislamiento es una ventana del sistema sin contenido — TalkBack no tiene nada que anunciar (comportamiento esperado; el patrón de salida es táctil, no accesible por TalkBack — limitación documentada).
- **Dynamic type:** respetado en todos los niveles; la UI debe renderizar legible en el ajuste de fuente más grande sin truncar controles.
- **Reduce Motion:** sin animaciones que omitir (la app no usa motion decorativo).
- **Tap targets ≥ 48dp** (Android).
- **Contraste:** texto sobre `surface-base`/`surface-raised` cumple AA (ink-primary `#E6E6E6` sobre `#121212` ≈ 15:1; ink-secondary `#9E9E9E` sobre `#121212` ≈ 7:1). Acento `#4CAF50` con texto `accent-on` `#0B3D0F` ≈ 4.6:1 (AA para texto normal).

## Key Flows

### Flow 1 — Iniciar jornada (Roberto, mañana, antes de concentrarse)

1. Roberto abre stayAlert (cold open → pantalla principal).
2. Pulsa "Iniciar Jornada".
3. La app valida permisos + app objetivo (si falta algo: mensaje + acceso directo; si todo OK: continúa).
4. La app abre la app objetivo en primer plano.
5. 1 segundo después, el overlay negro cubre la pantalla.
6. **Climax:** la pantalla queda negra absoluta; la notificación "Sesión activa" aparece; el estado de Teams permanece "Disponible".
7. Roberto deja el teléfono en la mesa; la sesión se mantiene hasta que él la detenga o el watchdog la termine.

Failure: app objetivo no instalada → mensaje claro, sin sesión. Permiso pendiente → mensaje + acceso directo a Ajustes.

### Flow 2 — Salir del modo (Roberto, tarde, termina su jornada)

1. Roberto pulsa 4 veces consecutivas en la esquina superior derecha de la pantalla negra.
2. El overlay se destruye al instante.
3. **Climax:** la pantalla vuelve a la normalidad; la notificación "Sesión terminada: patrón de salida" confirma el fin.
4. Roberto queda en el launcher; la sesión está terminada.

Failure: patrón interrumpido (toque fuera de región o fuera de ventana temporal) → contador se reinicia, overlay permanece, sin feedback (comportamiento esperado).

### Flow 3 — Primer uso (Roberto, instalación inicial)

1. Roberto instala el APK por sideload y abre la app.
2. **Climax:** el modal de aviso de uso responsable aparece — "Esta app mantiene una señal de presencia activa sin interacción del usuario. Su uso puede no estar alineado con la política de tu empleador." — con botón "Entiendo y acepto".
3. Roberto acepta; el onboarding guía: permiso de overlay → permiso de notificaciones → exención de batería → configuración de app objetivo (default Teams).
4. La pantalla principal queda lista con el botón habilitado.

Failure: usuario no acepta el aviso → no puede iniciar sesión (FR-3); puede cerrar la app.

### Flow 4 — Watchdog termina la sesión (Roberto, sesión activa, pantalla se apaga)

1. Roberto deja el teléfono; la pantalla se apaga por timeout (o pulsa el botón de encendido).
2. El watchdog detecta la pantalla apagada.
3. **Climax:** la sesión termina en < 5 s; la notificación "Sesión terminada: pantalla apagada" aparece.
4. Roberto reabre la app: estado Inactiva, sin residuos.

Failure: proceso eliminado por el sistema → la sesión termina sin notificación (FR-17 garantiza consistencia de estado, no notificación); al reabrir, estado Inactiva.
