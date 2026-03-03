package com.anticheat.alert;

import com.anticheat.AntiCheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages staff alerts and punishments.
 */
public class AlertManager {

    private final AntiCheatPlugin plugin;
    private final Set<UUID> alertsDisabled = ConcurrentHashMap.newKeySet();
    private int alertThreshold;

    public AlertManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        alertThreshold = plugin.getConfig().getInt("alerts.alert-threshold", 3);
    }

    /**
     * Send an alert to all staff members.
     */
    public void alert(Player player, String checkName, double vl, String details) {
        if (!plugin.getConfig().getBoolean("alerts.staff-alerts", true)) return;
        if (vl < alertThreshold) return;

        String message = String.format("§8[§cAntiCheat§8] §f%s §7failed §c%s §7(VL: §f%.1f§7) §8[%s]",
                player.getName(), checkName, vl, details);

        // Log to console
        if (plugin.getConfig().getBoolean("alerts.console-log", true)) {
            plugin.getLogger().log(Level.WARNING, "[Alert] " + player.getName()
                    + " failed " + checkName + " VL=" + String.format("%.1f", vl)
                    + " | " + details);
        }

        // Send to staff players
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("anticheat.staff") && !alertsDisabled.contains(staff.getUniqueId())) {
                staff.sendMessage(message);
            }
        }

        // Discord webhook (async)
        String webhookUrl = plugin.getConfig().getString("alerts.discord-webhook", "");
        if (!webhookUrl.isBlank()) {
            String webhookMsg = message;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sendDiscordWebhook(webhookUrl, webhookMsg));
        }
    }

    /**
     * Execute punishment for a player exceeding violation threshold.
     */
    public void punish(Player player, String checkName) {
        String action = plugin.getConfig().getString("punishments.action", "WARN");
        String command = plugin.getConfig().getString("punishments.command",
                "kick %player% [AntiCheat] Unfair advantage detected");

        switch (action.toUpperCase()) {
            case "KICK" -> Bukkit.getScheduler().runTask(plugin, () ->
                    player.kickPlayer("§cKicked by AntiCheat: " + checkName));
            case "BAN" -> Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            command.replace("%player%", player.getName())));
            case "COMMAND" -> Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            command.replace("%player%", player.getName())));
            case "WARN" -> {
                player.sendMessage("§cAntiCheat: You have been flagged for " + checkName);
                plugin.getLogger().warning("WARN: " + player.getName() + " flagged for " + checkName);
            }
        }
    }

    public void toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (alertsDisabled.contains(uuid)) {
            alertsDisabled.remove(uuid);
            player.sendMessage("§aAntiCheat alerts §fenabled§a.");
        } else {
            alertsDisabled.add(uuid);
            player.sendMessage("§cAntiCheat alerts §fdisabled§c.");
        }
    }

    private void sendDiscordWebhook(String url, String message) {
        // Strip color codes for Discord
        String clean = message.replaceAll("§[0-9a-fk-or]", "");
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            String json = "{\"content\": \"" + clean.replace("\"", "\\\"") + "\"}";
            try (var os = conn.getOutputStream()) {
                os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            conn.getResponseCode(); // Complete the request
            conn.disconnect();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
        }
    }
}
