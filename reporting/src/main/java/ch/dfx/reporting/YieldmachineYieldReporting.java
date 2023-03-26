package ch.dfx.reporting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.config.ReportingConfigEnum;
import ch.dfx.defichain.data.pool.DefiPoolPairData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.reporting.data.YieldPoolSheetDTO;
import ch.dfx.reporting.transparency.TransparencyReportCsvHelper;
import ch.dfx.reporting.transparency.TransparencyReportPriceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * Version 3: One customer sheets for all Tokens
 * 3 Vaults and 5 Liquidity Mining Pools ...
 */
public class YieldmachineYieldReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(YieldmachineYieldReporting.class);

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  // ...
  private final DefiDataProvider dataProvider;

  private final TransparencyReportPriceHelper transparencyReportPriceHelper;
  private final TransparencyReportCsvHelper transparencyReportCsvHelper;

  // ...
  private Map<TokenEnum, Map<String, BigDecimal>> lmTokenToAmountMap = null;

  private Map<String, DefiPoolPairData> dfiPoolTokenToPoolPairDataMap = null;
  private Map<String, DefiPoolPairData> dusdPoolTokenToPoolPairDataMap = null;

  /**
   * 
   */
  public YieldmachineYieldReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    this.transparencyReportPriceHelper = new TransparencyReportPriceHelper();
    this.transparencyReportCsvHelper = new TransparencyReportCsvHelper();
  }

  /**
   * 
   */
  public void report(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      setup();

      // ...
      writeReport(reportingTimestamp, rootPath, fileName);
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void setup() throws DfxException {
    LOGGER.debug("setup()");

    setupLiquidMining();
    setupLiquidMiningPoolInfo();
  }

  /**
   * 
   */
  private void setupLiquidMining() throws DfxException {
    LOGGER.trace("setupLiquidMining()");

    if (null == lmTokenToAmountMap) {
      String btcLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_BTC_LM_ADDRESS, "");
      String ethLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_ETH_LM_ADDRESS, "");
      String dusdLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_DUSD_LM_ADDRESS, "");
      String usdtLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_USDT_LM_ADDRESS, "");
      String usdcLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_USDC_LM_ADDRESS, "");
      String spyLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_SPY_LM_ADDRESS, "");

      lmTokenToAmountMap = new EnumMap<>(TokenEnum.class);

      lmTokenToAmountMap.put(TokenEnum.BTC, getTokenToAmountMap(btcLMAddress));
      lmTokenToAmountMap.put(TokenEnum.ETH, getTokenToAmountMap(ethLMAddress));
      lmTokenToAmountMap.put(TokenEnum.DUSD, getTokenToAmountMap(dusdLMAddress));
      lmTokenToAmountMap.put(TokenEnum.USDT, getTokenToAmountMap(usdtLMAddress));
      lmTokenToAmountMap.put(TokenEnum.USDC, getTokenToAmountMap(usdcLMAddress));
      lmTokenToAmountMap.put(TokenEnum.SPY, getTokenToAmountMap(spyLMAddress));
    }
  }

  /**
   * 
   */
  private void setupLiquidMiningPoolInfo() throws DfxException {
    LOGGER.trace("setupLiquidMiningPoolInfo()");

    if (null == dfiPoolTokenToPoolPairDataMap) {
      dfiPoolTokenToPoolPairDataMap = new HashMap<>();

      List<String> dfiPoolTokenList = new ArrayList<>();
      dfiPoolTokenList.add("BTC-DFI");
      dfiPoolTokenList.add("ETH-DFI");
      dfiPoolTokenList.add("DUSD-DFI");
      dfiPoolTokenList.add("USDT-DFI");
      dfiPoolTokenList.add("USDC-DFI");

      for (String dfiPoolToken : dfiPoolTokenList) {
        DefiPoolPairData poolPairData = dataProvider.getPoolPair(dfiPoolToken);
        dfiPoolTokenToPoolPairDataMap.put(dfiPoolToken, poolPairData);
      }
    }

    if (null == dusdPoolTokenToPoolPairDataMap) {
      dusdPoolTokenToPoolPairDataMap = new HashMap<>();

      List<String> dusdPoolTokenList = new ArrayList<>();
      dusdPoolTokenList.add("USDT-DUSD");
      dusdPoolTokenList.add("USDC-DUSD");
      dusdPoolTokenList.add("SPY-DUSD");

      for (String dusdPoolToken : dusdPoolTokenList) {
        DefiPoolPairData poolPairData = dataProvider.getPoolPair(dusdPoolToken);
        dusdPoolTokenToPoolPairDataMap.put(dusdPoolToken, poolPairData);
      }
    }
  }

  /**
   * 
   */
  private Map<String, BigDecimal> getTokenToAmountMap(@Nonnull String address) throws DfxException {
    LOGGER.trace("getTokenToAmountMap(): address=" + address);

    List<String> accountList = dataProvider.getAccount(address);

    return TransactionCheckerUtils.getTokenToAmountMap(accountList);
  }

  /**
   * 
   */
  private PoolTokenPairData getDFIPoolTokenPairData(@Nonnull TokenEnum tokenA) {
    LOGGER.trace("getDFIPoolTokenPairData()");

    TokenEnum tokenB = TokenEnum.DFI;

    String poolToken = tokenA.toString() + "-" + tokenB.toString();
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = lmTokenToAmountMap.get(tokenA).getOrDefault(poolToken, BigDecimal.ZERO);
    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);

    PoolTokenPairData poolTokenPairData = new PoolTokenPairData(poolToken, tokenA, tokenB);
    poolTokenPairData.setPoolTokenAmountPair(poolTokenAmountPair);

    return poolTokenPairData;
  }

  /**
   * 
   */
  private PoolTokenPairData getDUSDPoolTokenPairData(@Nonnull TokenEnum tokenA) {
    LOGGER.trace("getDUSDPoolTokenPairData()");

    TokenEnum tokenB = TokenEnum.DUSD;

    String poolToken = tokenA.toString() + "-" + tokenB.toString();
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = lmTokenToAmountMap.get(tokenA).getOrDefault(poolToken, BigDecimal.ZERO);
    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);

    PoolTokenPairData poolTokenPairData = new PoolTokenPairData(poolToken, tokenA, tokenB);
    poolTokenPairData.setPoolTokenAmountPair(poolTokenAmountPair);

    return poolTokenPairData;
  }

  /**
   * 
   */
  private void writeReport(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName) throws DfxException {
    LOGGER.debug("writeReport()");

    // ...
    Map<TokenEnum, BigDecimal> tokenToPriceMap = transparencyReportPriceHelper.createTokenToPriceMap();

    // ...
    openExcel(rootPath, fileName);

    // ...
    PoolTokenPairData usdtPoolTokenPairData = getDUSDPoolTokenPairData(TokenEnum.USDT);
    YieldPoolSheetDTO usdtYieldPoolSheetDTO = createYieldPoolSheetDTO(reportingTimestamp, usdtPoolTokenPairData, tokenToPriceMap);
    transparencyReportCsvHelper.writeToCSV(usdtYieldPoolSheetDTO);

    setSheet(usdtPoolTokenPairData.getPoolTokenId());
    writeYieldSheet(usdtPoolTokenPairData.getPoolTokenId());

    // ...
    PoolTokenPairData usdcPoolTokenPairData = getDUSDPoolTokenPairData(TokenEnum.USDC);
    YieldPoolSheetDTO usdcYieldPoolSheetDTO = createYieldPoolSheetDTO(reportingTimestamp, usdcPoolTokenPairData, tokenToPriceMap);
    transparencyReportCsvHelper.writeToCSV(usdcYieldPoolSheetDTO);

    setSheet(usdcPoolTokenPairData.getPoolTokenId());
    writeYieldSheet(usdcPoolTokenPairData.getPoolTokenId());

    // ...
    PoolTokenPairData spyPoolTokenPairData = getDUSDPoolTokenPairData(TokenEnum.SPY);
    YieldPoolSheetDTO spyYieldPoolSheetDTO = createYieldPoolSheetDTO(reportingTimestamp, spyPoolTokenPairData, tokenToPriceMap);
    transparencyReportCsvHelper.writeToCSV(spyYieldPoolSheetDTO);

    setSheet(spyPoolTokenPairData.getPoolTokenId());
    writeYieldSheet(spyPoolTokenPairData.getPoolTokenId());

    closeExcel();
  }

  /**
   * 
   */
  private YieldPoolSheetDTO createYieldPoolSheetDTO(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull PoolTokenPairData poolTokenPairData,
      @Nonnull Map<TokenEnum, BigDecimal> tokenToPriceMap) {
    LOGGER.debug("createYieldPoolSheetDTO(): poolTokenId=" + poolTokenPairData.getPoolTokenId());

    // ...
    YieldPoolSheetDTO yieldPoolDTO = new YieldPoolSheetDTO(poolTokenPairData.getPoolTokenId(), reportingTimestamp);

    // ...
    BigDecimal tokenAAmount = poolTokenPairData.getTokenAAmount();
    BigDecimal tokenAPrice =
        tokenAAmount.multiply(tokenToPriceMap.getOrDefault(poolTokenPairData.getTokenA(), BigDecimal.ZERO), MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    yieldPoolDTO.setTokenAAmount(tokenAAmount);
    yieldPoolDTO.setTokenAPrice(tokenAPrice);

    // ...
    BigDecimal tokenBAmount = poolTokenPairData.getTokenBAmount();
    BigDecimal tokenBPrice =
        tokenBAmount.multiply(tokenToPriceMap.getOrDefault(poolTokenPairData.getTokenB(), BigDecimal.ZERO), MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    yieldPoolDTO.setTokenBAmount(tokenBAmount);
    yieldPoolDTO.setTokenBPrice(tokenBPrice);

    // ...
    BigDecimal dfiAmount = lmTokenToAmountMap.get(poolTokenPairData.getTokenA()).getOrDefault(TokenEnum.DFI.toString(), BigDecimal.ZERO);
    BigDecimal dfiPrice =
        dfiAmount.multiply(tokenToPriceMap.getOrDefault(TokenEnum.DFI, BigDecimal.ZERO), MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    yieldPoolDTO.setBalanceAmount(dfiAmount);
    yieldPoolDTO.setBalancePrice(dfiPrice);

    return yieldPoolDTO;
  }

  /**
   * 
   */
  private void writeYieldSheet(@Nonnull String poolTokenId) throws DfxException {
    LOGGER.debug("writeYieldSheet(): poolTokenId=" + poolTokenId);

    RowDataList rowDataList = new RowDataList(1);

    List<YieldPoolSheetDTO> yieldPoolSheetDTOList = transparencyReportCsvHelper.readYieldPoolSheetDTOFromCSV(poolTokenId);

    for (YieldPoolSheetDTO yieldPoolSheetDTO : yieldPoolSheetDTOList) {
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setCellIndex(0).setValue(yieldPoolSheetDTO.getTimestamp()));

      cellDataList.add(new CellData().setCellIndex(1).setValue(yieldPoolSheetDTO.getTokenAAmount()));
      cellDataList.add(new CellData().setCellIndex(2).setValue(yieldPoolSheetDTO.getTokenAPrice()));
      cellDataList.add(new CellData().setCellIndex(3).setValue(yieldPoolSheetDTO.getTokenBAmount()));
      cellDataList.add(new CellData().setCellIndex(4).setValue(yieldPoolSheetDTO.getTokenBPrice()));
      cellDataList.add(new CellData().setCellIndex(5).setValue(yieldPoolSheetDTO.getBalanceAmount()));
      cellDataList.add(new CellData().setCellIndex(6).setValue(yieldPoolSheetDTO.getBalancePrice()));

      cellDataList.add(new CellData().setCellIndex(7).setValue(yieldPoolSheetDTO.getHourDifference()));
      cellDataList.add(new CellData().setCellIndex(8).setValue(yieldPoolSheetDTO.getHourInterval()));
      cellDataList.add(new CellData().setCellIndex(9).setValue(yieldPoolSheetDTO.getHourYield()));

      BigDecimal dayDifference = yieldPoolSheetDTO.getDayDifference();

      if (0 != BigDecimal.ZERO.compareTo(dayDifference)) {
        cellDataList.add(new CellData().setCellIndex(10).setValue(yieldPoolSheetDTO.getDayDifference()));
        cellDataList.add(new CellData().setCellIndex(11).setValue(yieldPoolSheetDTO.getDayInterval()));
        cellDataList.add(new CellData().setCellIndex(12).setValue(yieldPoolSheetDTO.getDayYield()));
      }

      RowData rowData = new RowData().addCellDataList(cellDataList);
      rowDataList.add(rowData);
    }

    cleanExcel(1);
    writeExcel(rowDataList);
  }

  /**
   * 
   */
  private class PoolTokenPairData {
    private final String poolTokenId;

    private final TokenEnum tokenA;
    private final TokenEnum tokenB;

    private Pair<BigDecimal, BigDecimal> poolTokenAmountPair = Pair.of(BigDecimal.ZERO, BigDecimal.ZERO);

    private PoolTokenPairData(
        @Nonnull String poolTokenId,
        @Nonnull TokenEnum tokenA,
        @Nonnull TokenEnum tokenB) {
      this.poolTokenId = poolTokenId;
      this.tokenA = tokenA;
      this.tokenB = tokenB;
    }

    private String getPoolTokenId() {
      return poolTokenId;
    }

    private TokenEnum getTokenA() {
      return tokenA;
    }

    private TokenEnum getTokenB() {
      return tokenB;
    }

    private void setPoolTokenAmountPair(@Nonnull Pair<BigDecimal, BigDecimal> poolTokenAmountPair) {
      this.poolTokenAmountPair = poolTokenAmountPair;
    }

    private BigDecimal getTokenAAmount() {
      return poolTokenAmountPair.getLeft();
    }

    private BigDecimal getTokenBAmount() {
      return poolTokenAmountPair.getRight();
    }
  }
}
