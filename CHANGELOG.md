# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Changed

- `lines: []` (or `lines: ~`) in `config/sidebar.yml`, and a `lines` value that is not a list (`''`, a word, a map), now runs the
  shipped 12 lines. The setting is declared `@NotEmpty`, and on UltiTools 6.3.0 the framework treats such a value as unusable: the
  sidebar runs on the shipped lines and the framework logs one WARNING naming `lines`, the value as written and that default.
  The file is left exactly as you wrote it. Earlier versions (UltiTools 6.2.x, UltiSideBar 1.0.0) reset `[]` to the default
  with a warning and, for `~`, wrote the default into the file. The sidebar shows the shipped lines in the server's language,
  like any built-in text, and the module adds no second WARNING ("the module's changes to 'lines' were not written") at
  start or `/ul reload`, and no report of unsaved changes when the server stops (UltiKits/UltiSideBar#36).
- `config/sidebar.yml` 中的 `lines: []`（或 `lines: ~`），以及不是列表的 `lines` 值（`''`、一个词、一个映射），现在改用出厂的 12 行。
  该设置声明了 `@NotEmpty`，在 UltiTools 6.3.0 上框架把这类值视为不可用：侧边栏改用出厂行，框架记录一条 WARNING，写明 `lines`、
  你写下的值与该默认值。文件保持你写的原样。早期版本（UltiTools 6.2.x、UltiSideBar 1.0.0）会把 `[]` 重置为默认值并给出警告，
  `~` 则会把默认值写入文件。侧边栏按服务器语言显示出厂行（与其他内置文本相同），模块也不再在启动或 `/ul reload` 时额外记录
  一条“模块对 'lines' 的更改未写入”的 WARNING，服务器停止时也不再报告未保存的更改（UltiKits/UltiSideBar#36）。
- A `lines` list of more than 15 entries now refuses the module at load, naming the field, the count and the bounds
  (`field 'lines' size 16 is out of bounds [1, 15]`), and the file is left as written. Earlier versions (UltiTools 6.2.x)
  reset an over-long list to the default with a warning (UltiKits/UltiSideBar#31).
- 超过 15 条的 `lines` 列表现在会在加载时拒绝本模块，并指出字段、条数与上限（`field 'lines' size 16 is out of bounds [1, 15]`），
  文件保持原样。早期版本（UltiTools 6.2.x）会把过长的列表重置为默认值并给出警告（UltiKits/UltiSideBar#31）。

- The sidebar's title and lines settings in `config/sidebar.yml` are written in the server's
  language when the module starts, and the file is what the sidebar shows (`title: '&6&lMy Server'`
  and English lines under `language: en`). A setting that is still built-in text — in any language,
  or a default an earlier version shipped — follows `language`: it is rewritten when the module starts
  or after `/ul reload`. A setting you edited is kept. To keep a built-in text but stop it following
  `language`, change at least one character (UltiKits/UltiSideBar#24). The text written is this module's built-in text: edit these settings in `config/sidebar.yml`; an edit of
  the extracted language file does not change them (earlier versions never read them from the
  language file either).
- The console warnings for a missing PlaceholderAPI and for a `sidebar.yml` that cannot be saved now
  follow the `language` setting.
- `config/sidebar.yml` 中的侧边栏标题与内容行设置在模块启动时按服务器语言写入，文件内容即侧边栏显示的内容
  （`language: en` 下为 `title: '&6&lMy Server'` 与英文内容行）。仍为内置文本（任一语言的内置文本，或旧版本的
  出厂默认值）的设置会跟随 `language`：模块启动或执行 `/ul reload` 后改写为当前语言的文本。你改过的设置保持不变。
  若想保留内置文本又不让它跟随语言，请至少改动一个字符（UltiKits/UltiSideBar#24）。写入的是本模块的内置文本：请在 `config/sidebar.yml` 中修改这些设置；修改已解压的语言文件不会改变它们（旧版本同样从不从语言文件读取它们）。
- 缺少 PlaceholderAPI 以及 `sidebar.yml` 无法保存时的控制台警告现在跟随 `language` 设置。

### Fixed

- The comments above the keys of `config/sidebar.yml` now come from the module's language files: a
  server set to `language: en` writes English comments on a fresh install (earlier versions wrote
  Chinese-only comments in every language). The comments the framework wrote on these six keys, the Chinese
  ones earlier versions wrote included, switch to the server's language at the next start, and after you
  change `language` and run a bare `/ul reload`; values are untouched, and a comment you wrote yourself is kept as
  you wrote it (UltiKits/UltiTools-Reborn#611) (UltiKits/UltiSideBar#32).
- `config/sidebar.yml` 中各配置项上方的注释现在取自模块的语言文件：`language: en` 的服务器全新安装时写入英文注释
  （此前所有语言下都写入纯中文注释）。框架在这六项上写下的注释（包括旧版本写下的中文注释）会在下次启动时、以及你修改
  `language` 并执行不带参数的 `/ul reload` 后切换为服务器语言；配置值不变，你自己写的注释保持原样（UltiKits/UltiTools-Reborn#611）
  （UltiKits/UltiSideBar#32）。
- The sidebar now recovers when another plugin changes the scoreboard it shows: if another plugin
  unregisters the sidebar's objective, clears or takes its display slot, or resets its scores, the next
  refresh registers the objective again, puts it back in the slot and draws every line again. When the
  lines change, only the lines this module wrote earlier are reset; before, every entry of the board was
  reset on every objective, which also erased another plugin's below-name, player-list or sidebar scores
  on the same board (UltiKits/UltiSideBar#30).
- 其他插件改动侧边栏所用的计分板后，侧边栏现在会恢复：若其他插件注销了侧边栏的目标、清空或占用了其显示位置、
  或重置了分数，下一次刷新会重新注册目标、放回显示位置并重画所有内容行。内容变化时只重置本模块之前写入的行；
  此前会对计分板上的每个条目在所有目标上执行重置，同时清掉其他插件在同一计分板上的名字下方、玩家列表或侧边栏分数
  （UltiKits/UltiSideBar#30）。
- Two sidebar lines that differ only after their 40th character now both show; previously the second
  one replaced the first (UltiKits/UltiSideBar#17).
- The sidebar no longer replaces another plugin's sidebar (such as UltiEssentials' scoreboard): whichever
  sidebar a player sees first stays, and this one appears once the other is turned off. `/sidebar on`
  and `/sidebar toggle` now say when another scoreboard keeps the slot (the choice is still saved), and
  `/sidebar off` no longer removes another plugin's sidebar. When UltiEssentials' sidebar is also
  enabled, one console line says so after start-up (UltiKits/UltiSideBar#26).
- 侧边栏不再顶替其它插件的侧边栏（例如 UltiEssentials 的计分板）：玩家先看到哪个侧边栏就保留哪个，另一个关闭后
  本侧边栏才显示。`/sidebar on` 与 `/sidebar toggle` 在另一个计分板占用该位置时会如实说明（选择仍会保存），
  `/sidebar off` 也不再移除其它插件的侧边栏。UltiEssentials 的侧边栏同时开启时，启动后控制台会有一行提示
  （UltiKits/UltiSideBar#26）。
- Name prefixes and other main-scoreboard teams (UltiEssentials name prefixes, vanilla `/team` teams,
  other plugins' teams) now show while the sidebar is on. The sidebar's own scoreboard now carries the
  server's main-scoreboard teams and follows their changes on every refresh; previously every player
  with the sidebar on saw no team prefix on anybody. A sidebar line that reads exactly like a team
  member's name keeps its own look rather than taking that team's prefix (UltiKits/UltiSideBar#27).
- 侧边栏开启时，名字前缀和主计分板上的其它队伍（UltiEssentials 的名字前缀、原版 `/team` 队伍、其它插件的队伍）
  现在都会显示：侧边栏自己的计分板会带上服务器主计分板的队伍，并在每次刷新时跟随其变化；此前开着侧边栏的
  玩家看不到任何人的队伍前缀。与某个队伍成员名字完全相同的侧边栏行保持原样，不会带上该队伍的前缀（UltiKits/UltiSideBar#27）。
- 只在第 40 个字符之后才不同的两行侧边栏内容现在都会显示；此前第二行会顶替第一行（UltiKits/UltiSideBar#17）。
- With `default-enabled: false`, reloading (`/sidebar reload`, `/ul reload UltiSideBar`) now keeps the
  sidebar of an online player who turned it on themselves; previously the reload removed it and did not
  bring it back. `default-enabled` now applies only to a player with no stored choice
  (UltiKits/UltiSideBar#20).
- 在 `default-enabled: false` 时，重载（`/sidebar reload`、`/ul reload UltiSideBar`）现在会保留自己开启了侧边栏的在线
  玩家的侧边栏；此前重载会移除它且不再恢复。`default-enabled` 现在只作用于没有保存过选择的玩家（UltiKits/UltiSideBar#20）。
- Reloading this module (`/ul reload UltiSideBar` or `/sidebar reload`) now also refreshes its
  language catalogue, which the module's own reload override skipped; configuration is still
  reloaded exactly once, now by the framework instead of by the module. Unloading it with
  `/upm uninstall UltiSideBar` still runs the module's own sidebar shutdown first; afterwards the
  module's commands are now really removed and its listeners stop firing, where previously both
  stayed active until the server restarted (UltiKits/UltiSideBar#16).
- 重载本模块（`/ul reload UltiSideBar` 或 `/sidebar reload`）现在还会刷新其语言目录——此前本模块自身的重载
  覆盖方法跳过了这一步；配置仍然只重载一次，只是改由框架而非模块执行。通过 `/upm uninstall UltiSideBar`
  卸载本模块时仍会先执行本模块自身的侧边栏关闭；之后本模块的命令现在会被真正移除，其监听器也不再触发，
  此前两者都会保持生效，直到服务器重启（UltiKits/UltiSideBar#16）。
