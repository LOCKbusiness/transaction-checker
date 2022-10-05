package ch.dfx.transactionserver.scheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public class SchedulerProvider {
  private static final Logger LOGGER = LogManager.getLogger(SchedulerProvider.class);

  private static final int MAX_EXECUTOR = 2;

  private static SchedulerProvider instance = null;

  private final ScheduledExecutorService executorService;

  private final Map<UUID, ScheduledFuture<?>> scheduledFutureMap;

  /**
   *
   */
  public static SchedulerProvider getInstance() {
    if (null == instance) {
      instance = new SchedulerProvider();
    }

    return instance;
  }

  /**
   *
   */
  public void shutdown() {
    LOGGER.trace("shutdown() ...");

    Set<UUID> executorUuidSet = new HashSet<>(scheduledFutureMap.keySet());

    for (UUID uuid : executorUuidSet) {
      remove(uuid);
    }

    executorService.shutdown();
  }

  /**
   *
   */
  public @Nullable UUID add(
      @Nonnull Runnable runnable,
      int initialDelay,
      long period,
      @Nonnull TimeUnit timeUnit) {
    LOGGER.trace("add() ...");

    UUID uuid = null;

    if (MAX_EXECUTOR > scheduledFutureMap.size()) {
      uuid = UUID.randomUUID();
      ScheduledFuture<?> scheduledFuture =
          executorService.scheduleAtFixedRate(runnable, initialDelay, period, timeUnit);

      scheduledFutureMap.put(uuid, scheduledFuture);
    }

    LOGGER.debug("Runnable: " + runnable.getClass().getSimpleName());
    LOGGER.debug("UUID:     " + uuid);

    return uuid;
  }

  /**
   *
   */
  public void remove(@Nonnull UUID uuid) {
    LOGGER.trace("remove(): UUID = '" + uuid + "' ...");

    ScheduledFuture<?> scheduledFuture = scheduledFutureMap.remove(uuid);

    if (null != scheduledFuture) {
      scheduledFuture.cancel(false);
    }
  }

  /**
   *
   */
  private SchedulerProvider() {
    this.executorService = Executors.newScheduledThreadPool(MAX_EXECUTOR);
    this.scheduledFutureMap = new HashMap<>();
  }
}
