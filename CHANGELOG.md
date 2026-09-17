# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Fixed

- Reloading this module (`/ul reload UltiSideBar` or `/sidebar reload`) now also refreshes its
  language catalogue, which the module's own reload override skipped; configuration is still
  reloaded exactly once, now by the framework instead of by the module. Unloading it now runs the
  module's own sidebar shutdown first and then also unregisters its commands, which were left
  registered on every unload path (`/upm uninstall UltiSideBar` and server shutdown), and, for
  `/upm uninstall UltiSideBar`, its listeners, which server shutdown already removed
  (UltiKits/UltiSideBar#16).
- 重载本模块（`/ul reload UltiSideBar` 或 `/sidebar reload`）现在还会刷新其语言目录——此前本模块自身的重载
  覆盖方法跳过了这一步；配置仍然只重载一次，只是改由框架而非模块执行。卸载本模块现在会先执行本模块自身的
  侧边栏关闭，然后还会注销其命令——此前在所有卸载路径（`/upm uninstall UltiSideBar` 与服务器关闭）上命令都
  未被注销；对于 `/upm uninstall UltiSideBar`，还会注销其监听器——服务器关闭时原本就会移除监听器
  （UltiKits/UltiSideBar#16）。
