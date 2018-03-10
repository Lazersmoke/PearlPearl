package com.github.lazersmoke.PearlPearl;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import vg.civcraft.mc.civmodcore.util.ConfigParsing;

public final class PearlPearlConfig{
  public static double pearlDecayBase;
  public static double pearlDecayScale;
  public static double pearlDecayRange;
  public static int exileRadius;
  public static long pearlDecayInterval;
  public static final Map<PearlPearlBehavior,Integer> behaviorCosts = new LinkedHashMap<PearlPearlBehavior,Integer>();
  public static final Map<PearlPearlBehavior,Integer> behaviorUpgradeCosts = new LinkedHashMap<PearlPearlBehavior,Integer>();
  public static final Set<PearlPearlBehavior> enabledBehaviors = new HashSet<PearlPearlBehavior>();
  public static World prisonWorld;

  public void reloadConfig(FileConfiguration config){
    pearlDecayBase = config.getDouble("pearlDecayBase", 0.1);
    pearlDecayScale = config.getDouble("pearlDecayScale", 10.0);
    pearlDecayRange = config.getDouble("pearlDecayRange", 100.0);
    exileRadius = config.getInt("exileRadius", 1000);
    pearlDecayInterval = ConfigParsing.parseTime(config.getString("pearlDecayInterval", "1s"));
    prisonWorld = Bukkit.getWorld(config.getString("prisonWorld", "world_the_end"));
    enabledBehaviors.clear();
    for(PearlPearlBehavior b : PearlPearlBehavior.values()){
      ConfigurationSection thisBehavior = config.getConfigurationSection("enabledBehaviors").getConfigurationSection(b.toString());
      if(thisBehavior != null){
        enabledBehaviors.add(b);
        behaviorCosts.put(b, thisBehavior.getInt("cost"));
        behaviorUpgradeCosts.put(b, thisBehavior.getInt("upgradeCost"));
      }
    }
  }
}
