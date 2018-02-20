package com.github.lazersmoke.PearlPearl;

import java.io.File;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.function.UnaryOperator;
import java.util.function.Function;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public final class PearlPearlConfig{
  private Set<Material> materialsModified;

  public boolean isModified(Material mat){
    return materialsModified.contains(mat);
  }

  public void reloadConfig(FileConfiguration config){
    materialsModified = new HashSet<Material>();
    List<String> drops = config.getStringList("drops");
    if(drops != null){
      for(String item : drops){
        Material mat = Material.getMaterial(item);
        if(mat != null){
          materialsModified.add(mat);
        }
      }
    }
  }
}
