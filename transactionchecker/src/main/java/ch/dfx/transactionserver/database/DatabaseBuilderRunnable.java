package ch.dfx.transactionserver.database;

import java.io.File;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class DatabaseBuilderRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseBuilderRunnable.class);

  private final File lockFile;

  private final boolean isServerOnly;

  private int errorCounter = 0;

  /**
   * 
   */
  public DatabaseBuilderRunnable(
      @Nonnull File lockFile,
      boolean isServerOnly) {
    Objects.requireNonNull(lockFile, "null lockFile is not allowed");
    this.lockFile = lockFile;
    this.isServerOnly = isServerOnly;
  }

  @Override
  public String getName() {
    return DatabaseBuilderRunnable.class.getSimpleName();
  }

  @Override
  public void run() {
    LOGGER.trace("run() ...");

    if (!isServerOnly) {
      executeDatabase();
      checkDatabase();

      checkErrorCounter();
    }

    checkLockFile();
  }

  /**
   * 
   */
  private void executeDatabase() {
    LOGGER.trace("executeDatabase() ...");

    try {
      DatabaseBuilder databaseBuilder = new DatabaseBuilder();
      databaseBuilder.execute();
    } catch (DfxException e) {
      errorCounter++;
      LOGGER.error("run: errorCounter=" + errorCounter, e.getMessage());
    } catch (Exception e) {
      errorCounter++;
      LOGGER.error("run: errorCounter=" + errorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkDatabase() {
    LOGGER.trace("checkDatabase() ...");

    try {
      DatabaseChecker databaseChecker = new DatabaseChecker();

      if (databaseChecker.check()) {
        errorCounter = 0;
      }
    } catch (DfxException e) {
      errorCounter++;
      LOGGER.error("run: errorCounter=" + errorCounter, e.getMessage());
    } catch (Exception e) {
      errorCounter++;
      LOGGER.error("run: errorCounter=" + errorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkErrorCounter() {
    LOGGER.trace("checkErrorCounter() ...");

    if (2 < errorCounter) {
      LOGGER.error("Too many errors, will exit now");
      System.exit(-1);
    }
  }

  /**
   * 
   */
  private void checkLockFile() {
    LOGGER.trace("checkLockFile() ...");

    if (!lockFile.exists()) {
      LOGGER.error("Lockfile missing, will exit now");
      System.exit(-1);
    }
  }
}
