package org.superfuntime.sftbet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class SFTBet extends JavaPlugin implements Listener {

    private Logger logger;
    private Configuration config;
    private World world;
    private Location arena;
    private Location spectate;
    // TODO: Vault integration
    // TODO: Permissions

    @Override
    public void onEnable() {    // Load logger, config, initialize queue

        logger = getLogger();   // Logger
        logger.info("Loading SFTBet...");
        logger.info("Reading config");
        try {

            config = getConfig();   // Get config
            world = Bukkit.getWorld(config.getString("world"));
            String[] coords = config.getString("arena").split(", ");
            arena = new Location(
                    world,
                    Double.parseDouble(coords[0]),
                    Double.parseDouble(coords[1]),
                    Double.parseDouble(coords[2])
            );
            coords = config.getString("spectate").split(", ");
            spectate = new Location(
                    world,
                    Double.parseDouble(coords[0]),
                    Double.parseDouble(coords[1]),
                    Double.parseDouble(coords[2])
            );
        } catch (Exception e) {     // Fatal failure while reading config

            logger.severe("Failed to read config. Cancelling enable...");
            this.setEnabled(false);
            return;
        }
    }

    @Override
    public void onDisable() {

        // TODO: Return active bets
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("pvp")) {

            if (sender instanceof Player) {

                Player player = (Player) sender;
                if(args.length > 0) {

                    if (args[0].equalsIgnoreCase("queue")){

                        // TODO: Show match queue
                    } else if (args[0].equalsIgnoreCase("spectate") || args[0].equalsIgnoreCase("watch")) {

                        player.teleport(spectate);
                    } else if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("join")) {

                        // TODO: Join matchmaking
                    }
                } else {

                    player.teleport(spectate);
                    player.sendMessage(ChatColor.YELLOW + "Use '/pvp help' to show usages.");
                }
                // TODO: Matchmaking ( ͡° ͜ʖ ͡°)
                // TODO: Permissions
                return true;
            } else {

                if (args.length > 0 && args[0].equalsIgnoreCase("queue")){

                    // TODO: Show match queue
                } else {

                    sender.sendMessage(ChatColor.DARK_RED + "Non-player senders may only view queue.");
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDisconnect(PlayerQuitEvent event) {

        // TODO: Lose match after 20 sec unless connect
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {

        // TODO: Check if in match & resume
    }
}
