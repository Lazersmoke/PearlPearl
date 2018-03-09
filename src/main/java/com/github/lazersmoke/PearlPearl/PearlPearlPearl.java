package com.github.lazersmoke.PearlPearl;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import vg.civcraft.mc.civmodcore.itemHandling.ISUtils;
import vg.civcraft.mc.namelayer.NameAPI;
import net.md_5.bungee.api.ChatColor;

public final class PearlPearlPearl{
  private static final String pearlIdPrefix = "" + ChatColor.DARK_GRAY + ChatColor.ITALIC + "Pearl Id ";
  private static final PearlPearlDAO dao = PearlPearl.getDAO();
  private static final Map<UUID,PearlPearlPearl> allPearls = new HashMap<UUID,PearlPearlPearl>();
  public final UUID uniqueId;
  public final UUID pearledId;
  public final UUID pearlerId;
  public final Instant timePearled;
  private final Set<PearlPearlBehavior> behaviors = new HashSet<PearlPearlBehavior>();
  private PearlPearlHolder holder;
  private long damage;

  public PearlPearlPearl(UUID uniqueId, UUID pearledId, UUID pearlerId, Instant timePearled, PearlPearlHolder holder, long damage){
    this.uniqueId = uniqueId;
    this.pearledId = pearledId;
    this.pearlerId = pearlerId;
    this.timePearled = timePearled;
    this.holder = holder;
    this.damage = damage;
  }

  public static PearlPearlPearl makePearlingHappen(UUID pearled, Player pearler){
    String pearledName = NameAPI.getCurrentName(pearled);
    String pearlerName = NameAPI.getCurrentName(pearler.getUniqueId());
    pearler.sendMessage(ChatColor.GREEN + "You pearled " + ChatColor.AQUA + pearledName);
    Optional.ofNullable(Bukkit.getPlayer(pearled)).ifPresent(p -> p.sendMessage(ChatColor.GOLD + "You got pearled by " + ChatColor.AQUA + pearlerName));
    PearlPearlPearl pearl = new PearlPearlPearl(UUID.randomUUID(), pearled, pearler.getUniqueId(), Instant.now(), new PearlPearlHolder.Player(pearler.getUniqueId()), 0);
    allPearls.put(pearl.uniqueId,pearl);
    dao.addNewPearl(pearl);
    ItemStack pearlItem = pearl.getItemRepr();
    // If the pearl couldn't fit in their inventory...
    if(pearler.getInventory().addItem(pearlItem).size() != 0){
      // ... drop it on the ground
      Item droppedPearl = pearler.getWorld().dropItem(pearler.getLocation().add(0,0.5,0),pearlItem);
      droppedPearl.setPickupDelay(10);
      pearl.setHolder(new PearlPearlHolder.Dropped(droppedPearl.getUniqueId()));
    }else{
      pearl.setHolder(new PearlPearlHolder.Player(pearler.getUniqueId()));
    }
    return pearl;
  }

  public PearlPearlHolder getHolder(){
    return holder;
  }

  public long getDamage(){
    return damage;
  }

  public void setHolder(PearlPearlHolder newHolder){
    Bukkit.getScheduler().runTask(PearlPearl.getInstance(), () -> {
      Optional.ofNullable(Bukkit.getPlayer(pearledId)).ifPresent(p -> p.sendMessage(ChatColor.RESET + "Your pearl is now held by " + ChatColor.GOLD + newHolder.getDisplay()));
    });
    holder = newHolder;
    Bukkit.getScheduler().runTaskAsynchronously(PearlPearl.getInstance(), () -> dao.updateHolder(this));
  }

  public static Optional<PearlPearlPearl> updateItemStack(ItemStack i, Consumer<ItemStack> update){
    Optional<UUID> pearlId = pearlIdFromItemStack(i);
    Optional<PearlPearlPearl> pearl = pearlId.flatMap(u -> Optional.ofNullable(allPearls.get(u)));
    pearlId.ifPresent(u -> PearlPearlListener.ifPresentOrElse(pearl,p -> update.accept(p.getItemRepr()), () -> update.accept(new ItemStack(Material.ENDER_PEARL))));
    return pearl;
  }

  public static Optional<PearlPearlPearl> fromItemStack(ItemStack i){
    return pearlIdFromItemStack(i).flatMap(u -> Optional.ofNullable(allPearls.get(u)));
  }

  public static Optional<UUID> pearlIdFromItemStack(ItemStack i){
    if(i == null || !i.hasItemMeta() || i.getItemMeta() == null || !i.getItemMeta().hasLore()){
      return Optional.empty();
    }
    try{
      return Optional.of(UUID.fromString(i.getItemMeta().getLore().get(4).substring(pearlIdPrefix.length())));
    } catch(Exception e){}
    // Doesn't look like a pearl
    return Optional.empty();
  }

  public ItemStack getItemRepr(){
    ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
    ItemMeta im = pearl.getItemMeta();
    im.setDisplayName(NameAPI.getCurrentName(pearledId));
    List<String> lore = getDetail();
    im.setLore(lore);
    im.addEnchant(Enchantment.DURABILITY, 1, true);
    im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    pearl.setItemMeta(im);
    return pearl;
  }

  public List<String> getDetail(){
    List<String> lines = new ArrayList<String>();
    lines.add(ChatColor.RESET + "Held by " + ChatColor.GOLD + holder.getDisplay());
    lines.add(ChatColor.RESET + "Time pearled: " + ChatColor.DARK_GREEN + ChatColor.ITALIC + timePearled.toString());
    lines.add(ChatColor.RESET + "Pearled by: " + ChatColor.AQUA + NameAPI.getCurrentName(pearlerId));
    lines.add(ChatColor.RESET + "Damage: " + ChatColor.GOLD + damage);
    lines.add(pearlIdPrefix + uniqueId.toString());
    return lines;
  }

  public static Set<PearlPearlPearl> getPearlsForUUID(UUID u){
    return allPearls.values().stream().filter(p -> p.pearledId.equals(u)).collect(Collectors.toSet());
  }

  public static void loadPearls(){
    allPearls.clear();
    dao.loadPearls(allPearls);
  }

  public Optional<Location> aggroVerify(){
    Optional<Location> verifiedLocation = holder.verify(this);
    if(!verifiedLocation.isPresent()){
      freePearl("pearl verification failed");
    }
    return verifiedLocation;
  }

  public void freePearl(String reason){
    Bukkit.getScheduler().runTaskAsynchronously(PearlPearl.getInstance(), () -> dao.snipePearl(uniqueId));
    allPearls.remove(uniqueId);
    Optional.ofNullable(Bukkit.getPlayer(pearledId)).ifPresent(p -> p.sendMessage(ChatColor.GREEN + "You were freed because " + reason));
  }

  public static LinkedList<PearlPearlPearl> getAllPearlsSnapshot(){
    return new LinkedList<PearlPearlPearl>(allPearls.values());
  }

  public static void verifyAllPearls(){
    getAllPearlsSnapshot().forEach(PearlPearlPearl::aggroVerify);
  }

  public void takePearlCostFrom(PearlPearlPearl other){
    if(other.uniqueId.equals(uniqueId)){
      return;
    }
    double distance = other.aggroVerify()
      .flatMap(o -> aggroVerify().map(o::distance))
      .orElse(0.0);
    PearlPearlConfig c = PearlPearl.getConfiguration();
    long damageTaken = (long) (c.pearlDecayScale * Math.pow(c.pearlDecayBase,distance/c.pearlDecayRange));
    damage += damageTaken;
    System.out.println(uniqueId + " is taking " + damageTaken + " from " + other.uniqueId);
    Bukkit.getScheduler().runTaskAsynchronously(PearlPearl.getInstance(), () -> dao.updateDamage(this));
  }

  public void takeDamage(long dmg){
    damage += dmg;
  }

  public static Optional<PearlPearlPearl> getPearlByUUID(UUID u){
    return Optional.ofNullable(allPearls.get(u));
  }

  public boolean exhibitsBehavior(PearlPearlBehavior b){
    return behaviors.contains(b);
  }

  public void setBehavior(PearlPearlBehavior b, boolean enable){
    if(enable){
      behaviors.add(b);
    }else{
      behaviors.remove(b);
    }
  }

  public static void pearlDecay(){
    LinkedList<PearlPearlPearl> allPearls = getAllPearlsSnapshot();
    allPearls.forEach(p -> {
      PearlPearlConfig c = PearlPearl.getConfiguration();
      // Take extra damage for enabled behaviors
      p.behaviors.forEach(b -> p.takeDamage(Optional.ofNullable(c.behaviorCosts.get(b)).map(Integer::longValue).orElse(0L)));
      // Take one damage first
      p.takeDamage(1L);
      // Take extra damage for nearby pearls
      allPearls.forEach(p::takePearlCostFrom);
    });
  }

  public static Set<PearlPearlPearl> pearlsWithBehavior(PearlPearlBehavior b){
    return allPearls.values().stream().filter(p -> p.exhibitsBehavior(b)).collect(Collectors.toSet());
  }
}
