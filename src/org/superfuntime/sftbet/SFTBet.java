package org.superfuntime.sftbet;

import net.minecraft.util.org.apache.commons.lang3.StringUtils;
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
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SFTBet extends JavaPlugin implements Listener {

    private Logger logger;
    private BukkitScheduler scheduler;
    private Configuration config;
    private World world;
    private Location arena;
    private Location spectate;
    private List<BetManager> queue;
    private List<String> waiting;
    private boolean pvpDisabled;
    private boolean betDisabled;
    // TODO: Vault integration
    // TODO: Permissions
    // TODO: Disable

    @Override
    public void onEnable() {    // Load logger, config, initialize queue

        queue = new ArrayList<BetManager>();
        waiting = new ArrayList<String>();
        scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> {   // Add waiting players to matches every 30 seconds

            if (waiting.size() > 1) {

                queue.add(new BetManager(waiting));
                for (String s : waiting) {

                    getServer().getPlayer(s).sendMessage(ChatColor.YELLOW + "Found match with " + waiting.size() + " players. Match is number " + queue.size() + " in match queue");
                }
                waiting.clear();
            }
        }, 0L, 600L);   // 20 ticks/second * 30 seconds
        logger = getLogger();   // Logger
        logger.info("Loading SFTBet...");
        logger.info("Reading config");
        try {

            config = getConfig();   // Get config
            config.addDefault("world", "world");
            config.addDefault("arena", "0.0, 64.0, 0.0");
            config.addDefault("spectate", "0.0, 64.0, 0.0");
            config.options().copyDefaults(true);
            saveConfig();
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
        } catch (Exception e) {     // Failure while reading config

            logger.severe("Failed to read config. Cancelling enable...");
            this.setEnabled(false);
            return;
        }
        pvpDisabled = false;
        betDisabled = false;
    }

    @Override
    public void onDisable() {

        // TODO: Return active bets
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("pvp")) {

            if (sender instanceof Player) {

                if (!pvpDisabled) {

                    Player player = (Player) sender;
                    if (args.length > 0) {

                        if (player.hasPermission("sftbet.queue") && (args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("list"))) {

                            // Show match queue
                            player.sendMessage(ChatColor.YELLOW + "Match Queue:");
                            List<BetManager> matches = new ArrayList(queue);
                            for (int i = 0; i < matches.size(); i++) {

                                player.sendMessage(ChatColor.YELLOW + "" + i + ". " + StringUtils.join(matches.get(i).getFighters(), ", "));
                            }
                        } else if (player.hasPermission("sftbet.spectate") && (args[0].equalsIgnoreCase("spectate") || args[0].equalsIgnoreCase("watch"))) {

                            player.teleport(spectate);
                        } else if (player.hasPermission("sftbet.join") && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("join"))) {

                            String name = player.getName();
                            boolean isWaiting = false;
                            for (String s : waiting) {

                                if (s.equalsIgnoreCase(name)) {

                                    isWaiting = true;
                                    break;
                                }
                            }
                            if (!isWaiting) {

                                waiting.add(name);
                                player.sendMessage(ChatColor.YELLOW + "Finding a match... " + waiting.size() + " player(s) waiting.");
                            } else {

                                player.sendMessage(ChatColor.DARK_RED + "Already in queue... " + waiting.size() + " player(s) waiting.");
                            }
                        } else if (player.hasPermission("sftbet.disable") && args[0].equalsIgnoreCase("disable")) {

                            pvpDisabled = true;
                        } else if (player.hasPermission("sftbet.info") && args[0].equalsIgnoreCase("info")) {

                            int n;
                            try {

                                n = Integer.parseInt(args[1]);
                                if (queue.size() > n) {

                                    sender.sendMessage(ChatColor.YELLOW + "Match " + args[1] + ":");
                                    for (String s : queue.get(n).toString().split("\n")) {
                                        sender.sendMessage(ChatColor.YELLOW + s);
                                    }
                                } else {

                                    sender.sendMessage(ChatColor.DARK_RED + "Match not found.");
                                }
                            } catch (Exception e) {

                                sender.sendMessage(ChatColor.DARK_RED + "Match not found.");
                            }
                        }
                    } else {

                        if (player.hasPermission("sftbet.spectate")) player.teleport(spectate);
                        player.sendMessage(ChatColor.YELLOW + "Use '/pvp help' to show usages.");
                    }
                    // TODO: Matchmaking ( ͡° ͜ʖ ͡°)
                    // TODO: Permissions
                } else {

                    if (args.length > 0 && sender.hasPermission("sftbet.queue") && args[0].equalsIgnoreCase("queue")) {

                        // Show match queue
                        sender.sendMessage(ChatColor.YELLOW + "Match Queue:");
                        List<BetManager> matches = new ArrayList(queue);
                        for (int i = 0; i < matches.size(); i++) {

                            sender.sendMessage(ChatColor.YELLOW + "" + i + ". " + StringUtils.join(matches.get(i).getFighters(), ", "));
                        }
                    } else if (sender.hasPermission("sftbet.disable") && args[0].equalsIgnoreCase("disable")) {

                        pvpDisabled = true;
                    } else if (sender.hasPermission("sftbet.info") && args[0].equalsIgnoreCase("info") && args.length >= 2) {

                        int n;
                        try {

                            n = Integer.parseInt(args[1]);
                            if (queue.size() > n) {

                                sender.sendMessage(ChatColor.YELLOW + "Match " + args[1] + ":");
                                for (String s : queue.get(n).toString().split("\n")) {
                                    sender.sendMessage(ChatColor.YELLOW + s);
                                }
                            } else {

                                sender.sendMessage(ChatColor.DARK_RED + "Match not found.");
                            }
                        } catch (Exception e) {

                            sender.sendMessage(ChatColor.DARK_RED + "Match not found.");
                        }
                    } else {

                        sender.sendMessage(ChatColor.DARK_RED + "Non-player senders may not execute this command.");
                    }
                }
            } else if (sender.hasPermission("sftbet.enable") && args[0].equalsIgnoreCase("enable")) {

                pvpDisabled = false;
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("bet") && !betDisabled) {

            if (sender instanceof Player) {

                if (args.length > 0) {

                } else {

                    sender.sendMessage(ChatColor.YELLOW + "Use '/bet help' to show usages.");
                }
            } else {

                sender.sendMessage(ChatColor.DARK_RED + "Non-player senders may not bet on matches.");
            }
        } else if (cmd.getName().equalsIgnoreCase("disable")) {

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
