package com.anticheat;

import com.anticheat.alert.AlertManager;
import com.anticheat.listener.PacketListener;
import com.anticheat.listener.PlayerListener;
import com.anticheat.manager.BypassManager;
import com.anticheat.manager.CheckManager;
import com.anticheat.manager.PlayerDataManager;
import com.anticheat.util.TpsUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class AntiCheatPlugin extends JavaPlugin {

    private static AntiCheatPlugin instance;

    private BypassManager bypassManager;
    private CheckManager checkManager;
    private PlayerDataManager playerDataManager;
    private AlertManager alertManager;
    private TpsUtil tpsUtil;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!getConfig().getBoolean("general.enabled", true)) {
            getLogger().warning("AntiCheat is disabled in config.yml. Shutting down.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize utilities first
        this.tpsUtil = new TpsUtil(this);

        // Initialize managers
        this.bypassManager = new BypassManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.checkManager = new CheckManager(this);
        this.alertManager = new AlertManager(this);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register ProtocolLib packet listener
        new PacketListener(this).register();

        getLogger().info("AntiCheat v" + getDescription().getVersion() + " enabled successfully.");
        getLogger().info("Server type: " + getConfig().getString("general.server-type", "BOXPVP"));
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.cleanup();
        }
        getLogger().info("AntiCheat disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage("§6AntiCheat §7v" + getDescription().getVersion());
            sender.sendMessage("§7/ac reload §8- §fReload configuration");
            sender.sendMessage("§7/ac alerts §8- §fToggle alert messages");
            sender.sendMessage("§7/ac debug §8- §fToggle debug mode");
            sender.sendMessage("§7/ac violations <player> §8- §fView player violations");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                reloadConfig();
                bypassManager.reload();
                checkManager.reload();
                sender.sendMessage("§aAntiCheat configuration reloaded.");
            }
            case "alerts" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can toggle alerts.");
                    return true;
                }
                alertManager.toggleAlerts(player);
            }
            case "debug" -> {
                boolean debug = !getConfig().getBoolean("general.debug", false);
                getConfig().set("general.debug", debug);
                sender.sendMessage("§aDebug mode: §f" + (debug ? "enabled" : "disabled"));
            }
            case "violations" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /ac violations <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                playerDataManager.printViolations(sender, target);
            }
            default -> sender.sendMessage("§cUnknown subcommand. Use /ac for help.");
        }
        return true;
    }

    public static AntiCheatPlugin getInstance() {
        return instance;
    }

    public BypassManager getBypassManager() {
        return bypassManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public TpsUtil getTpsUtil() {
        return tpsUtil;
    }

    public void debug(String message) {
        if (getConfig().getBoolean("general.debug", false)) {
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }
}
