package com.ultikits.plugins.sidebar.config;

import com.ultikits.plugins.sidebar.i18n.CatalogueText;
import com.ultikits.plugins.sidebar.service.SideBarService;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.ConfigEntry;
import com.ultikits.ultitools.annotations.config.Size;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@code lines: []} in {@code config/sidebar.yml} on UltiTools 6.3.0 (UltiKits/UltiSideBar#36; the framework side is
 * UltiKits/UltiTools-Reborn#630, maintainer decision of 2026-10-06, row 00:46 of the overnight decisions, and finding
 * F3 of its gate-1 review).
 * <p>
 * {@code lines} is declared {@code @NotEmpty} and {@code @Size(min = 1, max = 15)}. Before 6.3.0 {@code []} refused
 * the module. Now the framework runs the declared default lines in memory, logs ONE WARNING naming the key, the value
 * as written and the default, and leaves the file as the operator wrote it. The module must not add anything to that:
 * it rewrites built-in lines in the server's language and saves, but the substituted default is not what the file holds,
 * so the framework would decline the write and log "the module's changes to 'lines' were not written" at every start and
 * every reload, and report the same unsaved change again at server stop. The module therefore keeps the substituted
 * default untouched in memory, shows the language's text instead, and never calls {@code save()} for it.
 * <p>
 * Every case runs the framework's real {@code AbstractConfigEntity#init} and the module's real
 * {@code SideBarService#init} on a temporary folder, answers {@code i18n} from the module's real catalogues, and reads
 * the framework's WARNINGs from its own logger.
 */
@DisplayName("sidebar.yml with lines: [] runs the declared default lines and warns exactly once (#36)")
class SideBarConfigEmptyLinesTest {

    /** The last shipped lines, which are the Java default and the Chinese text this build writes. */
    private static final List<String> SHIPPED_LINES = Arrays.asList(
            "&7欢迎, &f%player_name%", "", "&e在线人数: &f%server_online%/%server_max_players%",
            "&e世界: &f%player_world%", "", "&e金币: &f%vault_eco_balance_formatted%", "&ePing: &f%player_ping%ms",
            "", "&7服务器时间", "&f%server_time_HH:mm:ss%", "", "&6play.example.com");

    private static final String SHIPPED_TITLE = "&6&l我的服务器";

    private static final Logger FRAMEWORK_LOGGER = Logger.getLogger("com.ultikits.ultitools");

    private static final String[] LANGUAGES = {"en", "zh"};

    @TempDir
    Path tempDir;

    private final String[] language = {"en"};

    private final PluginLogger logger = mock(PluginLogger.class);

    private final List<String> warnings = new ArrayList<>();

    private Handler capture;

    private final UltiToolsPlugin plugin = pluginDouble();

    @BeforeEach
    void captureFrameworkWarnings() {
        capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
                // nothing is buffered
            }

            @Override
            public void close() {
                // nothing is held
            }
        };
        FRAMEWORK_LOGGER.addHandler(capture);
    }

    @AfterEach
    void stopCapturing() {
        FRAMEWORK_LOGGER.removeHandler(capture);
    }

    @Test
    @DisplayName("lines: [] under en and under zh: the language's lines run, one framework WARNING, no save, no unsaved change, the file keeps []")
    void emptyLinesRunTheLanguagesDefaultWithOneWarning() throws Exception {
        for (String code : LANGUAGES) {
            for (String written : new String[] {"[]", "~"}) {
                warnings.clear();
                language[0] = code;
                String what = "language " + code + ", lines: " + written;
                write(title(code), "lines: " + written);
                SideBarConfig config = spy(load());

                SideBarService service = start(config);

                assertThat(config.getLines()).as(what + ": the sidebar shows the language's lines")
                        .containsExactlyElementsOf(lines(code));
                assertThat(only("key 'lines'")).as(what + ": the framework's own WARNING, once").hasSize(1);
                assertThat(only("key 'lines'").get(0)).as(what)
                        .contains("declared @NotEmpty").contains("using the declared default").contains("(the file is not changed)");
                assertThat(warnings).as(what + ": nothing else is warned").hasSize(1);
                verify(config, never()).save();
                assertThat(config.isModifiedSinceSnapshot()).as(what + ": nothing is left unsaved for the stop report").isFalse();
                assertThat(Files.readAllLines(file().toPath(), StandardCharsets.UTF_8))
                        .as(what + ": the operator's value is still in the file, and no list item follows it")
                        .contains("lines: " + written)
                        .doesNotContain("- '&7Welcome, &f%player_name%'", "- &7Welcome, &f%player_name%");

                warnings.clear();
                reload(config, service);

                assertThat(config.getLines()).as(what + ": after /ul reload").containsExactlyElementsOf(lines(code));
                assertThat(only("key 'lines'")).as(what + ": the reload warns once, as the framework does").hasSize(1);
                assertThat(warnings).as(what + ": and nothing else").hasSize(1);
                verify(config, never()).save();
                assertThat(config.isModifiedSinceSnapshot()).as(what + ": still nothing unsaved").isFalse();
            }
        }
    }

    @Test
    @DisplayName("lines the framework cannot read as a list ('', a word, a map): the default runs, one conversion WARNING, no 'not written', nothing unsaved")
    void aValueThatIsNotAListIsTreatedLikeAnEmptyOne() throws Exception {
        for (String code : LANGUAGES) {
            for (String written : new String[] {"lines: ''", "lines: foo", "lines:\n  a: b"}) {
                warnings.clear();
                language[0] = code;
                String what = "language " + code + ", " + written.replace("\n", " / ");
                write(title(code), written);
                SideBarConfig config = spy(load());

                SideBarService service = start(config);

                assertThat(config.getLines()).as(what + ": the language's lines are shown")
                        .containsExactlyElementsOf(lines(code));
                assertThat(only("were not written")).as(what + ": no 'not written' WARNING").isEmpty();
                assertThat(warnings).as(what + ": the framework's one WARNING and nothing else").hasSize(1);
                verify(config, never()).save();
                assertThat(config.isModifiedSinceSnapshot()).as(what + ": nothing unsaved for the stop report").isFalse();
                assertThat(Files.readAllLines(file().toPath(), StandardCharsets.UTF_8))
                        .as(what + ": the operator's text is still in the file").contains(written.split("\n")[0]);

                warnings.clear();
                reload(config, service);

                assertThat(only("were not written")).as(what + ": nor after /ul reload").isEmpty();
                assertThat(warnings).as(what + ": one WARNING per reload").hasSize(1);
                assertThat(config.isModifiedSinceSnapshot()).as(what + ": still nothing unsaved").isFalse();
            }
        }
    }

    @Test
    @DisplayName("lines: [] with the title still the earlier shipped text under en: the title is written and saved once, the lines stay unsaved and unwarned")
    void aWrittenTitleDoesNotDragTheSubstitutedLinesIntoTheSave() throws Exception {
        language[0] = "en";
        write(SHIPPED_TITLE, "lines: []");
        SideBarConfig config = spy(load());

        start(config);

        assertThat(onDisk().getString("title")).as("the built-in title follows the language, as before").isEqualTo(title("en"));
        verify(config, times(1)).save();
        assertThat(only("were not written")).as("no 'not written' WARNING for lines").isEmpty();
        assertThat(only("key 'lines'")).hasSize(1);
        assertThat(config.getLines()).containsExactlyElementsOf(lines("en"));
        assertThat(config.isModifiedSinceSnapshot()).isFalse();
        assertThat(Files.readAllLines(file().toPath(), StandardCharsets.UTF_8)).contains("lines: []");
    }

    @Test
    @DisplayName("a list the operator writes afterwards replaces the substituted lines at once, and a non-empty list is never warned about")
    void aLaterOperatorListWins() throws Exception {
        language[0] = "en";
        write(title("en"), "lines: []");
        SideBarConfig config = load();
        start(config);
        assertThat(config.getLines()).containsExactlyElementsOf(lines("en"));

        config.setLines(Arrays.asList("my line", "another"));

        assertThat(config.getLines()).containsExactly("my line", "another");

        warnings.clear();
        write(title("en"), "lines:\n- 'my line'\n- another");
        SideBarConfig reread = spy(load());
        start(reread);

        assertThat(reread.getLines()).containsExactly("my line", "another");
        assertThat(warnings).as("a non-empty list is used as written, silently").isEmpty();
        verify(reread, never()).save();
    }

    @Test
    @DisplayName("the declared default satisfies @Size(1..15), so the substitution can never itself be refused")
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    void theDeclaredDefaultFitsItsOwnLimits() throws Exception {
        Field lines = SideBarConfig.class.getDeclaredField("lines");
        Size size = lines.getAnnotation(Size.class);
        assertThat(lines.isAnnotationPresent(ConfigEntry.class)).isTrue();
        assertThat(size.min()).isEqualTo(1);
        assertThat(size.max()).isEqualTo(15);

        SideBarConfig fresh = new SideBarConfig();

        assertThat(fresh.getLines()).containsExactlyElementsOf(SHIPPED_LINES);
        assertThat(fresh.getLines().size()).isBetween(size.min(), size.max());
    }

    // ---- the texts, from the real catalogues ----

    private static String title(String code) {
        return CatalogueText.text(code, "sidebar_default_title");
    }

    private static List<String> lines(String code) {
        return Arrays.asList(CatalogueText.text(code, "sidebar_default_lines").split("\n", -1));
    }

    // ---- harness ----

    private List<String> only(String fragment) {
        List<String> mine = new ArrayList<>();
        for (String w : warnings) {
            if (w != null && w.contains(fragment)) {
                mine.add(w);
            }
        }
        return mine;
    }

    private File file() {
        return new File(tempDir.toFile(), "config/sidebar.yml");
    }

    private org.bukkit.configuration.file.YamlConfiguration onDisk() {
        return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file());
    }

    /** Writes a complete sidebar.yml whose {@code lines} entry is {@code linesYaml}, written byte for byte as given. */
    private void write(String title, String linesYaml) throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        String text = "enabled: true\n"
                + "title: '" + title.replace("'", "''") + "'\n"
                + "update-interval: 20\n"
                + linesYaml + "\n"
                + "world-blacklist:\n"
                + "- world_event\n"
                + "default-enabled: true\n";
        Files.write(file().toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

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
     * The framework's reload as it reaches this module: the real {@code AbstractConfigEntity#reload()} (the three-way
     * merge, then the change listeners), then {@code SideBarService#reload()}, which {@code onReload()} calls.
     */
    private void reload(SideBarConfig config, SideBarService service) throws Exception {
        try (MockedStatic<Bukkit> bukkit = bukkit()) {
            config.reload();
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

    private UltiToolsPlugin pluginDouble() {
        final Map<String, org.mockito.stubbing.Answer<String>> answers = new LinkedHashMap<>();
        for (String code : LANGUAGES) {
            answers.put(code, CatalogueText.answer(code));
        }
        return Mockito.mock(com.ultikits.plugins.sidebar.UltiSideBar.class, invocation -> {
            String name = invocation.getMethod().getName();
            if ("shippedCatalogueTexts".equals(name)) {
                return invocation.callRealMethod();
            }
            if ("getConfigFolder".equals(name)) {
                return tempDir.toString();
            }
            if ("getConfigFile".equals(name)) {
                return new File(tempDir.toFile(), invocation.<String>getArgument(0));
            }
            if ("i18n".equals(name)) {
                return answers.get(language[0]).answer(invocation);
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

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
