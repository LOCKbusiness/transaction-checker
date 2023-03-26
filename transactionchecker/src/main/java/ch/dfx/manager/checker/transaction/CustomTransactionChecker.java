package ch.dfx.manager.checker.transaction;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.enumeration.ApiTransactionTypeEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.custom.DefiCustomData;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * DEV-1424 ...
 */
public class CustomTransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(CustomTransactionChecker.class);

  // ...
  private interface CustomTransactionCheckerMethod {
    boolean check(@Nonnull String hex) throws DfxException;
  }

  private final Map<ApiTransactionTypeEnum, CustomTransactionCheckerMethod> customTransactionCheckerMethodMap;

  // ...
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public CustomTransactionChecker(@Nonnull DefiDataProvider dataProvider) {
    this.dataProvider = dataProvider;

    this.customTransactionCheckerMethodMap = new EnumMap<>(ApiTransactionTypeEnum.class);

    setup();
  }

  /**
   * 
   */
  private void setup() {
    LOGGER.trace("setup()");

    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.CREATE_VAULT, (hex) -> checkCreateVault(hex));
    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.DEPOSIT_TO_VAULT, (hex) -> checkDepositToVault(hex));
    customTransactionCheckerMethodMap.put(ApiTransactionTypeEnum.WITHDRAW_FROM_VAULT, (hex) -> checkWithdrawFromVault(hex));
  }

  /**
   * 
   */
  private boolean checkCreateVault(@Nonnull String hex) throws DfxException {
    LOGGER.trace("checkCreateVault()");

    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

    Map<String, Object> resultMap = customData.getResults();
    String ownerAddress = (String) resultMap.get("ownerAddress");

    // TODO: CheckMasternodeWhitelist ...
    return false;
  }

  /**
   * 
   */
  private boolean checkDepositToVault(@Nonnull String hex) throws DfxException {
    LOGGER.trace("checkDepositToVault()");

    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

    Map<String, Object> resultMap = customData.getResults();
    String fromAddress = (String) resultMap.get("from");

    // TODO: CheckMasternodeWhitelist ...
    return false;
  }

  /**
   * 
   */
  private boolean checkWithdrawFromVault(@Nonnull String hex) throws DfxException {
    LOGGER.trace("checkWithdrawFromVault()");

    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

    Map<String, Object> resultMap = customData.getResults();
    String toAddress = (String) resultMap.get("to");

    // TODO: CheckMasternodeWhitelist ...
    return false;
  }
}
