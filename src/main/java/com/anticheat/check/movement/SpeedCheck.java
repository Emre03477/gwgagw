package com.anticheat.check.movement;

import com.anticheat.AntiCheatPlugin;
import com.anticheat.check.Check;
import com.anticheat.data.PlayerData;
import com.anticheat.util.TpsUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Speed check - detects players moving faster than allowed.
 * Timer-aware and TPS-adaptive.
 */
public class SpeedCheck extends Check {

    private static final String CHECK_NAME = "speed";
    private static final double BASE_SPEED_LIMIT = 0.35;
    private static final double SPRINT_BONUS = 0.13;
    private static final double SPEED_EFFECT_BONUS = 0.065;
    private static final double BUFFER_THRESHOLD = 3.0;
    private static final double BUFFER_DECAY = 0.005;

    private double speedLimit;

    public SpeedCheck(AntiCheatPlugin plugin) {
        super(plugin, CHECK_NAME, CheckType.MOVEMENT);
        reload();
    }

    @Override
    public void reload() {
        super.reload();
        speedLimit = plugin.getConfig().getDouble("checks.speed.speed-limit", BASE_SPEED_LIMIT);
    }

    /**
     * Process a movement event for this player.
     *
     * @param player    the moving player
     * @param data      player data
     * @param from      previous location
     * @param to        new location
     */
    public void process(Player player, PlayerData data, Location from, Location to) {
        if (!isEnabled()) return;

        // Skip creative/spectator - they have no speed limits
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;

        // Skip if flying (allowed)
        if (player.isFlying()) return;

        // Skip if in vehicle
        if (player.isInsideVehicle()) return;

        // Get server TPS for lag compensation
        double tps = plugin.getTpsUtil().getTps();
        if (tps < plugin.getConfig().getDouble("tps-thresholds.lag-threshold", 17.0)) {
            // Server is lagging, be lenient
            return;
        }

        // Calculate horizontal distance moved
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double deltaXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Calculate allowed speed
        double allowedSpeed = calculateAllowedSpeed(player);

        // Lag compensation based on ping
        double pingCompensation = Math.min(player.getPing() * 0.00015, 0.1);
        allowedSpeed += pingCompensation;

        if (deltaXZ > allowedSpeed) {
            double excess = deltaXZ - allowedSpeed;
            data.addBuffer(CHECK_NAME, excess * 2.0);

            if (data.getBuffer(CHECK_NAME) > BUFFER_THRESHOLD) {
                String detail = String.format("deltaXZ=%.4f allowed=%.4f excess=%.4f",
                        deltaXZ, allowedSpeed, excess);
                flag(player, data, 1.0, detail);
                data.setBuffer(CHECK_NAME, data.getBuffer(CHECK_NAME) * 0.5);
            }
        } else {
            // Clean movement - decay buffer and violations
            data.decayBuffer(CHECK_NAME, BUFFER_DECAY);
            reward(data, 0.01);
        }

        // Update player data
        data.setLastDeltaXZ(deltaXZ);
        data.setLastLocation(from);
    }

    private double calculateAllowedSpeed(Player player) {
        double allowed = speedLimit;

        // Sprint bonus
        if (player.isSprinting()) {
            allowed += SPRINT_BONUS;
        }

        // Speed potion effect
        var speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
        if (speedEffect != null) {
            allowed += (speedEffect.getAmplifier() + 1) * SPEED_EFFECT_BONUS;
        }

        // Soul speed boots (Nether)
        // Slime blocks, honey blocks handled elsewhere

        return allowed;
    }
}
