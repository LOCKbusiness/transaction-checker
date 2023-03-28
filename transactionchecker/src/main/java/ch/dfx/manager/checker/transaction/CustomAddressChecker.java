package ch.dfx.manager.checker.transaction;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.enumeration.ApiTransactionTypeEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 *
 */
public class CustomAddressChecker extends TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(CustomAddressChecker.class);

  // ...
  private interface CustomTransactionCheckerMethod {
    boolean check(@Nonnull String hex) throws DfxException;
  }

  private final Map<ApiTransactionTypeEnum, CustomTransactionCheckerMethod> customTransactionCheckerMethodMap;

  // ...
  private final MasternodeWhitelistChecker masternodeWhitelistChecker;

  /**
   * 
   */
  public CustomAddressChecker(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiDataProvider dataProvider,
      @Nonnull MasternodeWhitelistChecker masternodeWhitelistChecker) {
    super(apiAccessHandler, dataProvider);

    this.masternodeWhitelistChecker = masternodeWhitelistChecker;

    this.customTransactionCheckerMethodMap = new EnumMap<>(ApiTransactionTypeEnum.class);

    setup();
  }

  /**
   * 
   */
  private void setup() {
    LOGGER.trace("setup()");

    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.CREATE_VAULT, (hex) -> doCheck(hex, "ownerAddress"));
    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.DEPOSIT_TO_VAULT, (hex) -> doCheck(hex, "from"));
    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.WITHDRAW_FROM_VAULT, (hex) -> doCheck(hex, "to"));
    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.TAKE_LOAN, (hex) -> doCheck(hex, "to"));
    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.PAYBACK_LOAN, (hex) -> doCheck(hex, "from"));
    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.POOL_ADD_LIQUIDITY, (hex) -> doCheck(hex, "shareaddress"));
    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.POOL_REMOVE_LIQUIDITY, (hex) -> doCheck(hex, "from"));
  }

  /**
   * 
   */
  public OpenTransactionDTOList checkCustomAddress(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("checkCustomAddress()");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      if (doCheckCustomAddress(openTransactionDTO)) {
        checkedOpenTransactionDTOList.add(openTransactionDTO);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doCheckCustomAddress(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("doCheckCustomAddress()");

    boolean isValid;

    try {
      ApiTransactionTypeEnum apiTransactionType = openTransactionDTO.getType();
      CustomTransactionCheckerMethod customTransactionCheckerMethod = customTransactionCheckerMethodMap.get(apiTransactionType);

      if (null == customTransactionCheckerMethod) {
        isValid = true;
      } else {
        String hex = openTransactionDTO.getRawTx().getHex();
        isValid = customTransactionCheckerMethod.check(hex);

        if (!isValid) {
          openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - custom address not in whitelist");
          sendInvalidated(openTransactionDTO);
        }
      }
    } catch (Exception e) {
      LOGGER.error("doCheckCustomAddress", e);
      isValid = false;
    }

    return isValid;
  }

  /**
   * 
   */
  private boolean doCheck(
      @Nonnull String hex,
      @Nonnull String resultAddressDefinition) throws DfxException {
    LOGGER.trace("doCheck()");

    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

    Map<String, Object> resultMap = customData.getResults();
    String address = (String) resultMap.getOrDefault(resultAddressDefinition, "unknown");

    return masternodeWhitelistChecker.checkMasternodeWhitelist(address);
  }
}
