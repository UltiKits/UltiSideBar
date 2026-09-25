package com.ultikits.plugins.sidebar.config;

import com.ultikits.plugins.sidebar.i18n.CatalogueText;
import com.ultikits.plugins.sidebar.service.SideBarService;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.ConfigEntry;
import com.ultikits.ultitools.annotations.config.NotEmpty;
import com.ultikits.ultitools.annotations.config.Size;
import com.ultikits.ultitools.interfaces.ConfigChangeListener;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code config/sidebar.yml} holds the sidebar's title and lines in the server's language, and the
 * sidebar shows what the file holds (maintainer decision 2026-09-25; UltiSideBar#24).
 * A value that is still built-in text — any language's text from this jar, or a default an earlier
 * version shipped — follows {@code language} at enable and on reload, in both directions; anything else
 * is the operator's and is kept byte for byte. Every case runs the framework's real
 * {@code AbstractConfigEntity#init} on a temporary folder and answers {@code i18n} from the module's
 * real catalogues.
 */
@DisplayName("sidebar.yml holds the sidebar text in the server's language")
class SideBarConfigTextTest {

    /** The title every earlier version shipped. */
    private static final String SHIPPED_TITLE = "&6&l我的服务器";

    /** The lines the first version shipped (invalid %world_name%, 12-hour time). */
    private static final List<String> SHIPPED_LINES_1 = Arrays.asList(
            "&7欢迎, &f%player_name%", "", "&e在线人数: &f%server_online%/%server_max_players%",
            "&e世界: &f%world_name%", "", "&e金币: &f%vault_eco_balance_formatted%", "&ePing: &f%player_ping%ms",
            "", "&7服务器时间", "&f%server_time_hh:mm:ss%", "", "&6play.example.com");

    /** The lines after UltiSideBar#13's world-line fix (still 12-hour time). */
    private static final List<String> SHIPPED_LINES_2 = Arrays.asList(
            "&7欢迎, &f%player_name%", "", "&e在线人数: &f%server_online%/%server_max_players%",
            "&e世界: &f%player_world%", "", "&e金币: &f%vault_eco_balance_formatted%", "&ePing: &f%player_ping%ms",
            "", "&7服务器时间", "&f%server_time_hh:mm:ss%", "", "&6play.example.com");

    /** The last shipped lines, which are also the Chinese text this build writes. */
    private static final List<String> SHIPPED_LINES_3 = Arrays.asList(
            "&7欢迎, &f%player_name%", "", "&e在线人数: &f%server_online%/%server_max_players%",
            "&e世界: &f%player_world%", "", "&e金币: &f%vault_eco_balance_formatted%", "&ePing: &f%player_ping%ms",
            "", "&7服务器时间", "&f%server_time_HH:mm:ss%", "", "&6play.example.com");

    /** The English text this build writes under {@code language: en}. */
    private static final String EN_TITLE = "&6&lMy Server";
    private static final List<String> EN_LINES = Arrays.asList(
            "&7Welcome, &f%player_name%", "", "&eOnline: &f%server_online%/%server_max_players%",
            "&eWorld: &f%player_world%", "", "&eBalance: &f%vault_eco_balance_formatted%", "&ePing: &f%player_ping%ms",
            "", "&7Server time", "&f%server_time_HH:mm:ss%", "", "&6play.example.com");

    private static final String[] LANGUAGES = {"en", "zh"};

    @TempDir
    Path tempDir;

    /** The language the plugin double answers in; switched by the language-change cases. */
    private final String[] language = {"en"};

    private final PluginLogger logger = mock(PluginLogger.class);

    /** Catalogue texts an operator changed in the language file on disk, answered before the real catalogue. */
    private final Map<String, String> diskOverrides = new LinkedHashMap<>();

    private final UltiToolsPlugin plugin = pluginDouble();

    // ---- the texts, from the real catalogues ----

    private static String title(String code) {
        return CatalogueText.text(code, "sidebar_default_title");
    }

    private static List<String> lines(String code) {
        return Arrays.asList(CatalogueText.text(code, "sidebar_default_lines").split("\n", -1));
    }

    @Test
    @DisplayName("the catalogues hold exactly the English text, and the last shipped defaults as the Chinese text")
    void catalogueTexts() {
        assertThat(title("en")).isEqualTo(EN_TITLE);
        assertThat(lines("en")).containsExactlyElementsOf(EN_LINES);
        assertThat(title("zh")).isEqualTo(SHIPPED_TITLE);
        assertThat(lines("zh")).containsExactlyElementsOf(SHIPPED_LINES_3);
    }

    @Test
    @DisplayName("fresh start under en: the file holds the English title and lines, and the getters return the file's values")
    void freshStartEnglish() throws Exception {
        language[0] = "en";
        SideBarConfig config = spy(load());

        start(config);

        YamlConfiguration disk = onDisk();
        assertThat(disk.getString("title")).isEqualTo(EN_TITLE);
        assertThat(disk.getStringList("lines")).containsExactlyElementsOf(EN_LINES);
        assertThat(config.getTitle()).isEqualTo(disk.getString("title"));
        assertThat(config.getLines()).containsExactlyElementsOf(disk.getStringList("lines"));
        verify(config, times(1)).save();
    }

    @Test
    @DisplayName("fresh start under zh: the file holds the Chinese title and lines, and the module writes nothing")
    void freshStartChinese() throws Exception {
        language[0] = "zh";
        SideBarConfig config = spy(load());
        byte[] afterFramework = bytes();

        start(config);

        assertThat(onDisk().getString("title")).isEqualTo(SHIPPED_TITLE);
        assertThat(onDisk().getStringList("lines")).containsExactlyElementsOf(SHIPPED_LINES_3);
        assertThat(config.getTitle()).isEqualTo(SHIPPED_TITLE);
        verify(config, never()).save();
        assertThat(bytes()).isEqualTo(afterFramework);
    }

    @Test
    @DisplayName("every built-in title and list in the file is replaced with the current language's text and saved, under en and under zh")
    void everyTrackedValueFollowsTheLanguage() throws Exception {
        List<String> titles = Arrays.asList(SHIPPED_TITLE, title("en"), title("zh"));
        List<List<String>> lists = Arrays.asList(SHIPPED_LINES_1, SHIPPED_LINES_2, SHIPPED_LINES_3, lines("en"), lines("zh"));
        for (String code : LANGUAGES) {
            for (String t : titles) {
                for (List<String> l : lists) {
                    language[0] = code;
                    write(t, l);
                    SideBarConfig config = spy(load());
                    boolean alreadyCurrent = t.equals(title(code)) && l.equals(lines(code));

                    start(config);

                    String what = "language " + code + ", file title " + t + ", lines " + l.get(3) + " / " + l.get(9)
                            + " / " + l.get(0);
                    assertThat(onDisk().getString("title")).as(what).isEqualTo(title(code));
                    assertThat(onDisk().getStringList("lines")).as(what).containsExactlyElementsOf(lines(code));
                    assertThat(config.getTitle()).as(what).isEqualTo(title(code));
                    assertThat(config.getLines()).as(what).containsExactlyElementsOf(lines(code));
                    verify(config, times(alreadyCurrent ? 0 : 1)).save();
                }
            }
        }
    }

    @Test
    @DisplayName("an upgraded file holding the shipped title and the first shipped lines reads exactly the English text under en, and the Chinese text under zh")
    void upgradedFileFollowsTheLanguage() throws Exception {
        language[0] = "en";
        write(SHIPPED_TITLE, SHIPPED_LINES_1);
        SideBarConfig config = spy(load());

        start(config);

        assertThat(onDisk().getString("title")).isEqualTo(EN_TITLE);
        assertThat(onDisk().getStringList("lines")).containsExactlyElementsOf(EN_LINES);
        assertThat(config.getTitle()).isEqualTo(EN_TITLE);
        verify(config, times(1)).save();

        language[0] = "zh";
        write(SHIPPED_TITLE, SHIPPED_LINES_1);
        SideBarConfig zh = load();
        start(zh);

        assertThat(onDisk().getString("title")).isEqualTo(SHIPPED_TITLE);
        assertThat(onDisk().getStringList("lines")).containsExactlyElementsOf(SHIPPED_LINES_3);
    }

    @Test
    @DisplayName("an over-long title or too many lines in the extracted language file never reach sidebar.yml; the jar's text is written and the file still loads")
    void aTextBreakingTheLimitsIsNotWritten() throws Exception {
        language[0] = "en";
        diskOverrides.put("sidebar_default_title", "&6&lA server name far longer than thirty-two characters");
        StringBuilder sixteen = new StringBuilder("line 1");
        for (int i = 2; i <= 16; i++) {
            sixteen.append("\n").append("line ").append(i);
        }
        diskOverrides.put("sidebar_default_lines", sixteen.toString());
        write(SHIPPED_TITLE, SHIPPED_LINES_3);
        SideBarConfig config = load();

        start(config);

        assertThat(config.getTitle()).isEqualTo(EN_TITLE);
        assertThat(config.getLines()).containsExactlyElementsOf(EN_LINES);
        // The file still loads: the framework's own validation accepts what is on disk.
        load();
    }

    @Test
    @DisplayName("a customised title, or built-in text changed by one character, is kept byte for byte under both languages and the file is not rewritten")
    void customisedValuesAreKept() throws Exception {
        List<String> editedZh = new ArrayList<>(SHIPPED_LINES_3);
        editedZh.set(11, "&6play.myserver.net");
        List<String> editedEn = new ArrayList<>(EN_LINES);
        editedEn.set(0, "&7Welcome, &f%player_name%!");
        String[][] cases = {
                {"&a&lOperator Title", "zh-edited"},
                {SHIPPED_TITLE + "!", "en-edited"},
                {EN_TITLE + " ", "zh-edited"},
                {"&6&lMy server", "en-edited"},
        };
        for (String code : LANGUAGES) {
            for (String[] c : cases) {
                language[0] = code;
                List<String> l = "zh-edited".equals(c[1]) ? editedZh : editedEn;
                write(c[0], l);
                SideBarConfig config = spy(load());
                byte[] before = bytes();

                start(config);

                String what = "language " + code + ", title '" + c[0] + "', " + c[1];
                assertThat(bytes()).as(what).isEqualTo(before);
                assertThat(config.getTitle()).as(what).isEqualTo(c[0]);
                assertThat(config.getLines()).as(what).containsExactlyElementsOf(l);
                verify(config, never()).save();
            }
        }
    }

    @Test
    @DisplayName("a second enable with the same language writes nothing")
    void secondEnableWritesNothing() throws Exception {
        for (String code : LANGUAGES) {
            language[0] = code;
            write(SHIPPED_TITLE, SHIPPED_LINES_1);
            start(load());
            byte[] afterFirst = bytes();

            SideBarConfig second = spy(load());
            start(second);

            assertThat(bytes()).as("language " + code).isEqualTo(afterFirst);
            verify(second, never()).save();
        }
    }

    @Test
    @DisplayName("a full reload after a language switch rewrites the text in the new language, in both directions")
    void reloadFollowsALanguageSwitchBothWays() throws Exception {
        for (String[] direction : new String[][] {{"en", "zh"}, {"zh", "en"}}) {
            language[0] = direction[0];
            Files.deleteIfExists(file().toPath());
            SideBarConfig config = load();
            SideBarService service = start(config);
            assertThat(onDisk().getString("title")).isEqualTo(title(direction[0]));

            language[0] = direction[1];
            reload(config, service);

            String what = direction[0] + " -> " + direction[1];
            assertThat(onDisk().getString("title")).as(what).isEqualTo(title(direction[1]));
            assertThat(onDisk().getStringList("lines")).as(what).containsExactlyElementsOf(lines(direction[1]));
            assertThat(config.getTitle()).as(what).isEqualTo(title(direction[1]));
            assertThat(config.getLines()).as(what).containsExactlyElementsOf(lines(direction[1]));
        }
    }

    @Test
    @DisplayName("the configuration change listener, which fires before the language is rebuilt, does not rewrite the text")
    void changeListenerDoesNotMaterialize() throws Exception {
        language[0] = "en";
        SideBarConfig config = load();
        start(config);
        byte[] before = bytes();
        assertThat(config.getChangeListeners()).isNotEmpty();

        language[0] = "zh";
        try (MockedStatic<Bukkit> bukkit = bukkit()) {
            for (ConfigChangeListener listener : new ArrayList<>(config.getChangeListeners())) {
                listener.onConfigReload(config);
            }
        }

        assertThat(bytes()).isEqualTo(before);
        assertThat(config.getTitle()).isEqualTo(EN_TITLE);
    }

    @Test
    @DisplayName("an operator-edited language file on disk does not widen what counts as built-in text")
    void diskCatalogueDoesNotWidenTheTrackedSet() throws Exception {
        Path lang = Files.createDirectories(tempDir.resolve("lang"));
        for (String code : LANGUAGES) {
            Files.write(lang.resolve(code + ".yml"),
                    "sidebar_default_title: \"&6&lEdited\"\nsidebar_default_lines: \"only\\nme\"\n".getBytes(StandardCharsets.UTF_8));
        }
        for (String code : LANGUAGES) {
            language[0] = code;
            write("&6&lEdited", Arrays.asList("only", "me"));
            SideBarConfig config = spy(load());

            start(config);

            assertThat(onDisk().getString("title")).as(code).isEqualTo("&6&lEdited");
            assertThat(onDisk().getStringList("lines")).as(code).containsExactly("only", "me");
            verify(config, never()).save();
        }
    }

    @Test
    @DisplayName("an operator's edit of the extracted language file is not written into sidebar.yml, so the value keeps following a language switch")
    void diskCatalogueEditDoesNotReachTheFile() throws Exception {
        // What the module's i18n answers when the operator edited both entries in lang/en.yml on disk.
        diskOverrides.put("sidebar_default_title", "&6&lEdited Server");
        diskOverrides.put("sidebar_default_lines", "edited\nlines");
        language[0] = "en";
        write(SHIPPED_TITLE, SHIPPED_LINES_3);
        SideBarConfig config = load();
        SideBarService service = start(config);

        assertThat(onDisk().getString("title")).as("en: the jar's English text, not the disk edit").isEqualTo(EN_TITLE);
        assertThat(onDisk().getStringList("lines")).containsExactlyElementsOf(EN_LINES);

        language[0] = "zh";
        reload(config, service);

        assertThat(onDisk().getString("title")).as("after a switch to zh the value follows").isEqualTo(SHIPPED_TITLE);
        assertThat(onDisk().getStringList("lines")).containsExactlyElementsOf(SHIPPED_LINES_3);
    }

    @Test
    @DisplayName("a file that cannot be saved is reported in the server's language, and the sidebar still shows the new text")
    void saveFailureIsReported() throws Exception {
        language[0] = "en";
        write(SHIPPED_TITLE, SHIPPED_LINES_3);
        SideBarConfig config = spy(load());
        doThrow(new IOException("disk full")).when(config).save();

        start(config);

        String expected = CatalogueText.text("en", "sidebar_log_defaults_save_failed").replace("{ERROR}", "disk full");
        verify(logger).warn(expected);
        assertThat(config.getTitle()).isEqualTo(EN_TITLE);
    }

    @Test
    @DisplayName("the sidebar a player sees is rendered from the file's text")
    void theSidebarShowsTheFileText() throws Exception {
        language[0] = "en";
        SideBarConfig config = load();
        SideBarService service = start(config);
        YamlConfiguration disk = onDisk();

        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        Scoreboard scoreboard = mock(Scoreboard.class);
        Objective objective = mock(Objective.class);
        Score score = mock(Score.class);
        when(scoreboard.getObjective("sidebar")).thenReturn(objective);
        when(scoreboard.getEntries()).thenReturn(Collections.<String>emptySet());
        when(objective.getScore(anyString())).thenReturn(score);
        playerScoreboards(service).put(id, scoreboard);

        service.updateSidebar(player);

        verify(objective).setDisplayName(ChatColor.translateAlternateColorCodes('&', disk.getString("title")));
        for (String line : disk.getStringList("lines")) {
            if (!line.isEmpty()) {
                String rendered = ChatColor.translateAlternateColorCodes('&', line);
                // SideBarService cuts an entry to 40 characters (UltiKits/UltiSideBar#17).
                verify(objective).getScore(rendered.length() > 40 ? rendered.substring(0, 40) : rendered);
            }
        }
        verify(objective).getScore(ChatColor.translateAlternateColorCodes('&', "&7Welcome, &f%player_name%"));
    }

    @Test
    @DisplayName("@NotEmpty is on exactly title and lines, and the title's Java default is the shipped title")
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    void validationAndJavaDefaults() throws Exception {
        Set<String> notEmpty = new TreeSet<>();
        for (Field f : SideBarConfig.class.getDeclaredFields()) {
            if (f.isAnnotationPresent(ConfigEntry.class) && f.isAnnotationPresent(NotEmpty.class)) {
                notEmpty.add(f.getName());
            }
        }
        assertThat(notEmpty).containsExactly("lines", "title");

        Field title = SideBarConfig.class.getDeclaredField("title");
        Size size = title.getAnnotation(Size.class);
        assertThat(size.min()).isEqualTo(1);
        assertThat(size.max()).isEqualTo(32);
        assertThat(EN_TITLE.length()).isLessThanOrEqualTo(32);

        SideBarConfig fresh = new SideBarConfig();
        title.setAccessible(true);
        assertThat(title.get(fresh)).isEqualTo(SHIPPED_TITLE);
        assertThat(fresh.getLines()).containsExactlyElementsOf(SHIPPED_LINES_3);
    }

    // ---- harness ----

    private File file() {
        return new File(tempDir.toFile(), "config/sidebar.yml");
    }

    private byte[] bytes() throws IOException {
        return Files.readAllBytes(file().toPath());
    }

    private YamlConfiguration onDisk() {
        return YamlConfiguration.loadConfiguration(file());
    }

    /** Writes a sidebar.yml holding {@code title} and {@code lines}, as an earlier version or an operator left it. */
    private void write(String title, List<String> lines) throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        YamlConfiguration persisted = new YamlConfiguration();
        persisted.set("title", title);
        persisted.set("lines", new ArrayList<>(lines));
        persisted.save(file());
    }

    /** The framework's own load: {@code init} fills missing keys with the Java defaults, saves, validates. */
    private SideBarConfig load() throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        SideBarConfig config = new SideBarConfig();
        config.init(plugin);
        return config;
    }

    /** The module's enable path: {@code SideBarService#init}, which {@code registerSelf()} calls. */
    private SideBarService start(SideBarConfig config) throws Exception {
        SideBarService service = new SideBarService();
        set(service, "plugin", plugin);
        set(service, "config", config);
        try (MockedStatic<Bukkit> bukkit = bukkit()) {
            service.init();
        }
        return service;
    }

    /**
     * The framework's full reload as it reaches this module: {@code reloadConfigs} re-runs {@code init}
     * (which fires the change listeners), the language is rebuilt, then {@code onReload()} calls
     * {@code SideBarService#reload()}. Here the language was already switched by the caller, which is
     * stricter: the listeners see the new language and still must not write.
     */
    private void reload(SideBarConfig config, SideBarService service) throws Exception {
        try (MockedStatic<Bukkit> bukkit = bukkit()) {
            config.init(plugin);
            service.reload();
        }
    }

    private MockedStatic<Bukkit> bukkit() {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        PluginManager plugins = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());
        bukkit.when(Bukkit::getScheduler).thenReturn(mock(BukkitScheduler.class));
        return bukkit;
    }

    /**
     * A plugin double whose configuration folder is the temporary directory and whose {@code i18n}
     * answers from the module's real catalogue for the language in {@link #language}, read at call
     * time. The two folder methods are {@code protected final} outside this package, so they are
     * answered by name.
     */
    private UltiToolsPlugin pluginDouble() {
        final Map<String, org.mockito.stubbing.Answer<String>> answers = new LinkedHashMap<>();
        for (String code : LANGUAGES) {
            answers.put(code, CatalogueText.answer(code));
        }
        return Mockito.mock(UltiToolsPlugin.class, invocation -> {
            String name = invocation.getMethod().getName();
            if ("getConfigFolder".equals(name)) {
                return tempDir.toString();
            }
            if ("getConfigFile".equals(name)) {
                return new File(tempDir.toFile(), invocation.<String>getArgument(0));
            }
            if ("i18n".equals(name)) {
                String key = invocation.getArgument(invocation.getArguments().length - 1);
                return diskOverrides.containsKey(key) ? diskOverrides.get(key) : answers.get(language[0]).answer(invocation);
            }
            if ("getLogger".equals(name)) {
                return logger;
            }
            if ("getLanguageCode".equals(name)) {
                return language[0];
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    @SuppressWarnings({"unchecked", "PMD.AvoidAccessibilityAlteration"})
    private static Map<UUID, Scoreboard> playerScoreboards(SideBarService service) throws Exception {
        Field f = SideBarService.class.getDeclaredField("playerScoreboards");
        f.setAccessible(true);
        return (Map<UUID, Scoreboard>) f.get(service);
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
