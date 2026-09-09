# UltiSideBar — UAT Checklist

This document is the executable companion to `FEATURES.md`: one row per feature stating the steps
to exercise it and the observable truth that proves it works. It is an internal reference for
real-machine verification, not user-facing documentation.

> Batches are dispatched at 60 rows or fewer, and a batch never spans two repositories. There are
> exactly two legitimate exits to `human-uat-pending`: a row needing the pixel layer while the
> real-client harness is not ready, and a row needing personal credentials. Every other row must
> reach `pass`, `fail`, or `blocked`.

## Conventions

- **Columns:** `ID`, `Preconditions`, `Steps`, `Expected`, `Layer`, `Covers`.
- **ID:** cites its `FEATURES.md` ID verbatim. A negative case suffixes the checklist ID only, as
  `.neg-<slug>` — a negative case still tests the same feature, so the base ID is unchanged.
- **Layer**, copied verbatim from Laojun's own `ultitools-real-client-uat` skill so no translation
  step exists at dispatch time: `protocol`, `java-client`, `os-input`, `pixel`, `server`, `human`.
  **This module's own division of labor across those values (D-19):** a row asserting that the
  sidebar scoreboard shows SPECIFIC lines with SPECIFIC resolved values (placeholder
  substitution, translated colors, the exact title) is `pixel` — a scoreboard's correctness is
  what a player actually sees, and no protocol-layer assertion substitutes for looking at it,
  exactly as `Modules/UltiMenu/UAT-CHECKLIST.md` documents for its own rendered GUI. A row
  asserting only that the sidebar appeared/disappeared/produced a chat message (without asserting
  its exact rendered content) is `server` — the state change and the chat line are both
  server-observable without a screenshot. No row in this document is downgraded from `pixel` to
  `protocol` to make it easier to run — an unready pixel harness is a legitimate
  `human-uat-pending` exit; a `protocol` assertion standing in for a rendering claim is not.
- **Human-authenticated-session rows (D-27b):** none exist in this module — it has no capability
  gated behind the maintainer's own UltiCloud panel session or an SMTP-gated recovery flow. Stated
  here for template consistency with the framework's own checklist.
- **Covers** back-references a Phase 9 GUI-excluded class name; left blank when no such class
  applies. UltiSideBar is NOT one of the nine modules in Phase 9's GUI-exclusion register
  (confirmed by reading `.planning/phases/09-module-ecosystem-readiness-and-test-coverage/
  gui-exclusions/` — no `UltiSideBar.md` file exists there, and no file in that directory names
  UltiSideBar), so every row below leaves `Covers` blank.
- A row whose Preconditions name a prior row must appear after that row in file order — asserted
  mechanically: for every row, every checklist ID cited in its Preconditions cell must have a
  strictly smaller line number in this file than the row citing it (sweep class 8, D-27a).
- **Config-per-file rule (D-06):** one checklist row per `@ConfigEntity`-annotated class, never one
  row per key. This is the one deliberate exception to the "ID cites its `FEATURES.md` ID
  verbatim" rule above (same exception `Modules/UltiChat/UAT-CHECKLIST.md` and
  `Modules/UltiMenu/UAT-CHECKLIST.md` document for their own config-per-file rows): a
  config-per-file row (ID suffixed `-yml`) aggregates every per-key `ultisidebar.config.sidebar.*`
  row into one exercise of the whole file at once — there is no single per-key ID to cite when the
  row's job is the file, not one key. One such row exists here: `ultisidebar.config.sidebar-yml`,
  covering all 7 `ultisidebar.config.sidebar.*` `FEATURES.md` rows (the 6 `@ConfigEntry` keys plus
  the legacy-line-migration behaviour) in a single pass.
- This module's shipped defaults are `language: "zh"` (core `config.yml`, not this module's own)
  and complete `lang/zh.yml`/`lang/en.yml` translations for all 12 of this module's own `i18n(...)`
  keys (0 missing either direction) — unlike `Modules/UltiMenu`, no row below needs to special-case
  an untranslated literal. Every row whose Expected quotes a literal chat/console line therefore
  carries the precondition `language: en` set in `plugins/UltiTools/config.yml`, so the observed
  line matches this document's English-only text exactly.

## Sidebar Commands

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultisidebar.sidebar.toggle | `language: en`; sender holds `ultisidebar.toggle`; sender's sidebar currently OFF (no persisted preference, `default-enabled: false` — or an explicit prior `/sidebar off`); `sidebar.yml` at its shipped defaults; PlaceholderAPI installed | Run `/sidebar toggle` | Chat shows `Sidebar has been enabled!` (green, an `i18n(...)` key that DOES honor `language: en`); a scoreboard appears in the sidebar slot titled with the LITERAL, UNTRANSLATED Chinese default at `SideBarConfig.java:37` (translated in `FEATURES.md` as `"My Server"` for this document's own English-only prose) — `language: en` governs only this module's twelve `i18n(...)` chat/console keys, NOT `title`/`lines`, which are plain config-file string literals with no i18n involvement at all; the scoreboard shows the 12 configured lines (with the 3 blank separators) at their shipped Chinese-literal content (`SideBarConfig.java:46-59`, translated in `FEATURES.md`), each PlaceholderAPI/`{player}`-substituted but NOT translated to English | pixel | |
| ultisidebar.sidebar.toggle.neg-console | `language: en`; run from console (not a player) | Run `/sidebar toggle` | The command is rejected before `SideBarCommand#toggle` runs at all — the framework's own sender-type validator refusal (this mapping carries `@CmdTarget(PLAYER)`), not a hand-written message; nothing else happens | server | |
| ultisidebar.sidebar.on | `language: en`; sender holds `ultisidebar.toggle`; sender's sidebar currently OFF | Run `/sidebar on` | Chat shows `Sidebar has been enabled!` (green); the sidebar appears (see `ultisidebar.sidebar.toggle` for the full content assertion — this row only re-confirms the state transition, not the exact content again) | server | |
| ultisidebar.sidebar.on.neg-repeat | `language: en`; sender's sidebar already ON (run `ultisidebar.sidebar.on` first) | Run `/sidebar on` again | Chat shows the IDENTICAL `Sidebar has been enabled!` message a second time — `SideBarCommand#on` has no "already enabled" branch (unlike `Modules/UltiEssentials`'s equivalent `/scoreboard on`), so a repeat call is NOT a no-op with a distinct message; the scoreboard object is silently rebuilt | server | |
| ultisidebar.sidebar.on.neg-console | `language: en`; run from console | Run `/sidebar on` | Rejected before `SideBarCommand#on` runs — the framework's own sender-type validator refusal (`@CmdTarget(PLAYER)` on this mapping) | server | |
| ultisidebar.sidebar.off | `language: en`; sender's sidebar currently ON | Run `/sidebar off` | Chat shows `Sidebar has been disabled!` (yellow); the scoreboard disappears — the player is reset to the server's main scoreboard (`Bukkit.getScoreboardManager().getMainScoreboard()`) | server | |
| ultisidebar.sidebar.off.neg-console | `language: en`; run from console | Run `/sidebar off` | Rejected before `SideBarCommand#off` runs — the framework's own sender-type validator refusal (`@CmdTarget(PLAYER)` on this mapping) | server | |
| ultisidebar.sidebar.reload | `language: en`; sender holds both `ultisidebar.toggle` and `ultisidebar.admin`; edit `sidebar.yml`'s `title` to a distinguishable value on disk first (do not reload yet) | Run `/sidebar reload`, then `/sidebar toggle` twice in a row (regardless of the sender's sidebar state going in, one of the two toggles is an OFF-to-ON transition, which is what triggers a fresh render) | Chat shows `UltiSideBar configuration has been reloaded!` (green); the subsequent re-render shows the EDITED title, not the previous one — `SideBarConfig` was re-read from disk and `SideBarService#reload` rebuilt every online sidebar | server | |
| ultisidebar.sidebar.reload.neg-permission | `language: en`; sender holds `ultisidebar.toggle` (so the command is visible/registered for them at all) but NOT `ultisidebar.admin` | Run `/sidebar reload` | Chat shows the framework's own permission-denied message naming `ultisidebar.admin` as the missing node (`§7Executing this command requires §fultisidebar.admin §7permission`, per the framework's own `lang/en.json`); `plugin.reloadSelf()` is never called | server | |
| ultisidebar.sidebar.reload.neg-language-not-refreshed | `language: zh` at plugin startup (so the module's own `language` field resolves to the Chinese catalogue); sender holds `ultisidebar.toggle` and `ultisidebar.admin`; WITHOUT restarting the server, change `plugins/UltiTools/config.yml`'s `language` to `en` | Run `/sidebar reload`, then `/sidebar help` | The reload's own success chat line, and every line `/sidebar help` prints afterward, are STILL in Chinese, not English — `UltiSideBar#reloadSelf()` overrides the framework's `reloadSelf()` without calling `super.reloadSelf()`, so `createLanguageFromPath` is never re-run and this module's own `language` field is never refreshed to the new setting (`UltiKits/UltiSideBar#16`, filed, not fixed here). Config VALUES (e.g. a changed `title`) DO still take effect via this same reload — only the language catalogue is stuck | server | |
| ultisidebar.sidebar.help | `language: en`; sender holds `ultisidebar.toggle` but NOT `ultisidebar.admin` | Run bare `/sidebar`, then separately `/sidebar help` | Both produce IDENTICAL output: `=== UltiSideBar Help ===` (gold), then three lines for `/sidebar toggle`, `/sidebar on`, `/sidebar off` (yellow command, white description — `§e`/`§f` per `lang/en.yml`) — the `/sidebar reload` help line is ABSENT because the sender lacks `ultisidebar.admin` | server | |
| ultisidebar.sidebar.help.admin | `language: en`; sender holds `ultisidebar.toggle` AND `ultisidebar.admin` | Run `/sidebar help` | Same four lines as `ultisidebar.sidebar.help`, PLUS a fifth line for `/sidebar reload` (yellow command, white description, same `§e`/`§f` pattern) | server | |

## Sidebar Lifecycle Events

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultisidebar.sidebar.auto-enable-on-join | `sidebar.yml` `enabled: true` (shipped default); the joining player has NO persisted preference row yet (a genuinely new player, or one whose row was externally deleted); `default-enabled: true` (shipped default) | The player joins the server and waits at least 0.5 seconds (10 ticks) | The sidebar scoreboard appears automatically, without the player running any command — `SideBarService#onPlayerJoin` scheduled `enableSidebar` 10 ticks after join because `isSidebarEnabledInDatabase` returned `config.isDefaultEnabled()` (`true`) for the missing row | server | |
| ultisidebar.sidebar.auto-enable-on-join.neg-default-disabled | Same as `ultisidebar.sidebar.auto-enable-on-join`, except `default-enabled: false` | The player joins the server and waits at least 0.5 seconds | No sidebar appears — `isSidebarEnabledInDatabase` returned `false` for the missing row, so `SideBarService#onPlayerJoin`'s own `if` never schedules the enable at all | server | |
| ultisidebar.sidebar.cache-cleanup-on-quit | The player's sidebar is currently ON (run `ultisidebar.sidebar.on` first); `default-enabled` or the persisted preference is such that a rejoin would NOT auto-re-enable within the observation window, to avoid the unrelated `ultisidebar.sidebar.auto-enable-on-join` race masking this row's own result | The player quits (a real disconnect, NOT `/sidebar off` — running `/sidebar off` first would independently call `removeSidebar`, exercising the exact same map-clearing code this row exists to isolate, which would make the row pass even if the quit handler itself were broken), then rejoins and, WITHOUT running any sidebar command, observes for at least 2 seconds | No exception or warning appears in the server log around the quit/rejoin sequence, and the player's client shows no leftover sidebar scoreboard from the pre-quit session (the previous `Scoreboard` object is not still assigned) — this is a weaker, indirect proof than inspecting `SideBarService#playerScoreboards`/`#contentCache` directly (not possible without server-side code access), but it is the strongest player-observable signal this handler's cleanup admits: the maps exist purely for memory hygiene across many quit/rejoin cycles, and a single cycle's gameplay outcome does not otherwise depend on whether they were cleared | server | |
| ultisidebar.sidebar.world-blacklist-hide | `world-blacklist` contains `world_event` (shipped default); the player's sidebar is currently ON; a second world named `world_event` exists on the server | The player changes into the `world_event` world | The sidebar scoreboard disappears immediately (the player is reset to the main scoreboard) — the player's persisted enabled/disabled preference is UNCHANGED (confirmed by a subsequent `ultisidebar.sidebar.world-blacklist-show`, not this row) | server | |
| ultisidebar.sidebar.world-blacklist-show | Immediately after `ultisidebar.sidebar.world-blacklist-hide` (same player, sidebar preference still enabled, currently in `world_event`) | The player changes from `world_event` into a non-blacklisted world | The sidebar scoreboard re-appears automatically, without the player running any command, showing the SAME title and lines content as `ultisidebar.sidebar.toggle`'s own render assertion (subject to whatever placeholder values changed between the two) | pixel | |

## Persistence

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultisidebar.sidebar.preference-persistence | The player's sidebar is currently OFF (run `ultisidebar.sidebar.off` first) | Run `/sidebar off`, restart the server (not `/ul reload` — a full process restart), rejoin, then run `/sidebar toggle` | After restart and rejoin, the sidebar does NOT auto-appear (the persisted preference from before restart is still `false`, overriding `default-enabled`); the subsequent `/sidebar toggle` flips it ON — proving the OFF state, not just the default, survived the restart via `SideBarPreference` (`sidebar_preferences` table) | server | |

## Configuration

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultisidebar.config.sidebar-yml | Fresh `config/sidebar.yml` at its shipped content (6 keys: `enabled: true`, `title` and `lines` as the Chinese literals translated in `FEATURES.md`, `update-interval: 20`, `world-blacklist: ["world_event"]`, `default-enabled: true`) | Load the file; confirm all 6 keys are present at their documented defaults; then exercise 4 representative changes in separate reloads so no change masks another's effect: (a) set `enabled: false` and reload, confirming NO new sidebar can be enabled at all (`/sidebar on` produces its normal success chat line, per `SideBarCommand#on`'s own logic, but no scoreboard actually appears — `SideBarService#enableSidebar`'s own `if (!config.isEnabled()) return;` guard fires silently before the message is sent); restore `enabled: true` before continuing; (b) set `title` to a distinguishable ASCII string (validated `@Size(min=1, max=32)`) and confirm the rendered scoreboard title changes to exactly that string on the next toggle; (c) set `update-interval: 100` (5 seconds) and confirm a `%server_online%`-driven line does NOT update within 2 seconds of a second player joining, but DOES update within the next 5-second tick — proving the interval, not merely the cache-skip logic, controls the cadence; (d) persist a `sidebar.yml` whose `lines` list contains the EXACT legacy string `%world_name%`-based world line (`UltiKits/UltiSideBar#13`'s pre-fix shipped default) and confirm it is silently rewritten to the corrected `%player_world%`-based line on the next plugin enable/reload, while an adjacent hand-customised line is left untouched | Before any change: all 6 keys present at their documented defaults, confirmed by reading `sidebar.yml` directly; (a) `enabled: false` fully suppresses new sidebars despite an unconditional success message; (b) the title change is visible; (c) the update cadence follows the configured interval, not a shorter or immediate refresh; (d) only the byte-identical legacy line is rewritten, any customised line survives unchanged | pixel | |
