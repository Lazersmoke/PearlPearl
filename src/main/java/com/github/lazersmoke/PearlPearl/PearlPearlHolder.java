package com.github.lazersmoke.PearlPearl;

import org.bukkit.inventory.ItemStack;
import java.util.Arrays;
import java.nio.ByteBuffer;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryType;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import java.util.Optional;
import org.bukkit.GameMode;
import vg.civcraft.mc.namelayer.NameAPI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import java.util.function.Function;
import org.bukkit.entity.HumanEntity;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Hopper;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.BlockState;

public abstract class PearlPearlHolder{
  public abstract Optional<Location> verify(PearlPearlPearl pearl);
  public abstract String getDisplay();
  public abstract byte[] serialize();
  public final PearlPearlHolderType type;

  // Order is important; it corresponds to database IDs
  public enum PearlPearlHolderType{
    PLAYER(PearlPearlHolder.Player::deserialize),
    DROPPED(PearlPearlHolder.Dropped::deserialize),
    BLOCK(PearlPearlHolder.Block::deserialize);

    public final Function<byte[],PearlPearlHolder> deserialize;

    private PearlPearlHolderType(Function<byte[],PearlPearlHolder> f){
      this.deserialize = f;
    }
  }

  private PearlPearlHolder(PearlPearlHolderType t){
    this.type = t;
  }

  public static PearlPearlHolder deserializePearlHolder(int type, byte[] data){
    return PearlPearlHolderType.values()[type].deserialize.apply(data);
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

  private static String showLocation(Location l){
    return l.getWorld().getName() + " [" + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ() + "]";
  }

  
  public static Optional<PearlPearlHolder> fromInventory(InventoryHolder holder){
    if(holder instanceof Chest || holder instanceof Dispenser || holder instanceof Dropper || holder instanceof Hopper){
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

  public static final class Player extends PearlPearlHolder{
    private final UUID holdingPlayerId;

    public Player(UUID holdingPlayer){
      super(PearlPearlHolderType.PLAYER);
      this.holdingPlayerId = holdingPlayer;
    }

    // Verifies that the holding player is online, and that they are either
    // in creative, or holding the pearl in their inventory, crafting grid, or cursor
    public Optional<Location> verify(PearlPearlPearl pearl){
      return Optional.ofNullable(Bukkit.getPlayer(holdingPlayerId)).filter(player -> 
        player.getGameMode() == GameMode.CREATIVE || allPlayerItems(player)
        .map(PearlPearlPearl::fromItemStack)
        .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))// In Java 9: .flatMap(Optional::stream)
        .anyMatch(p -> p.uniqueId.equals(pearl.uniqueId))).map(org.bukkit.entity.Player::getLocation);
    }

    private Stream<ItemStack> allPlayerItems(org.bukkit.entity.Player p){
      Stream<ItemStack> mainInv = Arrays.stream(p.getInventory().getContents());
      Stream<ItemStack> craftingInv = Arrays.stream(p.getOpenInventory().getTopInventory().getContents());
      Stream<ItemStack> cursorInv = Stream.of(p.getItemOnCursor());
      return Stream.of(mainInv,craftingInv,cursorInv).flatMap(s -> s);
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

  public static final class Dropped extends PearlPearlHolder{
    private final UUID droppedEntityId;

    public Dropped(UUID droppedEntity){
      super(PearlPearlHolderType.DROPPED);
      this.droppedEntityId = droppedEntity;
    }

    public Optional<Location> verify(PearlPearlPearl pearl){
      Entity e = Bukkit.getEntity(droppedEntityId);
      if(e == null || !(e instanceof Item)){
        return Optional.empty();
      }
      return PearlPearlPearl.fromItemStack(((Item) e).getItemStack()).filter(p -> pearl.uniqueId.equals(p.uniqueId)).map(x -> e.getLocation());
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

  public static final class Block extends PearlPearlHolder{
    private final Location holdingBlockLocation;

    public Block(Location holdingBlockLocation){
      super(PearlPearlHolderType.BLOCK);
      this.holdingBlockLocation = holdingBlockLocation;
    }

    public Optional<Location> verify(PearlPearlPearl pearl){
      return Optional.ofNullable(holdingBlockLocation.getBlock().getState()).filter(st -> st instanceof InventoryHolder).filter(st -> 
        allInventoryItems(((InventoryHolder) st).getInventory())
        .map(PearlPearlPearl::fromItemStack)
        .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))// In Java 9: .flatMap(Optional::stream)
        .anyMatch(p -> p.uniqueId.equals(pearl.uniqueId))).map(x -> holdingBlockLocation);
    }

    private Stream<ItemStack> allInventoryItems(Inventory inv){
      Stream<ItemStack> mainInv = Arrays.stream(inv.getContents());
      //Stream<ItemStack> cursorInv = inv.getViewers().stream().map(HumanEntity::getItemOnCursor);
      return Stream.of(mainInv/*,cursorInv*/).flatMap(s -> s);
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
