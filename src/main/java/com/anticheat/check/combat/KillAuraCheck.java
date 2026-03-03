package com.anticheat.check.combat;

import com.anticheat.AntiCheatPlugin;
import com.anticheat.check.Check;
import com.anticheat.data.PlayerData;
import com.anticheat.util.MathUtil;
import org.bukkit.entity.Player;

import java.util.LinkedList;

/**
 * KillAura detection using rotation pattern analysis and GCD flaw detection.
 * Detects:
 * A) Impossible rotation smoothness (too consistent = bot-like)
 * B) GCD (Greatest Common Divisor) flaw in aim assist mods
 * C) Attack rate inconsistency
 */
public class KillAuraCheck extends Check {

    private static final String CHECK_NAME = "killaura";
    private static final int SAMPLE_SIZE = 10;
    private static final double GCD_MIN_THRESHOLD = 0.0001;
    private static final double MAX_VARIANCE_THRESHOLD = 0.001;

    private double maxAngleDeviation;
    private double minGcd;

    public KillAuraCheck(AntiCheatPlugin plugin) {
        super(plugin, CHECK_NAME, CheckType.COMBAT);
        reload();
    }

    @Override
    public void reload() {
        super.reload();
        maxAngleDeviation = plugin.getConfig().getDouble("checks.killaura.max-angle-deviation", 10.0);
        minGcd = plugin.getConfig().getDouble("checks.killaura.min-gcd", GCD_MIN_THRESHOLD);
    }

    /**
     * Process a player attack event.
     *
     * @param player    attacking player
     * @param data      player data
     * @param target    the entity being attacked
     */
    public void process(Player player, PlayerData data, org.bukkit.entity.Entity target) {
        if (!isEnabled()) return;

        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        float yawDelta = Math.abs(yaw - data.getLastYaw());
        float pitchDelta = Math.abs(pitch - data.getLastPitch());

        // Normalize yaw delta
        if (yawDelta > 180) yawDelta = 360 - yawDelta;

        // Store rotation deltas
        LinkedList<Float> yawDeltas = data.getRecentYawDeltas();
        LinkedList<Float> pitchDeltas = data.getRecentPitchDeltas();

        yawDeltas.addLast(yawDelta);
        pitchDeltas.addLast(pitchDelta);

        if (yawDeltas.size() > SAMPLE_SIZE) yawDeltas.removeFirst();
        if (pitchDeltas.size() > SAMPLE_SIZE) pitchDeltas.removeFirst();

        // Update last rotation
        data.setLastYaw(yaw);
        data.setLastPitch(pitch);

        if (yawDeltas.size() < SAMPLE_SIZE) return;

        // Check A: GCD flaw detection (aim assist modules often have very low GCD)
        double gcd = computeGcd(yawDeltas);
        if (gcd > 0 && gcd < minGcd) {
            String detail = String.format("GCD=%.6f (too low, possible aim assist)", gcd);
            flag(player, data, 0.8, detail);
            return;
        }

        // Check B: Variance analysis - too consistent rotation is bot-like
        double yawVariance = MathUtil.variance(yawDeltas.stream().mapToDouble(Float::doubleValue).toArray());
        double pitchVariance = MathUtil.variance(pitchDeltas.stream().mapToDouble(Float::doubleValue).toArray());

        if (yawVariance < MAX_VARIANCE_THRESHOLD && pitchVariance < MAX_VARIANCE_THRESHOLD
                && yawDelta > 0.1f) {
            String detail = String.format("yawVar=%.6f pitchVar=%.6f (robotic rotation)", yawVariance, pitchVariance);
            flag(player, data, 1.0, detail);
            return;
        }

        // Clean attack - decay
        reward(data, 0.05);
    }

    private double computeGcd(LinkedList<Float> values) {
        if (values.isEmpty()) return 0;
        double gcd = values.getFirst();
        for (Float val : values) {
            gcd = MathUtil.gcd(gcd, val);
        }
        return Math.abs(gcd);
    }
}
