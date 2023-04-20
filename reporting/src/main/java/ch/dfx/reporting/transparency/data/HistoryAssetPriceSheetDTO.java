package ch.dfx.reporting.transparency.data;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ch.dfx.common.enumeration.TokenEnum;

/**
 * 
 */
public class HistoryAssetPriceSheetDTO extends HistorySheetDTO {
  private long blockNumber = 0;

  private final Map<TokenEnum, BigDecimal> tokenToAssetPriceMap;

  /**
   * 
   */
  public HistoryAssetPriceSheetDTO() {
    this.tokenToAssetPriceMap = new EnumMap<>(TokenEnum.class);
  }

  public void setBlockNumber(long blockNumber) {
    this.blockNumber = blockNumber;
  }

  public long getBlockNumber() {
    return blockNumber;
  }

  public BigDecimal getPrice(@Nonnull TokenEnum token) {
    return tokenToAssetPriceMap.getOrDefault(token, BigDecimal.ZERO);
  }

  public void put(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal price) {
    tokenToAssetPriceMap.put(token, price);
  }
}
