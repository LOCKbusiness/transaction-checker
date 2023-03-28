package ch.dfx.supervision;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.logging.MessageEventBus;
import ch.dfx.common.logging.MessageEventProvider;
import ch.dfx.common.logging.events.TelegramAutomaticInformationBotEvent;
import ch.dfx.defichain.data.vault.DefiVaultData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.logging.events.TelegramAutomaticVaultInformationBotEvent;

/**
 * 
 */
public class DefiVaultManager {
  private static final Logger LOGGER = LogManager.getLogger(DefiVaultManager.class);

  // ...
  private static final DecimalFormat GERMAN_PERCENTAGE_FORMAT = new DecimalFormat("#,##0.00");

  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  private static final BigDecimal MIN_DFI_RATIO = new BigDecimal(0.5);
  private static final BigDecimal HUNDERED = new BigDecimal(100);

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

  private StateEnum prevStateCollateralizationRatio = StateEnum.UNKNOWN;
  private StateEnum currStateCollateralizationRatio = StateEnum.UNKNOWN;

  private StateEnum prevStateDFIRatio = StateEnum.UNKNOWN;
  private StateEnum currStateDFIRatio = StateEnum.UNKNOWN;

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
  public void checkCollateralizationRatio() throws DfxException {
    LOGGER.trace("checkCollateralizationRatio()");

    try {
      if (StringUtils.isNotEmpty(vaultId)
          && !checkRatioList.isEmpty()) {
        String message = doCheckCollateralizationRatio(vaultId, checkRatioList);

        if (StateEnum.UNKNOWN == prevStateCollateralizationRatio) {
          prevStateCollateralizationRatio = StateEnum.OK;
        }

        if (prevStateCollateralizationRatio != currStateCollateralizationRatio) {
          MessageEventBus.getInstance().postEvent(new TelegramAutomaticVaultInformationBotEvent(message));
          LOGGER.info(message);
          messageEventProvider.run();

          prevStateCollateralizationRatio = currStateCollateralizationRatio;
        }
      }
    } catch (Exception e) {
      throw new DfxException("checkCollateralizationRatio", e);
    }
  }

  /**
   * 
   */
  private String doCheckCollateralizationRatio(
      @Nonnull String vaultId,
      @Nonnull List<String> checkRatioList) throws DfxException {
    LOGGER.debug("doCheckCollateralizationRatio()");

    if (2 != checkRatioList.size()) {
      throw new DfxException("unknown check ratio value: " + checkRatioList);
    }

    BigDecimal lowRatio = new BigDecimal(checkRatioList.get(0));
    BigDecimal highRatio = new BigDecimal(checkRatioList.get(1));

    DefiVaultData vaultData = dataProvider.getVault(vaultId);

    BigDecimal informativeRatio = vaultData.getInformativeRatio();

    String message;

    if (-1 == informativeRatio.compareTo(lowRatio)) {
      currStateCollateralizationRatio = StateEnum.LOW;
      message = "[LOW] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    } else if (1 == informativeRatio.compareTo(highRatio)) {
      currStateCollateralizationRatio = StateEnum.HIGH;
      message = "[HIGH] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    } else {
      currStateCollateralizationRatio = StateEnum.OK;
      message = "[OK] Vault Ratio: " + informativeRatio + "\nhttps://defiscan.live/vaults/" + vaultId;
    }

    return message;
  }

  /**
   * 
   */
  public void checkDFIRatio() throws DfxException {
    LOGGER.trace("checkDFIRatio()");

    try {
      if (StringUtils.isNotEmpty(vaultId)) {
        DefiVaultData vaultData = dataProvider.getVault(vaultId);
        List<String> collateralAmounts = vaultData.getCollateralAmounts();

        // ...
        Map<String, BigDecimal> tokenToAmountMap = TransactionCheckerUtils.getTokenToAmountMap(collateralAmounts);
        Map<String, BigDecimal> activePriceMap = dataProvider.getActivePriceMap(tokenToAmountMap.keySet());

        // ...
        String message = doCheckDFIRatio(tokenToAmountMap, activePriceMap);

        if (StateEnum.UNKNOWN == prevStateDFIRatio) {
          prevStateDFIRatio = StateEnum.OK;
        }

        if (prevStateDFIRatio != currStateDFIRatio) {
//          MessageEventBus.getInstance().postEvent(new TelegramAutomaticVaultInformationBotEvent(message));
          MessageEventBus.getInstance().postEvent(new TelegramAutomaticInformationBotEvent(message));
          LOGGER.info(message);
          messageEventProvider.run();

          prevStateDFIRatio = currStateDFIRatio;
        }
      }
    } catch (Exception e) {
      throw new DfxException("checkDFIRatio", e);
    }
  }

  /**
   * 
   */
  /**
   * 
   */
  public String doCheckDFIRatio(
      @Nonnull Map<String, BigDecimal> tokenToAmountMap,
      @Nonnull Map<String, BigDecimal> activePriceMap) throws DfxException {
    LOGGER.trace("doCheckDFIRatio()");

    BigDecimal dfiValue = BigDecimal.ZERO;
    BigDecimal otherValue = BigDecimal.ZERO;

    for (Entry<String, BigDecimal> tokenToAmountMapEntry : tokenToAmountMap.entrySet()) {
      String token = tokenToAmountMapEntry.getKey();
      BigDecimal amount = tokenToAmountMapEntry.getValue();

      BigDecimal tokenPrice = activePriceMap.get(token);

      if (TokenEnum.DFI.toString().equals(token)) {
        dfiValue = dfiValue.add(amount.multiply(tokenPrice));
      } else if (TokenEnum.DUSD.toString().equals(token)) {
        tokenPrice = new BigDecimal(1.2);
        otherValue = otherValue.add(amount.multiply(tokenPrice).setScale(SCALE, RoundingMode.HALF_UP));
      } else {
        otherValue = otherValue.add(amount.multiply(tokenPrice));
      }
    }

    // ...
    BigDecimal totalValue = dfiValue.add(otherValue);
    BigDecimal dfiRatio = dfiValue.divide(totalValue, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    // ...
    if (1 == MIN_DFI_RATIO.compareTo(dfiRatio)) {
      currStateDFIRatio = StateEnum.LOW;
    } else {
      currStateDFIRatio = StateEnum.OK;
    }

    String message =
        new StringBuilder()
            .append("[").append(currStateDFIRatio.toString()).append("] ")
            .append("DFI Collateral Ratio: ")
            .append(GERMAN_PERCENTAGE_FORMAT.format(dfiRatio.multiply(HUNDERED))).append("%")
            .append("\nhttps://defiscan.live/vaults/").append(vaultId)
            .toString();

    return message;
  }
}
