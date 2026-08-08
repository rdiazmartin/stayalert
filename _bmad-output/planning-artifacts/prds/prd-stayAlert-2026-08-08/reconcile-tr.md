# Reconcile: Technical Research → PRD (v2)

**Input:** `technical-android-presence-keep-awake-research-2026-08-08.md`
**Target:** `prd-stayAlert-2026-08-08/prd.md` (v2 — phantom-tap injection removed)
**Date:** 2026-08-08

## Summary

The PRD faithfully carries the v2 core: foreground as the presence mechanism (Vision §1, FR-6, Validation Gate §9, SM-1b), overlay contract (FLAG_KEEP_SCREEN_ON via Glossary "Retención de pantalla" + FR-10; FLAG_SECURE in FR-8; status bar/quick-settings accessible in FR-8; opaque overlay blocks touches in FR-8/FR-9), FGS `specialUse` (FR-11), Doze rationale for dropping PARTIAL_WAKE_LOCK (FR-6 Out of Scope), battery/OEM risk (FR-15, FR-16, OQ-3), and the semi-transparent-overlay pivot in the go/no-go STOP path (§9). Injection-related facts (dispatchGesture, 240s interval, tap 50–100ms, gesture limits) are correctly absent since the mechanism was removed.

## Gaps (4)

1. **Window type contract unnamed (TYPE_APPLICATION_OVERLAY / SYSTEM_ALERT_WINDOW) and minSdk 26 floor absent.** The PRD names `ACTION_MANAGE_OVERLAY_PERMISSION` and `Settings.canDrawOverlays()` (FR-1/FR-2) but never the window type that constrains the whole feature — and never the API-26 device floor it implies (research §Tech Stack: minSdk 26 required by TYPE_APPLICATION_OVERLAY). Downstream Architecture will re-derive this; the PRD should state the compatibility floor since it bounds Non-Users/compatibility.

2. **Android 15 rule "overlay visible before FGS start from background" not referenced.** Research §Integration Patterns (FGS specialUse): starting an FGS from background with SYSTEM_ALERT_WINDOW requires the overlay visible. The PRD's FR-6 sequencing (launch → 1s → overlay → FGS) is *consistent* with the rule but never cites it as the justification — and it matters for the watchdog: if the overlay disappears (FR-13), the FGS cannot simply be restarted from background.

3. **HIDE_OVERLAY_WINDOWS edge case unhandled.** Research §Integration Security: a target app declaring `HIDE_OVERLAY_WINDOWS` (API 31+) prevents the overlay from drawing over it. The PRD only covers this implicitly via FR-13 "overlay ausente → terminar sesión"; the overlay-fails-to-draw scenario should be explicit in FR-8 or the watchdog, else the user gets a session that "starts" but never isolates.

4. **Test-strategy mechanism only partially carried.** The research's core E2E pattern (UI Automator cross-app assertions + mock logging taps/lifecycle state + launcher abstraction behind an interface) survives only as "aserciones con el mock de Teams" (SM-2) and the glossary. The mock's second assertion axis — "app objetivo permanece resumed" (SM-1a) — is present, but the *mechanism* (UI Automator, mock log/broadcast contract) is dropped, risking the test story re-deriving it without the research's verified constraints (e.g., mock must declare exact components launched, `INSTALL_FAILED_UPDATE_INCOMPATIBLE`).

## Contradictions

- **None material.** All carried facts match the research; the removed injection is consistently purged (Non-Goals §6 "no inyecta ningún evento", FR-6, glossary) with no orphaned requirements.
- **Near-item (note, not a contradiction):** FR-13's watchdog mechanism `UsageStatsManager` ("salida de primer plano de la app objetivo") is **new technical surface not covered by the research** (research used `<queries>` for installed-check, FR-4 ✓). The PRD correctly labels it "si el permiso está concedido", but its availability/behavior (permission grant flow, Android 15 semantics) is unverified — flag for Architecture.
- **Consistent by design:** the 1 s delay (FR-6, Assumptions Index) is retained though its research rationale (let the injected tap land on the target) is gone; the PRD honestly re-labels it as pending empirical validation. The 240 s interval is correctly gone with the injection.

## Qualitative ideas the FR structure silently drops

- **OLED savings are modest, not a clean win** (research §Key Findings: "≈40% de LCD en imágenes negras… modesto vs. apagar la pantalla y con overhead de panel"). The PRD's glossary/FR-8 present pixel-off as an unqualified benefit and no metric measures it — consider a counter-metric (do not over-engineer energy instrumentation) or rephrase as "reduces panel energy, exact magnitude device-dependent".
- **The go/no-go gate's mock phase is gone**: research's "validación empírica temprana en emulador" (foreground timeout of the mock) is superseded by the real-device gate (§9). Worth one line: the mock validates *mechanism* (foreground + no-touches) only; it cannot validate Teams' actual Away timeout — §9 already implies this via OQ-1, but the gate procedure lists no emulator pre-step, so the first gate iteration depends solely on real-device availability.
- **The 1 s delay is configurable per research** ("el 1 s es configurable"); the PRD hard-codes it in FR-6 with an assumption — DataStore already exists (FR-3/FR-4); make the delay a stored preference to match the research's injectable-scheduler pattern.
- **`HIDE_OVERLAY_WINDOWS` also explains a possible false "watchdog termination"**: if a future target app blocks overlays, FR-13 will fire repeatedly — the FR structure has no place to distinguish "overlay revoked" from "overlay blocked by target app" in the user-facing reason string (FR-12 motive list); worth a design note.

## Verdict

Carry-forward is faithful; no contradictions. Close gaps 1–2 with one-line technical anchors in FR-8/FR-6 (window type, minSdk, Android 15 rule), gap 3 with an explicit edge case in FR-8 or FR-13, and gap 4 by referencing the mock/test contract in §7.1/SM-2 or deferring to Architecture with an explicit pointer to the research sections.
