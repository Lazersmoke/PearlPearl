package com.github.lazersmoke.PearlPearl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.UUID;
import java.util.Comparator;
import vg.civcraft.mc.namelayer.NameAPI;

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

  @EventHandler
  public void onDropPearl(PlayerDropItemEvent e){
    Item i = e.getItemDrop();
    PearlPearlPearl.fromItemStack(i.getItemStack()).ifPresent(pearl -> {
      pearl.setHolder(new PearlPearlPearl.PearlHolder.Dropped(i.getUniqueId()));
      i.setItemStack(pearl.getItemRepr());
    });
  }

  @EventHandler
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

  @EventHandler
  public void onClickPearl(InventoryClickEvent e){
    switch(e.getAction()){
      // Inventory -> Cursor
      case COLLECT_TO_CURSOR:
      case PICKUP_ALL:
      case PICKUP_HALF:
      case PICKUP_ONE:
        PearlPearlPearl.fromItemStack(e.getCurrentItem()).ifPresent(pearl -> pearl.setHolder(new PearlPearlPearl.PearlHolder.Player(((Player) e.getWhoClicked()).getUniqueId())));
      // Cursor -> Inventory
      case PLACE_ALL:
      case PLACE_SOME:
      case PLACE_ONE:
        PearlPearlPearl.fromItemStack(e.getCurrentItem()).ifPresent(pearl -> {
          Function<InventoryView,Inventory> getInventory = e.getRawSlot() < event.getView().getTopInventory().getSize() ? InventoryView::getTopInventory : InventoryView::getBottomInventory;
          Optional<PearlHolder> holder = PearlPearlPearl.fromInventoryHolder(getInventory.apply(e.getView()).getHolder());
          /* Java 9:
          holder.ifPresentOrElse(pearl::setHolder,...);
          */
          if(holder.isPresent){
            pearl.setHolder(holder.get());
          }else{
            Optional.ofNullable(Bukkit.getPlayer(pearl.pearledId)).ifPresent(p -> p.sendMessage(ChatColor.RED + "Your pearl was placed in an inventory, but that inventory wasn't a valid InventoryHolder :V"));
          }
        });
    }
  }
}
