package com.anticheat.manager;

import com.anticheat.AntiCheatPlugin;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages bypass logic.
 * Players with OP or the anticheat.bypass permission are completely ignored.
 * This check is done as early as possible for performance.
 */
public class BypassManager {

    private final AntiCheatPlugin plugin;
    private boolean bypassOperators;
    private final Set<UUID> manualBypass = ConcurrentHashMap.newKeySet();

    public BypassManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.bypassOperators = plugin.getConfig().getBoolean("general.bypass-operators", true);
    }

    /**
     * Returns true if this player should be completely bypassed by the anti-cheat.
     * This must be called as the FIRST check in every detection path.
     *
     * @param player the player to check
     * @return true if bypassed, false if should be checked
     */
    public boolean isBypassed(Player player) {
        // Check manual UUID bypass first (cheapest check)
        if (manualBypass.contains(player.getUniqueId())) {
            return true;
        }
        // Check OP bypass (configurable)
        if (bypassOperators && player.isOp()) {
            return true;
        }
        // Check permission node bypass
        if (player.hasPermission("anticheat.bypass")) {
            return true;
        }
        return false;
    }

    /**
     * Manually add a player to the bypass list (e.g. during teleport, event, etc.)
     */
    public void addBypass(UUID uuid) {
        manualBypass.add(uuid);
    }

    /**
     * Remove a player from manual bypass.
     */
    public void removeBypass(UUID uuid) {
        manualBypass.remove(uuid);
    }

    public boolean isBypassOperators() {
        return bypassOperators;
    }
}
