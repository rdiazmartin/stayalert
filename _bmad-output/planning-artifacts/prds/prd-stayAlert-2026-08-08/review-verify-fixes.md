# Verification: PRD v2 Fixes vs. Adversarial Review Findings

**PRD:** `prd.md` (v2, 2026-08-08) · **Review:** `review-adversarial-general.md`
**Date:** 2026-08-08

| Finding | Status | Justification (PRD section) |
|---|---|---|
| C1 | RESOLVED | New §9 "Validation Gate (Go/No-Go)" runs in Fase 0/1 before building, with real-device procedure (≥2 days), decision rule (GO ≥4 h foreground both days; STOP/pivot to semi-transparent overlay or abandon), and result recorded in memlog; SM-1 split into SM-1a (emulator proxy) and SM-1b (empirical, real Teams/tenant), with OQ-1 tying the undocumented timeout to the gate. |
| C2 | RESOLVED | Phantom-tap injection apparatus removed entirely: FR-7 is now the 4-tap exit pattern (§4.2), FR-9 is touch blocking ("los toques no producen ningún efecto sobre la app objetivo", §4.3), no periodic injection, no injection SM, no SM-C1; FR-9 no longer reads any accessibility tree, so the vacuous/privacy-violating reading is gone. |
| C3 | RESOLVED | New §5 "Ethics, Policy & Liability" states the tool's purpose honestly, employer-policy violation risk with user assumption of risk, no distribution outside owner's devices, transparency via FR-3 aviso de uso responsable, and no data collection; MDM raised in §5 and OQ-4 ("¿el dispositivo objetivo es gestionado por el empleador (MDM/BYOD)?"); §6 "jiggler" non-goal now framed factually (no event injection) rather than as a dodge. |
| H1 | RESOLVED | FR-11 makes the FGS notification "Detener sesión" action a v1 kill switch (same effect as the exit pattern, §4.4); FR-12 guarantees a "Sesión terminada" notification with reason on every termination, eliminating silent exit; FR-7 Out of Scope explicitly notes the notification action complements the pattern. |
| H2 | RESOLVED | FR-13 watchdog terminates with notification on screen-off, overlay absence, overlay-permission revoke, and target-app loss of foreground (UsageStatsManager); FR-14 handles target-app crash (<5 s, notification); FR-15 adds OEM battery-exemption/autostart onboarding (Xiaomi/Samsung); FR-16 handles low battery (15% warn, 5% end); FR-17 handles process kill; FR-12 covers feedback for all. |
| H3 | RESOLVED | SM-1 split into SM-1a (proxy: mock stays resumed ≥8 h with 0 touches, emulator) and SM-1b (empirical: real Teams "Disponible" ≥2 full workdays on Roberto's device/tenant, explicitly noting the undocumented foreground timeout and OQ-1); §9 gate attaches the go/no-go rule, so the proxy is no longer presented as evidence of the goal. |
| M1 | PARTIAL | FLAG_SECURE is now mandatory (FR-8 consequence, §7.1 MVP scope) and the AccessibilityService is gone entirely (no injection mechanism), eliminating the dangerous permission combo; §5 "Sin datos" and §6 non-goals cover data minimization. But there is still no explicit threat model section and no signing/hash/distribution-hygiene guidance for the sideload APK. |
| M2 | RESOLVED | SM-1a now states "con el dispositivo cargando o ≥ 80% de batería al inicio" (§8); FR-16 adds battery-level handling (warn at 15%, terminate at 5%). |
| M3 | RESOLVED | §9 Validation Gate explicitly names the TR's adaptation path as the pivot: "overlay semi-transparente con paso de toques, o abandono del producto"; FR-10 is no longer an opaque-overlay mandate (now "Retención de pantalla"), so nothing forecloses the semi-transparent variant. |
| M4 | RESOLVED | Glossary "Overlay de aislamiento" (§3) and FR-8 consequence (§4.3) now state plainly that the status bar and quick-settings shade remain accessible (system behavior, not a failure); OQ-5 asks about shade/incoming-call behavior during session. |
| L1 | RESOLVED | FR-6 consequence (§4.2): "Si la app objetivo no se confirma en primer plano... la sesión aborta limpiamente con mensaje de error, sin overlay huérfano" — the abort-on-not-resumed requirement the finding asked for; the 1 s delay is retained but flagged as an assumption pending empirical validation (§11). |
| L2 | RESOLVED | FR-4 consequence (§4.1): "La detección de instalación requiere declaración `<queries>` en el manifest (visibilidad de paquetes, API 30+)." |
| L3 | MOOT | Injection was removed in v2 — no injection point exists; FR-9 is touch blocking and FR-7 is the exit pattern, so the coordinate-policy/exit-region interaction question no longer applies. |
| L4 | RESOLVED | Old injection SM-4 is gone; new SM-4 (§8) is a meaningful, falsifiable metric: "Sin wake locks activos fuera de sesión; retención de pantalla liberada al terminar. Validates FR-10." |
| L5 | RESOLVED | Document is consistently Spanish throughout (no mixed-language artifacts); working title remains explicitly marked "Working title — confirm." at status draft (§0), which is the state the finding deemed acceptable before downstream handoff. |

## Summary

- **RESOLVED:** 13 (C1, C2, C3, H1, H2, H3, M2, M3, M4, L1, L2, L4, L5)
- **PARTIAL:** 1 (M1)
- **NOT RESOLVED:** 0
- **MOOT:** 1 (L3)

**Remaining action:** M1 — add an explicit threat-model note and signing/hash/distribution-hygiene guidance for the sideload APK (or explicitly defer it as an accepted risk for single-owner distribution).
