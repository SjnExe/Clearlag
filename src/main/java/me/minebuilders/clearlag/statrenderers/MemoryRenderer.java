package me.minebuilders.clearlag.statrenderers;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import me.minebuilders.clearlag.RAMUtil;
import me.minebuilders.clearlag.adapters.VersionAdapter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

/**
 * Memory renderer.
 *
 * @author bob7l
 */
public class MemoryRenderer extends StatRenderer {

  private final byte[] memoryPalettes =
      new byte[] {
        MapPalette.RED,
        MapPalette.LIGHT_GREEN,
        MapPalette.DARK_GREEN,
        MapPalette.LIGHT_BROWN,
        MapPalette.BROWN
      };

  private final Deque<MemorySampleColumn> memorySamples = new ArrayDeque<>();

  private final List<MemoryPoolMXBean> memoryBeans = new ArrayList<>();

  private final List<GarbageCollectorMXBean> gcBeans =
      ManagementFactory.getGarbageCollectorMXBeans();

  private long gcLastPauseTime = getTotalGcCompleteTime();

  public MemoryRenderer(
      Player observer,
      int sampleTicks,
      ItemStack mapItemStack,
      VersionAdapter versionAdapter,
      MapView mapView) {
    super(observer, sampleTicks, mapItemStack, versionAdapter, mapView);

    for (MemoryPoolMXBean memoryBean : ManagementFactory.getMemoryPoolMXBeans()) {

      if (memoryBean.getType() == MemoryType.HEAP) {
        memoryBeans.add(memoryBean);
      }
    }

    final String[] colorTypes =
        new String[] {"Dark Red", "Light Green", "Dark Green", "Light Brown", "Brown"};

    int colorIndex = 0;

    for (int i = memoryBeans.size() - 1; i >= 0; --i) {

      final MemoryPoolMXBean memoryBean = memoryBeans.get(i);

      final int color = colorIndex >= colorTypes.length ? colorIndex = 0 : colorIndex++;

      observer.sendMessage(
          ChatColor.RED
              + " - "
              + ChatColor.DARK_GREEN
              + memoryBean.getName()
              + ChatColor.DARK_GRAY
              + ": "
              + ChatColor.GREEN
              + colorTypes[color]);
    }
  }

  private long getTotalGcCompleteTime() {

    long totalGarbageCollections = 0;

    for (GarbageCollectorMXBean gc : gcBeans) {

      long count = gc.getCollectionTime();

      if (count >= 0) {
        totalGarbageCollections += count;
      }
    }

    return totalGarbageCollections;
  }

  @Override
  public void tick() {

    final MemorySampleColumn column = new MemorySampleColumn();

    for (MemoryPoolMXBean memoryBean : memoryBeans) {
      column.addMemorySample(new MemorySample(RAMUtil.toMB(memoryBean.getUsage().getUsed())));
    }

    final long totalGcTime = getTotalGcCompleteTime();

    final long gcTime = (totalGcTime - gcLastPauseTime);

    if (gcTime > 0) {
      column.garbageCollectionTime = (int) gcTime;
    }

    this.gcLastPauseTime = totalGcTime;

    memorySamples.addLast(column);

    while (memorySamples.size() > width) {
      memorySamples.removeFirst();
    }
  }

  @Override
  public void draw(MapView mapView, MapCanvas mapCanvas, Player player) {

    final int maxHeapSize = RAMUtil.toMB(Runtime.getRuntime().totalMemory());

    int x = 0;

    for (MemorySampleColumn memorySampleColumn : memorySamples) {

      int colorIndex = 0;

      int baseY = 0;

      int drawTo = height;

      for (int i = memorySampleColumn.memorySamples.size() - 1; i >= 0; --i) {

        final MemorySample sample = memorySampleColumn.memorySamples.get(i);

        final byte color =
            memoryPalettes[colorIndex >= memoryPalettes.length ? colorIndex = 0 : colorIndex++];

        final int y = (((sample.usageInMb + baseY) * height) / maxHeapSize);

        for (int j = height - y; j < drawTo; ++j) {
          mapCanvas.setPixel(x, j, color);
        }

        baseY = sample.usageInMb + baseY;
        drawTo = height - y;
      }

      ++x;
    }

    x = 0;

    for (MemorySampleColumn memorySampleColumn : memorySamples) {

      if (memorySampleColumn.didGarbageCollection()) {
        mapCanvas.drawText(
            x,
            height - 8,
            MinecraftFont.Font,
            "§32;" + memorySampleColumn.garbageCollectionTime + "ms");
      }

      ++x;
    }

    for (double ypos : new double[] {0.0, 0.25, 0.50, 0.75}) {

      final int y = (int) (((maxHeapSize * ypos) * height) / maxHeapSize);

      mapCanvas.drawText(
          5,
          y,
          MinecraftFont.Font,
          "§48;" + (Math.round(((maxHeapSize * (1 - ypos)) / 1000.0) * 10.0) / 10.0) + "GB");

      for (int i = 0; i < 5; ++i) {
        mapCanvas.setPixel(i, y, MapPalette.PALE_BLUE);
      }
    }

    final double time = (Math.round((width * sampleTicks) / 20.0 * 10.0) / 10.0);

    mapCanvas.drawText(35, 0, MinecraftFont.Font, time + " second sample");
  }

  private static class MemorySampleColumn {

    private final List<MemorySample> memorySamples = new ArrayList<>(5);

    private int garbageCollectionTime = -1;

    public void addMemorySample(MemorySample memorySample) {
      memorySamples.add(memorySample);
    }

    public boolean didGarbageCollection() {
      return garbageCollectionTime > 0;
    }
  }

  private static class MemorySample {

    private final int usageInMb;

    public MemorySample(int usageInMb) {
      this.usageInMb = usageInMb;
    }
  }
}
