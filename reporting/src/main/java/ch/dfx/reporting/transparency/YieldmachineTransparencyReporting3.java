package ch.dfx.reporting.transparency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.logging.notifier.TelegramNotifier;
import ch.dfx.config.ReportingConfigEnum;
import ch.dfx.defichain.data.pool.DefiPoolPairData;
import ch.dfx.defichain.data.vault.DefiVaultData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.reporting.Reporting;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * Version 3: One customer sheets for all Tokens
 * 3 Vaults and 5 Liquidity Mining Pools ...
 */
public class YieldmachineTransparencyReporting3 extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(YieldmachineTransparencyReporting3.class);

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  private static final BigDecimal SIGN_CHANGER = new BigDecimal(-1);

  private static final String TOTAL_DIFFERENCE_PROPERTY = "TOTAL_DIFFERENCE";
  private static final String TOTAL_DFI_BALANCE_PROPERTY = "TOTAL_DFI_BALANCE";
  private static final String TOTAL_BTC_BALANCE_PROPERTY = "TOTAL_BTC_BALANCE";
  private static final String TOTAL_ETH_BALANCE_PROPERTY = "TOTAL_ETH_BALANCE";
  private static final String TOTAL_USDT_BALANCE_PROPERTY = "TOTAL_USDT_BALANCE";
  private static final String TOTAL_USDC_BALANCE_PROPERTY = "TOTAL_USDC_BALANCE";

  // ...
  private final DefiDataProvider dataProvider;

  // ...
  private Date currentDate = null;

  private Map<String, BigDecimal> liquidityTokenToAmountMap = null;
  private Map<String, BigDecimal> rewardTokenToAmountMap = null;

  private Map<String, BigDecimal> vault1TokenToAmountMap = null;
  private Map<String, BigDecimal> vault1TokenToVaultCollateralMap = null;

  private Map<String, BigDecimal> vault2TokenToAmountMap = null;
  private Map<String, BigDecimal> vault2TokenToVaultCollateralMap = null;

  private Map<String, BigDecimal> vault3TokenToAmountMap = null;
  private Map<String, BigDecimal> vault3TokenToVaultCollateralMap = null;

  private Map<String, BigDecimal> btcLMTokenToAmountMap = null;
  private Map<String, BigDecimal> ethLMTokenToAmountMap = null;
  private Map<String, BigDecimal> dusdLMTokenToAmountMap = null;
  private Map<String, BigDecimal> usdtLMTokenToAmountMap = null;
  private Map<String, BigDecimal> usdcLMTokenToAmountMap = null;

  private Map<String, DefiPoolPairData> dfiPoolTokenToPoolPairDataMap = null;
  private Map<String, DefiPoolPairData> dusdPoolTokenToPoolPairDataMap = null;

  private Map<String, BigDecimal> activePriceMap = null;

  /**
   * 
   */
  public YieldmachineTransparencyReporting3(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void report(
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String transparencyReportDFITotalSheet,
      @Nonnull String transparencyReportBTCTotalSheet,
      @Nonnull String transparencyReportETHTotalSheet,
      @Nonnull String transparencyReportUSDTTotalSheet,
      @Nonnull String transparencyReportUSDCTotalSheet,
      @Nonnull String transparencyReportTransactionSheet,
      @Nonnull String transparencyReportCustomerSheet) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      setup();

      // ...
      Map<TokenEnum, ReportData> tokenToReportDataMap = new EnumMap<>(TokenEnum.class);

      Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap = getDepositAddressToStakingDTOMap();

      tokenToReportDataMap
          .put(TokenEnum.DFI, createReportData(TokenEnum.DFI, transparencyReportDFITotalSheet, depositAddressToStakingDTOMap));
      tokenToReportDataMap
          .put(TokenEnum.BTC, createReportData(TokenEnum.BTC, transparencyReportBTCTotalSheet, depositAddressToStakingDTOMap));
      tokenToReportDataMap
          .put(TokenEnum.ETH, createReportData(TokenEnum.ETH, transparencyReportETHTotalSheet, depositAddressToStakingDTOMap));
      tokenToReportDataMap
          .put(TokenEnum.USDT, createReportData(TokenEnum.USDT, transparencyReportUSDTTotalSheet, depositAddressToStakingDTOMap));
      tokenToReportDataMap
          .put(TokenEnum.USDC, createReportData(TokenEnum.USDC, transparencyReportUSDCTotalSheet, depositAddressToStakingDTOMap));

      // ...
      boolean isDifferenceNegative = tokenToReportDataMap.values().stream().anyMatch(data -> -1 != BigDecimal.ZERO.compareTo(data.difference));

      if (!isDifferenceNegative) {
        writeReport(
            rootPath, fileName, transparencyReportTransactionSheet, transparencyReportCustomerSheet, tokenToReportDataMap, depositAddressToStakingDTOMap);
      } else {
        String messageText =
            "Yield Machine Transparency Report:\n"
                + "LOCK Vermögen weniger als die Kundeneinlagen";
        sendTelegramMessage(messageText);
      }
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private ReportData createReportData(
      @Nonnull TokenEnum token,
      @Nonnull String transparencyReportTotalSheet,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    // ...
    CellDataList transparencyReportingCellDataList;

    if (TokenEnum.DFI == token) {
      transparencyReportingCellDataList =
          createDFICellDataList(currentDate, token, depositAddressToStakingDTOMap);
    } else {
      transparencyReportingCellDataList =
          createTokenCellDataList(currentDate, token, depositAddressToStakingDTOMap);
    }

    BigDecimal difference = (BigDecimal) transparencyReportingCellDataList.getProperty(TOTAL_DIFFERENCE_PROPERTY);

    // ...
    ReportData reportData = new ReportData();
    reportData.transparencyReportTotalSheet = transparencyReportTotalSheet;
    reportData.transparencyReportingCellDataList = transparencyReportingCellDataList;
    reportData.difference = difference;

    return reportData;
  }

  /**
   * 
   */
  private void setup() throws DfxException {
    LOGGER.debug("setup()");

    if (null == currentDate) {
      currentDate = new Date();
    }

    setupLiquidity();
    setupVault();
    setupLiquidMining();
    setupLiquidMiningPoolInfo();
    setupPriceFeed();
  }

  /**
   * 
   */
  private void setupLiquidity() throws DfxException {
    LOGGER.debug("setupLiquidity()");

    if (null == liquidityTokenToAmountMap) {
      String liquidityAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_LIQUIDITY_ADDRESS, "");
      liquidityTokenToAmountMap = getTokenToAmountMap(liquidityAddress);
    }

    if (null == rewardTokenToAmountMap) {
      String rewardAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_REWARD_ADDRESS, "");
      rewardTokenToAmountMap = getTokenToAmountMap(rewardAddress);
    }
  }

  /**
   * 
   */
  private void setupVault() throws DfxException {
    LOGGER.debug("setupVault()");

    if (null == vault1TokenToAmountMap
        || null == vault1TokenToVaultCollateralMap) {
      String vault1Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT1_ADDRESS, "");
      String vault1Id = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT1_ID, "");

      vault1TokenToAmountMap = getTokenToAmountMap(vault1Address);
      vault1TokenToVaultCollateralMap = getTokenToVaultCollateralMap(vault1Id);
    }

    if (null == vault2TokenToAmountMap
        || null == vault2TokenToVaultCollateralMap) {
      String vault2Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT2_ADDRESS, "");
      String vault2Id = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT2_ID, "");

      vault2TokenToAmountMap = getTokenToAmountMap(vault2Address);
      vault2TokenToVaultCollateralMap = getTokenToVaultCollateralMap(vault2Id);
    }

    if (null == vault3TokenToAmountMap
        || null == vault3TokenToVaultCollateralMap) {
      String vault3Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT3_ADDRESS, "");
      String vault3Id = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT3_ID, "");

      vault3TokenToAmountMap = getTokenToAmountMap(vault3Address);
      vault3TokenToVaultCollateralMap = getTokenToVaultCollateralMap(vault3Id);
    }
  }

  /**
   * 
   */
  private void setupLiquidMining() throws DfxException {
    LOGGER.debug("setupLiquidMining()");

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

  }

  /**
   * 
   */
  private void setupLiquidMiningPoolInfo() throws DfxException {
    LOGGER.debug("setupLiquidMiningPoolInfo()");

    if (null == dfiPoolTokenToPoolPairDataMap) {
      dfiPoolTokenToPoolPairDataMap = new HashMap<>();

      List<String> dfiPoolTokenList = new ArrayList<>();
      dfiPoolTokenList.add("BTC-DFI");
      dfiPoolTokenList.add("ETH-DFI");
      dfiPoolTokenList.add("DUSD-DFI");

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

      for (String dusdPoolToken : dusdPoolTokenList) {
        DefiPoolPairData poolPairData = dataProvider.getPoolPair(dusdPoolToken);
        dusdPoolTokenToPoolPairDataMap.put(dusdPoolToken, poolPairData);
      }
    }
  }

  /**
   * 
   */
  private void setupPriceFeed() throws DfxException {
    LOGGER.debug("setupPriceFeed()");

    Set<String> priceFeedTokenMap = new HashSet<>();
    priceFeedTokenMap.add(TokenEnum.DFI.toString());
    priceFeedTokenMap.add(TokenEnum.BTC.toString());
    priceFeedTokenMap.add(TokenEnum.ETH.toString());
    priceFeedTokenMap.add(TokenEnum.DUSD.toString());
    priceFeedTokenMap.add(TokenEnum.USDT.toString());
    priceFeedTokenMap.add(TokenEnum.USDC.toString());

    activePriceMap = dataProvider.getActivePriceMap(priceFeedTokenMap);
  }

  /**
   * 
   */
  private Map<String, BigDecimal> getTokenToAmountMap(@Nonnull String address) throws DfxException {
    LOGGER.debug("getTokenToAmountMap(): address=" + address);

    List<String> accountList = dataProvider.getAccount(address);

    return TransactionCheckerUtils.getTokenToAmountMap(accountList);
  }

  /**
   * 
   */
  private Map<String, BigDecimal> getTokenToVaultCollateralMap(@Nonnull String vaultId) throws DfxException {
    LOGGER.debug("getTokenToVaultCollateralMap(): vaultId=" + vaultId);

    DefiVaultData vaultData = dataProvider.getVault(vaultId);

    List<String> collateralAmountList = vaultData.getCollateralAmounts();

    return TransactionCheckerUtils.getTokenToAmountMap(collateralAmountList);
  }

  /**
   * 
   */
  private CellDataList createDFICellDataList(
      @Nonnull Date currentDate,
      @Nonnull TokenEnum token,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createCellDataList(): token=" + token);

    // ...
    BigDecimal totalCustomerValue = BigDecimal.ZERO;
    BigDecimal totalLockValue = BigDecimal.ZERO;

    // ...
    int numberOfCustomer = 0;
    BigDecimal stakingBalance = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : depositAddressToStakingDTOMap.values()) {
      if (token.getNumber() == stakingDTO.getTokenNumber()) {
        numberOfCustomer++;
        stakingBalance = stakingBalance.add(stakingDTO.getVin()).subtract(stakingDTO.getVout());
      }
    }

    // ...
    totalCustomerValue = totalCustomerValue.add(stakingBalance);

    CellDataList cellDataList = new CellDataList();
    cellDataList.add(new CellData().setRowIndex(1).setCellIndex(4).setKeepStyle(true).setValue(currentDate));
    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(0).setKeepStyle(true).setValue(numberOfCustomer));
    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(2).setKeepStyle(true).setValue(stakingBalance));

    // ...
    BigDecimal liquidityAmount = liquidityTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    totalLockValue = totalLockValue.add(liquidityAmount);

    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(5).setKeepStyle(true).setValue(liquidityAmount));

    // ...
    BigDecimal rewardAmount = rewardTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    totalLockValue = totalLockValue.add(rewardAmount);

    cellDataList.add(new CellData().setRowIndex(5).setCellIndex(5).setKeepStyle(true).setValue(rewardAmount));

    // ...
    BigDecimal vault1Amount = vault1TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault1Collateral = vault1TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(vault1Amount);
    totalLockValue = totalLockValue.add(vault1Collateral);

    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(5).setKeepStyle(true).setValue(vault1Amount));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(5).setKeepStyle(true).setValue(vault1Collateral));

    // ...
    BigDecimal vault2Amount = vault2TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault2Collateral = vault2TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(vault2Amount);
    totalLockValue = totalLockValue.add(vault2Collateral);

    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(5).setKeepStyle(true).setValue(vault2Amount));
    cellDataList.add(new CellData().setRowIndex(10).setCellIndex(5).setKeepStyle(true).setValue(vault2Collateral));

    // ...
    BigDecimal vault3Amount = vault3TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault3Collateral = vault3TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(vault3Amount);
    totalLockValue = totalLockValue.add(vault3Collateral);

    cellDataList.add(new CellData().setRowIndex(11).setCellIndex(5).setKeepStyle(true).setValue(vault3Amount));
    cellDataList.add(new CellData().setRowIndex(12).setCellIndex(5).setKeepStyle(true).setValue(vault3Collateral));

    // ...
    BigDecimal btcLMDFIAmount = btcLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal btcPoolDFIAmount = getBTCPoolDFIAmount();

    totalLockValue = totalLockValue.add(btcLMDFIAmount);
    totalLockValue = totalLockValue.add(btcPoolDFIAmount);

    cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(btcLMDFIAmount));
    cellDataList.add(new CellData().setRowIndex(15).setCellIndex(5).setKeepStyle(true).setValue(btcPoolDFIAmount));

    // ...
    BigDecimal ethLMDFIAmount = ethLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal ethPoolDFIAmount = getETHPoolDFIAmount();

    totalLockValue = totalLockValue.add(ethLMDFIAmount);
    totalLockValue = totalLockValue.add(ethPoolDFIAmount);

    cellDataList.add(new CellData().setRowIndex(16).setCellIndex(5).setKeepStyle(true).setValue(ethLMDFIAmount));
    cellDataList.add(new CellData().setRowIndex(17).setCellIndex(5).setKeepStyle(true).setValue(ethPoolDFIAmount));

    // ...
    BigDecimal dusdLMDFIAmount = dusdLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal dusdPoolDFIAmount = getDUSDPoolDFIAmount();

    totalLockValue = totalLockValue.add(dusdLMDFIAmount);
    totalLockValue = totalLockValue.add(dusdPoolDFIAmount);

    cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(dusdLMDFIAmount));
    cellDataList.add(new CellData().setRowIndex(19).setCellIndex(5).setKeepStyle(true).setValue(dusdPoolDFIAmount));

    // ...
    BigDecimal usdtLMDFImount = usdtLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    totalLockValue = totalLockValue.add(usdtLMDFImount);

    cellDataList.add(new CellData().setRowIndex(20).setCellIndex(5).setKeepStyle(true).setValue(usdtLMDFImount));

    // ...
    BigDecimal usdcLMDFImount = usdcLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(usdcLMDFImount);

    cellDataList.add(new CellData().setRowIndex(21).setCellIndex(5).setKeepStyle(true).setValue(usdcLMDFImount));

    // ...
    cellDataList.add(new CellData().setRowIndex(24).setCellIndex(2).setKeepStyle(true).setValue(totalCustomerValue));
    cellDataList.add(new CellData().setRowIndex(24).setCellIndex(5).setKeepStyle(true).setValue(totalLockValue));

    // ...
    BigDecimal difference = totalLockValue.subtract(totalCustomerValue);
    cellDataList.add(new CellData().setRowIndex(25).setCellIndex(5).setKeepStyle(true).setValue(difference));

    cellDataList.addProperty(TOTAL_DIFFERENCE_PROPERTY, difference);

    return cellDataList;
  }

  /**
   * 
   */
  private BigDecimal getBTCPoolDFIAmount() {
    String poolToken = "BTC-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = btcLMTokenToAmountMap.get(poolToken);

    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
    return poolTokenAmountPair.getRight();
  }

  /**
   * 
   */
  private BigDecimal getETHPoolDFIAmount() {
    String poolToken = "ETH-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = ethLMTokenToAmountMap.get(poolToken);

    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
    return poolTokenAmountPair.getRight();
  }

  /**
   * 
   */
  private BigDecimal getDUSDPoolDFIAmount() {
    String poolToken = "DUSD-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = dusdLMTokenToAmountMap.get(poolToken);

    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
    return poolTokenAmountPair.getRight();
  }

  /**
   * 
   */
  private CellDataList createTokenCellDataList(
      @Nonnull Date currentDate,
      @Nonnull TokenEnum token,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createCellDataList(): token=" + token);

    // ...
    BigDecimal totalCustomerValue = BigDecimal.ZERO;
    BigDecimal totalLockValue = BigDecimal.ZERO;

    // ...
    int numberOfCustomer = 0;
    BigDecimal stakingBalance = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : depositAddressToStakingDTOMap.values()) {
      if (token.getNumber() == stakingDTO.getTokenNumber()) {
        numberOfCustomer++;
        stakingBalance = stakingBalance.add(stakingDTO.getVin()).subtract(stakingDTO.getVout());
      }
    }

    // ...
    totalCustomerValue = totalCustomerValue.add(stakingBalance);

    CellDataList cellDataList = new CellDataList();
    cellDataList.add(new CellData().setRowIndex(1).setCellIndex(4).setKeepStyle(true).setValue(currentDate));
    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(0).setKeepStyle(true).setValue(numberOfCustomer));
    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(2).setKeepStyle(true).setValue(stakingBalance));

    // ...
    BigDecimal liquidityAmount = liquidityTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    totalLockValue = totalLockValue.add(liquidityAmount);

    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(5).setKeepStyle(true).setValue(liquidityAmount));

    // ...
    BigDecimal vault1Amount = vault1TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault1Collateral = vault1TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(vault1Amount);
    totalLockValue = totalLockValue.add(vault1Collateral);

    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(5).setKeepStyle(true).setValue(vault1Amount));
    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(5).setKeepStyle(true).setValue(vault1Collateral));

    // ...
    BigDecimal vault2Amount = vault2TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault2Collateral = vault2TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(vault2Amount);
    totalLockValue = totalLockValue.add(vault2Collateral);

    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(5).setKeepStyle(true).setValue(vault2Amount));
    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(5).setKeepStyle(true).setValue(vault2Collateral));

    // ...
    BigDecimal vault3Amount = vault3TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault3Collateral = vault3TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(vault3Amount);
    totalLockValue = totalLockValue.add(vault3Collateral);

    cellDataList.add(new CellData().setRowIndex(10).setCellIndex(5).setKeepStyle(true).setValue(vault3Amount));
    cellDataList.add(new CellData().setRowIndex(11).setCellIndex(5).setKeepStyle(true).setValue(vault3Collateral));

    // ...
    if (TokenEnum.BTC == token) {
      BigDecimal btcLMAmount = btcLMTokenToAmountMap.getOrDefault("BTC", BigDecimal.ZERO);
      BigDecimal btcPoolBTCAmount = getBTCPoolBTCAmount();
      totalLockValue = totalLockValue.add(btcLMAmount);
      totalLockValue = totalLockValue.add(btcPoolBTCAmount);

      cellDataList.add(new CellData().setRowIndex(13).setCellIndex(5).setKeepStyle(true).setValue(btcLMAmount));
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(btcPoolBTCAmount));
    } else if (TokenEnum.ETH == token) {
      BigDecimal ethLMAmount = ethLMTokenToAmountMap.getOrDefault("ETH", BigDecimal.ZERO);
      BigDecimal ethPoolETHAmount = getETHPoolETHAmount();
      totalLockValue = totalLockValue.add(ethLMAmount);
      totalLockValue = totalLockValue.add(ethPoolETHAmount);

      cellDataList.add(new CellData().setRowIndex(13).setCellIndex(5).setKeepStyle(true).setValue(ethLMAmount));
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(ethPoolETHAmount));
    } else if (TokenEnum.USDT == token) {
      BigDecimal usdtLMAmount = usdtLMTokenToAmountMap.getOrDefault("USDT", BigDecimal.ZERO);
      BigDecimal usdtPoolUSDTAmount = getUSDTPoolUSDTAmount();
      totalLockValue = totalLockValue.add(usdtLMAmount);
      totalLockValue = totalLockValue.add(usdtPoolUSDTAmount);

      cellDataList.add(new CellData().setRowIndex(13).setCellIndex(5).setKeepStyle(true).setValue(usdtLMAmount));
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(usdtPoolUSDTAmount));
    } else if (TokenEnum.USDC == token) {
      BigDecimal usdcLMAmount = usdcLMTokenToAmountMap.getOrDefault("USDC", BigDecimal.ZERO);
      BigDecimal usdcPoolUSDCAmount = getUSDCPoolUSDCAmount();
      totalLockValue = totalLockValue.add(usdcLMAmount);
      totalLockValue = totalLockValue.add(usdcPoolUSDCAmount);

      cellDataList.add(new CellData().setRowIndex(13).setCellIndex(5).setKeepStyle(true).setValue(usdcLMAmount));
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(usdcPoolUSDCAmount));
    }

    // ...
    cellDataList.add(new CellData().setRowIndex(16).setCellIndex(2).setKeepStyle(true).setValue(totalCustomerValue));
    cellDataList.add(new CellData().setRowIndex(16).setCellIndex(5).setKeepStyle(true).setValue(totalLockValue));

    // ...
    BigDecimal difference = totalLockValue.subtract(totalCustomerValue);
    cellDataList.add(new CellData().setRowIndex(17).setCellIndex(5).setKeepStyle(true).setValue(difference));

    cellDataList.addProperty(TOTAL_DIFFERENCE_PROPERTY, difference);

    return cellDataList;
  }

  /**
   * 
   */
  private BigDecimal getBTCPoolBTCAmount() {
    String poolToken = "BTC-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = btcLMTokenToAmountMap.get(poolToken);

    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
    return poolTokenAmountPair.getLeft();
  }

  /**
   * 
   */
  private BigDecimal getETHPoolETHAmount() {
    String poolToken = "ETH-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = ethLMTokenToAmountMap.get(poolToken);

    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
    return poolTokenAmountPair.getLeft();
  }

  /**
   * 
   */
  private BigDecimal getUSDTPoolUSDTAmount() {
    String poolToken = "USDT-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = usdtLMTokenToAmountMap.get(poolToken);

    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
    return poolTokenAmountPair.getLeft();
  }

  /**
   * 
   */
  private BigDecimal getUSDCPoolUSDCAmount() {
    String poolToken = "USDC-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = usdcLMTokenToAmountMap.get(poolToken);

    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
    return poolTokenAmountPair.getLeft();
  }

  /**
   *
   */
  private CellDataList createTransactionCellDataList(
      @Nonnull Map<TokenEnum, ReportData> tokenToReportDataMap) {
    LOGGER.debug("createTransactionCellDataList()");

    // ...
    ReportData btcReportData = tokenToReportDataMap.get(TokenEnum.BTC);
    BigDecimal btcDifference = btcReportData.difference;

    ReportData ethReportData = tokenToReportDataMap.get(TokenEnum.ETH);
    BigDecimal ethDifference = ethReportData.difference;

    ReportData usdtReportData = tokenToReportDataMap.get(TokenEnum.USDT);
    BigDecimal usdtDifference = usdtReportData.difference;

    ReportData usdcReportData = tokenToReportDataMap.get(TokenEnum.USDC);
    BigDecimal usdcDifference = usdcReportData.difference;

    CellDataList cellDataList = new CellDataList();

    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(1).setKeepStyle(true).setValue(btcDifference));
    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(1).setKeepStyle(true).setValue(ethDifference));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(1).setKeepStyle(true).setValue(usdtDifference));
    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(1).setKeepStyle(true).setValue(usdcDifference));

    // ...
    BigDecimal dfiPrice = activePriceMap.get(TokenEnum.DFI.toString());
    BigDecimal btcPrice = activePriceMap.get(TokenEnum.BTC.toString());
    BigDecimal ethPrice = activePriceMap.get(TokenEnum.ETH.toString());
    BigDecimal dusdPrice = activePriceMap.get(TokenEnum.DUSD.toString());
    BigDecimal usdtPrice = activePriceMap.get(TokenEnum.USDT.toString());
    BigDecimal usdcPrice = activePriceMap.get(TokenEnum.USDC.toString());

    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(4).setKeepStyle(true).setValue(dfiPrice));
    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(4).setKeepStyle(true).setValue(dfiPrice));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(4).setKeepStyle(true).setValue(dusdPrice));
    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(4).setKeepStyle(true).setValue(dusdPrice));

    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(6).setKeepStyle(true).setValue(btcPrice));
    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(6).setKeepStyle(true).setValue(ethPrice));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(6).setKeepStyle(true).setValue(usdtPrice));
    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(6).setKeepStyle(true).setValue(usdcPrice));

    // ...
    DefiPoolPairData btcDFIPoolPairData = dfiPoolTokenToPoolPairDataMap.get("BTC-DFI");
    DefiPoolPairData ethDFIPoolPairData = dfiPoolTokenToPoolPairDataMap.get("ETH-DFI");
    DefiPoolPairData usdtDUSDPoolPairData = dusdPoolTokenToPoolPairDataMap.get("USDT-DUSD");
    DefiPoolPairData usdcDUSDPoolPairData = dusdPoolTokenToPoolPairDataMap.get("USDC-DUSD");

    BigDecimal btcCommission = btcDFIPoolPairData.getCommission();
    BigDecimal ethCommission = ethDFIPoolPairData.getCommission();
    BigDecimal usdtCommission = usdtDUSDPoolPairData.getCommission();
    BigDecimal usdcCommission = usdcDUSDPoolPairData.getCommission();

    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(7).setKeepStyle(true).setValue(btcCommission));
    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(7).setKeepStyle(true).setValue(ethCommission));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(7).setKeepStyle(true).setValue(usdtCommission));
    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(7).setKeepStyle(true).setValue(usdcCommission));

    // ...
    BigDecimal btcDexFee = getDexFee(btcDFIPoolPairData);
    BigDecimal ethDexFee = getDexFee(ethDFIPoolPairData);
    BigDecimal usdtDexFee = getDexFee(usdtDUSDPoolPairData);
    BigDecimal usdcDexFee = getDexFee(usdcDUSDPoolPairData);

    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(8).setKeepStyle(true).setValue(btcDexFee));
    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(8).setKeepStyle(true).setValue(ethDexFee));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(8).setKeepStyle(true).setValue(usdtDexFee));
    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(8).setKeepStyle(true).setValue(usdcDexFee));

    // ...
    BigDecimal btcDFIInput =
        btcPrice.multiply(btcDifference, MATH_CONTEXT)
            .multiply(SIGN_CHANGER, MATH_CONTEXT)
            .divide(dfiPrice, MATH_CONTEXT)
            .divide(BigDecimal.ONE.subtract(btcCommission), MATH_CONTEXT)
            .divide(BigDecimal.ONE.subtract(btcDexFee), MATH_CONTEXT)
            .setScale(SCALE, RoundingMode.HALF_UP);

    BigDecimal ethDFIInput =
        ethPrice.multiply(ethDifference, MATH_CONTEXT)
            .multiply(SIGN_CHANGER, MATH_CONTEXT)
            .divide(dfiPrice, MATH_CONTEXT)
            .divide(BigDecimal.ONE.subtract(ethCommission), MATH_CONTEXT)
            .divide(BigDecimal.ONE.subtract(ethDexFee), MATH_CONTEXT)
            .setScale(SCALE, RoundingMode.HALF_UP);

    BigDecimal usdtDFIInput =
        usdtPrice.multiply(usdtDifference, MATH_CONTEXT)
            .multiply(SIGN_CHANGER, MATH_CONTEXT)
            .divide(dusdPrice, MATH_CONTEXT)
            .divide(BigDecimal.ONE.subtract(usdtCommission), MATH_CONTEXT)
            .divide(BigDecimal.ONE.subtract(usdtDexFee), MATH_CONTEXT)
            .setScale(SCALE, RoundingMode.HALF_UP);

    BigDecimal usdcDFIInput =
        usdcPrice.multiply(usdcDifference, MATH_CONTEXT)
            .multiply(SIGN_CHANGER, MATH_CONTEXT)
            .divide(dusdPrice, MATH_CONTEXT)
            .divide(BigDecimal.ONE.subtract(usdcCommission), MATH_CONTEXT)
            .divide(BigDecimal.ONE.subtract(usdcDexFee), MATH_CONTEXT)
            .setScale(SCALE, RoundingMode.HALF_UP);

    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(9).setKeepStyle(true).setValue(btcDFIInput));
    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(9).setKeepStyle(true).setValue(ethDFIInput));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(9).setKeepStyle(true).setValue(usdtDFIInput));
    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(9).setKeepStyle(true).setValue(usdcDFIInput));

    // ...
    BigDecimal btcDFIOutput =
        btcDFIInput.multiply(BigDecimal.ONE.subtract(ethCommission), MATH_CONTEXT)
            .multiply(BigDecimal.ONE.subtract(btcDexFee), MATH_CONTEXT)
            .multiply(dfiPrice, MATH_CONTEXT)
            .divide(btcPrice, MATH_CONTEXT)
            .setScale(SCALE, RoundingMode.HALF_UP);

    BigDecimal ethDFIOutput =
        ethDFIInput.multiply(BigDecimal.ONE.subtract(ethCommission), MATH_CONTEXT)
            .multiply(BigDecimal.ONE.subtract(ethDexFee), MATH_CONTEXT)
            .multiply(dfiPrice, MATH_CONTEXT)
            .divide(ethPrice, MATH_CONTEXT)
            .setScale(SCALE, RoundingMode.HALF_UP);

    BigDecimal usdtDFIOutput =
        usdtDFIInput.multiply(BigDecimal.ONE.subtract(usdtCommission), MATH_CONTEXT)
            .multiply(BigDecimal.ONE.subtract(usdtDexFee), MATH_CONTEXT)
            .multiply(dusdPrice, MATH_CONTEXT)
            .divide(usdtPrice, MATH_CONTEXT)
            .setScale(SCALE, RoundingMode.HALF_UP);

    BigDecimal usdcDFIOutput =
        usdcDFIInput.multiply(BigDecimal.ONE.subtract(usdcCommission), MATH_CONTEXT)
            .multiply(BigDecimal.ONE.subtract(usdcDexFee), MATH_CONTEXT)
            .multiply(dusdPrice, MATH_CONTEXT)
            .divide(usdcPrice, MATH_CONTEXT)
            .setScale(SCALE, RoundingMode.HALF_UP);

    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(11).setKeepStyle(true).setValue(btcDFIOutput));
    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(11).setKeepStyle(true).setValue(ethDFIOutput));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(11).setKeepStyle(true).setValue(usdtDFIOutput));
    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(11).setKeepStyle(true).setValue(usdcDFIOutput));

    return cellDataList;
  }

  /**
   * 
   */
  private BigDecimal getDexFee(@Nonnull DefiPoolPairData poolPairData) {

    BigDecimal dexFee = poolPairData.getDexFeeInPctTokenA();

    if (null == dexFee) {
      dexFee = BigDecimal.ZERO;
    }

    return dexFee;
  }

  /**
   * 
   */
  private Multimap<Integer, StakingDTO> getDepositAddressToStakingDTOMap() throws DfxException {
    LOGGER.debug("getDepositAddressToStakingDTOMap()");

    // ...
    List<StakingDTO> stakingDTOList = databaseBalanceHelper.getStakingDTOList(TokenEnum.DFI);
    stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(TokenEnum.BTC));
    stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(TokenEnum.ETH));
    stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(TokenEnum.USDT));
    stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(TokenEnum.USDC));

    // ...
    stakingDTOList.sort(new Comparator<StakingDTO>() {
      @Override
      public int compare(StakingDTO dto1, StakingDTO dto2) {
        int dto1BlockNumber = Math.max(dto1.getLastInBlockNumber(), dto1.getLastOutBlockNumber());
        int dto2BlockNumber = Math.max(dto2.getLastInBlockNumber(), dto2.getLastOutBlockNumber());

        int compare = dto2BlockNumber - dto1BlockNumber;

        if (0 == compare) {
          compare = ObjectUtils.compare(dto1.getDepositAddress(), dto2.getDepositAddress());
        }

        return compare;
      }
    });

    Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap = ArrayListMultimap.create();

    for (StakingDTO stakingDTO : stakingDTOList) {
      depositAddressToStakingDTOMap.put(stakingDTO.getDepositAddressNumber(), stakingDTO);
    }

    return depositAddressToStakingDTOMap;
  }

  /**
   * 
   */
  private void writeReport(
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String transparencyReportTransactionSheet,
      @Nonnull String transparencyReportCustomerSheet,
      @Nonnull Map<TokenEnum, ReportData> tokenToReportDataMap,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("writeReport()");

    // ...
    openExcel(rootPath, fileName);

    for (ReportData reportData : tokenToReportDataMap.values()) {
      setSheet(reportData.transparencyReportTotalSheet);
      writeExcel(new RowDataList(0), reportData.transparencyReportingCellDataList);
    }

    // ...
    CellDataList transactionsCellDataList = createTransactionCellDataList(tokenToReportDataMap);

    setSheet(transparencyReportTransactionSheet);
    writeExcel(new RowDataList(0), transactionsCellDataList);

    // ...
    RowDataList customerRowDataList = createRowDataList(depositAddressToStakingDTOMap);

    BigDecimal dfiTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_DFI_BALANCE_PROPERTY);
    BigDecimal btcTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_BTC_BALANCE_PROPERTY);
    BigDecimal ethTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_ETH_BALANCE_PROPERTY);
    BigDecimal usdtTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_USDT_BALANCE_PROPERTY);
    BigDecimal usdcTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_USDC_BALANCE_PROPERTY);

    CellDataList customerCellDataList = new CellDataList();
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(dfiTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(3).setKeepStyle(true).setValue(btcTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(4).setKeepStyle(true).setValue(ethTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(5).setKeepStyle(true).setValue(usdtTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(6).setKeepStyle(true).setValue(usdcTotalBalance));

    setSheet(transparencyReportCustomerSheet);
    cleanExcel(2);
    writeExcel(customerRowDataList, customerCellDataList);

    closeExcel();
  }

  /**
   * 
   */
  private RowDataList createRowDataList(@Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createRowDataList()");

    RowDataList rowDataList = new RowDataList(2);

    BigDecimal totalDFIBalance = BigDecimal.ZERO;
    BigDecimal totalBTCBalance = BigDecimal.ZERO;
    BigDecimal totalETHBalance = BigDecimal.ZERO;
    BigDecimal totalUSDTBalance = BigDecimal.ZERO;
    BigDecimal totalUSDCBalance = BigDecimal.ZERO;

    for (Integer depositAddressNumber : depositAddressToStakingDTOMap.keySet()) {
      List<StakingDTO> stakingDTOList = (List<StakingDTO>) depositAddressToStakingDTOMap.get(depositAddressNumber);

      StakingDTO firstStakingDTO = stakingDTOList.get(0);

      RowData rowData = new RowData();
      rowData.addCellData(new CellData().setValue(firstStakingDTO.getCustomerAddress()));
      rowData.addCellData(new CellData().setValue(firstStakingDTO.getDepositAddress()));

      BigDecimal dfiBalance = BigDecimal.ZERO;
      BigDecimal btcBalance = BigDecimal.ZERO;
      BigDecimal ethBalance = BigDecimal.ZERO;
      BigDecimal usdtBalance = BigDecimal.ZERO;
      BigDecimal usdcBalance = BigDecimal.ZERO;

      for (StakingDTO stakingDTO : stakingDTOList) {
        if (TokenEnum.DFI.getNumber() == stakingDTO.getTokenNumber()) {
          dfiBalance = stakingDTO.getVin().subtract(stakingDTO.getVout());
          totalDFIBalance = totalDFIBalance.add(dfiBalance);
        } else if (TokenEnum.BTC.getNumber() == stakingDTO.getTokenNumber()) {
          btcBalance = stakingDTO.getVin().subtract(stakingDTO.getVout());
          totalBTCBalance = totalBTCBalance.add(btcBalance);
        } else if (TokenEnum.ETH.getNumber() == stakingDTO.getTokenNumber()) {
          ethBalance = stakingDTO.getVin().subtract(stakingDTO.getVout());
          totalETHBalance = totalETHBalance.add(ethBalance);
        } else if (TokenEnum.USDT.getNumber() == stakingDTO.getTokenNumber()) {
          usdtBalance = stakingDTO.getVin().subtract(stakingDTO.getVout());
          totalUSDTBalance = totalUSDTBalance.add(usdtBalance);
        } else if (TokenEnum.USDC.getNumber() == stakingDTO.getTokenNumber()) {
          usdcBalance = stakingDTO.getVin().subtract(stakingDTO.getVout());
          totalUSDCBalance = totalUSDCBalance.add(usdcBalance);
        }
      }

      rowData.addCellData(new CellData().setValue(dfiBalance));
      rowData.addCellData(new CellData().setValue(btcBalance));
      rowData.addCellData(new CellData().setValue(ethBalance));
      rowData.addCellData(new CellData().setValue(usdtBalance));
      rowData.addCellData(new CellData().setValue(usdcBalance));

      rowDataList.add(rowData);
    }

    rowDataList.addProperty(TOTAL_DFI_BALANCE_PROPERTY, totalDFIBalance);
    rowDataList.addProperty(TOTAL_BTC_BALANCE_PROPERTY, totalBTCBalance);
    rowDataList.addProperty(TOTAL_ETH_BALANCE_PROPERTY, totalETHBalance);
    rowDataList.addProperty(TOTAL_USDT_BALANCE_PROPERTY, totalUSDTBalance);
    rowDataList.addProperty(TOTAL_USDC_BALANCE_PROPERTY, totalUSDCBalance);

    return rowDataList;
  }

  /**
   * 
   */
  private void sendTelegramMessage(@Nonnull String message) {
    LOGGER.debug("sendTelegramMessage()");

    String telegramToken = ConfigProvider.getInstance().getValue(ReportingConfigEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_TOKEN);
    String telegramChatId = ConfigProvider.getInstance().getValue(ReportingConfigEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_CHAT_ID);

    if (null != telegramToken
        && null != telegramChatId) {
      TelegramNotifier telegramNotifier = new TelegramNotifier();
      telegramNotifier.sendMessage(telegramToken, telegramChatId, message);
    }
  }

  /**
   * 
   */
  private class ReportData {
    private String transparencyReportTotalSheet;
    private CellDataList transparencyReportingCellDataList;
    private BigDecimal difference;
  }
}
