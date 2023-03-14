package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.enumeration.TokenEnum;

/**
 * 
 */
public class HistoryAmountSheetDTO {
  private final Timestamp timestamp;

  private final Map<TokenEnum, BigDecimal> tokenToAmountMap;

  /**
   * 
   */
  public HistoryAmountSheetDTO(@Nonnull Timestamp timestamp) {
    this.timestamp = timestamp;

    this.tokenToAmountMap = new EnumMap<>(TokenEnum.class);
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public BigDecimal getAmount(@Nonnull TokenEnum token) {
    return tokenToAmountMap.getOrDefault(token, BigDecimal.ZERO);
  }

  public void put(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal amount) {
    tokenToAmountMap.put(token, amount);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
