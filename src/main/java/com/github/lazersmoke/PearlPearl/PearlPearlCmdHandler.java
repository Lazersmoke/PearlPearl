package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.CommandHandler;

public class PearlPearlCmdHandler extends CommandHandler{
  @Override
  public void registerCommands() {
    addCommands(new PearlPearlCmdUnleash("Unleash"));
    addCommands(new PearlPearlCmdVerify("Verify"));
    addCommands(new PearlPearlCmdSmite("Smite"));
  }
}
