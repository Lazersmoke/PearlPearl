package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.LinkedList;

public class PearlPearlCmdOverview extends PlayerCommand {
  public PearlPearlCmdOverview(String name) {
    super(name);
    setIdentifier("ppoverview");
    setDescription("Overview all the pearls in existence");
    setUsage("/ppoverview");
    setArguments(0,0);
  }

  public boolean execute(CommandSender sender, String[] args) {
    if(!(sender instanceof Player)){
      return true;
    }
    Player p = (Player) sender;
    new PearlPearlPearlListGUI(p.getUniqueId(), PearlPearlPearl.getAllPearlsSnapshot(), pe -> true).showScreen();
    return true;
  }

  public List <String> tabComplete(CommandSender sender, String[] args) {
    return new LinkedList<String>(); //empty list
  }
}
