package com.github.lazersmoke.PearlPearl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.entity.EnderPearl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.UUID;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import vg.civcraft.mc.namelayer.NameAPI;
import net.md_5.bungee.api.ChatColor;

public final class PearlPearlListener implements Listener{
  private static final Map<UUID,AttackRecord> attacks = new HashMap<UUID,AttackRecord>();

  @EventHandler
  public void onPlayerKilled(PlayerDeathEvent e){
    Player p = e.getEntity();
    Optional.ofNullable(attacks.get(p.getUniqueId())).flatMap(r -> r.getMajorityStakeholder()).ifPresent(pearler -> {
      String pearledName = NameAPI.getCurrentName(p.getUniqueId());
      String pearlerName = NameAPI.getCurrentName(pearler);
      Player pearlerPlayer = Bukkit.getPlayer(pearler);
      e.setDeathMessage(pearledName + " just got pearled by " + pearlerName + ". Press F to pay respects.");
      PearlPearlPearl thePearl = PearlPearlPearl.makePearlingHappen(p.getUniqueId(), pearlerPlayer);
    });
  }

  @EventHandler
  public void onPlayerKilled(EntityDamageByEntityEvent e){
    if(!(e.getEntity() instanceof Player && e.getDamager() instanceof Player)){
      return;
    }
    UUID defender = ((Player) e.getEntity()).getUniqueId();
    UUID attacker = ((Player) e.getDamager()).getUniqueId();
    double strength = e.getFinalDamage();
    // Put this in for the death event to catch
    attacks.compute(defender,(k,v) -> {
      if(v == null){
        return new AttackRecord(attacker,strength);
      }else{
        v.addAttack(attacker,strength);
        return v;
      }
    });
  }

  private static class AttackRecord{
    private final Map<UUID,Double> attackValues;

    public AttackRecord(UUID attacker, double strength){
      this.attackValues = new HashMap<UUID,Double>();
      addAttack(attacker,strength);
    }

    public void addAttack(UUID attacker, double strength){
      attackValues.compute(attacker, (k,v) -> v == null ? strength : v + strength);
      Bukkit.getScheduler().runTaskLater(PearlPearl.getInstance(),() -> removeAttack(attacker,strength), 200L);
    }

    public Optional<UUID> getMajorityStakeholder(){
      Comparator<Map.Entry<UUID,Double>> c = Comparator.comparingDouble(Map.Entry::getValue);
      return attackValues.entrySet().stream().max(c).map(e -> e.getKey());
    }

    private void removeAttack(UUID attacker, double strength){
      attackValues.compute(attacker, (k,v) -> v == null ? null : strength + 0.0001d >= v ? null : v - strength);
    }
  }

  @EventHandler(ignoreCancelled=true)
  public void onSpawnPearlItem(ItemSpawnEvent e){
    Item i = e.getEntity();
    PearlPearlPearl.fromItemStack(i.getItemStack()).ifPresent(pearl -> {
      pearl.setHolder(new PearlPearlPearl.PearlHolder.Dropped(i.getUniqueId()));
      i.setItemStack(pearl.getItemRepr());
    });
  }

  @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
  public void onPickupPearl(EntityPickupItemEvent e){
    if(!(e.getEntity() instanceof Player)){
      return;
    }
    Player p = (Player) e.getEntity();
    Item i = e.getItem();
    PearlPearlPearl.fromItemStack(i.getItemStack()).ifPresent(pearl -> {
      pearl.setHolder(new PearlPearlPearl.PearlHolder.Player(p.getUniqueId()));
      i.setItemStack(pearl.getItemRepr());
    });
  }

  @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
  public void onPearlDespawn(ItemDespawnEvent e){
    e.setCancelled(PearlPearlPearl.fromItemStack(e.getEntity().getItemStack()).isPresent());
  }

  @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
  public void onChunkUnload(ChunkUnloadEvent e) {
    e.setCancelled(Arrays.stream(e.getChunk().getEntities()).anyMatch(i -> i instanceof Item && PearlPearlPearl.fromItemStack(((Item) i).getItemStack()).isPresent()));
    
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent e) {
    Player p = e.getPlayer();

    // Ignore if a player is just turning into a combat logger
    if(PearlPearl.isTagged(p.getUniqueId())){
      return;
    }

    Inventory inv = p.getInventory();
    IntStream.range(0,inv.getSize())
      // On all the pearls
      .filter(i -> PearlPearlPearl.fromItemStack(inv.getItem(i)).isPresent())
      .forEach(i -> {
        // Drop in world
        p.getWorld().dropItemNaturally(p.getLocation(), inv.getItem(i));
        // Remove from player inventory
        inv.clear(i);
      });
    p.saveData();
  }

  @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
  public void onDragPearl(InventoryDragEvent e){
    e.getNewItems().keySet().stream().forEach(i -> PearlPearlPearl.fromItemStack(e.getNewItems().get(i)).ifPresent(pearl -> {
      InventoryHolder holder = e.getView().convertSlot(i) == i ? e.getView().getTopInventory().getHolder() : e.getView().getBottomInventory().getHolder();
      Optional<PearlPearlPearl.PearlHolder> pholder = PearlPearlPearl.PearlHolder.fromInventory(holder);
      if(pholder.isPresent()){
        pearl.setHolder(pholder.get());
      }else{
        Optional.ofNullable(Bukkit.getPlayer(pearl.pearledId)).ifPresent(p -> p.sendMessage(ChatColor.RED + "Your pearl was placed in an inventory, but that inventory wasn't a valid PearlHolder :V"));
      }
    }));
  }

  @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
  public void onClickPearl(InventoryClickEvent e){
    // Did we click a non-player inventory?
    boolean clickedOther = e.getRawSlot() < e.getView().getTopInventory().getSize();
    Player clicker = (Player) e.getWhoClicked();
    PearlPearlPearl.PearlHolder clickerHolder = new PearlPearlPearl.PearlHolder.Player(clicker.getUniqueId());
    Optional<PearlPearlPearl.PearlHolder> otherHolder = Optional.ofNullable(e.getClickedInventory().getHolder()).flatMap(PearlPearlPearl.PearlHolder::fromInventory);
    Optional<PearlPearlPearl> clickedPearl = PearlPearlPearl.fromItemStack(e.getCurrentItem());
    Optional<PearlPearlPearl> cursorPearl = PearlPearlPearl.fromItemStack(e.getCursor());
    switch(e.getAction()){
      // Inventory -> Cursor
      case COLLECT_TO_CURSOR:
      case PICKUP_ALL:
      case PICKUP_HALF:
      case PICKUP_ONE:
        if(clickedOther){
          clickedPearl.ifPresent(pearl -> pearl.setHolder(clickerHolder));
        }
        break;
      // Cursor -> Inventory
      case PLACE_ALL:
      case PLACE_SOME:
      case PLACE_ONE:
        if(clickedOther){
          cursorPearl.ifPresent(pearl -> ifPresentOrElse(otherHolder,pearl::setHolder,() -> denyClick(e)));
        }
        break;
      // Shift click
      case MOVE_TO_OTHER_INVENTORY:
        clickedPearl.ifPresent(pearl -> {
          // The inventory the pearl will move to
          Inventory destinationInv = clickedOther ? e.getView().getBottomInventory() : e.getView().getTopInventory();
          ifPresentOrElse(Optional.ofNullable(destinationInv.getHolder()).flatMap(PearlPearlPearl.PearlHolder::fromInventory),pholder -> {
            // If the PearlHolder is legitimate, use our own handling
            e.setCancelled(true);
            // If we successfully transferred the pearl
            if(destinationInv.addItem(pearl.getItemRepr()).isEmpty()){
              // Delete the old one and set the new holder
              e.getClickedInventory().removeItem(e.getCurrentItem());
              pearl.setHolder(pholder);
            }
          },() -> denyClick(e));
        });
        break;
      case HOTBAR_SWAP:
      case HOTBAR_MOVE_AND_READD:
        if(clickedOther){
          clickedPearl.ifPresent(pearl -> pearl.setHolder(clickerHolder));
          PearlPearlPearl.fromItemStack(clicker.getInventory().getItem(e.getHotbarButton())).ifPresent(pearl -> ifPresentOrElse(otherHolder,pearl::setHolder,() -> denyClick(e)));
        }
        break;
      case SWAP_WITH_CURSOR:
        if(clickedOther){
          clickedPearl.ifPresent(pearl -> pearl.setHolder(clickerHolder));
          cursorPearl.ifPresent(pearl -> ifPresentOrElse(otherHolder,pearl::setHolder,() -> denyClick(e)));
        }
        break;
      case DROP_ALL_CURSOR:
      case DROP_ONE_CURSOR:
      case DROP_ALL_SLOT:
      case DROP_ONE_SLOT:
        // Caught when item is created
        break;
      case NOTHING:
        // No change in holder
        break;
      case CLONE_STACK:
      case UNKNOWN:
      default:
        if(clickedPearl.isPresent() || cursorPearl.isPresent()){
          denyClick(e);
        }
    }
  }

  private <T> void ifPresentOrElse(Optional<T> o, Consumer<T> f, Runnable def){
    if(o.isPresent()){
      f.accept(o.get());
    }else{
      def.run();
    }
  }

  private void denyClick(InventoryClickEvent e){
    e.getWhoClicked().sendMessage(ChatColor.RED + "You can't do that with a pearl, you monster!");
    e.setCancelled(true);
  }

  // Make pearls invincible except to void damage
  @EventHandler(ignoreCancelled=true)
  public void onPearlDamaged(EntityDamageEvent e){
    if(!(e.getEntity() instanceof Item)){
      return;
    }
    Item i = (Item) e.getEntity();
    PearlPearlPearl.fromItemStack(i.getItemStack()).ifPresent(pearl -> {
      if(e.getCause() == EntityDamageEvent.DamageCause.VOID){
        pearl.freePearl("your pearl fell in the void");
      }else{
        e.setCancelled(true);
      }
    });
  }

  @EventHandler(ignoreCancelled=true)
  public void onPearlThrow(ProjectileLaunchEvent e){
    if(!(e.getEntity() instanceof EnderPearl)){
      return;
    }
    Player p = (Player) e.getEntity().getShooter();
    if(p == null){
      return;
    }
    PearlPearlPearl.fromItemStack(p.getInventory().getItemInMainHand()).ifPresent(pearl -> {
      p.sendMessage(ChatColor.RED + "You can't throw Pearl Pearls");
      e.setCancelled(true);
      // Need to schedule this or else the re-created pearl doesn't show up
      Bukkit.getScheduler().scheduleSyncDelayedTask(PearlPearl.getInstance(), () -> p.getInventory().setItemInMainHand(pearl.getItemRepr()));
    });
  }

  @EventHandler(ignoreCancelled=true)
  public void onPearlPortal(EntityPortalEvent e) {
    e.setCancelled(e.getEntity() instanceof Item && PearlPearlPearl.fromItemStack(((Item) e.getEntity()).getItemStack()).isPresent());
  }
}
