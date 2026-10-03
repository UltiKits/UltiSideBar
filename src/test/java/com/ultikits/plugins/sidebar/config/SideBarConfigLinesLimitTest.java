package com.ultikits.plugins.sidebar.config;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.exceptions.ConfigurationException;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UltiKits/UltiSideBar#31: more than fifteen {@code lines} are refused and named at load, by design.
 * The limit is declared with {@code @Size(min = 1, max = 15)}; the framework checks every
 * {@code @Size}/{@code @Range} bound while it loads the file and refuses the module, naming the field, the
 * count written and the bounds, without rewriting the operator's file. Nothing is dropped silently.
 * This pins that behaviour with the framework's real config init on a temporary folder.
 * <p>
 * 超过 15 行的 lines 在加载时被拒绝并点名（按设计）：框架拒绝加载本模块，并指出字段、行数和上限，不改写文件。
 */
@DisplayName("sidebar.yml refuses more than fifteen lines at load (#31)")
class SideBarConfigLinesLimitTest {

    @TempDir
    Path tempDir;

    private File file() {
        return new File(tempDir.toFile(), "config/sidebar.yml");
    }

    private SideBarConfig load() throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        UltiToolsPlugin plugin = Mockito.mock(UltiToolsPlugin.class, invocation -> {
            String name = invocation.getMethod().getName();
            if ("getConfigFolder".equals(name)) {
                return tempDir.toString();
            }
            if ("getConfigFile".equals(name)) {
                return new File(tempDir.toFile(), invocation.<String>getArgument(0));
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

    private void writeLines(int count) throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        StringBuilder text = new StringBuilder("lines:\n");
        for (int i = 1; i <= count; i++) {
            text.append("  - 'line ").append(i).append("'\n");
        }
        Files.write(file().toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("sixteen lines refuse the module, naming the module, the field, the count and the bounds, and leave the file as written")
    void sixteenLinesAreRefusedAndNamed() throws IOException {
        writeLines(16);
        byte[] before = Files.readAllBytes(file().toPath());

        assertThatThrownBy(this::load).isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Module 'UltiSideBar' refused to load")
                .hasMessageContaining("field 'lines' size 16 is out of bounds [1, 15]")
                .hasMessageContaining("The file was not modified");

        assertThat(Files.readAllBytes(file().toPath())).as("the operator's file").isEqualTo(before);
    }

    @Test
    @DisplayName("control: exactly fifteen lines load, and all fifteen are kept")
    void fifteenLinesLoad() throws IOException {
        writeLines(15);

        SideBarConfig config = load();

        assertThat(config.getLines()).hasSize(15).startsWith("line 1").endsWith("line 15");
    }
}
