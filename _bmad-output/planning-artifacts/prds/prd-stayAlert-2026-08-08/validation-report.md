# Validation Report — stayAlert

- **PRD:** `_bmad-output/planning-artifacts/prds/prd-stayAlert-2026-08-08/prd.md`
- **Rubric:** `.agents/skills/bmad-prd/assets/prd-validation-checklist.md`
- **Run at:** 2026-08-08T21:30:00Z
- **Grade:** Fair

## Overall verdict

PRD sólido: forma de capability-spec que encaja con una herramienta internal de operador único, con 14/14 FRs con consecuencias testables, Non-Goals honestos y métricas de éxito que validan la tesis — incluyendo contra-métricas. Lo que está en riesgo es la precisión en los puntos de carga: el único mecanismo de salida se especifica con una región placeholder, el comportamiento de inyección/backoff está subespecificado, y los dos números sobre los que corre el producto (240 s, 1 s) se presentan como asentados sin evidencia ni tag de asunción.

La revisión adversarial cambia materialmente el cuadro: el PRD no debe pasar a UX/Architecture tal como está. Tres críticos: (C1) SM-1 es la tesis no validada disfrazada de métrica, sin gate go/no-go y sin importar el riesgo principal del TR ni su mitigación; (C2) el toque fantasma es teatro — el PRD contradice su propia investigación (el tap nunca llega a Teams, así que FR-7/FR-9/SM-4 regulan un evento inerte, y FR-9 es vacuo o viola privacidad); (C3) es una herramienta de fraude de presencia que nunca se nombra como tal — sin sección de ética/política, sin reconocer riesgo laboral/MDM, y con el "no es un jiggler" como evasión de categoría. Tres altos: salida como punto único de fallo, fallos de sesión a mitad de camino no manejados y silenciosos, y SM-1 no medible en emulador (el mock no tiene lógica de presencia).

## Dimension verdicts

- Decision-readiness — adequate
- Substance over theater — strong
- Strategic coherence — strong
- Done-ness clarity — adequate
- Scope honesty — strong
- Downstream usability — strong
- Shape fit — strong

## Findings by severity

### Critical (3)

**[Adversarial] C1. SM-1 es la tesis del producto disfrazada de métrica, sin gate de validación** (§7 SM-1, §8 OQ1, §6.2; TR risk table)
SM-1 admite en su propio paréntesis que el comportamiento del mecanismo no está documentado y necesita "medición empírica". Eso no es una métrica; es la razón de existir del producto sin validar. No hay validación secuenciada antes de la inversión, el riesgo principal del TR (Microsoft → presencia basada en interacción) y su mitigación (overlay semi-transparente) no se importan — FR-10 incluso la bloquea estructuralmente — y no hay punto go/no-go.
Fix: añadir sección "Validation Gate" (medir timeout de foreground de Teams real; regla de decisión go/no-go; degradar SM-1 a objetivo de validación; importar el riesgo y su mitigación como asunción con trigger).

**[Adversarial] C2. El toque fantasma es teatro — el PRD contradice su propio mecanismo** (§1, §3, FR-7, FR-9, SM-4, SM-C1; TR)
El tap inyectado cae sobre el overlay opaco y nunca llega a Teams; un evento que nunca llega no puede "reforzar la actividad percibida". FR-7/FR-9/SM-4/SM-C1 regulan un evento inerte. SM-4 es una tautología ("fallos con reintento exitoso no cuentan como fallo"). FR-9 es incoherente: ¿lee el árbol del overlay (vacuo) o el de Teams (viola §5 y crea superficie de datos)?
Fix: decidir y enunciar — en v1 el tap es vestigial (habilitador técnico para el futuro modo semi-transparente), degradando FR-7/FR-9/SM-4/SM-C1 a "sin valor de éxito"; o adoptar el overlay semi-transparente ahora. FR-9 debe especificar qué árbol lee.

**[Adversarial] C3. Ética y política: herramienta de fraude de presencia que nunca se nombra como tal** (§1, §2.1, §2.3 UJ-1, §5, §2.2)
El único escenario donde la herramienta se necesita (usuario no en la máquina) es el escenario donde es engañosa. "No es un jiggler" es evasión de categoría. No se reconoce: violación de política laboral, dispositivo gestionado por MDM (hueco de viabilidad), multiplicación de exposición si se comparte el APK, auditoría por admins de tenant.
Fix: sección "Ethics, Policy & Liability" con aviso en-app en primer lanzamiento; Open Question "¿El dispositivo objetivo es gestionado por el empleador (MDM)?"

### High (3)

**[Adversarial] H1. Punto único de fallo en la salida — y cada modo de fallo sale en silencio** (FR-6, §4.2 Out of Scope, FR-14, UJ-2, §8 OQ4)
El patrón 4-taps es el único mecanismo de salida; si el overlay falla, la única recuperación es force-stop. El patrón tiene cero confirmación: un teléfono en el bolsillo puede producirlo, y la salida es silenciosa (el usuario cree que la sesión corre).
Fix: control de stop en la notificación del FGS como requisito v1; feedback visible post-salida ("Sesión terminada"); documentar tasa de falsos positivos aceptada.

**[Adversarial] H2. Modos de fallo a mitad de sesión no manejados — la sesión muere en silencio** (FR-13, FR-14, FR-12, §8 OQ4, §2.1)
OEM mata el FGS (el overlay desaparece y Teams queda visible — el fallo más sensible), permiso de overlay revocado, servicio de accesibilidad deshabilitado, app objetivo crashea, botón de encendido (pantalla apagada). Ninguno se detecta; la sesión sigue "viva en papel".
Fix: watchdog de liveness (overlay ausente/servicio deshabilitado/pantalla apagada → terminar sesión con notificación); manejo de crash de la app objetivo; onboarding de exención de batería OEM como paso v1.

**[Adversarial] H3. El mock no puede medir lo que el PRD afirma — SM-1 no es verificable en el emulador** (SM-1, SM-2, §3, §6.1)
El mock no tiene lógica de presencia; no hay estado "Disponible" que observar. El emulador solo valida condiciones necesarias (resumed, 0 toques), no el objetivo. SM-1 solo es medible en el dispositivo de Roberto, con su Teams y su tenant — muestra de 1, sujeta a política del admin y cambios server-side.
Fix: dividir SM-1 en (a) proxy testeable — "app objetivo resumed ≥ 8 h con 0 toques" (emulador) — y (b) validación empírica — "Teams real Disponible ≥ 2 días laborales" (dispositivo real), con la regla go/no-go de C1. No presentar (a) como evidencia de (b).

### Medium (4)

**[Rubric] Números centrales afirmados sin evidencia** (§4.2 FR-5, §4.3 FR-7, §7 SM-C1) — "1 segundo", "240 s", "punto de equilibrio" presentados como asentados mientras OQ-1 admite que el comportamiento de Teams no está medido. Fix: etiquetar como `[ASSUMPTION: pending empirical validation]`.

**[Rubric] Riesgo de política laboral no superficializado** (§1, §2.2) — el propósito es evadir un monitor de presencia y solo se reconoce el riesgo de Play. Fix: una línea en Non-Goals u Open Questions.

**[Adversarial] M1. Sin modelo de amenazas para SYSTEM_ALERT_WINDOW + AccessibilityService** (§1, §5, FR-1/FR-2) — la combinación de troyanos bancarios; compartición de APK = superficie de trojanización; sin guía de firma/hash; FLAG_SECURE ausente (debería ser no-opcional para una app de ocultamiento visual). Fix: modelo de amenazas + mitigaciones v1 (sin red, sin almacenamiento, sin logging de árboles ajenos, verificación de firma).

**[Adversarial] M2. SM-1 ignora la física de 8 h con pantalla encendida** (SM-1, FR-12) — la batería muere antes; la métrica asume enchufe o batería llena. Fix: "≥ 8 h con el dispositivo cargando o ≥ 80% de batería al inicio" + FR de manejo de batería baja.

**[Adversarial] M3. El PRD bloquea la única mitigación del TR para el riesgo existencial** (FR-10, §5; TR) — el overlay opaco v1 es un invariante que impide la adaptación semi-transparente planificada. Fix: asunción "FR-10 es solo v1; la variante semi-transparente es la adaptación planificada".

**[Adversarial] M4. "Pantalla completa" sobreafirma lo que el overlay cubre** (FR-5, FR-10, UJ-1; TR) — la status bar y el shade quedan accesibles (probabilidad Alta, aceptado en el TR); el PRD lo tapa. Fix: decir lo que entrega + qué pasa con el shade/llamadas entrantes.

### Low (5)

**[Rubric] Asunción sin tag inline** (§9 vs §2.2) — distribución sideload. Fix: añadir tag inline.

**[Rubric] Asunción sin tag inline** (§9 vs §4.1) — permiso de notificaciones. Fix: añadir tag inline.

**[Adversarial] L1. Semántica de lanzamiento puede derrotar "resumed" de FR-5** (FR-5, UJ-1) — existing-task con FLAG_ACTIVITY_NEW_TASK varía; ventana de 1 s sin protección. Fix: verificar en Fase 4; FR de aborto limpio si la app objetivo no se confirma resumed.

**[Adversarial] L2. El check de instalado de FR-3 necesita `<queries>`** (FR-3) — visibilidad de paquetes API 30+. Fix: implicación de manifest en la FR.

**[Adversarial] L3. Punto de inyección sin especificar** (FR-7, FR-9) — dónde cae el tap y su relación con la región de salida de FR-6. Fix: fijar coordenadas y declarar la relación.

**[Adversarial] L4. SM-4 es una métrica diseñada para pasar** (SM-4) — no puede fallar por construcción. Fix: "éxito al primer intento ≥ 99%" o eliminar.

**[Adversarial] L5. Inconsistencia de idioma** (documento) — confirmar idioma downstream y working title.

## Mechanical notes

- Assumption Index roundtrip: 2 de 6 entradas carecen de tag inline `[ASSUMPTION]` — §2.2 y §4.1.
- Continuidad de IDs: limpia (FR-1…FR-14, SM-1…SM-5 + SM-C1/C2, UJ-1/2).
- Deriva de glosario: ninguna.
- "Working title — confirm." (§0) es una decisión colgante.
- Secciones requeridas: todas presentes.

## Reviewer files

- `review-rubric.md`
- `review-adversarial-general.md`
