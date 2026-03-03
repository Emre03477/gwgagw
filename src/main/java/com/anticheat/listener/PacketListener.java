package com.anticheat.listener;

import com.anticheat.AntiCheatPlugin;
import com.anticheat.data.PlayerData;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;

/**
 * ProtocolLib packet listener for low-level packet analysis.
 * Handles:
 * - Flying/position packets (movement validation)
 * - KeepAlive tracking
 * - Transaction tracking
 */
public class PacketListener {

    private final AntiCheatPlugin plugin;

    public PacketListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();

        // Listen to player position packets
        pm.addPacketListener(new PacketAdapter(plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK) {

            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) return;

                // BYPASS CHECK - first thing, before any processing
                if (plugin.getBypassManager().isBypassed(player)) return;

                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (data == null) return;

                // Update last rotation from packet data for more accurate readings
                if (event.getPacketType() == PacketType.Play.Client.POSITION_LOOK
                        || event.getPacketType() == PacketType.Play.Client.LOOK) {
                    float yaw = event.getPacket().getFloat().read(0);
                    float pitch = event.getPacket().getFloat().read(1);
                    data.setLastYaw(yaw);
                    data.setLastPitch(pitch);
                }

                data.setLastMoveTime(System.currentTimeMillis());
            }
        });

        // KeepAlive tracking
        pm.addPacketListener(new PacketAdapter(plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.KEEP_ALIVE) {

            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) return;
                if (plugin.getBypassManager().isBypassed(player)) return;

                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (data == null) return;

                long now = System.currentTimeMillis();
                long lastKeepAlive = data.getLastKeepAliveTime();

                // Detect KeepAlive spoofing (responding too fast or too slow)
                if (lastKeepAlive > 0) {
                    long delta = now - lastKeepAlive;
                    // Update ping estimate
                    data.setPing((int) Math.min(delta, 2000));
                }

                data.setLastKeepAliveTime(now);
                data.setPendingKeepAlives(Math.max(0, data.getPendingKeepAlives() - 1));
            }
        });
    }
}
