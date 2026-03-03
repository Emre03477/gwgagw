package com.anticheat.util;

import com.anticheat.AntiCheatPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tracks server TPS (ticks per second) for lag compensation.
 */
public class TpsUtil extends BukkitRunnable {

    private static final int SAMPLE_SIZE = 20;
    private static final long EXPECTED_TICK_MS = 50L;

    private final long[] tickTimes = new long[SAMPLE_SIZE];
    private int index = 0;
    private double tps = 20.0;
    private long lastTickTime;

    public TpsUtil(AntiCheatPlugin plugin) {
        lastTickTime = System.currentTimeMillis();
        // Schedule every tick
        runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        tickTimes[index % SAMPLE_SIZE] = now - lastTickTime;
        index++;
        lastTickTime = now;

        if (index >= SAMPLE_SIZE) {
            long total = 0;
            for (long t : tickTimes) total += t;
            double avgMs = (double) total / SAMPLE_SIZE;
            tps = (avgMs > 0) ? Math.min(20.0, 1000.0 / avgMs) : 20.0;
        }
    }

    /**
     * Get the current server TPS (0.0 - 20.0).
     */
    public double getTps() {
        return tps;
    }

    /**
     * Returns true if the server is lagging (TPS below threshold).
     */
    public boolean isLagging(double threshold) {
        return tps < threshold;
    }
}
