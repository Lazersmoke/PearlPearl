package com.github.lazersmoke.PearlPearl;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.time.Duration;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.civmodcore.inventorygui.Clickable;
import vg.civcraft.mc.civmodcore.inventorygui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;
import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.civmodcore.itemHandling.ItemMap;

public class PearlPearlPearlBehaviorGUI{
  private final UUID uuid;
  private final UUID pearlUUID;
  private PearlPearlPearl pearl;

  public PearlPearlPearlBehaviorGUI(UUID uuid, UUID pearlUUID){
    this.uuid = uuid;
    this.pearlUUID = pearlUUID;
  }

  public void showScreen() {
    Player p = Bukkit.getPlayer(uuid);
    if(p == null){
      return;
    }
    ClickableInventory.forceCloseInventory(p);
    pearl = PearlPearlPearl.getPearlByUUID(pearlUUID).orElse(null);
    if(pearl == null){
      return;
    }
    ClickableInventory ci = new ClickableInventory(54, "Pearl Options");
    PearlPearlConfig c = PearlPearl.getConfiguration();
    if (c.enabledBehaviors.size() != 0) {
      int nextSlot = 0;
      for(PearlPearlBehavior b : c.enabledBehaviors.stream().limit(54).collect(Collectors.toList())){
        ci.setSlot(createBehaviorDetail(b), nextSlot++);
      }
    }
    ci.showInventory(p);
  }

  private Clickable createBehaviorDetail(PearlPearlBehavior b) {
    ItemStack toShow = null;
    if(pearl.exhibitsBehavior(b)){
      toShow = b.displayStack;
    }else{
      toShow = new ItemStack(Material.RABBIT_FOOT);
    }
    ItemMeta im = toShow.getItemMeta();
    im.setDisplayName(b.name);
    im.setLore(Arrays.asList(b.lore));
    toShow.setItemMeta(im);
    return new Clickable(toShow) {
      @Override
      public void clicked(Player p) {
        boolean active = pearl.exhibitsBehavior(b);
        p.sendMessage((active ? ChatColor.RED + "Disabled " : ChatColor.GREEN + "Enabled ") + b.name + " on that pearl");
        pearl.setBehavior(b,!active);
        showScreen();
      }
    };
  }
}
