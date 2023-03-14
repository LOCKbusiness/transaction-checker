package ch.dfx.reporting.transparency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ch.dfx.reporting.transparency.data.DFISheetDTO;
import ch.dfx.reporting.transparency.data.DUSDSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryAmountSheetDTO;
import ch.dfx.reporting.transparency.data.HistoryPriceSheetDTO;
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
    HISTORY_PRICE
  }

  // ...
  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  private static final BigDecimal SIGN_CHANGER = new BigDecimal(-1);

  private static final String TOTAL_DFI_BALANCE_PROPERTY = "TOTAL_DFI_BALANCE";
  private static final String TOTAL_DUSD_BALANCE_PROPERTY = "TOTAL_DUSD_BALANCE";
  private static final String TOTAL_BTC_BALANCE_PROPERTY = "TOTAL_BTC_BALANCE";
  private static final String TOTAL_ETH_BALANCE_PROPERTY = "TOTAL_ETH_BALANCE";
  private static final String TOTAL_USDT_BALANCE_PROPERTY = "TOTAL_USDT_BALANCE";
  private static final String TOTAL_USDC_BALANCE_PROPERTY = "TOTAL_USDC_BALANCE";
  private static final String TOTAL_SPY_BALANCE_PROPERTY = "TOTAL_SPY_BALANCE";

  // ...
  private final DefiDataProvider dataProvider;

  private final TransparencyReportPriceHelper transparencyReportPriceHelper;
  private final TransparencyReportCsvHelper transparencyReportCsvHelper;

  private final CellListCreator cellListCreator;

  // ...
  private Timestamp currentTimestamp = null;

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
  private Map<String, BigDecimal> spyLMTokenToAmountMap = null;

  private Map<String, DefiPoolPairData> dfiPoolTokenToPoolPairDataMap = null;
  private Map<String, DefiPoolPairData> dusdPoolTokenToPoolPairDataMap = null;

//  private Map<String, BigDecimal> activePriceMap = null;

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
    this.transparencyReportCsvHelper = new TransparencyReportCsvHelper();

    this.cellListCreator = new CellListCreator();
  }

  /**
   * 
   */
  public void report(
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Map<TokenEnum, String> tokenToSheetNameMap) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      setup();

      // ...
      Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap = getDepositAddressToStakingDTOMap();

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
          .put(TokenEnum.SPY, createReportDTO(TokenEnum.SPY, tokenToSheetNameMap, depositAddressToStakingDTOMap));

      // ...
      Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap = createTokenToTransactionSheetDTOMap(tokenToReportDTOMap);

      // ...
      syncSheetData(TokenEnum.BTC, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.ETH, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.USDT, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.USDC, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncSheetData(TokenEnum.SPY, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);

      syncDFISheetData(tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
      syncDUSDSheetData(tokenToReportDTOMap, tokenToTransactionSheetDTOMap);

      // ...
      Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap = createTokenToTotalSheetDTOMap(tokenToReportDTOMap);

      // ...
      writeReport(
          rootPath, fileName, sheetIdToSheetNameMap,
          tokenToReportDTOMap, tokenToTransactionSheetDTOMap, tokenToTotalSheetDTOMap, depositAddressToStakingDTOMap);

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
    TransactionSheetDTO spyTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.SPY);

    BigDecimal usdtDUSDInput = usdtTransactionSheetDTO.getInput();
    BigDecimal usdcDUSDInput = usdcTransactionSheetDTO.getInput();
    BigDecimal spyDUSDInput = spyTransactionSheetDTO.getInput();

    dusdSheetDTO.setLockChangeUSDTTransactionBalance(usdtDUSDInput);
    dusdSheetDTO.setLockChangeUSDCTransactionBalance(usdcDUSDInput);
    dusdSheetDTO.setLockChangeSPYTransactionBalance(spyDUSDInput);

    // ...
    BigDecimal customerInterimBalance = dusdSheetDTO.getCustomerInterimBalance();
    BigDecimal totalDUSDInput = usdtDUSDInput.add(usdcDUSDInput).add(spyDUSDInput);

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

    if (null == currentTimestamp) {
      currentTimestamp = new Timestamp(System.currentTimeMillis());
    }

    setupLiquidity();
    setupVault();
    setupLiquidMining();
    setupLiquidMiningPoolInfo();
//    setupPriceFeed();
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

//  /**
//   * 
//   */
//  private void setupPriceFeed() throws DfxException {
//    LOGGER.trace("setupPriceFeed()");
//
//    Set<String> priceFeedTokenMap = new HashSet<>();
//
//    for (TokenEnum token : TokenEnum.values()) {
//      priceFeedTokenMap.add(token.toString());
//    }
//
//    activePriceMap = dataProvider.getActivePriceMap(priceFeedTokenMap);
//  }

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
    BigDecimal spyLMDFIAmount = spyLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    lockInterimBalance = lockInterimBalance.add(spyLMDFIAmount);

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

    dfiSheetDTO.setLockLMBalance1(btcLMDFIAmount);
    dfiSheetDTO.setLockLMPool1(btcPoolDFIAmount);
    dfiSheetDTO.setLockLMBalance2(ethLMDFIAmount);
    dfiSheetDTO.setLockLMPool2(ethPoolDFIAmount);
    dfiSheetDTO.setLockLMBalance3(dusdLMDFIAmount);
    dfiSheetDTO.setLockLMPool3(dusdPoolDFIAmount);
    dfiSheetDTO.setLockLMBalance4(usdtLMDFIAmount);
    dfiSheetDTO.setLockLMBalance5(usdcLMDFIAmount);
    dfiSheetDTO.setLockLMBalance6(spyLMDFIAmount);

    dfiSheetDTO.setCustomerInterimBalance(customerInterimBalance);
    dfiSheetDTO.setLockInterimBalance(lockInterimBalance);

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
    BigDecimal spyLMDUSDAmount = spyLMTokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal spyPoolDUSDAmount = getSPYPoolDUSDAmount();

    lockInterimBalance = lockInterimBalance.add(spyLMDUSDAmount);
    lockInterimBalance = lockInterimBalance.add(spyPoolDUSDAmount);

    // ...
    DUSDSheetDTO dusdSheetDTO = new DUSDSheetDTO();
    dusdSheetDTO.setSheetName(sheetName);

    dusdSheetDTO.setNumberOfCustomers(numberOfCustomer);
    dusdSheetDTO.setCustomerDeposits(customerDeposits);

    dusdSheetDTO.setLockLiquidityAmount(liquidityAmount);
    dusdSheetDTO.setLockRewardAmount(rewardAmount);

    dusdSheetDTO.setLockVault1Balance(vault1Amount);
    dusdSheetDTO.setLockVault1Collateral(vault1Collateral);
    dusdSheetDTO.setLockVault2Balance(vault2Amount);
    dusdSheetDTO.setLockVault2Collateral(vault2Collateral);
    dusdSheetDTO.setLockVault3Balance(vault3Amount);
    dusdSheetDTO.setLockVault3Collateral(vault3Collateral);

    dusdSheetDTO.setLockLMBalance1(dusdLMDUSDAmount);
    dusdSheetDTO.setLockLMPool1(dusdPoolDUSDAmount);
    dusdSheetDTO.setLockLMBalance2(usdtLMDUSDAmount);
    dusdSheetDTO.setLockLMPool2(usdtPoolDUSDAmount);
    dusdSheetDTO.setLockLMBalance3(usdcLMDUSDAmount);
    dusdSheetDTO.setLockLMPool3(usdcPoolDUSDAmount);
    dusdSheetDTO.setLockLMBalance4(spyLMDUSDAmount);
    dusdSheetDTO.setLockLMPool4(spyPoolDUSDAmount);

    dusdSheetDTO.setCustomerInterimBalance(customerInterimBalance);
    dusdSheetDTO.setLockInterimBalance(lockInterimBalance);

    dusdSheetDTO.setLockChangeUSDTTransactionBalance(BigDecimal.ZERO);
    dusdSheetDTO.setLockChangeUSDCTransactionBalance(BigDecimal.ZERO);
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
    BigDecimal vault1Loan = BigDecimal.ZERO;
    BigDecimal vault2Loan = BigDecimal.ZERO;
    BigDecimal vault3Loan = BigDecimal.ZERO;

    if (TokenEnum.SPY == token) {
      vault1Loan = vault1TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);
      vault2Loan = vault2TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);
      vault3Loan = vault3TokenToVaultLoanMap.getOrDefault(token.toString(), BigDecimal.ZERO);

      customerInterimBalance = customerInterimBalance.add(vault1Loan);
      customerInterimBalance = customerInterimBalance.add(vault2Loan);
      customerInterimBalance = customerInterimBalance.add(vault3Loan);
    }

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
    DefiPoolPairData spyDUSDPoolPairData = dusdPoolTokenToPoolPairDataMap.get("SPY-DUSD");

    // ...
    Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap = new EnumMap<>(TokenEnum.class);

    fillTokenToTransactionSheetDTOMap(TokenEnum.BTC, btcDFIPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
    fillTokenToTransactionSheetDTOMap(TokenEnum.ETH, ethDFIPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
    fillTokenToTransactionSheetDTOMap(TokenEnum.USDT, usdtDUSDPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
    fillTokenToTransactionSheetDTOMap(TokenEnum.USDC, usdcDUSDPoolPairData, tokenToReportDTOMap, tokenToTransactionSheetDTOMap);
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
  private Map<TokenEnum, TotalSheetDTO> createTokenToTotalSheetDTOMap(@Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) {
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

    BigDecimal totalDifference = dfiSheetDTO.getTotalDifference();

    fillTokenToTotalSheetDTOMap(token, totalDifference, tokenToPriceMap, tokenToTotalSheetDTOMap);
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

    BigDecimal totalDifference = dusdSheetDTO.getTotalDifference();

    fillTokenToTotalSheetDTOMap(token, totalDifference, tokenToPriceMap, tokenToTotalSheetDTOMap);
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

    BigDecimal totalDifference = tokenSheetDTO.getTotalDifference();

    fillTokenToTotalSheetDTOMap(token, totalDifference, tokenToPriceMap, tokenToTotalSheetDTOMap);
  }

  /**
   * 
   */
  private void fillTokenToTotalSheetDTOMap(
      @Nonnull TokenEnum token,
      @Nonnull BigDecimal totalDifference,
      @Nonnull Map<TokenEnum, BigDecimal> tokenToPriceMap,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.trace("fillTokenToTotalSheetDTOMap()");

    BigDecimal price = tokenToPriceMap.get(token);
    BigDecimal value = totalDifference.multiply(price, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);

    TotalSheetDTO totalSheetDTO = new TotalSheetDTO();

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
    BigDecimal poolAmount = btcLMTokenToAmountMap.get(poolToken);

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
    BigDecimal poolAmount = ethLMTokenToAmountMap.get(poolToken);

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
    BigDecimal poolAmount = dusdLMTokenToAmountMap.get(poolToken);

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
    BigDecimal poolAmount = usdtLMTokenToAmountMap.get(poolToken);

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
    BigDecimal poolAmount = usdcLMTokenToAmountMap.get(poolToken);

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
    BigDecimal poolAmount = spyLMTokenToAmountMap.get(poolToken);

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
  private Multimap<Integer, StakingDTO> getDepositAddressToStakingDTOMap() throws DfxException {
    LOGGER.debug("getDepositAddressToStakingDTOMap()");

    // ...
    List<StakingDTO> stakingDTOList = new ArrayList<>();

    for (TokenEnum token : TokenEnum.values()) {
      stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(token));
    }

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
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap,
      @Nonnull Map<TokenEnum, TransactionSheetDTO> tokenToTransactionSheetDTOMap,
      @Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("writeReport()");

    openExcel(rootPath, fileName);

    // ...
    writeTokenSheet(tokenToReportDTOMap);
    writeTransactionSheet(sheetIdToSheetNameMap, tokenToTransactionSheetDTOMap);
    writeTotalSheet(sheetIdToSheetNameMap, tokenToTotalSheetDTOMap);
    writeCustomerSheet(sheetIdToSheetNameMap, depositAddressToStakingDTOMap);

    // ...
    HistoryAmountSheetDTO historyAmountSheetDTO = createHistoryAmountSheetDTO(tokenToTotalSheetDTOMap);
    HistoryPriceSheetDTO historyPriceSheetDTO = createHistoryPriceSheetDTO(tokenToTotalSheetDTOMap);
    transparencyReportCsvHelper.writeToCSV(historyAmountSheetDTO, historyPriceSheetDTO);

    writeHistoryAmountSheet(sheetIdToSheetNameMap);
    writeHistoryPriceSheet(sheetIdToSheetNameMap);

    closeExcel();
  }

  /**
   * 
   */
  private void writeTokenSheet(@Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("writeTokenSheet()");

    writeDFISheet(TokenEnum.DFI, tokenToReportDTOMap);
    writeDUSDSheet(TokenEnum.DUSD, tokenToReportDTOMap);
    writeTokenSheet(TokenEnum.BTC, tokenToReportDTOMap);
    writeTokenSheet(TokenEnum.ETH, tokenToReportDTOMap);
    writeTokenSheet(TokenEnum.USDT, tokenToReportDTOMap);
    writeTokenSheet(TokenEnum.USDC, tokenToReportDTOMap);
    writeTokenSheet(TokenEnum.SPY, tokenToReportDTOMap);
  }

  /**
   * 
   */
  private void writeDFISheet(
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("writeDFISheet(): token=" + token);

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    DFISheetDTO sheetDTO = reportDTO.getDfiSheetDTO();

    CellDataList cellDataList = cellListCreator.createDFICellDataList(currentTimestamp, token, sheetDTO);

    String sheetName = sheetDTO.getSheetName();
    setSheet(sheetName);

    writeExcel(new RowDataList(0), cellDataList);
  }

  /**
   * 
   */
  private void writeDUSDSheet(
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("writeDUSDSheet(): token=" + token);

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    DUSDSheetDTO sheetDTO = reportDTO.getDusdSheetDTO();

    CellDataList cellDataList = cellListCreator.createDUSDCellDataList(currentTimestamp, token, sheetDTO);

    String sheetName = sheetDTO.getSheetName();
    setSheet(sheetName);

    writeExcel(new RowDataList(0), cellDataList);
  }

  /**
   * 
   */
  private void writeTokenSheet(
      @Nonnull TokenEnum token,
      @Nonnull Map<TokenEnum, ReportDTO> tokenToReportDTOMap) throws DfxException {
    LOGGER.trace("writeTokenSheet(): token=" + token);

    ReportDTO reportDTO = tokenToReportDTOMap.get(token);
    TokenSheetDTO sheetDTO = reportDTO.getTokenSheetDTO();

    CellDataList cellDataList;

    if (TokenEnum.SPY == token) {
      cellDataList = cellListCreator.createSPYCellDataList(currentTimestamp, token, sheetDTO);
    } else {
      cellDataList = cellListCreator.createTokenCellDataList(currentTimestamp, token, sheetDTO);
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
      @Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.trace("writeCustomerSheet()");

    RowDataList customerRowDataList = createCustomerRowDataList(depositAddressToStakingDTOMap);

    BigDecimal dfiTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_DFI_BALANCE_PROPERTY);
    BigDecimal dusdTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_DUSD_BALANCE_PROPERTY);
    BigDecimal btcTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_BTC_BALANCE_PROPERTY);
    BigDecimal ethTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_ETH_BALANCE_PROPERTY);
    BigDecimal usdtTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_USDT_BALANCE_PROPERTY);
    BigDecimal usdcTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_USDC_BALANCE_PROPERTY);
    BigDecimal spyTotalBalance = (BigDecimal) customerRowDataList.getProperty(TOTAL_SPY_BALANCE_PROPERTY);

    CellDataList customerCellDataList = new CellDataList();

    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(currentTimestamp));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(dfiTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(3).setKeepStyle(true).setValue(dusdTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(4).setKeepStyle(true).setValue(btcTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(5).setKeepStyle(true).setValue(ethTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(6).setKeepStyle(true).setValue(usdtTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(7).setKeepStyle(true).setValue(usdcTotalBalance));
    customerCellDataList.add(new CellData().setRowIndex(0).setCellIndex(8).setKeepStyle(true).setValue(spyTotalBalance));

    String customerSheetName = sheetIdToSheetNameMap.get(SheetEnum.CUSTOMER);
    setSheet(customerSheetName);

    cleanExcel(2);
    writeExcel(customerRowDataList, customerCellDataList);
  }

  /**
   * 
   */
  private void writeHistoryAmountSheet(@Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap) throws DfxException {
    LOGGER.trace("writeHistoryAmountSheet()");

    RowDataList rowDataList = new RowDataList(1);

    List<HistoryAmountSheetDTO> historyAmountSheetDTOList = transparencyReportCsvHelper.readHistoryAmountSheetDTOFromCSV();

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
  private void writeHistoryPriceSheet(@Nonnull Map<SheetEnum, String> sheetIdToSheetNameMap) throws DfxException {
    LOGGER.trace("writeHistoryPriceSheet()");

    RowDataList rowDataList = new RowDataList(1);

    List<HistoryPriceSheetDTO> historyPriceSheetDTOList = transparencyReportCsvHelper.readHistoryPriceSheetDTOFromCSV();

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
  private HistoryAmountSheetDTO createHistoryAmountSheetDTO(@Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.debug("createHistoryAmountSheetDTO()");

    HistoryAmountSheetDTO historyAmountSheetDTO = new HistoryAmountSheetDTO(currentTimestamp);

    for (TokenEnum token : TokenEnum.values()) {
      TotalSheetDTO totalSheetDTO = tokenToTotalSheetDTOMap.get(token);
      historyAmountSheetDTO.put(token, totalSheetDTO.getAmount());
    }

    return historyAmountSheetDTO;
  }

  /**
   * 
   */
  private HistoryPriceSheetDTO createHistoryPriceSheetDTO(@Nonnull Map<TokenEnum, TotalSheetDTO> tokenToTotalSheetDTOMap) {
    LOGGER.debug("createHistoryPriceSheetDTO()");

    HistoryPriceSheetDTO historyPriceSheetDTO = new HistoryPriceSheetDTO(currentTimestamp);

    for (TokenEnum token : TokenEnum.values()) {
      TotalSheetDTO totalSheetDTO = tokenToTotalSheetDTOMap.get(token);
      historyPriceSheetDTO.put(token, totalSheetDTO.getValue());
    }

    return historyPriceSheetDTO;
  }

  /**
   * 
   */
  private RowDataList createCustomerRowDataList(@Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("createCustomerRowDataList()");

    RowDataList rowDataList = new RowDataList(2);

    BigDecimal totalDFIBalance = BigDecimal.ZERO;
    BigDecimal totalDUSDBalance = BigDecimal.ZERO;
    BigDecimal totalBTCBalance = BigDecimal.ZERO;
    BigDecimal totalETHBalance = BigDecimal.ZERO;
    BigDecimal totalUSDTBalance = BigDecimal.ZERO;
    BigDecimal totalUSDCBalance = BigDecimal.ZERO;
    BigDecimal totalSPYBalance = BigDecimal.ZERO;

    for (Integer depositAddressNumber : depositAddressToStakingDTOMap.keySet()) {
      List<StakingDTO> stakingDTOList = (List<StakingDTO>) depositAddressToStakingDTOMap.get(depositAddressNumber);

      StakingDTO firstStakingDTO = stakingDTOList.get(0);

      RowData rowData = new RowData();
      rowData.addCellData(new CellData().setValue(firstStakingDTO.getCustomerAddress()));
      rowData.addCellData(new CellData().setValue(firstStakingDTO.getDepositAddress()));

      BigDecimal dfiBalance = BigDecimal.ZERO;
      BigDecimal dusdBalance = BigDecimal.ZERO;
      BigDecimal btcBalance = BigDecimal.ZERO;
      BigDecimal ethBalance = BigDecimal.ZERO;
      BigDecimal usdtBalance = BigDecimal.ZERO;
      BigDecimal usdcBalance = BigDecimal.ZERO;
      BigDecimal spyBalance = BigDecimal.ZERO;

      for (StakingDTO stakingDTO : stakingDTOList) {
        if (TokenEnum.DFI.getNumber() == stakingDTO.getTokenNumber()) {
          dfiBalance = stakingDTO.getVin().subtract(stakingDTO.getVout());
          totalDFIBalance = totalDFIBalance.add(dfiBalance);
        } else if (TokenEnum.DUSD.getNumber() == stakingDTO.getTokenNumber()) {
          dusdBalance = stakingDTO.getVin().subtract(stakingDTO.getVout());
          totalDUSDBalance = totalDUSDBalance.add(dusdBalance);
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
        } else if (TokenEnum.SPY.getNumber() == stakingDTO.getTokenNumber()) {
          spyBalance = stakingDTO.getVin().subtract(stakingDTO.getVout());
          totalSPYBalance = totalSPYBalance.add(spyBalance);
        }
      }

      rowData.addCellData(new CellData().setValue(dfiBalance));
      rowData.addCellData(new CellData().setValue(dusdBalance));
      rowData.addCellData(new CellData().setValue(btcBalance));
      rowData.addCellData(new CellData().setValue(ethBalance));
      rowData.addCellData(new CellData().setValue(usdtBalance));
      rowData.addCellData(new CellData().setValue(usdcBalance));
      rowData.addCellData(new CellData().setValue(spyBalance));

      rowDataList.add(rowData);
    }

    rowDataList.addProperty(TOTAL_DFI_BALANCE_PROPERTY, totalDFIBalance);
    rowDataList.addProperty(TOTAL_DUSD_BALANCE_PROPERTY, totalDUSDBalance);
    rowDataList.addProperty(TOTAL_BTC_BALANCE_PROPERTY, totalBTCBalance);
    rowDataList.addProperty(TOTAL_ETH_BALANCE_PROPERTY, totalETHBalance);
    rowDataList.addProperty(TOTAL_USDT_BALANCE_PROPERTY, totalUSDTBalance);
    rowDataList.addProperty(TOTAL_USDC_BALANCE_PROPERTY, totalUSDCBalance);
    rowDataList.addProperty(TOTAL_SPY_BALANCE_PROPERTY, totalSPYBalance);

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
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMBalance1()));
      cellDataList.add(new CellData().setRowIndex(15).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMPool1()));
      cellDataList.add(new CellData().setRowIndex(16).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMBalance2()));
      cellDataList.add(new CellData().setRowIndex(17).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMPool2()));
      cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMBalance3()));
      cellDataList.add(new CellData().setRowIndex(19).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMPool3()));

      // ...
      cellDataList.add(new CellData().setRowIndex(20).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMBalance4()));
      cellDataList.add(new CellData().setRowIndex(21).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMBalance5()));
      cellDataList.add(new CellData().setRowIndex(22).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockLMBalance6()));

      // ...
      cellDataList.add(new CellData().setRowIndex(24).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getCustomerInterimBalance()));
      cellDataList.add(new CellData().setRowIndex(24).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockInterimBalance()));

      // ...
      cellDataList.add(new CellData().setRowIndex(27).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getLockChangeBTCTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(28).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getLockChangeETHTransactionBalance()));

      // ...
      cellDataList.add(new CellData().setRowIndex(30).setCellIndex(2).setKeepStyle(true).setValue(dfiSheetDTO.getCustomerTotalBalance()));
      cellDataList.add(new CellData().setRowIndex(30).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getLockTotalBalance()));

      cellDataList.add(new CellData().setRowIndex(31).setCellIndex(5).setKeepStyle(true).setValue(dfiSheetDTO.getTotalDifference()));

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
      cellDataList.add(new CellData().setRowIndex(7).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault1Balance()));
      cellDataList.add(new CellData().setRowIndex(8).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault1Collateral()));
      cellDataList.add(new CellData().setRowIndex(9).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault2Balance()));
      cellDataList.add(new CellData().setRowIndex(10).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault2Collateral()));
      cellDataList.add(new CellData().setRowIndex(11).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault3Balance()));
      cellDataList.add(new CellData().setRowIndex(12).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockVault3Collateral()));

      // ...
      cellDataList.add(new CellData().setRowIndex(14).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLMBalance1()));
      cellDataList.add(new CellData().setRowIndex(15).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLMPool1()));
      cellDataList.add(new CellData().setRowIndex(16).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLMBalance2()));
      cellDataList.add(new CellData().setRowIndex(17).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLMPool2()));
      cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLMBalance3()));
      cellDataList.add(new CellData().setRowIndex(19).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLMPool3()));
      cellDataList.add(new CellData().setRowIndex(20).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLMBalance4()));
      cellDataList.add(new CellData().setRowIndex(21).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockLMPool4()));

      // ...
      cellDataList.add(new CellData().setRowIndex(23).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getCustomerInterimBalance()));
      cellDataList.add(new CellData().setRowIndex(23).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockInterimBalance()));

      // ...
      cellDataList.add(new CellData().setRowIndex(26).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockChangeUSDTTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(27).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockChangeUSDCTransactionBalance()));
      cellDataList.add(new CellData().setRowIndex(28).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getLockChangeSPYTransactionBalance()));

      // ...
      cellDataList.add(new CellData().setRowIndex(30).setCellIndex(2).setKeepStyle(true).setValue(dusdSheetDTO.getCustomerTotalBalance()));
      cellDataList.add(new CellData().setRowIndex(30).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getLockTotalBalance()));

      cellDataList.add(new CellData().setRowIndex(31).setCellIndex(5).setKeepStyle(true).setValue(dusdSheetDTO.getTotalDifference()));

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

      // ...
      cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockInterimDifference()));

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

      // ...
      cellDataList.add(new CellData().setRowIndex(15).setCellIndex(5).setKeepStyle(true).setValue(tokenSheetDTO.getLockInterimDifference()));

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
      TransactionSheetDTO spyTransactionSheetDTO = tokenToTransactionSheetDTOMap.get(TokenEnum.SPY);

      // ...
      CellDataList cellDataList = new CellDataList();

      fillTransactionCellDataList(btcTransactionSheetDTO, 6, cellDataList);
      fillTransactionCellDataList(ethTransactionSheetDTO, 7, cellDataList);
      fillTransactionCellDataList(usdtTransactionSheetDTO, 8, cellDataList);
      fillTransactionCellDataList(usdcTransactionSheetDTO, 9, cellDataList);
      fillTransactionCellDataList(spyTransactionSheetDTO, 10, cellDataList);

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
    private CellDataList createHistoryPriceCellDataList(@Nonnull HistoryPriceSheetDTO historyPriceSheetDTO) {
      LOGGER.trace("createHistoryPriceCellDataList()");

      // ...
      CellDataList cellDataList = new CellDataList();

      cellDataList.add(new CellData().setCellIndex(0).setValue(historyPriceSheetDTO.getTimestamp()));

      int i = 1;

      for (TokenEnum token : TokenEnum.values()) {
        cellDataList.add(new CellData().setCellIndex(i++).setValue(historyPriceSheetDTO.getPrice(token)));
      }

      return cellDataList;
    }
  }
}
