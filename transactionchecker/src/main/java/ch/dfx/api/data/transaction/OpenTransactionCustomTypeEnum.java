package ch.dfx.api.data.transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.dfx.api.enumeration.ApiTransactionTypeEnum;

/**
 * 
 */
public enum OpenTransactionCustomTypeEnum {
  CREATE_MASTERNODE("CreateMasternode", ApiTransactionTypeEnum.CREATE_MASTERNODE),
  RESIGN_MASTERNODE("ResignMasternode", ApiTransactionTypeEnum.RESIGN_MASTERNODE),

  VAULT("Vault", ApiTransactionTypeEnum.CREATE_VAULT),
  DEPOSIT_TO_VAULT("DepositToVault", ApiTransactionTypeEnum.DEPOSIT_TO_VAULT),
  WITHDRAW_FROM_VAULT("WithdrawFromVault", ApiTransactionTypeEnum.WITHDRAW_FROM_VAULT),
  TAKE_LOAN("TakeLoan", ApiTransactionTypeEnum.TAKE_LOAN),
  PAYBACK_LOAN("PaybackLoan", ApiTransactionTypeEnum.PAYBACK_LOAN),
  ADD_POOL_LIQUIDITY("AddPoolLiquidity", ApiTransactionTypeEnum.POOL_ADD_LIQUIDITY),
  REMOVE_POOL_LIQUIDITY("RemovePoolLiquidity", ApiTransactionTypeEnum.POOL_REMOVE_LIQUIDITY),

  POOL_SWAP("PoolSwap", ApiTransactionTypeEnum.COMPOSITE_SWAP),
  ANY_ACCOUNTS_TO_ACCOUNTS("AnyAccountsToAccounts", ApiTransactionTypeEnum.ACCOUNT_TO_ACCOUNT);

  // ...
  private final String chainTypeString;
  private final ApiTransactionTypeEnum apiTransactionType;

  /**
   * 
   */
  private static final Map<String, OpenTransactionCustomTypeEnum> chainTypeStringCacheMap;

  static {
    chainTypeStringCacheMap = new HashMap<>();
    Stream.of(OpenTransactionCustomTypeEnum.values()).forEach(type -> chainTypeStringCacheMap.put(type.getChainType(), type));
  }

  public static @Nullable OpenTransactionCustomTypeEnum createByChainType(String chainTypeString) {
    return chainTypeStringCacheMap.get(chainTypeString);
  }

  /**
   * 
   */
  private OpenTransactionCustomTypeEnum(
      @Nonnull String chainTypeString,
      @Nonnull ApiTransactionTypeEnum apiTransactionType) {
    this.chainTypeString = chainTypeString;
    this.apiTransactionType = apiTransactionType;
  }

  /**
   * 
   */
  public String getChainType() {
    return chainTypeString;
  }

  /**
   * 
   */
  public ApiTransactionTypeEnum getApiTransactionType() {
    return apiTransactionType;
  }
}
