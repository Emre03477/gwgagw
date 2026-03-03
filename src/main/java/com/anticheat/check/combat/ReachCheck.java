package com.anticheat.check.combat;

import com.anticheat.AntiCheatPlugin;
import com.anticheat.check.Check;
import com.anticheat.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Reach check - validates that players cannot attack entities beyond normal distance.
 * Uses raytrace-like distance calculation with lag compensation.
 */
public class ReachCheck extends Check {

    private static final String CHECK_NAME = "reach";
    private static final double SURVIVAL_REACH = 3.0;
    private static final double CREATIVE_REACH = 5.0;
    private static final double BUFFER_THRESHOLD = 2.5;
    private static final double BUFFER_DECAY = 0.1;

    private double maxReach;
    private double creativeReach;

    public ReachCheck(AntiCheatPlugin plugin) {
        super(plugin, CHECK_NAME, CheckType.COMBAT);
        reload();
    }

    @Override
    public void reload() {
        super.reload();
        maxReach = plugin.getConfig().getDouble("checks.reach.max-reach", 3.1);
        creativeReach = plugin.getConfig().getDouble("checks.reach.creative-reach", 5.1);
    }

    /**
     * Process an attack, validating the reach distance.
     *
     * @param player    attacking player
     * @param data      player data
     * @param target    the attacked entity
     */
    public void process(Player player, PlayerData data, Entity target) {
        if (!isEnabled()) return;

        // Determine max allowed reach
        double allowed = (player.getGameMode() == GameMode.CREATIVE) ? creativeReach : maxReach;

        // Lag compensation: add extra reach based on ping
        double pingComp = Math.min(player.getPing() * 0.00025, 0.5);
        allowed += pingComp;

        // Calculate 3D distance from player eye to target
        double distance = getEyeDistance(player, target);

        if (distance > allowed) {
            double excess = distance - allowed;
            data.addBuffer(CHECK_NAME, excess);

            if (data.getBuffer(CHECK_NAME) > BUFFER_THRESHOLD) {
                String detail = String.format("dist=%.4f allowed=%.4f excess=%.4f ping=%d",
                        distance, allowed, excess, player.getPing());
                flag(player, data, 1.0, detail);
                data.setBuffer(CHECK_NAME, data.getBuffer(CHECK_NAME) * 0.75);
            }
        } else {
            data.decayBuffer(CHECK_NAME, BUFFER_DECAY);
            reward(data, 0.05);
        }
    }

    private double getEyeDistance(Player player, Entity target) {
        // Use eye location for more accurate calculation
        Vector eyePos = player.getEyeLocation().toVector();
        Vector targetCenter;

        if (target instanceof LivingEntity living) {
            // Use the living entity's eye location for better accuracy
            targetCenter = living.getEyeLocation().toVector();
        } else {
            targetCenter = target.getLocation().toVector().add(new Vector(0, target.getHeight() / 2.0, 0));
        }

        return eyePos.distance(targetCenter);
    }
}
