package org.superfuntime.sftbet;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class SFTBet extends JavaPlugin {

    Logger logger;
    // TODO: Vault integration

    @Override
    public void onEnable() {    // Load logger, config, initialize queue

        logger = getLogger();
        logger.info("Loading SFTBet...");
        // TODO: Load config
    }

    @Override
    public void onDisable() {

        // TODO: Return active bets
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(cmd.getName().equalsIgnoreCase("pvp")) {

            if (sender instanceof Player) {

                Player player = (Player) sender;
                // TODO: Teleport
                // TODO: Load coords from config
                // TODO: Matchmaking
                // TODO: Permissions
                return true;
            }
        }
        return false;
    }
}
