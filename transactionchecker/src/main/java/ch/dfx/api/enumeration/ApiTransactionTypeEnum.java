package ch.dfx.api.enumeration;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.dfx.api.data.transaction.OpenTransactionTypeEnum;

/**
 * 
 */
public enum ApiTransactionTypeEnum {
  // Masternode ...
  CREATE_MASTERNODE("CreateMasternode", OpenTransactionTypeEnum.MASTERNODE),
  RESIGN_MASTERNODE("ResignMasternode", OpenTransactionTypeEnum.MASTERNODE),

  // UTXO ...
  SEND_FROM_LIQ("SendFromLiq", OpenTransactionTypeEnum.UTXO),
  SEND_TO_LIQ("SendToLiq", OpenTransactionTypeEnum.UTXO),

  UTXO_MERGE("UtxoMerge", OpenTransactionTypeEnum.UTXO),
  UTXO_SPLIT("UtxoSplit", OpenTransactionTypeEnum.UTXO),

  ACCOUNT_TO_ACCOUNT("AccountToAccount", OpenTransactionTypeEnum.UTXO),
  COMPOSITE_SWAP("CompositeSwap", OpenTransactionTypeEnum.UTXO),

  // Withdrawal ...
  WITHDRAWAL("Withdrawal", OpenTransactionTypeEnum.WITHDRAWAL),

  // Yield Machine ...
  CREATE_VAULT("CreateVault", OpenTransactionTypeEnum.YIELD_MASCHINE),
  DEPOSIT_TO_VAULT("DepositToVault", OpenTransactionTypeEnum.YIELD_MASCHINE),
  WITHDRAW_FROM_VAULT("WithdrawFromVault", OpenTransactionTypeEnum.YIELD_MASCHINE),
  TAKE_LOAN("TakeLoan", OpenTransactionTypeEnum.YIELD_MASCHINE),
  PAYBACK_LOAN("PaybackLoan", OpenTransactionTypeEnum.YIELD_MASCHINE),
  POOL_ADD_LIQUIDITY("PoolAddLiquidity", OpenTransactionTypeEnum.YIELD_MASCHINE),
  POOL_REMOVE_LIQUIDITY("PoolRemoveLiquidity", OpenTransactionTypeEnum.YIELD_MASCHINE);

  // ...
  private final String typeAsString;
  private final OpenTransactionTypeEnum openTransactionType;

  /**
   * 
   */
  private static final Map<String, ApiTransactionTypeEnum> apiTransactionTypeStringCacheMap;

  static {
    apiTransactionTypeStringCacheMap = new HashMap<>();
    Stream.of(ApiTransactionTypeEnum.values()).forEach(type -> apiTransactionTypeStringCacheMap.put(type.getTypeAsString(), type));
  }

  public static @Nullable ApiTransactionTypeEnum createByApiType(String apiTypeString) {
    return apiTransactionTypeStringCacheMap.get(apiTypeString);
  }

  /**
   * 
   */
  private ApiTransactionTypeEnum(
      @Nonnull String typeAsString,
      @Nonnull OpenTransactionTypeEnum openTransactionType) {
    this.typeAsString = typeAsString;
    this.openTransactionType = openTransactionType;
  }

  /**
   * 
   */
  public String getTypeAsString() {
    return typeAsString;
  }

  /**
   * 
   */
  public OpenTransactionTypeEnum getOpenTransactionType() {
    return openTransactionType;
  }
}
