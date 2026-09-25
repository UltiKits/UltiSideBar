# UltiSideBar — Feature Inventory

This document catalogues every operator- or player-visible function, command, content item and
configuration key in this repository, as read directly from source. It is an internal reference
for UAT execution and issue reconciliation — the public description of these features lives on
<https://doc.ultikits.com/>. Update this file in the same pull request as any feature change.

## Conventions

- **ID grammar:** `<repo-slug>.<area>.<action>`, dot-separated, every segment lowercase ASCII
  drawn from `[a-z0-9-]`. `<repo-slug>` is the repository name lowercased with no separators —
  `ultisidebar` here. `<area>` is the feature section's slug. `<action>` is the verb. A `config`
  row is the one shape that exceeds three segments and is exempt from the lowercase-ASCII rule for
  its key-path suffix: `<repo-slug>.config.<file-stem>.<yml key path>`, the key path keeping its
  own dots and its own casing verbatim from the yml file — a config ID is a citation of the key,
  not a re-derived slug. An ID changes only when the feature's identity changes, never on
  rewording. IDs are unique within a repository.
- **Kind**, exactly these eight values: `command`, `config`, `event`, `gui`, `scheduled`,
  `placeholder`, `persistence`, `gate`. Each maps one-to-one onto a reconciliation-table line,
  except the two `ultisidebar.lifecycle.*` `event` rows under `## Lifecycle Hooks`, which are
  framework-invoked lifecycle hooks with no annotation site to reconcile against.
  This module has no `gui` rows (no GUI page class — a Bukkit scoreboard sidebar is not an
  inventory), no `scheduled` rows (this module's periodic content refresh is a hand-rolled
  `Bukkit.getScheduler().runTaskTimer` call inside `SideBarService#startUpdateTask`, NOT a
  `@Scheduled`-annotated method — the `scheduled` Kind in this document family is reserved for the
  annotation, matching the reconciliation table's own `@Scheduled` line 1:1 (see UltiChat's
  `AnnouncementService`, whose `scheduled` rows ARE `@Scheduled`-annotated); the refresh cadence
  itself is documented as part of the `update-interval` config row below, not as its own row), no
  `placeholder` rows (it consumes PlaceholderAPI variables via `PlaceholderAPI#setPlaceholders`
  inside a player's configured sidebar lines, it does not register its own expansion), and no
  `gate` rows (0 `@ConditionalOnConfig` sites) — all four Kinds stay in the vocabulary for
  cross-repository consistency even though none appears below.
- **Tier**, exactly three: `player`, `admin`, `internal`. Judged from what the feature is for, not
  from whether it carries a permission string — every command below carries one.
- **Manual**, exactly three: `detailed`, `brief`, `none`.
- **Target**, exactly four: `player`, `console`, `both`, or `n/a` — the first three read straight
  off `@CmdTarget` for a `command` row; it is a property, not a tier. `n/a` is for every other Kind
  (`config`, `event`, `persistence`) — the concept of "who this targets" does not apply to a config
  key or a background cleanup task the way it applies to a command.
- **Permission:** the literal node string, `none`, or `n/a`, each optionally suffixed with the
  literal text `(requireOp=true)` when the row's class-level `@CmdExecutor` carries that flag.
  This module's one `@CmdExecutor(permission = "ultisidebar.toggle")` sets no `requireOp`, so no
  row below carries that suffix. **Repository extension of the suffix grammar (shared with
  `Modules/UltiMenu/FEATURES.md`, which documents the same framework mechanism for a
  hand-written, non-framework-validated second check):** `ultisidebar.sidebar.reload` is gated by
  TWO permission checks that ARE the same mechanism, both framework-validated by
  `PermissionValidator` — the class-level `ultisidebar.toggle` (checked once per invocation,
  regardless of mapping) AND the method-level `ultisidebar.admin` declared directly on this
  mapping's own `@CmdMapping(permission = "ultisidebar.admin")`. Read `PermissionValidator`'s own
  javadoc for which senders actually reach which branch: the class-level permission is
  Bukkit-registered (`command.setPermission(...)`), so a player lacking it never sees a command at
  all (Bukkit hides it from the command tree before `onCommand` runs) — only the console, which
  always satisfies a Bukkit permission check, and a player who DOES hold `ultisidebar.toggle`
  reach the method-level `ultisidebar.admin` check inside the validator itself. This row's
  Permission cell states both, connected by ` + `: `ultisidebar.toggle + ultisidebar.admin`.
  `n/a` is for every Kind that is not `command`.
- **Source:** `ClassName#member` — the class and member that actually reads or applies the
  feature — for every Kind, `config` included.
- **Row order:** by section, then by ID ascending within the section.
- **No manual prose:** no troubleshooting column, no explanatory paragraphs, no draft page text. A
  hazard noticed while reading becomes a negative checklist row or a filed issue, not a note here,
  except where a feature's actual runtime behaviour genuinely diverges from what it appears to do —
  that fact is itself part of "what the feature does" and is stated here as a plain, sourced
  observation, with the filed issue number, never as advice on how to fix it.
- **Config text in the server's language:** `title` and `lines` are written into
  `config/sidebar.yml` in the server's language — the language files' `sidebar_default_title` and
  `sidebar_default_lines` — at enable and after a full reload while they are still built-in text, and
  the sidebar shows exactly what the file holds (`ultisidebar.config.sidebar.materialize`). Their Java
  defaults are the last shipped Simplified Chinese title and template, which the framework writes
  for a missing key before the module rewrites them. This module's `i18n(...)` command/status
  messages have complete `lang/en.yml` and `lang/zh.yml` translations (0 missing keys either direction —
  confirmed by a full per-file key diff, unlike the defect classes filed against other modules in
  this phase). Per D-02's English-only rule, every quoted default below is translated into English
  with the exact source `file:line` cited, never reproduced as raw CJK.

### Reconciliation command family

The canonical form for counting an annotation site across this repository's real sources:

```bash
find <repo-root> -path '*/src/main/java/*' -name '*.java' -not -path '*/target/*' \
  -not -path '*/.worktrees/*' -print0 | xargs -0 grep -nE '^[[:space:]]*@AnnotationName\b' | wc -l
```

This module is a single-root, single-file-per-class Maven project (6 files under
`src/main/java`, no worktree directory, no javadoc/string-literal false positive found for any
annotation kind measured below), so none of the three traps this command family defeats
(multi-root layout, git worktrees, javadoc/string mentions) actually changes any count for this
repository — the same robust command is still used, because it must work unmodified across all 18
repositories.

**Positive control:** the line-start form returns `@CmdExecutor` = 1, `@CmdMapping` = 5,
`@EventListener` = 1 (class), `@EventHandler` = 3 (handler methods), `@Scheduled` = 0,
`@ConditionalOnConfig` = 0, `@ConfigEntity` = 1 (class), `@ConfigEntry` = 6, `@Table` = 1 —
confirmed by reading `SideBarCommand.java` directly (5 `@CmdMapping` sites at lines 36, 47, 54, 61,
67: `toggle`, `on`, `off`, `reload`, `` [bare]), `SideBarListener.java` (3 `@EventHandler` sites at
lines 25, 30, 35: `onPlayerJoin`, `onPlayerQuit`, `onWorldChange`), `SideBarConfig.java` (6
`@ConfigEntry` sites at lines 31, 36, 40, 45, 61, 64), and `SideBarPreference.java` (1 `@Table`
site at line 23) directly, not by trusting the count alone. This document's command-row count
matches the `@CmdMapping` annotation-site count exactly (5 against 5); its event-row count is 4
against 3 handler methods, because `onWorldChange` implements two independently observable
behaviours (see `## Sidebar Lifecycle Events` below), with the reason stated once here and in the
reconciliation table.
The two further `event`-Kind rows under `## Lifecycle Hooks` are not `@EventHandler` sites and are
not counted against that line: they are the framework-invoked `UltiSideBar#onReload()`/
`#onUnregister()` lifecycle hooks, catalogued as `event` because the framework, not a player command
or config read, triggers them.

## Sidebar Commands

`SideBarCommand` — class-level `@CmdExecutor(alias = {"sidebar", "sb"}, permission =
"ultisidebar.toggle", description = "sidebar_command_description")`, `@CmdTarget(BOTH)`.
`sidebar_command_description` resolves via `plugin.i18n(...)` to `lang/en.yml`'s
`"Toggle sidebar display"` (or `lang/zh.yml`'s Chinese equivalent) — unlike `Modules/UltiMenu`'s
class-level description, this one has a complete translation in both shipped language files, no
raw-key fallback.

**Note on bare `/sidebar` and `/sidebar help`:** `BaseCommandExecutor#onCommand` short-circuits a
literal `help` argument to `SideBarCommand#handleHelp` before format-matching runs, and
`handleHelp` simply delegates to `help(sender)` — the same method the real, empty-format
`@CmdMapping(format = "")` mapping (line 67) calls for a bare `/sidebar`. Both paths produce
identical output; this document's command-row count is fixed at exactly 5 (matching the 5 real
`@CmdMapping` sites 1:1), with no added short-circuit-only row, the same deliberate scope decision
`Modules/UltiMenu/FEATURES.md` and `Modules/UltiChat/FEATURES.md` record for their own `help`
short-circuits.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultisidebar.sidebar.toggle | Toggle the sender's own sidebar scoreboard on or off, based on its CURRENT persisted state (see `ultisidebar.sidebar.preference-persistence`), console refused before this method runs (`@CmdTarget(PLAYER)` on this mapping) | command | `/sidebar toggle` (alias `/sb toggle`) | ultisidebar.toggle | player | player | brief | SideBarCommand#toggle |
| ultisidebar.sidebar.on | Explicitly enable the sender's sidebar scoreboard; unlike `Modules/UltiEssentials`'s equivalent `/scoreboard on`, this method has NO "already enabled" branch — running it while already on unconditionally rebuilds the scoreboard object and sends the same success message again, it is not a no-op with a distinct message | command | `/sidebar on` | ultisidebar.toggle | player | player | brief | SideBarCommand#on |
| ultisidebar.sidebar.off | Explicitly disable the sender's sidebar scoreboard; like `.on`, has no "already disabled" branch — running it while already off unconditionally resets the sender to the server's main scoreboard and sends the same success message again | command | `/sidebar off` | ultisidebar.toggle | player | player | brief | SideBarCommand#off |
| ultisidebar.sidebar.reload | Reload this module's configuration and refresh online players' sidebars — NOT unconditionally "every" one: `SideBarService#init`'s refresh loop only re-enables a player whose preference is enabled AND `config.isDefaultEnabled()` is true, so with `default-enabled: false` an online player with an explicitly-enabled preference is removed by the preceding `shutdown()` and NOT restored (`UltiKits/UltiSideBar#20`, filed, not fixed here, see `ultisidebar.config.sidebar.default-enabled`'s own row). Also gated by TWO permission checks — see this row's own Permission cell for the mechanism split. Delegates to `UltiToolsPlugin#reloadSelf()`, a `final` framework template method as of UltiTools 6.3.0 (`UltiKits/UltiSideBar#16`): it reloads this module's configuration (once — this module no longer reloads it itself), re-creates the language catalogue from this module's language files, reports `@ConditionalOnConfig` drift, logs the framework's own `Module 'UltiSideBar' reloaded.` line, and only then calls this module's `onReload()` hook (see `ultisidebar.lifecycle.reload`). A change to the framework's `language` SETTING in `plugins/UltiTools/config.yml` is NOT picked up by this path — the catalogue is rebuilt for the language code read from the framework's cached `config.yml`, which only a bare `/ul reload` or a restart re-reads (`UltiKits/UltiTools-Reborn#502`, filed, not fixed here). Each reload also leaves one more configuration change listener registered on `SideBarConfig`, so every later reload rebuilds each enabled online sidebar once per accumulated listener (`UltiKits/UltiSideBar#21`, filed, not fixed here) | command | `/sidebar reload` | ultisidebar.toggle + ultisidebar.admin | both | admin | detailed | SideBarCommand#reload |
| ultisidebar.sidebar.help | Print the command's own help lines (title, toggle, on, off — plus reload, only if the sender holds `ultisidebar.admin`); reachable both as the bare `/sidebar` (a real, empty-format `@CmdMapping`) and as `/sidebar help` (the framework's own literal-`help` short-circuit, routed to the identical method) | command | `/sidebar` (bare) or `/sidebar help` | ultisidebar.toggle | both | player | brief | SideBarCommand#help |

## Sidebar Lifecycle Events

`SideBarListener` — one `@EventListener` class with 3 `@EventHandler` methods, each delegating
straight to `SideBarService`. `onWorldChange` implements two independently observable behaviours
(hiding the sidebar on entering a blacklisted world, and re-showing it on leaving one), catalogued
as two rows — the reason the reconciliation table's `@EventListener` handler-method line reads 4
rows against 3 methods.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultisidebar.sidebar.auto-enable-on-join | 10 ticks (0.5s) after a player joins, auto-enable their sidebar IF their persisted preference (or, absent one, `default-enabled`) says enabled. Has NO online-status re-check before enabling — `Bukkit.getScheduler().runTaskLater(bukkitPlugin, () -> enableSidebar(player), 10L)`'s lambda calls `enableSidebar(player)` unconditionally, even if that same player quit during the 10-tick delay; `onPlayerQuit`'s cleanup already ran by the time this delayed callback fires, so it silently re-populates `playerScoreboards`/`contentCache` for an offline player, which is never cleaned up again (`UltiKits/UltiSideBar#19`, filed, not fixed here) | event | join the server with an enabled persisted preference, or no preference and `default-enabled: true` (the shipped default) | n/a | n/a | player | detailed | SideBarListener#onPlayerJoin, SideBarService#onPlayerJoin |
| ultisidebar.sidebar.cache-cleanup-on-quit | On quit, remove the player's in-memory scoreboard object and content cache — this does NOT touch the persisted enabled/disabled preference row, only in-memory state; the DB preference from `ultisidebar.sidebar.preference-persistence` is what a later rejoin reads | event | quit the server with an active sidebar | n/a | n/a | internal | none | SideBarListener#onPlayerQuit, SideBarService#onPlayerQuit |
| ultisidebar.sidebar.world-blacklist-hide | Entering a world named in `world-blacklist` removes the visible sidebar immediately, regardless of the player's persisted enabled/disabled preference (the preference itself is untouched — this is a display-only hide, like `.cache-cleanup-on-quit`) | event | change into a world listed in `world-blacklist` (shipped default: `world_event`) while the sidebar is currently visible | n/a | n/a | player | brief | SideBarListener#onWorldChange, SideBarService#onWorldChange |
| ultisidebar.sidebar.world-blacklist-show | Leaving a blacklisted world for a non-blacklisted one re-shows the sidebar, IF the player's sidebar is otherwise enabled (config `enabled: true`, persisted preference enabled) and it is not already being displayed | event | change from a world listed in `world-blacklist` into one that is not, with an otherwise-enabled sidebar | n/a | n/a | player | brief | SideBarListener#onWorldChange, SideBarService#onWorldChange |

## Persistence

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultisidebar.sidebar.preference-persistence | A player's sidebar enabled/disabled preference (set via `/sidebar toggle`/`on`/`off`) survives a server restart — backed by `SideBarPreference` (`@Table("sidebar_preferences")`) through the framework's own `DataOperator`, written synchronously at the moment of the toggle, not deferred to shutdown. If duplicate rows ever exist for one player (e.g. from an external data-store edit), the service deterministically picks the row with the lexicographically-smallest entity id as canonical, rather than depending on undefined query-result ordering | persistence | run `/sidebar off`, restart the server, run `/sidebar toggle` | n/a | n/a | player | brief | SideBarService#isSidebarEnabledInDatabase, SideBarService#setSidebarEnabledInDatabase, SideBarService#selectCanonicalPreference, SideBarPreference |

## Lifecycle Hooks

As of UltiTools 6.3.0 `UltiToolsPlugin#reloadSelf()`/`#unregisterSelf()` are `final`; this module
overrides the `onReload()`/`onUnregister()` hooks they call (`UltiKits/UltiSideBar#16`). Reload order:
`ConfigManager#reloadConfigs`, language catalogue refresh, `@ConditionalOnConfig` drift report,
`Module 'UltiSideBar' reloaded.`, then `onReload()`. Unload order: `onUnregister()`, then command
unregistration, then listener unregistration. Before the migration this module replaced both
methods. Its reload override reloaded configuration itself (`origin/master` `UltiSideBar.java:48`)
but skipped the language refresh. Its unload override made `/upm uninstall UltiSideBar` skip both
command and listener unregistration. Server shutdown was unaffected: the framework ran its own command
and listener cleanup there independently of the override. The drift report and the reload log line are new in 6.3.0.
Configuration is reloaded once per reload both before and after the migration.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultisidebar.lifecycle.reload | After the framework's `reloadSelf()` has reloaded this module's configuration, refreshed its language catalogue, reported drift and logged `Module 'UltiSideBar' reloaded.`, rebuild the sidebar service (`SideBarService#reload()`: cancel the update task and remove every sidebar, then re-initialise and re-enable the sidebar of each online player whose preference is enabled while `default-enabled` is `true` — see `ultisidebar.config.sidebar.default-enabled` and `UltiKits/UltiSideBar#20`) and log `sidebar_reloaded`; the hook itself never reloads configuration | event | `/ul reload UltiSideBar`, or `/sidebar reload` (both call the framework's `reloadSelf()`) | n/a | n/a | admin | brief | UltiSideBar#onReload, SideBarService#reload |
| ultisidebar.lifecycle.unload | When the module is unloaded, shut the sidebar service down (`SideBarService#shutdown()`: cancel the periodic update task and reset every online player to the server's main scoreboard) and log `sidebar_disabled`, before the framework's own command and listener cleanup for this module | event | `/upm uninstall UltiSideBar` (framework `PluginInstallUtils#uninstallPlugin` calls `unregisterSelf()`, which invokes this hook first) | n/a | n/a | admin | brief | UltiSideBar#onUnregister, SideBarService#shutdown |

## Configuration

`@ConfigEntity("config/sidebar.yml")` on `SideBarConfig`. All 6 keys below are framework-bound
`@ConfigEntry` fields.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultisidebar.config.sidebar.enabled | Master on/off switch for the entire sidebar feature — when `false`, `SideBarService#startUpdateTask` never starts the periodic refresh task at all, and `enableSidebar` is a no-op regardless of a player's persisted preference | config | `config/sidebar.yml: enabled (default: true)` | n/a | n/a | admin | brief | SideBarConfig#enabled, SideBarService#startUpdateTask, SideBarService#enableSidebar |
| ultisidebar.config.sidebar.title | The sidebar's scoreboard objective title, `&`-color-coded and PlaceholderAPI-substituted (via `parsePlaceholders`, which ONLY calls `PlaceholderAPI#setPlaceholders` — there is NO `{player}`-token replacement anywhere in this module, unlike `Modules/UltiMenu`'s own `{player}` mechanism) on every refresh; validated `@NotEmpty` and `@Size(min=1, max=32)`. The value shown is the file's value, as written. The Java default is the title every earlier version shipped (a Chinese literal, `SideBarConfig#SHIPPED_TITLE`, translated `"&6&lMy Server"`); at first start and whenever it is still built-in text it is rewritten to the language file's `sidebar_default_title` in the server's language — `"&6&lMy Server"` under `language: en` (`ultisidebar.config.sidebar.materialize`) | config | `config/sidebar.yml: title (written in the server's language: '&6&lMy Server' under language: en)` | n/a | n/a | admin | detailed | SideBarConfig#title, SideBarConfig#materializeText, SideBarService#updateSidebar |
| ultisidebar.config.sidebar.update-interval | Ticks between sidebar content refreshes for every online player with an active sidebar, applied via a hand-rolled `Bukkit.getScheduler().runTaskTimer` (NOT a `@Scheduled` method — see this document's Kind Conventions); validated `@Range(min=1, max=1200)` | config | `config/sidebar.yml: update-interval (default: 20 — 1 second)` | n/a | n/a | admin | brief | SideBarConfig#updateInterval, SideBarService#startUpdateTask |
| ultisidebar.config.sidebar.lines | The sidebar's body lines, in display order, each PlaceholderAPI-substituted (NOT `{player}`-substituted — see `.title`'s own note) and `&`-color-coded on every refresh; validated `@NotEmpty` and `@Size(min=1, max=15)`. A line whose rendered (post-substitution, post-color, UNTRUNCATED) text exactly matches an already-added line's untruncated text is disambiguated with an appended invisible `ChatColor.RESET` marker BEFORE truncation to 40 characters — but for two DIFFERENT untruncated lines that merely share their first 40+ characters, the dedup check (run against the untruncated candidate) never fires, so both truncate to the identical 40-character string and the second silently overwrites the first's `Score` entry, showing only one line where two were configured (`UltiKits/UltiSideBar#17`, filed, not fixed here — the same ordering defect independently observed, but not yet filed, in `Modules/UltiEssentials`'s equivalent `ScoreboardService#ensureUnique`). The Java default is the 12-line template the last version shipped (Chinese literals, `SideBarConfig#SHIPPED_LINES`); at first start and whenever the whole list is still built-in text it is rewritten to the language file's `sidebar_default_lines` in the server's language (`ultisidebar.config.sidebar.materialize`) — under `language: en`: `"&7Welcome, &f%player_name%"`, `""`, `"&eOnline: &f%server_online%/%server_max_players%"`, `"&eWorld: &f%player_world%"`, `""`, `"&eBalance: &f%vault_eco_balance_formatted%"`, `"&ePing: &f%player_ping%ms"`, `""`, `"&7Server time"`, `"&f%server_time_HH:mm:ss%"`, `""`, `"&6play.example.com"` | config | `config/sidebar.yml: lines (written in the server's language: the 12 English lines quoted here under language: en)` | n/a | n/a | admin | detailed | SideBarConfig#lines, SideBarService#updateSidebar |
| ultisidebar.config.sidebar.legacy-line-migration | On plugin enable and on every `/sidebar reload`, a persisted `sidebar.yml` whose `lines` list still contains one or more BYTE-IDENTICAL pre-6.3.0 stale shipped defaults (the invalid `%world_name%` world line and the ambiguous 12-hour `%server_time_hh:mm:ss%` line, `UltiKits/UltiSideBar#13`) has ONLY those exact entries rewritten in place to their corrected replacements and persisted back to disk; any operator customisation, including a line that merely mentions a legacy token alongside other text, is left untouched. Idempotent — once migrated, a second run finds nothing left to rewrite | config | automatic on plugin enable/reload, for any `sidebar.yml` carrying a tracked legacy default line | n/a | n/a | internal | detailed | SideBarConfig#migrateLegacyDefaultLines, SideBarService#init |
| ultisidebar.config.sidebar.materialize | On plugin enable and on every reload (`SideBarService#init`, called from `registerSelf()` and from `onReload()` after the framework has reloaded the language), `title` and `lines` are each replaced with the server's-language text — the language file's `sidebar_default_title`, and `sidebar_default_lines` split on `\n` — when the value in `sidebar.yml` is still built-in text: the title or one of the three `lines` lists an earlier version shipped (`SHIPPED_TITLE`; `SHIPPED_LINES_FIRST`, `SHIPPED_LINES_SECOND`, `SHIPPED_LINES`), or this jar's own English or Chinese text for it (read from the module jar, never from the language files on disk). The file is saved once when anything changed. So an untouched value follows a `language` switch in both directions; any other value, including a built-in text changed by one character or a list with one edited line, is kept byte for byte and the file is not rewritten. A second start with the same language writes nothing. The configuration change listener does not do this: the framework fires it before it reloads the language. A single-module `ul reload UltiSideBar` does not re-read the framework's `language` setting, so a changed `language` is picked up on a full `ul reload` or a restart (UltiKits/UltiSideBar#24) The text written is this jar's own built-in text for the server's language (the jar's `lang/<language>.*`), not the extracted language file on disk, so every value the module writes is one it recognises again; these settings are edited in the config file, and an edit of the extracted language file does not change them (earlier versions never read them from the language file either; orchestrator ruling O3, 2026-09-25). | config | automatic on plugin enable/reload | n/a | n/a | internal | detailed | SideBarConfig#materializeText, ConfigTextDefaults, SideBarService#init |
| ultisidebar.config.sidebar.world-blacklist | World names in which the sidebar is force-hidden regardless of the player's persisted preference (see `ultisidebar.sidebar.world-blacklist-hide`/`.world-blacklist-show`) | config | `config/sidebar.yml: world-blacklist (default: a single-entry list ["world_event"])` | n/a | n/a | admin | brief | SideBarConfig#worldBlacklist, SideBarService#enableSidebar, SideBarService#onWorldChange |
| ultisidebar.config.sidebar.default-enabled | The sidebar-enabled state assumed for a player with NO persisted preference row yet (their very first join, or after any external deletion of their row), consulted by `isSidebarEnabledInDatabase`. It ALSO has a second, unrelated effect this key's own name does not suggest: `SideBarService#init`'s online-player refresh loop (lines 96-99, run at plugin enable AND on every `/sidebar reload`) re-enables an online player's sidebar only when `config.isDefaultEnabled() && isSidebarEnabledInDatabase(...)` BOTH hold — so with `default-enabled: false`, `/sidebar reload` silently removes (via the preceding `shutdown()`) and does NOT restore the sidebar of an online player who has an EXISTING, explicitly-enabled preference, contradicting this key's own "no further effect once a preference exists" premise (`UltiKits/UltiSideBar#20`, filed, not fixed here) | config | `config/sidebar.yml: default-enabled (default: true)` | n/a | n/a | admin | detailed | SideBarConfig#defaultEnabled, SideBarService#isSidebarEnabledInDatabase, SideBarService#init |
