package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ch.dfx.common.enumeration.TokenEnum;

/**
 * 
 */
public class HistoryAmountSheetDTO extends HistorySheetDTO {
  private final Map<TokenEnum, BigDecimal> tokenToAmountMap;

  /**
   * 
   */
  public HistoryAmountSheetDTO() {
    this.tokenToAmountMap = new EnumMap<>(TokenEnum.class);
  }

  public BigDecimal getAmount(@Nonnull TokenEnum token) {
    return tokenToAmountMap.getOrDefault(token, BigDecimal.ZERO);
  }

  public void put(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal amount) {
    tokenToAmountMap.put(token, amount);
  }
}
