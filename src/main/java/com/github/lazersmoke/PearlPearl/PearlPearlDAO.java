package com.github.lazersmoke.PearlPearl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import org.bukkit.Location;

import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;

public class PearlPearlDAO {

  private static final String ADD_PEARL = "insert into pearlPearlPearls (pearlIdMost,pearlIdLeast,pearledIdMost,pearledIdLeast,pearlerIdMost,pearlerIdLeast,timePearled,pearlHolderType,pearlHolder) values (?,?,?,?,?,?,?,?,?);";
  private static final String SNIPE_PEARL = "delete from pearlPearlPearls where pearlIdMost=? and pearlIdLeast=?;";
  private static final String ALL_PEARLS = "select * from pearlPearlPearls;";
  private final ManagedDatasource db;

  public PearlPearlDAO(PearlPearl plugin, String user, String pass, String host, int port, String database, int poolSize, long connectionTimeout, long idleTimeout, long maxLifetime) {
    ManagedDatasource theDB = null;
    try {
      theDB = new ManagedDatasource(plugin, user, pass, host, port, database, poolSize, connectionTimeout, idleTimeout, maxLifetime);
      theDB.getConnection().close(); // Test connection
      registerMigrations(theDB);
      if(!theDB.updateDatabase()) {
        plugin.warning("Failed to update database, stopping PearlPearl");
        plugin.getServer().getPluginManager().disablePlugin(plugin);
      }
    } catch(Exception e) {
      plugin.warning("Could not connect to database, stopping PearlPearl", e);
      plugin.getServer().getPluginManager().disablePlugin(plugin);
    } finally {db = theDB;}
  }

  private void registerMigrations(ManagedDatasource theDB) {
    theDB.registerMigration(1, true,
        "create table if not exists pearlPearlPearls ("
        + "pearlIdMost bigint not null,"
        + "pearlIdLeast bigint not null,"
        + "pearledIdMost bigint not null,"
        + "pearledIdLeast bigint not null,"
        + "pearlerIdMost bigint not null,"
        + "pearlerIdLeast bigint not null,"
        + "timePearled timestamp not null,"
        + "pearlHolderType int not null,"
        + "pearlHolder varbinary(128) not null,"
        + "primary key(pearlIdMost,pearlIdLeast));");
  }

  private void writeUUID(PreparedStatement ps, int i, UUID u) throws SQLException{
    ps.setLong(i + 0,u.getMostSignificantBits());
    ps.setLong(i + 1,u.getLeastSignificantBits());
  }

  private UUID readUUID(ResultSet rs, int i) throws SQLException{
    return new UUID(rs.getLong(i),rs.getLong(i + 1));
  }

  private void writeLocation(PreparedStatement ps, int i, Location l) throws SQLException{
    ps.setInt(i + 0,l.getBlockX());
    ps.setInt(i + 1,l.getBlockY());
    ps.setInt(i + 2,l.getBlockZ());
  }

  /*private Location readLocation(ResultSet rs, int i) throws SQLException{
    return new Location(rs.getInt(i + 0), rs.getInt(i + 1), rs.getInt(i + 2));
  }*/

  public void addNewPearl(PearlPearlPearl pearl){
    try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(ADD_PEARL)) {
      writeUUID(ps,1,pearl.uniqueId);
      writeUUID(ps,3,pearl.pearledId);
      writeUUID(ps,5,pearl.pearlerId);
      ps.setTimestamp(7, Timestamp.from(pearl.timePearled));
      ps.setInt(8, pearl.getHolder().type.magic);
      ps.setBytes(9, pearl.getHolder().serialize());
      ps.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void snipePearl(UUID u){
    try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(SNIPE_PEARL)) {
      writeUUID(ps,1,u);
      ps.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void loadPearls(Map<UUID,PearlPearlPearl> pearl){
    try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(ALL_PEARLS)) {
      ResultSet rs = ps.executeQuery();
      while(rs.next()){
        pearl.put(readUUID(rs,1),new PearlPearlPearl(readUUID(rs,1),readUUID(rs,3),readUUID(rs,5),rs.getTimestamp(7).toInstant(), PearlPearlPearl.deserializePearlHolder(rs.getInt(8),rs.getBytes(9))));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
