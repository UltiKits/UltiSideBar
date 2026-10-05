package com.ultikits.plugins.sidebar.service;

import com.ultikits.plugins.sidebar.UltiSideBarTestHelper;
import com.ultikits.plugins.sidebar.config.SideBarConfig;
import com.ultikits.plugins.sidebar.data.SideBarPreference;
import com.ultikits.ultitools.interfaces.DataOperator;
import com.ultikits.ultitools.interfaces.Query;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UltiKits/UltiSideBar#30: the sidebar keeps one private scoreboard per player and updates it in place,
 * so another plugin that changes that board while the player views it must not leave the sidebar broken,
 * and the sidebar's own line change must not reach another plugin's scores on the same board.
 * <p>
 * The board is a stateful fake with the vanilla meaning of {@code Scoreboard#resetScores(entry)} (clears the
 * entry on every objective), so each assertion reads what the board holds after the update.
 * <p>
 * 验证其他插件改动玩家的侧边栏计分板（移除目标、清空或占用显示位置、重置分数）后侧边栏会恢复，
 * 且内容变化只重置本模块自己的行，不影响其他插件在同一计分板上的分数。
 */
@DisplayName("SideBarService recovers when another plugin changes its board (#30)")
class SideBarServiceBoardRecoveryTest {

    private SideBarService service;
    private SideBarConfig config;
    private Player player;
    private UUID playerUuid;
    private FakeObjectives.Board board;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        UltiSideBarTestHelper.setUp();
        config = mock(SideBarConfig.class);
        lenient().when(config.isEnabled()).thenReturn(true);
        lenient().when(config.isDefaultEnabled()).thenReturn(true);
        lenient().when(config.getTitle()).thenReturn("&6&lTest Server");
        lenient().when(config.getUpdateInterval()).thenReturn(20);
        lenient().when(config.getLines()).thenReturn(Arrays.asList("Line 1", "Line 2"));
        lenient().when(config.getWorldBlacklist()).thenReturn(Collections.<String>emptyList());

        DataOperator<SideBarPreference> dataOperator = mock(DataOperator.class);
        Query<SideBarPreference> query = mock(Query.class);
        lenient().when(dataOperator.query()).thenReturn(query);
        lenient().when(query.where(anyString())).thenReturn(query);
        lenient().when(query.eq(org.mockito.ArgumentMatchers.any())).thenReturn(query);
        lenient().when(query.list()).thenReturn(Collections.<SideBarPreference>emptyList());

        service = new SideBarService();
        UltiSideBarTestHelper.setField(service, "plugin", UltiSideBarTestHelper.getMockPlugin());
        UltiSideBarTestHelper.setField(service, "config", config);
        UltiSideBarTestHelper.setField(service, "dataOperator", dataOperator);
        UltiSideBarTestHelper.setField(service, "bukkitPlugin", mock(org.bukkit.plugin.Plugin.class));

        playerUuid = UUID.randomUUID();
        player = UltiSideBarTestHelper.createMockPlayer("TestPlayer", playerUuid);

        board = FakeObjectives.board();
        org.bukkit.scoreboard.Scoreboard mainBoard = FakeScoreboards.board();
        ScoreboardManager manager = mock(ScoreboardManager.class);
        lenient().when(manager.getMainScoreboard()).thenReturn(mainBoard);
        lenient().when(manager.getNewScoreboard()).thenReturn(board.scoreboard);
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(manager);
        bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));
    }

    @AfterEach
    void tearDown() throws Exception {
        bukkitMock.close();
        UltiSideBarTestHelper.tearDown();
    }

    private Objective sidebar() {
        return board.objectives.get("sidebar");
    }

    @Test
    @DisplayName("control: enabling draws the objective in the sidebar slot with both lines")
    void enableDrawsTheSidebar() {
        service.enableSidebar(player);

        assertThat(sidebar()).isNotNull();
        assertThat(board.slots.get(DisplaySlot.SIDEBAR)).isSameAs(sidebar());
        assertThat(FakeObjectives.linesOf(board, sidebar())).containsExactly("Line 1", "Line 2");
    }

    @Test
    @DisplayName("another plugin unregisters the objective: the next update creates it again, in the slot, with every line")
    void objectiveRemovedIsRecreated() {
        service.enableSidebar(player);
        sidebar().unregister();
        assertThat(sidebar()).as("the other plugin removed it").isNull();

        service.updateSidebar(player);

        assertThat(sidebar()).isNotNull();
        assertThat(board.slots.get(DisplaySlot.SIDEBAR)).isSameAs(sidebar());
        assertThat(FakeObjectives.linesOf(board, sidebar())).containsExactly("Line 1", "Line 2");
    }

    @Test
    @DisplayName("another plugin clears the sidebar slot: the next update puts the objective back in it")
    void clearedSlotIsRestored() {
        service.enableSidebar(player);
        board.clearSlot(DisplaySlot.SIDEBAR);

        service.updateSidebar(player);

        assertThat(board.slots.get(DisplaySlot.SIDEBAR)).isSameAs(sidebar());
    }

    @Test
    @DisplayName("another plugin shows its own objective in the sidebar slot: the next update takes the slot back, and leaves that objective registered")
    void foreignObjectiveInTheSlotIsReplacedInTheSlotOnly() {
        service.enableSidebar(player);
        Objective foreign = board.register("other", "Other");
        foreign.setDisplaySlot(DisplaySlot.SIDEBAR);
        assertThat(board.slots.get(DisplaySlot.SIDEBAR)).isSameAs(foreign);

        service.updateSidebar(player);

        assertThat(board.slots.get(DisplaySlot.SIDEBAR)).isSameAs(sidebar());
        assertThat(board.objectives.get("other")).as("the other plugin's objective stays registered").isSameAs(foreign);
    }

    @Test
    @DisplayName("another plugin resets the scores on the board: the next update draws every line again, though the text did not change")
    void resetScoresAreDrawnAgain() {
        service.enableSidebar(player);
        board.resetEverywhere("Line 1");
        board.resetEverywhere("Line 2");
        assertThat(board.entriesOf(sidebar())).as("the other plugin reset them").isEmpty();

        service.updateSidebar(player);

        assertThat(FakeObjectives.linesOf(board, sidebar())).containsExactly("Line 1", "Line 2");
    }

    @Test
    @DisplayName("while the objective holds the slot and keeps its scores, an update sets neither the slot nor a score again")
    void anIntactBoardIsNotWrittenAgain() {
        service.enableSidebar(player);
        Objective objective = sidebar();
        org.mockito.Mockito.clearInvocations(objective);
        board.scoreWrites = 0;

        service.updateSidebar(player);
        service.updateSidebar(player);

        verify(objective, never()).setDisplaySlot(org.mockito.ArgumentMatchers.any());
        assertThat(board.scoreWrites).as("scores written by the two updates").isZero();
        assertThat(FakeObjectives.linesOf(board, objective)).containsExactly("Line 1", "Line 2");
    }

    @Test
    @DisplayName("a line change resets no entry board-wide: another plugin's score on the same name on another objective stays")
    void lineChangeLeavesAnotherObjectivesScoresAlone() {
        service.enableSidebar(player);
        Objective foreign = board.register("belowname", "Below name");
        board.put(foreign, "Line 1", 7);
        board.put(foreign, "Other plugin's entry", 3);

        lenient().when(config.getLines()).thenReturn(Arrays.asList("Line A", "Line 2"));
        service.updateSidebar(player);

        assertThat(FakeObjectives.linesOf(board, sidebar())).containsExactly("Line A", "Line 2");
        assertThat(board.scoreOf(foreign, "Line 1")).as("the other plugin's score on the old line's name").isEqualTo(7);
        assertThat(board.scoreOf(foreign, "Other plugin's entry")).isEqualTo(3);
        assertThat(board.boardWideResets).as("Scoreboard#resetScores calls").isZero();
    }

    @Test
    @DisplayName("with the line record cleared, a line change still removes this objective's own old lines and no other objective's score")
    void clearedCacheStillRemovesOnlyOwnOldLines() {
        service.enableSidebar(player);
        Objective foreign = board.register("belowname", "Below name");
        board.put(foreign, "Line 1", 7);

        service.clearCache();
        lenient().when(config.getLines()).thenReturn(Arrays.asList("Line A", "Line B"));
        service.updateSidebar(player);

        assertThat(FakeObjectives.linesOf(board, sidebar())).containsExactly("Line A", "Line B");
        assertThat(board.scoreOf(foreign, "Line 1")).isEqualTo(7);
        assertThat(board.boardWideResets).isZero();
    }
}
