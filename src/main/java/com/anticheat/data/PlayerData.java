package com.anticheat.data;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private final UUID uuid;
    private final String name;

    // Violation levels per check name
    private final Map<String, Double> violationLevels = new ConcurrentHashMap<>();
    // Violation history per check name
    private final Map<String, LinkedList<Long>> violationHistory = new ConcurrentHashMap<>();

    // Movement data
    private Location lastLocation;
    private Location lastGroundLocation;
    private double lastDeltaXZ;
    private double lastDeltaY;
    private long lastMoveTime;
    private int airTicks;
    private int groundTicks;
    private boolean wasOnGround;
    private boolean isFlying;

    // Combat data
    private long lastAttackTime;
    private int attackCount;
    private final LinkedList<Float> recentYawDeltas = new LinkedList<>();
    private final LinkedList<Float> recentPitchDeltas = new LinkedList<>();
    private final LinkedList<Long> clickTimes = new LinkedList<>();
    private float lastYaw;
    private float lastPitch;

    // Packet data
    private long lastKeepAliveTime;
    private long lastTransactionTime;
    private int pendingKeepAlives;

    // Buffers for check logic
    private final Map<String, Double> checkBuffers = new ConcurrentHashMap<>();

    // Latency
    private int ping;

    // Exemption
    private boolean exempt;

    // Staff alerts toggle
    private boolean alertsEnabled = true;

    public PlayerData(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.lastLocation = player.getLocation();
        this.wasOnGround = player.isOnGround();
    }

    // ===== Violation Management =====

    public double getViolationLevel(String checkName) {
        return violationLevels.getOrDefault(checkName, 0.0);
    }

    public void addViolation(String checkName, double amount) {
        double current = violationLevels.getOrDefault(checkName, 0.0);
        violationLevels.put(checkName, current + amount);
        violationHistory.computeIfAbsent(checkName, k -> new LinkedList<>()).addLast(System.currentTimeMillis());
        // Keep history limited to last 100 entries
        LinkedList<Long> history = violationHistory.get(checkName);
        while (history.size() > 100) {
            history.removeFirst();
        }
    }

    public void decayViolation(String checkName, double amount) {
        double current = violationLevels.getOrDefault(checkName, 0.0);
        violationLevels.put(checkName, Math.max(0.0, current - amount));
    }

    public void resetViolation(String checkName) {
        violationLevels.put(checkName, 0.0);
    }

    public Map<String, Double> getAllViolations() {
        return new HashMap<>(violationLevels);
    }

    public Map<String, LinkedList<Long>> getViolationHistory() {
        return violationHistory;
    }

    // ===== Buffer Management =====

    public double getBuffer(String checkName) {
        return checkBuffers.getOrDefault(checkName, 0.0);
    }

    public void setBuffer(String checkName, double value) {
        checkBuffers.put(checkName, value);
    }

    public void addBuffer(String checkName, double amount) {
        double current = checkBuffers.getOrDefault(checkName, 0.0);
        checkBuffers.put(checkName, current + amount);
    }

    public void decayBuffer(String checkName, double amount) {
        double current = checkBuffers.getOrDefault(checkName, 0.0);
        checkBuffers.put(checkName, Math.max(0.0, current - amount));
    }

    // ===== UUID & Name =====

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    // ===== Movement Getters/Setters =====

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public Location getLastGroundLocation() {
        return lastGroundLocation;
    }

    public void setLastGroundLocation(Location lastGroundLocation) {
        this.lastGroundLocation = lastGroundLocation;
    }

    public double getLastDeltaXZ() {
        return lastDeltaXZ;
    }

    public void setLastDeltaXZ(double lastDeltaXZ) {
        this.lastDeltaXZ = lastDeltaXZ;
    }

    public double getLastDeltaY() {
        return lastDeltaY;
    }

    public void setLastDeltaY(double lastDeltaY) {
        this.lastDeltaY = lastDeltaY;
    }

    public long getLastMoveTime() {
        return lastMoveTime;
    }

    public void setLastMoveTime(long lastMoveTime) {
        this.lastMoveTime = lastMoveTime;
    }

    public int getAirTicks() {
        return airTicks;
    }

    public void setAirTicks(int airTicks) {
        this.airTicks = airTicks;
    }

    public int getGroundTicks() {
        return groundTicks;
    }

    public void setGroundTicks(int groundTicks) {
        this.groundTicks = groundTicks;
    }

    public boolean wasOnGround() {
        return wasOnGround;
    }

    public void setWasOnGround(boolean wasOnGround) {
        this.wasOnGround = wasOnGround;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public void setFlying(boolean flying) {
        isFlying = flying;
    }

    // ===== Combat Getters/Setters =====

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public void incrementAttackCount() {
        this.attackCount++;
    }

    public void resetAttackCount() {
        this.attackCount = 0;
    }

    public LinkedList<Float> getRecentYawDeltas() {
        return recentYawDeltas;
    }

    public LinkedList<Float> getRecentPitchDeltas() {
        return recentPitchDeltas;
    }

    public LinkedList<Long> getClickTimes() {
        return clickTimes;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }

    // ===== Packet Getters/Setters =====

    public long getLastKeepAliveTime() {
        return lastKeepAliveTime;
    }

    public void setLastKeepAliveTime(long lastKeepAliveTime) {
        this.lastKeepAliveTime = lastKeepAliveTime;
    }

    public long getLastTransactionTime() {
        return lastTransactionTime;
    }

    public void setLastTransactionTime(long lastTransactionTime) {
        this.lastTransactionTime = lastTransactionTime;
    }

    public int getPendingKeepAlives() {
        return pendingKeepAlives;
    }

    public void setPendingKeepAlives(int pendingKeepAlives) {
        this.pendingKeepAlives = pendingKeepAlives;
    }

    // ===== Misc =====

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public boolean isExempt() {
        return exempt;
    }

    public void setExempt(boolean exempt) {
        this.exempt = exempt;
    }

    public boolean isAlertsEnabled() {
        return alertsEnabled;
    }

    public void setAlertsEnabled(boolean alertsEnabled) {
        this.alertsEnabled = alertsEnabled;
    }
}
