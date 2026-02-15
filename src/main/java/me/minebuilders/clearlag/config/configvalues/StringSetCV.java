package me.minebuilders.clearlag.config.configvalues;

import java.util.HashSet;
import java.util.Set;
import me.minebuilders.clearlag.annotations.AutoWire;
import me.minebuilders.clearlag.config.ConfigHandler;

/**
 * @author bob7l
 */
public class StringSetCV implements ConfigData<Set<String>> {

  @AutoWire private ConfigHandler configHandler;

  @Override
  public Set<String> getValue(String path) {

    if (configHandler.getConfig().get(path) == null) return new HashSet<>();

    return new HashSet<>(configHandler.getConfig().getStringList(path));
  }
}
