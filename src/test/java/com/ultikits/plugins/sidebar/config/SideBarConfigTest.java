package com.ultikits.plugins.sidebar.config;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

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
         * Placeholder tokens the shipped defaults are allowed to contain. Every one of these
         * is a real PlaceholderAPI placeholder that resolves once PlaceholderAPI (and, for
         * vault_eco_balance_formatted, Vault plus an economy provider) is installed --
         * "documented as requiring the external placeholder provider" is a legitimate category,
         * distinct from a token that resolves nowhere no matter what is installed.
         */
        private final Set<String> KNOWN_RESOLVABLE_TOKENS = new HashSet<>(Arrays.asList(
                "player_name",
                "server_online",
                "server_max_players",
                "player_world",
                "vault_eco_balance_formatted",
                "player_ping"
        ));

        private boolean isKnownResolvable(String token) {
            if (KNOWN_RESOLVABLE_TOKENS.contains(token)) {
                return true;
            }
            // PlaceholderAPI's Server expansion accepts an arbitrary SimpleDateFormat pattern
            // as a dynamic suffix: %server_time_<SimpleDateFormat>%.
            return token.startsWith("server_time_");
        }

        private List<String> extractTokens(List<String> lines) {
            Pattern tokenPattern = Pattern.compile("%([a-zA-Z0-9_:]+)%");
            List<String> tokens = new ArrayList<>();
            for (String line : lines) {
                Matcher matcher = tokenPattern.matcher(line);
                while (matcher.find()) {
                    tokens.add(matcher.group(1));
                }
            }
            return tokens;
        }

        @Test
        @DisplayName("Default lines contain no token that nothing resolves")
        void defaultLinesContainNoTokenThatNothingResolves() {
            SideBarConfig config = createRealConfig();

            List<String> tokens = extractTokens(config.getLines());
            assertThat(tokens).isNotEmpty();

            List<String> unresolvableTokens = new ArrayList<>();
            for (String token : tokens) {
                if (!isKnownResolvable(token)) {
                    unresolvableTokens.add(token);
                }
            }

            assertThat(unresolvableTokens)
                    .as("Every placeholder token in the shipped defaults must be a real, " +
                            "resolvable PlaceholderAPI placeholder -- not a token nothing provides")
                    .isEmpty();
        }

        @Test
        @DisplayName("An operator-configured line is unaffected")
        void anOperatorConfiguredLineIsUnaffected() {
            SideBarConfig config = createRealConfig();
            List<String> customLines = Arrays.asList(
                    "&aCustom Line 1", "%world_name%", "&bAnother line"
            );

            config.setLines(customLines);

            assertThat(config.getLines()).isEqualTo(customLines);
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
