package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ch.dfx.common.enumeration.TokenEnum;

/**
 * 
 */
public class HistoryPriceSheetDTO extends HistorySheetDTO {
  private final Map<TokenEnum, BigDecimal> tokenToPriceMap;

  /**
   * 
   */
  public HistoryPriceSheetDTO() {
    this.tokenToPriceMap = new EnumMap<>(TokenEnum.class);
  }

  public BigDecimal getPrice(@Nonnull TokenEnum token) {
    return tokenToPriceMap.getOrDefault(token, BigDecimal.ZERO);
  }

  public void put(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal price) {
    tokenToPriceMap.put(token, price);
  }
}
