package com.ultikits.plugins.sidebar.service;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * A scoreboard that keeps its objectives, display slots and scores, with the vanilla meaning of
 * {@code resetScores(entry)}: it clears that entry on every objective of the board, another plugin's
 * included. {@code Score#resetScore()} clears the one objective's score only. Lets a test assert what a
 * board holds after the code under test ran, and what another plugin's objective on the same board
 * still holds.
 */
final class FakeObjectives {

    private FakeObjectives() {
    }

    /** The state behind a fake board, so a test can play another plugin. */
    static final class Board {
        final Scoreboard scoreboard = FakeScoreboards.board();
        final Map<String, Objective> objectives = new LinkedHashMap<>();
        final Map<DisplaySlot, Objective> slots = new LinkedHashMap<>();
        private final Map<Objective, Map<String, Integer>> scores = new LinkedHashMap<>();
        /** How many times {@code Scoreboard#resetScores(String)} ran: the board-wide reset. */
        int boardWideResets;
        /** How many times a score was written, by anyone. */
        int scoreWrites;

        Objective register(String name, String displayName) {
            if (objectives.containsKey(name)) {
                throw new IllegalArgumentException("An objective of name '" + name + "' already exists");
            }
            Objective objective = mock(Objective.class);
            Map<String, Integer> own = new LinkedHashMap<>();
            scores.put(objective, own);
            objectives.put(name, objective);
            String[] title = {displayName};
            lenient().when(objective.getName()).thenReturn(name);
            lenient().when(objective.getDisplayName()).thenAnswer(inv -> title[0]);
            lenient().doAnswer(inv -> {
                title[0] = inv.getArgument(0);
                return null;
            }).when(objective).setDisplayName(anyString());
            lenient().doAnswer(inv -> {
                DisplaySlot slot = inv.getArgument(0);
                slots.values().removeIf(held -> held == objective);
                if (slot != null) {
                    slots.put(slot, objective);
                }
                return null;
            }).when(objective).setDisplaySlot(any());
            lenient().when(objective.getScore(anyString())).thenAnswer(inv -> score(objective, inv.<String>getArgument(0)));
            lenient().doAnswer(inv -> {
                objectives.remove(name);
                slots.values().removeIf(held -> held == objective);
                scores.remove(objective);
                return null;
            }).when(objective).unregister();
            return objective;
        }

        private Score score(Objective objective, String entry) {
            Score score = mock(Score.class);
            lenient().when(score.getEntry()).thenReturn(entry);
            lenient().when(score.isScoreSet()).thenAnswer(inv -> scores.containsKey(objective)
                    && scores.get(objective).containsKey(entry));
            lenient().when(score.getScore()).thenAnswer(inv -> scores.containsKey(objective)
                    ? scores.get(objective).getOrDefault(entry, 0) : 0);
            lenient().doAnswer(inv -> {
                scores.get(objective).put(entry, inv.<Integer>getArgument(0));
                scoreWrites++;
                return null;
            }).when(score).setScore(org.mockito.ArgumentMatchers.anyInt());
            lenient().doAnswer(inv -> {
                if (scores.containsKey(objective)) {
                    scores.get(objective).remove(entry);
                }
                return null;
            }).when(score).resetScore();
            return score;
        }

        /** What another plugin does with {@code objective.getScore(entry).setScore(value)}. */
        void put(Objective objective, String entry, int value) {
            scores.get(objective).put(entry, value);
        }

        /** The score the objective holds for the entry, or {@code null} when it holds none. */
        Integer scoreOf(Objective objective, String entry) {
            Map<String, Integer> own = scores.get(objective);
            return own == null ? null : own.get(entry);
        }

        /** The entries the objective holds a score for. */
        Set<String> entriesOf(Objective objective) {
            Map<String, Integer> own = scores.get(objective);
            return own == null ? new HashSet<String>() : new HashSet<>(own.keySet());
        }

        /** What another plugin does with {@code scoreboard.resetScores(entry)}. */
        void resetEverywhere(String entry) {
            for (Map<String, Integer> own : scores.values()) {
                own.remove(entry);
            }
        }

        /** What another plugin does with {@code scoreboard.clearSlot(slot)}. */
        void clearSlot(DisplaySlot slot) {
            slots.remove(slot);
        }

        Set<String> allEntries() {
            Set<String> all = new HashSet<>();
            for (Map<String, Integer> own : scores.values()) {
                all.addAll(own.keySet());
            }
            return all;
        }
    }

    /** A board with the objectives, slots and scores behaviour, on top of {@link FakeScoreboards#board()}. */
    static Board board() {
        final Board board = new Board();
        Scoreboard scoreboard = board.scoreboard;
        lenient().when(scoreboard.registerNewObjective(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> board.register(inv.<String>getArgument(0), inv.<String>getArgument(2)));
        lenient().when(scoreboard.getObjective(anyString()))
                .thenAnswer(inv -> board.objectives.get(inv.<String>getArgument(0)));
        lenient().when(scoreboard.getObjective(any(DisplaySlot.class)))
                .thenAnswer(inv -> board.slots.get(inv.<DisplaySlot>getArgument(0)));
        lenient().when(scoreboard.getObjectives()).thenAnswer(inv -> new HashSet<>(board.objectives.values()));
        lenient().when(scoreboard.getEntries()).thenAnswer(inv -> board.allEntries());
        lenient().doAnswer(inv -> {
            board.boardWideResets++;
            board.resetEverywhere(inv.<String>getArgument(0));
            return null;
        }).when(scoreboard).resetScores(anyString());
        return board;
    }

    /** The names of the lines on the objective, highest score first. */
    static List<String> linesOf(Board board, Objective objective) {
        Map<Integer, String> byScore = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        for (String entry : board.entriesOf(objective)) {
            byScore.put(board.scoreOf(objective, entry), entry);
        }
        return new ArrayList<>(byScore.values());
    }
}
