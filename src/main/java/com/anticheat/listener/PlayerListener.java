package com.anticheat.listener;

import com.anticheat.AntiCheatPlugin;
import com.anticheat.check.movement.SpeedCheck;
import com.anticheat.check.combat.KillAuraCheck;
import com.anticheat.check.combat.ReachCheck;
import com.anticheat.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final AntiCheatPlugin plugin;

    public PlayerListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().createData(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().removeData(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // BYPASS CHECK - must be first, before any other logic
        if (plugin.getBypassManager().isBypassed(player)) return;

        // Skip if no meaningful movement
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getZ() == event.getTo().getZ()
                && event.getFrom().getY() == event.getTo().getY()) {
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getData(player);
        if (data == null) return;

        // Update ground/air ticks
        if (player.isOnGround()) {
            data.setGroundTicks(data.getGroundTicks() + 1);
            data.setAirTicks(0);
            data.setLastGroundLocation(event.getTo());
        } else {
            data.setAirTicks(data.getAirTicks() + 1);
            data.setGroundTicks(0);
        }

        // Run checks
        for (var check : plugin.getCheckManager().getEnabledChecks()) {
            if (check instanceof SpeedCheck speedCheck) {
                speedCheck.process(player, data, event.getFrom(), event.getTo());
            }
        }

        data.setWasOnGround(player.isOnGround());
        data.setLastLocation(event.getTo());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        // BYPASS CHECK - must be first
        if (plugin.getBypassManager().isBypassed(player)) return;

        Entity target = event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().getData(player);
        if (data == null) return;

        // Run combat checks
        for (var check : plugin.getCheckManager().getEnabledChecks()) {
            if (check instanceof ReachCheck reachCheck) {
                reachCheck.process(player, data, target);
            }
            if (check instanceof KillAuraCheck killAuraCheck) {
                if (target instanceof org.bukkit.entity.LivingEntity) {
                    killAuraCheck.process(player, data, target);
                }
            }
        }

        data.setLastAttackTime(System.currentTimeMillis());
        data.incrementAttackCount();
    }
}
