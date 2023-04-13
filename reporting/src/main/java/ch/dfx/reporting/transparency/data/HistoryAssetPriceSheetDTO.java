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
public class HistoryAssetPriceSheetDTO {
  private final Timestamp timestamp;

  private final Map<TokenEnum, BigDecimal> tokenToAssetPriceMap;

  /**
   * 
   */
  public HistoryAssetPriceSheetDTO(@Nonnull Timestamp timestamp) {
    this.timestamp = timestamp;

    this.tokenToAssetPriceMap = new EnumMap<>(TokenEnum.class);
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public BigDecimal getPrice(@Nonnull TokenEnum token) {
    return tokenToAssetPriceMap.getOrDefault(token, BigDecimal.ZERO);
  }

  public void put(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal price) {
    tokenToAssetPriceMap.put(token, price);
  }

  @Override
  public String toString() {
    return TransactionCheckerUtils.toJson(this);
  }
}
