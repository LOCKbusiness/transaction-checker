package ch.dfx.api.enumeration;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

  // Yield Machine ...
  CREATE_VAULT("CreateVault", OpenTransactionTypeEnum.YIELD_MACHINE),
  DEPOSIT_TO_VAULT("DepositToVault", OpenTransactionTypeEnum.YIELD_MACHINE),
  WITHDRAW_FROM_VAULT("WithdrawFromVault", OpenTransactionTypeEnum.YIELD_MACHINE),
  TAKE_LOAN("TakeLoan", OpenTransactionTypeEnum.YIELD_MACHINE),
  PAYBACK_LOAN("PaybackLoan", OpenTransactionTypeEnum.YIELD_MACHINE),
  POOL_ADD_LIQUIDITY("PoolAddLiquidity", OpenTransactionTypeEnum.YIELD_MACHINE),
  POOL_REMOVE_LIQUIDITY("PoolRemoveLiquidity", OpenTransactionTypeEnum.YIELD_MACHINE),
  COMPOSITE_SWAP("CompositeSwap", OpenTransactionTypeEnum.YIELD_MACHINE),

  // Account To Account ...
  ACCOUNT_TO_ACCOUNT("AccountToAccount", OpenTransactionTypeEnum.ACCOUNT_TO_ACCOUNT),

  // Withdrawal ...
  WITHDRAWAL("Withdrawal", OpenTransactionTypeEnum.WITHDRAWAL);

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

  // https://github.com/DeFiCh/ain /src/masternodes/mn_checks.h ...
  private static final Map<ApiTransactionTypeEnum, Set<String>> apiTransactionTypeToCustomTransactionTypeMap;

  static {
    apiTransactionTypeToCustomTransactionTypeMap = new EnumMap<>(ApiTransactionTypeEnum.class);

    apiTransactionTypeToCustomTransactionTypeMap.put(CREATE_MASTERNODE, new HashSet<>(Arrays.asList("C")));
    apiTransactionTypeToCustomTransactionTypeMap.put(RESIGN_MASTERNODE, new HashSet<>(Arrays.asList("R")));
    apiTransactionTypeToCustomTransactionTypeMap.put(UPDATE_MASTERNODE, new HashSet<>(Arrays.asList("m")));
    apiTransactionTypeToCustomTransactionTypeMap.put(VOTE_MASTERNODE, new HashSet<>(Arrays.asList("O")));

    apiTransactionTypeToCustomTransactionTypeMap.put(SEND_FROM_LIQ, new HashSet<>(Arrays.asList("0")));
    apiTransactionTypeToCustomTransactionTypeMap.put(SEND_TO_LIQ, new HashSet<>(Arrays.asList("0")));
    apiTransactionTypeToCustomTransactionTypeMap.put(UTXO_MERGE, new HashSet<>(Arrays.asList("0")));
    apiTransactionTypeToCustomTransactionTypeMap.put(UTXO_SPLIT, new HashSet<>(Arrays.asList("0")));

    apiTransactionTypeToCustomTransactionTypeMap.put(ACCOUNT_TO_ACCOUNT, new HashSet<>(Arrays.asList("B", "a")));

    apiTransactionTypeToCustomTransactionTypeMap.put(WITHDRAWAL, new HashSet<>(Arrays.asList("0", "B", "a")));

    apiTransactionTypeToCustomTransactionTypeMap.put(CREATE_VAULT, new HashSet<>(Arrays.asList("V")));
    apiTransactionTypeToCustomTransactionTypeMap.put(DEPOSIT_TO_VAULT, new HashSet<>(Arrays.asList("S")));
    apiTransactionTypeToCustomTransactionTypeMap.put(WITHDRAW_FROM_VAULT, new HashSet<>(Arrays.asList("J")));
    apiTransactionTypeToCustomTransactionTypeMap.put(TAKE_LOAN, new HashSet<>(Arrays.asList("X")));
    apiTransactionTypeToCustomTransactionTypeMap.put(PAYBACK_LOAN, new HashSet<>(Arrays.asList("H", "k")));

    apiTransactionTypeToCustomTransactionTypeMap.put(POOL_ADD_LIQUIDITY, new HashSet<>(Arrays.asList("l")));
    apiTransactionTypeToCustomTransactionTypeMap.put(POOL_REMOVE_LIQUIDITY, new HashSet<>(Arrays.asList("r")));

    apiTransactionTypeToCustomTransactionTypeMap.put(COMPOSITE_SWAP, new HashSet<>(Arrays.asList("s", "i")));
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

  /**
   * 
   */
  public boolean checkCustomType(@Nonnull String customType) {
    Set<String> customTypeSet = apiTransactionTypeToCustomTransactionTypeMap.getOrDefault(this, new HashSet<>());
    return customTypeSet.contains(customType);
  }
}
