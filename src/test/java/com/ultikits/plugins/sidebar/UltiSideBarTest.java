package com.ultikits.plugins.sidebar;

import com.ultikits.plugins.sidebar.service.SideBarService;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.context.SimpleContainer;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;
import com.ultikits.ultitools.manager.ConfigManager;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UltiSideBar Main Class Tests")
class UltiSideBarTest {

    @Test
    @DisplayName("registerSelf should return true and init service")
    void registerSelf() throws Exception {
        UltiSideBar plugin = mock(UltiSideBar.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer context = mock(SimpleContainer.class);
        SideBarService service = mock(SideBarService.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getContext()).thenReturn(context);
        when(context.getBean(SideBarService.class)).thenReturn(service);
        when(plugin.i18n("sidebar_enabled")).thenReturn("sidebar_enabled");
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(service).init();
        verify(logger).info("sidebar_enabled");
    }

    @Test
    @DisplayName("onUnregister should call service shutdown exactly once (UltiKits/UltiSideBar#16)")
    void onUnregisterShutsDownService() throws Exception {
        UltiSideBar plugin = mock(UltiSideBar.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer context = mock(SimpleContainer.class);
        SideBarService service = mock(SideBarService.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getContext()).thenReturn(context);
        when(context.getBean(SideBarService.class)).thenReturn(service);
        when(plugin.i18n("sidebar_disabled")).thenReturn("sidebar_disabled");
        doCallRealMethod().when(plugin).onUnregister();

        plugin.onUnregister();

        verify(service, times(1)).shutdown();
        verify(logger).info("sidebar_disabled");
    }

    @Test
    @DisplayName("onReload should reload the service exactly once and never reload configuration itself (UltiKits/UltiSideBar#16)")
    void onReloadReloadsServiceWithoutReloadingConfigs() throws Exception {
        UltiSideBar plugin = mock(UltiSideBar.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer context = mock(SimpleContainer.class);
        SideBarService service = mock(SideBarService.class);
        ConfigManager configManager = mock(ConfigManager.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getContext()).thenReturn(context);
        when(context.getBean(SideBarService.class)).thenReturn(service);
        when(plugin.i18n("sidebar_reloaded")).thenReturn("sidebar_reloaded");
        doCallRealMethod().when(plugin).onReload();

        try (MockedStatic<UltiToolsPlugin> pluginStatic = mockStatic(UltiToolsPlugin.class)) {
            pluginStatic.when(UltiToolsPlugin::getConfigManager).thenReturn(configManager);

            plugin.onReload();

            // The framework's final reloadSelf() has already reloaded configuration before it
            // invokes this hook; the hook must not do it a second time.
            pluginStatic.verify(UltiToolsPlugin::getConfigManager, never());
            verifyNoInteractions(configManager);
            verify(service, times(1)).reload();
            verify(logger).info("sidebar_reloaded");
        }
    }

    @Test
    @DisplayName("registerSelf should handle null service gracefully")
    void registerSelfNullService() throws Exception {
        UltiSideBar plugin = mock(UltiSideBar.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer context = mock(SimpleContainer.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getContext()).thenReturn(context);
        when(context.getBean(SideBarService.class)).thenReturn(null);
        when(plugin.i18n("sidebar_enabled")).thenReturn("sidebar_enabled");
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(logger).info("sidebar_enabled");
    }

    @Test
    @DisplayName("onUnregister should handle null service gracefully")
    void onUnregisterNullService() throws Exception {
        UltiSideBar plugin = mock(UltiSideBar.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer context = mock(SimpleContainer.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getContext()).thenReturn(context);
        when(context.getBean(SideBarService.class)).thenReturn(null);
        when(plugin.i18n("sidebar_disabled")).thenReturn("sidebar_disabled");
        doCallRealMethod().when(plugin).onUnregister();

        plugin.onUnregister();

        verify(logger).info("sidebar_disabled");
    }

    @Test
    @DisplayName("onReload should handle null service gracefully and still never reload configuration itself")
    void onReloadNullService() throws Exception {
        UltiSideBar plugin = mock(UltiSideBar.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer context = mock(SimpleContainer.class);
        ConfigManager configManager = mock(ConfigManager.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getContext()).thenReturn(context);
        when(context.getBean(SideBarService.class)).thenReturn(null);
        when(plugin.i18n("sidebar_reloaded")).thenReturn("sidebar_reloaded");
        doCallRealMethod().when(plugin).onReload();

        try (MockedStatic<UltiToolsPlugin> pluginStatic = mockStatic(UltiToolsPlugin.class)) {
            pluginStatic.when(UltiToolsPlugin::getConfigManager).thenReturn(configManager);

            plugin.onReload();

            pluginStatic.verify(UltiToolsPlugin::getConfigManager, never());
            verifyNoInteractions(configManager);
            verify(logger).info("sidebar_reloaded");
        }
    }
}
