package ch.dfx.supervision;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.MessageEventProvider;
import ch.dfx.logging.events.MessageEvent;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class DefiManagerRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(DefiManagerRunnable.class);

  // ...
  private final MessageEventProvider messageEventProvider;
  private final DefiVaultManager vaultManager;

  // ...
  private boolean isProcessing = false;

  // ...
  private int vaultCheckErrorCounter = 0;

  /**
   * 
   */
  public DefiManagerRunnable(@Nonnull MessageEventProvider messageEventProvider) {
    this.messageEventProvider = messageEventProvider;

    this.vaultManager = new DefiVaultManager(messageEventProvider);
  }

  @Override
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.debug("run()");

    long startTime = System.currentTimeMillis();

    isProcessing = true;

    try {
      doRun();

      checkErrorCounter();
    } catch (Throwable t) {
      vaultCheckErrorCounter++;

      String message = "[ERROR]: Vault Check Error - " + t.getMessage();
      MessageEventBus.getInstance().postEvent(new MessageEvent(message));
      messageEventProvider.run();

      LOGGER.error("run", t);
    } finally {
      isProcessing = false;

      LOGGER.debug("[DefiManagerRunnable] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void doRun() {
    LOGGER.trace("doRun()");

    executeVaultCheckRatio();
  }

  /**
   * 
   */
  private void executeVaultCheckRatio() {
    LOGGER.trace("executeVaultCheckRatio()");

    try {
      // ...
      vaultManager.checkRatio();

      vaultCheckErrorCounter = 0;
    } catch (DfxException e) {
      vaultCheckErrorCounter++;
      LOGGER.error("Vault Manager: errorCounter=" + vaultCheckErrorCounter, e.getMessage());
    } catch (Exception e) {
      vaultCheckErrorCounter++;
      LOGGER.error("Vault Manager: errorCounter=" + vaultCheckErrorCounter, e);
    }
  }

  /**
   * 
   */
  private void checkErrorCounter() {
    LOGGER.trace("checkErrorCounter()");

    if (2 < vaultCheckErrorCounter) {
      String message = "[ERROR]: " + vaultCheckErrorCounter + " Vault Check Errors";
      MessageEventBus.getInstance().postEvent(new MessageEvent(message));
      messageEventProvider.run();
    }
  }
}
