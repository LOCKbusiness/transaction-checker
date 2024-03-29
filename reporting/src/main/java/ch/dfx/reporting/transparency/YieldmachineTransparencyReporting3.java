package ch.dfx.reporting.transparency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import ch.dfx.reporting.BalanceReporting;
import ch.dfx.reporting.BalanceReporting.BalanceReportingTypeEnum;
import ch.dfx.reporting.Reporting;
import ch.dfx.reporting.transparency.data.DFISheetDTO;
import ch.dfx.reporting.transparency.data.DUSDSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAmountSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAmountSheetDTOList;
import ch.dfx.reporting.transparency.data.HistoryAssetPriceSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAssetPriceSheetDTOList;
import ch.dfx.reporting.transparency.data.HistoryInterimDifferenceSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryInterimDifferenceSheetDTOList;
import ch.dfx.reporting.transparency.data.HistoryPriceSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryPriceSheetDTOList;
import ch.dfx.reporting.transparency.data.ReportDTO;
import ch.dfx.reporting.transparency.data.TokenSheetDTO;
import ch.dfx.reporting.transparency.data.TotalSheetDTO;
import ch.dfx.reporting.transparency.data.TransactionSheetDTO;
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
  public enum SheetEnum {
    TOTAL,
    TRANSACTION,
    CUSTOMER,
    DIAGRAM,
    HISTORY_AMOUNT,
    HISTORY_INTERIM_DIFFERENCE,
    HISTORY_ASSET_PRICE,
    HISTORY_PRICE
  }

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final BigDecimal SIGN_CHANGER = new BigDecimal(-1);

  // ...
  private final DefiDataProvider dataProvider;

  private final TransparencyReportPriceHelper transparencyReportPriceHelper;
  private final TransparencyReportFileHelper transparencyReportFileHelper;

  private final CellListCreator cellListCreator;
  private final BalanceReporting balanceReporting;

  // ...
  private Map<String, BigDecimal> liquidityTokenToAmountMap = null;
  private Map<String, BigDecimal> rewardTokenToAmountMap = null;

  private Map<String, BigDecimal> vault1TokenToAmountMap = null;
  private Map<String, BigDecimal> vault1TokenToVaultCollateralMap = null;
  private Map<String, BigDecimal> vault1TokenToVaultLoanMap = null;

  private Map<String, BigDecimal> vault2TokenToAmountMap = null;
  private Map<String, BigDecimal> vault2TokenToVaultCollateralMap = null;
  private Map<String, BigDecimal> vault2TokenToVaultLoanMap = null;

  private Map<String, BigDecimal> vault3TokenToAmountMap = null;
  private Map<String, BigDecimal> vault3TokenToVaultCollateralMap = null;
  private Map<String, BigDecimal> vault3TokenToVaultLoanMap = null;

  private Map<String, BigDecimal> dusdLMTokenToAmountMap = null;
  private Map<String, BigDecimal> btcLMTokenToAmountMap = null;
  private Map<String, BigDecimal> ethLMTokenToAmountMap = null;
  private Map<String, BigDecimal> usdtLMTokenToAmountMap = null;
  private Map<String, BigDecimal> usdcLMTokenToAmountMap = null;
  private Map<String, BigDecimal> eurocLMTokenToAmountMap = null;
  private Map<String, BigDecimal> spyLMTokenToAmountMap = null;

  private Map<String, DefiPoolPairData> dfiPoolTokenToPoolPairDataMap = null;
  private Map<String, DefiPoolPairData> dusdPoolTokenToPoolPairDataMap = null;

  /**
   * 
   */
  public YieldmachineTransparencyReporting3(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    this.transparencyReportPriceHelper = new TransparencyReportPriceHelper();
    this.transparencyReportFileHelper = new TransparencyReportFileHelper();

    this.cellListCreator = new CellListCreator();

    this.balanceReporting =
        new BalanceReporting(network, databaseBlockHelper, databaseBalanceHelper, new ArrayList<>(), BalanceReportingTypeEnum.YIELD_MACHINE);
  }

  /**
   * 
   */
  public void report(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileNameExtern,
      @Nonnull String fileNameIntern,
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Map<TokenEnum, String> tokenToSheetNameMap) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      setup();

      // ...
      Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap = balanceReporting.getDepositAddressToStakingDTOMap();

      // ...
      Map<TokenEnum, ReportDTO> tokenToReportDTOMap = new EnumMap<>(TokenEnum.class);

      tokenToReportDTOMap
          .put(TokenEnum.DFI, createReportDTO(TokenEnum.DFI, tokenToSheetNameMap, depositAddressToStakingDTOMap));
      tokenToReportDTOMap
          .put(TokenEnum.DUSD, createReportDTO(TokenEnum.DUSD, tokenToSheetNameMap, depositAddressToStakingDTOMap));
      tokenToReportDTOMap
          .put(TokenEnum.BTC, createReportDTO(TokenEnum.BTC, tokenToSheetNameMap, depositAddressToStakingDTOMap));
      tokenToReportDTOMap
          .put(TokenEnum.ETH, createReportDTO(TokenEnum.ETH, tokenToSheetNameMap, depositAddressToStakingDTOMap));
      tokenToReportDTOMap
          .put(TokenEnum.USDT, createReportDTO(TokenEnum.USDT, tokenToSheetNameMap, depositAddressToStakingDTOMap));
      tokenToReportDTOMap
          .put(TokenEnum.USDC, createReportDTO(TokenEnum.USDC, tokenToSheetNameMap, depositAddressToStakingDTOMap));
      tokenToReportDTOMap
          .put(TokenEnum.EUROC, createReportDTO(TokenEnum.EUROC, tokenToSheetNameMap, depositAddressToStakingDTOMap));
      tokenToReportDTOMap
          .put(TokenEnum.SPY, createReportDTO(TokenEnum.SPY, tokenToSheetNameMap, depositAddressToStakingDTOMap));

      // ...
      Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap = createTokenToTransactionSheetDTOMap(tokenToReportDTOMap);

      // ...
      syncSheetData(TokenEnum.BTC, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.ETH, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.USDT, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.USDC, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.EUROC, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.SPY, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);

      syncDFISheetData(tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncDUSDSheetData(tokenToReportDTOMap, tokenToTransactionSheetDTOMap);

      // ...
      Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap = createTokenToTotalSheetDTOMap(tokenToReportDTOMap);
      long blockCount = transparencyReportPriceHelper.getBlockCount();

      // ...
      HistoryAmountSheetDTO historyAmountSheetDTO = createHistoryAmountSheetDTO(reportingTimestamp, tokenToTotalSheetDTOMap);
      transparencyReportFileHelper.appendHistoryAmountSheetDTOToJSON(historyAmountSheetDTO);

      HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTO = createHistoryInterimDifferenceSheetDTO(reportingTimestamp, tokenToTotalSheetDTOMap);
      transparencyReportFileHelper.appendHistoryInterimDifferenceSheetDTOToJSON(historyInterimDifferenceSheetDTO);

      HistoryAssetPriceSheetDTO historyAssetPriceSheetDTO = createHistoryAssetPriceSheetDTO(reportingTimestamp, blockCount, tokenToTotalSheetDTOMap);
      transparencyReportFileHelper.appendHistoryAssetPriceSheetDTOToJSON(historyAssetPriceSheetDTO);

      HistoryPriceSheetDTO historyPriceSheetDTO = createHistoryPriceSheetDTO(reportingTimestamp, tokenToTotalSheetDTOMap);
      transparencyReportFileHelper.appendHistoryPriceSheetDTOToJSON(historyPriceSheetDTO);

      // ...
      int hour = reportingTimestamp.toLocalDateTime().getHour();

      BigDecimal totalBalance =
          tokenToTotalSheetDTOMap.values().stream()
              .map(dto -> dto.getValue())
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (0 == hour
          && -1 == BigDecimal.ZERO.compareTo(totalBalance)) {
        Set<Integer> historyHourSet = new HashSet<>();
        historyHourSet.add(hour);

        writeReport(
            reportingTimestamp,
            rootPath, fileNameExtern, sheetIdToSheetNameMap,
            tokenToReportDTOMap, tokenToTransactionSheetDTOMap, tokenToTotalSheetDTOMap, depositAddressToStakingDTOMap,
            historyHourSet,
            false);
      }

      // ...
      writeReport(
          reportingTimestamp,
          rootPath, fileNameIntern, sheetIdToSheetNameMap,
          tokenToReportDTOMap, tokenToTransactionSheetDTOMap, tokenToTotalSheetDTOMap, depositAddressToStakingDTOMap,
          new HashSet<>(),
          true);

      // ...
      if (-1 != BigDecimal.ZERO.compareTo(totalBalance)) {
        String messageText =
            "Yieldmachine Transparency Report:\n"
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
  private ReportDTO createReportDTO(
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, String> tokenToSheetNameMap,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createReportDTO(): token=" + token);

    // ...
    String sheetName = tokenToSheetNameMap.get(token);

    DFISheetDTO dfiSheetDTO = null;
    DUSDSheetDTO dusdSheetDTO = null;
    TokenSheetDTO tokenSheetDTO = null;

    if (TokenEnum.DFI == token) {
      dfiSheetDTO = createDFISheetDTO(token, sheetName, depositAddressToStakingDTOMap);
    } else if (TokenEnum.DUSD == token) {
      dusdSheetDTO = createDUSDSheetDTO(token, sheetName, depositAddressToStakingDTOMap);
    } else {
      tokenSheetDTO = createTokenSheetDTO(token, sheetName, depositAddressToStakingDTOMap);
    }

    // ...
    ReportDTO reportDTO = new ReportDTO();
    reportDTO.setDfiSheetDTO(dfiSheetDTO);
    reportDTO.setDusdSheetDTO(dusdSheetDTO);
    reportDTO.setTokenSheetDTO(tokenSheetDTO);

    return reportDTO;
  }

  /**
   * 
   */
  private void syncSheetData(
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap) {
    LOGGER.trace("syncSheetData(): token=" + token);

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    TokenSheetDTO tokenSheetDTO = reportDTO.getTokenSheetDTO();
    TransactionSheetDTO transactionSheetDTO = tokenToTransactionSheetDTOMap.get(token);

    BigDecimal lockInterimDifference = tokenSheetDTO.getLockInterimDifference();
    BigDecimal lockChangeTransactionBalance = transactionSheetDTO.getOutput();

    tokenSheetDTO.setLockChangeTransactionBalance(lockChangeTransactionBalance);
    tokenSheetDTO.setTotalDifference(lockInterimDifference.add(lockChangeTransactionBalance));
  }

  /**
   * 
   */
  private void syncDFISheetData(
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap) {
    LOGGER.trace("syncDFISheetData()");

    // ...
    ReportDTO reportDTO = tokenToReportDTOMap.get(TokenEnum.DFI);
    DFISheetDTO dfiSheetDTO = reportDTO.getDfiSheetDTO();

    // ...
    TransactionSheetDTO btcTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.BTC);
    TransactionSheetDTO ethTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.ETH);

    BigDecimal btcDFIInput = btcTransactionSheetDTO.getInput();
    BigDecimal ethDFIInput = ethTransactionSheetDTO.getInput();

    dfiSheetDTO.setLockChangeBTCTransactionBalance(btcDFIInput);
    dfiSheetDTO.setLockChangeETHTransactionBalance(ethDFIInput);

    // ...
    BigDecimal customerInterimBalance = dfiSheetDTO.getCustomerInterimBalance();
    BigDecimal totalDFIInput = btcDFIInput.add(ethDFIInput);

    BigDecimal customerTotalBalance = customerInterimBalance.add(totalDFIInput);

    dfiSheetDTO.setCustomerTotalBalance(customerTotalBalance);

    // ...
    BigDecimal lockInterimBalance = dfiSheetDTO.getLockInterimBalance();
    BigDecimal lockTotalBalance = lockInterimBalance;

    dfiSheetDTO.setLockTotalBalance(lockTotalBalance);

    // ...
    BigDecimal totalDifference = lockTotalBalance.subtract(customerTotalBalance);

    dfiSheetDTO.setTotalDifference(totalDifference);
  }

  /**
   * 
   */
  private void syncDUSDSheetData(
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap) {
    LOGGER.trace("syncDUSDSheetData()");

    // ...
    ReportDTO reportDTO = tokenToReportDTOMap.get(TokenEnum.DUSD);
    DUSDSheetDTO dusdSheetDTO = reportDTO.getDusdSheetDTO();

    TransactionSheetDTO usdtTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.USDT);
    TransactionSheetDTO usdcTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.USDC);
    TransactionSheetDTO eurocTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.EUROC);
    TransactionSheetDTO spyTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.SPY);

    BigDecimal usdtDUSDInput = usdtTransactionSheetDTO.getInput();
    BigDecimal usdcDUSDInput = usdcTransactionSheetDTO.getInput();
    BigDecimal eurocDUSDInput = eurocTransactionSheetDTO.getInput();
    BigDecimal spyDUSDInput = spyTransactionSheetDTO.getInput();

    dusdSheetDTO.setLockChangeUSDTTransactionBalance(usdtDUSDInput);
    dusdSheetDTO.setLockChangeUSDCTransactionBalance(usdcDUSDInput);
    dusdSheetDTO.setLockChangeEUROCTransactionBalance(eurocDUSDInput);
    dusdSheetDTO.setLockChangeSPYTransactionBalance(spyDUSDInput);

    // ...
    BigDecimal customerInterimBalance = dusdSheetDTO.getCustomerInterimBalance();
    BigDecimal totalDUSDInput = usdtDUSDInput.add(usdcDUSDInput).add(eurocDUSDInput).add(spyDUSDInput);

    BigDecimal customerTotalBalance = customerInterimBalance.add(totalDUSDInput);

    dusdSheetDTO.setCustomerTotalBalance(customerTotalBalance);

    // ...
    BigDecimal lockInterimBalance = dusdSheetDTO.getLockInterimBalance();
    BigDecimal lockTotalBalance = lockInterimBalance;

    dusdSheetDTO.setLockTotalBalance(lockTotalBalance);

    // ...
    BigDecimal totalDifference = lockTotalBalance.subtract(customerTotalBalance);

    dusdSheetDTO.setTotalDifference(totalDifference);
  }

  /**
   * 
   */
  private void setup() throws DfxException {
    LOGGER.debug("setup()");

    setupLiquidity();
    setupVault();
    setupLiquidMining();
    setupLiquidMiningPoolInfo();
  }

  /**
   * 
   */
  private void setupLiquidity() throws DfxException {
    LOGGER.trace("setupLiquidity()");

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
    LOGGER.trace("setupVault()");

    if (null == vault1TokenToAmountMap
        || null == vault1TokenToVaultCollateralMap
        || null == vault1TokenToVaultLoanMap) {
      String vault1Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT1_ADDRESS, "");
      String vault1Id = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT1_ID, "");

      vault1TokenToAmountMap = getTokenToAmountMap(vault1Address);
      vault1TokenToVaultCollateralMap = getTokenToVaultCollateralMap(vault1Id);
      vault1TokenToVaultLoanMap = getTokenToVaultLoanMap(vault1Id);
    }

    if (null == vault2TokenToAmountMap
        || null == vault2TokenToVaultCollateralMap
        || null == vault2TokenToVaultLoanMap) {
      String vault2Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT2_ADDRESS, "");
      String vault2Id = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT2_ID, "");

      vault2TokenToAmountMap = getTokenToAmountMap(vault2Address);
      vault2TokenToVaultCollateralMap = getTokenToVaultCollateralMap(vault2Id);
      vault2TokenToVaultLoanMap = getTokenToVaultLoanMap(vault2Id);
    }

    if (null == vault3TokenToAmountMap
        || null == vault3TokenToVaultCollateralMap
        || null == vault3TokenToVaultLoanMap) {
      String vault3Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT3_ADDRESS, "");
      String vault3Id = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT3_ID, "");

      vault3TokenToAmountMap = getTokenToAmountMap(vault3Address);
      vault3TokenToVaultCollateralMap = getTokenToVaultCollateralMap(vault3Id);
      vault3TokenToVaultLoanMap = getTokenToVaultLoanMap(vault3Id);
    }
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

    if (null == eurocLMTokenToAmountMap) {
      String eurocLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_EUROC_LM_ADDRESS, "");
      eurocLMTokenToAmountMap = getTokenToAmountMap(eurocLMAddress);
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
      dusdPoolTokenList.add("EUROC-DUSD");
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
  private Map<String, BigDecimal> getTokenToVaultCollateralMap(@Nonnull String vaultId) throws DfxException {
    LOGGER.trace("getTokenToVaultCollateralMap(): vaultId=" + vaultId);

    DefiVaultData vaultData = dataProvider.getVault(vaultId);

    List<String> collateralAmountList = vaultData.getCollateralAmounts();

    return TransactionCheckerUtils.getTokenToAmountMap(collateralAmountList);
  }

  /**
   * 
   */
  private Map<String, BigDecimal> getTokenToVaultLoanMap(@Nonnull String vaultId) throws DfxException {
    LOGGER.trace("getTokenToVaultLoanMap(): vaultId=" + vaultId);

    DefiVaultData vaultData = dataProvider.getVault(vaultId);

    List<String> loanAmountList = vaultData.getLoanAmounts();

    return TransactionCheckerUtils.getTokenToAmountMap(loanAmountList);
  }

  /**
   * 
   */
  private DFISheetDTO createDFISheetDTO(
      @Nonnull TokenEnum token,
      @Nonnull String sheetName,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createDFISheetDTO(): token=" + token);

    // ...
    BigDecimal customerInterimBalance = BigDecimal.ZERO;
    BigDecimal lockInterimBalance = BigDecimal.ZERO;

    // ...
    int numberOfCustomer = 0;
    BigDecimal customerDeposits = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : depositAddressToStakingDTOMap.values()) {
      if (token.getNumber() == stakingDTO.getTokenNumber()) {
        numberOfCustomer++;
        customerDeposits = customerDeposits.add(stakingDTO.getVin()).subtract(stakingDTO.getVout());
      }
    }

    // ...
    customerInterimBalance = customerInterimBalance.add(customerDeposits);

    // ...
    BigDecimal liquidityAmount = liquidityTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    lockInterimBalance = lockInterimBalance.add(liquidityAmount);

    // ...
    BigDecimal rewardAmount = rewardTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    lockInterimBalance = lockInterimBalance.add(rewardAmount);

    // ...
    BigDecimal vault1Amount = vault1TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault1Collateral = vault1TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(vault1Amount);
    lockInterimBalance = lockInterimBalance.add(vault1Collateral);

    // ...
    BigDecimal vault2Amount = vault2TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault2Collateral = vault2TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(vault2Amount);
    lockInterimBalance = lockInterimBalance.add(vault2Collateral);

    // ...
    BigDecimal vault3Amount = vault3TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault3Collateral = vault3TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(vault3Amount);
    lockInterimBalance = lockInterimBalance.add(vault3Collateral);

    // ...
    BigDecimal btcLMDFIAmount = btcLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal btcPoolDFIAmount = getBTCPoolDFIAmount();

    lockInterimBalance = lockInterimBalance.add(btcLMDFIAmount);
    lockInterimBalance = lockInterimBalance.add(btcPoolDFIAmount);

    // ...
    BigDecimal ethLMDFIAmount = ethLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal ethPoolDFIAmount = getETHPoolDFIAmount();

    lockInterimBalance = lockInterimBalance.add(ethLMDFIAmount);
    lockInterimBalance = lockInterimBalance.add(ethPoolDFIAmount);

    // ...
    BigDecimal dusdLMDFIAmount = dusdLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal dusdPoolDFIAmount = getDUSDPoolDFIAmount();

    lockInterimBalance = lockInterimBalance.add(dusdLMDFIAmount);
    lockInterimBalance = lockInterimBalance.add(dusdPoolDFIAmount);

    // ...
    BigDecimal usdtLMDFIAmount = usdtLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(usdtLMDFIAmount);

    // ...
    BigDecimal usdcLMDFIAmount = usdcLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(usdcLMDFIAmount);

    // ...
    BigDecimal eurocLMDFIAmount = eurocLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(eurocLMDFIAmount);

    // ...
    BigDecimal spyLMDFIAmount = spyLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(spyLMDFIAmount);

    // ...
    BigDecimal lockInterimDifference = lockInterimBalance.subtract(customerInterimBalance);

    // ...
    DFISheetDTO dfiSheetDTO = new DFISheetDTO();
    dfiSheetDTO.setSheetName(sheetName);

    dfiSheetDTO.setNumberOfCustomers(numberOfCustomer);
    dfiSheetDTO.setCustomerDeposits(customerDeposits);

    dfiSheetDTO.setLockLiquidityAmount(liquidityAmount);
    dfiSheetDTO.setLockRewardAmount(rewardAmount);

    dfiSheetDTO.setLockVault1Balance(vault1Amount);
    dfiSheetDTO.setLockVault1Collateral(vault1Collateral);
    dfiSheetDTO.setLockVault2Balance(vault2Amount);
    dfiSheetDTO.setLockVault2Collateral(vault2Collateral);
    dfiSheetDTO.setLockVault3Balance(vault3Amount);
    dfiSheetDTO.setLockVault3Collateral(vault3Collateral);

    dfiSheetDTO.setLock_LM_BTC_DFI_Balance(btcLMDFIAmount);
    dfiSheetDTO.setLock_LM_BTC_DFI_Pool(btcPoolDFIAmount);
    dfiSheetDTO.setLock_LM_ETH_DFI_Pool(ethLMDFIAmount);
    dfiSheetDTO.setLock_LM_ETH_DFI_Pool(ethPoolDFIAmount);
    dfiSheetDTO.setLock_LM_DUSD_DFI_Balance(dusdLMDFIAmount);
    dfiSheetDTO.setLock_LM_DUSD_DFI_Pool(dusdPoolDFIAmount);
    dfiSheetDTO.setLock_LM_USDT_DFI_Balance(usdtLMDFIAmount);
    dfiSheetDTO.setLock_LM_USDC_DFI_Balance(usdcLMDFIAmount);
    dfiSheetDTO.setLock_LM_EUROC_DFI_Balance(eurocLMDFIAmount);
    dfiSheetDTO.setLock_LM_SPY_DFI_Balance(spyLMDFIAmount);

    dfiSheetDTO.setCustomerInterimBalance(customerInterimBalance);
    dfiSheetDTO.setLockInterimBalance(lockInterimBalance);
    dfiSheetDTO.setLockInterimDifference(lockInterimDifference);

    dfiSheetDTO.setLockChangeBTCTransactionBalance(BigDecimal.ZERO);
    dfiSheetDTO.setLockChangeETHTransactionBalance(BigDecimal.ZERO);

    dfiSheetDTO.setCustomerTotalBalance(BigDecimal.ZERO);
    dfiSheetDTO.setLockTotalBalance(BigDecimal.ZERO);

    dfiSheetDTO.setTotalDifference(BigDecimal.ZERO);

    return dfiSheetDTO;
  }

  /**
   * 
   */
  private DUSDSheetDTO createDUSDSheetDTO(
      @Nonnull TokenEnum token,
      @Nonnull String sheetName,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createDUSDSheetDTO(): token=" + token);

    // ...
    BigDecimal customerInterimBalance = BigDecimal.ZERO;
    BigDecimal lockInterimBalance = BigDecimal.ZERO;

    // ...
    int numberOfCustomer = 0;
    BigDecimal customerDeposits = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : depositAddressToStakingDTOMap.values()) {
      if (token.getNumber() == stakingDTO.getTokenNumber()) {
        numberOfCustomer++;
        customerDeposits = customerDeposits.add(stakingDTO.getVin()).subtract(stakingDTO.getVout());
      }
    }

    // ...
    customerInterimBalance = customerInterimBalance.add(customerDeposits);

    // ...
    BigDecimal vault1Loan = vault1TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault2Loan = vault2TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault3Loan = vault3TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    customerInterimBalance = customerInterimBalance.add(vault1Loan);
    customerInterimBalance = customerInterimBalance.add(vault2Loan);
    customerInterimBalance = customerInterimBalance.add(vault3Loan);

    // ...
    BigDecimal liquidityAmount = liquidityTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    lockInterimBalance = lockInterimBalance.add(liquidityAmount);

    // ...
    BigDecimal rewardAmount = rewardTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    lockInterimBalance = lockInterimBalance.add(rewardAmount);

    // Vault 1 Amount == SPY LM Balance ...
    BigDecimal vault1Amount = BigDecimal.ZERO; // vault1TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault1Collateral = vault1TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(vault1Amount);
    lockInterimBalance = lockInterimBalance.add(vault1Collateral);

    // ...
    BigDecimal vault2Amount = vault2TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault2Collateral = vault2TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(vault2Amount);
    lockInterimBalance = lockInterimBalance.add(vault2Collateral);

    // ...
    BigDecimal vault3Amount = vault3TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault3Collateral = vault3TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(vault3Amount);
    lockInterimBalance = lockInterimBalance.add(vault3Collateral);

    // ...
    BigDecimal dusdLMDUSDAmount = dusdLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal dusdPoolDUSDAmount = getDUSDPoolDUSDAmount();

    lockInterimBalance = lockInterimBalance.add(dusdLMDUSDAmount);
    lockInterimBalance = lockInterimBalance.add(dusdPoolDUSDAmount);

    // ...
    BigDecimal usdtLMDUSDAmount = usdtLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal usdtPoolDUSDAmount = getUSDTPoolDUSDAmount();

    lockInterimBalance = lockInterimBalance.add(usdtLMDUSDAmount);
    lockInterimBalance = lockInterimBalance.add(usdtPoolDUSDAmount);

    // ...
    BigDecimal usdcLMDUSDAmount = usdcLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal usdcPoolDUSDAmount = getUSDCPoolDUSDAmount();

    lockInterimBalance = lockInterimBalance.add(usdcLMDUSDAmount);
    lockInterimBalance = lockInterimBalance.add(usdcPoolDUSDAmount);

    // ...
    BigDecimal eurocLMDUSDAmount = eurocLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal eurocPoolDUSDAmount = getEUROCPoolDUSDAmount();

    lockInterimBalance = lockInterimBalance.add(eurocLMDUSDAmount);
    lockInterimBalance = lockInterimBalance.add(eurocPoolDUSDAmount);

    // ...
    BigDecimal spyLMDUSDAmount = spyLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal spyPoolDUSDAmount = getSPYPoolDUSDAmount();

    lockInterimBalance = lockInterimBalance.add(spyLMDUSDAmount);
    lockInterimBalance = lockInterimBalance.add(spyPoolDUSDAmount);

    // ...
    BigDecimal lockInterimDifference = lockInterimBalance.subtract(customerInterimBalance);

    // ...
    DUSDSheetDTO dusdSheetDTO = new DUSDSheetDTO();
    dusdSheetDTO.setSheetName(sheetName);

    dusdSheetDTO.setNumberOfCustomers(numberOfCustomer);
    dusdSheetDTO.setCustomerDeposits(customerDeposits);

    dusdSheetDTO.setLockLiquidityAmount(liquidityAmount);
    dusdSheetDTO.setLockRewardAmount(rewardAmount);

    dusdSheetDTO.setLockVault1Balance(vault1Amount);
    dusdSheetDTO.setLockVault1Collateral(vault1Collateral);
    dusdSheetDTO.setLockVault1Loan(vault1Loan);
    dusdSheetDTO.setLockVault2Balance(vault2Amount);
    dusdSheetDTO.setLockVault2Collateral(vault2Collateral);
    dusdSheetDTO.setLockVault2Loan(vault2Loan);
    dusdSheetDTO.setLockVault3Balance(vault3Amount);
    dusdSheetDTO.setLockVault3Collateral(vault3Collateral);
    dusdSheetDTO.setLockVault3Loan(vault3Loan);

    dusdSheetDTO.setLock_LM_DUSD_DFI_Balance(dusdLMDUSDAmount);
    dusdSheetDTO.setLock_LM_DUSD_DFI_Pool(dusdPoolDUSDAmount);
    dusdSheetDTO.setLock_LM_USDT_DUSD_Balance(usdtLMDUSDAmount);
    dusdSheetDTO.setLock_LM_USDT_DUSD_Pool(usdtPoolDUSDAmount);
    dusdSheetDTO.setLock_LM_USDC_DUSD_Balance(usdcLMDUSDAmount);
    dusdSheetDTO.setLock_LM_USDC_DUSD_Pool(usdcPoolDUSDAmount);
    dusdSheetDTO.setLock_LM_EUROC_DUSD_Balance(eurocLMDUSDAmount);
    dusdSheetDTO.setLock_LM_EUROC_DUSD_Pool(eurocPoolDUSDAmount);
    dusdSheetDTO.setLock_LM_SPY_DUSD_Balance(spyLMDUSDAmount);
    dusdSheetDTO.setLock_LM_SPY_DUSD_Pool(spyPoolDUSDAmount);

    dusdSheetDTO.setCustomerInterimBalance(customerInterimBalance);
    dusdSheetDTO.setLockInterimBalance(lockInterimBalance);
    dusdSheetDTO.setLockInterimDifference(lockInterimDifference);

    dusdSheetDTO.setLockChangeUSDTTransactionBalance(BigDecimal.ZERO);
    dusdSheetDTO.setLockChangeUSDCTransactionBalance(BigDecimal.ZERO);
    dusdSheetDTO.setLockChangeEUROCTransactionBalance(BigDecimal.ZERO);
    dusdSheetDTO.setLockChangeSPYTransactionBalance(BigDecimal.ZERO);

    dusdSheetDTO.setCustomerTotalBalance(BigDecimal.ZERO);
    dusdSheetDTO.setLockTotalBalance(BigDecimal.ZERO);

    dusdSheetDTO.setTotalDifference(BigDecimal.ZERO);

    return dusdSheetDTO;
  }

  /**
   * 
   */
  private TokenSheetDTO createTokenSheetDTO(
      @Nonnull TokenEnum token,
      @Nonnull String sheetName,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createTokenSheetDTO(): token=" + token);

    // ...
    BigDecimal customerInterimBalance = BigDecimal.ZERO;
    BigDecimal lockInterimBalance = BigDecimal.ZERO;

    // ...
    int numberOfCustomer = 0;
    BigDecimal customerDeposits = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : depositAddressToStakingDTOMap.values()) {
      if (token.getNumber() == stakingDTO.getTokenNumber()) {
        numberOfCustomer++;
        customerDeposits = customerDeposits.add(stakingDTO.getVin()).subtract(stakingDTO.getVout());
      }
    }

    customerInterimBalance = customerInterimBalance.add(customerDeposits);

    // ...
    BigDecimal vault1Loan = vault1TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault2Loan = vault2TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault3Loan = vault3TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    customerInterimBalance = customerInterimBalance.add(vault1Loan);
    customerInterimBalance = customerInterimBalance.add(vault2Loan);
    customerInterimBalance = customerInterimBalance.add(vault3Loan);

    // ...
    BigDecimal liquidityAmount = liquidityTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    lockInterimBalance = lockInterimBalance.add(liquidityAmount);

    // ...
    BigDecimal rewardAmount = rewardTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    lockInterimBalance = lockInterimBalance.add(rewardAmount);

    // ...
    BigDecimal vault1Amount = BigDecimal.ZERO;
    BigDecimal vault1Collateral = BigDecimal.ZERO;

    // SPY: Address of Vault 1 = Address of SPY Liquidity Mining Pool ...
    if (TokenEnum.SPY != token) {
      vault1Amount = vault1TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
      vault1Collateral = vault1TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    }

    lockInterimBalance = lockInterimBalance.add(vault1Amount);
    lockInterimBalance = lockInterimBalance.add(vault1Collateral);

    // ...
    BigDecimal vault2Amount = vault2TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault2Collateral = vault2TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(vault2Amount);
    lockInterimBalance = lockInterimBalance.add(vault2Collateral);

    // ...
    BigDecimal vault3Amount = vault3TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault3Collateral = vault3TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(vault3Amount);
    lockInterimBalance = lockInterimBalance.add(vault3Collateral);

    // ...
    BigDecimal lmAmount = BigDecimal.ZERO;
    BigDecimal poolAmount = BigDecimal.ZERO;

    if (TokenEnum.BTC == token) {
      lmAmount = btcLMTokenToAmountMap.getOrDefault("BTC", BigDecimal.ZERO);
      poolAmount = getBTCPoolBTCAmount();

      lockInterimBalance = lockInterimBalance.add(lmAmount);
      lockInterimBalance = lockInterimBalance.add(poolAmount);
    } else if (TokenEnum.ETH == token) {
      lmAmount = ethLMTokenToAmountMap.getOrDefault("ETH", BigDecimal.ZERO);
      poolAmount = getETHPoolETHAmount();

      lockInterimBalance = lockInterimBalance.add(lmAmount);
      lockInterimBalance = lockInterimBalance.add(poolAmount);
    } else if (TokenEnum.USDT == token) {
      lmAmount = usdtLMTokenToAmountMap.getOrDefault("USDT", BigDecimal.ZERO);
      poolAmount = getUSDTPoolUSDTAmount();

      lockInterimBalance = lockInterimBalance.add(lmAmount);
      lockInterimBalance = lockInterimBalance.add(poolAmount);
    } else if (TokenEnum.USDC == token) {
      lmAmount = usdcLMTokenToAmountMap.getOrDefault("USDC", BigDecimal.ZERO);
      poolAmount = getUSDCPoolUSDCAmount();

      lockInterimBalance = lockInterimBalance.add(lmAmount);
      lockInterimBalance = lockInterimBalance.add(poolAmount);
    } else if (TokenEnum.EUROC == token) {
      lmAmount = eurocLMTokenToAmountMap.getOrDefault("EUROC", BigDecimal.ZERO);
      poolAmount = getEUROCPoolEUROCAmount();

      lockInterimBalance = lockInterimBalance.add(lmAmount);
      lockInterimBalance = lockInterimBalance.add(poolAmount);
    } else if (TokenEnum.SPY == token) {
      lmAmount = spyLMTokenToAmountMap.getOrDefault("SPY", BigDecimal.ZERO);
      poolAmount = getSPYPoolSPYAmount();

      lockInterimBalance = lockInterimBalance.add(lmAmount);
      lockInterimBalance = lockInterimBalance.add(poolAmount);
    }

    // ...
    BigDecimal lockInterimDifference = lockInterimBalance.subtract(customerInterimBalance);

    // ...
    TokenSheetDTO tokenSheetDTO = new TokenSheetDTO();
    tokenSheetDTO.setSheetName(sheetName);

    tokenSheetDTO.setNumberOfCustomers(numberOfCustomer);
    tokenSheetDTO.setCustomerDeposits(customerDeposits);
    tokenSheetDTO.setCustomerInterimBalance(customerInterimBalance);

    tokenSheetDTO.setLockLiquidityAmount(liquidityAmount);
    tokenSheetDTO.setLockRewardAmount(rewardAmount);

    tokenSheetDTO.setLockVault1Balance(vault1Amount);
    tokenSheetDTO.setLockVault1Collateral(vault1Collateral);
    tokenSheetDTO.setLockVault1Loan(vault1Loan);
    tokenSheetDTO.setLockVault2Balance(vault2Amount);
    tokenSheetDTO.setLockVault2Collateral(vault2Collateral);
    tokenSheetDTO.setLockVault2Loan(vault2Loan);
    tokenSheetDTO.setLockVault3Balance(vault3Amount);
    tokenSheetDTO.setLockVault3Collateral(vault3Collateral);
    tokenSheetDTO.setLockVault3Loan(vault3Loan);

    tokenSheetDTO.setLockLMBalance(lmAmount);
    tokenSheetDTO.setLockLMPool(poolAmount);

    tokenSheetDTO.setLockInterimBalance(lockInterimBalance);
    tokenSheetDTO.setLockInterimDifference(lockInterimDifference);
    tokenSheetDTO.setLockChangeTransactionBalance(BigDecimal.ZERO);
    tokenSheetDTO.setTotalDifference(BigDecimal.ZERO);

    return tokenSheetDTO;
  }

  /**
   *
   */
  private Map<TokenEnum, TransactionSheetDTO> createTokenToTransactionSheetDTOMap(
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) {
    LOGGER.debug("createTokenToTransactionSheetDTOMap()");

    // ...
    DefiPoolPairData btcDFIPoolPairData = dfiPoolTokenToPoolPairDataMap.get("BTC-DFI");
    DefiPoolPairData ethDFIPoolPairData = dfiPoolTokenToPoolPairDataMap.get("ETH-DFI");
    DefiPoolPairData usdtDUSDPoolPairData = dusdPoolTokenToPoolPairDataMap.get("USDT-DUSD");
    DefiPoolPairData usdcDUSDPoolPairData = dusdPoolTokenToPoolPairDataMap.get("USDC-DUSD");
    DefiPoolPairData eurocDUSDPoolPairData = dusdPoolTokenToPoolPairDataMap.get("EUROC-DUSD");
    DefiPoolPairData spyDUSDPoolPairData = dusdPoolTokenToPoolPairDataMap.get("SPY-DUSD");

    // ...
    Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap = new EnumMap<>(TokenEnum.class);

    fillTokenToTransactionSheetDTOMap(TokenEnum.BTC, btcDFIPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
    fillTokenToTransactionSheetDTOMap(TokenEnum.ETH, ethDFIPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
    fillTokenToTransactionSheetDTOMap(TokenEnum.USDT, usdtDUSDPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
    fillTokenToTransactionSheetDTOMap(TokenEnum.USDC, usdcDUSDPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
    fillTokenToTransactionSheetDTOMap(TokenEnum.EUROC, eurocDUSDPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
    fillTokenToTransactionSheetDTOMap(TokenEnum.SPY, spyDUSDPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);

    return tokenToTransactionSheetDTOMap;
  }

  /**
   * 
   */
  private void fillTokenToTransactionSheetDTOMap(
      @Nonnull TokenEnum token,
      @Nonnull DefiPoolPairData poolPairData,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap) {
    // ...
    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    BigDecimal difference = reportDTO.getTokenSheetDTO().getLockInterimDifference();

    // ...
    BigDecimal poolTokenReserveA = poolPairData.getReserveA();
    BigDecimal poolTokenReserveB = poolPairData.getReserveB();
    BigDecimal poolTotalLiquidity = poolPairData.getTotalLiquidity();
    BigDecimal poolRatioAB = poolTokenReserveA.divide(poolTokenReserveB, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    // ...
    BigDecimal commission = poolPairData.getCommission();
    BigDecimal dexFee = getDexFee(poolPairData);

    // ...
    BigDecimal input = BigDecimal.ZERO;
    BigDecimal output = BigDecimal.ZERO;

    if (-1 != BigDecimal.ZERO.compareTo(difference)) {
      // =B7/H7/(1-J7)/(1-K7) ...
      input =
          difference.multiply(SIGN_CHANGER, MATH_CONTEXT)
              .divide(poolRatioAB, MATH_CONTEXT)
              .divide(BigDecimal.ONE.subtract(commission), MATH_CONTEXT)
              .divide(BigDecimal.ONE.subtract(dexFee), MATH_CONTEXT)
              .setScale(SCALE, RoundingMode.HALF_UP);

      // =L7*(1-J7)*(1-K7)*H7 ...
      output =
          input.multiply(BigDecimal.ONE.subtract(commission), MATH_CONTEXT)
              .multiply(BigDecimal.ONE.subtract(dexFee), MATH_CONTEXT)
              .multiply(poolRatioAB, MATH_CONTEXT)
              .setScale(SCALE, RoundingMode.HALF_UP);
    }

    // ...
    TransactionSheetDTO transactionSheetDTO = new TransactionSheetDTO();

    transactionSheetDTO.setDifference(difference);
    transactionSheetDTO.setTotalPoolTokenA(poolTokenReserveA);
    transactionSheetDTO.setTotalPoolTokenB(poolTokenReserveB);
    transactionSheetDTO.setTotalLiquidityPoolToken(poolTotalLiquidity);
    transactionSheetDTO.setPoolTokenRatio(poolRatioAB);
    transactionSheetDTO.setSwapFee(commission);
    transactionSheetDTO.setDexFee(dexFee);
    transactionSheetDTO.setInput(input);
    transactionSheetDTO.setOutput(output);

    tokenToTransactionSheetDTOMap.put(token, transactionSheetDTO);
  }

  /**
   * 
   */
  private Map<TokenEnum, TotalSheetDTO> createTokenToTotalSheetDTOMap(@Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("createTokenToTotalSheetDTOMap()");

    Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap = new HashMap<>();

    // ...
    Map<TokenEnum, BigDecimal> tokenToPriceMap = transparencyReportPriceHelper.createTokenToPriceMap();

    fillDFITokenToTotalSheetDTOMap(TokenEnum.DFI, tokenToReportDTOMap, tokenToPriceMap, tokenToTotalSheetDTOMap);
    fillDUSDTokenToTotalSheetDTOMap(TokenEnum.DUSD, tokenToReportDTOMap, tokenToPriceMap, tokenToTotalSheetDTOMap);
    fillTokenToTotalSheetDTOMap(TokenEnum.BTC, tokenToReportDTOMap, tokenToPriceMap, tokenToTotalSheetDTOMap);
    fillTokenToTotalSheetDTOMap(TokenEnum.ETH, tokenToReportDTOMap, tokenToPriceMap, tokenToTotalSheetDTOMap);
    fillTokenToTotalSheetDTOMap(TokenEnum.USDT, tokenToReportDTOMap, tokenToPriceMap, tokenToTotalSheetDTOMap);
    fillTokenToTotalSheetDTOMap(TokenEnum.USDC, tokenToReportDTOMap, tokenToPriceMap, tokenToTotalSheetDTOMap);
    fillTokenToTotalSheetDTOMap(TokenEnum.EUROC, tokenToReportDTOMap, tokenToPriceMap, tokenToTotalSheetDTOMap);
    fillTokenToTotalSheetDTOMap(TokenEnum.SPY, tokenToReportDTOMap, tokenToPriceMap, tokenToTotalSheetDTOMap);

    return tokenToTotalSheetDTOMap;
  }

  /**
   * 
   */
  private void fillDFITokenToTotalSheetDTOMap(
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, BigDecimal> tokenToPriceMap,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.trace("fillDFITokenToTotalSheetDTOMap()");

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    DFISheetDTO dfiSheetDTO = reportDTO.getDfiSheetDTO();

    BigDecimal interimDifference = dfiSheetDTO.getLockInterimDifference();
    BigDecimal totalDifference = dfiSheetDTO.getTotalDifference();

    fillTokenToTotalSheetDTOMap(token, interimDifference, totalDifference, tokenToPriceMap, tokenToTotalSheetDTOMap);
  }

  /**
   * 
   */
  private void fillDUSDTokenToTotalSheetDTOMap(
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, BigDecimal> tokenToPriceMap,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.trace("fillDUSDTokenToTotalSheetDTOMap()");

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    DUSDSheetDTO dusdSheetDTO = reportDTO.getDusdSheetDTO();

    BigDecimal interimDifference = dusdSheetDTO.getLockInterimDifference();
    BigDecimal totalDifference = dusdSheetDTO.getTotalDifference();

    fillTokenToTotalSheetDTOMap(token, interimDifference, totalDifference, tokenToPriceMap, tokenToTotalSheetDTOMap);
  }

  /**
   * 
   */
  private void fillTokenToTotalSheetDTOMap(
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, BigDecimal> tokenToPriceMap,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.trace("fillTokenToTotalSheetDTOMap()");

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    TokenSheetDTO tokenSheetDTO = reportDTO.getTokenSheetDTO();

    BigDecimal interimDifference = tokenSheetDTO.getLockInterimDifference();
    BigDecimal totalDifference = tokenSheetDTO.getTotalDifference();

    fillTokenToTotalSheetDTOMap(token, interimDifference, totalDifference, tokenToPriceMap, tokenToTotalSheetDTOMap);
  }

  /**
   * 
   */
  private void fillTokenToTotalSheetDTOMap(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal interimDifference,
      @Nonnull BigDecimal totalDifference,
      @Nonnull Map<TokenEnum, BigDecimal> tokenToPriceMap,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.trace("fillTokenToTotalSheetDTOMap()");

    BigDecimal price = tokenToPriceMap.getOrDefault(token, BigDecimal.ZERO);
    BigDecimal value = totalDifference.multiply(price, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    TotalSheetDTO totalSheetDTO = new TotalSheetDTO();

    totalSheetDTO.setInterimDifference(interimDifference);
    totalSheetDTO.setAmount(totalDifference);
    totalSheetDTO.setPrice(price);
    totalSheetDTO.setValue(value);

    tokenToTotalSheetDTOMap.put(token, totalSheetDTO);
  }

  /**
   * 
   */
  private BigDecimal getBTCPoolBTCAmount() {
    LOGGER.trace("getBTCPoolBTCAmount()");
    return getBTCPoolAmount().getLeft();
  }

  /**
   * 
   */
  private BigDecimal getBTCPoolDFIAmount() {
    LOGGER.trace("getBTCPoolDFIAmount()");
    return getBTCPoolAmount().getRight();
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getBTCPoolAmount() {
    LOGGER.trace("getBTCPoolAmount()");

    String poolToken = "BTC-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = btcLMTokenToAmountMap.getOrDefault(poolToken, BigDecimal.ZERO);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private BigDecimal getETHPoolETHAmount() {
    LOGGER.trace("getETHPoolETHAmount()");
    return getETHPoolAmount().getLeft();
  }

  /**
   * 
   */
  private BigDecimal getETHPoolDFIAmount() {
    LOGGER.trace("getETHPoolDFIAmount()");
    return getETHPoolAmount().getRight();
  }

  private Pair<BigDecimal, BigDecimal> getETHPoolAmount() {
    LOGGER.trace("getETHPoolAmount()");

    String poolToken = "ETH-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = ethLMTokenToAmountMap.getOrDefault(poolToken, BigDecimal.ZERO);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private BigDecimal getDUSDPoolDUSDAmount() {
    LOGGER.trace("getDUSDPoolDUSDAmount()");
    return getDUSDPoolAmount().getLeft();
  }

  /**
   * 
   */
  private BigDecimal getDUSDPoolDFIAmount() {
    LOGGER.trace("getDUSDPoolDFIAmount()");
    return getDUSDPoolAmount().getRight();
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getDUSDPoolAmount() {
    LOGGER.trace("getDUSDPoolAmount()");

    String poolToken = "DUSD-DFI";
    DefiPoolPairData poolPairData = dfiPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = dusdLMTokenToAmountMap.getOrDefault(poolToken, BigDecimal.ZERO);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private BigDecimal getUSDTPoolUSDTAmount() {
    LOGGER.trace("getUSDTPoolUSDTAmount()");
    return getUSDTPoolAmount().getLeft();
  }

  /**
   * 
   */
  private BigDecimal getUSDTPoolDUSDAmount() {
    LOGGER.trace("getUSDTPoolDUSDAmount()");
    return getUSDTPoolAmount().getRight();
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getUSDTPoolAmount() {
    LOGGER.trace("getUSDTPoolAmount()");

    String poolToken = "USDT-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = usdtLMTokenToAmountMap.getOrDefault(poolToken, BigDecimal.ZERO);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private BigDecimal getUSDCPoolUSDCAmount() {
    LOGGER.trace("getUSDCPoolUSDCAmount()");
    return getUSDCPoolAmount().getLeft();
  }

  /**
   * 
   */
  private BigDecimal getUSDCPoolDUSDAmount() {
    LOGGER.trace("getUSDCPoolDUSDAmount()");
    return getUSDCPoolAmount().getRight();
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getUSDCPoolAmount() {
    LOGGER.trace("getUSDCPoolAmount()");

    String poolToken = "USDC-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = usdcLMTokenToAmountMap.getOrDefault(poolToken, BigDecimal.ZERO);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private BigDecimal getEUROCPoolEUROCAmount() {
    LOGGER.trace("getEUROCPoolEUROCAmount()");
    return getEUROCPoolAmount().getLeft();
  }

  /**
   * 
   */
  private BigDecimal getEUROCPoolDUSDAmount() {
    LOGGER.trace("getEUROCPoolDUSDAmount()");
    return getEUROCPoolAmount().getRight();
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getEUROCPoolAmount() {
    LOGGER.trace("getEUROCPoolAmount()");

    String poolToken = "EUROC-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = eurocLMTokenToAmountMap.getOrDefault(poolToken, BigDecimal.ZERO);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private BigDecimal getSPYPoolSPYAmount() {
    LOGGER.trace("getSPYPoolSPYAmount()");
    return getSPYPoolAmount().getLeft();
  }

  /**
   * 
   */
  private BigDecimal getSPYPoolDUSDAmount() {
    LOGGER.trace("getSPYPoolDUSDAmount()");
    return getSPYPoolAmount().getRight();
  }

  /**
   * 
   */
  private Pair<BigDecimal, BigDecimal> getSPYPoolAmount() {
    LOGGER.trace("getSPYPoolAmount()");

    String poolToken = "SPY-DUSD";
    DefiPoolPairData poolPairData = dusdPoolTokenToPoolPairDataMap.get(poolToken);
    BigDecimal poolAmount = spyLMTokenToAmountMap.getOrDefault(poolToken, BigDecimal.ZERO);

    return TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, poolAmount);
  }

  /**
   * 
   */
  private BigDecimal getDexFee(@Nonnull DefiPoolPairData poolPairData) {

    BigDecimal dexFee = poolPairData.getDexFeePctTokenA();

    if (null == dexFee) {
      dexFee = poolPairData.getDexFeePctTokenB();
    }

    if (null == dexFee) {
      dexFee = BigDecimal.ZERO;
    }

    return dexFee;
  }

  /**
   * 
   */
  private void writeReport(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap,
      @Nonnull Set<Integer> historyHourSet,
      boolean isInternal) throws DfxException {
    LOGGER.debug("writeReport()");

    openExcel(rootPath, fileName);

    writeTokenSheet(reportingTimestamp, tokenToReportDTOMap);
    writeTransactionSheet(sheetIdToSheetNameMap, tokenToTransactionSheetDTOMap);
    writeTotalSheet(sheetIdToSheetNameMap, tokenToTotalSheetDTOMap);
    writeCustomerSheet(reportingTimestamp, sheetIdToSheetNameMap, depositAddressToStakingDTOMap);

    writeHistoryAmountSheet(sheetIdToSheetNameMap, historyHourSet);
    writeHistoryPriceSheet(sheetIdToSheetNameMap, historyHourSet);

    if (isInternal) {
      writeHistoryInterimDifferenceSheet(sheetIdToSheetNameMap, historyHourSet);
      writeHistoryAssetPriceSheet(sheetIdToSheetNameMap, historyHourSet);
    }

    closeExcel();
  }

  /**
   * 
   */
  private void writeTokenSheet(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("writeTokenSheet()");

    writeDFISheet(reportingTimestamp, TokenEnum.DFI, tokenToReportDTOMap);
    writeDUSDSheet(reportingTimestamp, TokenEnum.DUSD, tokenToReportDTOMap);
    writeTokenSheet(reportingTimestamp, TokenEnum.BTC, tokenToReportDTOMap);
    writeTokenSheet(reportingTimestamp, TokenEnum.ETH, tokenToReportDTOMap);
    writeTokenSheet(reportingTimestamp, TokenEnum.USDT, tokenToReportDTOMap);
    writeTokenSheet(reportingTimestamp, TokenEnum.USDC, tokenToReportDTOMap);
    writeTokenSheet(reportingTimestamp, TokenEnum.EUROC, tokenToReportDTOMap);
    writeTokenSheet(reportingTimestamp, TokenEnum.SPY, tokenToReportDTOMap);
  }

  /**
   * 
   */
  private void writeDFISheet(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("writeDFISheet(): token=" + token);

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    DFISheetDTO sheetDTO = reportDTO.getDfiSheetDTO();

    CellDataList cellDataList = cellListCreator.createDFICellDataList(reportingTimestamp, token, sheetDTO);

    String sheetName = sheetDTO.getSheetName();
    setSheet(sheetName);

    writeExcel(new RowDataList(0), cellDataList);
  }

  /**
   * 
   */
  private void writeDUSDSheet(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("writeDUSDSheet(): token=" + token);

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    DUSDSheetDTO sheetDTO = reportDTO.getDusdSheetDTO();

    CellDataList cellDataList = cellListCreator.createDUSDCellDataList(reportingTimestamp, token, sheetDTO);

    String sheetName = sheetDTO.getSheetName();
    setSheet(sheetName);

    writeExcel(new RowDataList(0), cellDataList);
  }

  /**
   * 
   */
  private void writeTokenSheet(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("writeTokenSheet(): token=" + token);

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    TokenSheetDTO sheetDTO = reportDTO.getTokenSheetDTO();

    CellDataList cellDataList;

    if (TokenEnum.SPY == token) {
      cellDataList = cellListCreator.createSPYCellDataList(reportingTimestamp, token, sheetDTO);
    } else {
      cellDataList = cellListCreator.createTokenCellDataList(reportingTimestamp, token, sheetDTO);
    }

    String sheetName = sheetDTO.getSheetName();
    setSheet(sheetName);

    writeExcel(new RowDataList(0), cellDataList);
  }

  /**
   * 
   */
  private void writeTransactionSheet(
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap) throws DfxException {
    LOGGER.trace("writeTransactionSheet()");

    CellDataList transactionsCellDataList = cellListCreator.createTransactionCellDataList(tokenToTransactionSheetDTOMap);

    String transactionSheetName = sheetIdToSheetNameMap.get(SheetEnum.TRANSACTION);
    setSheet(transactionSheetName);

    writeExcel(new RowDataList(0), transactionsCellDataList);
  }

  /**
   * 
   */
  private void writeTotalSheet(
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) throws DfxException {
    LOGGER.trace("writeTotalSheet()");

    CellDataList totalCellDataList = cellListCreator.createTotalCellDataList(tokenToTotalSheetDTOMap);

    String totalSheetName = sheetIdToSheetNameMap.get(SheetEnum.TOTAL);
    setSheet(totalSheetName);

    writeExcel(new RowDataList(0), totalCellDataList);
  }

  /**
   * 
   */
  private void writeCustomerSheet(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.trace("writeCustomerSheet()");

    RowDataList customerRowDataList = balanceReporting.createYieldmachineRowDataList(depositAddressToStakingDTOMap);

    CellDataList customerCellDataList = new CellDataList();

    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(reportingTimestamp));

    @SuppressWarnings("unchecked")
    Map<TokenEnum, BigDecimal> tokenToTotalBalanceMap =
        (EnumMap<TokenEnum, BigDecimal>) customerRowDataList.getProperty(BalanceReporting.TOTAL_BALANCE_PROPERTY);

    int cellIndex = 2;

    for (TokenEnum token : TokenEnum.values()) {
      // TODO: currently without SPY, coming later ...
      if (TokenEnum.SPY != token) {
        customerCellDataList.add(
            new CellData().setRowIndex(0).setCellIndex(cellIndex++).setKeepStyle(true).setValue(tokenToTotalBalanceMap.getOrDefault(token, BigDecimal.ZERO)));
      }
    }

    String customerSheetName = sheetIdToSheetNameMap.get(SheetEnum.CUSTOMER);
    setSheet(customerSheetName);

    cleanExcel(2);
    writeExcel(customerRowDataList, customerCellDataList);
  }

  /**
   * 
   */
  private void writeHistoryAmountSheet(
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.trace("writeHistoryAmountSheet()");

    RowDataList rowDataList = new RowDataList(1);

    HistoryAmountSheetDTOList historyAmountSheetDTOList =
        transparencyReportFileHelper.readHistoryAmountSheetDTOFromJSON(historyHourSet);

    for (HistoryAmountSheetDTO historyAmountSheetDTO : historyAmountSheetDTOList) {
      CellDataList historyAmountCellDataList = cellListCreator.createHistoryAmountCellDataList(historyAmountSheetDTO);

      RowData rowData = new RowData().addCellDataList(historyAmountCellDataList);
      rowDataList.add(rowData);
    }

    String historyAmountSheetName = sheetIdToSheetNameMap.get(SheetEnum.HISTORY_AMOUNT);
    setSheet(historyAmountSheetName);

    cleanExcel(1);
    writeExcel(rowDataList);
  }

  /**
   * 
   */
  private void writeHistoryInterimDifferenceSheet(
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.trace("writeHistoryInterimDifferenceSheet()");

    RowDataList rowDataList = new RowDataList(1);

    HistoryInterimDifferenceSheetDTOList historyInterimDifferenceSheetDTOList =
        transparencyReportFileHelper.readHistoryInterimDifferenceSheetDTOFromJSON(historyHourSet);

    for (HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTO : historyInterimDifferenceSheetDTOList) {
      CellDataList historyInterimDifferenceCellDataList = cellListCreator.createHistoryInterimDifferenceCellDataList(historyInterimDifferenceSheetDTO);

      RowData rowData = new RowData().addCellDataList(historyInterimDifferenceCellDataList);
      rowDataList.add(rowData);
    }

    String historyInterimDifferenceSheetName = sheetIdToSheetNameMap.get(SheetEnum.HISTORY_INTERIM_DIFFERENCE);
    setSheet(historyInterimDifferenceSheetName);

    cleanExcel(1);
    writeExcel(rowDataList);
  }

  /**
   * 
   */
  private void writeHistoryAssetPriceSheet(
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.trace("writeHistoryAssetPriceSheet()");

    RowDataList rowDataList = new RowDataList(1);

    HistoryAssetPriceSheetDTOList historyAssetPriceSheetDTOList =
        transparencyReportFileHelper.readHistoryAssetPriceSheetDTOFromJSON(historyHourSet);

    for (HistoryAssetPriceSheetDTO historyAssetPriceSheetDTO : historyAssetPriceSheetDTOList) {
      CellDataList historyAssetPriceCellDataList = cellListCreator.createHistoryAssetPriceCellDataList(historyAssetPriceSheetDTO);

      RowData rowData = new RowData().addCellDataList(historyAssetPriceCellDataList);
      rowDataList.add(rowData);
    }

    String historyPriceSheetName = sheetIdToSheetNameMap.get(SheetEnum.HISTORY_ASSET_PRICE);
    setSheet(historyPriceSheetName);

    cleanExcel(1);
    writeExcel(rowDataList);
  }

  /**
   * 
   */
  private void writeHistoryPriceSheet(
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Set<Integer> historyHourSet) throws DfxException {
    LOGGER.trace("writeHistoryPriceSheet()");

    RowDataList rowDataList = new RowDataList(1);

    HistoryPriceSheetDTOList historyPriceSheetDTOList =
        transparencyReportFileHelper.readHistoryPriceSheetDTOFromJSON(historyHourSet);

    for (HistoryPriceSheetDTO historyPriceSheetDTO : historyPriceSheetDTOList) {
      CellDataList historyPriceCellDataList = cellListCreator.createHistoryPriceCellDataList(historyPriceSheetDTO);

      RowData rowData = new RowData().addCellDataList(historyPriceCellDataList);
      rowDataList.add(rowData);
    }

    String historyPriceSheetName = sheetIdToSheetNameMap.get(SheetEnum.HISTORY_PRICE);
    setSheet(historyPriceSheetName);

    cleanExcel(1);
    writeExcel(rowDataList);
  }

  /**
   * 
   */
  private HistoryAmountSheetDTO createHistoryAmountSheetDTO(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.debug("createHistoryAmountSheetDTO()");

    HistoryAmountSheetDTO historyAmountSheetDTO = new HistoryAmountSheetDTO();
    historyAmountSheetDTO.setTimestamp(DATE_FORMAT.format(reportingTimestamp));

    for (TokenEnum token : TokenEnum.values()) {
      TotalSheetDTO totalSheetDTO = tokenToTotalSheetDTOMap.get(token);
      historyAmountSheetDTO.put(token, totalSheetDTO.getAmount());
    }

    return historyAmountSheetDTO;
  }

  /**
   * 
   */
  private HistoryInterimDifferenceSheetDTO createHistoryInterimDifferenceSheetDTO(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.debug("createHistoryInterimDifferenceSheetDTO()");

    HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTO = new HistoryInterimDifferenceSheetDTO();
    historyInterimDifferenceSheetDTO.setTimestamp(DATE_FORMAT.format(reportingTimestamp));

    for (TokenEnum token : TokenEnum.values()) {
      TotalSheetDTO totalSheetDTO = tokenToTotalSheetDTOMap.get(token);
      historyInterimDifferenceSheetDTO.put(token, totalSheetDTO.getInterimDifference());
    }

    return historyInterimDifferenceSheetDTO;
  }

  /**
   * 
   */
  private HistoryAssetPriceSheetDTO createHistoryAssetPriceSheetDTO(
      @Nonnull Timestamp reportingTimestamp,
      long blockNumber,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.debug("createHistoryAssetPriceSheetDTO()");

    HistoryAssetPriceSheetDTO historyAssetPriceSheetDTO = new HistoryAssetPriceSheetDTO();
    historyAssetPriceSheetDTO.setTimestamp(DATE_FORMAT.format(reportingTimestamp));
    historyAssetPriceSheetDTO.setBlockNumber(blockNumber);

    for (TokenEnum token : TokenEnum.values()) {
      TotalSheetDTO totalSheetDTO = tokenToTotalSheetDTOMap.get(token);
      historyAssetPriceSheetDTO.put(token, totalSheetDTO.getPrice());
    }

    return historyAssetPriceSheetDTO;
  }

  /**
   * 
   */
  private HistoryPriceSheetDTO createHistoryPriceSheetDTO(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.debug("createHistoryPriceSheetDTO()");

    HistoryPriceSheetDTO historyPriceSheetDTO = new HistoryPriceSheetDTO();
    historyPriceSheetDTO.setTimestamp(DATE_FORMAT.format(reportingTimestamp));

    for (TokenEnum token : TokenEnum.values()) {
      TotalSheetDTO totalSheetDTO = tokenToTotalSheetDTOMap.get(token);
      historyPriceSheetDTO.put(token, totalSheetDTO.getValue());
    }

    return historyPriceSheetDTO;
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
  private class CellListCreator {
    /**
     * 
     */
    private CellDataList createDFICellDataList(
        @Nonnull Timestamp currentTimestamp,
        @Nonnull TokenEnum token,
        @Nonnull DFISheetDTO dfiSheetDTO) throws DfxException {
      LOGGER.debug("createDFICellDataList(): token=" + token);

      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setRowIndex(1).setCellIndex(4).setKeepStyle(true).setValue(currentTimestamp));
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(0).setKeepStyle(true).setValue(dfiSheetDTO.getNumberOfCustomers()));
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getCustomerDeposits()));

      // ...
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLiquidityAmount()));
      cellDataList.add(new CellData().setRowIndex(5).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockRewardAmount()));

      // ...
      cellDataList.add(new CellData().setRowIndex(7).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockVault1Balance()));
      cellDataList.add(new CellData().setRowIndex(8).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockVault1Collateral()));
      cellDataList.add(new CellData().setRowIndex(9).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockVault2Balance()));
      cellDataList.add(new CellData().setRowIndex(10).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockVault2Collateral()));
      cellDataList.add(new CellData().setRowIndex(11).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockVault3Balance()));
      cellDataList.add(new CellData().setRowIndex(12).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockVault3Collateral()));

      // ...
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_BTC_DFI_Balance()));
      cellDataList.add(new CellData().setRowIndex(15).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_BTC_DFI_Pool()));
      cellDataList.add(new CellData().setRowIndex(16).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_ETH_DFI_Balance()));
      cellDataList.add(new CellData().setRowIndex(17).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_ETH_DFI_Pool()));
      cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_DUSD_DFI_Balance()));
      cellDataList.add(new CellData().setRowIndex(19).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_DUSD_DFI_Pool()));

      // ...
      cellDataList.add(new CellData().setRowIndex(20).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_USDT_DFI_Balance()));
      cellDataList.add(new CellData().setRowIndex(21).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_USDC_DFI_Balance()));
      cellDataList.add(new CellData().setRowIndex(22).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_EUROC_DFI_Balance()));
      cellDataList.add(new CellData().setRowIndex(23).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLock_LM_SPY_DFI_Balance()));

      // ...
      cellDataList.add(new CellData().setRowIndex(25).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getCustomerInterimBalance()));
      cellDataList.add(new CellData().setRowIndex(25).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockInterimBalance()));

      cellDataList.add(new CellData().setRowIndex(26).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockInterimDifference()));

      // ...
      cellDataList.add(new CellData().setRowIndex(29).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getLockChangeBTCTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(30).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getLockChangeETHTransactionBalance()));

      // ...
      cellDataList.add(new CellData().setRowIndex(32).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getCustomerTotalBalance()));
      cellDataList.add(new CellData().setRowIndex(32).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockTotalBalance()));

      cellDataList.add(new CellData().setRowIndex(33).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getTotalDifference()));

      return cellDataList;
    }

    /**
     * 
     */
    private CellDataList createDUSDCellDataList(
        @Nonnull Timestamp currentTimestamp,
        @Nonnull TokenEnum token,
        @Nonnull DUSDSheetDTO dusdSheetDTO) throws DfxException {
      LOGGER.debug("createDUSDCellDataList(): token=" + token);

      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setRowIndex(1).setCellIndex(4).setKeepStyle(true).setValue(currentTimestamp));
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(0).setKeepStyle(true).setValue(dusdSheetDTO.getNumberOfCustomers()));
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getCustomerDeposits()));

      // ...
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLiquidityAmount()));
      cellDataList.add(new CellData().setRowIndex(5).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockRewardAmount()));

      // ...
      cellDataList.add(new CellData().setRowIndex(7).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault1Loan()));
      cellDataList.add(new CellData().setRowIndex(7).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault1Balance()));
      cellDataList.add(new CellData().setRowIndex(8).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault2Loan()));
      cellDataList.add(new CellData().setRowIndex(8).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault1Collateral()));
      cellDataList.add(new CellData().setRowIndex(9).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault3Loan()));
      cellDataList.add(new CellData().setRowIndex(9).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault2Balance()));
      cellDataList.add(new CellData().setRowIndex(10).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault2Collateral()));
      cellDataList.add(new CellData().setRowIndex(11).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault3Balance()));
      cellDataList.add(new CellData().setRowIndex(12).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault3Collateral()));

      // ...
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_DUSD_DFI_Balance()));
      cellDataList.add(new CellData().setRowIndex(15).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_DUSD_DFI_Pool()));
      cellDataList.add(new CellData().setRowIndex(16).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_USDT_DUSD_Balance()));
      cellDataList.add(new CellData().setRowIndex(17).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_USDT_DUSD_Pool()));
      cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_USDC_DUSD_Balance()));
      cellDataList.add(new CellData().setRowIndex(19).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_USDC_DUSD_Pool()));
      cellDataList.add(new CellData().setRowIndex(20).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_EUROC_DUSD_Balance()));
      cellDataList.add(new CellData().setRowIndex(21).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_EUROC_DUSD_Pool()));
      cellDataList.add(new CellData().setRowIndex(22).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_SPY_DUSD_Balance()));
      cellDataList.add(new CellData().setRowIndex(23).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLock_LM_SPY_DUSD_Pool()));

      // ...
      cellDataList.add(new CellData().setRowIndex(25).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getCustomerInterimBalance()));
      cellDataList.add(new CellData().setRowIndex(25).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockInterimBalance()));

      cellDataList.add(new CellData().setRowIndex(26).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockInterimDifference()));

      // ...
      cellDataList.add(new CellData().setRowIndex(29).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockChangeUSDTTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(30).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockChangeUSDCTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(31).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockChangeEUROCTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(32).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockChangeSPYTransactionBalance()));

      // ...
      cellDataList.add(new CellData().setRowIndex(34).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getCustomerTotalBalance()));
      cellDataList.add(new CellData().setRowIndex(34).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockTotalBalance()));

      cellDataList.add(new CellData().setRowIndex(35).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getTotalDifference()));

      return cellDataList;
    }

    /**
     * 
     */
    private CellDataList createTokenCellDataList(
        @Nonnull Timestamp currentTimestamp,
        @Nonnull TokenEnum token,
        @Nonnull TokenSheetDTO tokenSheetDTO) throws DfxException {
      LOGGER.debug("createCellDataList(): token=" + token);

      // ...
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setRowIndex(1).setCellIndex(4).setKeepStyle(true).setValue(currentTimestamp));
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(0).setKeepStyle(true).setValue(tokenSheetDTO.getNumberOfCustomers()));
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(2).setKeepStyle(true).setValue(tokenSheetDTO.getCustomerDeposits()));

      // ...
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockLiquidityAmount()));
      cellDataList.add(new CellData().setRowIndex(5).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockRewardAmount()));

      // ...
      cellDataList.add(new CellData().setRowIndex(7).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault1Balance()));
      cellDataList.add(new CellData().setRowIndex(8).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault1Collateral()));
      cellDataList.add(new CellData().setRowIndex(9).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault2Balance()));
      cellDataList.add(new CellData().setRowIndex(10).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault2Collateral()));
      cellDataList.add(new CellData().setRowIndex(11).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault3Balance()));
      cellDataList.add(new CellData().setRowIndex(12).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault3Collateral()));

      // ...
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockLMBalance()));
      cellDataList.add(new CellData().setRowIndex(15).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockLMPool()));

      // ...
      cellDataList.add(new CellData().setRowIndex(17).setCellIndex(2).setKeepStyle(true).setValue(tokenSheetDTO.getCustomerInterimBalance()));
      cellDataList.add(new CellData().setRowIndex(17).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockInterimBalance()));

      cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockInterimDifference()));

      // ...
      cellDataList.add(new CellData().setRowIndex(21).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockChangeTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(22).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getTotalDifference()));

      return cellDataList;
    }

    /**
     * 
     */
    private CellDataList createSPYCellDataList(
        @Nonnull Timestamp currentTimestamp,
        @Nonnull TokenEnum token,
        @Nonnull TokenSheetDTO tokenSheetDTO) throws DfxException {
      LOGGER.debug("createSPYCellDataList(): token=" + token);

      // ...
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setRowIndex(1).setCellIndex(4).setKeepStyle(true).setValue(currentTimestamp));
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(0).setKeepStyle(true).setValue(tokenSheetDTO.getNumberOfCustomers()));
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(2).setKeepStyle(true).setValue(tokenSheetDTO.getCustomerDeposits()));

      // ...
      cellDataList.add(new CellData().setRowIndex(4).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockLiquidityAmount()));
      cellDataList.add(new CellData().setRowIndex(5).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockRewardAmount()));

      // ...
      cellDataList.add(new CellData().setRowIndex(7).setCellIndex(2).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault1Loan()));
      cellDataList.add(new CellData().setRowIndex(7).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault1Balance()));
      cellDataList.add(new CellData().setRowIndex(8).setCellIndex(2).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault2Loan()));
      cellDataList.add(new CellData().setRowIndex(8).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault2Balance()));
      cellDataList.add(new CellData().setRowIndex(9).setCellIndex(2).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault3Loan()));
      cellDataList.add(new CellData().setRowIndex(9).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockVault3Balance()));

      // ...
      cellDataList.add(new CellData().setRowIndex(11).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockLMBalance()));
      cellDataList.add(new CellData().setRowIndex(12).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockLMPool()));

      // ...
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(2).setKeepStyle(true).setValue(tokenSheetDTO.getCustomerInterimBalance()));
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockInterimBalance()));

      cellDataList.add(new CellData().setRowIndex(15).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockInterimDifference()));

      // ...
      cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockChangeTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(19).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getTotalDifference()));

      return cellDataList;
    }

    /**
     *
     */
    private CellDataList createTransactionCellDataList(
        @Nonnull Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap) {
      LOGGER.debug("createTransactionCellDataList()");

      // ...
      TransactionSheetDTO btcTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.BTC);
      TransactionSheetDTO ethTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.ETH);
      TransactionSheetDTO usdtTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.USDT);
      TransactionSheetDTO usdcTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.USDC);
      TransactionSheetDTO eurocTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.EUROC);
      TransactionSheetDTO spyTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.SPY);

      // ...
      CellDataList cellDataList = new CellDataList();

      fillTransactionCellDataList(btcTransactionSheetDTO, 6, cellDataList);
      fillTransactionCellDataList(ethTransactionSheetDTO, 7, cellDataList);
      fillTransactionCellDataList(usdtTransactionSheetDTO, 8, cellDataList);
      fillTransactionCellDataList(usdcTransactionSheetDTO, 9, cellDataList);
      fillTransactionCellDataList(eurocTransactionSheetDTO, 10, cellDataList);
      fillTransactionCellDataList(spyTransactionSheetDTO, 11, cellDataList);

      return cellDataList;
    }

    /**
     * 
     */
    private void fillTransactionCellDataList(
        @Nonnull TransactionSheetDTO transactionSheetDTO,
        int rowIndex,
        @Nonnull CellDataList cellDataList) {
      LOGGER.trace("fillTransactionCellDataList()");

      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(1).setKeepStyle(true).setValue(transactionSheetDTO.getDifference()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(4).setKeepStyle(true).setValue(transactionSheetDTO.getTotalLiquidityPoolToken()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(5).setKeepStyle(true).setValue(transactionSheetDTO.getTotalPoolTokenA()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(6).setKeepStyle(true).setValue(transactionSheetDTO.getTotalPoolTokenB()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(7).setKeepStyle(true).setValue(transactionSheetDTO.getPoolTokenRatio()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(9).setKeepStyle(true).setValue(transactionSheetDTO.getSwapFee()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(10).setKeepStyle(true).setValue(transactionSheetDTO.getDexFee()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(11).setKeepStyle(true).setValue(transactionSheetDTO.getInput()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(13).setKeepStyle(true).setValue(transactionSheetDTO.getOutput()));
    }

    /**
     * 
     */
    private CellDataList createTotalCellDataList(@Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
      LOGGER.debug("createTotalCellDataList()");

      // ...
      CellDataList cellDataList = new CellDataList();

      // ...
      BigDecimal totalBalance = BigDecimal.ZERO;

      int rowIndex = 1;

      for (TokenEnum token : TokenEnum.values()) {
        TotalSheetDTO totalSheetDTO = tokenToTotalSheetDTOMap.get(token);
        totalBalance = totalBalance.add(totalSheetDTO.getValue());

        fillTotalCellDataList(totalSheetDTO, rowIndex++, cellDataList);
      }

      rowIndex++;

      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(3).setKeepStyle(true).setValue(totalBalance));

      return cellDataList;
    }

    /**
     * 
     */
    private void fillTotalCellDataList(
        @Nonnull TotalSheetDTO totalSheetDTO,
        int rowIndex,
        @Nonnull CellDataList cellDataList) {
      LOGGER.trace("fillTotalCellDataList()");

      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(1).setKeepStyle(true).setValue(totalSheetDTO.getAmount()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(2).setKeepStyle(true).setValue(totalSheetDTO.getPrice()));
      cellDataList.add(new CellData().setRowIndex(rowIndex).setCellIndex(3).setKeepStyle(true).setValue(totalSheetDTO.getValue()));
    }

    /**
     * 
     */
    private CellDataList createHistoryAmountCellDataList(@Nonnull HistoryAmountSheetDTO historyAmountSheetDTO) {
      LOGGER.trace("createHistoryAmountCellDataList()");

      // ...
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setCellIndex(0).setValue(historyAmountSheetDTO.getTimestamp()));

      int i = 1;

      for (TokenEnum token : TokenEnum.values()) {
        cellDataList.add(new CellData().setCellIndex(i++).setValue(historyAmountSheetDTO.getAmount(token)));
      }

      return cellDataList;
    }

    /**
     * 
     */
    private CellDataList createHistoryInterimDifferenceCellDataList(@Nonnull HistoryInterimDifferenceSheetDTO historyInterimDifferenceSheetDTO) {
      LOGGER.trace("createHistoryInterimDifferenceCellDataList()");

      // ...
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setCellIndex(0).setValue(historyInterimDifferenceSheetDTO.getTimestamp()));

      int i = 1;

      for (TokenEnum token : TokenEnum.values()) {
        cellDataList.add(new CellData().setCellIndex(i++).setValue(historyInterimDifferenceSheetDTO.getInterimDifference(token)));
      }

      return cellDataList;
    }

    /**
     * 
     */
    private CellDataList createHistoryAssetPriceCellDataList(@Nonnull HistoryAssetPriceSheetDTO historyAssetPriceSheetDTO) {
      LOGGER.trace("createHistoryAssetPriceCellDataList()");

      // ...
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setCellIndex(0).setValue(historyAssetPriceSheetDTO.getTimestamp()));

      int i = 1;

      for (TokenEnum token : TokenEnum.values()) {
        BigDecimal price = historyAssetPriceSheetDTO.getPrice(token);
        cellDataList.add(new CellData().setCellIndex(i++).setValue(price));
      }

      cellDataList.add(new CellData().setCellIndex(i++).setValue(historyAssetPriceSheetDTO.getBlockNumber()));

      return cellDataList;
    }

    /**
     * 
     */
    private CellDataList createHistoryPriceCellDataList(@Nonnull HistoryPriceSheetDTO historyPriceSheetDTO) {
      LOGGER.trace("createHistoryPriceCellDataList()");

      // ...
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setCellIndex(0).setValue(historyPriceSheetDTO.getTimestamp()));

      int i = 1;
      BigDecimal totalPrice = BigDecimal.ZERO;

      for (TokenEnum token : TokenEnum.values()) {
        BigDecimal price = historyPriceSheetDTO.getPrice(token);
        totalPrice = totalPrice.add(price);

        cellDataList.add(new CellData().setCellIndex(i++).setValue(price));
      }

      cellDataList.add(new CellData().setCellIndex(i++).setValue(totalPrice));

      return cellDataList;
    }
  }
}
