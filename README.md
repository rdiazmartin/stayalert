# stayAlert

Mantén tu estado "Disponible" en tu app de comunicación corporativa (Microsoft Teams y otras) mientras trabajas en otra cosa.

stayAlert abre la app objetivo en primer plano, despliega una pantalla negra a pantalla completa (apaga los píxeles en OLED y bloquea toques accidentales) y mantiene la app objetivo en primer plano — el mecanismo oficialmente documentado por Microsoft que conserva el estado "Disponible" en móvil. Para salir, un patrón táctil de seguridad (4 toques en la esquina superior derecha) restaura el dispositivo al instante.

> **Aviso de uso responsable:** esta app mantiene una señal de presencia activa sin interacción del usuario. Su uso puede no estar alineado con la política de tu empleador. Úsala solo en dispositivos propios.

## Características

- **Aviso de uso responsable** en el primer inicio (aceptación única, persistida).
- **Auditoría y guiado de permisos**: overlay ("Mostrar sobre otras apps") y notificaciones, con acceso directo a los menús de Ajustes.
- **App objetivo configurable**: paquete y actividad principal (default `com.microsoft.teams`), con validación de instalación.
- **Exención de batería del fabricante**: guiado a Ajustes para que el servicio no sea eliminado.
- **Sesión de presencia**: app objetivo en primer plano → overlay negro → servicio en primer plano con notificación.
- **Patrón de salida**: 4 toques en la esquina superior derecha (región 15% × 15%, ventana 500 ms).
- **Watchdog de sesión**: termina la sesión automáticamente ante pantalla apagada, overlay ausente, permiso revocado, salida de primer plano o batería baja.
- **100% on-device**: sin red, sin cuentas, sin telemetría.

## Requisitos

- JDK 17
- Android SDK (compileSdk 37, minSdk 26, targetSdk 36)
- Android Studio (recomendado) o Gradle 9.5+ desde CLI

## Compilar

```bash
# Compilar APK de debug
./gradlew assembleDebug

# Ejecutar tests unitarios
./gradlew test

# Ejecutar lint
./gradlew lint
```

El APK de debug se genera en `app/build/outputs/apk/debug/app-debug.apk`.

### Emulador

```bash
# Crear AVD (una vez)
avdmanager create avd -n stayalert_avd -k "system-images;android-34;google_apis;x86_64" -d pixel_5

# Lanzar emulador
emulator -avd stayalert_avd

# Instalar y abrir
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.stayalert/.ui.MainActivity
```

## Guía de uso

### Primer inicio

1. Abre stayAlert. Aparece el **aviso de uso responsable** — léelo y pulsa **"Entiendo y acepto"**.
2. Pulsa el icono de **configuración** (engranaje, esquina superior derecha).
3. **Permisos**: toca cada permiso pendiente para abrir el menú de Ajustes correspondiente y concédelo:
   - **Permiso de overlay** → "Mostrar sobre otras apps" → activa stayAlert.
   - **Permiso de notificaciones** → activa las notificaciones de stayAlert.
4. **App objetivo**: verifica que el paquete y la actividad sean correctos (default: Microsoft Teams). Si la app no está instalada, verás el aviso "La app objetivo no está instalada".
5. **Batería**: toca "Exención de batería" y permite que stayAlert ignore la optimización de batería (evita que el sistema elimine el servicio).

### Iniciar una jornada

1. En la pantalla principal, pulsa **"Iniciar Jornada"**.
2. La app objetivo se abre en primer plano; 1 segundo después, la pantalla se cubre con el **overlay negro**.
3. Aparece la notificación **"Sesión activa"** con la acción **"Detener"**.
4. Deja el teléfono en la mesa. La pantalla permanece encendida (píxeles negros en OLED) y tu estado permanece "Disponible".

### Terminar una jornada

- **Patrón de salida**: pulsa **4 veces consecutivas** en la **esquina superior derecha** de la pantalla negra (ventana de 500 ms entre toques). El overlay se destruye al instante.
- **Desde la notificación**: pulsa **"Detener"** en la notificación de sesión activa.
- **Automático**: el watchdog termina la sesión si la pantalla se apaga, el overlay desaparece, el permiso se revoca, la app objetivo sale de primer plano o la batería baja del 5%. Recibirás una notificación con el motivo.

## Distribución

v1 se distribuye como **APK sideload** (sin Google Play). El APK combina permisos de overlay y notificaciones; verifica la firma y el hash antes de instalar. No compartas el APK fuera de tus dispositivos.

## Arquitectura

- **Layered + single-activity + UDF** (Compose, Material 3 dark).
- `ui/` — Compose (pantalla principal, configuración, aviso modal).
- `domain/` — `SessionController` (único propietario del estado de sesión), eventos, comandos.
- `data/` — `SettingsRepository` (DataStore), `PermissionAuditor`, `Notifier`.
- `system/` — `OverlayController`, `PresenceForegroundService`, `Watchdog`, `TargetAppLauncher`.
- DI manual por constructor (sin Hilt).

## Licencia

Uso personal/internal. Sin garantías.
