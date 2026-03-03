package com.anticheat.check;

import com.anticheat.AntiCheatPlugin;
import com.anticheat.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Abstract base class for all anti-cheat checks.
 * Subclasses implement specific detection logic.
 */
public abstract class Check {

    protected final AntiCheatPlugin plugin;
    private final String name;
    private final CheckType type;
    private boolean enabled;
    protected double maxViolations;

    public Check(AntiCheatPlugin plugin, String name, CheckType type) {
        this.plugin = plugin;
        this.name = name;
        this.type = type;
        reload();
    }

    public void reload() {
        String path = "checks." + name.toLowerCase().replace(" ", "");
        this.enabled = plugin.getConfig().getBoolean(path + ".enabled", true);
        this.maxViolations = plugin.getConfig().getDouble(path + ".max-violations", 10.0);
    }

    /**
     * Add a violation to the player.
     *
     * @param player    the player
     * @param data      player data
     * @param amount    violation amount to add
     * @param details   human-readable detection details
     */
    protected void flag(Player player, PlayerData data, double amount, String details) {
        data.addViolation(name, amount);
        double vl = data.getViolationLevel(name);

        plugin.getAlertManager().alert(player, name, vl, details);

        if (vl >= maxViolations) {
            plugin.getAlertManager().punish(player, name);
            data.resetViolation(name);
        }
    }

    /**
     * Reward the player for clean movement by decaying violations.
     */
    protected void reward(PlayerData data, double decayAmount) {
        data.decayViolation(name, decayAmount);
    }

    public String getName() {
        return name;
    }

    public CheckType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public enum CheckType {
        MOVEMENT,
        COMBAT,
        PACKET,
        WORLD
    }
}
