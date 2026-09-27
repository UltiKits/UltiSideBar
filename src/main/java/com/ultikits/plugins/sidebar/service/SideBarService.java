package com.ultikits.plugins.sidebar.service;

import com.ultikits.plugins.sidebar.config.ConfigTextDefaults;
import com.ultikits.plugins.sidebar.config.SideBarConfig;
import com.ultikits.plugins.sidebar.data.SideBarPreference;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.Autowired;
import com.ultikits.ultitools.annotations.Service;
import com.ultikits.ultitools.interfaces.ConfigChangeListener;
import com.ultikits.ultitools.interfaces.DataOperator;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing player sidebars.
 *
 * @author wisdomme
 * @version 1.0.0
 */
@Service
public class SideBarService {
    
    @Autowired
    private UltiToolsPlugin plugin;

    @Autowired
    private SideBarConfig config;
    
    /** The module whose sidebar shares the player's sidebar slot with this one. */
    private static final String OTHER_SIDEBAR_MODULE = "UltiEssentials";

    /** That module's configuration file, relative to its module folder. */
    private static final String OTHER_SIDEBAR_CONFIG = "config/essentials.yml";

    /** That module's switch for its sidebar. */
    private static final String OTHER_SIDEBAR_KEY = "features.scoreboard.enabled";

    /** Longest entry text a scoreboard line keeps (the limit older clients enforce). */
    private static final int MAX_ENTRY_LENGTH = 40;

    // Track player scoreboard state
    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    
    // Content cache for performance optimization (avoid unnecessary scoreboard updates)
    private final Map<UUID, List<String>> contentCache = new ConcurrentHashMap<>();
    
    // Data operator for persistent storage
    private DataOperator<SideBarPreference> dataOperator;
    
    // Update task
    private BukkitTask updateTask;

    // Bukkit plugin instance for scheduler calls
    private Plugin bukkitPlugin;

    // The config change listener init() registered, removed again by shutdown()
    private ConfigChangeListener changeListener;

    // PlaceholderAPI availability
    private boolean placeholderApiAvailable = false;

    /**
     * Initialize the sidebar service.
     */
    public void init() {
        // Initialize data operator for persistent storage
        dataOperator = plugin.getDataOperator(SideBarPreference.class);
        bukkitPlugin = Bukkit.getPluginManager().getPlugin("UltiTools");

        // One-time migration (issue #13; extended by PR #15 to also cover the legacy 12-hour
        // server-time line): AbstractConfigEntity.init() -- which has already run by this point,
        // via UltiToolsPlugin's constructor -- only fills keys that are MISSING from the
        // persisted file and never overwrites an existing "lines" value, so a server that has
        // ever started an older version of this plugin keeps every stale shipped default (the
        // invalid %world_name% line, the ambiguous %server_time_hh:mm:ss% line) forever without
        // this explicit, exact-match rewrite. Runs again on every reload() (this method is also
        // called from reload()), which is harmless: once migrated, the exact-match check finds nothing
        // left to rewrite.
        // The same pass writes the title and lines in the server's language while they are still
        // built-in text (maintainer decision 2026-09-25): this method runs from registerSelf() and from
        // onReload() via reload(), both after the framework's language setting is loaded -- never from
        // the change listener below, which the framework fires before it reloads the language. The text
        // comes from this jar's own catalogue, not from plugin.i18n (which reads the operator's extracted
        // language file first), so every value written is one the next pass recognises.
        if (config.materializeText(ConfigTextDefaults.jarLanguage(SideBarConfig.class,
                plugin.getLanguageCode())::getLocalizedText)) {
            try {
                config.save();
            } catch (IOException e) {
                plugin.getLogger().warn(plugin.i18n("sidebar_log_defaults_save_failed")
                        .replace("{ERROR}", String.valueOf(e.getMessage())));
            }
        }

        // Check PlaceholderAPI
        placeholderApiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (!placeholderApiAvailable) {
            plugin.getLogger().warn(plugin.i18n("sidebar_log_papi_missing"));
        }
        
        // Register the config change listener. reload() runs shutdown() and then init(), and
        // shutdown() removes the listener this method added, so exactly one stays registered however
        // often the module reloads (UltiKits/UltiSideBar#21).
        if (changeListener != null) {
            config.removeChangeListener(changeListener);
        }
        changeListener = cfg -> {
            clearCache();
            refreshAllSidebars();
        };
        config.addChangeListener(changeListener);
        
        startUpdateTask();
        
        // Initialize for online players. The stored preference decides; default-enabled applies only
        // to a player with no stored preference, which isSidebarEnabledInDatabase already encodes
        // (UltiKits/UltiSideBar#20).
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSidebarEnabledInDatabase(player.getUniqueId())) {
                enableSidebar(player);
            }
        }
    }
    
    /**
     * Shutdown the service.
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        if (changeListener != null) {
            config.removeChangeListener(changeListener);
            changeListener = null;
        }
        
        // Remove all scoreboards
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeSidebar(player);
        }
        
        playerScoreboards.clear();
        contentCache.clear();
    }
    
    /**
     * Reload configuration and refresh all sidebars.
     */
    public void reload() {
        shutdown();
        init();
    }
    
    /**
     * Clear content cache (called on config reload).
     */
    public void clearCache() {
        contentCache.clear();
    }
    
    /**
     * Refresh all online players' sidebars.
     */
    private void refreshAllSidebars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSidebarEnabled(player)) {
                removeSidebar(player);
                enableSidebar(player);
            }
        }
    }
    
    /**
     * Start the update task.
     */
    private void startUpdateTask() {
        if (!config.isEnabled()) {
            return;
        }
        
        updateTask = Bukkit.getScheduler().runTaskTimer(
            bukkitPlugin,
            this::updateAllSidebars,
            0L, config.getUpdateInterval()
        );
    }
    
    /**
     * Update all player sidebars.
     */
    private void updateAllSidebars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isSidebarEnabled(player)) {
                continue;
            }
            Scoreboard own = playerScoreboards.get(player.getUniqueId());
            if (own != null && own.equals(player.getScoreboard())) {
                mirrorMainTeams(own);
                updateSidebar(player);
            } else if (!isSlotTakenByAnother(player)) {
                // The slot is free again (another scoreboard was put away): show this sidebar.
                showSidebar(player);
            }
            // Otherwise another scoreboard holds the slot: leave it on screen (UltiKits/UltiSideBar#26).
        }
    }

    /**
     * Copies the main scoreboard's teams onto a private sidebar board, so name prefixes and every
     * other main-board team stay visible to the player viewing it (UltiKits/UltiSideBar#27).
     */
    private void mirrorMainTeams(Scoreboard board) {
        if (board == null) {
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            MainTeamMirror.mirror(manager.getMainScoreboard(), board);
        }
    }
    
    /**
     * Enable sidebar for player.
     */
    public void enableSidebar(Player player) {
        if (!config.isEnabled()) {
            return;
        }
        
        // Update database
        setSidebarEnabledInDatabase(player.getUniqueId(), true);
        
        // Check world blacklist
        if (config.getWorldBlacklist().contains(player.getWorld().getName())) {
            return;
        }
        
        showSidebar(player);
    }

    /**
     * Shows this module's sidebar board to the player, unless another scoreboard holds the slot.
     * <p>
     * The slot is free when the player views the server's main scoreboard or this module's own
     * board. When another plugin's scoreboard is on screen (UltiEssentials' sidebar, for example),
     * the first one shown keeps it and this method changes nothing; the update loop shows this
     * sidebar once the slot is free again (maintainer decision 2026-09-27, UltiKits/UltiSideBar#26).
     */
    private void showSidebar(Player player) {
        if (isSlotTakenByAnother(player)) {
            return;
        }
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("sidebar", "dummy",
                ChatColor.translateAlternateColorCodes('&', config.getTitle()));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            playerScoreboards.put(player.getUniqueId(), scoreboard);
        }
        mirrorMainTeams(scoreboard);
        if (!scoreboard.equals(player.getScoreboard())) {
            player.setScoreboard(scoreboard);
        }

        updateSidebar(player);
    }

    /**
     * Whether another plugin's scoreboard holds the player's sidebar slot: the player views neither
     * the server's main scoreboard nor this module's own board.
     *
     * @param player the player
     * @return {@code true} if this module's sidebar yields to the scoreboard on screen
     */
    public boolean isSlotTakenByAnother(Player player) {
        Scoreboard current = player.getScoreboard();
        if (current == null || current.equals(playerScoreboards.get(player.getUniqueId()))) {
            return false;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        return manager == null || !current.equals(manager.getMainScoreboard());
    }
    
    /**
     * Disable sidebar for player.
     */
    public void disableSidebar(Player player) {
        setSidebarEnabledInDatabase(player.getUniqueId(), false);
        removeSidebar(player);
    }
    
    /**
     * Remove sidebar from player.
     */
    public void removeSidebar(Player player) {
        Scoreboard own = playerScoreboards.remove(player.getUniqueId());
        contentCache.remove(player.getUniqueId());
        
        // Return the player to the main scoreboard only while this module's board is on screen:
        // another plugin's scoreboard stays where it is (UltiKits/UltiSideBar#26).
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null && own != null && own.equals(player.getScoreboard())) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }
    
    /**
     * Check if sidebar is enabled for player.
     */
    public boolean isSidebarEnabled(Player player) {
        return config.isEnabled() && 
               isSidebarEnabledInDatabase(player.getUniqueId()) &&
               !config.getWorldBlacklist().contains(player.getWorld().getName());
    }
    
    /**
     * Check if sidebar is enabled in database for player.
     * Returns true if not found (default enabled based on config).
     */
    private boolean isSidebarEnabledInDatabase(UUID playerUuid) {
        List<SideBarPreference> prefs = dataOperator.query()
            .where("player_uuid")
            .eq(playerUuid.toString())
            .list();

        if (prefs.isEmpty()) {
            return config.isDefaultEnabled();
        }

        SideBarPreference pref = selectCanonicalPreference(prefs);
        if (pref == null) {
            return config.isDefaultEnabled();
        }

        Boolean enabled = pref.getEnabled();
        return enabled != null ? enabled : config.isDefaultEnabled();
    }

    /**
     * Select a deterministic row when duplicate preference rows exist for a player.
     * <p>
     * The ORM does not guarantee query order here, so using list order makes duplicate
     * rows first-row-wins and can flip sidebar state between reads. Prefer the stable
     * entity id as the canonical row without deleting or migrating duplicate rows.
     * </p>
     */
    private SideBarPreference selectCanonicalPreference(List<SideBarPreference> preferences) {
        return preferences.stream()
            .filter(Objects::nonNull)
            .min(Comparator.comparing(SideBarPreference::getId, Comparator.nullsLast(String::compareTo)))
            .orElse(null);
    }
    
    /**
     * Set sidebar enabled state in database.
     */
    private void setSidebarEnabledInDatabase(UUID playerUuid, boolean enabled) {
        List<SideBarPreference> existing = dataOperator.query()
            .where("player_uuid")
            .eq(playerUuid.toString())
            .list();

        if (existing.isEmpty()) {
            // Insert new record
            SideBarPreference pref = new SideBarPreference(playerUuid.toString(), enabled);
            dataOperator.insert(pref);
        } else {
            // Update the deterministic canonical row; do not delete or migrate duplicates.
            SideBarPreference pref = selectCanonicalPreference(existing);
            if (pref != null) {
                dataOperator.update("enabled", enabled, pref.getId());
            }
        }
    }
    
    /**
     * Toggle sidebar for player.
     * 
     * @return true if now enabled
     */
    public boolean toggleSidebar(Player player) {
        if (isSidebarEnabledInDatabase(player.getUniqueId())) {
            disableSidebar(player);
            return false;
        } else {
            enableSidebar(player);
            return true;
        }
    }
    
    /**
     * Update sidebar for player.
     */
    public void updateSidebar(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            return;
        }
        
        Objective objective = scoreboard.getObjective("sidebar");
        if (objective == null) {
            return;
        }
        
        // Update title
        String title = parsePlaceholders(player, config.getTitle());
        try {
            objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
        } catch (Exception ignored) {
            // Ignore title too long errors
        }
        
        // Get lines
        List<String> lines = config.getLines();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        
        // Parse and build new content
        List<String> newContent = new ArrayList<>();
        Set<String> usedEntries = new HashSet<>();
        
        for (String line : lines) {
            String parsed = parsePlaceholders(player, line);
            parsed = ChatColor.translateAlternateColorCodes('&', parsed);
            
            newContent.add(uniqueEntry(parsed, usedEntries));
        }
        
        // Check if content changed (performance optimization)
        List<String> cachedContent = contentCache.get(player.getUniqueId());
        if (cachedContent != null && cachedContent.equals(newContent)) {
            return; // No change, skip update
        }
        
        // Update cache
        contentCache.put(player.getUniqueId(), new ArrayList<>(newContent));
        
        // Clear old entries
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }
        
        // Add new entries (reverse order for correct display)
        int score = newContent.size();
        for (String entry : newContent) {
            try {
                objective.getScore(entry).setScore(score--);
            } catch (Exception e) {
                // Ignore invalid entries
            }
        }
    }
    
    /**
     * Truncates a rendered line to the entry length limit first and only then makes it unique
     * against the entries already used, so two lines that differ only after the limit still become
     * two entries (UltiKits/UltiSideBar#17). A duplicate gets invisible reset codes appended, and
     * enough of its text is cut to keep the whole entry within the limit.
     */
    static String uniqueEntry(String rendered, Set<String> usedEntries) {
        String base = truncate(rendered, MAX_ENTRY_LENGTH);
        String candidate = base;
        String marker = "";
        while (usedEntries.contains(candidate)) {
            marker = marker + ChatColor.RESET;
            candidate = truncate(base, MAX_ENTRY_LENGTH - marker.length()) + marker;
        }
        usedEntries.add(candidate);
        return candidate;
    }

    /**
     * Cuts text to at most {@code max} characters, dropping a trailing colour-code character that
     * the cut would leave without its code.
     */
    private static String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        String cut = text.substring(0, Math.max(0, max));
        if (cut.endsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut;
    }

    /**
     * Parse PlaceholderAPI placeholders.
     */
    private String parsePlaceholders(Player player, String text) {
        if (placeholderApiAvailable) {
            try {
                return PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception e) {
                return text;
            }
        }
        return text;
    }
    
    /**
     * Schedules the one start-up notice for a server that also runs UltiEssentials' sidebar. It runs
     * on the first server tick, after every module has been loaded, whatever their load order.
     */
    public void scheduleOtherSidebarNotice() {
        Bukkit.getScheduler().runTask(bukkitPlugin, this::reportOtherSidebar);
    }

    /**
     * Logs one line when this module's sidebar and UltiEssentials' sidebar are both enabled: each
     * player keeps whichever is shown first, and the other one waits (UltiKits/UltiSideBar#26).
     */
    private void reportOtherSidebar() {
        if (!config.isEnabled()) {
            return;
        }
        for (UltiToolsPlugin module : UltiToolsPlugin.getPluginManager().getPluginList()) {
            if (OTHER_SIDEBAR_MODULE.equals(module.getPluginName())
                    && isOtherSidebarEnabled(module.getResourceFolderPath())) {
                plugin.getLogger().info(plugin.i18n("sidebar_log_other_sidebar"));
                return;
            }
        }
    }

    /**
     * Reads UltiEssentials' own switch for its sidebar from that module's configuration file; the
     * switch defaults to on, as it ships.
     */
    private static boolean isOtherSidebarEnabled(String moduleFolder) {
        File file = new File(moduleFolder, OTHER_SIDEBAR_CONFIG);
        if (!file.isFile()) {
            return true;
        }
        return YamlConfiguration.loadConfiguration(file).getBoolean(OTHER_SIDEBAR_KEY, true);
    }

    /**
     * Handle player join.
     */
    public void onPlayerJoin(Player player) {
        if (isSidebarEnabledInDatabase(player.getUniqueId())) {
            // Delay to allow other plugins to load. A player who quits within the delay has already
            // been cleaned up by onPlayerQuit; enabling them anyway would leave a board and a cache
            // entry that nothing removes again (UltiKits/UltiSideBar#19).
            Bukkit.getScheduler().runTaskLater(
                bukkitPlugin,
                () -> {
                    if (player.isOnline()) {
                        enableSidebar(player);
                    }
                },
                10L
            );
        }
    }
    
    /**
     * Handle player quit.
     */
    public void onPlayerQuit(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        contentCache.remove(player.getUniqueId());
    }
    
    /**
     * Handle world change.
     */
    public void onWorldChange(Player player) {
        if (config.getWorldBlacklist().contains(player.getWorld().getName())) {
            removeSidebar(player);
        } else if (isSidebarEnabled(player) && !playerScoreboards.containsKey(player.getUniqueId())) {
            enableSidebar(player);
        }
    }
}
