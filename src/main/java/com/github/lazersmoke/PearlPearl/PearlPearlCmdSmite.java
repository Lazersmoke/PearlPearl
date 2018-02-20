package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import vg.civcraft.mc.namelayer.NameAPI;
import net.md_5.bungee.api.ChatColor;

public class PearlPearlCmdSmite extends PlayerCommand {
  public PearlPearlCmdSmite(String name) {
    super(name);
    setIdentifier("ppsmite");
    setDescription("Smite a player and put them in a pearl");
    setUsage("/ppsmite <Player>");
    setArguments(1,1);
  }

  public boolean execute(CommandSender sender, String[] args) {
    if(!(sender instanceof Player)){
      return true;
    }
    Player smiter = (Player) sender;
    UUID smitten = NameAPI.getUUID(args[0]);
    if(smitten == null){
      smiter.sendMessage(ChatColor.DARK_RED + args[0] + ChatColor.RED + " is not a valid player");
      return false;
    }
    PearlPearlPearl.makePearlingHappen(smitten,smiter);
    return true;
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return null; // Defaults to players
  }
}
