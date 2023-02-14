package ch.dfx.supervision;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.vault.DefiVaultData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.MessageEventProvider;
import ch.dfx.logging.events.TelegramAutomaticVaultInformationBotEvent;

/**
 * 
 */
public class DefiVaultManager {
  private static final Logger LOGGER = LogManager.getLogger(DefiVaultManager.class);

  // ...
  private enum StateEnum {
    UNKNOWN,

    OK,
    LOW,
    HIGH
  }

  // ...
  private final MessageEventProvider messageEventProvider;
  private final DefiDataProvider dataProvider;

  private final String vaultId;
  private final String checkRatio;

  private StateEnum prevStateVault = StateEnum.UNKNOWN;
  private StateEnum currStateVault = StateEnum.UNKNOWN;

  /**
   * 
   */
  public DefiVaultManager(
      @Nonnull MessageEventProvider messageEventProvider,
      @Nonnull String vaultId,
      @Nonnull String checkRatio) {
    this.messageEventProvider = messageEventProvider;
    this.vaultId = vaultId;
    this.checkRatio = checkRatio;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void checkRatio() throws DfxException {
    LOGGER.trace("checkRatio()");

    try {
      if (StringUtils.isNotEmpty(vaultId)
          && StringUtils.isNotEmpty(checkRatio)) {
        String message = doCheckRatio(vaultId, checkRatio);

        if (prevStateVault != currStateVault) {
          MessageEventBus.getInstance().postEvent(new TelegramAutomaticVaultInformationBotEvent(message));
          LOGGER.info(message);
          messageEventProvider.run();

          prevStateVault = currStateVault;
        }
      }
    } catch (Exception e) {
      throw new DfxException("checkRatio", e);
    }
  }

  /**
   * 
   */
  private String doCheckRatio(
      @Nonnull String vaultId,
      @Nonnull String checkRatio) throws DfxException {
    LOGGER.debug("doCheckRatio()");

    String[] checkRatioSplitArray = checkRatio.split("\\,");

    BigDecimal lowRatio = new BigDecimal(checkRatioSplitArray[0]);
    BigDecimal highRatio = 2 == checkRatioSplitArray.length ? new BigDecimal(checkRatioSplitArray[1]) : lowRatio;

    DefiVaultData vaultData = dataProvider.getVault(vaultId);

    BigDecimal informativeRatio = vaultData.getInformativeRatio();

    String message;

    if (-1 == informativeRatio.compareTo(lowRatio)) {
      currStateVault = StateEnum.LOW;
      message = "[LOW] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    } else if (1 == informativeRatio.compareTo(highRatio)) {
      currStateVault = StateEnum.HIGH;
      message = "[HIGH] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    } else {
      currStateVault = StateEnum.OK;
      message = "[OK] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    }

    return message;
  }
}
