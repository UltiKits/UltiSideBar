package com.ultikits.plugins.sidebar.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.ultikits.ultitools.abstracts.AbstractConfigEntity;
import com.ultikits.ultitools.annotations.ConfigEntity;
import com.ultikits.ultitools.annotations.ConfigEntry;
import com.ultikits.ultitools.annotations.config.NotEmpty;
import com.ultikits.ultitools.annotations.config.Range;
import com.ultikits.ultitools.annotations.config.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for UltiSideBar.
 *
 * @author wisdomme
 * @version 1.0.0
 */
@Getter
@Setter
@ConfigEntity("config/sidebar.yml")
public class SideBarConfig extends AbstractConfigEntity {

    @ConfigEntry(path = "enabled", comment = "启用侧边栏")
    private boolean enabled = true;

    // The Java default is the title earlier versions shipped: the framework writes it for a missing
    // key and materializeText() then rewrites it in the server's language.
    @NotEmpty
    @Size(min = 1, max = 32)
    @ConfigEntry(path = "title", comment = "侧边栏标题（支持颜色代码和变量）")
    private String title = SHIPPED_TITLE;

    @Range(min = 1, max = 1200)
    @ConfigEntry(path = "update-interval", comment = "更新间隔（tick，20 tick = 1秒）")
    private int updateInterval = 20;

    @NotEmpty
    @Size(min = 1, max = 15)
    @ConfigEntry(path = "lines", comment = "侧边栏内容（支持 PlaceholderAPI 变量）")
    private List<String> lines = new ArrayList<>(SHIPPED_LINES);

    @ConfigEntry(path = "world-blacklist", comment = "禁用侧边栏的世界")
    private List<String> worldBlacklist = Collections.singletonList("world_event");

    @ConfigEntry(path = "default-enabled", comment = "玩家默认启用侧边栏")
    private boolean defaultEnabled = true;

    public SideBarConfig() {
        super("config/sidebar.yml");
    }

    /** The catalogue key of the title's text in the server's language. */
    static final String TITLE_KEY = "sidebar_default_title";

    /** The catalogue key of the lines' text in the server's language, one entry with the lines separated by "\n". */
    static final String LINES_KEY = "sidebar_default_lines";

    /**
     * Writes the title and lines in the server's language (maintainer decision 2026-09-25): first the
     * exact-match line fixes of {@link #migrateLegacyDefaultLines()}, then each of the two settings is
     * replaced with {@code text}'s current text when it is still built-in text -- the title or lines an
     * earlier version shipped, or this jar's text for it in any language -- and differs from the
     * current text, and fits the setting's own limits. Any other value is the operator's and is kept.
     * Idempotent. Must run after the
     * module's language is loaded (enable and {@code onReload()}), never from a change listener; the
     * caller saves the file when this returns {@code true}.
     *
     * @param text catalogue key to text in the server's language, from this jar's own catalogue
     *             ({@code ConfigTextDefaults#jarLanguage}), so every value written is in the tracked set
     * @return {@code true} if at least one value was rewritten
     */
    public boolean materializeText(Function<String, String> text) {
        boolean changed = migrateLegacyDefaultLines();
        Map<String, Map<String, String>> jar = ConfigTextDefaults.jarCatalogues(SideBarConfig.class);

        String newTitle = ConfigTextDefaults.materialize(SideBarConfig.class, "title", title,
                ConfigTextDefaults.currentText(text, "", TITLE_KEY),
                ConfigTextDefaults.tracked(jar, "", TITLE_KEY, SHIPPED_TITLE));
        if (!Objects.equals(newTitle, title)) {
            title = newTitle;
            changed = true;
        }

        List<String> newLines = ConfigTextDefaults.materializeLines(SideBarConfig.class, "lines", lines,
                ConfigTextDefaults.currentLines(text, LINES_KEY),
                ConfigTextDefaults.trackedLines(jar, LINES_KEY, SHIPPED_LINES_FIRST, SHIPPED_LINES_SECOND, SHIPPED_LINES));
        if (!Objects.equals(newLines, lines)) {
            lines = newLines;
            changed = true;
        }
        return changed;
    }

    /**
     * The pre-6.3.0 shipped default world-name line, which used the invalid PlaceholderAPI
     * syntax {@code %world_name%} (UltiKits/UltiSideBar#13 -- the real "World" expansion
     * placeholder, {@code %world_name_<world>%}, requires an explicit world argument).
     * {@code AbstractConfigEntity.init()} never overwrites a key that already exists on disk,
     * so any server that has ever started this plugin keeps this exact string in its persisted
     * {@code sidebar.yml} forever unless it is rewritten explicitly.
     */
    private static final String LEGACY_WORLD_NAME_LINE = "&e世界: &f%world_name%";

    /**
     * The corrected default that replaces {@link #LEGACY_WORLD_NAME_LINE}; part of
     * {@link #SHIPPED_LINES}, the "lines" default.
     */
    private static final String CURRENT_WORLD_NAME_LINE = "&e世界: &f%player_world%";

    /**
     * The pre-6.3.0 shipped default server-time line, which used the ambiguous 12-hour pattern
     * {@code hh:mm:ss} with no AM/PM marker (PR #15 round-3 review). Same persistence problem as
     * {@link #LEGACY_WORLD_NAME_LINE}: {@code AbstractConfigEntity.init()} preserves this exact
     * string in {@code sidebar.yml} on every server that has ever started an older version of
     * this plugin, unless it is rewritten explicitly.
     */
    private static final String LEGACY_SERVER_TIME_LINE = "&f%server_time_hh:mm:ss%";

    /**
     * The corrected default that replaces {@link #LEGACY_SERVER_TIME_LINE} with the unambiguous
     * 24-hour pattern; part of {@link #SHIPPED_LINES}, the "lines" default.
     */
    private static final String CURRENT_SERVER_TIME_LINE = "&f%server_time_HH:mm:ss%";

    /**
     * The title every earlier version shipped. The Java default, and one of the values
     * {@link #materializeText} recognises as built-in text in an operator's file; compared byte for byte.
     */
    private static final String SHIPPED_TITLE = "&6&l我的服务器";

    // The lines every shipped default is built from. Each Chinese literal appears once here and is
    // compared byte for byte by materializeText(); the text written into a file comes from the language
    // file's sidebar_default_lines.
    private static final String WELCOME_LINE = "&7欢迎, &f%player_name%";
    private static final String ONLINE_LINE = "&e在线人数: &f%server_online%/%server_max_players%";
    private static final String BALANCE_LINE = "&e金币: &f%vault_eco_balance_formatted%";
    private static final String PING_LINE = "&ePing: &f%player_ping%ms";
    private static final String SERVER_TIME_LABEL_LINE = "&7服务器时间";
    private static final String ADDRESS_LINE = "&6play.example.com";

    /** The lines the first version shipped: the invalid world line and the 12-hour time line. */
    private static final List<String> SHIPPED_LINES_FIRST = Collections.unmodifiableList(Arrays.asList(
            WELCOME_LINE, "", ONLINE_LINE, LEGACY_WORLD_NAME_LINE, "", BALANCE_LINE, PING_LINE,
            "", SERVER_TIME_LABEL_LINE, LEGACY_SERVER_TIME_LINE, "", ADDRESS_LINE));

    /** The lines shipped after UltiKits/UltiSideBar#13 fixed the world line, still with the 12-hour time line. */
    private static final List<String> SHIPPED_LINES_SECOND = Collections.unmodifiableList(Arrays.asList(
            WELCOME_LINE, "", ONLINE_LINE, CURRENT_WORLD_NAME_LINE, "", BALANCE_LINE, PING_LINE,
            "", SERVER_TIME_LABEL_LINE, LEGACY_SERVER_TIME_LINE, "", ADDRESS_LINE));

    /** The lines the last version shipped: the Java default of {@code lines}. */
    private static final List<String> SHIPPED_LINES = Collections.unmodifiableList(Arrays.asList(
            WELCOME_LINE, "", ONLINE_LINE, CURRENT_WORLD_NAME_LINE, "", BALANCE_LINE, PING_LINE,
            "", SERVER_TIME_LABEL_LINE, CURRENT_SERVER_TIME_LINE, "", ADDRESS_LINE));

    /**
     * Every byte-identical legacy default line this plugin has ever shipped, mapped to its
     * corrected replacement. Extend this map -- not the loop in
     * {@link #migrateLegacyDefaultLines()} -- when a future shipped default needs the same
     * exact-match migration treatment.
     */
    private static final Map<String, String> LEGACY_LINE_REPLACEMENTS;

    static {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put(LEGACY_WORLD_NAME_LINE, CURRENT_WORLD_NAME_LINE);
        replacements.put(LEGACY_SERVER_TIME_LINE, CURRENT_SERVER_TIME_LINE);
        LEGACY_LINE_REPLACEMENTS = Collections.unmodifiableMap(replacements);
    }

    /**
     * One-time migration for a persisted {@code sidebar.yml} whose {@code lines} list still
     * carries one or more old, invalid shipped defaults tracked in
     * {@link #LEGACY_LINE_REPLACEMENTS} (issue #13; PR #15 round-3 review extended this from the
     * world-name line alone to also cover the 12-hour server-time line). Rewrites only a list
     * entry that is byte-identical to a tracked legacy default -- any operator customisation,
     * including a line that merely mentions a legacy token alongside other text, is left
     * untouched. Idempotent: once migrated, no entry matches a tracked legacy default any more,
     * so a second call is a no-op.
     * <p>
     * Must be called after {@code init(UltiToolsPlugin)} has populated {@link #lines} from
     * disk. The caller is responsible for persisting the result with {@code save()} when this
     * method returns {@code true} -- this method only updates the in-memory value.
     *
     * @return {@code true} if at least one line was rewritten, {@code false} otherwise
     */
    public boolean migrateLegacyDefaultLines() {
        if (lines == null) {
            return false;
        }
        boolean changed = false;
        List<String> migrated = new ArrayList<>(lines.size());
        for (String line : lines) {
            String replacement = LEGACY_LINE_REPLACEMENTS.get(line);
            if (replacement != null) {
                migrated.add(replacement);
                changed = true;
            } else {
                migrated.add(line);
            }
        }
        if (changed) {
            lines = migrated;
        }
        return changed;
    }
}
