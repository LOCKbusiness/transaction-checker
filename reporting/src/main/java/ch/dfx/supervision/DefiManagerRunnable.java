package ch.dfx.supervision;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.logging.MessageEventBus;
import ch.dfx.common.logging.MessageEventProvider;
import ch.dfx.common.logging.events.TelegramAutomaticInformationBotEvent;
import ch.dfx.config.ReportingConfigEnum;
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

    String vaultId1 = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT1_ID, "");
    List<String> checkRatio1List = ConfigProvider.getInstance().getListValue(ReportingConfigEnum.YM_VAULT1_CHECK_RATIO_LIST);
    this.vaultManager1 = new DefiVaultManager(messageEventProvider, vaultId1, checkRatio1List);

    String vaultId2 = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT2_ID, "");
    List<String> checkRatio2List = ConfigProvider.getInstance().getListValue(ReportingConfigEnum.YM_VAULT2_CHECK_RATIO_LIST);
    this.vaultManager2 = new DefiVaultManager(messageEventProvider, vaultId2, checkRatio2List);

    String vaultId3 = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT3_ID, "");
    List<String> checkRatio3List = ConfigProvider.getInstance().getListValue(ReportingConfigEnum.YM_VAULT3_CHECK_RATIO_LIST);
    this.vaultManager3 = new DefiVaultManager(messageEventProvider, vaultId3, checkRatio3List);
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
      sendTelegramMessage(message);

      LOGGER.error("run", t);
    } finally {
      isProcessing = false;

      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void doRun() {
    LOGGER.trace("doRun()");

    executeVaultCheckCollateralizationRatio();
  }

  /**
   * 
   */
  private void executeVaultCheckCollateralizationRatio() {
    LOGGER.trace("executeVaultCheckCollateralizationRatio()");

    try {
      vaultManager1.checkCollateralizationRatio();
      vaultManager2.checkCollateralizationRatio();
      vaultManager3.checkCollateralizationRatio();

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
      sendTelegramMessage(message);
    }
  }

  /**
   * 
   */
  private void sendTelegramMessage(@Nonnull String message) {
    MessageEventBus.getInstance().postEvent(new TelegramAutomaticInformationBotEvent(message));
    messageEventProvider.run();
  }
}
