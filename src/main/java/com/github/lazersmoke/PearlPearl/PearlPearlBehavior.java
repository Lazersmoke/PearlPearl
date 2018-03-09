package com.github.lazersmoke.PearlPearl;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.Optional;
import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;

public enum PearlPearlBehavior{
  EXILE("Exile", new ItemStack(Material.POTION, 1, (short) 12), new String[]{"Prevents the pearled player from", "being within " + PearlPearl.getInstance().getConfiguration().exileRadius + " blocks of their pearl"}),
  END("Prison", new ItemStack(Material.ENDER_STONE), new String[]{"Banishes the pearled player to the End"}),
  BASTION("Bastion Banish", new ItemStack(Material.SPONGE), new String[]{"Prevents the pearled player from", "entering hostile bastion fields", "within " + PearlPearl.getInstance().getConfiguration().exileRadius + " blocks of their pearl"});
  
  public final String name;
  public final ItemStack displayStack;
  public final String[] lore;

  private PearlPearlBehavior(String name, ItemStack displayStack, String[] lore){
    this.name = name;
    this.displayStack = displayStack;
    this.lore = lore;
  }

  public static void startTasks(){
    // Pearl Exile Rule
    Bukkit.getScheduler().runTaskTimer(PearlPearl.getInstance(),PearlPearlBehavior::applyExileRule, 20L, 20L * 1L);
    // Pearl Bastion Rule
    Bukkit.getScheduler().runTaskTimer(PearlPearl.getInstance(),PearlPearlBehavior::applyBastionRule, 20L, 20L * 1L);
  }

  public static void applyExileRule(){
    PearlPearlPearl.pearlsWithBehavior(EXILE).forEach(pearl -> {
      Optional.ofNullable(Bukkit.getPlayer(pearl.pearledId)).ifPresent(player -> {
        pearl.aggroVerify().ifPresent(pearlLocation -> {
          if(pearlLocation.distance(player.getLocation()) < PearlPearl.getInstance().getConfiguration().exileRadius){
            player.getWorld().playSound(player.getLocation(),Sound.ENTITY_POLAR_BEAR_DEATH,1.0f,1.5f + new Random().nextFloat() * 0.5f);
            player.getWorld().playSound(player.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,0.5f,0.5f);
            player.damage(2);
          }
        });
      });
    });
  }

  public static void applyBastionRule(){
    final Set<UUID> alreadyUsed = new HashSet<UUID>();
    PearlPearlPearl.pearlsWithBehavior(BASTION).forEach(pearl -> {
      if(!alreadyUsed.add(pearl.pearledId)){
        return;
      }
      Optional.ofNullable(Bukkit.getPlayer(pearl.pearledId)).ifPresent(player -> {
        pearl.aggroVerify().ifPresent(pearlLocation -> {
          if(pearlLocation.distance(player.getLocation()) < PearlPearl.getInstance().getConfiguration().exileRadius){
            for(Location l : PearlPearl.getInstance().getBlockingBastions(player)){
              player.getWorld().playSound(l,Sound.ENTITY_EVOCATION_ILLAGER_CAST_SPELL,64.0f,2.0f);
              player.getWorld().playSound(player.getLocation(),Sound.ENTITY_POLAR_BEAR_DEATH,1.0f,1.5f + new Random().nextFloat() * 0.5f);
              player.getWorld().playSound(player.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,0.5f,0.5f);
              player.damage(2);
            }
          }
        });
      });
    });
  }
}
