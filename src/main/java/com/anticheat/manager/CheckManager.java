package com.anticheat.manager;

import com.anticheat.AntiCheatPlugin;
import com.anticheat.check.Check;
import com.anticheat.check.combat.KillAuraCheck;
import com.anticheat.check.combat.ReachCheck;
import com.anticheat.check.movement.SpeedCheck;
import com.anticheat.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CheckManager {

    private final AntiCheatPlugin plugin;
    private final List<Check> checks = new ArrayList<>();

    public CheckManager(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        registerChecks();
    }

    private void registerChecks() {
        checks.clear();
        checks.add(new SpeedCheck(plugin));
        checks.add(new KillAuraCheck(plugin));
        checks.add(new ReachCheck(plugin));
    }

    public void reload() {
        registerChecks();
    }

    public List<Check> getChecks() {
        return checks;
    }

    public List<Check> getEnabledChecks() {
        return checks.stream().filter(Check::isEnabled).toList();
    }
}
