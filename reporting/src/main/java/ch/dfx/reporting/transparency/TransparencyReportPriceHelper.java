package ch.dfx.reporting.transparency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.defichain.data.pool.DefiPoolPairData;

/**
 * 
 */
public class TransparencyReportPriceHelper {
  private static final Logger LOGGER = LogManager.getLogger(TransparencyReportPriceHelper.class);

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  // ...
  private Map<String, DefiPoolPairData> dfiPoolTokenToPoolPairDataMap = null;
  private Map<String, DefiPoolPairData> dusdPoolTokenToPoolPairDataMap = null;

  // ...
  private BigDecimal usdtDFIRatioAB = BigDecimal.ZERO;
  private BigDecimal usdtDFICommission = BigDecimal.ZERO;
  private BigDecimal usdtDFIDexFeePctTokenB = BigDecimal.ZERO;

  private BigDecimal usdtDUSDRatioAB = BigDecimal.ZERO;
  private BigDecimal usdtDUSDCommission = BigDecimal.ZERO;
  private BigDecimal usdtDUSDDexFeePctTokenB = BigDecimal.ZERO;

  /**
   * 
   */
  public TransparencyReportPriceHelper() {
  }

  public void setDFIPoolTokenToPoolPairDataMap(@Nonnull Map<String, DefiPoolPairData> dfiPoolTokenToPoolPairDataMap) {
    this.dfiPoolTokenToPoolPairDataMap = dfiPoolTokenToPoolPairDataMap;
  }

  public void setdusdPoolTokenToPoolPairDataMap(@Nonnull Map<String, DefiPoolPairData> dusdPoolTokenToPoolPairDataMap) {
    this.dusdPoolTokenToPoolPairDataMap = dusdPoolTokenToPoolPairDataMap;
  }

  /**
   * 
   */
  public Map<TokenEnum, BigDecimal> createTokenToPriceMap() {
    LOGGER.debug("createTokenToPriceMap()");
    Objects.requireNonNull(dfiPoolTokenToPoolPairDataMap, "dfiPoolTokenToPoolPairDataMap null object is not allowed");
    Objects.requireNonNull(dusdPoolTokenToPoolPairDataMap, "dusdPoolTokenToPoolPairDataMap null object is not allowed");

    // ...
    BigDecimal dfiPrice = setupUSDTDFI();
    BigDecimal dusdPrice = setupUSDTDUSD();

    BigDecimal btcPrice = getPriceViaDFIPools("BTC-DFI");
    BigDecimal ethPrice = getPriceViaDFIPools("ETH-DFI");
    BigDecimal usdtPrice = getPriceViaDFIPools("USDT-DFI");
    BigDecimal usdcPrice = getPriceViaDFIPools("USDC-DFI");

    BigDecimal spyPrice = getPriceViaDUSDPools("SPY-DUSD");

    // ...
    Map<TokenEnum, BigDecimal> tokenToPriceMap = new EnumMap<>(TokenEnum.class);

    tokenToPriceMap.put(TokenEnum.DFI, dfiPrice);
    tokenToPriceMap.put(TokenEnum.DUSD, dusdPrice);
    tokenToPriceMap.put(TokenEnum.BTC, btcPrice);
    tokenToPriceMap.put(TokenEnum.ETH, ethPrice);
    tokenToPriceMap.put(TokenEnum.USDT, usdtPrice);
    tokenToPriceMap.put(TokenEnum.USDC, usdcPrice);
    tokenToPriceMap.put(TokenEnum.SPY, spyPrice);

    return tokenToPriceMap;
  }

  /**
   * 
   */
  private BigDecimal setupUSDTDFI() {
    LOGGER.trace("setupUSDTDFI()");

    DefiPoolPairData usdtDFIPoolPairData = dfiPoolTokenToPoolPairDataMap.get("USDT-DFI");
    BigDecimal usdtDFIReserveA = usdtDFIPoolPairData.getReserveA();
    BigDecimal usdtDFIReserveB = usdtDFIPoolPairData.getReserveB();
    usdtDFIRatioAB = usdtDFIReserveA.divide(usdtDFIReserveB, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    usdtDFICommission = usdtDFIPoolPairData.getCommission();

    if (null == usdtDFICommission) {
      usdtDFICommission = BigDecimal.ZERO;
    }

    usdtDFIDexFeePctTokenB = usdtDFIPoolPairData.getDexFeePctTokenB();

    if (null == usdtDFIDexFeePctTokenB) {
      usdtDFIDexFeePctTokenB = BigDecimal.ZERO;
    }

    BigDecimal dfiPrice = usdtDFIRatioAB;
    dfiPrice = dfiPrice.multiply(BigDecimal.ONE.subtract(usdtDFIDexFeePctTokenB));
    dfiPrice = dfiPrice.multiply(BigDecimal.ONE.subtract(usdtDFICommission));
    dfiPrice = dfiPrice.setScale(SCALE, RoundingMode.HALF_UP);

    return dfiPrice;
  }

  /**
   * 
   */
  private BigDecimal setupUSDTDUSD() {
    LOGGER.trace("setupUSDTDUSD()");

    DefiPoolPairData usdtDUSDPoolPairData = dusdPoolTokenToPoolPairDataMap.get("USDT-DUSD");
    BigDecimal usdtDUSDReserveA = usdtDUSDPoolPairData.getReserveA();
    BigDecimal usdtDUSDReserveB = usdtDUSDPoolPairData.getReserveB();
    usdtDUSDRatioAB = usdtDUSDReserveA.divide(usdtDUSDReserveB, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    usdtDUSDCommission = usdtDUSDPoolPairData.getCommission();

    if (null == usdtDUSDCommission) {
      usdtDUSDCommission = BigDecimal.ZERO;
    }

    usdtDUSDDexFeePctTokenB = usdtDUSDPoolPairData.getDexFeePctTokenB();

    if (null == usdtDUSDDexFeePctTokenB) {
      usdtDUSDDexFeePctTokenB = BigDecimal.ZERO;
    }

    BigDecimal dusdPrice = usdtDUSDRatioAB;
    dusdPrice = dusdPrice.multiply(BigDecimal.ONE.subtract(usdtDUSDDexFeePctTokenB));
    dusdPrice = dusdPrice.multiply(BigDecimal.ONE.subtract(usdtDUSDCommission));
    dusdPrice = dusdPrice.setScale(SCALE, RoundingMode.HALF_UP);

    return dusdPrice;
  }

  /**
   * BTC-DFI, ETH-DFI, USDT-DFI, USDC-DFI
   */
  private BigDecimal getPriceViaDFIPools(@Nonnull String poolToken) {
    LOGGER.trace("getPriceViaDFIPools()");

    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolTokenReserveA = poolPairData.getReserveA();
    BigDecimal poolTokenReserveB = poolPairData.getReserveB();
    BigDecimal poolTokenRatioAB = poolTokenReserveA.divide(poolTokenReserveB, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    BigDecimal commission = poolPairData.getCommission();

    if (null == commission) {
      commission = BigDecimal.ZERO;
    }

    BigDecimal dexFeePctTokenA = poolPairData.getDexFeePctTokenA();

    if (null == dexFeePctTokenA) {
      dexFeePctTokenA = BigDecimal.ZERO;
    }

    BigDecimal price = BigDecimal.ONE.divide(poolTokenRatioAB, MATH_CONTEXT).multiply(usdtDFIRatioAB, MATH_CONTEXT);
    price = price.multiply(BigDecimal.ONE.subtract(dexFeePctTokenA));
    price = price.multiply(BigDecimal.ONE.subtract(commission));
    price = price.multiply(BigDecimal.ONE.subtract(usdtDFICommission));
    price = price.setScale(SCALE, RoundingMode.HALF_UP);

    return price;
  }

  /**
   * SPY-DUSD
   */
  private BigDecimal getPriceViaDUSDPools(@Nonnull String poolToken) {
    LOGGER.trace("getPriceViaDUSDPools()");

    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);

    BigDecimal poolTokenReserveA = poolPairData.getReserveA();
    BigDecimal poolTokenReserveB = poolPairData.getReserveB();
    BigDecimal poolTokenRatioAB = poolTokenReserveA.divide(poolTokenReserveB, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    BigDecimal commission = poolPairData.getCommission();

    if (null == commission) {
      commission = BigDecimal.ZERO;
    }

    BigDecimal dexFeePctTokenA = poolPairData.getDexFeePctTokenA();

    if (null == dexFeePctTokenA) {
      dexFeePctTokenA = BigDecimal.ZERO;
    }

    BigDecimal price = BigDecimal.ONE.divide(poolTokenRatioAB, MATH_CONTEXT).multiply(usdtDFIRatioAB, MATH_CONTEXT);
    price = price.multiply(BigDecimal.ONE.subtract(dexFeePctTokenA));
    price = price.multiply(BigDecimal.ONE.subtract(commission));
    price = price.multiply(BigDecimal.ONE.subtract(usdtDUSDCommission));
    price = price.setScale(SCALE, RoundingMode.HALF_UP);

    return price;
  }
}
