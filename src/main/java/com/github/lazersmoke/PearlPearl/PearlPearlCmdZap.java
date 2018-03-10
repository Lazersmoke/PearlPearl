package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;
import vg.civcraft.mc.namelayer.NameAPI;
import net.md_5.bungee.api.ChatColor;

public class PearlPearlCmdZap extends PlayerCommand {
  public PearlPearlCmdZap(String name) {
    super(name);
    setIdentifier("ppzap");
    setDescription("Zap a specific pearl out of existence, by pearl UUID");
    setUsage("/ppzap");
    setArguments(1,1);
  }

  public boolean execute(CommandSender sender, String [] args) {
    UUID u = UUID.fromString(args[0]);
    if(u == null){
      sender.sendMessage(ChatColor.DARK_RED + args[0] + ChatColor.RED + " is not a valid UUID");
    }
    Optional<PearlPearlPearl> pearl = PearlPearlPearl.getPearlByUUID(u);
    if(pearl.isPresent()){
      pearl.get().freePearl(sender.getName() + " zapped your pearl out of existence");
      sender.sendMessage(ChatColor.GREEN + "You zapped the pearl " + ChatColor.AQUA + args[0] + ChatColor.GREEN + " out of existence");
    }else{
      sender.sendMessage(ChatColor.DARK_RED + args[0] + ChatColor.RED + " doesn't match any pearls");
    }
    return true;
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return PearlPearlPearl.getAllPearlsSnapshot().stream().map(p -> p.uniqueId.toString()).filter(s -> args.length == 0 || s.startsWith(args[0])).collect(Collectors.toList());
  }
}
