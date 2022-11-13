package ch.dfx.transactionserver.scheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Not used at the moment ...
 */
public class TimerProvider {
  private static final Logger LOGGER = LogManager.getLogger(TimerProvider.class);

  private static TimerProvider instance = null;

  private final Map<UUID, TimerData> timerMap;

  /**
   *
   */
  public static TimerProvider getInstance() {
    if (null == instance) {
      instance = new TimerProvider();
    }

    return instance;
  }

  /**
   *
   */
  public synchronized @Nonnull UUID add(
      @Nonnull TimerProviderTask task,
      long initialDelay,
      long period) {
    LOGGER.trace("add()");

    UUID uuid = UUID.randomUUID();

    TimerData timerData = new TimerData();

    timerData.timer = new Timer();
    timerData.task = task;
    timerData.initialDelay = initialDelay * 1000;
    timerData.period = period * 1000;

    timerMap.put(uuid, timerData);

    LOGGER.debug("Add Task: " + task.getClass().getSimpleName());
    LOGGER.debug("Add UUID: " + uuid);

    return uuid;
  }

  /**
   *
   */
  public synchronized void remove(@Nonnull UUID uuid) {
    LOGGER.trace("remove(): UUID = '" + uuid);

    TimerData timerData = timerMap.remove(uuid);

    if (null != timerData) {
      Timer timer = timerData.timer;
      timer.cancel();

      int timeout = 0;

      while (timerData.task.isProcessing()
          && 5 > timeout++) {
        try {
          Thread.sleep(timeout * 1000);
        } catch (Exception e) {
          // Intentionally left blank ...
        }
      }

      LOGGER.debug("Remove Task: " + timerData.task.getClass().getSimpleName());
      LOGGER.debug("Remove UUID:     " + uuid);
    }
  }

  /**
   * 
   */
  public synchronized void execute(@Nonnull UUID uuid) {
    LOGGER.trace("execute(): UUID = '" + uuid);

    TimerData timerData = timerMap.get(uuid);

    if (null != timerData) {
      LOGGER.debug("Execute Task: " + timerData.task.getClass().getSimpleName());

      timerData.timer.schedule(timerData.task, timerData.initialDelay, timerData.period);
    }
  }

  /**
   * 
   */
  public synchronized void exit(int exitCode) {
    LOGGER.trace("exit()");

    try {
      Set<UUID> uuidSet = new HashSet<>(timerMap.keySet());

      for (UUID uuid : uuidSet) {
        remove(uuid);
      }
    } finally {
      System.exit(exitCode);
    }
  }

  /**
   *
   */
  public synchronized void shutdown() {
    LOGGER.trace("shutdown()");

    Set<UUID> uuidSet = new HashSet<>(timerMap.keySet());

    for (UUID uuid : uuidSet) {
      remove(uuid);
    }
  }

  /**
   * 
   */
  private TimerProvider() {
    this.timerMap = new HashMap<>();
  }

  /**
   * 
   */
  private class TimerData {
    private Timer timer;
    private TimerProviderTask task;

    private long initialDelay;
    private long period;
  }
}
