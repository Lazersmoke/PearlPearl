package com.github.lazersmoke.PearlPearl;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
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

public class PearlPearlPearlListGUI{
  private final UUID uuid;
  private final Map<UUID,PearlPearlPearl> allPearls;
  private int currentPage;

  public PearlPearlPearlListGUI(UUID uuid, Map<UUID,PearlPearlPearl> allPearls){
    this.uuid = uuid;
    this.allPearls = allPearls;
    this.currentPage = 0;
  }

  public void showScreen() {
    Player p = Bukkit.getPlayer(uuid);
    if(p == null){
      return;
    }
    ClickableInventory.forceCloseInventory(p);
    ClickableInventory ci = new ClickableInventory(54, "Pearl Pearls");
    Set<PearlPearlPearl> pearls = allPearls.entrySet().stream().map(Map.Entry::getValue).filter(pe -> pe.pearledId.equals(uuid)).collect(Collectors.toSet());
    if (pearls.size() < 45 * currentPage) {
      // would show an empty page, so go to previous
      currentPage--;
      showScreen();
    }
    if (pearls.size() != 0) {
      int nextSlot = 0;
      for(PearlPearlPearl pearl : pearls.stream().skip(45 * currentPage).limit(45).collect(Collectors.toList())){
        ci.setSlot(createPearlDetail(pearl), nextSlot++);
      }
    }
    // previous button
    if (currentPage > 0) {
      ItemStack back = new ItemStack(Material.ARROW);
      ISUtils.setName(back, ChatColor.GOLD + "Go to previous page");
      Clickable baCl = new Clickable(back) {

        @Override
        public void clicked(Player arg0) {
          if (currentPage > 0) {
            currentPage--;
          }
          showScreen();
        }
      };
      ci.setSlot(baCl, 45);
    }
    // next button
    if ((45 * (currentPage + 1)) <= pearls.size()) {
      ItemStack forward = new ItemStack(Material.ARROW);
      ISUtils.setName(forward, ChatColor.GOLD + "Go to next page");
      Clickable forCl = new Clickable(forward) {

        @Override
        public void clicked(Player arg0) {
          if ((45 * (currentPage + 1)) <= pearls.size()) {
            currentPage++;
          }
          showScreen();
        }
      };
      ci.setSlot(forCl, 53);
    }
    ci.showInventory(p);
  }

  private Clickable createPearlDetail(PearlPearlPearl pearl) {
    ItemStack toShow = pearl.getItemRepr();
    return new Clickable(toShow) {
      @Override
      public void clicked(Player p) {
        p.sendMessage(ChatColor.GOLD + "Pearl Pearl Info:");
        for(String s : pearl.getDetail()){
          p.sendMessage(ChatColor.GOLD + "  " + s);
        }
        pearl.aggroVerify();
        showScreen();
      }
    };
  }
}
