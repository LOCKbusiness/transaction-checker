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
  CREATE_MASTERNODE("CREATE_MASTERNODE", OpenTransactionTypeEnum.MASTERNODE),
  RESIGN_MASTERNODE("RESIGN_MASTERNODE", OpenTransactionTypeEnum.MASTERNODE),

  // UTXO ...
  SEND_FROM_LIQ("SEND_FROM_LIQ", OpenTransactionTypeEnum.UTXO),
  SEND_TO_LIQ("SEND_TO_LIQ", OpenTransactionTypeEnum.UTXO),

  UTXO_MERGE("UTXO_MERGE", OpenTransactionTypeEnum.UTXO),
  UTXO_SPLIT("UTXO_SPLIT", OpenTransactionTypeEnum.UTXO),

  ACCOUNT_TO_ACCOUNT("ACCOUNT_TO_ACCOUNT", OpenTransactionTypeEnum.UTXO),
  COMPOSITE_SWAP("COMPOSITE_SWAP", OpenTransactionTypeEnum.UTXO),

  // Withdrawal ...
  WITHDRAWAL("WITHDRAWAL", OpenTransactionTypeEnum.WITHDRAWAL),

  // Yield Machine ...
  CREATE_VAULT("CREATE_VAULT", OpenTransactionTypeEnum.YIELD_MASCHINE),
  DEPOSIT_TO_VAULT("DEPOSIT_TO_VAULT", OpenTransactionTypeEnum.YIELD_MASCHINE),
  WITHDRAW_FROM_VAULT("WITHDRAW_FROM_VAULT", OpenTransactionTypeEnum.YIELD_MASCHINE),
  TAKE_LOAN("TAKE_LOAN", OpenTransactionTypeEnum.YIELD_MASCHINE),
  PAYBACK_LOAN("PAYBACK_LOAN", OpenTransactionTypeEnum.YIELD_MASCHINE),
  POOL_ADD_LIQUIDITY("POOL_ADD_LIQUIDITY", OpenTransactionTypeEnum.YIELD_MASCHINE),
  POOL_REMOVE_LIQUIDITY("POOL_REMOVE_LIQUIDITY", OpenTransactionTypeEnum.YIELD_MASCHINE);

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
