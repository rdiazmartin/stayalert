---
name: stayAlert
description: Presence keeper. Dark, quiet, single-purpose. One button, one black screen. No decoration.
colors:
  surface-base: '#121212'
  surface-raised: '#1E1E1E'
  surface-overlay: '#000000'
  ink-primary: '#E6E6E6'
  ink-secondary: '#9E9E9E'
  ink-disabled: '#616161'
  accent: '#4CAF50'
  accent-on: '#0B3D0F'
  border-hairline: '#2C2C2C'
  error: '#CF6679'
  warning: '#F0A020'
typography:
  title:
    note: 'Android Headline Small — 24sp'
  body:
    note: 'Android Body Large — 16sp'
  meta:
    note: 'Android Body Small — 12sp'
  button:
    note: 'Android Label Large — 14sp, medium weight'
rounded:
  sm: 8dp
  md: 16dp
  full: 999dp
spacing:
  '1': 4dp
  '2': 8dp
  '3': 12dp
  '4': 16dp
  '5': 24dp
  '6': 32dp
  '7': 48dp
---

## Brand & Style

stayAlert es una herramienta de una sola función: mantenerte "Disponible" mientras trabajas en otra cosa. La identidad visual es deliberadamente **silenciosa** — la app existe para desaparecer detrás de una pantalla negra. No hay marca que presumir, no hay gamificación, no hay notificaciones de re-engagement. La pantalla principal es un botón y un estado; el overlay de aislamiento es negro absoluto sin un solo píxel de UI.

El lenguaje visual sigue esa postura: superficies oscuras y planas (Material 3 dark), un único acento verde que significa "sesión activa / presencia viva", y nada más. El verde es el único color cromático — se usa exclusivamente para el estado de sesión y la acción primaria. La app se siente como un interruptor, no como una aplicación.

## Colors

- **Surface Base (`#121212`)** — lienzo principal en modo oscuro (Material 3 dark baseline). La app es oscura por defecto y única: no hay modo claro. El contexto de uso es la pantalla negra del overlay; la app debe sentirse como su antesala.
- **Surface Raised (`#1E1E1E`)** — tarjetas, diálogos y superficies elevadas. Distinguida de la base solo por tono, nunca por sombra.
- **Surface Overlay (`#000000`)** — el negro absoluto del overlay de aislamiento. Píxeles apagados en OLED. No es un token de superficie normal: es el producto.
- **Ink Primary (`#E6E6E6`)** — texto principal.
- **Ink Secondary (`#9E9E9E`)** — texto secundario, metadatos, descripciones.
- **Ink Disabled (`#616161`)** — estados deshabilitados.
- **Accent (`#4CAF50`)** — el único color cromático. Verde de "presencia viva". Se usa en: el botón "Iniciar Jornada", el indicador de estado "Sesión activa", y el icono de la notificación del FGS. Nunca decorativo.
- **Accent On (`#0B3D0F`)** — texto/icono sobre el acento (contraste AA sobre `#4CAF50`).
- **Border Hairline (`#2C2C2C`)** — separadores al menor contraste legible.
- **Error (`#CF6679`)** — errores de validación (permisos pendientes, app objetivo no instalada).
- **Warning (`#F0A020`)** — avisos (batería baja, exención de batería pendiente).

Evitar: gradientes, sombras decorativas, acentos saturados adicionales, iconografía de estado con color (el estado se comunica con texto + un punto verde).

## Typography

Convenciones de plataforma Android (Material 3 type scale). `title` = Headline Small (24sp) para el título de la pantalla principal. `body` = Body Large (16sp) para contenido. `meta` = Body Small (12sp) para metadatos y descripciones. `button` = Label Large (14sp, medium) para botones. Dynamic type respetado en todos los niveles — la UI debe renderizar legible en el ajuste de fuente más grande sin truncar controles.

## Layout & Spacing

Escala: 4 / 8 / 12 / 16 / 24 / 32 / 48 dp. Márgenes móviles de plataforma (16dp). Columna única siempre. La pantalla principal respira: el botón y el estado centrados con `spacing/7` (48dp) de separación vertical. El overlay de aislamiento no tiene layout — es un lienzo negro sin contenido.

## Elevation & Depth

Sin elevación como dispositivo visual. Las superficies se distinguen por tono (`surface-raised` sobre `surface-base`), nunca por sombra. El overlay de aislamiento es la superficie más "profunda" del producto pero se percibe como plana — es el fondo del mundo.

## Shapes

`rounded/sm` (8dp) para filas y superficies pequeñas. `rounded/md` (16dp) para tarjetas y diálogos. `rounded/full` (999dp) exclusivamente para el botón principal "Iniciar Jornada" (píldora) — el único elemento con forma completamente redondeada, para que se lea como el interruptor central. Nada más usa píldoras.

## Components

- **Botón principal "Iniciar Jornada"** — Píldora (`rounded/full`), `accent` de fondo, texto `accent-on` en `button`. Ocupa el ancho del contenido (no full-bleed). Estado deshabilitado: `ink-disabled` de fondo con texto `surface-base`. Es el único componente con color de fondo cromático.
- **Indicador de estado** — Texto + punto. Punto de 8dp circular en `accent` cuando la sesión está activa; `ink-disabled` cuando inactiva. Texto en `body`: "Sesión activa" / "Sin sesión activa". Nunca iconografía de estado con color adicional.
- **Tarjeta de configuración** — `surface-raised`, `rounded/md`, borde `border-hairline`. Filas con label a la izquierda y valor/chevron a la derecha.
- **Diálogo de aviso** — `surface-raised`, `rounded/md`. Título en `title`, cuerpo en `body`, botón de confirmación en `accent`.
- **Notificación FGS** — Canal propio "Sesión activa". Icono monocromo, texto "Sesión activa — toca para detener". Acción "Detener" en la notificación.
- **Notificación de fin de sesión** — Canal "Fin de sesión". Texto "Sesión terminada: {motivo}".

## Do's and Don'ts

| Do | Don't |
|---|---|
| Un solo acento verde, usado solo en estado y acción primaria | Color-codificar por sección o decorar con acento |
| Estado comunicado con texto + punto verde | Iconografía de estado con color (✓, ⚠, ● de colores) |
| Superficies planas distinguidas por tono | Sombras, gradientes, elevación decorativa |
| El overlay negro absolutamente vacío | Cualquier elemento gráfico en el overlay (ni logo, ni texto, ni indicador) |
| Honor convenciones de plataforma (Material 3) | Navegación custom, drawer, hamburguesa |
| Texto en español, corto y directo | Jerga técnica, exclamaciones, tono de marketing |
