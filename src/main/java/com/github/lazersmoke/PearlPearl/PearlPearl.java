package com.github.lazersmoke.PearlPearl;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
import java.util.Optional;
import java.util.UUID;
import net.minelink.ctplus.CombatTagPlus;
import vg.civcraft.mc.civmodcore.ACivMod;

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
    // Load pearls
    PearlPearlPearl.loadPearls();
    // Enable aggro verifier
    PearlPearlCmdAggro.enableAggro();
    getServer().getPluginManager().registerEvents(listener, this);
    // Register commands
    PearlPearlCmdHandler handle = new PearlPearlCmdHandler();
    setCommandHandler(handle);
    handle.registerCommands();
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
}
