package ch.dfx.supervision;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.logging.MessageEventBus;
import ch.dfx.common.logging.MessageEventProvider;
import ch.dfx.defichain.data.vault.DefiVaultData;
import ch.dfx.defichain.provider.DefiDataProvider;
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
  private final List<String> checkRatioList;

  private StateEnum prevStateVault = StateEnum.UNKNOWN;
  private StateEnum currStateVault = StateEnum.UNKNOWN;

  /**
   * 
   */
  public DefiVaultManager(
      @Nonnull MessageEventProvider messageEventProvider,
      @Nonnull String vaultId,
      @Nonnull List<String> checkRatioList) {
    this.messageEventProvider = messageEventProvider;
    this.vaultId = vaultId;
    this.checkRatioList = checkRatioList;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void checkRatio() throws DfxException {
    LOGGER.trace("checkRatio()");

    try {
      if (StringUtils.isNotEmpty(vaultId)
          && !checkRatioList.isEmpty()) {
        String message = doCheckRatio(vaultId, checkRatioList);

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
      @Nonnull List<String> checkRatioList) throws DfxException {
    LOGGER.debug("doCheckRatio()");

    if (2 != checkRatioList.size()) {
      throw new DfxException("unknown check ratio value: " + checkRatioList);
    }

    BigDecimal lowRatio = new BigDecimal(checkRatioList.get(0));
    BigDecimal highRatio = new BigDecimal(checkRatioList.get(1));

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
