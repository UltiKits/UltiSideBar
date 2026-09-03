package com.ultikits.plugins.sidebar.service;

import com.ultikits.plugins.sidebar.UltiSideBar;
import com.ultikits.plugins.sidebar.UltiSideBarTestHelper;
import com.ultikits.plugins.sidebar.config.SideBarConfig;
import com.ultikits.plugins.sidebar.data.SideBarPreference;
import com.ultikits.ultitools.interfaces.DataOperator;
import com.ultikits.ultitools.interfaces.Query;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scoreboard.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SideBarService.
 */
@DisplayName("SideBarService Tests")
class SideBarServiceTest {

    private SideBarService service;
    private SideBarConfig config;
    @SuppressWarnings("unchecked")
    private DataOperator<SideBarPreference> dataOperator = mock(DataOperator.class);
    @SuppressWarnings("unchecked")
    private Query<SideBarPreference> query = mock(Query.class);

    private Player player;
    private UUID playerUuid;

    @BeforeEach
    void setUp() throws Exception {
        UltiSideBarTestHelper.setUp();

        config = mock(SideBarConfig.class);
        lenient().when(config.isEnabled()).thenReturn(true);
        lenient().when(config.isDefaultEnabled()).thenReturn(true);
        lenient().when(config.getTitle()).thenReturn("&6&lTest Server");
        lenient().when(config.getUpdateInterval()).thenReturn(20);
        lenient().when(config.getLines()).thenReturn(Arrays.asList("Line 1", "Line 2"));
        lenient().when(config.getWorldBlacklist()).thenReturn(Collections.emptyList());

        // Setup query chain mocking
        lenient().when(dataOperator.query()).thenReturn(query);
        lenient().when(query.where(anyString())).thenReturn(query);
        lenient().when(query.eq(any())).thenReturn(query);
        lenient().when(query.list()).thenReturn(Collections.emptyList());

        service = new SideBarService();

        // Inject dependencies via reflection
        UltiSideBarTestHelper.setField(service, "plugin", UltiSideBarTestHelper.getMockPlugin());
        UltiSideBarTestHelper.setField(service, "config", config);
        UltiSideBarTestHelper.setField(service, "dataOperator", dataOperator);

        playerUuid = UUID.randomUUID();
        player = UltiSideBarTestHelper.createMockPlayer("TestPlayer", playerUuid);
    }

    @AfterEach
    void tearDown() throws Exception {
        UltiSideBarTestHelper.tearDown();
    }

    // ==================== init ====================

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("Should initialize with PlaceholderAPI not available")
        void initWithoutPlaceholderAPI() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                service.init();

                verify(UltiSideBarTestHelper.getMockLogger()).warn("PlaceholderAPI not found! Variables will not work.");
            }
        }

        @Test
        @DisplayName("Should mark PlaceholderAPI available and skip the warning when the plugin is present")
        void initWithPlaceholderAPIAvailable() throws Exception {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(mock(Plugin.class));
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                service.init();

                verify(UltiSideBarTestHelper.getMockLogger(), never()).warn(anyString());

                java.lang.reflect.Field field = SideBarService.class.getDeclaredField("placeholderApiAvailable");
                field.setAccessible(true);
                assertThat(field.getBoolean(service)).isTrue();
            }
        }

        @Test
        @DisplayName("Should not enable an online player's sidebar when default-enabled is false, regardless of the database")
        void doesNotEnableOnlinePlayerWhenDefaultDisabled() {
            when(config.isDefaultEnabled()).thenReturn(false);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                service.init();

                // Short-circuits on isDefaultEnabled() before ever consulting the database.
                @SuppressWarnings("unchecked")
                DataOperator<SideBarPreference> pluginDataOp =
                    UltiSideBarTestHelper.getMockPlugin().getDataOperator(SideBarPreference.class);
                verify(pluginDataOp, never()).query();
                verify(player, never()).setScoreboard(any());
            }
        }

        @Test
        @DisplayName("Should not enable an online player's sidebar when default-enabled is true but the database says disabled")
        void doesNotEnableOnlinePlayerWhenDisabledInDatabase() {
            when(config.isDefaultEnabled()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                @SuppressWarnings("unchecked")
                DataOperator<SideBarPreference> pluginDataOp =
                    UltiSideBarTestHelper.getMockPlugin().getDataOperator(SideBarPreference.class);
                @SuppressWarnings("unchecked")
                Query<SideBarPreference> pluginQuery = mock(Query.class);
                when(pluginDataOp.query()).thenReturn(pluginQuery);
                when(pluginQuery.where(anyString())).thenReturn(pluginQuery);
                when(pluginQuery.eq(any())).thenReturn(pluginQuery);
                when(pluginQuery.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), false)));

                service.init();

                verify(player, never()).setScoreboard(any());
            }
        }

        @Test
        @DisplayName("Should initialize for online players")
        void initForOnlinePlayers() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                // init() reassigns dataOperator from the mock plugin, so stub
                // the DataOperator that the plugin returns
                @SuppressWarnings("unchecked")
                DataOperator<SideBarPreference> pluginDataOp =
                    UltiSideBarTestHelper.getMockPlugin().getDataOperator(SideBarPreference.class);
                @SuppressWarnings("unchecked")
                Query<SideBarPreference> pluginQuery = mock(Query.class);
                when(pluginDataOp.query()).thenReturn(pluginQuery);
                when(pluginQuery.where(anyString())).thenReturn(pluginQuery);
                when(pluginQuery.eq(any())).thenReturn(pluginQuery);
                when(pluginQuery.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard scoreboard = mock(Scoreboard.class);
                Objective objective = mock(Objective.class);
                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getNewScoreboard()).thenReturn(scoreboard);
                when(scoreboard.registerNewObjective(anyString(), anyString(), anyString())).thenReturn(objective);
                when(scoreboard.getEntries()).thenReturn(Collections.emptySet());

                service.init();

                // Should query for player preferences
                verify(pluginDataOp, atLeastOnce()).query();
            }
        }

        @Test
        @DisplayName("Should not schedule the update task when the sidebar is disabled")
        void doesNotScheduleUpdateTaskWhenDisabled() {
            when(config.isEnabled()).thenReturn(false);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                service.init();

                verify(scheduler, never()).runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong());
            }
        }

        @Test
        @DisplayName("Should schedule the update task on runTaskTimer when the sidebar is enabled")
        void schedulesUpdateTaskWhenEnabled() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                service.init();

                // config.getUpdateInterval() is stubbed to 20 in setUp().
                verify(scheduler).runTaskTimer(any(), any(Runnable.class), eq(0L), eq(20L));
            }
        }

        @Test
        @DisplayName("The scheduled update loop (private updateAllSidebars) calls updateSidebar only for players whose sidebar is enabled")
        void updateLoopOnlyTouchesEnabledPlayers() throws Exception {
            Player enabledPlayer = UltiSideBarTestHelper.createMockPlayer("Enabled", UUID.randomUUID());
            Player disabledPlayer = UltiSideBarTestHelper.createMockPlayer("Disabled", UUID.randomUUID());

            SideBarService spyService = spy(service);
            doReturn(true).when(spyService).isSidebarEnabled(enabledPlayer);
            doReturn(false).when(spyService).isSidebarEnabled(disabledPlayer);
            doNothing().when(spyService).updateSidebar(any());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Arrays.asList(enabledPlayer, disabledPlayer));

                java.lang.reflect.Method updateAllSidebars =
                    SideBarService.class.getDeclaredMethod("updateAllSidebars");
                updateAllSidebars.setAccessible(true);
                updateAllSidebars.invoke(spyService);
            }

            verify(spyService).updateSidebar(enabledPlayer);
            verify(spyService, never()).updateSidebar(disabledPlayer);
        }

        @Test
        @DisplayName("The config change listener clears the content cache and refreshes online players' sidebars")
        void configChangeListenerClearsCacheAndRefreshes() throws Exception {
            Map<UUID, List<String>> cache = new HashMap<>();
            cache.put(playerUuid, Arrays.asList("stale"));
            UltiSideBarTestHelper.setField(service, "contentCache", cache);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                @SuppressWarnings("unchecked")
                DataOperator<SideBarPreference> pluginDataOp =
                    UltiSideBarTestHelper.getMockPlugin().getDataOperator(SideBarPreference.class);
                @SuppressWarnings("unchecked")
                Query<SideBarPreference> pluginQuery = mock(Query.class);
                when(pluginDataOp.query()).thenReturn(pluginQuery);
                when(pluginQuery.where(anyString())).thenReturn(pluginQuery);
                when(pluginQuery.eq(any())).thenReturn(pluginQuery);
                when(pluginQuery.list()).thenReturn(Collections.emptyList());

                service.init();

                ArgumentCaptor<com.ultikits.ultitools.interfaces.ConfigChangeListener> listenerCaptor =
                    ArgumentCaptor.forClass(com.ultikits.ultitools.interfaces.ConfigChangeListener.class);
                verify(config).addChangeListener(listenerCaptor.capture());

                // Now put an online, sidebar-enabled player in place and fire the reload.
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));
                UltiSideBarTestHelper.setField(service, "config", config);
                when(config.isEnabled()).thenReturn(true);
                when(config.getWorldBlacklist()).thenReturn(Collections.emptyList());

                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard scoreboard = mock(Scoreboard.class);
                Objective objective = mock(Objective.class);
                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getNewScoreboard()).thenReturn(scoreboard);
                when(scoreboard.registerNewObjective(anyString(), anyString(), anyString())).thenReturn(objective);
                when(scoreboard.getEntries()).thenReturn(Collections.emptySet());
                // init() reassigned service's dataOperator field to the plugin-supplied one, so
                // the enabled-preference stub belongs on that query mock, not the original.
                when(pluginQuery.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

                listenerCaptor.getValue().onConfigReload(config);

                java.lang.reflect.Field cacheField = SideBarService.class.getDeclaredField("contentCache");
                cacheField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<UUID, List<String>> resultCache = (Map<UUID, List<String>>) cacheField.get(service);
                assertThat(resultCache).doesNotContainKey(playerUuid);
                // refreshAllSidebars() removed then re-created the scoreboard for the online player.
                verify(player).setScoreboard(scoreboard);
            }
        }

        @Test
        @DisplayName("refreshAllSidebars (via the config change listener) leaves a disabled online player's sidebar untouched")
        void configChangeListenerSkipsDisabledPlayer() throws Exception {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                PluginManager pluginManager = mock(PluginManager.class);
                when(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null);
                bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                @SuppressWarnings("unchecked")
                DataOperator<SideBarPreference> pluginDataOp =
                    UltiSideBarTestHelper.getMockPlugin().getDataOperator(SideBarPreference.class);
                @SuppressWarnings("unchecked")
                Query<SideBarPreference> pluginQuery = mock(Query.class);
                when(pluginDataOp.query()).thenReturn(pluginQuery);
                when(pluginQuery.where(anyString())).thenReturn(pluginQuery);
                when(pluginQuery.eq(any())).thenReturn(pluginQuery);
                when(pluginQuery.list()).thenReturn(Collections.emptyList());

                service.init();

                ArgumentCaptor<com.ultikits.ultitools.interfaces.ConfigChangeListener> listenerCaptor =
                    ArgumentCaptor.forClass(com.ultikits.ultitools.interfaces.ConfigChangeListener.class);
                verify(config).addChangeListener(listenerCaptor.capture());

                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));
                when(config.isEnabled()).thenReturn(true);
                when(config.getWorldBlacklist()).thenReturn(Collections.emptyList());
                when(pluginQuery.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), false)));

                listenerCaptor.getValue().onConfigReload(config);

                verify(player, never()).setScoreboard(any());
            }
        }
    }

    // ==================== shutdown ====================

    @Nested
    @DisplayName("shutdown")
    class Shutdown {

        @Test
        @DisplayName("Should cancel a running update task")
        void cancelsRunningUpdateTask() throws Exception {
            org.bukkit.scheduler.BukkitTask task = mock(org.bukkit.scheduler.BukkitTask.class);
            UltiSideBarTestHelper.setField(service, "updateTask", task);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

                service.shutdown();

                verify(task).cancel();
            }
        }

        @Test
        @DisplayName("Should clear all scoreboards on shutdown")
        void shutdownClearsScoreboards() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));

                service.shutdown();

                // Verify player scoreboard cleanup would be attempted
                bukkitMock.verify(Bukkit::getOnlinePlayers);
            }
        }
    }

    // ==================== enableSidebar ====================

    @Nested
    @DisplayName("enableSidebar")
    class EnableSidebar {

        @Test
        @DisplayName("Should not enable when config disabled")
        void configDisabled() {
            when(config.isEnabled()).thenReturn(false);

            service.enableSidebar(player);

            verify(dataOperator, never()).query();
            verify(dataOperator, never()).insert(any());
        }

        @Test
        @DisplayName("Should not enable in blacklisted world")
        void blacklistedWorld() {
            when(config.getWorldBlacklist()).thenReturn(Collections.singletonList("world"));
            when(query.list()).thenReturn(Collections.emptyList());

            service.enableSidebar(player);

            // Should update database but not show scoreboard
            verify(dataOperator).insert(any(SideBarPreference.class));
        }

        @Test
        @DisplayName("Should create scoreboard and set to player")
        void createsScoreboard() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard scoreboard = mock(Scoreboard.class);
                Objective objective = mock(Objective.class);
                Score score = mock(Score.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getNewScoreboard()).thenReturn(scoreboard);
                when(scoreboard.registerNewObjective(anyString(), anyString(), anyString())).thenReturn(objective);
                when(objective.getScore(anyString())).thenReturn(score);
                when(scoreboard.getEntries()).thenReturn(Collections.emptySet());

                when(query.list()).thenReturn(Collections.emptyList());

                service.enableSidebar(player);

                verify(player).setScoreboard(scoreboard);
                verify(objective).setDisplaySlot(DisplaySlot.SIDEBAR);
            }
        }

        @Test
        @DisplayName("Should update database with enabled state")
        void updatesDatabase() {
            when(query.list()).thenReturn(Collections.emptyList());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard scoreboard = mock(Scoreboard.class);
                Objective objective = mock(Objective.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getNewScoreboard()).thenReturn(scoreboard);
                when(scoreboard.registerNewObjective(anyString(), anyString(), anyString())).thenReturn(objective);
                when(scoreboard.getEntries()).thenReturn(Collections.emptySet());

                service.enableSidebar(player);

                ArgumentCaptor<SideBarPreference> captor = ArgumentCaptor.forClass(SideBarPreference.class);
                verify(dataOperator).insert(captor.capture());
                assertThat(captor.getValue().getPlayerUuid()).isEqualTo(playerUuid.toString());
                assertThat(captor.getValue().getEnabled()).isTrue();
            }
        }
    }

    // ==================== disableSidebar ====================

    @Nested
    @DisplayName("disableSidebar")
    class DisableSidebar {

        @Test
        @DisplayName("Should update database and remove scoreboard")
        void disables() {
            when(query.list()).thenReturn(Collections.emptyList());

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard mainScoreboard = mock(Scoreboard.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getMainScoreboard()).thenReturn(mainScoreboard);

                service.disableSidebar(player);

                ArgumentCaptor<SideBarPreference> captor = ArgumentCaptor.forClass(SideBarPreference.class);
                verify(dataOperator).insert(captor.capture());
                assertThat(captor.getValue().getEnabled()).isFalse();
                verify(player).setScoreboard(mainScoreboard);
            }
        }

        @Test
        @DisplayName("Should update canonical duplicate preference row by id")
        void updatesCanonicalDuplicatePreferenceRow() {
            SideBarPreference laterPreference = preference("pref-b", true);
            SideBarPreference canonicalPreference = preference("pref-a", true);
            when(query.list()).thenReturn(Arrays.asList(laterPreference, canonicalPreference));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard mainScoreboard = mock(Scoreboard.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getMainScoreboard()).thenReturn(mainScoreboard);

                service.disableSidebar(player);

                verify(dataOperator).update("enabled", false, "pref-a");
                verify(dataOperator, never()).update("enabled", false, "pref-b");
                verify(player).setScoreboard(mainScoreboard);
            }
        }

        @Test
        @DisplayName("Should not call update when existing rows are non-empty but none is selectable")
        void doesNotUpdateWhenNoCanonicalRowSelectable() {
            when(query.list()).thenReturn(Arrays.asList((SideBarPreference) null));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard mainScoreboard = mock(Scoreboard.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getMainScoreboard()).thenReturn(mainScoreboard);

                service.disableSidebar(player);

                verify(dataOperator, never()).update(anyString(), any(), any());
                verify(dataOperator, never()).insert(any());
            }
        }
    }

    // ==================== toggleSidebar ====================

    @Nested
    @DisplayName("toggleSidebar")
    class ToggleSidebar {

        @Test
        @DisplayName("Should toggle from enabled to disabled")
        void toggleOff() {
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard mainScoreboard = mock(Scoreboard.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getMainScoreboard()).thenReturn(mainScoreboard);

                boolean result = service.toggleSidebar(player);

                assertThat(result).isFalse();
            }
        }

        @Test
        @DisplayName("Should toggle from disabled to enabled")
        void toggleOn() {
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), false)));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard scoreboard = mock(Scoreboard.class);
                Objective objective = mock(Objective.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getNewScoreboard()).thenReturn(scoreboard);
                when(scoreboard.registerNewObjective(anyString(), anyString(), anyString())).thenReturn(objective);
                when(scoreboard.getEntries()).thenReturn(Collections.emptySet());

                boolean result = service.toggleSidebar(player);

                assertThat(result).isTrue();
            }
        }

        @Test
        @DisplayName("Should enable by default when no preference exists")
        void toggleDefaultEnabled() {
            when(query.list()).thenReturn(Collections.emptyList());
            when(config.isDefaultEnabled()).thenReturn(true);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard mainScoreboard = mock(Scoreboard.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getMainScoreboard()).thenReturn(mainScoreboard);

                boolean result = service.toggleSidebar(player);

                assertThat(result).isFalse();
            }
        }
    }

    // ==================== isSidebarEnabled ====================

    @Nested
    @DisplayName("isSidebarEnabled")
    class IsSidebarEnabled {

        @Test
        @DisplayName("Should return false when config disabled")
        void configDisabled() {
            when(config.isEnabled()).thenReturn(false);

            boolean result = service.isSidebarEnabled(player);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false in blacklisted world")
        void blacklistedWorld() {
            when(config.getWorldBlacklist()).thenReturn(Collections.singletonList("world"));
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

            boolean result = service.isSidebarEnabled(player);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when enabled in database")
        void enabledInDb() {
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

            boolean result = service.isSidebarEnabled(player);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when disabled in database")
        void disabledInDb() {
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), false)));

            boolean result = service.isSidebarEnabled(player);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should read canonical duplicate preference row by id")
        void readsCanonicalDuplicatePreferenceRow() {
            SideBarPreference laterPreference = preference("pref-b", false);
            SideBarPreference canonicalPreference = preference("pref-a", true);
            when(query.list()).thenReturn(Arrays.asList(laterPreference, canonicalPreference));

            boolean result = service.isSidebarEnabled(player);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should use default enabled when no preference exists")
        void defaultEnabled() {
            when(query.list()).thenReturn(Collections.emptyList());
            when(config.isDefaultEnabled()).thenReturn(true);

            boolean result = service.isSidebarEnabled(player);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should use default enabled when canonical selection yields no row (all rows null)")
        void defaultEnabledWhenCanonicalSelectionIsNull() {
            // A non-empty list that nonetheless filters down to nothing selectable exercises the
            // "pref == null" branch distinctly from the already-covered "prefs.isEmpty()" branch.
            when(query.list()).thenReturn(Arrays.asList((SideBarPreference) null));
            when(config.isDefaultEnabled()).thenReturn(false);

            boolean result = service.isSidebarEnabled(player);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should use default enabled when the canonical row's own enabled field is null")
        void defaultEnabledWhenRowFieldIsNull() {
            // Distinct from "no row" and "no selectable row": a real row exists, but its own
            // `enabled` field was never set.
            SideBarPreference rowWithNullField = new SideBarPreference(playerUuid.toString(), null);
            when(query.list()).thenReturn(Arrays.asList(rowWithNullField));
            when(config.isDefaultEnabled()).thenReturn(true);

            boolean result = service.isSidebarEnabled(player);

            assertThat(result).isTrue();
        }
    }

    // ==================== updateSidebar ====================

    @Nested
    @DisplayName("updateSidebar")
    class UpdateSidebar {

        @Test
        @DisplayName("Should do nothing when scoreboard not found")
        void noScoreboard() {
            service.updateSidebar(player);

            // No exception should be thrown
        }

        @Test
        @DisplayName("Should skip update when content unchanged")
        void skipWhenUnchanged() throws Exception {
            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);
            when(scoreboard.getEntries()).thenReturn(Collections.emptySet());

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            Map<UUID, List<String>> cache = new HashMap<>();
            cache.put(playerUuid, Arrays.asList("Line 1", "Line 2"));
            UltiSideBarTestHelper.setField(service, "contentCache", cache);

            service.updateSidebar(player);

            // Should not clear scores since content is same
            verify(scoreboard, never()).resetScores(anyString());
        }

        @Test
        @DisplayName("Should do nothing when the tracked scoreboard has no 'sidebar' objective")
        void noObjective() throws Exception {
            Scoreboard scoreboard = mock(Scoreboard.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(null);

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            assertThatCode(() -> service.updateSidebar(player)).doesNotThrowAnyException();
            verify(scoreboard, never()).getEntries();
        }

        @Test
        @DisplayName("Should return without touching the scoreboard when configured lines is null")
        void nullLines() throws Exception {
            when(config.getLines()).thenReturn(null);

            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            assertThatCode(() -> service.updateSidebar(player)).doesNotThrowAnyException();
            verify(scoreboard, never()).resetScores(anyString());
            verify(objective, never()).getScore(anyString());
        }

        @Test
        @DisplayName("Should return without touching the scoreboard when configured lines are empty")
        void emptyLines() throws Exception {
            when(config.getLines()).thenReturn(Collections.emptyList());

            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            service.updateSidebar(player);

            verify(scoreboard, never()).resetScores(anyString());
            verify(objective, never()).getScore(anyString());
        }

        @Test
        @DisplayName("Should swallow an exception from setting the objective's display name and still update entries")
        void titleUpdateExceptionIsSwallowed() throws Exception {
            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            Score score = mock(Score.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);
            when(scoreboard.getEntries()).thenReturn(Collections.emptySet());
            when(objective.getScore(anyString())).thenReturn(score);
            doThrow(new IllegalArgumentException("title too long"))
                .when(objective).setDisplayName(anyString());

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            assertThatCode(() -> service.updateSidebar(player)).doesNotThrowAnyException();

            // Line content is still rebuilt despite the title update failing.
            verify(objective, atLeastOnce()).getScore(anyString());
        }

        @Test
        @DisplayName("Should append a unique-entry marker when two configured lines render identically")
        void deduplicatesIdenticalRenderedLines() throws Exception {
            when(config.getLines()).thenReturn(Arrays.asList("Same", "Same"));

            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            Score score = mock(Score.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);
            when(scoreboard.getEntries()).thenReturn(Collections.emptySet());
            when(objective.getScore(anyString())).thenReturn(score);

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            service.updateSidebar(player);

            ArgumentCaptor<String> entryCaptor = ArgumentCaptor.forClass(String.class);
            verify(objective, times(2)).getScore(entryCaptor.capture());
            List<String> entries = entryCaptor.getAllValues();
            // The two rendered entries must differ (the second gets a RESET-code suffix appended);
            // identical entries would collapse into one scoreboard line and silently drop content.
            assertThat(entries.get(0)).isNotEqualTo(entries.get(1));
            assertThat(entries.get(1)).startsWith(entries.get(0));
        }

        @Test
        @DisplayName("Should truncate a rendered entry longer than 40 characters")
        void truncatesLongEntries() throws Exception {
            String longLine = "This line is deliberately far longer than forty characters";
            assertThat(longLine.length()).isGreaterThan(40);
            when(config.getLines()).thenReturn(Arrays.asList(longLine));

            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            Score score = mock(Score.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);
            when(scoreboard.getEntries()).thenReturn(Collections.emptySet());
            when(objective.getScore(anyString())).thenReturn(score);

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            service.updateSidebar(player);

            ArgumentCaptor<String> entryCaptor = ArgumentCaptor.forClass(String.class);
            verify(objective).getScore(entryCaptor.capture());
            assertThat(entryCaptor.getValue()).hasSize(40);
        }

        @Test
        @DisplayName("Should reset stale entries and rebuild the scoreboard when content actually changed")
        void rebuildsWhenContentChanged() throws Exception {
            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            Score score = mock(Score.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);
            when(scoreboard.getEntries()).thenReturn(new HashSet<>(Arrays.asList("Old entry")));
            when(objective.getScore(anyString())).thenReturn(score);

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            Map<UUID, List<String>> cache = new HashMap<>();
            cache.put(playerUuid, Arrays.asList("Something entirely different"));
            UltiSideBarTestHelper.setField(service, "contentCache", cache);

            service.updateSidebar(player);

            verify(scoreboard).resetScores("Old entry");
            verify(objective, times(2)).getScore(anyString()); // "Line 1", "Line 2" from setUp()'s config
            verify(score, times(2)).setScore(anyInt());
        }

        @Test
        @DisplayName("Should swallow an exception from setting one entry's score and still process the rest")
        void perEntryScoreExceptionIsSwallowed() throws Exception {
            when(config.getLines()).thenReturn(Arrays.asList("First", "Second"));

            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            Score badScore = mock(Score.class);
            Score goodScore = mock(Score.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);
            when(scoreboard.getEntries()).thenReturn(Collections.emptySet());
            when(objective.getScore("First")).thenReturn(badScore);
            when(objective.getScore("Second")).thenReturn(goodScore);
            doThrow(new IllegalStateException("scoreboard entry rejected")).when(badScore).setScore(anyInt());

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            assertThatCode(() -> service.updateSidebar(player)).doesNotThrowAnyException();

            verify(goodScore).setScore(anyInt());
        }

        @Test
        @DisplayName("Should delegate line text through PlaceholderAPI when it is available")
        void delegatesToPlaceholderApiWhenAvailable() throws Exception {
            UltiSideBarTestHelper.setField(service, "placeholderApiAvailable", true);
            when(config.getLines()).thenReturn(Arrays.asList("%player_name%"));

            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            Score score = mock(Score.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);
            when(scoreboard.getEntries()).thenReturn(Collections.emptySet());
            when(objective.getScore(anyString())).thenReturn(score);

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            try (MockedStatic<me.clip.placeholderapi.PlaceholderAPI> papiMock =
                    mockStatic(me.clip.placeholderapi.PlaceholderAPI.class)) {
                papiMock.when(() -> me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%player_name%"))
                    .thenReturn("TestPlayer");

                service.updateSidebar(player);

                ArgumentCaptor<String> entryCaptor = ArgumentCaptor.forClass(String.class);
                verify(objective).getScore(entryCaptor.capture());
                assertThat(entryCaptor.getValue()).isEqualTo("TestPlayer");
            }
        }

        @Test
        @DisplayName("Should fall back to the raw text when PlaceholderAPI itself throws")
        void fallsBackToRawTextWhenPlaceholderApiThrows() throws Exception {
            UltiSideBarTestHelper.setField(service, "placeholderApiAvailable", true);
            when(config.getLines()).thenReturn(Arrays.asList("%broken_placeholder%"));

            Scoreboard scoreboard = mock(Scoreboard.class);
            Objective objective = mock(Objective.class);
            Score score = mock(Score.class);
            when(scoreboard.getObjective("sidebar")).thenReturn(objective);
            when(scoreboard.getEntries()).thenReturn(Collections.emptySet());
            when(objective.getScore(anyString())).thenReturn(score);

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, scoreboard);
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            try (MockedStatic<me.clip.placeholderapi.PlaceholderAPI> papiMock =
                    mockStatic(me.clip.placeholderapi.PlaceholderAPI.class)) {
                papiMock.when(() -> me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%broken_placeholder%"))
                    .thenThrow(new RuntimeException("expansion crashed"));

                service.updateSidebar(player);

                ArgumentCaptor<String> entryCaptor = ArgumentCaptor.forClass(String.class);
                verify(objective).getScore(entryCaptor.capture());
                assertThat(entryCaptor.getValue()).isEqualTo("%broken_placeholder%");
            }
        }
    }

    // ==================== clearCache ====================

    @Nested
    @DisplayName("clearCache")
    class ClearCache {

        @Test
        @DisplayName("Should clear content cache")
        void clearsCache() throws Exception {
            Map<UUID, List<String>> cache = new HashMap<>();
            cache.put(playerUuid, Arrays.asList("Line 1"));
            UltiSideBarTestHelper.setField(service, "contentCache", cache);

            service.clearCache();

            // Verify by reading the field back via reflection on SideBarService
            java.lang.reflect.Field cacheField = SideBarService.class.getDeclaredField("contentCache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, List<String>> resultCache = (Map<UUID, List<String>>) cacheField.get(service);
            assertThat(resultCache).isEmpty();
        }
    }

    // ==================== reload ====================

    @Nested
    @DisplayName("reload")
    class Reload {

        @Test
        @DisplayName("Should shutdown and reinitialize")
        void reloads() {
            SideBarService spyService = spy(service);

            doNothing().when(spyService).shutdown();
            doNothing().when(spyService).init();

            spyService.reload();

            verify(spyService).shutdown();
            verify(spyService).init();
        }
    }

    // ==================== removeSidebar ====================

    @Nested
    @DisplayName("removeSidebar")
    class RemoveSidebar {

        @Test
        @DisplayName("Should reset to main scoreboard")
        void resetsScoreboard() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard mainScoreboard = mock(Scoreboard.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getMainScoreboard()).thenReturn(mainScoreboard);

                service.removeSidebar(player);

                verify(player).setScoreboard(mainScoreboard);
            }
        }

        @Test
        @DisplayName("Should handle null scoreboard manager")
        void nullScoreboardManager() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(null);

                service.removeSidebar(player);

                // Should not throw exception
                verify(player, never()).setScoreboard(any());
            }
        }
    }

    // ==================== onPlayerJoin ====================

    @Nested
    @DisplayName("onPlayerJoin")
    class OnPlayerJoin {

        @Test
        @DisplayName("Should schedule sidebar enable for player")
        void schedulesEnable() {
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                service.onPlayerJoin(player);

                verify(scheduler).runTaskLater(any(), any(Runnable.class), eq(10L));
            }
        }

        @Test
        @DisplayName("Should not schedule when disabled in database")
        void doesNotScheduleWhenDisabled() {
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), false)));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                service.onPlayerJoin(player);

                verify(scheduler, never()).runTaskLater(any(), any(Runnable.class), anyLong());
            }
        }

        @Test
        @DisplayName("The scheduled delayed task actually enables the sidebar when it runs")
        void scheduledTaskActuallyEnablesSidebar() {
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard scoreboard = mock(Scoreboard.class);
                Objective objective = mock(Objective.class);
                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getNewScoreboard()).thenReturn(scoreboard);
                when(scoreboard.registerNewObjective(anyString(), anyString(), anyString())).thenReturn(objective);
                when(scoreboard.getEntries()).thenReturn(Collections.emptySet());

                service.onPlayerJoin(player);

                ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
                verify(scheduler).runTaskLater(any(), taskCaptor.capture(), eq(10L));

                taskCaptor.getValue().run();

                verify(player).setScoreboard(scoreboard);
            }
        }
    }

    // ==================== onPlayerQuit ====================

    @Nested
    @DisplayName("onPlayerQuit")
    class OnPlayerQuit {

        @Test
        @DisplayName("Should clean up player data")
        void cleansUpData() throws Exception {
            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, mock(Scoreboard.class));
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            Map<UUID, List<String>> cache = new HashMap<>();
            cache.put(playerUuid, Arrays.asList("test"));
            UltiSideBarTestHelper.setField(service, "contentCache", cache);

            service.onPlayerQuit(player);

            assertThat(scoreboards).doesNotContainKey(playerUuid);
            assertThat(cache).doesNotContainKey(playerUuid);
        }
    }

    // ==================== onWorldChange ====================

    @Nested
    @DisplayName("onWorldChange")
    class OnWorldChange {

        @Test
        @DisplayName("Should remove sidebar in blacklisted world")
        void removesInBlacklistedWorld() {
            when(config.getWorldBlacklist()).thenReturn(Collections.singletonList("world"));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard mainScoreboard = mock(Scoreboard.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getMainScoreboard()).thenReturn(mainScoreboard);

                service.onWorldChange(player);

                verify(player).setScoreboard(mainScoreboard);
            }
        }

        @Test
        @DisplayName("Should enable sidebar in allowed world")
        void enablesInAllowedWorld() throws Exception {
            when(config.getWorldBlacklist()).thenReturn(Collections.emptyList());
            when(config.isEnabled()).thenReturn(true);
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
                Scoreboard scoreboard = mock(Scoreboard.class);
                Objective objective = mock(Objective.class);

                bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);
                when(scoreboardManager.getNewScoreboard()).thenReturn(scoreboard);
                when(scoreboard.registerNewObjective(anyString(), anyString(), anyString())).thenReturn(objective);
                when(scoreboard.getEntries()).thenReturn(Collections.emptySet());

                service.onWorldChange(player);

                verify(player).setScoreboard(scoreboard);
            }
        }

        @Test
        @DisplayName("Should do nothing when the world is allowed but the sidebar isn't enabled")
        void doesNothingWhenNotEnabledInAllowedWorld() {
            // config.isEnabled() stays true here deliberately: enableSidebar() has its own
            // "!config.isEnabled()" guard, so a config-disabled scenario would pass even if
            // onWorldChange's own isSidebarEnabled(player) gate were removed entirely. The
            // player's own database opt-out is what this specific decision protects, and it can
            // only be proven by disabling in the database while config itself stays enabled.
            when(config.getWorldBlacklist()).thenReturn(Collections.emptyList());
            when(config.isEnabled()).thenReturn(true);
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), false)));

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                service.onWorldChange(player);

                verify(player, never()).setScoreboard(any());
            }
        }

        @Test
        @DisplayName("Should do nothing when the world is allowed, the sidebar is enabled, but it is already showing")
        void doesNothingWhenAlreadyShowingInAllowedWorld() throws Exception {
            when(config.getWorldBlacklist()).thenReturn(Collections.emptyList());
            when(config.isEnabled()).thenReturn(true);
            when(query.list()).thenReturn(Arrays.asList(new SideBarPreference(playerUuid.toString(), true)));

            Map<UUID, Scoreboard> scoreboards = new HashMap<>();
            scoreboards.put(playerUuid, mock(Scoreboard.class));
            UltiSideBarTestHelper.setField(service, "playerScoreboards", scoreboards);

            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                service.onWorldChange(player);

                // enableSidebar() would call Bukkit.getScoreboardManager() to build a fresh one;
                // it must not be reached since the player already has a tracked scoreboard.
                bukkitMock.verify(Bukkit::getScoreboardManager, never());
                verify(player, never()).setScoreboard(any());
            }
        }
    }

    private SideBarPreference preference(String id, boolean enabled) {
        SideBarPreference preference = new SideBarPreference(playerUuid.toString(), enabled);
        preference.setId(id);
        return preference;
    }
}
