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
public class HistoryInterimDifferenceSheetDTO {
  private final Timestamp timestamp;

  private final Map<TokenEnum, BigDecimal> tokenToInterimDifferenceMap;

  /**
   * 
   */
  public HistoryInterimDifferenceSheetDTO(@Nonnull Timestamp timestamp) {
    this.timestamp = timestamp;

    this.tokenToInterimDifferenceMap = new EnumMap<>(TokenEnum.class);
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public BigDecimal getInterimDifference(@Nonnull TokenEnum token) {
    return tokenToInterimDifferenceMap.getOrDefault(token, BigDecimal.ZERO);
  }

  public void put(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal interimDifference) {
    tokenToInterimDifferenceMap.put(token, interimDifference);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
