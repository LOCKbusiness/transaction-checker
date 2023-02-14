package ch.dfx.supervision;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
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
  private final DefiVaultManager vaultManager1;
  private final DefiVaultManager vaultManager2;
  private final DefiVaultManager vaultManager3;

  // ...
  private boolean isProcessing = false;

  // ...
  private int vaultCheckErrorCounter = 0;

  /**
   * 
   */
  public DefiManagerRunnable(@Nonnull MessageEventProvider messageEventProvider) {
    this.messageEventProvider = messageEventProvider;

    String vaultId1 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT1_ID, "");
    String checkRatio1 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT1_CHECK_RATIO, "");
    this.vaultManager1 = new DefiVaultManager(messageEventProvider, vaultId1, checkRatio1);

    String vaultId2 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT2_ID, "");
    String checkRatio2 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT2_CHECK_RATIO, "");
    this.vaultManager2 = new DefiVaultManager(messageEventProvider, vaultId2, checkRatio2);

    String vaultId3 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT3_ID, "");
    String checkRatio3 = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT3_CHECK_RATIO, "");
    this.vaultManager3 = new DefiVaultManager(messageEventProvider, vaultId3, checkRatio3);
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
      vaultManager1.checkRatio();
      vaultManager2.checkRatio();
      vaultManager3.checkRatio();

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
