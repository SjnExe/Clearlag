package me.minebuilders.clearlag.tasks;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;
import me.minebuilders.clearlag.Clearlag;
import me.minebuilders.clearlag.annotations.AutoWire;
import me.minebuilders.clearlag.annotations.ConfigPath;
import me.minebuilders.clearlag.annotations.ConfigValue;
import me.minebuilders.clearlag.config.ConfigHandler;
import me.minebuilders.clearlag.managers.EntityManager;
import me.minebuilders.clearlag.modules.ClearModule;
import me.minebuilders.clearlag.modules.ClearlagModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;

/** Halt task. */
@ConfigPath(path = "halt-command")
public class HaltTask extends ClearlagModule implements Listener {

  @ConfigValue private boolean removeEntities;

  @ConfigValue private boolean disableNaturalEntitySpawning;

  @AutoWire private EntityManager entityManager;

  @AutoWire private ConfigHandler config;

  private HashMap<UUID, Integer[]> valuelist;

  @Override
  public void setEnabled() {
    super.setEnabled();

    valuelist = new HashMap<>(Bukkit.getWorlds().size());

    for (World w : Bukkit.getWorlds()) {

      if (removeEntities) {

        entityManager.removeEntities(
            new ClearModule() {

              @Override
              public boolean isRemovable(Entity e) {
                return e instanceof Item
                    || e instanceof TNTPrimed
                    || e instanceof ExperienceOrb
                    || e instanceof FallingBlock
                    || e instanceof Monster;
              }

              @Override
              public boolean isWorldEnabled(World w) {
                return true;
              }
            });
      }

      if (disableNaturalEntitySpawning) {

        Integer[] values = new Integer[6];

        values[0] = w.getSpawnLimit(SpawnCategory.AMBIENT);
        w.setSpawnLimit(SpawnCategory.AMBIENT, 0);
        values[1] = w.getSpawnLimit(SpawnCategory.ANIMAL);
        w.setSpawnLimit(SpawnCategory.ANIMAL, 0);
        values[2] = w.getSpawnLimit(SpawnCategory.MONSTER);
        w.setSpawnLimit(SpawnCategory.MONSTER, 0);
        values[3] = (int) w.getTicksPerSpawns(SpawnCategory.ANIMAL);
        w.setTicksPerSpawns(SpawnCategory.ANIMAL, 0);
        values[4] = (int) w.getTicksPerSpawns(SpawnCategory.MONSTER);
        w.setTicksPerSpawns(SpawnCategory.MONSTER, 0);
        values[5] = w.getSpawnLimit(SpawnCategory.WATER_ANIMAL);
        w.setSpawnLimit(SpawnCategory.WATER_ANIMAL, 0);

        valuelist.put(w.getUID(), values);
      }
    }

    PluginManager pm = Clearlag.getInstance().getServer().getPluginManager();

    Method[] methods = this.getClass().getDeclaredMethods();

    for (final Method method : methods) {

      final EventHandler he = method.getAnnotation(EventHandler.class);

      if (he != null
          && config
              .getConfig()
              .getBoolean("halt-command.halted." + config.javaToConfigValue(method.getName()))) {

        Class<?>[] params = method.getParameterTypes();

        if (!Event.class.isAssignableFrom(params[0]) || params.length != 1) {
          continue;
        }

        final Class<? extends Event> eventClass = params[0].asSubclass(Event.class);

        method.setAccessible(true);

        EventExecutor executor =
            new EventExecutor() {

              public void execute(Listener listener, Event event) throws EventException {

                try {

                  if (!eventClass.isAssignableFrom(event.getClass())) {
                    return;
                  }

                  method.invoke(listener, event);

                } catch (Exception ex) {
                  throw new EventException(ex.getCause());
                }
              }
            };

        pm.registerEvent(
            eventClass,
            this,
            he.priority(),
            executor,
            Clearlag.getInstance(),
            he.ignoreCancelled());
      }
    }
  }

  @Override
  public void setDisabled() {
    super.setDisabled();

    if (!valuelist.isEmpty()) {

      for (UUID uuid : valuelist.keySet()) {
        World w = Bukkit.getWorld(uuid);
        if (w != null) {
          Integer[] values = valuelist.get(uuid);
          w.setSpawnLimit(SpawnCategory.AMBIENT, values[0]);
          w.setSpawnLimit(SpawnCategory.ANIMAL, values[1]);
          w.setSpawnLimit(SpawnCategory.MONSTER, values[2]);
          w.setTicksPerSpawns(SpawnCategory.ANIMAL, values[3]);
          w.setTicksPerSpawns(SpawnCategory.MONSTER, values[4]);
          w.setSpawnLimit(SpawnCategory.WATER_ANIMAL, values[5]);
        }
      }
    }

    valuelist = null;

    HandlerList.unregisterAll(this);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void fire(BlockIgniteEvent e) {
    e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void fireBurn(BlockBurnEvent e) {
    e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void explosion(EntityExplodeEvent e) {
    e.setCancelled(true);
    e.blockList().clear();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void decay(LeavesDecayEvent e) {
    e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void blockForm(BlockFormEvent e) {
    e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void blockSpread(BlockSpreadEvent e) {
    e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void blockFade(BlockFadeEvent e) {
    e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void blockNaturalChange(BlockFromToEvent e) {
    e.setCancelled(true);
  }
}
