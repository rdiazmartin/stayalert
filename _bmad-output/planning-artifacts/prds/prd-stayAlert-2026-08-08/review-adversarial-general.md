# Adversarial Review: PRD stayAlert (2026-08-08)

**Reviewer stance:** cynical, adversarial. Assumes the document is trying to sound reasonable while skipping the parts that would hurt.
**Inputs reviewed:** `prd.md` (302 lines), cross-checked against `technical-android-presence-keep-awake-research-2026-08-08.md` (which the PRD claims as its technical basis).
**Line/§ citations below refer to `prd.md` unless marked TR (research doc).**

---

## Verdict

**REVISE — do not hand off to UX/Architecture in current state.**

The PRD is well-structured and unusually honest in places (it names the mock strategy, the undocumented Teams timeout, the OEM risk). But it commits three structural sins:

1. It states the product's *thesis* as a primary success metric (SM-1) without a validation gate — if the thesis is false, the whole product dies after the investment is already made.
2. It mechanically inherits requirements (FR-7, FR-9, SM-4, the configurable interval, the "refuerzo" language) from the research without reconciling them with the research's *own central finding* that the phantom tap is functionally inert. The document simultaneously claims the tap "refuerza la actividad percibida" and that the tap never reaches Teams. Both cannot be true.
3. It never confronts what the product actually is: a tool to defeat an employer's presence-monitoring system. The "internal use, one button" framing is a dodge, not an answer.

Below, findings ranked critical → low. Each cites PRD locations and, where relevant, the contradiction with the TR.

---

## CRITICAL

### C1. SM-1 is the product thesis disguised as a success metric, with no validation gate

**Location:** §7 SM-1 (L276), §8 OQ1 (L290), §6.2 L271; TR risk table ("Microsoft endurece presencia móvil" — Prob. Media, Impacto Alto, TR L288) and TR conclusion (TR L344: "La tendencia de Microsoft hacia presencia basada en interacción es el riesgo evolutivo principal").

**The problem.** SM-1 says the primary measure of success is "la app objetivo permanece 'Disponible' durante sesiones de ≥ 8 h" — and then admits, in its own parenthetical, that the mechanism's behavior is undocumented and needs "medición empírica en validación." That is not a success metric; it is an open research question that happens to be the product's entire reason to exist. The PRD:

- Does not sequence any validation *before* the implementation investment. The TR (TR L350) explicitly recommends early empirical validation ("antes de invertir en UI"), but the PRD's MVP scope (§6.1) has no such gate.
- Does not carry forward the TR's principal risk (Microsoft moving to interaction-based presence — rated Media probability, **Alto** impact) into any requirement, assumption, or kill-criterion. The only mitigations the TR proposes (semi-transparent overlay alpha ≤ 0.8 + NOT_TOUCHABLE to let taps reach Teams, TR L288/L338) appear **nowhere** in the PRD. The PRD's FR-10 even mandates an opaque overlay, structurally foreclosing that mitigation.
- Has no go/no-go decision point: "if empirical validation shows Teams flips to Away after N minutes of foreground without interaction, we do X" is absent. If the thesis fails, the PRD contains no exit.

**Recommendation.** Add a §"Validation Gate" (before MVP completion): measure real-Teams foreground timeout on a real device with the target tenant; define the decision rule (e.g., "if presence flips Away within 4 h of uninterrupted foreground on ≥ 2 consecutive days, STOP / pivot to interaction-mode"); demote SM-1 from metric to validation target; import the interaction-based-presence risk and its mitigation into a FR or at minimum an explicit assumption with trigger.

---

### C2. The phantom tap is theater — the PRD contradicts its own mechanism

**Location:** §1 L19 ("toques fantasma inofensivos que... refuerza la actividad percibida"), §3 L66 (Glossary "Toque fantasma" — "Refuerza la actividad percibida sin disparar lógica"), FR-7 (L153-160), FR-9 (L170-176), SM-4 (L281), SM-C1 (L285); TR L332 ("El tap es refuerzo, no mecanismo"), TR L333.

**The problem.** The PRD's own research (and its own FR-7 consequence, L160) establishes: the injected tap lands on the **opaque overlay** and *never* reaches Teams. An event that never reaches Teams cannot "reforzar la actividad percibida" of Teams — Teams never perceives it. The **only** mechanism keeping presence green is foreground (TR L332). The PRD therefore ships a large apparatus of requirements around an inert event:

- FR-7 (periodic injection, 240 s, 50–100 ms taps), the configurable interval NFR (L179), SM-4 (99 % injection success), and SM-C1 ("no optimizar por debajo de 240 s") all regulate a mechanism that does nothing for the product's goal.
- SM-4 is additionally a tautology: "fallos con reintento exitoso no cuentan como fallo" (L281) — a metric defined to always pass.
- FR-9 (L170-176) is internally incoherent. It checks whether the injection point "no contenga un elemento interactivo (nodo clickable)" before injecting. Against which window's accessibility tree? (a) The overlay's own tree — which is a bare black View with no clickable nodes, making the check vacuous; or (b) the target app's tree — which means the accessibility service is reading Teams' UI, contradicting §5 L244 ("No interactúa con la UI de la app objetivo") and creating an actual data-touch surface. The PRD does not say, and either reading is damning.
- The whole FR-7/FR-9 apparatus makes sense **only** in the TR's future semi-transparent-overlay mode (where taps would pass through to Teams). The PRD copied the future mode's requirements onto v1's opaque overlay without reconciling.

**Recommendation.** Decide and state plainly: in v1 the tap is *vestigial* (kept only to exercise `dispatchGesture` for the future semi-transparent mode) — then strip or downgrade FR-7/FR-9/SM-4/SM-C1 to "technical enablers, no success value," and stop implying the tap contributes to presence. OR keep the tap meaningful by adopting the TR's semi-transparent overlay now. Either way, FR-9 must specify which window's accessibility tree it reads and what happens with that data.

---

### C3. Ethics and policy: the PRD is a presence-fraud tool that never says so

**Location:** §1 L17-21, §2.1 L27-32 (JTBD: "mantener 'Disponible'... durante jornadas... sin interacción"), §2.3 UJ-1 (L42-47), §5 L243 ("No es un 'jiggler'"), §2.2 L38.

**The problem.** The product's only function is to make an employer-facing presence system report "Available" when the user is not interacting with it. Three things the PRD refuses to confront:

1. **Decomposition of the use case.** "Mantenerte Disponible mientras trabajas en otra cosa" (L17) — if the user is working at a desk on a computer, Teams *desktop* (or the corporate laptop) already shows them Available; the phone app is only load-bearing when the user is **not** at the machine — i.e., away, or not working. The one scenario where the tool is needed is the one scenario where it is deceptive. The PRD never distinguishes "working elsewhere (legit)" from "not working (presence fraud)", and the SM-1 metric — "≥ 8 h continuas **sin interacción del usuario**" — is precisely the fraud signature.
2. **The "jiggler" dodge.** §5 L243 says "No es un 'jiggler' de escritorio: no simula movimiento de ratón ni teclado." That is a category dodge, not a distinction. On mobile, foreground-keeping plus injected activity to defeat an inactivity monitor is functionally the mobile equivalent of a jiggler. The Non-Goal is written to feel clean rather than to answer the real question.
3. **Consequence and exposure.** Nowhere does the PRD acknowledge: (a) use may violate the user's employment terms/corporate policy (most employers treat presence fraud as a disciplinary/termination event); (b) the device may be employer-managed — MDM/BYOD compliance can flag sideloaded apps or accessibility services, and the PRD never asks whether the target device is managed (a *feasibility* gap, not just an ethics one); (c) "internal use" (§1 L21) is a euphemism — if the APK is shared with colleagues, the exposure multiplies, and any of them discovering a silent failure means the app shows an *uncovered Teams screen* on a work device; (d) Microsoft/tenant admins can audit presence and device activity.

**Recommendation.** Add an explicit "Ethics, Policy & Liability" section stating: the tool's purpose is to keep an employer-presence signal green without user interaction; use may violate employer policy and the user assumes that risk; no distribution outside the owner's own devices; a plain-language warning shown in-app at first launch ("Using this app may misrepresent your availability to your employer"). Whether the PM keeps building is his call — but the PRD must not pretend the question doesn't exist. Also add an Open Question: "Is the target device employer-managed (MDM)?"

---

## HIGH

### H1. Single point of failure on exit — and every failure mode exits silently

**Location:** FR-6 (L135-145), §4.2 Out of Scope L145 (notification exit deferred to v2), FR-14 (L230-239), UJ-2 (L50-56); §8 OQ4 (L293).

**The problem.** The 4-tap overlay pattern is the *only* exit mechanism in v1 (L145). If the overlay fails to render or the process wedges, the user's only recovery is force-stop — invisible to a non-technical user, and the emotional JTBD ("confianza en que... salir del modo es siempre posible", L31) is violated by design. A notification-based kill switch is the cheapest robustness feature in this app and it is deferred. Separately, the pattern itself has zero confirmation: a 4-tap sequence within 500 ms windows in the top-right 15 %×15 % region — a phone in a pocket, edge of a table, or bag can produce this (the top-right corner is exactly where a thumb/palm presses against a folded phone). An accidental exit is low-cost, but the *silence* of it is not: no "sesión terminada" feedback, no state indicator, so the user can believe the session is running for hours after it died.

**Recommendation.** Make the notification control a v1 requirement (stop-action on the FGS notification). Add a post-exit user-visible event (snackbar/notification "Sesión terminada"). Consider requiring a confirm (e.g., 4-taps + hold) if false-positive exit matters more than exit speed — at minimum document the accepted false-positive rate.

---

### H2. Mid-session failure modes are unhandled — the session dies silently and the user cannot know

**Location:** FR-13 (L221-228), FR-14 (L230-239), FR-12 (L204-210), §8 OQ4 (L293), §2.1 L31.

**The problem.** FR-14 only guarantees *state consistency* after a process kill ("no deja estado inconsistente"). It does nothing about the failure modes that will actually happen in production:

- **OEM battery manager kills the FGS** (Xiaomi/Samsung — the TR rates this Media/Medio and the PRD relegates it to an Open Question): overlay disappears, Teams becomes *visible on screen*, presence flips to Away, and the user gets nothing. The app's most sensitive failure — exposing the target app's content on a work device — is also its most likely one.
- **Overlay permission revoked mid-session** (user or system): the session state remains "Aislada" while no overlay exists; nothing detects it.
- **Accessibility service disabled mid-session**: the orchestrator keeps "injecting" into the void (or failing), session stays alive on paper. No FR monitors service liveness.
- **Target app crashes**: session continues, overlay sits on top of the launcher, presence is dead. No FR handles "target app left foreground."
- **Power button pressed during session**: screen off ends `FLAG_KEEP_SCREEN_ON`'s relevance and the session's premise; the orchestrator keeps running, presence flips. The PRD has no screen-off detection and UJ-1 (L47: "la sesión se mantiene indefinidamente") is simply false under this event.

**Recommendation.** Add FRs for: (a) liveness watchdog that detects overlay absence/service disable/screen-off and terminates the session with a user-visible notification; (b) target-app-crash handling (end session or relaunch); (c) OEM battery-exemption onboarding as a v1 config step (currently only an Open Question, L293 — this is the difference between "works" and "works for a day" on the user's actual device).

---

### H3. The mock cannot measure what the PRD claims it measures — SM-1 is unverifiable in the emulator

**Location:** SM-1 (L276: "Medible en emulador con mock y en dispositivo real con Teams"), SM-2 (L277), §3 L71 (Mock definition), §6.1 L262, TR L265.

**The problem.** SM-1 claims measurability "en emulador con mock." But the mock is defined (L71) as an app that "registra los toques recibidos" — it has **no presence logic at all**. There is no "Disponible" state in the mock to observe. The emulator can validate exactly two things: the mock stays in foreground (resumed), and the mock receives 0 touches (SM-2). Both are *necessary conditions* for the product; neither is the *goal*. The actual goal — "Teams shows Available for 8 h" — depends on Teams' proprietary presence logic (undocumented timeout, tenant admin policy, version, server-side changes) that the mock structurally cannot model. So:

- SM-1 as stated is **not** measurable in the emulator. The PRD overclaims its own testability.
- The only place SM-1 can be measured is Roberto's device, with Roberto's Teams version, on Roberto's tenant — a sample size of 1, subject to tenant admin policy (admins can alter presence timeouts or disable presence entirely) and to a Microsoft server-side change that breaks the mechanism at any time (TR L288).
- The gap between "testable in emulator" and "what matters in production" is the *entire risk surface* of this product, and the PRD papers over it with the word "mock."

**Recommendation.** Split SM-1 into: (a) a testable proxy — "target app stays resumed ≥ 8 h with 0 touches received" (mock, emulator), and (b) an explicit empirical validation step — "real Teams shows Available across ≥ 2 full workdays" (device, real Teams, documented tenant config), with the go/no-go rule from C1 attached. Do not present (a) as evidence of (b).

---

## MEDIUM

### M1. No threat model for the most dangerous permission combo on Android

**Location:** §1 L21, §5 L248-249, FR-1/FR-2 (L81-95); TR L220-222, TR L171.

**The problem.** SYSTEM_ALERT_WINDOW + AccessibilityService is the exact combination used by banking trojans and spyware; the app additionally declares a `specialUse` FGS. The PRD has no security section at all. For a single-user personal APK the direct risk is modest, but: (a) "internal use" with APK sharing creates a trojanization surface — a re-signed copy with these permissions is a perfect malware vehicle, and the PRD has no guidance on signing, hash verification, or distribution hygiene; (b) the accessibility service *does* hold the capability to read other apps' UI (and FR-9's ambiguity, C2, may actually exercise it); (c) the research's own security notes (TR L171, L221) never surface as requirements — e.g., the TR mentions `FLAG_SECURE` as optional; for an app whose function is visual concealment that flag should be non-optional, and nothing in the PRD mentions it. Document the threat model and the v1 mitigations (no network access — enforce via manifest, no storage access, no logging of other apps' trees, signature verification step for distribution).

---

### M2. SM-1 ignores the physics of an 8-hour screen-on session

**Location:** SM-1 (L276), FR-12 (L204-210), TR L280.

**The problem.** "≥ 8 h continuas" with the screen on (even OLED-black) drains a phone battery to empty in the same span. A session that dies at hour 6 because the battery hit 1 % is a *failed* SM-1 measurement caused by physics, not by the product under test. The metric definition silently assumes the phone is plugged in or near-full. State it: "session must survive ≥ 8 h **with the device charging or ≥ 80 % battery at start**," and add battery-level handling (warn/end at low battery) as a FR.

---

### M3. The PRD forecloses the TR's only mitigation for the existential risk

**Location:** FR-10 (L187-194: opaque overlay, mandatory), §5 L244; TR L288/L338 (semi-transparent overlay alpha ≤ 0.8 + NOT_TOUCHABLE as the response to interaction-based presence).

**The problem.** The TR's sole forward path if Microsoft tightens presence (the risk it rates Alto) is a semi-transparent, touch-passing overlay so injected taps reach Teams. The PRD's FR-10 mandates an opaque overlay as a v1 invariant. Nothing in the PRD marks FR-10 as contingent or sketches the transition. The product is architecturally locked into the mechanism that is most likely to be the first thing Microsoft kills.

**Recommendation.** At minimum, add an assumption: "FR-10 opaque overlay is v1-only; the semi-transparent variant (TR) is the planned adaptation if interaction-based presence lands," and ensure the FR-9/accessibility-tree machinery (C2) is the groundwork for that mode rather than dead code.

---

### M4. "Pantalla completa" overstates what the overlay covers

**Location:** FR-5 (L128), FR-10 (L192), UJ-1 (L46: "a pantalla completa"); TR L291 (risk table: status bar/quick settings NOT covered — probability **Alta**, accepted).

**The problem.** The PRD says "pantalla completa" / "cubre la pantalla completa" while its own research states the status bar and quick-settings shade remain accessible (TR L291). The PRD's testable consequences (L192) hedge ("incluida la zona de la app objetivo") — a sign the contradiction is known and papered over. Beyond honesty: quick settings accessible mid-session means the user can accidentally toggle Wi-Fi/brightness/Do Not Disturb, and a status bar is visible on the "black" screen — minor, but the doc should say what it delivers. Also add to FR-12/FR-6: what happens when the user pulls the shade or an incoming call renders over the overlay.

---

## LOW

### L1. Launch semantics may defeat FR-5's "resumed" claim

**Location:** FR-5 (L127), UJ-1 (L45), TR L150.

If Teams is already running with a chat activity on top of its task, an explicit intent to `MainActivity` may not bring the *task* to the foreground the way the FR assumes (existing-task behavior with `FLAG_ACTIVITY_NEW_TASK` varies). Also, the 1-second window between launch and overlay (FR-5b) is an unguarded moment where user touches reach Teams. Verify in Fase-4 validation; add a FR that the session aborts cleanly if the target app is not confirmed resumed.

### L2. FR-3 installed-check needs `<queries>`

**Location:** FR-3 (L103), TR L150. Detecting whether `com.microsoft.teams` is installed under package-visibility rules (API 30+) requires a `<queries>` declaration in the manifest (or `resolveActivity` against a declared intent). The PRD's consequence says "el sistema valida que el paquete esté instalado" without the manifest implication. Trivial, but it will bite at implementation.

### L3. Injection point unspecified

**Location:** FR-7 (L158), FR-9 (L174). The PRD never says *where* on the overlay the tap lands (coordinate policy), nor what "zona inerte" means for an overlay with no UI. Decide: fixed point (e.g., 50 %/85 %), which must also be outside the FR-6 exit region, or the exit region and the tap region could interact in a way nobody has specified.

### L4. SM-4 is a metric designed to pass

**Location:** SM-4 (L281). "Fallos con reintento exitoso no cuentan como fallo" — combined with FR-8's "la sesión no se interrumpe por un fallo puntual," the metric cannot fail by construction. Either define it as "first-attempt success ≥ 99 %" or drop it.

### L5. Language inconsistency

**Location:** entire document. The PRD is in Spanish; if downstream artifacts (UX, stories) continue in Spanish, fine — but confirm deliberately. A working title marked "confirm" (L9) at "status: draft" is fine; just don't ship it downstream unconfirmed.

---

## Summary table

| # | Severity | Finding |
|---|---|---|
| C1 | Critical | SM-1 is the unvalidated thesis, no go/no-go gate, TR's principal risk + mitigation dropped |
| C2 | Critical | Phantom tap is inert per own research; FR-7/FR-9/SM-4 theater; FR-9 vacuous or privacy-contradictory |
| C3 | Critical | Presence-fraud tool without ethics/policy acknowledgment; "jiggler" dodge; MDM unasked |
| H1 | High | Sole exit path = single point of failure; silent accidental exit; no fallback kill switch in v1 |
| H2 | High | Mid-session failures (OEM kill, permission revoke, service disable, target crash, screen-off) unhandled and silent |
| H3 | High | Mock cannot measure "Disponible"; SM-1 unverifiable in emulator; sample-size-1 in production |
| M1 | Medium | No threat model for SYSTEM_ALERT_WINDOW + AccessibilityService combo; no signing/hash hygiene; FLAG_SECURE absent |
| M2 | Medium | 8 h screen-on session ignores battery physics; SM-1 lacks charging constraint |
| M3 | Medium | FR-10 opaque overlay forecloses the TR's only adaptation path |
| M4 | Medium | "Pantalla completa" contradicts TR (status bar/shade uncovered, probability Alta) |
| L1 | Low | Launch semantics may break FR-5 "resumed"; 1 s unguarded window |
| L2 | Low | FR-3 misses `<queries>` manifest implication |
| L3 | Low | Injection point and exit-region interaction unspecified |
| L4 | Low | SM-4 cannot fail by construction |
| L5 | Low | Language/working-title confirmation |

**Bottom line:** the mechanisms are real and the research is solid, but the PRD as written would take a well-built app and still leave the user *deceived, exposed, and possibly fired* — while measuring the wrong things. Resolve C1-C3 and H1-H3 before UX/Architecture.
