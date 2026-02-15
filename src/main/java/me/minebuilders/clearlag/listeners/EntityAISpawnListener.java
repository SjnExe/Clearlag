package me.minebuilders.clearlag.listeners;

import java.util.EnumMap;
import java.util.Map;
import me.minebuilders.clearlag.Util;
import me.minebuilders.clearlag.annotations.AutoWire;
import me.minebuilders.clearlag.annotations.ConfigPath;
import me.minebuilders.clearlag.config.ConfigHandler;
import me.minebuilders.clearlag.modules.EventModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;

@ConfigPath(path = "mob-range")
public class EntityAISpawnListener extends EventModule {

  private final Map<EntityType, Double> mobRanges = new EnumMap<>(EntityType.class);

  @AutoWire private ConfigHandler configHandler;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    setEntityRange(event.getEntity());
  }

  @Override
  public void setEnabled() {
    try {
      Configuration config = configHandler.getConfig();

      if (config.isConfigurationSection("mob-range")) {
        for (String s : config.getConfigurationSection("mob-range").getKeys(false)) {
          if (s.equalsIgnoreCase("enabled")) continue;

          EntityType type = Util.getEntityTypeFromString(s);

          if (type != null) {
            mobRanges.put(type, config.getDouble("mob-range." + s));
          }
        }
      }

      for (World w : Bukkit.getWorlds()) {
        for (Entity e : w.getEntities()) {
          if (e instanceof LivingEntity) {
            setEntityRange(e);
          }
        }
      }

      super.setEnabled();

    } catch (Exception e) {
      Util.warning("Failed to initialize 'mob-range' controller: " + e.getMessage());
    }
  }

  private void setEntityRange(Entity e) {
    if (!(e instanceof LivingEntity)) return;

    Double value = mobRanges.get(e.getType());

    if (value != null) {
      AttributeInstance attribute = ((LivingEntity) e).getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
      if (attribute != null) {
        attribute.setBaseValue(value);
      }
    }
  }
}
