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
  // ...
  UNKNOWN(null, OpenTransactionTypeEnum.UNKNOWN),

  // Masternode ...
  CREATE_MASTERNODE("CreateMasternode", OpenTransactionTypeEnum.MASTERNODE),
  RESIGN_MASTERNODE("ResignMasternode", OpenTransactionTypeEnum.MASTERNODE),
  UPDATE_MASTERNODE("UpdateMasternode", OpenTransactionTypeEnum.MASTERNODE),
  VOTE_MASTERNODE("VoteMasternode", OpenTransactionTypeEnum.MASTERNODE),

  // UTXO ...
  SEND_FROM_LIQ("SendFromLiq", OpenTransactionTypeEnum.UTXO),
  SEND_TO_LIQ("SendToLiq", OpenTransactionTypeEnum.UTXO),

  UTXO_MERGE("UtxoMerge", OpenTransactionTypeEnum.UTXO),
  UTXO_SPLIT("UtxoSplit", OpenTransactionTypeEnum.UTXO),

  ACCOUNT_TO_ACCOUNT("AccountToAccount", OpenTransactionTypeEnum.UTXO),
  UTXO_TO_ACCOUNT("UtxoToAccount", OpenTransactionTypeEnum.UTXO),
  COMPOSITE_SWAP("CompositeSwap", OpenTransactionTypeEnum.UTXO),

  // Withdrawal ...
  WITHDRAWAL("Withdrawal", OpenTransactionTypeEnum.WITHDRAWAL),

  // Yield Machine ...
  CREATE_VAULT("CreateVault", OpenTransactionTypeEnum.YIELD_MACHINE),
  DEPOSIT_TO_VAULT("DepositToVault", OpenTransactionTypeEnum.YIELD_MACHINE),
  WITHDRAW_FROM_VAULT("WithdrawFromVault", OpenTransactionTypeEnum.YIELD_MACHINE),
  TAKE_LOAN("TakeLoan", OpenTransactionTypeEnum.YIELD_MACHINE),
  PAYBACK_LOAN("PaybackLoan", OpenTransactionTypeEnum.YIELD_MACHINE),
  POOL_ADD_LIQUIDITY("PoolAddLiquidity", OpenTransactionTypeEnum.YIELD_MACHINE),
  POOL_REMOVE_LIQUIDITY("PoolRemoveLiquidity", OpenTransactionTypeEnum.YIELD_MACHINE);

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

  public static @Nonnull ApiTransactionTypeEnum createByApiType(@Nullable String apiTypeString) {
    ApiTransactionTypeEnum apiTransactionType = apiTransactionTypeStringCacheMap.get(apiTypeString);

    if (null == apiTransactionType) {
      apiTransactionType = UNKNOWN;
    }

    return apiTransactionType;
  }

  /**
   * 
   */
  private ApiTransactionTypeEnum(
      @Nullable String typeAsString,
      @Nonnull OpenTransactionTypeEnum openTransactionType) {
    this.typeAsString = typeAsString;
    this.openTransactionType = openTransactionType;
  }

  /**
   * 
   */
  public @Nullable String getTypeAsString() {
    return typeAsString;
  }

  /**
   * 
   */
  public OpenTransactionTypeEnum getOpenTransactionType() {
    return openTransactionType;
  }
}
