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
import ch.dfx.defichain.handler.DefiMessageHandler;

/**
 *
 */
public class CustomAddressChecker extends TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(CustomAddressChecker.class);

  // ...
  private interface CustomCheckerMethod {
    boolean check(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException;
  }

  private final Map<ApiTransactionTypeEnum, CustomCheckerMethod> customCheckerMethodMap;

  // ...
  private final MasternodeWhitelistChecker masternodeWhitelistChecker;
  private final VaultWhitelistChecker vaultWhitelistChecker;

  /**
   * 
   */
  public CustomAddressChecker(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiMessageHandler messageHandler,
      @Nonnull MasternodeWhitelistChecker masternodeWhitelistChecker,
      @Nonnull VaultWhitelistChecker vaultWhitelistChecker) {
    super(apiAccessHandler, messageHandler);

    this.masternodeWhitelistChecker = masternodeWhitelistChecker;
    this.vaultWhitelistChecker = vaultWhitelistChecker;

    this.customCheckerMethodMap = new EnumMap<>(ApiTransactionTypeEnum.class);

    setup();
  }

  /**
   * 
   */
  private void setup() {
    LOGGER.trace("setup()");

    customCheckerMethodMap.put(ApiTransactionTypeEnum.CREATE_VAULT, (openTransactionDTO) -> doAddressCheck(openTransactionDTO, "ownerAddress"));
    customCheckerMethodMap.put(ApiTransactionTypeEnum.DEPOSIT_TO_VAULT, (openTransactionDTO) -> doVaultCheck(openTransactionDTO, "from"));
    customCheckerMethodMap.put(ApiTransactionTypeEnum.WITHDRAW_FROM_VAULT, (openTransactionDTO) -> doVaultCheck(openTransactionDTO, "to"));
    customCheckerMethodMap.put(ApiTransactionTypeEnum.TAKE_LOAN, (openTransactionDTO) -> doVaultCheck(openTransactionDTO, "to"));
    customCheckerMethodMap.put(ApiTransactionTypeEnum.PAYBACK_LOAN, (openTransactionDTO) -> doVaultCheck(openTransactionDTO, "from"));
    customCheckerMethodMap.put(ApiTransactionTypeEnum.POOL_ADD_LIQUIDITY, (openTransactionDTO) -> doAddressCheck(openTransactionDTO, "shareaddress"));
    customCheckerMethodMap.put(ApiTransactionTypeEnum.POOL_REMOVE_LIQUIDITY, (openTransactionDTO) -> doAddressCheck(openTransactionDTO, "from"));

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
      CustomCheckerMethod customTransactionCheckerMethod = customCheckerMethodMap.get(apiTransactionType);

      if (null == customTransactionCheckerMethod) {
        isValid = true;
      } else {
        isValid = customTransactionCheckerMethod.check(openTransactionDTO);
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
  private boolean doAddressCheck(
      @Nonnull OpenTransactionDTO openTransactionDTO,
      @Nonnull String resultAddressDefinition) throws DfxException {
    LOGGER.trace("doAddressCheck()");

    DefiCustomData customData = openTransactionDTO.getTransactionCustomData();

    return doMasternodeWhitelistCheck(customData, openTransactionDTO, resultAddressDefinition);
  }

  /**
   * 
   */
  private boolean doVaultCheck(
      @Nonnull OpenTransactionDTO openTransactionDTO,
      @Nonnull String resultAddressDefinition) throws DfxException {
    LOGGER.trace("doVaultCheck()");

    DefiCustomData customData = openTransactionDTO.getTransactionCustomData();

    boolean isValid = doMasternodeWhitelistCheck(customData, openTransactionDTO, resultAddressDefinition);

    if (isValid) {
      isValid = doVaultIdCheck(customData, openTransactionDTO);
    }

    return isValid;
  }

  /**
   * 
   */
  private boolean doMasternodeWhitelistCheck(
      @Nonnull DefiCustomData customData,
      @Nonnull OpenTransactionDTO openTransactionDTO,
      @Nonnull String resultAddressDefinition) throws DfxException {
    LOGGER.trace("doMasternodeWhitelistCheck()");

    Map<String, Object> resultMap = customData.getResults();
    String address = (String) resultMap.getOrDefault(resultAddressDefinition, "unknown");

    boolean isValid = masternodeWhitelistChecker.checkMasternodeWhitelist(address);

    if (!isValid) {
      openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - invalid custom address");
      sendInvalidated(openTransactionDTO);
    }

    return isValid;
  }

  /**
   * 
   */
  private boolean doVaultIdCheck(
      @Nonnull DefiCustomData customData,
      @Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("doVaultIdCheck()");

    Map<String, Object> resultMap = customData.getResults();
    String vaultId = (String) resultMap.getOrDefault("vaultId", "unknown");

    boolean isValid = vaultWhitelistChecker.checkVaultWhitelist(vaultId);

    if (!isValid) {
      openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - invalid vault id");
      sendInvalidated(openTransactionDTO);
    }

    return isValid;
  }
}
