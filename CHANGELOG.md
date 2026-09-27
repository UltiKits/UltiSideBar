# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Changed

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

- Two sidebar lines that differ only after their 40th character now both show; previously the second
  one replaced the first (UltiKits/UltiSideBar#17).
- Name prefixes and other main-scoreboard teams (UltiEssentials name prefixes, vanilla `/team` teams,
  other plugins' teams) now show while the sidebar is on. The sidebar's own scoreboard now carries the
  server's main-scoreboard teams and follows their changes on every refresh; previously every player
  with the sidebar on saw no team prefix on anybody (UltiKits/UltiSideBar#27).
- 侧边栏开启时，名字前缀和主计分板上的其它队伍（UltiEssentials 的名字前缀、原版 `/team` 队伍、其它插件的队伍）
  现在都会显示：侧边栏自己的计分板会带上服务器主计分板的队伍，并在每次刷新时跟随其变化；此前开着侧边栏的
  玩家看不到任何人的队伍前缀（UltiKits/UltiSideBar#27）。
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
