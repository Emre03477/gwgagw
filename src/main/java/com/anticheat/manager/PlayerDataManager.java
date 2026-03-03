package com.anticheat.manager;

import com.anticheat.AntiCheatPlugin;
import com.anticheat.data.PlayerData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final AntiCheatPlugin plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public PlayerDataManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        // Load data for already online players (e.g. after reload)
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            createData(player);
        }
    }

    public PlayerData createData(Player player) {
        PlayerData data = new PlayerData(player);
        playerDataMap.put(player.getUniqueId(), data);
        return data;
    }

    public PlayerData getData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(player));
    }

    public PlayerData getData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void removeData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public Collection<PlayerData> getAllData() {
        return playerDataMap.values();
    }

    public void printViolations(CommandSender sender, Player target) {
        PlayerData data = getData(target);
        if (data == null) {
            sender.sendMessage("§cNo data found for " + target.getName());
            return;
        }
        Map<String, Double> violations = data.getAllViolations();
        if (violations.isEmpty()) {
            sender.sendMessage("§a" + target.getName() + " has no violations.");
            return;
        }
        sender.sendMessage("§6Violations for §f" + target.getName() + "§6:");
        violations.forEach((check, vl) ->
                sender.sendMessage("  §7" + check + "§8: §c" + String.format("%.2f", vl)));
    }

    public void cleanup() {
        playerDataMap.clear();
    }
}
