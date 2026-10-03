package com.ultikits.plugins.sidebar.config;

import com.ultikits.plugins.sidebar.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The comments above the keys of {@code config/sidebar.yml} come from the module's language files, so
 * a server set to English writes English comments (UltiKits/UltiSideBar#32; framework
 * UltiTools-Reborn#542). Every case runs the framework's real {@code AbstractConfigEntity#init} on a
 * temporary folder and answers {@code i18n} from the module's real catalogues.
 * <p>
 * 注释从语言文件取：英文服务器写入英文注释，中文服务器写入中文注释；升级时只改注释，不改值。
 */
@DisplayName("sidebar.yml writes its comments in the server's language (#32)")
class SideBarConfigCommentsTest {

    /** The six keys of the file, in declaration order, with the catalogue key of the comment above each. */
    private static final List<String[]> KEYS = Arrays.asList(
            new String[] {"enabled", "sidebar_config_comment_enabled"},
            new String[] {"title", "sidebar_config_comment_title"},
            new String[] {"update-interval", "sidebar_config_comment_update_interval"},
            new String[] {"lines", "sidebar_config_comment_lines"},
            new String[] {"world-blacklist", "sidebar_config_comment_world_blacklist"},
            new String[] {"default-enabled", "sidebar_config_comment_default_enabled"});

    @TempDir
    Path tempDir;

    private File file() {
        return new File(tempDir.toFile(), "config/sidebar.yml");
    }

    private String read() throws IOException {
        return new String(Files.readAllBytes(file().toPath()), StandardCharsets.UTF_8);
    }

    private SideBarConfig load(String language) throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        UltiToolsPlugin plugin = Mockito.mock(UltiToolsPlugin.class, invocation -> {
            String name = invocation.getMethod().getName();
            if ("getConfigFolder".equals(name)) {
                return tempDir.toString();
            }
            if ("getConfigFile".equals(name)) {
                return new File(tempDir.toFile(), invocation.<String>getArgument(0));
            }
            if ("i18n".equals(name)) {
                return CatalogueText.answer(language).answer(invocation);
            }
            if ("getPluginName".equals(name)) {
                return "UltiSideBar";
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        SideBarConfig config = new SideBarConfig();
        config.init(plugin);
        return config;
    }

    /** The comment line directly above {@code key:} in the file text, without its leading {@code # }. */
    private static String commentAbove(String text, String key) {
        String[] lines = text.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].startsWith(key + ":")) {
                String above = lines[i - 1];
                return above.startsWith("# ") ? above.substring(2) : above;
            }
        }
        throw new AssertionError("no line for key " + key + " in:\n" + text);
    }

    @Test
    @DisplayName("a fresh install under language: en writes the English comment above every key")
    void freshInstallWritesEnglishComments() throws IOException {
        load("en");

        String text = read();
        for (String[] key : KEYS) {
            assertThat(commentAbove(text, key[0])).as("comment above " + key[0])
                    .isEqualTo(CatalogueText.text("en", key[1]));
        }
        assertThat(text).as("no Chinese comment is written").doesNotContainPattern("#.*[\\u4e00-\\u9fff]");
    }

    @Test
    @DisplayName("a fresh install under language: zh writes the Chinese comment above every key")
    void freshInstallWritesChineseComments() throws IOException {
        load("zh");

        String text = read();
        for (String[] key : KEYS) {
            assertThat(commentAbove(text, key[0])).as("comment above " + key[0])
                    .isEqualTo(CatalogueText.text("zh", key[1]));
        }
    }

    @Test
    @DisplayName("an upgrade: a file written with the Chinese comments gets the English ones, its values stay, and a second start leaves it byte for byte")
    void upgradeSwitchesTheCommentsAndKeepsTheValues() throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        StringBuilder old = new StringBuilder();
        old.append("# ").append(CatalogueText.text("zh", "sidebar_config_comment_enabled")).append("\nenabled: false\n");
        old.append("# ").append(CatalogueText.text("zh", "sidebar_config_comment_title")).append("\ntitle: 'My own title'\n");
        old.append("# ").append(CatalogueText.text("zh", "sidebar_config_comment_update_interval")).append("\nupdate-interval: 40\n");
        old.append("# ").append(CatalogueText.text("zh", "sidebar_config_comment_lines")).append("\nlines:\n- 'First'\n- 'Second'\n");
        old.append("# ").append(CatalogueText.text("zh", "sidebar_config_comment_world_blacklist")).append("\nworld-blacklist:\n- 'nether'\n");
        old.append("# ").append(CatalogueText.text("zh", "sidebar_config_comment_default_enabled")).append("\ndefault-enabled: false\n");
        Files.write(file().toPath(), old.toString().getBytes(StandardCharsets.UTF_8));

        SideBarConfig first = load("en");

        String afterFirst = read();
        for (String[] key : KEYS) {
            assertThat(commentAbove(afterFirst, key[0])).as("comment above " + key[0])
                    .isEqualTo(CatalogueText.text("en", key[1]));
        }
        assertThat(first.isEnabled()).isFalse();
        assertThat(first.getTitle()).isEqualTo("My own title");
        assertThat(first.getUpdateInterval()).isEqualTo(40);
        assertThat(first.getLines()).containsExactly("First", "Second");
        assertThat(first.getWorldBlacklist()).containsExactly("nether");
        assertThat(first.isDefaultEnabled()).isFalse();

        load("en");

        assertThat(read()).as("the second start").isEqualTo(afterFirst);
    }
}
