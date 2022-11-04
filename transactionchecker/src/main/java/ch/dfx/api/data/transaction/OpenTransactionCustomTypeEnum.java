package ch.dfx.api.data.transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 
 */
public enum OpenTransactionCustomTypeEnum {
  CREATE_MASTERNODE("CreateMasternode", OpenTransactionTypeEnum.MASTERNODE),
  RESIGN_MASTERNODE("ResignMasternode", OpenTransactionTypeEnum.MASTERNODE),
  UPDATE_MASTERNODE("UpdateMasternode", OpenTransactionTypeEnum.MASTERNODE),

  VAULT("Vault", OpenTransactionTypeEnum.YIELD_MASCHINE),
  DEPOSIT_TO_VAULT("DepositToVault", OpenTransactionTypeEnum.YIELD_MASCHINE),
  WITHDRAW_FROM_VAULT("WithdrawFromVault", OpenTransactionTypeEnum.YIELD_MASCHINE),
  TAKE_LOAN("TakeLoan", OpenTransactionTypeEnum.YIELD_MASCHINE),
  PAYBACK_LOAN("PaybackLoan", OpenTransactionTypeEnum.YIELD_MASCHINE),
  ADD_POOL_LIQUIDITY("AddPoolLiquidity", OpenTransactionTypeEnum.YIELD_MASCHINE),
  REMOV_EPOOL_LIQUIDITY("RemovePoolLiquidity", OpenTransactionTypeEnum.YIELD_MASCHINE),
  POOL_SWAP("PoolSwap", OpenTransactionTypeEnum.YIELD_MASCHINE),
  ANY_ACCOUNTS_TO_ACCOUNTS("AnyAccountsToAccounts", OpenTransactionTypeEnum.YIELD_MASCHINE);

  // ...
  private final String chainTypeString;
  private final OpenTransactionTypeEnum openTransactionType;

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
      @Nonnull OpenTransactionTypeEnum openTransactionType) {
    this.chainTypeString = chainTypeString;
    this.openTransactionType = openTransactionType;
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
  public OpenTransactionTypeEnum getOpenTransactionType() {
    return openTransactionType;
  }
}
