package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ch.dfx.common.enumeration.TokenEnum;

/**
 * 
 */
public class HistoryInterimDifferenceSheetDTO extends HistorySheetDTO {
  private final Map<TokenEnum, BigDecimal> tokenToInterimDifferenceMap;

  /**
   * 
   */
  public HistoryInterimDifferenceSheetDTO() {
    this.tokenToInterimDifferenceMap = new EnumMap<>(TokenEnum.class);
  }

  public BigDecimal getInterimDifference(@Nonnull TokenEnum token) {
    return tokenToInterimDifferenceMap.getOrDefault(token, BigDecimal.ZERO);
  }

  public void put(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal interimDifference) {
    tokenToInterimDifferenceMap.put(token, interimDifference);
  }
}
