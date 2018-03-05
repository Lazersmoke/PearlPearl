package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.namelayer.NameAPI;
import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.UUID;
import java.util.LinkedList;

public class PearlPearlCmdCheck extends PlayerCommand {
  public PearlPearlCmdCheck(String name) {
    super(name);
    setIdentifier("ppcheck");
    setDescription("Check that someone's pearl is legitimately held");
    setUsage("/ppcheck [Player]");
    setArguments(1,1);
  }

  public boolean execute(CommandSender sender, String[] args) {
    UUID u = NameAPI.getUUID(args[0]);
    if(u == null){
      sender.sendMessage(ChatColor.DARK_RED + args[0] + ChatColor.RED + " is not a valid player");
      return true;
    }
    PearlPearlPearl.getPearlsForUUID(u).stream().forEach(pearl -> pearl.getDetail().forEach(sender::sendMessage));
    return true;
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return null; // Defaults to players
  }
}
