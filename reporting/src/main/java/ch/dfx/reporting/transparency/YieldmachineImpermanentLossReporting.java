package ch.dfx.reporting.transparency;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.config.ReportingConfigEnum;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.graphgl.GraphGLPoolGrabber;
import ch.dfx.graphgl.data.FarmingHistoryPoolDTO;
import ch.dfx.graphgl.data.FarmingHistoryPoolDTOList;
import ch.dfx.ocean.OceanHistoryHandler;
import ch.dfx.ocean.OceanHistoryPoolTransactionGrabber;
import ch.dfx.ocean.data.HistoryDetailDTOList;
import ch.dfx.ocean.data.HistoryPoolDTO;
import ch.dfx.ocean.data.HistoryPoolDTOList;
import ch.dfx.reporting.Reporting;
import ch.dfx.reporting.transparency.data.HistoryImpermanentLossSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryImpermanentLossSheetDTOList;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class YieldmachineImpermanentLossReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(YieldmachineImpermanentLossReporting.class);

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final File ROOT_PATH = Path.of("data", "yieldmachine", "impermanentloss").toFile();

  // ...
  private final DefiDataProvider dataProvider;
  private final OceanHistoryPoolTransactionGrabber oceanHistoryPoolTransactionGrabber;
  private final GraphGLPoolGrabber graphGLPoolGrabber;
  private final TransparencyReportPriceHelper transparencyReportPriceHelper;
  private final TransparencyReportFileHelper transparencyReportFileHelper;

  /**
   * 
   */
  public YieldmachineImpermanentLossReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
    this.oceanHistoryPoolTransactionGrabber = new OceanHistoryPoolTransactionGrabber(ROOT_PATH);
    this.graphGLPoolGrabber = new GraphGLPoolGrabber(ROOT_PATH);
    this.transparencyReportPriceHelper = new TransparencyReportPriceHelper();
    this.transparencyReportFileHelper = new TransparencyReportFileHelper();
  }

  /**
   * 
   */
  public void updateData() throws DfxException {
    // ...
    String liquidityAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_LIQUIDITY_ADDRESS, "");
    String btcPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_BTC_LM_ADDRESS, "");
    String ethPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_ETH_LM_ADDRESS, "");
    String dusdPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_DUSD_LM_ADDRESS, "");
    String usdtPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_USDT_LM_ADDRESS, "");
    String usdcPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_USDC_LM_ADDRESS, "");
    String eurocPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_EUROC_LM_ADDRESS, "");
    String spyPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_SPY_LM_ADDRESS, "");

    OceanHistoryHandler oceanHistoryHandler = new OceanHistoryHandler(network, ROOT_PATH);
    OceanHistoryPoolTransactionGrabber oceanHistoryPoolTransactionGrabber = new OceanHistoryPoolTransactionGrabber(ROOT_PATH);

    // ...
//    HistoryDetailDTOList liquidityHistoryDetailDTOList = oceanHistoryHandler.writeToJSON(liquidityAddress);
//    HistoryDetailDTOList btcPoolHistoryDetailDTOList = oceanHistoryHandler.writeToJSON(btcPoolAddress);
//    HistoryDetailDTOList ethPoolHistoryDetailDTOList = oceanHistoryHandler.writeToJSON(ethPoolAddress);
//    HistoryDetailDTOList dusdPoolHistoryDetailDTOList = oceanHistoryHandler.writeToJSON(dusdPoolAddress);
//    HistoryDetailDTOList usdtPoolHistoryDetailDTOList = oceanHistoryHandler.writeToJSON(usdtPoolAddress);
//    HistoryDetailDTOList usdcPoolHistoryDetailDTOList = oceanHistoryHandler.writeToJSON(usdcPoolAddress);
//    HistoryDetailDTOList eurocPoolHistoryDetailDTOList = oceanHistoryHandler.writeToJSON(eurocPoolAddress);
//    HistoryDetailDTOList spyPoolHistoryDetailDTOList = oceanHistoryHandler.writeToJSON(spyPoolAddress);

    // ...
    HistoryDetailDTOList liquidityHistoryDetailDTOList = oceanHistoryHandler.readFromJSON(liquidityAddress);
    HistoryDetailDTOList btcPoolHistoryDetailDTOList = oceanHistoryHandler.readFromJSON(btcPoolAddress);
    HistoryDetailDTOList ethPoolHistoryDetailDTOList = oceanHistoryHandler.readFromJSON(ethPoolAddress);
    HistoryDetailDTOList dusdPoolHistoryDetailDTOList = oceanHistoryHandler.readFromJSON(dusdPoolAddress);
    HistoryDetailDTOList usdtPoolHistoryDetailDTOList = oceanHistoryHandler.readFromJSON(usdtPoolAddress);
    HistoryDetailDTOList usdcPoolHistoryDetailDTOList = oceanHistoryHandler.readFromJSON(usdcPoolAddress);
    HistoryDetailDTOList eurocPoolHistoryDetailDTOList = oceanHistoryHandler.readFromJSON(eurocPoolAddress);
    HistoryDetailDTOList spyPoolHistoryDetailDTOList = oceanHistoryHandler.readFromJSON(spyPoolAddress);

    oceanHistoryPoolTransactionGrabber
        .grabPoolTransaction(liquidityHistoryDetailDTOList, btcPoolHistoryDetailDTOList, btcPoolAddress, TokenEnum.BTC, TokenEnum.DFI);

    oceanHistoryPoolTransactionGrabber
        .grabPoolTransaction(liquidityHistoryDetailDTOList, ethPoolHistoryDetailDTOList, ethPoolAddress, TokenEnum.ETH, TokenEnum.DFI);

    oceanHistoryPoolTransactionGrabber
        .grabPoolTransaction(liquidityHistoryDetailDTOList, dusdPoolHistoryDetailDTOList, dusdPoolAddress, TokenEnum.DUSD, TokenEnum.DFI);

    oceanHistoryPoolTransactionGrabber
        .grabPoolTransaction(liquidityHistoryDetailDTOList, usdtPoolHistoryDetailDTOList, usdtPoolAddress, TokenEnum.USDT, TokenEnum.DUSD);

    oceanHistoryPoolTransactionGrabber
        .grabPoolTransaction(liquidityHistoryDetailDTOList, usdcPoolHistoryDetailDTOList, usdcPoolAddress, TokenEnum.USDC, TokenEnum.DUSD);

    oceanHistoryPoolTransactionGrabber
        .grabPoolTransaction(liquidityHistoryDetailDTOList, eurocPoolHistoryDetailDTOList, eurocPoolAddress, TokenEnum.EUROC, TokenEnum.DUSD);

    oceanHistoryPoolTransactionGrabber
        .grabPoolTransaction(liquidityHistoryDetailDTOList, spyPoolHistoryDetailDTOList, spyPoolAddress, TokenEnum.SPY, TokenEnum.DUSD);

    // ...
    graphGLPoolGrabber.grabPoolTransaction();
  }

  /**
   * 
   */
  public void report(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull Map<TokenEnum, String> tokenToPoolSheetNameMap) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      HistoryImpermanentLossSheetDTOList btcHistoryImpermanentLossDTOList = createBTCHistoryImpermanentLossDTOList();
      transparencyReportFileHelper.writeHistoryImpermanentLossSheetDTOToJSON(TokenEnum.BTC, btcHistoryImpermanentLossDTOList);

      HistoryImpermanentLossSheetDTOList ethHistoryImpermanentLossDTOList = createETHHistoryImpermanentLossDTOList();
      transparencyReportFileHelper.writeHistoryImpermanentLossSheetDTOToJSON(TokenEnum.ETH, ethHistoryImpermanentLossDTOList);

      HistoryImpermanentLossSheetDTOList dusdHistoryImpermanentLossDTOList = createDUSDHistoryImpermanentLossDTOList();
      transparencyReportFileHelper.writeHistoryImpermanentLossSheetDTOToJSON(TokenEnum.DUSD, dusdHistoryImpermanentLossDTOList);

      HistoryImpermanentLossSheetDTOList usdtHistoryImpermanentLossDTOList = createUSDTHistoryImpermanentLossDTOList();
      transparencyReportFileHelper.writeHistoryImpermanentLossSheetDTOToJSON(TokenEnum.USDT, usdtHistoryImpermanentLossDTOList);

      HistoryImpermanentLossSheetDTOList usdcHistoryImpermanentLossDTOList = createUSDCHistoryImpermanentLossDTOList();
      transparencyReportFileHelper.writeHistoryImpermanentLossSheetDTOToJSON(TokenEnum.USDC, usdcHistoryImpermanentLossDTOList);

      HistoryImpermanentLossSheetDTOList eurocHistoryImpermanentLossDTOList = createEUROCHistoryImpermanentLossDTOList();
      transparencyReportFileHelper.writeHistoryImpermanentLossSheetDTOToJSON(TokenEnum.EUROC, eurocHistoryImpermanentLossDTOList);

      HistoryImpermanentLossSheetDTOList spyHistoryImpermanentLossDTOList = createSPYHistoryImpermanentLossDTOList();
      transparencyReportFileHelper.writeHistoryImpermanentLossSheetDTOToJSON(TokenEnum.SPY, spyHistoryImpermanentLossDTOList);

      // ...
      writeReport(reportingTimestamp, rootPath, fileName, tokenToPoolSheetNameMap);

    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTOList createBTCHistoryImpermanentLossDTOList() throws DfxException {
    LOGGER.trace("createBTCHistoryImpermanentLossDTOList()");

    // ...
    String btcPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_BTC_LM_ADDRESS, "");

    TokenEnum tokenA = TokenEnum.BTC;
    TokenEnum tokenB = TokenEnum.DFI;

    HistoryPoolDTOList historyPoolDTOList = oceanHistoryPoolTransactionGrabber.readTotalHistoryPoolDTOList(btcPoolAddress, tokenA, tokenB);
    FarmingHistoryPoolDTOList btcFarmingHistoryPoolDTOList = graphGLPoolGrabber.readBTCFarmingHistoryPoolDTOList();

    return createHistoryImpermanentLossSheetDTOList(historyPoolDTOList, btcFarmingHistoryPoolDTOList);
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTOList createETHHistoryImpermanentLossDTOList() throws DfxException {
    LOGGER.trace("createETHHistoryImpermanentLossDTOList()");

    // ...
    String ethPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_ETH_LM_ADDRESS, "");

    TokenEnum tokenA = TokenEnum.ETH;
    TokenEnum tokenB = TokenEnum.DFI;

    HistoryPoolDTOList historyPoolDTOList = oceanHistoryPoolTransactionGrabber.readTotalHistoryPoolDTOList(ethPoolAddress, tokenA, tokenB);
    FarmingHistoryPoolDTOList ethFarmingHistoryPoolDTOList = graphGLPoolGrabber.readETHFarmingHistoryPoolDTOList();

    return createHistoryImpermanentLossSheetDTOList(historyPoolDTOList, ethFarmingHistoryPoolDTOList);
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTOList createDUSDHistoryImpermanentLossDTOList() throws DfxException {
    LOGGER.trace("createDUSDHistoryImpermanentLossDTOList()");

    // ...
    String dusdPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_DUSD_LM_ADDRESS, "");

    TokenEnum tokenA = TokenEnum.DUSD;
    TokenEnum tokenB = TokenEnum.DFI;

    HistoryPoolDTOList historyPoolDTOList = oceanHistoryPoolTransactionGrabber.readTotalHistoryPoolDTOList(dusdPoolAddress, tokenA, tokenB);
    FarmingHistoryPoolDTOList dusdFarmingHistoryPoolDTOList = graphGLPoolGrabber.readDUSDFarmingHistoryPoolDTOList();

    return createHistoryImpermanentLossSheetDTOList(historyPoolDTOList, dusdFarmingHistoryPoolDTOList);
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTOList createUSDTHistoryImpermanentLossDTOList() throws DfxException {
    LOGGER.trace("createUSDTHistoryImpermanentLossDTOList()");

    // ...
    String usdtPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_USDT_LM_ADDRESS, "");

    TokenEnum tokenA = TokenEnum.USDT;
    TokenEnum tokenB = TokenEnum.DUSD;

    HistoryPoolDTOList historyPoolDTOList = oceanHistoryPoolTransactionGrabber.readTotalHistoryPoolDTOList(usdtPoolAddress, tokenA, tokenB);
    FarmingHistoryPoolDTOList usdtFarmingHistoryPoolDTOList = graphGLPoolGrabber.readUSDTFarmingHistoryPoolDTOList();

    return createHistoryImpermanentLossSheetDTOList(historyPoolDTOList, usdtFarmingHistoryPoolDTOList);
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTOList createUSDCHistoryImpermanentLossDTOList() throws DfxException {
    LOGGER.trace("createUSDCHistoryImpermanentLossDTOList()");

    // ...
    String usdcPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_USDC_LM_ADDRESS, "");

    TokenEnum tokenA = TokenEnum.USDC;
    TokenEnum tokenB = TokenEnum.DUSD;

    HistoryPoolDTOList historyPoolDTOList = oceanHistoryPoolTransactionGrabber.readTotalHistoryPoolDTOList(usdcPoolAddress, tokenA, tokenB);
    FarmingHistoryPoolDTOList usdcFarmingHistoryPoolDTOList = graphGLPoolGrabber.readUSDCFarmingHistoryPoolDTOList();

    return createHistoryImpermanentLossSheetDTOList(historyPoolDTOList, usdcFarmingHistoryPoolDTOList);
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTOList createEUROCHistoryImpermanentLossDTOList() throws DfxException {
    LOGGER.trace("createEUROCHistoryImpermanentLossDTOList()");

    // ...
    String eurocPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_EUROC_LM_ADDRESS, "");

    TokenEnum tokenA = TokenEnum.EUROC;
    TokenEnum tokenB = TokenEnum.DUSD;

    HistoryPoolDTOList historyPoolDTOList = oceanHistoryPoolTransactionGrabber.readTotalHistoryPoolDTOList(eurocPoolAddress, tokenA, tokenB);
    FarmingHistoryPoolDTOList eurocFarmingHistoryPoolDTOList = graphGLPoolGrabber.readEUROCFarmingHistoryPoolDTOList();

    return createHistoryImpermanentLossSheetDTOList(historyPoolDTOList, eurocFarmingHistoryPoolDTOList);
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTOList createSPYHistoryImpermanentLossDTOList() throws DfxException {
    LOGGER.trace("createSPYHistoryImpermanentLossDTOList()");

    // ...
    String spyPoolAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_SPY_LM_ADDRESS, "");

    TokenEnum tokenA = TokenEnum.SPY;
    TokenEnum tokenB = TokenEnum.DUSD;

    HistoryPoolDTOList historyPoolDTOList = oceanHistoryPoolTransactionGrabber.readTotalHistoryPoolDTOList(spyPoolAddress, tokenA, tokenB);
    FarmingHistoryPoolDTOList spyFarmingHistoryPoolDTOList = graphGLPoolGrabber.readSPYFarmingHistoryPoolDTOList();

    return createHistoryImpermanentLossSheetDTOList(historyPoolDTOList, spyFarmingHistoryPoolDTOList);
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTOList createHistoryImpermanentLossSheetDTOList(
      @Nonnull HistoryPoolDTOList historyPoolDTOList,
      @Nonnull FarmingHistoryPoolDTOList farmingHistoryPoolDTOList) throws DfxException {
    LOGGER.trace("createHistoryImpermanentLossSheetDTOList()");

    HistoryImpermanentLossSheetDTOList historyImpermanentLossSheetDTOList = new HistoryImpermanentLossSheetDTOList();

    // ...
    for (FarmingHistoryPoolDTO farmingHistoryPoolDTO : farmingHistoryPoolDTOList) {
      Timestamp farmingTimestamp = new Timestamp(Instant.parse(farmingHistoryPoolDTO.getDate()).toEpochMilli());

      HistoryPoolDTO totalHistoryPoolDTO = new HistoryPoolDTO();
      totalHistoryPoolDTO.setTimestamp(DATE_FORMAT.format(farmingTimestamp));

      for (HistoryPoolDTO historyPoolDTO : historyPoolDTOList) {
        Timestamp historyTimestamp = Timestamp.valueOf(historyPoolDTO.getTimestamp());

        if (historyTimestamp.before(farmingTimestamp)) {
          totalHistoryPoolDTO.addPoolTokenA(historyPoolDTO.getPoolTokenA());
          totalHistoryPoolDTO.addPoolTokenB(historyPoolDTO.getPoolTokenB());
          totalHistoryPoolDTO.addPoolToken(historyPoolDTO.getPoolToken());
        }
      }

      if (0 != totalHistoryPoolDTO.getPoolToken().compareTo(BigDecimal.ZERO)) {
        HistoryImpermanentLossSheetDTO historyImpermanentLossDTO = createHistoryImpermanentLossDTO(totalHistoryPoolDTO, farmingHistoryPoolDTO);
        historyImpermanentLossSheetDTOList.add(historyImpermanentLossDTO);
      }
    }

    return historyImpermanentLossSheetDTOList;
  }

  /**
   * 
   */
  private HistoryImpermanentLossSheetDTO createHistoryImpermanentLossDTO(
      @Nonnull HistoryPoolDTO historyPoolDTO,
      @Nonnull FarmingHistoryPoolDTO btcFarmingHistoryPoolDTO) throws DfxException {
    LOGGER.trace("createHistoryImpermanentLossDTO()");

    BigDecimal poolTokenAReserve = btcFarmingHistoryPoolDTO.getReserveA();
    BigDecimal poolTokenBReserve = btcFarmingHistoryPoolDTO.getReserveB();
    BigDecimal poolTotalLiquidity = btcFarmingHistoryPoolDTO.getTotalLiquidity();
    BigDecimal poolTokenRatio = poolTokenBReserve.divide(poolTokenAReserve, MATH_CONTEXT);

    BigDecimal poolTokenAAmount = historyPoolDTO.getPoolTokenA().abs();
    BigDecimal poolTokenBAmount = historyPoolDTO.getPoolTokenB().abs();
    BigDecimal poolTokenAmount = historyPoolDTO.getPoolToken().abs();

    BigDecimal calcTokenAAmount = poolTokenAReserve.divide(poolTotalLiquidity, MATH_CONTEXT).multiply(poolTokenAmount);
    BigDecimal calcTokenBAmount = poolTokenBReserve.divide(poolTotalLiquidity, MATH_CONTEXT).multiply(poolTokenAmount);

    BigDecimal tokenABalance = calcTokenAAmount.subtract(poolTokenAAmount);
    BigDecimal tokenBBalance = calcTokenBAmount.subtract(poolTokenBAmount);

    BigDecimal tokenBInA = tokenBBalance.divide(poolTokenRatio, MATH_CONTEXT);
    BigDecimal total = tokenABalance.add(tokenBInA);

    BigDecimal impermanentLoss = btcFarmingHistoryPoolDTO.getPriceA().multiply(total);

    // ...
    HistoryImpermanentLossSheetDTO historyImpermanentLossDTO = new HistoryImpermanentLossSheetDTO();
    historyImpermanentLossDTO.setTimestamp(historyPoolDTO.getTimestamp());

    historyImpermanentLossDTO.setPoolTokenAAmount(poolTokenAAmount.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setPoolTokenBAmount(poolTokenBAmount.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setPoolTokenAmount(poolTokenAmount.setScale(SCALE, RoundingMode.HALF_UP));

    historyImpermanentLossDTO.setPoolTotalLiquidity(poolTotalLiquidity.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setPoolTokenAReserve(poolTokenAReserve.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setPoolTokenBReserve(poolTokenBReserve.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setPoolTokenRatio(poolTokenRatio.setScale(SCALE, RoundingMode.HALF_UP));

    historyImpermanentLossDTO.setCalcTokenAAmount(calcTokenAAmount.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setCalcTokenBAmount(calcTokenBAmount.setScale(SCALE, RoundingMode.HALF_UP));

    historyImpermanentLossDTO.setTokenABalance(tokenABalance.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setTokenBBalance(tokenBBalance.setScale(SCALE, RoundingMode.HALF_UP));

    historyImpermanentLossDTO.setTokenBInA(tokenBInA.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setTotal(total.setScale(SCALE, RoundingMode.HALF_UP));
    historyImpermanentLossDTO.setTokenAPrice(btcFarmingHistoryPoolDTO.getPriceA().setScale(SCALE, RoundingMode.HALF_UP));

    historyImpermanentLossDTO.setImpermanentLoss(impermanentLoss.setScale(SCALE, RoundingMode.HALF_UP));

    return historyImpermanentLossDTO;
  }

//  /**
//   * 
//   */
//  private HistoryImpermanentLossSheetDTO createHistoryImpermanentLossDTO(
//      @Nonnull HistoryPoolDTO historyPoolDTO,
//      @Nonnull TokenEnum tokenA,
//      @Nonnull TokenEnum tokenB) throws DfxException {
//    LOGGER.trace("createHistoryImpermanentLossDTO()");
//
//    Map<TokenEnum, BigDecimal> tokenToPriceMap = transparencyReportPriceHelper.createTokenToPriceMap();
//    BigDecimal tokenAPrice = tokenToPriceMap.get(tokenA);
//
//    String poolName = tokenA.toString() + "-" + tokenB.toString();
//    DefiPoolPairData poolPairData = dataProvider.getPoolPair(poolName);
//
//    BigDecimal poolTokenAReserve = poolPairData.getReserveA();
//    BigDecimal poolTokenBReserve = poolPairData.getReserveB();
//    BigDecimal poolTotalLiquidity = poolPairData.getTotalLiquidity();
//    BigDecimal poolTokenRatio = poolTokenBReserve.divide(poolTokenAReserve, MATH_CONTEXT);
//
//    BigDecimal poolTokenAAmount = historyPoolDTO.getPoolTokenA().abs();
//    BigDecimal poolTokenBAmount = historyPoolDTO.getPoolTokenB().abs();
//    BigDecimal poolTokenAmount = historyPoolDTO.getPoolToken().abs();
//
//    BigDecimal calcTokenAAmount = poolTokenAReserve.divide(poolTotalLiquidity, MATH_CONTEXT).multiply(poolTokenAmount);
//    BigDecimal calcTokenBAmount = poolTokenBReserve.divide(poolTotalLiquidity, MATH_CONTEXT).multiply(poolTokenAmount);
//
//    BigDecimal tokenABalance = calcTokenAAmount.subtract(poolTokenAAmount);
//    BigDecimal tokenBBalance = calcTokenBAmount.subtract(poolTokenBAmount);
//
//    BigDecimal tokenBInA = tokenBBalance.divide(poolTokenRatio, MATH_CONTEXT);
//    BigDecimal total = tokenABalance.add(tokenBInA);
//
//    BigDecimal impermanentLoss = tokenAPrice.multiply(total);
//
//    // ...
//    HistoryImpermanentLossSheetDTO historyImpermanentLossDTO = new HistoryImpermanentLossSheetDTO();
//
//    historyImpermanentLossDTO.setPoolTokenAAmount(poolTokenAAmount.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setPoolTokenBAmount(poolTokenBAmount.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setPoolTokenAmount(poolTokenAmount.setScale(SCALE, RoundingMode.HALF_UP));
//
//    historyImpermanentLossDTO.setPoolTotalLiquidity(poolTotalLiquidity.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setPoolTokenAReserve(poolTokenAReserve.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setPoolTokenBReserve(poolTokenBReserve.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setPoolTokenRatio(poolTokenRatio.setScale(SCALE, RoundingMode.HALF_UP));
//
//    historyImpermanentLossDTO.setCalcTokenAAmount(calcTokenAAmount.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setCalcTokenBAmount(calcTokenBAmount.setScale(SCALE, RoundingMode.HALF_UP));
//
//    historyImpermanentLossDTO.setTokenABalance(tokenABalance.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setTokenBBalance(tokenBBalance.setScale(SCALE, RoundingMode.HALF_UP));
//
//    historyImpermanentLossDTO.setTokenBInA(tokenBInA.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setTotal(total.setScale(SCALE, RoundingMode.HALF_UP));
//    historyImpermanentLossDTO.setTokenAPrice(tokenAPrice.setScale(SCALE, RoundingMode.HALF_UP));
//
//    historyImpermanentLossDTO.setImpermanentLoss(impermanentLoss.setScale(SCALE, RoundingMode.HALF_UP));
//
//    return historyImpermanentLossDTO;
//  }

  /**
   * 
   */
  private void writeReport(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull Map<TokenEnum, String> tokenToPoolSheetNameMap) throws DfxException {
    LOGGER.debug("writeReport()");

    openExcel(rootPath, fileName);

    writePoolSheet(reportingTimestamp, TokenEnum.BTC, tokenToPoolSheetNameMap);
    writePoolSheet(reportingTimestamp, TokenEnum.ETH, tokenToPoolSheetNameMap);
    writePoolSheet(reportingTimestamp, TokenEnum.DUSD, tokenToPoolSheetNameMap);
    writePoolSheet(reportingTimestamp, TokenEnum.USDT, tokenToPoolSheetNameMap);
    writePoolSheet(reportingTimestamp, TokenEnum.USDC, tokenToPoolSheetNameMap);
    writePoolSheet(reportingTimestamp, TokenEnum.EUROC, tokenToPoolSheetNameMap);
    writePoolSheet(reportingTimestamp, TokenEnum.SPY, tokenToPoolSheetNameMap);

    closeExcel();
  }

  /**
   * 
   */
  private void writePoolSheet(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, String> tokenToPoolSheetNameMap) throws DfxException {
    LOGGER.trace("writePoolSheet()");

    RowDataList rowDataList = new RowDataList(1);

    HistoryImpermanentLossSheetDTOList historyImpermanentLossSheetDTOList =
        transparencyReportFileHelper.readHistoryImpermanentLossSheetDTOFromJSON(token);

    for (HistoryImpermanentLossSheetDTO historyImpermanentLossSheetDTO : historyImpermanentLossSheetDTOList) {
      rowDataList.add(createRowData(historyImpermanentLossSheetDTO));
    }

    String sheetName = tokenToPoolSheetNameMap.get(token);

    setSheet(sheetName);
    cleanExcel(1);
    writeExcel(rowDataList);
  }

  /**
   * 
   */
  private RowData createRowData(@Nonnull HistoryImpermanentLossSheetDTO historyImpermanentLossSheetDTO) {
    LOGGER.trace("createRowData()");

    RowData rowData = new RowData();

    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getTimestamp()));

    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getPoolTokenAAmount()));
    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getPoolTokenBAmount()));
    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getPoolTokenAmount()));

    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getPoolTotalLiquidity()));
    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getPoolTokenAReserve()));
    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getPoolTokenBReserve()));

    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getPoolTokenRatio()));

    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getCalcTokenAAmount()));
    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getCalcTokenBAmount()));

    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getTokenABalance()));
    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getTokenBBalance()));

    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getTokenBInA()));
    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getTotal()));
    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getTokenAPrice()));

    rowData.addCellData(new CellData().setValue(historyImpermanentLossSheetDTO.getImpermanentLoss()));

    return rowData;
  }
}
