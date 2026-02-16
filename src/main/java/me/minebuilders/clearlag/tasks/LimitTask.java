package me.minebuilders.clearlag.tasks;

import java.util.ArrayList;
import java.util.List;
import me.minebuilders.clearlag.annotations.AutoWire;
import me.minebuilders.clearlag.annotations.ConfigModule;
import me.minebuilders.clearlag.annotations.ConfigPath;
import me.minebuilders.clearlag.annotations.ConfigValue;
import me.minebuilders.clearlag.config.ConfigHandler;
import me.minebuilders.clearlag.modules.BroadcastHandler;
import me.minebuilders.clearlag.modules.TaskModule;
import me.minebuilders.clearlag.removetype.LimitClear;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

@ConfigPath(path = "limit")
public class LimitTask extends TaskModule {

  @ConfigValue private int max;

  @ConfigModule private final LimitClear limitClear = new LimitClear();

  @AutoWire private ConfigHandler configHandler;

  @AutoWire private BroadcastHandler broadcastHandler;

  public void run() {
    List<Entity> ents = new ArrayList<>();

    for (World w : Bukkit.getWorlds()) {
      ents.addAll(limitClear.getRemovables(w.getEntities(), w));
    }

    if (ents.size() <= max) return;

    for (Entity entity : ents) {
      entity.remove();
    }

    if (configHandler.getConfig().getBoolean("limit.broadcast-removal")) {
      broadcastHandler.broadcast(
          configHandler
              .getConfig()
              .getString("limit.broadcast-message")
              .replace("+RemoveAmount", "" + ents.size()));
    }
  }

  @Override
  public int getInterval() {
    return configHandler.getConfig().getInt("limit.check-interval") * 20;
  }
}
