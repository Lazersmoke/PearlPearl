package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.UUID;
import java.util.LinkedList;
import java.util.Optional;

public class PearlPearlCmdAggro extends PlayerCommand {
  private static Optional<BukkitTask> aggroVerifier;
  public PearlPearlCmdAggro(String name) {
    super(name);
    setIdentifier("ppaggro");
    setDescription("Configure the aggro verifier");
    setUsage("/ppaggro");
    setArguments(0,0);
  }

  public boolean execute(CommandSender sender, String[] args) {
    if(aggroVerifier.isPresent()){
      sender.sendMessage(ChatColor.RED + "Disabling aggro verifier");
      aggroVerifier.get().cancel();
      aggroVerifier = Optional.empty();
    }else{
      sender.sendMessage(ChatColor.GREEN + "Enabling aggro verifier");
      enableAggro();
    }
    return true;
  }

  public static void enableAggro(){
    aggroVerifier = Optional.of(Bukkit.getScheduler().runTaskTimer(PearlPearl.getInstance(),() -> PearlPearlPearl.verifyAllPearls(),1L,1L)); 
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return new LinkedList<String>();
  }
}
