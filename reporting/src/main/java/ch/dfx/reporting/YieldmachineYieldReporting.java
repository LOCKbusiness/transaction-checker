package ch.dfx.reporting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
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

  private Map<String, BigDecimal> dusdLMTokenToAmountMap = null;
  private Map<String, BigDecimal> btcLMTokenToAmountMap = null;
  private Map<String, BigDecimal> ethLMTokenToAmountMap = null;
  private Map<String, BigDecimal> usdtLMTokenToAmountMap = null;
  private Map<String, BigDecimal> usdcLMTokenToAmountMap = null;
  private Map<String, BigDecimal> spyLMTokenToAmountMap = null;

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
      @Nonnull String fileName,
      @Nonnull String sheetName) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      setup();

      // ...
      writeReport(reportingTimestamp, rootPath, fileName, sheetName);
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

    if (null == btcLMTokenToAmountMap) {
      String btcLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_BTC_LM_ADDRESS, "");
      btcLMTokenToAmountMap = getTokenToAmountMap(btcLMAddress);
    }

    if (null == ethLMTokenToAmountMap) {
      String ethLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_ETH_LM_ADDRESS, "");
      ethLMTokenToAmountMap = getTokenToAmountMap(ethLMAddress);
    }

    if (null == dusdLMTokenToAmountMap) {
      String dusdLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_DUSD_LM_ADDRESS, "");
      dusdLMTokenToAmountMap = getTokenToAmountMap(dusdLMAddress);
    }

    if (null == usdtLMTokenToAmountMap) {
      String usdtLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_USDT_LM_ADDRESS, "");
      usdtLMTokenToAmountMap = getTokenToAmountMap(usdtLMAddress);
    }

    if (null == usdcLMTokenToAmountMap) {
      String usdcLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_USDC_LM_ADDRESS, "");
      usdcLMTokenToAmountMap = getTokenToAmountMap(usdcLMAddress);
    }

    if (null == spyLMTokenToAmountMap) {
      String spyLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_SPY_LM_ADDRESS, "");
      spyLMTokenToAmountMap = getTokenToAmountMap(spyLMAddress);
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

    transparencyReportPriceHelper.setDFIPoolTokenToPoolPairDataMap(dfiPoolTokenToPoolPairDataMap);
    transparencyReportPriceHelper.setdusdPoolTokenToPoolPairDataMap(dusdPoolTokenToPoolPairDataMap);
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
  private Pair<BigDecimal, BigDecimal> getBTCPoolAmount() {
    LOGGER.trace("getBTCPoolAmount()");

    String poolToken = "BTC-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = btcLMTokenToAmountMap.get(poolToken);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getETHPoolAmount() {
    LOGGER.trace("getETHPoolAmount()");

    String poolToken = "ETH-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = ethLMTokenToAmountMap.get(poolToken);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getDUSDPoolAmount() {
    LOGGER.trace("getDUSDPoolAmount()");

    String poolToken = "DUSD-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = dusdLMTokenToAmountMap.get(poolToken);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getUSDTPoolAmount() {
    LOGGER.trace("getUSDTPoolAmount()");

    String poolToken = "USDT-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = usdtLMTokenToAmountMap.get(poolToken);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getUSDCPoolAmount() {
    LOGGER.trace("getUSDCPoolAmount()");

    String poolToken = "USDC-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = usdcLMTokenToAmountMap.get(poolToken);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getSPYPoolAmount() {
    LOGGER.trace("getSPYPoolAmount()");

    String poolToken = "SPY-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = spyLMTokenToAmountMap.get(poolToken);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private void writeReport(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheetName) throws DfxException {
    LOGGER.debug("writeReport()");

    // ...
    YieldPoolSheetDTO yieldPoolSheetDTO = createYieldPoolSheetDTO(reportingTimestamp);
    transparencyReportCsvHelper.writeToCSV(yieldPoolSheetDTO);

    // ...
    openExcel(rootPath, fileName, sheetName);
    writeYieldSheet();
    closeExcel();
  }

  /**
   * 
   */
  private YieldPoolSheetDTO createYieldPoolSheetDTO(@Nonnull Timestamp reportingTimestamp) {
    LOGGER.debug("createYieldPoolSheetDTO()");

    // ...
    Map<TokenEnum, BigDecimal> tokenToPriceMap = transparencyReportPriceHelper.createTokenToPriceMap();

    // ...
    Pair<BigDecimal, BigDecimal> usdtPoolAmountPair = getUSDTPoolAmount();
    BigDecimal usdtLMDFIAmount = usdtLMTokenToAmountMap.getOrDefault(TokenEnum.DFI.toString(), BigDecimal.ZERO);

    YieldPoolSheetDTO yieldPoolDTO = new YieldPoolSheetDTO("USDT-DUSD", reportingTimestamp);

    // ...
    BigDecimal usdtAmount = usdtPoolAmountPair.getLeft();
    BigDecimal usdtPrice =
        usdtAmount.multiply(tokenToPriceMap.getOrDefault(TokenEnum.USDT, BigDecimal.ZERO), MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    yieldPoolDTO.setTokenAAmount(usdtAmount);
    yieldPoolDTO.setTokenAPrice(usdtPrice);

    // ...
    BigDecimal dusdAmount = usdtPoolAmountPair.getRight();
    BigDecimal dusdPrice =
        dusdAmount.multiply(tokenToPriceMap.getOrDefault(TokenEnum.DUSD, BigDecimal.ZERO), MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    yieldPoolDTO.setTokenBAmount(dusdAmount);
    yieldPoolDTO.setTokenBPrice(dusdPrice);

    // ...
    BigDecimal dfiAmount = usdtLMDFIAmount;
    BigDecimal dfiPrice =
        dfiAmount.multiply(tokenToPriceMap.getOrDefault(TokenEnum.DFI, BigDecimal.ZERO), MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    yieldPoolDTO.setBalanceAmount(dfiAmount);
    yieldPoolDTO.setBalancePrice(dfiPrice);

    return yieldPoolDTO;
  }

  /**
   * 
   */
  private void writeYieldSheet() throws DfxException {
    LOGGER.debug("writeYieldSheet()");

    RowDataList rowDataList = new RowDataList(1);

    List<YieldPoolSheetDTO> yieldPoolSheetDTOList = transparencyReportCsvHelper.readYieldPoolSheetDTOFromCSV();

    for (YieldPoolSheetDTO yieldPoolSheetDTO : yieldPoolSheetDTOList) {
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setCellIndex(0).setValue(yieldPoolSheetDTO.getTimestamp()));

      cellDataList.add(new CellData().setCellIndex(1).setValue(yieldPoolSheetDTO.getTokenAAmount()));
      cellDataList.add(new CellData().setCellIndex(2).setValue(yieldPoolSheetDTO.getTokenAPrice()));
      cellDataList.add(new CellData().setCellIndex(3).setValue(yieldPoolSheetDTO.getTokenBAmount()));
      cellDataList.add(new CellData().setCellIndex(4).setValue(yieldPoolSheetDTO.getTokenBPrice()));
      cellDataList.add(new CellData().setCellIndex(5).setValue(yieldPoolSheetDTO.getBalanceAmount()));
      cellDataList.add(new CellData().setCellIndex(6).setValue(yieldPoolSheetDTO.getBalancePrice()));

      cellDataList.add(new CellData().setCellIndex(7).setValue(yieldPoolSheetDTO.getDifference()));
      cellDataList.add(new CellData().setCellIndex(8).setValue(yieldPoolSheetDTO.getInterval()));
      cellDataList.add(new CellData().setCellIndex(9).setValue(yieldPoolSheetDTO.getYield()));

      RowData rowData = new RowData().addCellDataList(cellDataList);
      rowDataList.add(rowData);
    }

    cleanExcel(1);
    writeExcel(rowDataList);
  }
}
