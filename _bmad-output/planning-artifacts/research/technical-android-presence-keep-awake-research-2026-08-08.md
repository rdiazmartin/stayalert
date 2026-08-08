---
stepsCompleted: [1, 2, 3, 4, 5, 6]
inputDocuments: []
workflowType: 'research'
lastStep: 6
research_type: 'technical'
research_topic: 'Android presence keeper: touch injection, power retention, overlay composition and Teams mocking'
research_goals: 'Validar viabilidad técnica de stayAlert (app Android de gestión de presencia): inyección de eventos táctiles vía AccessibilityService, retención de CPU/pantalla, overlay negro a pantalla completa, y estrategia de mockeo de Microsoft Teams en emulador para pruebas.'
user_name: 'Roberto'
date: '2026-08-08'
web_research_enabled: true
source_verification: true
---

# Research Report: technical

**Date:** 2026-08-08
**Author:** Roberto
**Research Type:** technical

---

## Research Overview

Investigación técnica sobre la viabilidad de **stayAlert**, una aplicación Android que mantiene el estado "Disponible" en Microsoft Teams evitando el cambio a "Away" por inactividad, mediante: (1) inyección de eventos táctiles fantasma vía `AccessibilityService.dispatchGesture()`, (2) retención de pantalla/CPU, y (3) una interfaz de aislamiento visual negra a pantalla completa con patrón táctil de salida. Incluye la estrategia de **mockeo de Microsoft Teams en emulador de Android Studio** (sin Teams instalado) para el plan de pruebas.

**Hallazgos clave:** `dispatchGesture()` (API 24+) es el único conducto no-root para inyección táctil; el tap inyectado cae en la ventana topmost (el overlay negro), por lo que **Teams nunca recibe toques** — el mecanismo que mantiene "verde" a Teams es el **foreground** (documentado oficialmente por Microsoft: en móvil, Away = app en background). El mockeo de Teams es viable instalando un APK con package `com.microsoft.teams` en el emulador limpio, con el launcher abstraído tras una interfaz para testabilidad. Riesgo principal: política de Google Play sobre automatización de accesibilidad.

**Metodología:** datos web actuales (2026-08-08) con verificación multi-fuente (developer.android.com, AOSP, Microsoft Learn, Google Play policy, support.microsoft.com), niveles de confianza por claim y citas completas. El resumen ejecutivo completo está en la sección de Síntesis Final al final del documento.

---

## Technical Research Scope Confirmation

**Research Topic:** Android presence keeper: touch injection, power retention, overlay composition and Teams mocking
**Research Goals:** Validar viabilidad técnica de stayAlert (app Android de gestión de presencia): inyección de eventos táctiles vía AccessibilityService, retención de CPU/pantalla, overlay negro a pantalla completa, y estrategia de mockeo de Microsoft Teams en emulador para pruebas.

**Technical Research Scope:**

- Architecture Analysis - design patterns, frameworks, system architecture
- Implementation Approaches - development methodologies, coding patterns
- Technology Stack - languages, frameworks, tools, platforms
- Integration Patterns - APIs, protocols, interoperability
- Performance Considerations - scalability, optimization, patterns

**Research Methodology:**

- Current web data with rigorous source verification
- Multi-source validation for critical technical claims
- Confidence level framework for uncertain information
- Comprehensive technical coverage with architecture-specific insights

**Scope Confirmed:** 2026-08-08

---

## Technology Stack Analysis

### Programming Languages

**Kotlin** es el lenguaje oficial y recomendado para Android (Google). Versión estable actual: **Kotlin 2.4.x** (2.4.10, julio 2026; K2 compiler estable desde 2.0.0, mayo 2024). Java sigue siendo soportado pero está en declive para apps nuevas. El caso de uso (servicios, corrutinas, concurrencia del orquestador) encaja perfectamente con Kotlin y sus coroutines.

_Popular Languages: Kotlin (recomendado oficial), Java (legado), C/C++ (NDK, no necesario aquí)_
_Emerging Languages: Kotlin Multiplatform (fuera de alcance: app Android nativa)_
_Language Evolution: Kotlin 2.x con compilador K2; ciclo de release semestral de lenguaje_
_Performance Characteristics: Kotlin/JVM con ART; corrutinas ideales para el orquestador temporal_
_Source: https://kotlinlang.org/docs/releases.html_

### Development Frameworks and Libraries

- **Jetpack Compose** es el toolkit UI recomendado oficialmente (BOM 2026.06.x). Arquitectura single-activity + Compose es el estándar moderno. Para stayAlert: la UI es simple (pantalla de activación + overlay negro), Compose es adecuado; el overlay negro puede ser una ventana Compose o una View clásica (según necesidades del WindowManager).
- **AndroidX Core / Lifecycle**: ViewModel, coroutines/Flow para el estado de sesión.
- **Hilt** es la librería DI recomendada oficialmente (aunque para una app pequeña DI manual es viable).
- **Accesibilidad**: `AccessibilityService` + `dispatchGesture()` (API 24+) — el único conducto no-root en runtime para inyección táctil.

_Major Frameworks: Jetpack Compose (UI), AndroidX Lifecycle (ViewModel/Flow), Hilt (DI)_
_Micro-frameworks: no aplica; ninguna micro-librería crítica_
_Evolution Trends: Compose como estándar; Views clásicas solo para casos especiales (overlay vía WindowManager puede usar View simple)_
_Ecosystem Maturity: ecosistema maduro y estable_
_Source: https://developer.android.com/develop/ui/compose; https://developer.android.com/topic/architecture; https://developer.android.com/training/dependency-injection_

### Database and Storage Technologies

**No aplica directamente** — stayAlert no requiere persistencia compleja. A lo sumo DataStore (preferencias) para configuración de usuario (intervalo del orquestador, habilitación de permisos). DataStore con `PreferencesDataStore` es la recomendación actual (reemplaza SharedPreferences).

_Relational Databases: No aplica_
_NoSQL Databases: No aplica_
_In-Memory Databases: No aplica_
_Data Warehousing: No aplica_
_Source: https://developer.android.com/topic/libraries/architecture/datastore_

### Development Tools and Platforms

- **Android Studio** con emulador (entorno disponible del proyecto) — la plataforma de desarrollo y prueba.
- **AGP 9.3.0** (julio 2026): requiere Gradle 9.5+, JDK 17, SDK Build Tools 36.0.0.
- **targetSdk**: requerido por Play — API 35+ desde ago 2025; **API 36+ desde ago 2026** para nuevas apps/actualizaciones.
- **minSdk**: sin mandato oficial de Play; el floor efectivo de AndroidX es 21. **Decisión recomendada: minSdk 26** (requerido por `TYPE_APPLICATION_OVERLAY`, API 26).
- **Testing**: JUnit 4 + MockK/Mockito, Robolectric 4.16 (unit tests en JVM sin emulador), Espresso 3.7.0 + Compose UI tests + **UI Automator** (tests cross-app, clave para validar el mock de Teams), todo ejecutable en emuladores de Android Studio.

_IDE and Editors: Android Studio (latest stable)_
_Version Control: Git (repositorio existente)_
_Build Systems: Gradle + AGP 9.3.0_
_Testing Frameworks: JUnit4, Robolectric, Espresso, Compose UI tests, UI Automator_
_Source: https://developer.android.com/build/releases/gradle-plugin; https://developer.android.com/training/testing/unit-testing/local-unit-tests; https://developer.android.com/training/testing/instrumented-tests; https://developer.android.com/jetpack/androidx/releases/test_

### Cloud Infrastructure and Deployment

**No aplica** para stayAlert: aplicación 100% on-device, sin backend. Distribución opcional vía Google Play (con las consideraciones de política para AccessibilityService y FGS specialUse) o sideload/APK para uso personal.

_Major Cloud Providers: No aplica_
_Container Technologies: No aplica_
_Serverless Platforms: No aplica_
_CDN and Edge Computing: No aplica_
_Source: No aplica_

### Technology Adoption Trends

_Migration Patterns: Apps nuevas → Kotlin + Compose + single-activity_
_Emerging Technologies: Compose multiplataforma; acceso a gestos por accesibilidad sigue siendo la vía estándar no-root para automatización_
_Legacy Technology: Views XML en declive para UI nueva; FULL_WAKE_LOCK deprecado (API 17)_
_Community Trends: Automatización de presencia vía AccessibilityService es un patrón conocido (comunidad XDA, Airtest, etc.)_
_Source: https://developer.android.com/topic/architecture; https://developer.android.com/reference/android/os/PowerManager#FULL_WAKE_LOCK_

### Key Technology Stack Findings

- **Kotlin 2.4.x + Jetpack Compose** es el stack recomendado y adecuado al tamaño del proyecto.
- **minSdk 26** (por TYPE_APPLICATION_OVERLAY), **targetSdk 36** (requisito Play ago 2026).
- **Stack de testing**: JUnit4 + Robolectric + Espresso/Compose UI + **UI Automator** (clave para E2E con el mock de Teams).
- Sin backend, sin base de datos: DataStore para preferencias.
- AGP 9.3.0, Gradle 9.5+, JDK 17.

---

## Integration Patterns Analysis

> Nota de dominio: stayAlert es una app 100% on-device sin backend. Las secciones genéricas de integración (REST/GraphQL, microservicios, brokers, etc.) no aplican. La superficie de integración real es: **app ↔ sistema operativo Android** (contratos de servicio) y **app ↔ aplicación objetivo** (lanzamiento y visibilidad). Se documenta esa superficie con verificación web.

### API Design Patterns — Integración app ↔ Sistema Operativo

- **Contrato de Accesibilidad (AccessibilityService)**: patrón de integración principal para inyección táctil. Se declara en `AndroidManifest.xml` con `<service android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" android:exported="true">`, intent-filter `android.accessibilityservice.AccessibilityService` y `<meta-data android:name="android.accessibilityservice" android:resource="@xml/accessibility_service_config"/>`. El XML debe incluir `android:canPerformGestures="true"`. No existe diálogo de permiso en runtime: el usuario debe activarlo en **Ajustes → Accesibilidad**; la app puede verificar el estado leyendo `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` y guiar al usuario al menú. [Confianza: Alta] — https://developer.android.com/guide/topics/ui/accessibility/service
- **Contrato de inyección de gestos**: `AccessibilityService.dispatchGesture(GestureDescription, GestureResultCallback, Handler)` — API 24+. Invocación: construir `GestureDescription` con `StrokeDescription` (path + duración), llamar y recibir resultado en callback. Un solo gesto a la vez; el toque real del usuario cancela el gesto en curso. [Confianza: Alta] — https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- **Contrato de ventana (WindowManager)**: crear overlay con `Context.createWindowContext(TYPE_APPLICATION_OVERLAY, ...)` → `getSystemService(WindowManager.class)`. Permiso `SYSTEM_ALERT_WINDOW` (special permission, vía `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`; verificar con `Settings.canDrawOverlays()`). En Android 11+ el intent siempre abre la lista general "Mostrar sobre otras apps". [Confianza: Alta] — https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION; https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY
- **Contrato de energía (PowerManager)**: `FLAG_KEEP_SCREEN_ON` como flag de ventana del overlay (mantiene pantalla encendida mientras la ventana sea visible; AOSP lo promueve en `DisplayContent.handleNotObscuredLocked`). Para CPU con pantalla apagada: `PARTIAL_WAKE_LOCK` + permiso `WAKE_LOCK` (ignorado en Doze; requiere exención de optimización de batería). [Confianza: Alta] — https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON; https://developer.android.com/reference/android/os/PowerManager.WakeLock
- **Contrato de servicio en primer plano (FGS)**: tipo `specialUse` con `FOREGROUND_SERVICE_SPECIAL_USE` + `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">` (justificación revisada en Play Console). Evitar `dataSync` (límite 6h/24h en API 35+). En Android 15, iniciar FGS desde background con `SYSTEM_ALERT_WINDOW` requiere overlay visible. [Confianza: Alta] — https://developer.android.com/develop/background-work/services/fgs/service-types
- **API de toques para patrón de salida**: `View.OnTouchListener` + `GestureDetector` sobre el overlay; para 4-taps consecutivos en esquina superior derecha, contador propio con ventana temporal (~300–500 ms) y región por `getRawX()/getRawY()`. [Confianza: Alta] — https://developer.android.com/reference/android/view/View#setOnTouchListener(android.view.View.OnTouchListener)

### Communication Protocols — Lanzamiento de la app objetivo

- **`adb shell am start`** (explícito por componente): `-n <paquete>/<actividad>`; `-W` espera el lanzamiento. Si el paquete/actividad no existe: `Error type 3` / `Error: Activity class ... does not exist` (`START_CLASS_NOT_FOUND`). Útil en tests y diagnóstico, no en producción (la app usa Intents). [Confianza: Alta] — https://developer.android.com/tools/adb; AOSP `ActivityManagerShellCommand.java`
- **Lanzamiento desde la app (producción)**: Intent explícito `Intent().setComponent(ComponentName("com.microsoft.teams", "com.microsoft.teams.activities.MainActivity"))` con `FLAG_ACTIVITY_NEW_TASK`. La visibilidad de paquetes (API 30+) NO bloquea iniciar actividades con intent explícito; solo afecta a `queryIntentActivities()/resolveActivity()`, que requeriría `<queries>` para comprobar si Teams está instalado. [Confianza: Alta] — https://developer.android.com/training/package-visibility/automatic
- **Deep links como alternativa**: `msteams://` y `https://teams.microsoft.com/l/...` son los handlers oficiales documentados por Microsoft para el cliente de escritorio; el comportamiento en Android móvil es inconsistente según reportes comunitarios (Medium). Un mock que registre el mismo scheme con `<intent-filter>` (ACTION_VIEW + `android:scheme="msteams"` + BROWSABLE) recibe el enlace de forma determinista. [Confianza: Media] — https://learn.microsoft.com/en-us/microsoftteams/platform/concepts/build-and-test/deep-links; https://developer.android.com/training/app-links/deep-linking
- **Detección de paquete instalado**: `adb shell pm list packages` (lista) y `adb shell cmd package resolve-activity --brief <paquete>` (actividad launcher). Códigos de error de `am start` comprobables en exit code/stderr. [Confianza: Alta/Media] — https://developer.android.com/tools/adb

### Data Formats and Standards

- **No aplican formatos de intercambio de datos** (sin backend, sin API HTTP). El único "contrato de datos" es la configuración persistida (DataStore, preferencias simples) y la definición del esquema de gestos (coordenadas/distancias en px, duración en ms). [Confianza: Alta]

### System Interoperability Approaches — Mockeo de Teams (clave para pruebas)

- **Mock APK con el mismo package name**: en un emulador limpio **sin Teams instalado**, un APK de prueba que declare `com.microsoft.teams` se instala sin problema; Android solo impide coinstalación cuando existe un paquete del mismo nombre con firma distinta (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). El mock debe declarar exactamente los componentes que el orquestador lanza (p. ej. `com.microsoft.teams/.activities.MainActivity`), o `am start` fallará con `Error type 3`. **No puede coexistir** con el Teams real en la misma imagen. [Confianza: Alta] — https://stackoverflow.com/questions/19959890; https://support.google.com/googleplay/android-developer/thread/232501122
- **Patrón de abstracción para testabilidad**: envolver el "lanzar app objetivo" tras una interfaz (p. ej. `TargetAppLauncher`): implementación de producción → Intent explícito; implementación de test → fake/no-op. Recomendado oficialmente: "Make your app components the only classes that rely on Android framework SDK APIs… Abstracting other classes away helps with testability". DI (Hilt o manual) para inyectar la implementación de test. [Confianza: Alta] — https://developer.android.com/topic/architecture
- **UI Automator para E2E cross-app**: los tests de UI Automator corren fuera del proceso de la app, pueden iniciar cualquier app (`startApp("com.microsoft.teams")`) y asertar sobre su UI — ideal para verificar el flujo completo (lanzar mock → overlay → gestos) en el emulador. [Confianza: Alta] — https://developer.android.com/training/testing/other-components/ui-automator
- **Estrategia de prueba recomendada (sin Teams real)**: (1) unit tests con fake launcher (Robolectric), (2) instrumentation/Compose UI tests de la app con el overlay real, (3) E2E con UI Automator contra el mock APK instalado con package `com.microsoft.teams`, (4) opcional: deep link `msteams://` hacia el mock registrado. [Confianza: Alta] — síntesis de fuentes oficiales anteriores

### Microservices Integration Patterns / Event-Driven Integration

- **No aplica** (sin microservicios, sin broker/mensajería). El único "evento" relevante es el patrón de comunicación interna: el orquestador (temporizador) → servicio de accesibilidad (inyección), y el listener de toques del overlay → controlador de sesión (salida del modo). Se implementa con callbacks/corrutinas in-process, no con infraestructura de mensajería. [Confianza: Alta]

### Integration Security Patterns

- **`filterTouchesWhenObscured` / tapjacking**: los toques del overlay sobre otra app son el patrón opuesto al ataque; Android 12+ bloquea la entrega de toques a apps oscurecidas por overlays no confiables de otro UID (protección automática). La app debajo puede declarar `HIDE_OVERLAY_WINDOWS` (API 31+) para que el overlay no se dibuje sobre ella (p. ej. apps bancarias). [Confianza: Alta] — https://developer.android.com/privacy-and-security/risks/tapjacking; https://developer.android.com/reference/android/Manifest.permission#HIDE_OVERLAY_WINDOWS
- **Sin OAuth/JWT/mTLS**: no hay API remota que autenticar. El único "secreto" es la configuración local (DataStore). [Confianza: Alta]
- **`FLAG_SECURE`** (opcional) en el overlay para impedir captura de pantalla/grabación durante el aislamiento visual. [Confianza: Alta] — https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE

### Key Integration Patterns Findings

- **Dos contratos críticos verificados**: AccessibilityService (`canPerformGestures=true` + `dispatchGesture`) y TYPE_APPLICATION_OVERLAY (`SYSTEM_ALERT_WINDOW` + flags de ventana).
- **Mockeo de Teams viable**: APK con package `com.microsoft.teams` se instala en emulador limpio; requiere que el mock declare los componentes lanzados; abstraer el launcher tras interfaz + UI Automator para E2E.
- **Deep links `msteams://`**: fiables solo contra el mock (registro determinista); comportamiento real en Android no confirmado oficialmente.
- **Sin backend ni formatos de datos**: integración 100% in-process + contratos del SO.

---

## Architectural Patterns and Design

### System Architecture Patterns

- **Arquitectura en capas + single-activity (recomendación oficial)**: UI layer (Compose, un solo `MainActivity`) + data layer (repositorio/preferencias DataStore) + capa de dominio opcional. Flujo de datos unidireccional (UDF) con ViewModel como state holder. Para stayAlert es suficiente: el estado de sesión (INACTIVA / LANZANDO / AISLADA) vive en un `SessionViewModel` (StateFlow) observado por la UI y el orquestador. [Confianza: Alta] — https://developer.android.com/topic/architecture
- **Arquitectura orientada a servicios del SO**: la app orquesta 3 componentes del sistema con ciclos de vida independientes, coordinados por un controlador central de sesión:
  1. **MainActivity** (Compose): pantalla de activación/onboarding.
  2. **PresenceForegroundService** (FGS `specialUse`): mantiene el proceso vivo, host del orquestador temporal.
  3. **PresenceAccessibilityService**: inyecta gestos (`dispatchGesture`) — corre en proceso separado del sistema, comunicado con la app vía `bindService`/`startService` y callbacks de estado.
  4. **OverlayController** (WindowManager): crea/destruye la ventana negra; detector del patrón de salida.
  - El **Controlador de Sesión** (singleton inyectado) es el orquestador maestro: "Iniciar Jornada" → lanza Teams (vía `TargetAppLauncher` abstracto) → +1s → muestra overlay → arranca temporizador → 4-taps → destruye overlay, detiene temporizador, libera pantalla. [Confianza: Media-Alta] — síntesis de contratos verificados en pasos 2-3
- **Patrón de testabilidad (clave)**: `TargetAppLauncher` interfaz → impl producción (Intent explícito a `com.microsoft.teams`), impl fake (tests unitarios) + mock APK con package `com.microsoft.teams` (tests E2E). [Confianza: Alta] — https://developer.android.com/topic/architecture

### Design Principles and Best Practices

- **Single Responsibility por contrato**: cada integración con el SO aislada en su propio componente (accesibilidad ≠ ventana ≠ energía), comunicada solo con el controlador de sesión — evita que fallos de un contrato rompan los demás. [Confianza: Alta]
- **Dependency Inversion para el launcher y el reloj**: `TargetAppLauncher` y un `Scheduler`/temporizador inyectables (corrutinas + `delay()`, o `Handler` con `postDelayed`) para tests deterministas (fake clock). [Confianza: Alta] — https://developer.android.com/training/dependency-injection
- **Fail-fast con degradación guiada**: al iniciar, auditar permisos (accesibilidad habilitada, overlay concedido, notificaciones) y guiar al usuario por menús con intents directos a Ajustes — patrón onboarding del PRD. [Confianza: Alta] — https://developer.android.com/guide/topics/ui/accessibility/service
- **Estados explícitos, no condiciones dispersas**: sesión modelada como sealed class (Idle, Launching, Isolated(since), Stopping) para que el patrón de salida y el temporizador reaccionen a transiciones, no a flags sueltos. [Confianza: Alta]

### Scalability and Performance Patterns

- **Escalabilidad no aplica** (app single-device). Sí aplican **consideraciones de rendimiento/energía**:
  - Overlay `#000000` en OLED/AMOLED: píxeles negros apagados → ahorro real vs. pantalla normal (≈40% de consumo de LCD en imágenes mayormente negras), aunque modesto vs. apagar la pantalla y con overhead de panel. [Confianza: Alta (física)/Media (magnitud por dispositivo)] — https://en.wikipedia.org/wiki/OLED
  - No usar `FLAG_NOT_TOUCHABLE` en el overlay: la ventana debe recibir el patrón de salida; un overlay opaco touchable ya bloquea toques a la app inferior (y Android 12+ refuerza anti-tapjacking con alpha ≤ 0.8). [Confianza: Alta] — https://developer.android.com/reference/android/view/WindowManager.LayoutParams
  - Temporizador de inyección: tap corto (~50–100 ms) cada intervalo configurable (default 240 s); un solo gesto a la vez; manejar el fallo de callback (p. ej. reintentar con backoff). [Confianza: Alta] — https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
  - **Costo energético**: mantener pantalla encendida es la principal carga; el overlay negro lo mitiga parcialmente. `FLAG_KEEP_SCREEN_ON` sin wake lock adicional (FULL_WAKE_LOCK deprecado). [Confianza: Alta] — https://developer.android.com/develop/background-work/background-tasks/awake/screen-on

### Integration and Communication Patterns

- **Comunicación in-process**: corrutinas/Flow entre ViewModel, FGS y OverlayController (mismo proceso); callback/binder para el AccessibilityService (proceso del sistema) — patrón `bindService` con `Messenger` o AIDL simple, o `LocalBroadcast`-style vía corrutinas si se mantiene en el mismo proceso (AccessibilityService puede correr en el proceso de la app si se declara así). [Confianza: Media]
- **Secuencia temporal del PRD**: lanzar Teams → esperar 1 s → overlay → temporizador. Implementable con `delay()` en el scope del controlador; el 1 s es configurable. [Confianza: Alta]
- **Patrón de salida**: contador de taps con ventana temporal (300–500 ms) y región (esquina sup. derecha, `rawX > ancho - margen && rawY < margen`); al alcanzar 4 taps consecutivos → transición Stopping. [Confianza: Alta] — https://developer.android.com/reference/android/view/GestureDetector

### Security Architecture Patterns

- **Permisos mínimos y especiales**: `SYSTEM_ALERT_WINDOW` (special), `BIND_ACCESSIBILITY_SERVICE` (firmado, en el service), `POST_NOTIFICATIONS` (runtime, para la notificación del FGS), `WAKE_LOCK` (normal) — solo si se usa PARTIAL_WAKE_LOCK. [Confianza: Alta] — https://developer.android.com/guide/topics/permissions/overview
- **Sin secretos ni red**: no hay superficie de ataque remota; riesgo principal es el uso indebido del overlay (tapjacking) mitigado por la protección nativa de Android 12+ y `HIDE_OVERLAY_WINDOWS` en apps sensibles. [Confianza: Alta] — https://developer.android.com/privacy-and-security/risks/tapjacking
- **Cumplimiento Play**: disclosure prominente del uso de accesibilidad, declaración en Play Console (no elegible como `isAccessibilityTool`), FGS `specialUse` justificado, uso determinista/reglas fijas del acceso (evitar "automatización autónoma" prohibida). Riesgo real de revisión por ser herramienta de automatización de presencia. [Confianza: Alta] — https://support.google.com/googleplay/android-developer/answer/10964491

### Data Architecture Patterns

- **DataStore Preferences** para configuración (intervalo de inyección, primera vez completada, paquete objetivo — default `com.microsoft.teams`). Sin base de datos. [Confianza: Alta] — https://developer.android.com/topic/libraries/architecture/datastore
- **Estado de sesión efímero**: no persiste sesiones activas (si el proceso muere, la sesión termina; el FGS minimiza el riesgo). [Confianza: Alta]

### Deployment and Operations Architecture

- **Entorno de pruebas disponible**: Android Studio + emulador; **sin Teams instalado** → se usa el **mock APK** (package `com.microsoft.teams`, actividad `MainActivity` que muestra UI identifiable y registra los taps recibidos en log para aserciones). [Confianza: Alta]
- **Fases de despliegue**: (1) debug en emulador con mock, (2) prueba manual en dispositivo físico real con Teams (validar comportamiento real del gesto), (3) distribución: sideload APK o Google Play (con los riesgos de política documentados). [Confianza: Alta]
- **Configuración del emulador**: instalar mock con `adb install`, conceder permisos especiales (overlay, accesibilidad) vía comandos adb o UI — automatizable en CI con `adb shell settings` y `cmd`. [Confianza: Alta] — https://developer.android.com/tools/adb

### Key Architectural Findings

- **Arquitectura en capas + single-activity** (estándar oficial) con 4 componentes SO coordinados por un Controlador de Sesión.
- **Patrón clave de testabilidad**: interfaz `TargetAppLauncher` + mock APK `com.microsoft.teams` + UI Automator → E2E completo sin Teams real.
- **Decisión energética**: overlay opaco touchable con `FLAG_KEEP_SCREEN_ON`; sin wake locks salvo necesidad de CPU en pantalla apagada.
- **Riesgo principal**: política de Google Play sobre automatización de presencia (mitigable con disclosure + reglas deterministas + distribución sideload como plan B).

---

## Implementation Approaches and Technology Adoption

### Technology Adoption Strategies

- **Greenfield con stack oficial**: proyecto nuevo → adoptar directamente Kotlin 2.4.x + Compose + AGP 9.3.0 (sin deuda de migración). No hay sistema legado que modernizar. [Confianza: Alta] — https://developer.android.com/build/releases/gradle-plugin
- **Adopción incremental de permisos (onboarding del PRD)**: auditar y guiar permisos en orden — (1) `SYSTEM_ALERT_WINDOW` (overlay), (2) Accesibilidad (activación manual en Ajustes, verificar `ENABLED_ACCESSIBILITY_SERVICES`), (3) `POST_NOTIFICATIONS` (FGS). Cada uno con intent directo a su menú. [Confianza: Alta] — https://developer.android.com/guide/topics/ui/accessibility/service
- **Estrategia de distribución en fases**: (1) debug en emulador con mock, (2) APK sideload en dispositivo real con Teams (validar comportamiento real), (3) opcional Google Play (con riesgos de política documentados). [Confianza: Alta]

### Development Workflows and Tooling

- **Android Studio + Gradle (AGP 9.3.0, Gradle 9.5+, JDK 17)**; Kotlin 2.4.x; Compose BOM 2026.06.x. [Confianza: Alta] — https://developer.android.com/build/releases/gradle-plugin
- **CI/CD**: GitHub Actions (repo existente) con jobs: lint + unit tests (JVM/Robolectric) + instrumented tests en emulador (managed device o `avdmanager`). Automatizar concesión de permisos en el emulador vía adb (`settings put secure enabled_accessibility_services`, `appops set` para overlay) para tests E2E. [Confianza: Alta] — https://developer.android.com/training/testing/instrumented-tests
- **Control de versiones**: Git; ramas por feature; PR con revisión (flujo BMad: DS → CR). [Confianza: Alta]

### Testing and Quality Assurance

- **Pirámide de tests adaptada**:
  1. **Unit (JVM/Robolectric)**: `SessionController` con fake clock y fake `TargetAppLauncher`; lógica del patrón 4-taps; transiciones de estado. [Confianza: Alta] — https://developer.android.com/training/testing/unit-testing/local-unit-tests
  2. **Instrumented (Compose UI tests)**: flujo de activación, onboarding de permisos, overlay visible. [Confianza: Alta] — https://developer.android.com/develop/ui/compose/testing
  3. **E2E (UI Automator) contra el mock APK `com.microsoft.teams`**: lanzar mock → overlay → verificar que el mock sigue en foreground (resumed) y que NO recibe toques (el tap cae en el overlay). [Confianza: Alta] — https://developer.android.com/training/testing/other-components/ui-automator
- **Validación empírica pendiente (emulador)**: (a) timeout de foreground de Teams móvil (no documentado oficialmente — medir cuándo pasa a Away con la app abierta sin tocar); (b) confirmar que el tap inyectado cae en el overlay y no en Teams (verificar con `uiautomator dump` / logs del mock). [Confianza: Media] — síntesis de hallazgos comunitarios
- **Mock de Teams**: APK con package `com.microsoft.teams`, actividad `MainActivity` que (1) muestra UI identificable, (2) registra en logcat los toques recibidos (para aserciones de "no recibió toques"), (3) expone su estado de lifecycle (resumed/stopped) vía broadcast o log para verificar foreground. [Confianza: Alta] — https://developer.android.com/tools/adb

### Deployment and Operations Practices

- **Emulador Android Studio** como entorno de pruebas (disponible); instalar mock con `adb install`; conceder permisos especiales vía adb para automatización. [Confianza: Alta] — https://developer.android.com/tools/adb
- **Observabilidad**: logcat estructurado del controlador de sesión (transiciones, taps inyectados, fallos de `dispatchGesture`); sin backend. [Confianza: Alta]
- **Operación en dispositivo real**: validar OEM quirks (Xiaomi/Samsung matan servicios en background — `dontkillmyapp.com`); documentar pasos de configuración para el usuario final (exención de batería, autostart). [Confianza: Media] — https://dontkillmyapp.com/

### Team Organization and Skills

- **Proyecto personal (Roberto)**: perfil full-stack Android necesario — Kotlin/Compose, AccessibilityService, WindowManager, testing instrumentado. Nivel intermedio declarado en config; el TR cubre los huecos de conocimiento. [Confianza: Alta]
- **Flujo BMad**: PRD → UX → Architecture → Epics/Stories → Sprint → Dev → Code Review. [Confianza: Alta]

### Cost Optimization and Resource Management

- **Costo energético (el recurso crítico)**: overlay `#000000` (píxeles OLED apagados) + `FLAG_KEEP_SCREEN_ON`; sin wake locks innecesarios; intervalo de inyección configurable (default 240 s) para minimizar trabajo. [Confianza: Alta] — https://en.wikipedia.org/wiki/OLED
- **Sin costos de infraestructura**: app on-device, sin backend, sin servicios cloud. [Confianza: Alta]

### Risk Assessment and Mitigation

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Play Store rechaza por política de accesibilidad/automatización | Media | Alto | Disclosure prominente + declaración Play Console + reglas deterministas; plan B: sideload APK |
| Microsoft endurece presencia móvil (exige interacción real) | Media | Alto | Documentado; mitigación futura: overlay semi-transparente (alpha ≤ 0.8) + NOT_TOUCHABLE para dejar pasar toques; monitorizar comunidad |
| OEMs matan el FGS/overlay (Xiaomi, Samsung) | Media | Medio | Guía de configuración (exención batería, autostart); FGS con notificación visible |
| `dispatchGesture` falla o es detectado (bug InputDispatcher #384188031) | Baja-Media | Medio | Callback de resultado + reintento con backoff; validar en emulador y dispositivo real |
| Overlay no cubre status bar / quick settings | Alta | Bajo | Aceptado: el aislamiento visual cubre la app; sistema UI queda accesible (comportamiento esperado) |
| Mock no refleja el layout real de Teams | Alta | Medio | Mock solo valida el mecanismo (foreground + no-toques); validación final en dispositivo real con Teams |

### Technical Research Recommendations

#### Implementation Roadmap

1. **Fase 0 — Scaffolding**: proyecto Android (Kotlin, Compose, minSdk 26, targetSdk 36), CI básico, mock APK `com.microsoft.teams`.
2. **Fase 1 — Núcleo**: `SessionController` (sealed state), `TargetAppLauncher` (interfaz + impl real + fake), FGS `specialUse`, onboarding de permisos.
3. **Fase 2 — Overlay**: `OverlayController` (TYPE_APPLICATION_OVERLAY, negro opaco, detector 4-taps esquina sup. derecha), `FLAG_KEEP_SCREEN_ON`.
4. **Fase 3 — Inyección**: `PresenceAccessibilityService` (`canPerformGestures=true`), orquestador temporal (tap 50–100 ms, intervalo 240 s), verificación de nodo clickable antes de inyectar.
5. **Fase 4 — Validación**: E2E UI Automator contra mock (foreground + sin toques recibidos), medición empírica del timeout de Teams, prueba en dispositivo real.

#### Technology Stack Recommendations

- **Kotlin 2.4.x + Jetpack Compose** (BOM 2026.06.x), single-activity, ViewModel + StateFlow, Hilt o DI manual.
- **minSdk 26 / targetSdk 36**; AGP 9.3.0, Gradle 9.5+, JDK 17.
- **DataStore** para configuración; sin base de datos.
- **Testing**: JUnit4 + Robolectric + Compose UI tests + UI Automator; mock APK `com.microsoft.teams`.

#### Skill Development Requirements

- AccessibilityService (declaración, `dispatchGesture`, ciclo de vida) — cubierto por este TR.
- WindowManager/overlays y flags de ventana — cubierto por este TR.
- Testing instrumentado en emulador (Compose UI, UI Automator) — práctica en Fase 4.

#### Success Metrics and KPIs

- **Funcional**: sesión activa mantiene Teams en foreground ≥ 8 h sin pasar a Away (medible en emulador con mock y en real).
- **Seguridad de UI**: 0 toques recibidos por el mock durante sesiones E2E (aserciones UI Automator).
- **Salida**: patrón 4-taps detectado en < 500 ms; overlay destruido y orquestador detenido.
- **Energía**: sin wake locks activos fuera de sesión; overlay negro verificado (captura de consumo opcional).
- **Calidad**: 100% tests verdes en CI antes de cada merge.

---

## Technical Research Conclusion

### Summary of Key Technical Findings

1. **Inyección táctil**: `AccessibilityService.dispatchGesture()` (API 24+) es el único conducto no-root en runtime. Límites reales: 60 s/gesto, 20 strokes, un gesto a la vez; el "timeout de 10 s" es un mito. El toque real del usuario cancela el gesto inyectado. Requiere `canPerformGestures="true"` y activación manual en Ajustes.
2. **El tap no llega a Teams (hallazgo central)**: los gestos inyectados caen en la ventana topmost — con el overlay negro opaco encima, el tap aterriza en el overlay. Teams no recibe ningún evento → **cero riesgo de disparar lógica de UI en Teams**. Lo que mantiene "verde" a Teams es el **foreground** (oficial: en móvil, Away = app en background). El tap es refuerzo, no mecanismo.
3. **Taps inofensivos por diseño**: tap puro 50–100 ms, cero movimiento, sin doble-tap; verificación del nodo de accesibilidad en el punto objetivo antes de inyectar (saltar si hay elemento clickable); zonas inertes (franja de status bar, espacio vacío de lista).
4. **Overlay**: `TYPE_APPLICATION_OVERLAY` (API 26+) + `SYSTEM_ALERT_WINDOW`; negro opaco touchable (recibe el patrón 4-taps y bloquea toques a la app inferior); `FLAG_KEEP_SCREEN_ON` mantiene la pantalla; `#000000` apaga píxeles OLED.
5. **Energía**: sin wake locks salvo necesidad de CPU con pantalla apagada (`PARTIAL_WAKE_LOCK` ignorado en Doze); FGS tipo `specialUse` (evitar `dataSync`: límite 6 h/24 h en API 35+).
6. **Mockeo de Teams (viable)**: APK con package `com.microsoft.teams` se instala en emulador limpio; el mock debe declarar los componentes lanzados; abstraer el launcher tras interfaz (`TargetAppLauncher`) + UI Automator para E2E; el mock registra toques recibidos para aserciones de "0 toques".
7. **Stack**: Kotlin 2.4.x + Compose (BOM 2026.06.x), minSdk 26 / targetSdk 36, AGP 9.3.0, DataStore, JUnit4 + Robolectric + Compose UI + UI Automator.
8. **Riesgos**: política Play sobre automatización (plan B: sideload); Microsoft endureciendo presencia móvil (mitigación futura: overlay semi-transparente alpha ≤ 0.8 + NOT_TOUCHABLE); OEMs matando FGS/overlay; bug InputDispatcher #384188031.

### Strategic Technical Impact Assessment

- **Viabilidad: ALTA.** Todos los mecanismos del PRD tienen soporte técnico verificado en Android moderno (API 26+). La arquitectura propuesta (Controlador de Sesión + 4 componentes SO) es implementable con el stack estándar.
- **La restricción de pruebas (sin Teams) no bloquea el desarrollo**: el mock APK + la abstracción del launcher permiten validar el 100% del mecanismo (foreground, overlay, inyección, salida) en el emulador. Solo la validación final de presencia real requiere un dispositivo con Teams.
- **Incertidumbre documentada**: el timeout de foreground de Teams móvil no está documentado oficialmente → medición empírica en Fase 4. La tendencia de Microsoft hacia presencia basada en interacción es el riesgo evolutivo principal.

### Next Steps Technical Recommendations

1. **Siguiente paso BMad**: formalizar el PRD (`bmad-prd`) incorporando los hallazgos (mecanismo = foreground + overlay; taps como refuerzo inofensivo), luego UX (`bmad-ux`) y Architecture (`bmad-architecture`).
2. **Fase 0 inmediata**: scaffolding del proyecto Android + mock APK `com.microsoft.teams` + CI básico.
3. **Validación empírica temprana** (antes de invertir en UI): en el emulador, medir el comportamiento de foreground del mock y confirmar que el tap inyectado cae en el overlay (uiautomator dump / logs).
4. **Decisión de distribución**: definir desde el inicio si el objetivo es Google Play (requiere disclosure + declaración de accesibilidad + FGS specialUse justificado) o sideload personal (sin fricción de política).

---

**Technical Research Completion Date:** 2026-08-08
**Research Period:** current comprehensive technical analysis
**Document Length:** As needed for comprehensive technical coverage
**Source Verification:** All technical facts cited with current sources
**Technical Confidence Level:** High - based on multiple authoritative technical sources

_This comprehensive technical research document serves as an authoritative technical reference on stayAlert (Android presence keeper) and provides strategic technical insights for informed decision-making and implementation._
