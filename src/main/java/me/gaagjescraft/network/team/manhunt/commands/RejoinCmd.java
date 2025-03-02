package me.gaagjescraft.network.team.manhunt.commands;

import me.gaagjescraft.network.team.manhunt.Manhunt;
import me.gaagjescraft.network.team.manhunt.games.Game;
import me.gaagjescraft.network.team.manhunt.utils.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RejoinCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cYou must be a player to perform this command.");
            return true;
        }

        Player player = (Player) sender;
        Game game = Game.getGame(player);
        if (game == null) {
            player.sendMessage(Util.c(Manhunt.get().getCfg().noGameToRejoinMessage));
            return true;
        } else if (game.getPlayer(player).isOnline()) {
            player.sendMessage(Util.c(Manhunt.get().getCfg().alreadyInGameRejoinMessage));
            return true;
        }

        if (!game.addPlayer(player)) {
            player.sendMessage(Util.c(Manhunt.get().getCfg().rejoinFailMessage));
        } else {
            player.sendMessage(Util.c(Manhunt.get().getCfg().playerRejoinedMessage));
        }
        return true;
    }
}
