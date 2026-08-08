# PRD Quality Review — stayAlert

## Overall verdict

This is a strong PRD: a capability-spec shape that fits a single-operator internal tool, with 14/14 FRs carrying testable consequences, an honest Non-Goals section, and success metrics that validate the thesis rather than measure activity — including counter-metrics, which most PRDs lack. What's at risk is precision at the load-bearing points: the only exit mechanism is specified with a placeholder region, the injection/backoff behavior is underspecified, and the two numbers the whole product runs on (240 s interval, 1 s delay) are presented as settled without evidence or an assumption tag. The assumption index also has two roundtrip breaks. None of this blocks the PRD's usefulness, but downstream architecture and story creation will need these pinned before build.

## Decision-readiness — adequate

Decisions are mostly stated as decisions, not buried: "v1 usa exclusivamente el patrón de salida" (§4.2 FR-6 Out of Scope), "descartado en v1 porque los toques fantasma se descartan con pantalla apagada y Doze ignora el wake lock" (§4.2 FR-5 Out of Scope), and the `[NOTE FOR PM]` at §6.2 on Play distribution sits at a real tension. Trade-offs name what was given up, not just what was chosen. The four Open Questions (§8) are genuinely open — empirical validation items, not rhetorical. What keeps this from strong: the two core operating numbers are asserted as settled ("el intervalo default (240 s) es el punto de equilibrio", §7 SM-C1) while OQ-1 admits the underlying Teams behavior is unmeasured, and the most obvious pushback on this product — the employer-policy risk of evading presence status — is never acknowledged (only Play policy risk appears, §2.2).

### Findings
- **[medium]** Core numbers asserted without evidence (§4.2 FR-5, §4.3 FR-7, §7 SM-C1) — "transcurrido 1 segundo", "cada 240 s (intervalo configurable)", "el intervalo default (240 s) es el punto de equilibrio" — presented as settled while OQ-1 says the Teams foreground timeout "no está documentado oficialmente; debe medirse empíricamente". A decision-maker can't tell whether these are confirmed or provisional. *Fix:* tag the 1 s delay and 240 s default as `[ASSUMPTION: pending empirical validation]` (or cite the TR's basis), and state what would change them.
- **[medium]** Employer-policy risk not surfaced (§1, §2.2) — the product's purpose is evading a presence monitor, and the PRD acknowledges only the Play policy risk ("la política de Play sobre automatización de accesibilidad es un riesgo documentado", §2.2). The obvious objection — corporate policy/ToS exposure for the user — is dodged. *Fix:* one line in Non-Goals or Open Questions acknowledging the risk and the decision to accept it for internal use.

## Substance over theater — strong

No theater detected. One persona (Roberto) that drives both UJs and is referenced in FRs; the Vision (§1) is product-specific and non-swappable (black overlay, phantom taps, 4-tap exit); NFRs are bounded and product-specific ("intervalo configurable (DataStore) con rango validado (p. ej. 60–600 s)", §4.3; "desplegarse en < 500 ms", §4.4). The "Emotional" JTBDs (§2.1) are light but do real work — they justify the exit-pattern design and the "never touches the target app" guarantee. No findings.

## Strategic coherence — strong

The thesis is stated and the features serve it: keep "Disponible" while working elsewhere with minimal energy/visual impact (§1). Success metrics validate the thesis rather than measure activity — SM-1 ("permanece en estado 'Disponible' durante sesiones de ≥ 8 h continuas") is the thesis metric, and SM-2 ("0 eventos táctiles recibidos por la app objetivo") validates the core safety claim. Counter-metrics are present and meaningful (SM-C1, SM-C2), and the apparent tension between the energy JTBD (§2.1) and SM-C1's "no optimizar el consumo energético" is resolved honestly. MVP scope (§6) is a coherent problem-solving scope for a single-operator tool. No findings.

## Done-ness clarity — adequate

Unusually strong on the whole: every one of FR-1…FR-14 has testable consequences, with real bounds ("cada 240 s ± 5 s", "tap puro de 50–100 ms", "terminan la sesión en < 500 ms tras el 4º toque", "negro absoluto `#000000`"). But three spots would leave an engineer guessing, and one is load-bearing.

### Findings
- **[high]** Exit region is a placeholder (§4.2 FR-6) — "región definida, p. ej. 15% del ancho y 15% del alto" — the *only* exit mechanism is specified with a "p. ej." example. Architecture and stories cannot build the exit pattern from this. *Fix:* pin the region as v1 spec (e.g., "esquina superior derecha: 15% del ancho × 15% del alto"), keeping the example language out of the requirement.
- **[medium]** Backoff policy unspecified (§4.2 FR-8) — "reintenta con backoff y registra el fallo" but the consequence says "se reintenta en el siguiente ciclo o con backoff" — two alternatives, and "backoff" is never defined. *Fix:* state the policy (e.g., retry on next cycle, log the failure; no exponential backoff in v1).
- **[medium]** Untestable qualifier (§4.5 FR-13) — "El proceso no es eliminado por el sistema durante la sesión (en condiciones normales)". "En condiciones normales" is not a verifiable condition. *Fix:* define the test (e.g., 8 h session on a reference device without user interaction) or drop the qualifier.

## Scope honesty — strong

Non-Goals (§5) does real work and is specific ("No es un 'jiggler' de escritorio", "No es una herramienta de accesibilidad: el uso del AccessibilityService es instrumental"). De-scoping is explicit with reasons, not silent: wake-lock mode (§4.2 FR-5), alternative exit mechanisms (§4.2 FR-6), app list (§4.1 FR-3), auto-resume (§4.5 FR-14), each with a rationale and v2/v3 deferral (§6.2). Open-items density (4 OQs + 6 assumptions + 1 NOTE FOR PM) is proportionate for a green-light internal tool, and the OQs are genuinely open. The two index roundtrip breaks are mechanical (see below), not scope problems.

### Findings
- **[low]** Assumption without inline tag (§9 vs §2.2) — the index entry "§2.2 — Distribución por sideload APK (confirmado con el usuario)" has no `[ASSUMPTION: …]` tag at §2.2. *Fix:* add the inline tag at §2.2.
- **[low]** Assumption without inline tag (§9 vs §4.1) — the index entry "§4.1 — El permiso de notificaciones es necesario para la visibilidad de la notificación del FGS (no para iniciarlo)" has no inline tag in §4.1. *Fix:* add the inline tag at §4.1.

## Downstream usability — strong

This PRD feeds UX, architecture, and story creation, and it is built for that: glossary terms are used identically across FRs, UJs, and SM definitions ("app objetivo", "overlay de aislamiento", "orquestador de presencia", "toque fantasma", "patrón de salida"); IDs are contiguous and unique (FR-1…FR-14, SM-1…SM-5, SM-C1/C2, UJ-1/2); cross-references resolve (UJ-1 → FR-4, UJ-2 → FR-6, SM "Validates FR-x" chains all resolve); both UJs have a named protagonist (Roberto) carrying context inline. Sections stand alone via glossary terms rather than "see above". No findings beyond the mechanical notes below.

## Shape fit — strong

The shape matches the product: internal tool, single-operator role → capability spec with FRs + consequences, which is exactly what this is. UJ density (2, named protagonist) is appropriate, not over-formalized. SMs are operational rather than user-facing (SM-1, SM-2, SM-4 are test/validation metrics), matching the rubric's guidance for this product type. As a chain-top PRD, the downstream-usability strengths above are the right investment. No findings.

## Mechanical notes

- **Assumption Index roundtrip:** 2 of 6 index entries lack inline `[ASSUMPTION]` tags — §2.2 (sideload distribution) and §4.1 (notifications permission). The other four (FR-3, FR-5, FR-6, FR-14) roundtrip correctly.
- **ID continuity:** clean — FR-1…FR-14 contiguous, SM-1…SM-5 + SM-C1/C2, UJ-1/2; no gaps, no duplicates, all cross-refs resolve.
- **Glossary drift:** none found — terms are used consistently in case and form throughout.
- **Unresolved naming:** "Working title — confirm." (§0) is a dangling decision; it belongs in Open Questions or should be resolved.
- **Required sections:** all present for a chain-top internal tool (Vision, Target User, Glossary, Features, Non-Goals, MVP Scope, Success Metrics, Open Questions, Assumptions Index).
