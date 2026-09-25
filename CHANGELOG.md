# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Changed

- The sidebar title now follows the server's `language` setting. `title` in `config/sidebar.yml` now
  ships blank, and while it is blank the sidebar shows the language file's title (`My Server` under
  `language: en`). On start-up and on every reload, a `title` that is exactly the title earlier
  versions shipped is blanked and the file saved, so an upgraded server shows the translated title
  too; any other title is kept as written. `lines` is unchanged: it is the server's own layout and is
  shown as written.
- The console warnings for a missing PlaceholderAPI and for a `sidebar.yml` that cannot be saved now
  follow the `language` setting.
- 侧边栏标题现在跟随服务器的 `language` 设置。`config/sidebar.yml` 中的 `title` 默认留空，留空时侧边栏显示
  语言文件中的标题（`language: en` 下为 `My Server`）。启动时和每次重载时，若 `title` 与旧版本出厂的标题
  完全相同，会被清空并保存文件，使升级后的服务器同样显示翻译后的标题；其他任何标题都按原样保留。`lines`
  不变：它是服务器自己的布局，按原样显示。
- 缺少 PlaceholderAPI 以及 `sidebar.yml` 无法保存时的控制台警告现在跟随 `language` 设置。

### Fixed

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
