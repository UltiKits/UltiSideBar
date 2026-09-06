package com.ultikits.plugins.sidebar.config;

import com.ultikits.plugins.sidebar.UltiSideBarTestHelper;
import com.ultikits.plugins.sidebar.service.SideBarService;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@DisplayName("SideBarConfig Tests")
class SideBarConfigTest {

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have sidebar enabled by default")
        void enabled() {
            SideBarConfig config = createRealConfig();
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have default title")
        void title() {
            SideBarConfig config = createRealConfig();
            assertThat(config.getTitle()).isEqualTo("&6&l我的服务器");
        }

        @Test
        @DisplayName("Should have 20 tick update interval by default")
        void updateInterval() {
            SideBarConfig config = createRealConfig();
            assertThat(config.getUpdateInterval()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should have default lines")
        void lines() {
            SideBarConfig config = createRealConfig();
            assertThat(config.getLines()).isNotEmpty();
            assertThat(config.getLines()).contains("&7欢迎, &f%player_name%");
        }

        @Test
        @DisplayName("Should have default world blacklist")
        void worldBlacklist() {
            SideBarConfig config = createRealConfig();
            assertThat(config.getWorldBlacklist()).containsExactly("world_event");
        }

        @Test
        @DisplayName("Should have default enabled true")
        void defaultEnabled() {
            SideBarConfig config = createRealConfig();
            assertThat(config.isDefaultEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("Should update enabled")
        void setEnabled() {
            SideBarConfig config = createRealConfig();
            config.setEnabled(false);
            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should update title")
        void setTitle() {
            SideBarConfig config = createRealConfig();
            config.setTitle("&e&lNew Title");
            assertThat(config.getTitle()).isEqualTo("&e&lNew Title");
        }

        @Test
        @DisplayName("Should update update interval")
        void setUpdateInterval() {
            SideBarConfig config = createRealConfig();
            config.setUpdateInterval(40);
            assertThat(config.getUpdateInterval()).isEqualTo(40);
        }

        @Test
        @DisplayName("Should update lines")
        void setLines() {
            SideBarConfig config = createRealConfig();
            config.setLines(Arrays.asList("Line 1", "Line 2"));
            assertThat(config.getLines()).containsExactly("Line 1", "Line 2");
        }

        @Test
        @DisplayName("Should update world blacklist")
        void setWorldBlacklist() {
            SideBarConfig config = createRealConfig();
            config.setWorldBlacklist(Arrays.asList("world1", "world2"));
            assertThat(config.getWorldBlacklist()).containsExactly("world1", "world2");
        }

        @Test
        @DisplayName("Should update default enabled")
        void setDefaultEnabled() {
            SideBarConfig config = createRealConfig();
            config.setDefaultEnabled(false);
            assertThat(config.isDefaultEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should initialize with correct path")
        void constructorPath() {
            SideBarConfig config = createRealConfig();
            assertThat(config).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty world blacklist")
        void emptyBlacklist() {
            SideBarConfig config = createRealConfig();
            config.setWorldBlacklist(Collections.emptyList());
            assertThat(config.getWorldBlacklist()).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty lines")
        void emptyLines() {
            SideBarConfig config = createRealConfig();
            config.setLines(Collections.emptyList());
            assertThat(config.getLines()).isEmpty();
        }
    }

    // ============================
    // Default sidebar renderability
    // ============================

    @Nested
    @DisplayName("Default Sidebar Renderability")
    class DefaultSidebarRenderabilityTests {

        /**
         * Sentinel substitutions for the placeholders a real PlaceholderAPI installation (Player
         * + Server expansions) resolves, sourced independently from PlaceholderAPI's own
         * placeholder wiki rather than re-derived from the defaults under test in this file --
         * so a future commit cannot introduce a broken token and "fix" this test in the same
         * edit by adding the same name to a local allow-list.
         * <p>
         * {@code vault_eco_balance_formatted} is deliberately excluded (WR-03): it additionally
         * requires Vault plus a registered economy provider, a materially larger install surface
         * than "PlaceholderAPI is installed", so it is exempted below by name rather than
         * silently substituted here.
         */
        private static final String VAULT_DEPENDENT_TOKEN = "%vault_eco_balance_formatted%";

        private String stubResolve(String text) {
            String resolved = text
                    .replace("%player_name%", "Steve")
                    .replace("%server_online%", "12")
                    .replace("%server_max_players%", "100")
                    .replace("%player_world%", "world")
                    .replace("%player_ping%", "42");
            // PlaceholderAPI's Server expansion accepts an arbitrary SimpleDateFormat pattern as
            // a dynamic suffix: %server_time_<SimpleDateFormat>%.
            return resolved.replaceAll("%server_time_[^%]+%", "12:00:00");
        }

        @Test
        @DisplayName("Default lines contain no token that nothing resolves")
        void defaultLinesContainNoTokenThatNothingResolves() throws Exception {
            SideBarConfig config = createRealConfig();

            // Route every default line through the module's own placeholder-resolution path
            // (SideBarService.parsePlaceholders -> PlaceholderAPI.setPlaceholders) instead of
            // checking token names against a hand-authored allow-list mirroring this same
            // file's defaults -- that allow-list could never catch a broken token added
            // alongside a matching allow-list entry in the same commit. The PlaceholderAPI seam
            // is stubbed to behave the way a real installation does: a recognized placeholder is
            // substituted, and one no registered expansion recognizes is left untouched in the
            // output -- exactly the symptom the original issue (#13) reported.
            SideBarService service = new SideBarService();
            UltiSideBarTestHelper.setField(service, "placeholderApiAvailable", true);
            Player player = Mockito.mock(Player.class);

            Method parsePlaceholders = SideBarService.class
                    .getDeclaredMethod("parsePlaceholders", Player.class, String.class);
            parsePlaceholders.setAccessible(true);

            try (MockedStatic<PlaceholderAPI> placeholderApi = Mockito.mockStatic(PlaceholderAPI.class)) {
                placeholderApi.when(() -> PlaceholderAPI.setPlaceholders(eq(player), anyString()))
                        .thenAnswer(invocation -> stubResolve(invocation.getArgument(1)));

                List<String> unresolvedTokensRemaining = new ArrayList<>();
                Pattern leftoverTokenPattern = Pattern.compile("%[^%]+%");
                for (String line : config.getLines()) {
                    String rendered = (String) parsePlaceholders.invoke(service, player, line);
                    Matcher matcher = leftoverTokenPattern.matcher(rendered);
                    while (matcher.find()) {
                        String leftover = matcher.group();
                        if (!VAULT_DEPENDENT_TOKEN.equals(leftover)) {
                            unresolvedTokensRemaining.add(leftover);
                        }
                    }
                }

                assertThat(unresolvedTokensRemaining)
                        .as("Every default line must render through the module's own placeholder "
                                + "path with no leftover token that nothing resolves")
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("An operator-configured line survives init() against a persisted file that also holds the legacy default")
        void anOperatorConfiguredLineIsUnaffected(@TempDir Path tempDir) throws Exception {
            // Drives the real init()-mediated persisted-file-vs-default precedence (CR-01) --
            // a bare setLines()/getLines() round-trip cannot fail for any change to
            // SideBarConfig's default-handling behavior and proves nothing about upgrade safety.
            File configFile = new File(tempDir.toFile(), "config/sidebar.yml");
            Files.createDirectories(configFile.getParentFile().toPath());
            YamlConfiguration persisted = new YamlConfiguration();
            persisted.set("lines", Arrays.asList(
                    "&e世界: &f%world_name%",
                    "&aOperator's own custom line"
            ));
            persisted.save(configFile);

            SideBarConfig config = createRealConfig();
            config.init(mockPluginBackedBy(tempDir));

            assertThat(config.getLines())
                    .as("an operator's own persisted line must survive init() untouched")
                    .contains("&aOperator's own custom line");
        }
    }

    /**
     * Builds an {@code UltiToolsPlugin} test double whose {@code getConfigFolder()}/
     * {@code getConfigFile(String)} resolve against {@code tempDir}. Those two methods are
     * {@code protected final} on {@code UltiToolsPlugin}, declared outside this test's package,
     * so a normal {@code Mockito.when(mock.getConfigFolder())...} does not even compile here --
     * this uses Mockito's {@code mock(Class, Answer)} default-answer form instead, which
     * intercepts every method call by reflection ({@code invocation.getMethod()}) rather than by
     * a source-level call to the (inaccessible) method.
     */
    private static UltiToolsPlugin mockPluginBackedBy(Path tempDir) {
        return Mockito.mock(UltiToolsPlugin.class, invocation -> {
            String methodName = invocation.getMethod().getName();
            if ("getConfigFolder".equals(methodName)) {
                return tempDir.toString();
            }
            if ("getConfigFile".equals(methodName)) {
                String path = invocation.getArgument(0);
                return new File(tempDir.toFile(), path);
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    // ============================
    // Legacy %world_name% default line migration (issue #13, CR-01)
    // ============================

    @Nested
    @DisplayName("Legacy World-Name Default Line Migration")
    class LegacyWorldNameLineMigration {

        @TempDir
        Path tempDir;

        private UltiToolsPlugin mockPlugin;

        @BeforeEach
        void setUp() {
            mockPlugin = mockPluginBackedBy(tempDir);
        }

        private File persistLines(List<String> lines) throws Exception {
            File configFile = new File(tempDir.toFile(), "config/sidebar.yml");
            Files.createDirectories(configFile.getParentFile().toPath());
            YamlConfiguration persisted = new YamlConfiguration();
            persisted.set("lines", lines);
            persisted.save(configFile);
            return configFile;
        }

        @Test
        @DisplayName("Rewrites a persisted line byte-identical to the old %world_name% default; a custom line survives")
        void rewritesLegacyLineButLeavesCustomLineUntouched() throws Exception {
            File configFile = persistLines(Arrays.asList(
                    "&7欢迎, &f%player_name%",
                    "&e世界: &f%world_name%",
                    "&aOperator's own custom line"
            ));

            SideBarConfig config = new SideBarConfig();
            config.init(mockPlugin);

            boolean rewritten = config.migrateLegacyWorldNameDefaultLine();
            assertThat(rewritten).isTrue();
            config.save();

            assertThat(config.getLines())
                    .as("the stale %world_name% line must be rewritten to the corrected default")
                    .contains("&e世界: &f%player_world%")
                    .doesNotContain("&e世界: &f%world_name%");
            assertThat(config.getLines())
                    .as("an operator's own custom line must be left untouched")
                    .contains("&aOperator's own custom line");

            YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(configFile);
            assertThat(onDisk.getStringList("lines"))
                    .as("the migration must be persisted back to disk")
                    .contains("&e世界: &f%player_world%", "&aOperator's own custom line")
                    .doesNotContain("&e世界: &f%world_name%");
        }

        @Test
        @DisplayName("Does not touch a line that merely mentions %world_name% inside other text")
        void doesNotTouchLineThatOnlyMentionsTheLegacyToken() throws Exception {
            persistLines(Collections.singletonList("&7Custom: &f%world_name% (renamed by admin)"));

            SideBarConfig config = new SideBarConfig();
            config.init(mockPlugin);

            boolean rewritten = config.migrateLegacyWorldNameDefaultLine();

            assertThat(rewritten).isFalse();
            assertThat(config.getLines())
                    .containsExactly("&7Custom: &f%world_name% (renamed by admin)");
        }

        @Test
        @DisplayName("Is a no-op once the persisted line already uses the corrected placeholder")
        void noOpWhenAlreadyMigrated() throws Exception {
            persistLines(Collections.singletonList("&e世界: &f%player_world%"));

            SideBarConfig config = new SideBarConfig();
            config.init(mockPlugin);

            assertThat(config.migrateLegacyWorldNameDefaultLine()).isFalse();
        }
    }

    /**
     * Create a real SideBarConfig instance.
     * The no-arg constructor calls super("config/sidebar.yml") which only stores the path
     * without triggering file I/O (init() does that separately).
     */
    private SideBarConfig createRealConfig() {
        return new SideBarConfig();
    }
}
