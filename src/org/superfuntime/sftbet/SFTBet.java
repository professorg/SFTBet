package org.superfuntime.sftbet;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SFTBet extends JavaPlugin implements Listener {

    private Logger logger;
    private BukkitScheduler scheduler;
    private Configuration config;
    private World world;
    private Location arena;
    private Location spectate;
    private BetManager[] queue;
    private List<String> waiting;
    private BetManager activeMatch;
    private List<String> activePlayers;
    private int activeMatchId;
    private boolean pvpDisabled;
    private boolean betDisabled;
    private int currentId;
    // TODO: GUI

    private Economy economy;

    @Override
    public void onEnable() {    // Load logger, config, initialize queue

        queue = new BetManager[32];
        waiting = new ArrayList<String>();
        activePlayers = new ArrayList<String>();
        currentId = 0;
        activeMatchId = -1;
        activeMatch = null;
        logger = getLogger();   // Logger
        logger.info("Loading SFTBet...");
        logger.info("Reading config");
        if (setupEconomy()) {
            logger.info("Found economy: " + economy.getName());
        }
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
        getServer().getPluginManager().registerEvents(this, this);
        scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> {   // Add waiting players to matches every 30 seconds

            if (waiting.size() > 1) {

                BetManager match = new BetManager(waiting);
                queue[currentId] = match;
                for (String s : waiting) {

                    getServer().getPlayer(s).sendMessage(ChatColor.YELLOW + "Found match with " + waiting.size() + " players. Match is number " + currentId + " in match queue");
                }
                waiting.clear();
                if (activeMatch == null) {

                    int n = currentId;
                    scheduler.scheduleSyncDelayedTask(this, () -> {

                        match.setState(1);
                        activeMatch = match;
                        activeMatchId = n;
                        activePlayers = new ArrayList<String>(match.getFighters());
                        for (String s : match.getFighters()) {

                            getServer().getPlayer(s).teleport(arena);
                        }
                        Map<String, Map<String, Double>> betInfo = match.getBetInfo();
                        betInfo.forEach((k, v) -> {
                            v.forEach((k1, v1) -> {

                                getServer().getPlayer(k1).sendMessage(ChatColor.YELLOW + "Betting has ended for match " + activeMatchId);
                                economy.withdrawPlayer(getServer().getPlayer(k1), v1);
                            });
                        });
                    }, 600L);   // 30 seconds to bet
                }
                currentId++;
                currentId %= 32;
            }
        }, 0L, 600L);   // 20 ticks/second * 30 seconds

        pvpDisabled = false;
        betDisabled = false;
    }

    @Override
    public void onDisable() {

        if (activeMatch != null && activeMatch.getState() < 2) {

            Map<String, Map<String, Double>> betInfo = activeMatch.getBetInfo();
            betInfo.forEach((k, v) -> {
                v.forEach((k1, v1) -> {
                    economy.depositPlayer(getServer().getPlayer(k1), v1);
                });
            });
        }
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
                            sender.sendMessage(ChatColor.YELLOW + "Match Queue:");
                            boolean found = false;
                            for (int i = currentId; i < 32; i++) {

                                if (queue[i] != null) {

                                    ChatColor color;
                                    if (queue[i].getState() == 0) color = ChatColor.GREEN;
                                    else if (queue[i].getState() == 1) color = ChatColor.BLUE;
                                    else if (queue[i].getState() == 2) color = ChatColor.RED;
                                    else color = ChatColor.BLACK;
                                    player.sendMessage(color + "  " + i + ": " + StringUtils.join(queue[i].getFighters(), ", "));
                                    found = true;
                                }
                            }
                            for (int i = 0; i < currentId; i++) {

                                if (queue[i] != null) {

                                    ChatColor color;
                                    if (queue[i].getState() == 0) color = ChatColor.GREEN;
                                    else if (queue[i].getState() == 1) color = ChatColor.BLUE;
                                    else if (queue[i].getState() == 2) color = ChatColor.RED;
                                    else color = ChatColor.BLACK;
                                    player.sendMessage(color + "  " + i + ": " + StringUtils.join(queue[i].getFighters(), ", "));
                                    found = true;
                                }
                            }
                            if (!found) sender.sendMessage(ChatColor.GRAY + "  None");
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
                                if (queue[n] != null) {

                                    sender.sendMessage(ChatColor.YELLOW + "Match " + args[1] + ":");
                                    for (String s : queue[n].toString().split("\n")) {
                                        sender.sendMessage(ChatColor.YELLOW + s);
                                    }
                                } else {

                                    sender.sendMessage(ChatColor.DARK_RED + "Match not found.");
                                }
                            } catch (Exception e) {

                                sender.sendMessage(ChatColor.DARK_RED + "Match not found.");
                            }
                        } else if (player.hasPermission("sftbet.help") && args[0].equalsIgnoreCase("help")) {

                            showPvpHelp(player);
                        } else {

                            sender.sendMessage(ChatColor.DARK_RED + "Invalid usage. Use '/pvp help' to show usages.");
                        }
                    } else {

                        if (player.hasPermission("sftbet.spectate")) player.teleport(spectate);
                        player.sendMessage(ChatColor.YELLOW + "Use '/pvp help' to show usages.");
                    }
                } else {

                    if (args.length > 0) {
                        if (sender.hasPermission("sftbet.queue") && args[0].equalsIgnoreCase("queue")) {

                            // Show match queue
                            sender.sendMessage(ChatColor.YELLOW + "Match Queue:");
                            boolean found = false;
                            for (int i = currentId; i < 32; i++) {

                                if (queue[i] != null) {

                                    ChatColor color;
                                    if (queue[i].getState() == 0) color = ChatColor.GREEN;
                                    else if (queue[i].getState() == 1) color = ChatColor.BLUE;
                                    else if (queue[i].getState() == 2) color = ChatColor.RED;
                                    else color = ChatColor.BLACK;
                                    sender.sendMessage(color + "  " + i + ": " + StringUtils.join(queue[i].getFighters(), ", "));
                                    found = true;
                                }
                            }
                            for (int i = 0; i < currentId; i++) {

                                if (queue[i] != null) {

                                    ChatColor color;
                                    if (queue[i].getState() == 0) color = ChatColor.GREEN;
                                    else if (queue[i].getState() == 1) color = ChatColor.BLUE;
                                    else if (queue[i].getState() == 2) color = ChatColor.RED;
                                    else color = ChatColor.BLACK;
                                    sender.sendMessage(color + "  " + i + ": " + StringUtils.join(queue[i].getFighters(), ", "));
                                    found = true;
                                }
                            }
                            if (!found) sender.sendMessage(ChatColor.GRAY + "  None");
                        } else if (sender.hasPermission("sftbet.disable") && args[0].equalsIgnoreCase("disable")) {

                            pvpDisabled = true;
                        } else if (sender.hasPermission("sftbet.info") && args[0].equalsIgnoreCase("info") && args.length >= 2) {

                            int n;
                            try {

                                n = Integer.parseInt(args[1]);
                                if (queue[n] != null) {

                                    sender.sendMessage(ChatColor.YELLOW + "Match " + args[1] + ":");
                                    for (String s : queue[n].toString().split("\n")) {
                                        sender.sendMessage(ChatColor.YELLOW + s);
                                    }
                                } else {

                                    sender.sendMessage(ChatColor.DARK_RED + "Match not found.");
                                }
                            } catch (Exception e) {

                                sender.sendMessage(ChatColor.DARK_RED + "Match not found.");
                            }
                        } else if (sender.hasPermission("sftbet.help") && args[0].equalsIgnoreCase("help")) {

                            showPvpHelp(sender);
                        } else {

                            sender.sendMessage(ChatColor.DARK_RED + "Non-player senders may not execute this command.");
                        }
                    } else {

                        showPvpHelp(sender);
                    }
                }
            } else if (sender.hasPermission("sftbet.enable") && args[0].equalsIgnoreCase("enable")) {

                pvpDisabled = false;
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("bet") && !betDisabled) {

            if (sender instanceof Player) {
                Player player = (Player) sender;
                boolean first = true;
                try {
                    if (args.length >= 3) {
                        int matchid = Integer.parseInt(args[0]);
                        first = false;
                        if (queue.length > matchid && matchid >= 0) {
                            BetManager match = queue[matchid];
                            boolean worked = false;
                            if (match != null) {
                                worked = match.addBet(args[1], player.getName(), Integer.parseInt(args[2]));
                                if (!worked) {
                                    if (match.getState() == 1) {
                                        sender.sendMessage(ChatColor.YELLOW + "Betting for match " + matchid + " has ended already.");
                                    } else if (match.getState() >= 2) {
                                        sender.sendMessage(ChatColor.DARK_RED + "" + matchid + " is not an ongoing match.");
                                    } else {
                                        sender.sendMessage(ChatColor.DARK_RED + "There is no fighter " + args[1] + " in match " + matchid);
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.YELLOW + "Bet " + args[2] + economy.currencyNamePlural() + " on " + args[1]);
                                }
                            } else {
                                sender.sendMessage(ChatColor.DARK_RED + "" + matchid + " is not an ongoing match.");
                            }
                        } else {

                            sender.sendMessage(ChatColor.YELLOW + "Use '/bet help' to show usages.");
                        }

                    } else {

                        showBetHelp(sender);
                    }
                } catch (Exception e) {
                    if (first) {
                        sender.sendMessage(ChatColor.DARK_RED + args[0] + " must be an valid match number.");
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + args[2] + " must be an valid amount of money.");
                    }
                }
            } else {

                sender.sendMessage(ChatColor.DARK_RED + "Non-player senders may not bet on matches.");
            }
        } else if (cmd.getName().equalsIgnoreCase("disable")) {

            betDisabled = true;
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

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        if (activeMatch == null) return;
        boolean inMatch = false;
        for (String s : activePlayers) {

            if (s.equalsIgnoreCase(event.getEntity().getName())) {

                activePlayers.remove(s);
                inMatch = true;
                break;
            }
        }
        if (!inMatch) return;
        if (activePlayers.size() == 1) {

            activeMatch.setState(2);
            Map<String, Double> winnings = activeMatch.calcWins(activePlayers.get(0));
            winnings.forEach((k, v) -> {

                getServer().getPlayer(k).sendMessage(ChatColor.GREEN + "You won " + v + economy.currencyNamePlural() + " on match " + activeMatchId);
                economy.depositPlayer(getServer().getPlayer(k), v);
            });

            getServer().getPlayer(activePlayers.get(0)).teleport(spectate);

            activeMatch = null;
            queue[activeMatchId] = null;
            activeMatchId = -1;
            activePlayers.clear();
            for (int i = 0; i < 32; i++) {

                BetManager match = queue[i];
                if (match != null) {

                    int n = currentId;

                    scheduler.scheduleSyncDelayedTask(this, () -> {

                        match.setState(1);
                        activeMatch = match;
                        activeMatchId = n;
                        activePlayers = new ArrayList<String>(match.getFighters());
                        for (String s : match.getFighters()) {

                            getServer().getPlayer(s).teleport(arena);
                        }
                        Map<String, Map<String, Double>> betInfo = match.getBetInfo();
                        betInfo.forEach((k, v) -> {
                            v.forEach((k1, v1) -> {
                                economy.withdrawPlayer(getServer().getPlayer(k1), v1);
                            });
                        });
                    }, 600L);   // 30 seconds to bet
                }
            }
        }
    }

    private void showPvpHelp(CommandSender sender) {

        sender.sendMessage(ChatColor.YELLOW + "/pvp help: Shows this message");
        sender.sendMessage(ChatColor.YELLOW + "/pvp queue: Shows match queue");
        sender.sendMessage(ChatColor.YELLOW + "/pvp list: Alias for queue");
        sender.sendMessage(ChatColor.YELLOW + "/pvp spectate: Watch matches from spectator arena");
        sender.sendMessage(ChatColor.YELLOW + "/pvp watch: Alias for spectate");
        sender.sendMessage(ChatColor.YELLOW + "/pvp join: Joins matchmaking waiting list");
        sender.sendMessage(ChatColor.YELLOW + "/pvp start: Alias for join");
        sender.sendMessage(ChatColor.YELLOW + "/pvp info: Show info for match");
        sender.sendMessage(ChatColor.YELLOW + "    Usage: /pvp info <match number>");
        sender.sendMessage(ChatColor.YELLOW + "/pvp disable: Disables all pvp commands");
    }

    private void showBetHelp(CommandSender sender) {

        sender.sendMessage(ChatColor.YELLOW + "Usage: /bet <match id> <player> <amount>");
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
            return true;
        }
        return false;
    }

}
