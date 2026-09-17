# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Fixed

- Unloading or reloading this module (`/ul reload UltiSideBar`, `/sidebar reload`, or disabling the
  module) now actually runs the framework's own reload/unload steps (config reload, language
  refresh, `@ConditionalOnConfig` drift reporting, command/listener cleanup) before this module's
  own sidebar service reload/shutdown — previously these framework steps were silently skipped
  (UltiKits/UltiSideBar#16).
- 卸载或重载本模块（`/ul reload UltiSideBar`、`/sidebar reload`，或禁用本模块）现在会先真正执行框架自身的
  重载/卸载步骤（配置重载、语言刷新、`@ConditionalOnConfig` 漂移报告、命令/监听器清理），再执行本模块自身的
  侧边栏服务重载/关闭——此前这些框架步骤会被静默跳过（UltiKits/UltiSideBar#16）。
