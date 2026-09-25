package com.ultikits.plugins.sidebar.config;

import com.ultikits.plugins.sidebar.i18n.CatalogueText;
import com.ultikits.plugins.sidebar.service.SideBarService;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The sidebar title left blank in {@code config/sidebar.yml} reads the language file's text in the
 * server's language; the title every earlier version shipped is recognised in an upgraded file and
 * blanked, so the language file takes over; any other value is the operator's and is kept (maintainer
 * ruling 2026-09-24 (d)).
 */
@DisplayName("The sidebar title follows the language setting when left blank")
class SideBarTitleLanguageTest {

    /** The title every earlier version of this module shipped. */
    private static final String SHIPPED_TITLE = "&6&l我的服务器";

    @TempDir
    Path tempDir;

    private PluginLogger logger;

    @Test
    @DisplayName("A fresh install shows the language file's title in English and in Chinese")
    void freshInstallShowsTheLanguageFileTitle() throws Exception {
        for (String code : new String[] {"en", "zh"}) {
            SideBarConfig config = load(code, null);

            assertThat(config.getTitle())
                    .as("title under language: %s", code)
                    .isEqualTo(CatalogueText.text(code, "sidebar_default_title"));
        }
    }

    @Test
    @DisplayName("An upgraded file holding the old shipped title is blanked and saved, then shows the language file's title")
    void oldShippedTitleIsBlankedAndSaved() throws Exception {
        for (String code : new String[] {"en", "zh"}) {
            SideBarConfig config = load(code, SHIPPED_TITLE);

            startService(config);

            assertThat(YamlConfiguration.loadConfiguration(configFile()).getString("title"))
                    .as("title saved in sidebar.yml under language: %s", code)
                    .isEmpty();
            assertThat(config.getTitle())
                    .as("title shown under language: %s", code)
                    .isEqualTo(CatalogueText.text(code, "sidebar_default_title"));
        }
    }

    @Test
    @DisplayName("A customised title is kept unchanged, in the file and on screen")
    void customisedTitleIsKept() throws Exception {
        SideBarConfig config = load("en", "&a&lOperator Title");

        startService(config);

        assertThat(YamlConfiguration.loadConfiguration(configFile()).getString("title"))
                .isEqualTo("&a&lOperator Title");
        assertThat(config.getTitle()).isEqualTo("&a&lOperator Title");
    }

    @Test
    @DisplayName("A blank title is not rewritten again on the next start")
    void blankTitleIsNotRewrittenAgain() throws Exception {
        SideBarConfig config = load("en", "");
        byte[] before = Files.readAllBytes(configFile().toPath());

        startService(config);

        assertThat(Files.readAllBytes(configFile().toPath())).isEqualTo(before);
        assertThat(config.getTitle()).isEqualTo(CatalogueText.text("en", "sidebar_default_title"));
    }

    @Test
    @DisplayName("A file that cannot be saved after the old title is blanked is reported in the server's language")
    void saveFailureIsLoggedInTheServerLanguage() throws Exception {
        SideBarConfig config = spy(load("zh", SHIPPED_TITLE));
        doThrow(new IOException("disk full")).when(config).save();

        startService(config);

        // Computed before verify(): a lookup that throws inside verify() leaves Mockito mid-verification.
        String expected = CatalogueText.text("zh", "sidebar_log_defaults_save_failed").replace("{ERROR}", "disk full");
        verify(logger).warn(expected);
    }

    private File configFile() {
        return new File(tempDir.toFile(), "config/sidebar.yml");
    }

    /** Writes {@code title} (absent when null) to a fresh sidebar.yml and loads it for language {@code code}. */
    private SideBarConfig load(String code, String title) throws Exception {
        File file = configFile();
        Files.createDirectories(file.getParentFile().toPath());
        Files.deleteIfExists(file.toPath());
        if (title != null) {
            YamlConfiguration persisted = new YamlConfiguration();
            persisted.set("title", title);
            persisted.save(file);
        }
        SideBarConfig config = new SideBarConfig();
        config.init(pluginIn(code));
        return config;
    }

    /**
     * A plugin double for language {@code code}: its configuration folder is the temporary directory
     * and its {@code i18n} answers from the module's real catalogue for that language. The two folder
     * methods are {@code protected final} outside this package, so they are answered by reflection.
     */
    private UltiToolsPlugin pluginIn(String code) {
        logger = mock(PluginLogger.class);
        final org.mockito.stubbing.Answer<String> text = CatalogueText.answer(code);
        return Mockito.mock(UltiToolsPlugin.class, invocation -> {
            String name = invocation.getMethod().getName();
            if ("getConfigFolder".equals(name)) {
                return tempDir.toString();
            }
            if ("getConfigFile".equals(name)) {
                return new File(tempDir.toFile(), invocation.<String>getArgument(0));
            }
            if ("i18n".equals(name)) {
                return text.answer(invocation);
            }
            if ("getLogger".equals(name)) {
                return logger;
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    /** Runs the sidebar service's start-up, the path that recognises old shipped defaults on enable and reload. */
    private void startService(SideBarConfig config) throws Exception {
        SideBarService service = new SideBarService();
        set(service, "plugin", pluginOf(config));
        set(service, "config", config);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            PluginManager plugins = mock(PluginManager.class);
            when(plugins.getPlugin("PlaceholderAPI")).thenReturn(mock(org.bukkit.plugin.Plugin.class));
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());
            bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));
            service.init();
        }
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static UltiToolsPlugin pluginOf(SideBarConfig config) throws Exception {
        Field f = com.ultikits.ultitools.abstracts.AbstractConfigEntity.class.getDeclaredField("ultiToolsPlugin");
        f.setAccessible(true);
        return (UltiToolsPlugin) f.get(config);
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
