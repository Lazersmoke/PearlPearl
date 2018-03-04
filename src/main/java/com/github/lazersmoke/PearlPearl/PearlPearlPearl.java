package com.github.lazersmoke.PearlPearl;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Hopper;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.BlockState;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.nio.ByteBuffer;
import java.util.function.Function;
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
  private PearlHolder holder;

  public enum PearlHolderType{
    PLAYER(0,PearlHolder.Player::deserialize),
    DROPPED(1,PearlHolder.Dropped::deserialize),
    BLOCK(2,PearlHolder.Block::deserialize);

    public static final Map<Integer,PearlHolderType> holderTypes = new HashMap<Integer,PearlHolderType>();
    static{
      for(PearlHolderType t : PearlHolderType.values()){
        holderTypes.put(t.magic,t);
      }
    }
    public final Function<byte[],PearlHolder> deserialize;
    public final int magic;

    private PearlHolderType(int magic, Function<byte[],PearlHolder> f){
      this.magic = magic;
      this.deserialize = f;
    }
  }

  public static PearlHolder deserializePearlHolder(int type, byte[] data){
    return PearlHolderType.holderTypes.get(type).deserialize.apply(data);
  }

  public static abstract class PearlHolder{
    public abstract boolean verify(PearlPearlPearl pearl);
    public abstract Location getLocation();
    public abstract String getDisplay();
    public abstract byte[] serialize();
    public final PearlHolderType type;

    private PearlHolder(PearlHolderType t){
      this.type = t;
    }

    private static byte[] getBytesFromUUID(UUID uuid) {
      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      bb.putLong(uuid.getMostSignificantBits());
      bb.putLong(uuid.getLeastSignificantBits());
      return bb.array();
    }

    private static UUID getUUIDFromBytes(byte[] bytes) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      Long high = byteBuffer.getLong();
      Long low = byteBuffer.getLong();
      return new UUID(high, low);
    }

    
    public static Optional<PearlHolder> fromInventory(InventoryHolder holder){
      if(holder instanceof Chest || holder instanceof Furnace || holder instanceof Dispenser || holder instanceof Dropper || holder instanceof Hopper || holder instanceof BrewingStand){
        return Optional.of(new Block(((BlockState)holder).getLocation()));
      // DoubleChest is not a BlockState -.-
      }else if(holder instanceof DoubleChest) {
        return Optional.of(new Block(((DoubleChest)holder).getLocation()));
      }else if(holder instanceof org.bukkit.entity.Player) {
        return Optional.of(new Player(((org.bukkit.entity.Player)holder).getUniqueId()));
      }else{
        return Optional.empty();
      }
    }

    public static final class Player extends PearlHolder{
      private final UUID holdingPlayerId;

      public Player(UUID holdingPlayer){
        super(PearlHolderType.PLAYER);
        this.holdingPlayerId = holdingPlayer;
      }

      // Verifies that the holding player is online, and that they are either
      // in creative, or holding the pearl in their inventory, crafting grid, or cursor
      public boolean verify(PearlPearlPearl pearl){
        return Optional.ofNullable(Bukkit.getPlayer(holdingPlayerId)).filter(player -> 
          player.getGameMode() == GameMode.CREATIVE || allPlayerItems(player)
          .map(PearlPearlPearl::fromItemStack)
          .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))// In Java 9: .flatMap(Optional::stream)
          .anyMatch(p -> p.uniqueId.equals(pearl.uniqueId))).isPresent();
      }

      private Stream<ItemStack> allPlayerItems(org.bukkit.entity.Player p){
        Stream<ItemStack> mainInv = Arrays.stream(p.getInventory().getContents());
        Stream<ItemStack> craftingInv = p.getOpenInventory().getType() != InventoryType.CHEST ? Arrays.stream(p.getOpenInventory().getTopInventory().getContents()) : Stream.empty();
        Stream<ItemStack> cursorInv = Stream.of(p.getItemOnCursor());
        return Stream.of(mainInv,craftingInv,cursorInv).flatMap(s -> s);
      }

      public Location getLocation(){
        return Bukkit.getPlayer(holdingPlayerId).getLocation();
      }

      public String getDisplay(){
        return NameAPI.getCurrentName(holdingPlayerId) + Optional.ofNullable(Bukkit.getPlayer(holdingPlayerId)).map(p -> " at " + showLocation(p.getLocation())).orElse(" somewhere");
      }

      public byte[] serialize(){
        return getBytesFromUUID(holdingPlayerId);
      }

      public static Player deserialize(byte[] b){
        return new Player(getUUIDFromBytes(b));
      }
    }

    public static final class Dropped extends PearlHolder{
      private final UUID droppedEntityId;

      public Dropped(UUID droppedEntity){
        super(PearlHolderType.DROPPED);
        this.droppedEntityId = droppedEntity;
      }

      public boolean verify(PearlPearlPearl pearl){
        Entity e = Bukkit.getEntity(droppedEntityId);
        if(e == null || !(e instanceof Item)){
          return false;
        }
        return PearlPearlPearl.fromItemStack(((Item) e).getItemStack()).filter(p -> pearl.uniqueId.equals(p.uniqueId)).isPresent();
      }

      public Location getLocation(){
        return Bukkit.getEntity(droppedEntityId).getLocation();
      }

      public String getDisplay(){
        return "a dropped item" + Optional.ofNullable(Bukkit.getEntity(droppedEntityId)).map(e -> " at " + showLocation(e.getLocation())).orElse(" somewhere");
      }

      public byte[] serialize(){
        return getBytesFromUUID(droppedEntityId);
      }

      public static Dropped deserialize(byte[] b){
        return new Dropped(getUUIDFromBytes(b));
      }
    }

    public static final class Block extends PearlHolder{
      private final Location holdingBlockLocation;

      public Block(Location holdingBlockLocation){
        super(PearlHolderType.BLOCK);
        this.holdingBlockLocation = holdingBlockLocation;
      }

      public boolean verify(PearlPearlPearl pearl){
        return Optional.ofNullable(holdingBlockLocation.getBlock().getState()).filter(st -> st instanceof InventoryHolder).map(st -> 
          allInventoryItems(((InventoryHolder) st).getInventory())
          .map(PearlPearlPearl::fromItemStack)
          .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))// In Java 9: .flatMap(Optional::stream)
          .anyMatch(p -> p.uniqueId.equals(pearl.uniqueId))).orElse(false);
      }

      private Stream<ItemStack> allInventoryItems(Inventory inv){
        Stream<ItemStack> mainInv = Arrays.stream(inv.getContents());
        Stream<ItemStack> cursorInv = inv.getViewers().stream().map(HumanEntity::getItemOnCursor);
        return Stream.of(mainInv,cursorInv).flatMap(s -> s);
      }

      public Location getLocation(){
        return holdingBlockLocation;
      }

      public String getDisplay(){
        return Optional.of(holdingBlockLocation.getBlock().getState()).filter(st -> st instanceof InventoryHolder).map(st -> "a " + ((InventoryHolder) st).getInventory().getName()).orElse("something") + " at " + showLocation(holdingBlockLocation);
      }

      public byte[] serialize(){
        ByteBuffer bb = ByteBuffer.wrap(new byte[16 + 12]);
        bb.putLong(holdingBlockLocation.getWorld().getUID().getMostSignificantBits());
        bb.putLong(holdingBlockLocation.getWorld().getUID().getLeastSignificantBits());
        bb.putInt(holdingBlockLocation.getBlockX());
        bb.putInt(holdingBlockLocation.getBlockY());
        bb.putInt(holdingBlockLocation.getBlockZ());
        return bb.array();
      }

      public static Block deserialize(byte[] b){
        ByteBuffer bb = ByteBuffer.wrap(b);
        return new Block(new Location(Bukkit.getWorld(new UUID(bb.getLong(),bb.getLong())), bb.getInt(), bb.getInt(), bb.getInt()));
      }
    }
  }

  public PearlPearlPearl(UUID uniqueId, UUID pearledId, UUID pearlerId, Instant timePearled, PearlHolder holder){
    this.uniqueId = uniqueId;
    this.pearledId = pearledId;
    this.pearlerId = pearlerId;
    this.timePearled = timePearled;
    this.holder = holder;
  }

  public static PearlPearlPearl makePearlingHappen(UUID pearled, Player pearler){
    String pearledName = NameAPI.getCurrentName(pearled);
    String pearlerName = NameAPI.getCurrentName(pearler.getUniqueId());
    pearler.sendMessage(ChatColor.GREEN + "You pearled " + ChatColor.AQUA + pearledName);
    Optional.ofNullable(Bukkit.getPlayer(pearled)).ifPresent(p -> p.sendMessage(ChatColor.GOLD + "You got pearled by " + ChatColor.AQUA + pearlerName));
    PearlPearlPearl pearl = new PearlPearlPearl(UUID.randomUUID(), pearled, pearler.getUniqueId(), Instant.now(), new PearlHolder.Player(pearler.getUniqueId()));
    ItemStack pearlItem = pearl.getItemRepr();
    // If the pearl couldn't fit in their inventory...
    if(pearler.getInventory().addItem(pearlItem).size() != 0){
      // ... drop it on the ground
      Item droppedPearl = pearler.getWorld().dropItem(pearler.getLocation().add(0,0.5,0),pearlItem);
      droppedPearl.setPickupDelay(10);
      pearl.setHolder(new PearlHolder.Dropped(droppedPearl.getUniqueId()));
    }else{
      pearl.setHolder(new PearlHolder.Player(pearler.getUniqueId()));
    }
    allPearls.put(pearl.uniqueId,pearl);
    dao.addNewPearl(pearl);
    return pearl;
  }

  public PearlHolder getHolder(){
    return holder;
  }

  public void setHolder(PearlHolder newHolder){
    Bukkit.getScheduler().runTask(PearlPearl.getInstance(), () -> Optional.ofNullable(Bukkit.getPlayer(pearledId)).ifPresent(p -> p.sendMessage(ChatColor.RESET + "Your pearl is now held by " + ChatColor.GOLD + newHolder.getDisplay())));
    holder = newHolder;
  }

  public static Optional<PearlPearlPearl> fromItemStack(ItemStack i){
    if(i == null || !i.hasItemMeta() || i.getItemMeta() == null || !i.getItemMeta().hasLore()){
      return Optional.empty();
    }
    try{
      return Optional.ofNullable(allPearls.get(UUID.fromString(i.getItemMeta().getLore().get(4).substring(pearlIdPrefix.length()))));
    } catch(Exception e){}
    // None of the lore lines looks like a pearl id
    return Optional.empty();
  }

  public ItemStack getItemRepr(){
    ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
    ISUtils.setName(pearl,NameAPI.getCurrentName(pearledId));
    ISUtils.addLore(pearl,ChatColor.DARK_PURPLE + "This is a pearl pearl, uh, pearl");
    for(String s : getDetail()){
      ISUtils.addLore(pearl,s);
    }
    ItemMeta im = pearl.getItemMeta();
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

  public void aggroVerify(){
    if(!holder.verify(this)){
      freePearl("pearl verification failed");
    }
  }

  public void freePearl(String reason){
    dao.snipePearl(uniqueId);
    allPearls.remove(uniqueId);
    Optional.ofNullable(Bukkit.getPlayer(pearledId)).ifPresent(p -> p.sendMessage(ChatColor.GREEN + "You were freed because " + reason));
  }

  public static Map<UUID,PearlPearlPearl> getAllPearls(){
    return allPearls;
  }

  public static String showLocation(Location l){
    return l.getWorld().getName() + " [" + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ() + "]";
  }

  public static void verifyAllPearls(){
    LinkedList<PearlPearlPearl> pearls = new LinkedList<PearlPearlPearl>(allPearls.values());
    pearls.forEach(PearlPearlPearl::aggroVerify);
  }
}
