package com.github.lazersmoke.PearlPearl;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import net.minelink.ctplus.CombatTagPlus;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import isaac.bastion.Bastion;
import isaac.bastion.BastionBlock;
import isaac.bastion.manager.BastionBlockManager;
import java.util.LinkedList;

public final class PearlPearl extends ACivMod {
  private static PearlPearl instance;
  private static PearlPearlListener listener;
  private static PearlPearlConfig config;
  private static PearlPearlDAO dao;

  @Override
  public void onEnable(){
    instance = this;
    listener = new PearlPearlListener();
    config = new PearlPearlConfig();
    dao = setupDatabase();
    saveDefaultConfig();
    config.reloadConfig(this.getConfig());
    registerNameLayerPerm();
    PearlPearlPearl.loadPearls();
    // Enable aggro verifier
    PearlPearlCmdAggro.enableAggro();
    PearlPearlPearl.startDecayTask();
    PearlPearlBehavior.startTasks();
    getServer().getPluginManager().registerEvents(listener, this);
    // Register commands
    PearlPearlCmdHandler handle = new PearlPearlCmdHandler();
    setCommandHandler(handle);
    handle.registerCommands();
  }

  public static final String bastionPermissionName = "EXILE_ENTER_BASTION";
  // By default *everyone* can bypass bastion rules
  private void registerNameLayerPerm(){
    LinkedList<PlayerType> defaultPerms = new LinkedList<PlayerType>();
    defaultPerms.add(PlayerType.NOT_BLACKLISTED);
    defaultPerms.add(PlayerType.MEMBERS);
    defaultPerms.add(PlayerType.MODS);
    defaultPerms.add(PlayerType.ADMINS);
    defaultPerms.add(PlayerType.OWNER);
    PermissionType.registerPermission(bastionPermissionName, defaultPerms);
  }

  private PearlPearlDAO setupDatabase() {
    ConfigurationSection config = getConfig().getConfigurationSection("mysql");
    String host = config.getString("host");
    int port = config.getInt("port");
    String user = config.getString("user");
    String pass = config.getString("password");
    String dbname = config.getString("database");
    int poolsize = config.getInt("poolsize");
    long connectionTimeout = config.getLong("connectionTimeout");
    long idleTimeout = config.getLong("idleTimeout");
    long maxLifetime = config.getLong("maxLifetime");
    return new PearlPearlDAO(this, user, pass, host, port, dbname, poolsize, connectionTimeout, idleTimeout, maxLifetime);
  }

  public String getPluginName(){
    return "PearlPearl";
  }

  public static PearlPearl getInstance(){
    return instance;
  }

  public static PearlPearlConfig getConfiguration(){
    return config;
  }

  public static PearlPearlDAO getDAO(){
    return dao;
  }

  public static boolean isTagged(UUID u){
    if(Bukkit.getPluginManager().getPlugin("CombatTagPlus") != null){
      return ((CombatTagPlus) Bukkit.getPluginManager().getPlugin("CombatTagPlus")).getTagManager().isTagged(u);
    }
    return false;
  }

  public Set<Location> getBlockingBastions(Player player) {
    Set<Location> out = new HashSet<Location>();
    if(Bukkit.getPluginManager().getPlugin("Bastion") == null){
      return out;
    }
    try {
      final BastionBlockManager manager = Bastion.getBastionManager();
      Set<BastionBlock> bastions = manager.getBlockingBastions(player.getLocation());
      PermissionType perm = PermissionType.getPermission(bastionPermissionName);
      for (BastionBlock bastion : bastions) {
        if (!bastion.permAccess(player, perm)) {
          out.add(bastion.getLocation());
        }
      }
    }catch(Exception ex){
      ex.printStackTrace();
    }
    return out;
  }
}
