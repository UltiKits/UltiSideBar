# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Fixed

- Reloading this module (`/ul reload UltiSideBar`, or `/sidebar reload`) now runs the framework's own
  reload steps (config reload, language refresh, `@ConditionalOnConfig` drift report, one framework
  reload log line) before this module's sidebar service reload; unloading it (for example
  `/upm uninstall UltiSideBar`, or server shutdown) now runs this module's sidebar shutdown and then
  the framework's command and listener unregistration — previously the module's overrides replaced
  both framework methods, so those framework steps were silently skipped (UltiKits/UltiSideBar#16).
- 重载本模块（`/ul reload UltiSideBar` 或 `/sidebar reload`）现在会先执行框架自身的重载步骤（配置重载、
  语言刷新、`@ConditionalOnConfig` 漂移报告、一条框架重载日志），再执行本模块的侧边栏服务重载；卸载本模块
  （例如 `/upm uninstall UltiSideBar`，或服务器关闭）现在会先执行本模块的侧边栏关闭，再执行框架的命令与
  监听器注销——此前本模块的覆盖方法替换了这两个框架方法，这些框架步骤会被静默跳过（UltiKits/UltiSideBar#16）。
