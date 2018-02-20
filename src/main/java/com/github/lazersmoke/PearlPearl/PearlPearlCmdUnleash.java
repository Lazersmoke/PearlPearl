package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.LinkedList;
import java.util.Optional;
import vg.civcraft.mc.namelayer.NameAPI;
import net.md_5.bungee.api.ChatColor;

public class PearlPearlCmdUnleash extends PlayerCommand {
  public PearlPearlCmdUnleash(String name) {
    super(name);
    setIdentifier("ppunleash");
    setDescription("Unleash a player from their pearl");
    setUsage("/ppunleash");
    setArguments(0,0);
  }

  public boolean execute(CommandSender sender, String [] args) {
    if(!(sender instanceof Player)){
      return true;
    }
    Player p = (Player) sender;
    ItemStack i = p.getInventory().getItemInMainHand();
    Optional<PearlPearlPearl> pearl = PearlPearlPearl.fromItemStack(i);
    if(pearl.isPresent()){
      pearl.get().freePearl("you were unleashed by " + ChatColor.AQUA + p.getName());
      p.sendMessage(ChatColor.GREEN + "You unleashed " + ChatColor.AQUA + NameAPI.getCurrentName(pearl.get().pearledId));
    }else{
      p.sendMessage(ChatColor.RED + "You must be holding a pearl to unleash it");
    }
    return true;
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return new LinkedList<String>(); //empty list
  }
}
