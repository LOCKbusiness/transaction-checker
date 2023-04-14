package ch.dfx.reporting.transparency;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class TransparencyReportPriceHelper {
  private static final Logger LOGGER = LogManager.getLogger(TransparencyReportPriceHelper.class);

  // ...
  private final DefiDataProvider dataProvider;

  private long blockCount = 0;

  /**
   * 
   */
  public TransparencyReportPriceHelper() {
    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  public long getBlockCount() {
    return blockCount;
  }

  /**
   * 
   */
  public Map<TokenEnum, BigDecimal> createTokenToPriceMap() throws DfxException {
    LOGGER.debug("createTokenToPriceMap()");

    blockCount = dataProvider.getBlockCount();

    // ...
    Map<TokenEnum, BigDecimal> tokenToPriceMap = new EnumMap<>(TokenEnum.class);

    for (TokenEnum token : TokenEnum.values()) {
      tokenToPriceMap.put(token, dataProvider.testPoolSwap(token.toString(), TokenEnum.USDT.toString()));
    }

    return tokenToPriceMap;
  }
}
