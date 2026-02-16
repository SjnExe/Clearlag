package me.minebuilders.clearlag.commands;

import me.minebuilders.clearlag.Clearlag;
import me.minebuilders.clearlag.Util;
import me.minebuilders.clearlag.modules.CommandModule;
import org.bukkit.command.CommandSender;

/** Debug command. */
public class DebugCmd extends CommandModule {

  /** Debug command constructor. */
  public DebugCmd() {
    this.name = "debug";
    this.desc = "Toggles debug mode on/off";
    this.usage = "/lagg debug [on/off]";
    this.argLength = 0;
  }

  @Override
  protected void run(CommandSender sender, String[] args) {
    boolean newState;

    if (args.length > 0) {
      if (args[0].equalsIgnoreCase("on")) {
        newState = true;
      } else if (args[0].equalsIgnoreCase("off")) {
        newState = false;
      } else {
        sender.sendMessage(Util.color("&cUsage: " + usage));
        return;
      }
    } else {
      // Toggle if no argument
      newState = !Util.isDebugMode();
    }

    setDebug(sender, newState);
  }

  private void setDebug(CommandSender sender, boolean enabled) {
    Util.setDebugMode(enabled);

    // Save to config
    Clearlag.getInstance().getConfig().set("settings.debug-mode", enabled);
    Clearlag.getInstance().saveConfig();

    if (enabled) {
      sender.sendMessage(Util.color("&a[ClearLag] &7Debug mode has been &aenabled&7."));
    } else {
      sender.sendMessage(Util.color("&a[ClearLag] &7Debug mode has been &cdisabled&7."));
    }
  }
}
