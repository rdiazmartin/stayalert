---
title: Adversarial Review — Architecture Spine stayAlert
type: review
reviewed: ARCHITECTURE-SPINE.md (2026-08-08, status: draft)
method: adversarial construction — two units one level down, each AD-compliant, forced to collide
reviewer: adversarial reviewer (attack mode)
date: 2026-08-08
---

# Adversarial Review: ARCHITECTURE-SPINE.md

## Verdict

**DO NOT promote from `draft` to `build-substrate` in current form.** The spine gets the macro-shape right (single owner of state AD-1, components-as-clients AD-2, overlay-first ordering, AD-4's numbers) but the **component↔controller interface layer is a name-only contract**: `SessionEvent` is a noun with no shape, the *command vocabulary* from controller to components does not exist, the Lanzando→Aislada handshake is unspecified, the "Sesión terminada" notification has two plausible owners, and there is no concurrency/idempotency model. These are exactly the seams where two developers obeying every AD to the letter will produce units that cannot be wired together. ~5 load-bearing decisions are missing; 3 of them are critical.

---

## Attack Method

For each AD, I constructed two minimal units (dev A, dev B) one level below the spine that **each satisfy every AD and the PRD consequences**, then forced them to integrate. Where the two builds contradict at integration time, the spine has a hole at that seam. The most damaging collisions are demonstrated below; all findings cite spine locations.

---

## Demonstrated Collisions (two AD-compliant units that cannot integrate)

### Collision 1 — The central handshake (Lanzando → Aislada)

- **Unit A (OverlayController):** `fun showOverlay(): Boolean` — synchronous, returns after `windowManager.addView()` returns. No confirmation event. `SessionController` transitions Lanzando→Aislada on return, then orders FGS start.
- **Unit B (OverlayController):** `fun showOverlay()` returns Unit; emits `SessionEvent.OverlayShown` when the view reports `onWindowFocusChanged(true)`. `SessionController` transitions Lanzando→Aislada **only** on that event, then orders FGS start.

Both obey AD-2 ("el FGS se inicia después de que el overlay esté visible" — one reads "visible" as "addView returned", the other as "window focused", AD-2 · L50). Both obey AD-1 (SC decides transitions). **Integration result:** if B's controller is paired with A's overlay, the session hangs in `Lanzando` forever and the FGS never starts. The spine defines the *state names* but not the *trigger protocol* for the only state transition that matters (AD-1 · L44).

### Collision 2 — "Sesión terminada" notification has two plausible owners

- **Unit A (Watchdog):** interprets AD-5's "Toda terminación emite notificación 'Sesión terminada' con el motivo correspondiente" (AD-5 · L68) literally — the **watchdog owns and posts** termination notifications, including the motivo it detected. It also creates its own channel (`stayalert_anomalies`).
- **Unit B (SessionController/FGS):** interprets FR-12 ("Toda terminación de sesión produce una notificación con el motivo" — pattern, manual stop, anomalies) as requiring **one termination path in the controller**; FGS posts "Sesión terminada" from `onDestroy` for user exits (pattern/Detener), using channel `stayalert_session`.

Both obey AD-1 (neither mutates session state), AD-2, AD-5's text. **Integration result:** battery-critical termination posts **two** "Sesión terminada: batería baja" notifications (watchdog posts, FGS `onDestroy` posts, or both race `stopForeground` and one is silently dropped). Channel IDs drift because the spine fixes only DataStore keys (`SettingsKeys`, conventions · L83) — not notification channels. FR-12's "100% de terminaciones → una notificación" (PRD consequence) is unachievable deterministically.

### Collision 3 — Watchdog activation gating (false termination on every launch)

- **Unit A (Watchdog):** its scope lives and dies with the session — `SessionController` orders `start()` on entering Aislada, `stop()` on leaving it. Polls only in Aislada.
- **Unit B (Watchdog):** app-scoped coroutine, always polling; emits `SessionEvent.Anomaly` unconditionally; `SessionController` filters events by current state (ignores anomalies in Inactiva/Lanzando).

Both obey AD-5 (same detection list), AD-1, AD-2. **Integration result:** with Unit B's watchdog and any controller that trusts it, the "overlay ausente" check fires **during Lanzando** — the overlay is *supposed* to be absent for the first second — and the session aborts on every launch attempt. The spine does not bind whether the watchdog is gated by state or by orders, nor what the controller must do with events received outside `Aislada`. FR-13's "< 5 s" SLA (PRD) also floats free of any polling interval decision.

### Collision 4 — Startup-failure events don't exist in the diagram

The spine's event arrows are labeled only "patrón salida", "anomalías", "acción Detener" (diagram · L33–35). FR-6 requires abort-with-message if the target app doesn't reach foreground or the overlay fails (PRD · L138) — but there is **no `LaunchFailed`/`OverlayFailed` event** in the vocabulary, and `TargetAppLauncher`'s contract is unspecified (AD-3 · L56: "interfaz con dos implementaciones" — nothing about error channel):

- **Unit A:** `fun launch(target): Result<Unit>`; SC aborts on `Result.failure`.
- **Unit B:** `fun launch(target)` throws `TargetLaunchException`; SC wraps in try/catch.

Both obey AD-3. A third party writing `SessionController` cannot code against an interface whose failure semantics are undefined. The FR-6 abort path is one of the few *required* behaviors of the whole product and it has no fixed seam.

---

## Findings by Severity

### CRITICAL

#### C-1 — No command contract exists: only events are named, orders are invisible
**Locations:** diagram · L28–36 (SC→OL/FGS/WD arrows unlabeled); AD-1 · L44; AD-2 · L50.
The mermaid shows SC→component arrows with no labels while component→SC arrows carry event names — the spine pins the *reporting* direction and says nothing about the *commanding* direction. `showOverlay`/`hideOverlay`/`startFgs`/`stopFgs`/`startWatchdog`/`stopWatchdog` (and their signatures, sync vs async, ack semantics) are all unbound. Combined with Collision 1, the controller cannot be written by anyone not also writing all three components. **Fix:** add a `SessionCommand` sealed contract to the domain layer with signatures and ack/confirmation rule; bind the Lanzando→Aislada trigger to one mechanism (recommended: `OverlayShown` event; forbid sync-return interpretation by making `showOverlay` fire-and-forget + confirm event).

#### C-2 — "Sesión terminada" notification ownership is ambiguous and races the FGS stop
**Locations:** AD-5 · L68 ("Toda terminación emite notificación…" assigned to watchdog rule); capability map · L124 (FR-12 → Watchdog **+** FGS); AD-2 · L50.
AD-5's literal text assigns termination-notification emission to the watchdog, but FR-12 requires the notification on *every* termination — pattern exit (FR-7, owned by OverlayController per map · L121) and Detener action (FGS) never involve the watchdog. So user-initiated exits need a second poster (FGS? SC?), and that poster must post while simultaneously executing the stop order — `stopForeground(REMOVE)` and the "Sesión terminada" post can race and drop the notification. Two channels, two posters, ordering unbound. **Fix:** bind a single owner (recommended: `SessionController` posts termination notifications via an injected `Notifier`; watchdog's job is *detection only* — rephrase AD-5's rule) and fix channel IDs + creation point in the conventions table.

#### C-3 — Watchdog lifecycle/gating unbound → false termination during Lanzando, self-induced anomaly during Deteniendo
**Locations:** AD-5 · L68 (rule lists detections, not when the watchdog is active); AD-1 · L44 (SC decides transitions — but nothing says SC must *filter by state*); diagram · L31.
Two compliant implementations exist (Collision 3); one aborts every launch; the other silently changes SLA. Symmetrically, during Deteniendo the controller *orders* overlay removal — which is exactly the watchdog's "overlay ausente" trigger — so an always-on watchdog generates a second termination event caused by the termination itself. The `Deteniendo` state exists but its semantics (what events are legal in it, who clears it, idempotency of termination) are nowhere defined. **Fix:** bind one of: (a) watchdog started/stopped by orders, active only in Aislada; or (b) mandatory state filtering in SC plus an "events during Deteniendo are ignored" invariant. Also bind the polling interval vs the 5 s SLA.

### HIGH

#### H-1 — `SessionEvent` is a name, not a contract: payloads and the motivo vocabulary are unbounded
**Locations:** AD-1 · L44 (names the type only); conventions · L80–82; diagram · L33–35.
Two devs may define `sealed interface SessionEvent { data class PatternDetected; data class StopRequested; sealed class Anomaly(val reason: AnomalyReason) }` vs `sealed class SessionEvent { object PatternDetected; object StopRequested; data class WatchdogEvent(kind, batteryLevel) }` — both AD-compliant, mutually unbuildable, and they diverge on the **motivo** enum that FR-12's notification text depends on (who owns the mapping motivo→string?). **Fix:** enumerate the event subclasses + payloads (at minimum: `PatternDetected`, `StopRequested`, `ScreenOff`, `OverlayMissing`, `PermissionRevoked`, `TargetLeftForeground`, `TargetCrashed`, `HideOverlayWindows`, `BatteryWarning(15%)`, `BatteryCritical(5%)`, `OverlayShown`, `OverlayFailed(cause)`, `LaunchFailed(cause)`) and the motivo enum in the spine.

#### H-2 — No concurrency model: three event sources race into a mutable state machine
**Locations:** AD-1 · L44 ("el controlador decide las transiciones" — but no statement that transitions are serialized/deduped); diagram · L33–35 (events arrive from touch thread, FGS, watchdog coroutine).
4-tap and "Detener" can legitimately fire within milliseconds of each other (user taps the pattern as they hit the notification action). The watchdog adds a third thread. Without a bound "events are processed on a single dispatcher / termination is idempotent / second termination event is dropped", duplicate notifications (FR-12 violation) and double state-transition side effects (double overlay destroy, double FGS stop) are the default outcome, not the edge case. **Fix:** bind `SessionController` as a single-consumer event loop (e.g., `Channel`/`MutableStateFlow` on one dispatcher) + a "termination is idempotent, later events in Deteniendo/Inactiva are no-ops" invariant.

#### H-3 — Startup-failure path has no vocabulary and `TargetAppLauncher` has no error contract
**Locations:** AD-3 · L56; diagram · L32–35 (no failure events); AD-2 · L50.
FR-6's abort-with-message consequence (PRD · L138) has no event type, no launcher error channel (throws vs `Result` vs Boolean — Collision 4), and no stated behavior when the overlay cannot be added (permission revoked between FR-5 check and `addView`). The overlay-fails case also strands the already-launched target app in foreground with no overlay and no FGS — FR-6 says "aborta limpiamente" but the cleanup of the launched target (nothing brings the user back; is that accepted?) is unstated. **Fix:** bind `Result`-style or exception contract for `TargetAppLauncher`; add `OverlayFailed`/`LaunchFailed` events to H-1's list; state the abort target-state and whether the launched app is left foreground.

#### H-4 — FR-6 "confirm target in foreground" has no mechanism and no test seam
**Locations:** AD-5 · L68 (UsageStatsManager only as optional watchdog sensor); AD-3 · L56 (only launcher is injectable); structural seed · L109–111.
The launch sequence must abort if the target doesn't reach foreground — but the sensor is unspecified. The only candidate (UsageStatsManager) is optional (PRD · L224), so on devices without `PACKAGE_USAGE_STATS` the FR-6 confirmation either doesn't run or uses an undefined mechanism. `FakeLauncher` (AD-3) validates the launch *call*, not the foreground *confirmation*; the E2E mock validates it only cross-process (UI Automator asserts on mock lifecycle), so the abort path is **untestable in JVM/Robolectric** — precisely the layer where FR-6's consequence should live. **Fix:** abstract a `ForegroundMonitor` (or make the confirmation a testable policy in SC); bind what runs when usage stats are absent.

### MEDIUM

#### M-1 — Notification channels: count, IDs, creators, and the Detener PendingIntent routing are all unbound
**Locations:** conventions · L83 (only DataStore keys fixed); AD-5 · L68; structural seed · L106–108.
One channel or two (FGS-persistent vs termination)? Created by whom and when (channel must exist before first post)? The "Detener sesión" action's PendingIntent target (manifest BroadcastReceiver vs FGS `onStartCommand` intent vs activity) determines whether AD-2's "FGS reports the action as an event" is even expressible. None of this is in the spine; it's the FR-11 kill switch's entire plumbing.

#### M-2 — Battery monitoring has two owners and its thresholds live only in the PRD
**Locations:** capability map · L124 (FR-16 → "Watchdog + PresenceForegroundService"); AD-5 · L68 (rule omits battery entirely); AD-6 · L74 (config keys don't include thresholds).
The map assigns FR-16 to two components; AD-5's detection list never mentions battery. The 15%/5% numbers (PRD · L247–248) are absent from the spine, and the 15% *warning* path (notification without termination) is a third mutation channel: does the watchdog post it directly (making it a notifier — clashes with C-2's fix) or emit an event? Unbound. **Fix:** single owner (recommended: watchdog detects, SC decides, one poster), thresholds into the spine or `SettingsKeys`.

#### M-3 — Only the launcher is injectable: no clock/time seam for the load-bearing delays
**Locations:** AD-3 · L56 (launcher only); AD-4 · L62 (500 ms fixed in spine — good); stack · L98 (Robolectric testing).
The 1 s launch delay (FR-6) and the < 5 s watchdog SLA (FR-13) require deterministic time control in unit tests; the TR explicitly recommended an injectable `Scheduler`/clock (research · L200) and the spine dropped it. Robolectric can shadow time, but a bound `Clock` seam is what makes SC's Lanzando sequence and the watchdog's SLA testable without sleeps. **Fix:** add injectable clock to the domain layer.

#### M-4 — Two readers of the same permission: PermissionAuditor vs Watchdog (and no mid-session re-audit of notifications)
**Locations:** conventions · L84 (`PermissionAuditor`); AD-5 · L68 (watchdog polls `Settings.canDrawOverlays()`).
Three places touch overlay permission state (pre-start validation FR-5, onboarding audit FR-1, watchdog FR-13) with no single source; duplication drift (e.g., one reads `canDrawOverlays`, another caches it). Also unbound: `POST_NOTIFICATIONS` revoked mid-session silently hides the FGS notification with no reaction. **Fix:** bind that `PermissionAuditor` is the only reader of permission state and the watchdog consumes its data or an event.

### LOW

#### L-1 — Load-bearing numbers drift between PRD and spine
**Locations:** AD-4 · L62 fixes 500 ms/15%/15% (good); the 1 s delay (PRD · L349 flags it as empirical), 15%/5% battery, and < 5 s SLA exist **only** in the PRD. PRD already admits the 1 s is provisional; with no spine binding, one dev hardcodes, another makes it configurable, a third reads it from DataStore — behavior drifts silently. **Fix:** promote the constants (or their configuration key) to the spine, matching AD-4's style.

#### L-2 — Lanzando lifecycle gaps: power button, screen timeout, re-entry
**Locations:** AD-4 · L62 (`FLAG_KEEP_SCREEN_ON` only exists once the overlay is shown); AD-1 · L44.
Pressing power during the 1 s Lanzando window (screen has no keep-on flag yet) lets the screen sleep mid-sequence; whether the sequence proceeds, aborts, or hangs in Lanzando is unbound, and there is no Lanzando timeout. Double-tapping "Iniciar Jornada" re-entry semantics are also unbound. **Fix:** bind a Lanzando timeout + abort-on-screen-off rule.

#### L-3 — `mock-teams` module hygiene and untested kill-switch path
**Locations:** structural seed · L111; stack · L98.
`mock-teams` (package `com.microsoft.teams`) must never leak into release builds — variant/build-type exclusion is unstated. The FR-11 notification kill-switch and the FR-13/<5 s watchdog SLAs have no assigned test strategy (UI Automator can't drive PendingIntents well); the spine lists them nowhere. **Fix:** note the release-variant exclusion and assign the kill-switch to instrumented tests.

---

## Summary Matrix

| # | Severity | Seam | Spine location | Collision |
|---|---|---|---|---|
| C-1 | Critical | Command vocabulary + Lanzando→Aislada handshake | AD-2 · L50, AD-1 · L44, diagram · L28–36 | Collision 1 |
| C-2 | Critical | Termination-notification owner + channel | AD-5 · L68, map · L124 | Collision 2 |
| C-3 | Critical | Watchdog gating + Deteniendo semantics | AD-5 · L68, AD-1 · L44 | Collision 3 |
| H-1 | High | SessionEvent shape / motivo vocabulary | AD-1 · L44, diagram · L33–35 | Collisions 1, 2 |
| H-2 | High | Concurrency / idempotent termination | AD-1 · L44 | Collision 2 (race) |
| H-3 | High | Launch/overlay failure path, launcher contract | AD-3 · L56, diagram · L32–35 | Collision 4 |
| H-4 | High | Foreground-confirmation sensor + test seam | AD-5 · L68, AD-3 · L56 | E2E hole |
| M-1 | Medium | Notification channels + Detener routing | conventions · L83 | Collision 2 |
| M-2 | Medium | Battery owner + thresholds | map · L124, AD-5 · L68 | — |
| M-3 | Medium | Injectable clock | AD-3 · L56 | — |
| M-4 | Medium | Permission single-reader | conventions · L84 | — |
| L-1 | Low | Constants in PRD only | AD-4 · L62 vs PRD | — |
| L-2 | Low | Lanzando power/re-entry | AD-4 · L62 | — |
| L-3 | Low | mock-teams hygiene, kill-switch tests | seed · L111 | — |

---

## Bottom Line

The spine's direction is sound (AD-1/AD-2/AD-4 are the right rules and AD-4's numbers are exemplary), but **as a build-substrate contract it is incomplete at the interface layer**: three critical seams (commands+handshake, notification ownership, watchdog gating) and four high seams (event shapes, concurrency, failure vocabulary, test seams) will each produce silently-incompatible units from AD-compliant implementations. The four demonstrated collisions are not hypotheticals — every one is triggered by *normal* product flows (launch, exit, battery-low). Recommended: close C-1..C-3 and H-1..H-4 in a spine revision (fixes proposed inline), then re-review before promoting from `draft`.
