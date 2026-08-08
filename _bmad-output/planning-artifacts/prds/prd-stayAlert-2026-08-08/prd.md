---
title: stayAlert
created: 2026-08-08
updated: 2026-08-08
status: final
---

# PRD: stayAlert
*Working title — confirm.*

## 0. Document Purpose

Este PRD define **stayAlert**, una aplicación Android utilitaria que mantiene el estado "Disponible" en aplicaciones de comunicación corporativa (Microsoft Teams y otras) evitando el cambio a "Away" por inactividad, mientras minimiza el impacto energético y visual del dispositivo. Está dirigido al PM (Roberto), al flujo downstream de BMad (UX, Architecture, Epics/Stories) y al equipo de desarrollo. Se apoya en la investigación técnica previa — `_bmad-output/planning-artifacts/research/technical-android-presence-keep-awake-research-2026-08-08.md` — que valida la viabilidad del mecanismo (foreground + overlay negro) y define la estrategia de mockeo de Teams para pruebas. Este PRD no duplica esa investigación: la referencia y traduce sus hallazgos a requisitos. Vocabulario anclado en el Glosario (§3); features agrupadas con FRs anidadas y numeradas globalmente; supuestos etiquetados inline e indexados (§10).

## 1. Vision

stayAlert es una aplicación Android de una sola función: **mantenerte "Disponible" en tu app de comunicación corporativa mientras trabajas en otra cosa**. Cuando la inicias, abre la app objetivo (p. ej. Microsoft Teams), despliega una pantalla negra a pantalla completa que apaga los píxeles en pantallas OLED y bloquea toques accidentales, y mantiene la app objetivo en primer plano (foreground) — el mecanismo oficialmente documentado por Microsoft que conserva el estado "Disponible" en móvil. Para salir, un patrón táctil de seguridad (4 toques en la esquina superior derecha) restaura el dispositivo al instante.

El problema que resuelve es real y cotidiano: las plataformas de comunicación corporativa marcan "Away" tras periodos de inactividad, y en móvil el estado pasa a "Away" cuando la app va a background. stayAlert mantiene la app objetivo en primer plano y cubre su contenido con una pantalla negra, de modo que el usuario no necesita dejar visible la app (con sus notificaciones y contenido) mientras trabaja en otra cosa.

Es una herramienta personal/internal: se distribuye como APK sideload, sin dependencia de Google Play, sin backend, sin cuentas. Se configura una vez (permisos + app objetivo) y se usa con un solo botón.

## 2. Target User

### 2.1 Jobs To Be Done

- **Funcional**: mantener el estado "Disponible" en mi app de comunicación corporativa durante jornadas de trabajo sin interacción con el dispositivo.
- **Funcional**: iniciar y detener el modo de aislamiento con el mínimo de pasos (un botón para iniciar; un patrón táctil para salir).
- **Contextual**: no tener que dejar la pantalla de la app objetivo visible (con su contenido y notificaciones) mientras trabajo en otra cosa.
- **Contextual**: minimizar el consumo energético de la pantalla durante el aislamiento (píxeles negros apagados en OLED).
- **Emocional**: confianza en que la sesión no se interrumpe sola y que salir del modo es siempre posible.
- **Emocional**: seguridad de que la herramienta no interfiere con la app objetivo (no toca su UI, no dispara acciones).

### 2.2 Non-Users (v1)

- Usuarios que necesitan mantener presencia en **múltiples** apps objetivo simultáneamente (v1 soporta una sola sesión con una sola app objetivo).
- Usuarios que quieren mantener la presencia con la **pantalla apagada** (v1 exige pantalla encendida; el TR documenta que la presencia móvil depende del foreground).
- Usuarios que necesitan **distribución vía Google Play** (v1 se distribuye por sideload). `[ASSUMPTION: confirmado con el usuario — distribución por sideload APK]`

### 2.3 Key User Journeys

- **UJ-1. Roberto inicia su jornada de presencia.**
  - **Persona + contexto:** Roberto, trabajador remoto, necesita que Teams lo muestre "Disponible" mientras se concentra en otra tarea sin tocar el teléfono.
  - **Entry state:** app instalada por sideload; permisos concedidos en el onboarding previo; app objetivo configurada (default `com.microsoft.teams`).
  - **Path:** abre stayAlert → pulsa "Iniciar Jornada" → la app abre la app objetivo en primer plano → 1 segundo después se despliega el overlay negro a pantalla completa → la sesión queda activa con notificación visible.
  - **Climax:** la pantalla queda negra (píxeles apagados en OLED), sin elementos gráficos; el estado de Teams permanece "Disponible".
  - **Resolution:** Roberto deja el teléfono en la mesa; la sesión se mantiene hasta que él la detenga o el sistema detecte una anomalía (pantalla apagada, batería baja).
  - **Edge case:** si la app objetivo no está instalada, stayAlert muestra un error claro y no inicia la sesión (FR-5).

- **UJ-2. Roberto sale del modo de aislamiento.**
  - **Persona + contexto:** Roberto termina su jornada y quiere usar el teléfono con normalidad.
  - **Entry state:** sesión activa; overlay negro cubriendo la pantalla.
  - **Path:** pulsa 4 veces consecutivas en la esquina superior derecha de la pantalla negra (ventana temporal de 500 ms entre toques) → el overlay se destruye → la sesión termina → la pantalla vuelve a su brillo normal y aparece la notificación "Sesión terminada".
  - **Climax:** la pantalla se restaura al instante y el teléfono responde con normalidad.
  - **Resolution:** Roberto queda en el launcher o en la app que estaba antes; la sesión está terminada.
  - **Edge case:** si el patrón se interrumpe (toque fuera de la esquina o fuera de la ventana temporal), el contador se reinicia y el overlay permanece (FR-7).

## 3. Glossary

- **App objetivo** — La aplicación de comunicación corporativa cuya presencia se mantiene (p. ej. Microsoft Teams). Identificada por su **paquete** y su **actividad principal**. Configurable en v1.
- **Paquete** — Identificador de aplicación Android (p. ej. `com.microsoft.teams`). Usado para lanzar la **app objetivo**.
- **Actividad principal** — Componente de la **app objetivo** que se lanza al abrirla (p. ej. `com.microsoft.teams.activities.MainActivity`).
- **Sesión** — Periodo activo de aislamiento: desde "Iniciar Jornada" hasta la detección del **patrón de salida** o de una anomalía (pantalla apagada, batería baja, fallo del sistema). Estados: `Inactiva`, `Lanzando`, `Aislada`, `Deteniendo`.
- **Overlay de aislamiento** — Ventana a pantalla completa, negra absoluta (`#000000`), sin elementos gráficos, que cubre la **app objetivo** durante la **sesión**. Bloquea toques accidentales y apaga píxeles en pantallas OLED. Cubre la zona de la app objetivo; la status bar y el shade de quick settings permanecen accesibles (comportamiento del sistema).
- **Patrón de salida** — Secuencia de seguridad: 4 toques consecutivos en la esquina superior derecha de la pantalla (región: 15% del ancho × 15% del alto), con ventana temporal de 500 ms entre toques. Único mecanismo de salida de la **sesión**.
- **Retención de pantalla** — Mecanismo del sistema (`FLAG_KEEP_SCREEN_ON`) que mantiene la pantalla encendida mientras el **overlay de aislamiento** es visible.
- **Onboarding** — Flujo de primer uso que audita y guía la concesión de permisos (overlay, notificaciones), la exención de batería del fabricante y la configuración de la **app objetivo**.
- **Watchdog de sesión** — Mecanismo de supervisión que detecta anomalías durante la **sesión** (pantalla apagada, overlay ausente, permiso revocado, salida de primer plano de la **app objetivo**) y la termina con notificación al usuario.
- **Mock de Teams** — Aplicación de prueba con paquete `com.microsoft.teams` instalada en el emulador para validar el comportamiento sin Teams real. Declara los componentes que stayAlert lanza, registra los toques recibidos para aserciones de prueba y expone su estado de lifecycle (resumed/stopped) vía log para verificar foreground. La validación E2E usa UI Automator (tests cross-app fuera del proceso de stayAlert).

## 4. Features

### 4.1 Onboarding y Configuración

**Description:** En el primer inicio, stayAlert muestra un aviso de uso responsable, audita el estado del sistema y guía al usuario por los menús de configuración para conceder los privilegios necesarios: permiso de overlay ("Mostrar sobre otras apps") y permiso de notificaciones (para el servicio en primer plano). También permite configurar la **app objetivo** (paquete + actividad principal), con `com.microsoft.teams` como default. Realiza UJ-1.

**Functional Requirements:**

#### FR-1: Auditoría de permisos

El usuario puede ver el estado de cada permiso requerido (overlay, notificaciones) en la pantalla de configuración, con indicación clara de concedido/pendiente. Realiza UJ-1.

**Consequences (testable):**
- La pantalla de configuración muestra el estado real de los 2 permisos al abrirse (verificado contra `Settings.canDrawOverlays()` y el estado de notificaciones).
- El estado se refresca al volver de cada menú de Ajustes.

#### FR-2: Guiado a los menús de permisos

El usuario puede pulsar un acceso directo por permiso pendiente que abre el menú de Ajustes correspondiente (overlay: `ACTION_MANAGE_OVERLAY_PERMISSION`; notificaciones: ajustes de la app). Realiza UJ-1.

**Consequences (testable):**
- Cada acceso directo abre el menú correcto del sistema.
- Al regresar a stayAlert, el estado del permiso se re-audita automáticamente.

#### FR-3: Aviso de uso responsable

En el primer inicio, el usuario ve un aviso en lenguaje claro que informa que la app mantiene una señal de presencia activa sin interacción del usuario y que su uso puede no estar alineado con la política de su empleador; el usuario debe aceptarlo para continuar. Realiza UJ-1.

**Consequences (testable):**
- El aviso se muestra una sola vez (persistido en DataStore).
- Sin aceptación, no se puede iniciar una **sesión**.

#### FR-4: Configuración de la app objetivo

El usuario puede configurar la **app objetivo** (paquete y actividad principal) desde la pantalla de configuración; el default es `com.microsoft.teams` con su actividad principal. Realiza UJ-1.

**Consequences (testable):**
- El valor configurado persiste entre reinicios (DataStore).
- El sistema valida que el paquete esté instalado antes de permitir iniciar una **sesión** (ver FR-5). La detección de instalación requiere declaración `<queries>` en el manifest (visibilidad de paquetes, API 30+).

**Out of Scope:**
- Selección de la app objetivo desde una lista de apps instaladas (v1 usa entrada manual de paquete/actividad). `[ASSUMPTION: la entrada manual es aceptable para uso internal; una lista de apps instaladas se evalúa en v2]`

**Notes:**
- El overlay usa `TYPE_APPLICATION_OVERLAY` (requiere minSdk 26) con permiso `SYSTEM_ALERT_WINDOW`; ver TR §Integration Patterns.
- Si la app objetivo declara `HIDE_OVERLAY_WINDOWS` (API 31+, p. ej. apps bancarias), el overlay no se dibujará sobre ella; la sesión debe detectarlo (watchdog) y terminar con notificación.

### 4.2 Inicio y Ejecución de Sesión

**Description:** El botón principal "Iniciar Jornada" arranca la secuencia: (1) valida permisos y app objetivo; (2) abre la **app objetivo** en primer plano; (3) 1 segundo después despliega el **overlay de aislamiento**; (4) inicia el servicio en primer plano con notificación. El **overlay de aislamiento** mantiene la **retención de pantalla** activa. Realiza UJ-1.

**Functional Requirements:**

#### FR-5: Validación previa al inicio

El usuario puede iniciar una **sesión** solo si los permisos (overlay, notificaciones) están concedidos, el aviso de uso responsable fue aceptado y la **app objetivo** está instalada; en caso contrario se muestra un mensaje claro indicando qué falta. Realiza UJ-1.

**Consequences (testable):**
- Con cualquier permiso pendiente, aviso no aceptado o app objetivo no instalada, el botón "Iniciar Jornada" no inicia la sesión y muestra el motivo.
- Con todo concedido, el botón inicia la sesión.

#### FR-6: Despliegue secuencial

Al iniciar la **sesión**, el sistema: (a) lanza la **app objetivo** en primer plano; (b) transcurrido 1 segundo, despliega el **overlay de aislamiento**; (c) inicia el servicio en primer plano con notificación. Realiza UJ-1.

**Consequences (testable):**
- La app objetivo queda en primer plano (estado resumed) antes de desplegar el overlay.
- Si la app objetivo no se confirma en primer plano (p. ej. el lanzamiento falla o la task no trae al frente la actividad esperada), la sesión aborta limpiamente con mensaje de error, sin overlay huérfano.
- El overlay se despliega en < 500 ms tras el disparo de inicio.
- La pantalla permanece encendida durante toda la **sesión** (retención de pantalla activa).
- En Android 15+, si el servicio en primer plano debe iniciarse desde background, el overlay debe estar visible antes del inicio (regla del sistema); la secuencia (a)→(b)→(c) lo garantiza por construcción.

**Out of Scope:**
- Retención de CPU con pantalla apagada (`PARTIAL_WAKE_LOCK`): descartado en v1 porque la presencia móvil depende del foreground y Doze ignora el wake lock. `[ASSUMPTION: confirmado con el usuario — pantalla encendida + overlay negro]`

#### FR-7: Patrón de salida

El usuario puede terminar la **sesión** pulsando 4 veces consecutivas en la esquina superior derecha de la pantalla (región: 15% del ancho × 15% del alto), con ventana temporal de 500 ms entre toques; al detectarse el patrón, el sistema destruye el **overlay de aislamiento**, detiene el servicio en primer plano y libera la **retención de pantalla**. Realiza UJ-2.

**Consequences (testable):**
- 4 toques consecutivos en la región definida dentro de la ventana temporal terminan la sesión en < 500 ms tras el 4º toque.
- Un toque fuera de la región o fuera de la ventana temporal reinicia el contador sin terminar la sesión.
- Tras la salida, el overlay no existe, el servicio está detenido y la pantalla puede apagarse con normalidad.

**Out of Scope:**
- Mecanismos de salida alternativos (botón en pantalla): v1 usa exclusivamente el patrón de salida, complementado por la acción de detención en la notificación del servicio (FR-11). `[ASSUMPTION: confirmado con el usuario — solo 4-taps como patrón táctil]`

### 4.3 Overlay de Aislamiento

**Description:** El **overlay de aislamiento** es una ventana a pantalla completa, negra absoluta (`#000000`), sin elementos gráficos, desplegada sobre la **app objetivo** durante la **sesión**. Apaga los píxeles en pantallas OLED (ahorro energético), bloquea toques accidentales y recibe el **patrón de salida**. Mantiene la **retención de pantalla** mientras es visible. Realiza UJ-1, UJ-2.

**Functional Requirements:**

#### FR-8: Overlay negro a pantalla completa

Durante la **sesión**, el sistema despliega una ventana de color negro absoluto `#000000`, sin elementos gráficos, sobre la **app objetivo**. Realiza UJ-1.

**Consequences (testable):**
- El overlay cubre la zona de la app objetivo y es `#000000` puro.
- El overlay no muestra ningún elemento gráfico (ni texto, ni iconos, ni indicadores).
- El overlay es la ventana topmost sobre la app objetivo: los toques del usuario caen sobre él y no llegan a la app objetivo.
- El overlay declara `FLAG_SECURE` (no capturable por screenshot/grabación).
- La status bar y el shade de quick settings permanecen accesibles (comportamiento del sistema; no es un fallo).

#### FR-9: Bloqueo de toques accidentales

Los toques del usuario sobre el **overlay de aislamiento** no producen ningún efecto sobre la **app objetivo** (salvo el **patrón de salida**). Realiza UJ-1.

**Consequences (testable):**
- Un toque simple en cualquier zona del overlay no produce ningún cambio de estado en la app objetivo.
- La app objetivo no recibe eventos táctiles mientras el overlay está visible.

#### FR-10: Retención de pantalla

Mientras el **overlay de aislamiento** es visible, la pantalla permanece encendida (retención de pantalla activa). Realiza UJ-1.

**Consequences (testable):**
- La pantalla no se apaga por timeout mientras la sesión está activa.
- Al terminar la sesión, la retención se libera y la pantalla puede apagarse con normalidad.

### 4.4 Servicio en Primer Plano y Ciclo de Vida

**Description:** Durante la **sesión**, stayAlert ejecuta un servicio en primer plano (FGS) con notificación visible que mantiene el proceso vivo y el **overlay de aislamiento** estable. La notificación incluye una acción de detención (kill switch). Un **watchdog de sesión** supervisa anomalías y termina la sesión con notificación al usuario. Realiza UJ-1, UJ-2.

**Functional Requirements:**

#### FR-11: Servicio en primer plano con acción de detención

El sistema mantiene un servicio en primer plano (tipo `specialUse`) con notificación visible mientras la **sesión** está activa; la notificación incluye una acción "Detener sesión" que termina la sesión. Realiza UJ-1, UJ-2.

**Consequences (testable):**
- Al iniciar la sesión, el FGS está activo con notificación visible.
- La acción "Detener sesión" de la notificación termina la sesión (mismo efecto que el patrón de salida).
- Al terminar la sesión, el FGS se detiene y la notificación desaparece.

#### FR-12: Feedback de fin de sesión

Al terminar la **sesión** (por patrón de salida, acción de notificación o watchdog), el usuario recibe una notificación "Sesión terminada" con el motivo. Realiza UJ-2.

**Consequences (testable):**
- Toda terminación de sesión produce una notificación con el motivo (patrón, detención manual, pantalla apagada, batería baja, fallo del sistema).

#### FR-13: Watchdog de sesión

El **watchdog de sesión** detecta anomalías durante la **sesión** — pantalla apagada, overlay ausente, permiso de overlay revocado, o salida de primer plano de la **app objetivo** (vía `UsageStatsManager` si el permiso está concedido) — y termina la sesión con notificación al usuario. Realiza UJ-1.

**Consequences (testable):**
- Al apagarse la pantalla durante la sesión, la sesión termina en < 5 s con notificación "Sesión terminada: pantalla apagada".
- Si el overlay desaparece o el permiso de overlay se revoca, la sesión termina con notificación.
- Si la app objetivo deja de estar en primer plano (con permiso de uso concedido), la sesión termina con notificación.
- Si la app objetivo declara `HIDE_OVERLAY_WINDOWS` y el overlay no se dibuja sobre ella, la sesión termina con notificación.

**Notes:**
- La detección de primer plano de la app objetivo usa `UsageStatsManager` (requiere permiso de uso de apps, concedido en el onboarding); si el usuario no lo concede, el watchdog cubre los demás casos y la salida de primer plano queda sin detectar (limitación documentada).

#### FR-14: Manejo de crash de la app objetivo

Si la **app objetivo** se cierra o crashea durante la **sesión**, el sistema termina la sesión con notificación (no deja el overlay sobre el launcher). Realiza UJ-1.

**Consequences (testable):**
- Tras un crash de la app objetivo, la sesión termina y el overlay desaparece en < 5 s.
- El usuario recibe notificación "Sesión terminada: la app objetivo se cerró".

#### FR-15: Onboarding de exención de batería del fabricante

El **onboarding** incluye un paso que guía al usuario a la exención de optimización de batería y, según fabricante (Xiaomi, Samsung, etc.), a la configuración de autostart/no-restricciones para que el servicio no sea eliminado. Realiza UJ-1.

**Consequences (testable):**
- El onboarding muestra el paso de exención de batería (con acceso directo a Ajustes) al menos una vez.
- El estado de exención se muestra en la pantalla de configuración.

#### FR-16: Manejo de batería baja

Durante la **sesión**, si la batería baja del 15%, el sistema notifica al usuario; si baja del 5%, termina la sesión con notificación. Realiza UJ-1.

**Consequences (testable):**
- Al cruzar 15% de batería durante la sesión, se emite una notificación de aviso.
- Al cruzar 5%, la sesión termina con notificación "Sesión terminada: batería baja".

#### FR-17: Recuperación ante muerte del proceso

Si el proceso de stayAlert es eliminado durante la **sesión**, la **sesión** termina (el overlay desaparece y el servicio se detiene) sin dejar estado inconsistente. Realiza UJ-1.

**Consequences (testable):**
- Tras un kill del proceso, no queda overlay huérfano ni servicio activo.
- Al reabrir la app, el estado es `Inactiva` y no hay residuos de la sesión anterior.

**Out of Scope:**
- Reanudación automática de la sesión tras reinicio del dispositivo (v1 no auto-reanuda). `[ASSUMPTION: aceptable para uso internal; se evalúa en v2]`

## 5. Ethics, Policy & Liability

stayAlert devuelve al usuario el control sobre su propia señal de presencia. La vigilancia constante de la actividad de los trabajadores es una práctica cuestionable; esta herramienta restaura la agencia del usuario sobre cómo y cuándo se reporta su disponibilidad. Dicho esto, el PRD reconoce con honestidad los riesgos:

- **Política del empleador**: el uso de la app puede violar la política corporativa o los términos de empleo del usuario (la mayoría de empleadores tratan la tergiversación de presencia como evento disciplinario). El usuario asume ese riesgo; stayAlert no se distribuye fuera de los dispositivos del propietario.
- **Dispositivo gestionado**: si el dispositivo es gestionado por el empleador (MDM/BYOD), la instalación de APKs sideload o el uso de overlays pueden ser detectados o bloqueados. Ver OQ-4.
- **Transparencia**: la app muestra un aviso de uso responsable en el primer inicio (FR-3) y no oculta su función.
- **Sin datos**: la app no recopila, transmite ni almacena datos de uso; 100% on-device, sin red, sin cuentas.
- **Higiene de distribución**: el APK se distribuye firmado con la clave de release del propietario; quien lo reciba debe verificar la firma y el hash antes de instalar (el APK combina permisos de overlay y notificaciones; una copia re-firmada por terceros sería un vehículo de malware). No compartir el APK fuera de los dispositivos del propietario.

## 6. Non-Goals (Explicit)

- **No es un "jiggler"**: no simula movimiento de ratón, teclado ni toques; no inyecta ningún evento en el sistema. Mantiene la app objetivo en primer plano y cubre su contenido.
- **No interactúa con la UI de la app objetivo**: no automatiza acciones dentro de ella; no lee su interfaz.
- **No mantiene presencia con pantalla apagada**: v1 exige pantalla encendida (mecanismo verificado).
- **No soporta múltiples sesiones simultáneas**: una sola app objetivo por sesión.
- **No se distribuye vía Google Play en v1**: distribución por sideload APK.
- **No recopila datos ni tiene backend**: 100% on-device, sin cuentas, sin telemetría.

## 7. MVP Scope

### 7.1 In Scope

- Onboarding de permisos (overlay, notificaciones) con guiado a Ajustes.
- Aviso de uso responsable en el primer inicio.
- Configuración de la app objetivo (paquete + actividad principal, default Teams).
- Inicio de sesión con despliegue secuencial (app objetivo → 1 s → overlay).
- Overlay negro a pantalla completa con retención de pantalla y `FLAG_SECURE`.
- Patrón de salida (4 toques esquina superior derecha, región 15% × 15%).
- FGS `specialUse` con notificación y acción "Detener sesión".
- Watchdog de sesión (pantalla apagada, overlay ausente, permiso revocado, salida de primer plano).
- Manejo de crash de la app objetivo y de batería baja.
- Onboarding de exención de batería del fabricante.
- Mock de Teams para pruebas en emulador (sin Teams real instalado).

### 7.2 Out of Scope for MVP

- Lista de apps instaladas para seleccionar la app objetivo (entrada manual en v1). *Deferred v2.*
- Reanudación automática de sesión tras reinicio. *Deferred v2.*
- Distribución vía Google Play. *Deferred v2 — `[NOTE FOR PM]` si el alcance crece a más usuarios, revisar.*
- Soporte multi-app objetivo simultáneo. *Deferred v3.*
- Modo pantalla apagada con CPU activa. *Deferred v3 — requiere validación empírica del comportamiento de Teams.*

## 8. Success Metrics

**Primary**
- **SM-1a (proxy, emulador)**: La **app objetivo** (mock) permanece en primer plano (resumed) durante sesiones de ≥ 8 h continuas con 0 toques recibidos, con el dispositivo cargando o ≥ 80% de batería al inicio. Validates FR-6, FR-9, FR-10.
- **SM-1b (validación empírica, dispositivo real)**: Teams real muestra "Disponible" a través de ≥ 2 días laborales completos de sesión en el dispositivo de Roberto, con su Teams y su tenant. Validates FR-6. *(El timeout de foreground de Teams móvil no está documentado oficialmente — medición empírica; ver OQ-1.)*
- **SM-2**: 0 eventos táctiles recibidos por la app objetivo durante sesiones de prueba E2E (aserciones con el mock de Teams). Validates FR-8, FR-9.

**Secondary**
- **SM-3**: Salida del modo de aislamiento en < 500 ms tras el 4º toque del patrón. Validates FR-7.
- **SM-4**: Sin wake locks activos fuera de sesión; retención de pantalla liberada al terminar. Validates FR-10.
- **SM-5**: Toda terminación de sesión produce notificación con motivo (100% en pruebas). Validates FR-12, FR-13, FR-14, FR-16.

**Counter-metrics (do not optimize)**
- **SM-C1**: No optimizar la velocidad de salida reduciendo la ventana temporal del patrón por debajo de 500 ms — aumentaría los falsos positivos de salida accidental.

## 9. Validation Gate (Go/No-Go)

**Premisa central del producto:** Teams móvil (y la app objetivo configurada) se mantiene en estado "Disponible" mientras la app está en primer plano (foreground) con la pantalla encendida, sin interacción del usuario. Esta premisa no está documentada oficialmente por Microsoft (OQ-1) — el gate la valida empíricamente **antes** de invertir en la construcción de la app.

**Cuándo se ejecuta:** en la Fase 0/1 del roadmap de implementación, antes de construir la UI y las features. Es la primera tarea de validación del sprint inicial.

**Procedimiento (mínimo viable):**
1. En el dispositivo real de Roberto, con Teams real instalado y su tenant, abrir Teams y dejarlo en primer plano con la pantalla encendida (sin tocar, sin bloqueo de pantalla).
2. Registrar el momento en que el estado pasa a "Away" (o confirmar que no pasa).
3. Repetir en ≥ 2 días consecutivos para descartar variabilidad.

**Regla de decisión:**
- **GO**: Teams permanece "Disponible" ≥ 4 h de foreground ininterrumpido en ambos días → la premisa se sostiene; se procede a construir el MVP completo.
- **STOP/pivot**: Teams pasa a "Away" antes de 4 h en cualquiera de los días → la premisa es falsa; se detiene la construcción y se evalúa el pivot documentado en el TR (overlay semi-transparente con paso de toques, o abandono del producto).

**Resultado del gate:** se registra en el memlog del proyecto y condiciona la entrada al desarrollo del MVP. SM-1b (validación empírica) es la versión extendida de este gate aplicada al producto terminado.

## 10. Open Questions

1. **Timeout de foreground de Teams móvil**: ¿cuánto tarda Teams en pasar a "Away" con la app en primer plano sin interacción? No documentado oficialmente; debe medirse empíricamente (SM-1b). Esta medición es la premisa central del producto — ver Validation Gate.
2. **Actividad principal de Teams**: ¿cuál es el componente exacto a lanzar en `com.microsoft.teams`? Verificar con `cmd package resolve-activity` en un dispositivo con Teams.
3. **OEM quirks**: ¿qué configuraciones adicionales (exención de batería, autostart) requieren los dispositivos Xiaomi/Samsung para no matar el FGS? Documentar por fabricante en la guía de instalación (FR-15).
4. **Dispositivo gestionado**: ¿el dispositivo objetivo es gestionado por el empleador (MDM/BYOD)? Si lo es, la instalación sideload y el uso de overlays pueden ser detectados o bloqueados.
5. **Comportamiento del shade y llamadas entrantes**: ¿qué ocurre cuando el usuario tira del shade de quick settings o recibe una llamada durante la sesión? Verificar en validación; el overlay no cubre la status bar.

## 11. Assumptions Index

- §2.2 — Distribución por sideload APK (confirmado con el usuario).
- §4.1 FR-4 — Entrada manual de paquete/actividad es aceptable para uso internal; lista de apps instaladas se evalúa en v2.
- §4.2 FR-6 — Pantalla encendida + overlay negro (confirmado con el usuario); sin PARTIAL_WAKE_LOCK en v1.
- §4.2 FR-6 — El delay de 1 s entre lanzamiento de la app objetivo y despliegue del overlay es un valor inicial pendiente de validación empírica.
- §4.2 FR-7 — Solo patrón 4-taps como patrón táctil de salida (confirmado con el usuario); la notificación del FGS es el complemento de detención.
- §4.4 FR-17 — No auto-reanudación de sesión tras reinicio es aceptable para uso internal.
- §4.1 — El permiso de notificaciones es necesario para la visibilidad de la notificación del FGS (no para iniciarlo).
