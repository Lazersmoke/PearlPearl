package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.List;
import java.util.LinkedList;
import java.util.Optional;
import vg.civcraft.mc.namelayer.NameAPI;
import net.md_5.bungee.api.ChatColor;

public class PearlPearlCmdBehave extends PlayerCommand {
  public PearlPearlCmdBehave(String name) {
    super(name);
    setIdentifier("ppbehave");
    setDescription("Configure the behavior of a pearl");
    setUsage("/ppbehave");
    setArguments(0,0);
  }

  public boolean execute(CommandSender sender, String [] args) {
    if(!(sender instanceof Player)){
      return true;
    }
    Player p = (Player) sender;
    PlayerInventory inv = p.getInventory();
    Optional<PearlPearlPearl> pearl = PearlPearlPearl.updateItemStack(inv.getItemInMainHand(),inv::setItemInMainHand);
    if(pearl.isPresent()){
      new PearlPearlPearlBehaviorGUI(p.getUniqueId(), pearl.get().uniqueId).showScreen();
    }else{
      p.sendMessage(ChatColor.RED + "You must be holding a pearl to configure it");
    }
    return true;
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return new LinkedList<String>(); //empty list
  }
}
