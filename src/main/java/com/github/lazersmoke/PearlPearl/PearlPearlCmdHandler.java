package com.github.lazersmoke.PearlPearl;

import vg.civcraft.mc.civmodcore.command.CommandHandler;

public class PearlPearlCmdHandler extends CommandHandler{
  @Override
  public void registerCommands() {
    addCommands(new PearlPearlCmdAggro("Aggro"));
    addCommands(new PearlPearlCmdBehave("Behave"));
    addCommands(new PearlPearlCmdCheck("Check"));
    addCommands(new PearlPearlCmdSmite("Smite"));
    addCommands(new PearlPearlCmdUnleash("Unleash"));
    addCommands(new PearlPearlCmdVerify("Verify"));
  }
}
