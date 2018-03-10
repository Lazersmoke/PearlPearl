package com.github.lazersmoke.PearlPearl;

import org.bukkit.Bukkit;
import org.bukkit.World;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryAction;
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
    PearlPearlPearl.updateItemStack(i.getItemStack(),i::setItemStack).ifPresent(pearl -> {
      pearl.setHolder(new PearlPearlHolder.Dropped(i.getUniqueId()));
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
      pearl.setHolder(new PearlPearlHolder.Player(p.getUniqueId()));
      i.setItemStack(pearl.getItemRepr());
    });
  }

  @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
  public void onPearlDespawn(ItemDespawnEvent e){
    Item i = e.getEntity();
    e.setCancelled(PearlPearlPearl.updateItemStack(i.getItemStack(),i::setItemStack).isPresent());
  }

  @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
  public void onChunkUnload(ChunkUnloadEvent e) {
    e.setCancelled(Arrays.stream(e.getChunk().getEntities()).anyMatch(i -> i instanceof Item && PearlPearlPearl.updateItemStack(((Item) i).getItemStack(),s -> ((Item) i).setItemStack(s)).isPresent()));
    
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
      .filter(i -> PearlPearlPearl.updateItemStack(inv.getItem(i),s -> inv.setItem(i,s)).isPresent())
      .forEach(i -> {
        // Drop in world
        p.getWorld().dropItemNaturally(p.getLocation(), inv.getItem(i));
        // Remove from player inventory
        inv.clear(i);
      });
    p.saveData();
  }

  @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
  public void onDragPearl(InventoryDragEvent e){
    e.getNewItems().keySet().stream().forEach(i -> PearlPearlPearl.fromItemStack(e.getNewItems().get(i)).ifPresent(pearl -> {
      Inventory destinationInv = i < e.getView().getTopInventory().getSize() ? e.getView().getTopInventory() : e.getView().getBottomInventory();
      ifPresentOrElse(Optional.ofNullable(destinationInv.getHolder()).flatMap(PearlPearlHolder::fromInventory),pearl::setHolder,() -> denyClick(e));
    }));
  }

  @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
  public void onClickPearl(InventoryClickEvent e){
    Player clicker = (Player) e.getWhoClicked();
    PearlPearlHolder clickerHolder = new PearlPearlHolder.Player(clicker.getUniqueId());
    final Optional<PearlPearlHolder> otherHolder = Optional.ofNullable(e.getClickedInventory()).map(Inventory::getHolder).flatMap(PearlPearlHolder::fromInventory);
    final Optional<PearlPearlPearl> currentPearl = PearlPearlPearl.updateItemStack(e.getCurrentItem(),e::setCurrentItem);
    final Optional<PearlPearlPearl> cursorPearl = PearlPearlPearl.fromItemStack(e.getCursor());
    final Inventory clickerInv = clicker.getInventory();
    Optional<PearlPearlPearl> hotbarPearl = Optional.empty();
    if(e.getAction() == InventoryAction.HOTBAR_SWAP || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD){
      hotbarPearl = PearlPearlPearl.updateItemStack(clickerInv.getItem(e.getHotbarButton()),s -> clickerInv.setItem(e.getHotbarButton(),s));
    }
    // Did we click a non-player inventory?
    boolean clickedOther = e.getRawSlot() < e.getView().getTopInventory().getSize();
    if(!clickedOther && e.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY){
      return;
    }
    // Don't allow pearls in invalid slots
    switch(e.getAction()){
      // Inventory -> Cursor
      case COLLECT_TO_CURSOR:
      case PICKUP_ALL:
      case PICKUP_HALF:
      case PICKUP_ONE:
        currentPearl.ifPresent(p -> p.setHolder(clickerHolder));
        break;
      // Cursor -> Inventory
      case PLACE_ALL:
      case PLACE_SOME:
      case PLACE_ONE:
        cursorPearl.ifPresent(pearl -> ifPresentOrElse(otherHolder,pearl::setHolder,() -> denyClick(e)));
        break;
      // Shift click
      case MOVE_TO_OTHER_INVENTORY:
        currentPearl.ifPresent(pearl -> {
          // The inventory the pearl will move to
          Inventory destinationInv = clickedOther ? e.getView().getBottomInventory() : e.getView().getTopInventory();
          ifPresentOrElse(Optional.ofNullable(destinationInv.getHolder()).flatMap(PearlPearlHolder::fromInventory),pholder -> {
            // If the PearlPearlHolder is legitimate, use our own handling
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
        currentPearl.ifPresent(pearl -> pearl.setHolder(clickerHolder));
        hotbarPearl.ifPresent(pearl -> ifPresentOrElse(otherHolder,pearl::setHolder,() -> denyClick(e)));
        break;
      case SWAP_WITH_CURSOR:
        currentPearl.ifPresent(p -> p.setHolder(clickerHolder));
        cursorPearl.ifPresent(p -> ifPresentOrElse(otherHolder,p::setHolder,() -> denyClick(e)));
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
        if(currentPearl.isPresent()){
          denyClick(e);
        }
    }
  }

  public static <T> void ifPresentOrElse(Optional<T> o, Consumer<T> f, Runnable def){
    if(o.isPresent()){
      f.accept(o.get());
    }else{
      def.run();
    }
  }

  private void denyClick(InventoryInteractEvent e){
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
    PearlPearlPearl.updateItemStack(i.getItemStack(),i::setItemStack).ifPresent(pearl -> {
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
    e.setCancelled(e.getEntity() instanceof Item && PearlPearlPearl.updateItemStack(((Item) e.getEntity()).getItemStack(),s -> ((Item) e.getEntity()).setItemStack(s)).isPresent());
  }

  @EventHandler(ignoreCancelled=true)
  public void onHopperSuckPearl(InventoryPickupItemEvent e) {
    Item i = e.getItem();
    PearlPearlPearl.updateItemStack(i.getItemStack(),i::setItemStack).ifPresent(pearl -> {
      ifPresentOrElse(Optional.ofNullable(e.getInventory().getHolder()).flatMap(PearlPearlHolder::fromInventory),pearl::setHolder,() -> e.setCancelled(true));
    });
  }

  @EventHandler(ignoreCancelled=true)
  public void onHopperMovePearl(InventoryMoveItemEvent e) {
    PearlPearlPearl.fromItemStack(e.getItem()).ifPresent(pearl -> {
      ifPresentOrElse(Optional.ofNullable(e.getDestination().getHolder()).flatMap(PearlPearlHolder::fromInventory),pearl::setHolder,() -> e.setCancelled(true));
    });
  }

  @EventHandler(ignoreCancelled=true)
  public void onPlayerJoin(PlayerJoinEvent e){
    Player p = e.getPlayer();
    if(isPrisonPearled(p.getUniqueId()) && !p.getWorld().equals(PearlPearl.getConfiguration().prisonWorld)){
      p.teleport(PearlPearl.getConfiguration().prisonWorld.getSpawnLocation().add(0,0.5,0));
    }
  }

  @EventHandler(ignoreCancelled=true)
  public void onPlayerRespawn(PlayerRespawnEvent e){
    Player p = e.getPlayer();
    if(isPrisonPearled(p.getUniqueId())){
      e.setRespawnLocation(PearlPearl.getConfiguration().prisonWorld.getSpawnLocation().add(0,0.5,0));
    }
  }

  @EventHandler(ignoreCancelled=true)
  public void onEscapeEndViaPortal(PlayerPortalEvent e){
    Player p = e.getPlayer();
    if(e.getCause() == TeleportCause.END_PORTAL && isPrisonPearled(p.getUniqueId())){
      e.setTo(PearlPearl.getConfiguration().prisonWorld.getSpawnLocation().add(0,0.5,0));
    }
  }

  private boolean isPrisonPearled(UUID u){
    return PearlPearlPearl.getPearlsForUUID(u).stream().anyMatch(pe -> pe.exhibitsBehavior(PearlPearlBehavior.PRISON));
  }
}
