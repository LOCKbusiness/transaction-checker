package ch.dfx.supervision;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.data.vault.DefiVaultData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.logging.MessageEventBus;
import ch.dfx.logging.MessageEventProvider;
import ch.dfx.logging.events.MessageEvent;

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
  private final DefiDataProvider dataProvider;
  private final MessageEventProvider messageEventProvider;

  private StateEnum prevState = StateEnum.UNKNOWN;
  private StateEnum currState = StateEnum.UNKNOWN;

  /**
   * 
   */
  public DefiVaultManager(@Nonnull MessageEventProvider messageEventProvider) {
    this.messageEventProvider = messageEventProvider;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void checkRatio() throws DfxException {
    LOGGER.trace("checkRatio()");

    try {
      String vaultId = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT_ID, "");
      String checkRatio = ConfigPropertyProvider.getInstance().getPropertyOrDefault(PropertyEnum.DFI_YM_VAULT_CHECK_RATIO, "");
      LOGGER.trace("VaultId: " + vaultId);
      LOGGER.trace("CheckRatio: " + checkRatio);

      if (StringUtils.isNotEmpty(vaultId)
          && StringUtils.isNotEmpty(checkRatio)) {
        String message = doCheckRatio(vaultId, checkRatio);

        if (prevState != currState) {
          MessageEventBus.getInstance().postEvent(new MessageEvent(message));
          LOGGER.info(message);
          messageEventProvider.run();

          prevState = currState;
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
      currState = StateEnum.LOW;
      message = "[LOW] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    } else if (1 == informativeRatio.compareTo(highRatio)) {
      currState = StateEnum.HIGH;
      message = "[HIGH] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    } else {
      currState = StateEnum.OK;
      message = "[OK] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    }

    return message;
  }
}
