package me.minebuilders.clearlag.commands;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import me.minebuilders.clearlag.Callback;
import me.minebuilders.clearlag.Clearlag;
import me.minebuilders.clearlag.RAMUtil;
import me.minebuilders.clearlag.Util;
import me.minebuilders.clearlag.exceptions.WrongCommandArgumentException;
import me.minebuilders.clearlag.language.LanguageValue;
import me.minebuilders.clearlag.language.messages.MessageTree;
import me.minebuilders.clearlag.modules.CommandModule;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Sample memory command.
 *
 * @author bob7l
 */
public class SampleMemoryCmd extends CommandModule {

  @LanguageValue(key = "command.samplememory.")
  private MessageTree lang;

  public SampleMemoryCmd() {
    argLength = 1;
  }

  @Override
  protected void run(CommandSender sender, String[] args) throws WrongCommandArgumentException {

    if (!Util.isInteger(args[0])) {
      throw new WrongCommandArgumentException(lang.getMessage("invalidinteger"), args[0]);
    }

    lang.sendMessage("begin", sender, args[0]);

    // Todo: Replace this pointless code with new Java 8 features...
    new MemorySamlier(
            Integer.parseInt(args[0]) * 20,
            (s) -> {
              java.util.LongSummaryStatistics memoryStats =
                  s.memoryList.stream()
                      .filter(m -> m > 0)
                      .mapToLong(Long::longValue)
                      .summaryStatistics();

              // If count is 0, defaults are 0, min/max are MAX_VALUE/MIN_VALUE. Handle gracefully.
              long count = memoryStats.getCount();
              long highestMemory = count > 0 ? memoryStats.getMax() : 0;
              // long lowestMemory = count > 0 ? memoryStats.getMin() : 0; // Unused
              long averageMemory = count > 0 ? (long) memoryStats.getAverage() : 0;

              lang.sendMessage("header", sender);

              lang.sendMessage(
                  "memory",
                  sender,
                  Util.getChatColorByNumberLength((int) highestMemory, 100, 200)
                      + Long.toString(highestMemory),
                  Util.getChatColorByNumberLength((int) averageMemory, 30, 100)
                      + Long.toString(averageMemory));

              if (s.gcCollectionTickstamps.size() > 1) {

                java.util.LongSummaryStatistics gcStats =
                    s.gcCollectionTickstamps.stream()
                        .mapToLong(sample -> sample.data)
                        .summaryStatistics();

                long highestGc = gcStats.getMax();
                long lowestGc = gcStats.getMin();
                long averageGc = (long) gcStats.getAverage();

                int totalBetweenGcTime = 0;
                Sample lastSample = null;

                for (Sample sample : s.gcCollectionTickstamps) {
                  if (lastSample != null) {
                    totalBetweenGcTime += (sample.timeStamp - lastSample.timeStamp);
                  }
                  lastSample = sample;
                }

                int averageBetweenTime =
                    (totalBetweenGcTime / (s.gcCollectionTickstamps.size() - 1));

                lang.sendMessage(
                    "gc",
                    sender,
                    s.gcCollectionTickstamps.size(),
                    Util.getChatColorByNumberLength((int) highestGc, 100, 200) + "" + highestGc,
                    Util.getChatColorByNumberLength((int) lowestGc, 100, 200) + "" + lowestGc,
                    Util.getChatColorByNumberLength((int) averageGc, 100, 200) + "" + averageGc,
                    averageBetweenTime);

              } else {
                lang.sendMessage("notenoughtime", sender);
              }
            })
        .runTaskTimer(Clearlag.getInstance(), 1L, 1L);
  }

  private static class MemorySamlier extends BukkitRunnable {

    private int currentTick = 0;

    private final int runTicks;

    private long gcCollections = getTotalGcEvents();

    private long gcLastPauseTime = getTotalGcCompleteTime();

    private long lastMemoryUsed = RAMUtil.getUsedMemory();

    private final List<Sample> gcCollectionTickstamps = new ArrayList<>();

    private final List<Long> memoryList;

    private final Callback<MemorySamlier> callback;

    public MemorySamlier(int runTicks, Callback<MemorySamlier> callback) {
      this.runTicks = runTicks;
      this.callback = callback;

      this.memoryList = new ArrayList<>(runTicks);
    }

    @Override
    public void run() {

      long currentGcCollections = getTotalGcEvents();

      if (currentGcCollections != gcCollections) {

        long currentTotalGCPauseTime = getTotalGcCompleteTime();

        gcCollectionTickstamps.add(
            new Sample(currentTick, currentTotalGCPauseTime - gcLastPauseTime));

        gcCollections = currentGcCollections;
        gcLastPauseTime = currentTotalGCPauseTime;
      }

      long memoryUsedDif = (RAMUtil.getUsedMemory() - lastMemoryUsed);

      memoryList.add(memoryUsedDif);

      lastMemoryUsed = RAMUtil.getUsedMemory();

      if (++currentTick > runTicks) {
        cancel();
        callback.call(this);
      }
    }

    private long getTotalGcEvents() {

      long totalGarbageCollections = 0;

      for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {

        long count = gc.getCollectionCount();

        if (count >= 0) {
          totalGarbageCollections += count;
        }
      }

      return totalGarbageCollections;
    }

    private long getTotalGcCompleteTime() {

      long totalGarbageCollections = 0;

      for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {

        long count = gc.getCollectionTime();

        if (count >= 0) {
          totalGarbageCollections += count;
        }
      }

      return totalGarbageCollections;
    }
  }

  private static class Sample {

    private final int timeStamp;

    private final long data;

    public Sample(int timeStamp, long data) {
      this.timeStamp = timeStamp;
      this.data = data;
    }
  }
}
