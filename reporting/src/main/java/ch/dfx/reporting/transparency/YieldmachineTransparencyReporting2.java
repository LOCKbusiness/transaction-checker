package ch.dfx.reporting.transparency;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ObjectUtils;
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
 * Version 2: One customer sheets for all Tokens (Vaults only) ...
 */
public class YieldmachineTransparencyReporting2 extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(YieldmachineTransparencyReporting2.class);

  // ...
  private static final String TOTAL_DIFFERENCE_PROPERTY = "TOTAL_DIFFERENCE";
  private static final String TOTAL_DFI_BALANCE_PROPERTY = "TOTAL_DFI_BALANCE";
  private static final String TOTAL_BTC_BALANCE_PROPERTY = "TOTAL_BTC_BALANCE";
  private static final String TOTAL_ETH_BALANCE_PROPERTY = "TOTAL_ETH_BALANCE";
  private static final String TOTAL_USDT_BALANCE_PROPERTY = "TOTAL_USDT_BALANCE";
  private static final String TOTAL_USDC_BALANCE_PROPERTY = "TOTAL_USDC_BALANCE";

  // ...
  private final DefiDataProvider dataProvider;

  // ...
  private Map<String, BigDecimal> liquidityTokenToAmountMap = null;

  private Map<String, BigDecimal> vault1TokenToAmountMap = null;
  private Map<String, BigDecimal> vault1TokenToVaultCollateralMap = null;

  private Map<String, BigDecimal> vault2TokenToAmountMap = null;
  private Map<String, BigDecimal> vault2TokenToVaultCollateralMap = null;

  private Map<String, BigDecimal> vault3TokenToAmountMap = null;
  private Map<String, BigDecimal> vault3TokenToVaultCollateralMap = null;

  private Map<String, BigDecimal> dusdLMTokenToAmountMap = null;
  private Map<String, BigDecimal> btcLMTokenToAmountMap = null;
  private Map<String, BigDecimal> ethLMTokenToAmountMap = null;
  private Map<String, BigDecimal> usdtLMTokenToAmountMap = null;
  private Map<String, BigDecimal> usdcLMTokenToAmountMap = null;

  /**
   * 
   */
  public YieldmachineTransparencyReporting2(
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
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String transparencyReportDFITotalSheet,
      @Nonnull String transparencyReportBTCTotalSheet,
      @Nonnull String transparencyReportETHTotalSheet,
      @Nonnull String transparencyReportUSDTTotalSheet,
      @Nonnull String transparencyReportUSDCTotalSheet,
      @Nonnull String transparencyReportCustomerSheet) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      setup();

      // ...
      Map<TokenEnum, ReportData> tokenToReportDataMap = new EnumMap<>(TokenEnum.class);

      Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap = createDepositAddressToStakingDTOMap();

      tokenToReportDataMap
          .put(TokenEnum.DFI, createReportData(reportingTimestamp, TokenEnum.DFI, transparencyReportDFITotalSheet, depositAddressToStakingDTOMap));
      tokenToReportDataMap
          .put(TokenEnum.BTC, createReportData(reportingTimestamp, TokenEnum.BTC, transparencyReportBTCTotalSheet, depositAddressToStakingDTOMap));
      tokenToReportDataMap
          .put(TokenEnum.ETH, createReportData(reportingTimestamp, TokenEnum.ETH, transparencyReportETHTotalSheet, depositAddressToStakingDTOMap));
      tokenToReportDataMap
          .put(TokenEnum.USDT, createReportData(reportingTimestamp, TokenEnum.USDT, transparencyReportUSDTTotalSheet, depositAddressToStakingDTOMap));
      tokenToReportDataMap
          .put(TokenEnum.USDC, createReportData(reportingTimestamp, TokenEnum.USDC, transparencyReportUSDCTotalSheet, depositAddressToStakingDTOMap));

      // ...
      boolean isDifferenceNegative = tokenToReportDataMap.values().stream().anyMatch(data -> -1 != BigDecimal.ZERO.compareTo(data.difference));

      if (!isDifferenceNegative) {
        writeReport(rootPath, fileName, transparencyReportCustomerSheet, tokenToReportDataMap, depositAddressToStakingDTOMap);
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
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull TokenEnum token,
      @Nonnull String transparencyReportTotalSheet,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    // ...
    CellDataList transparencyReportingCellDataList =
        createCellDataList(reportingTimestamp, token, depositAddressToStakingDTOMap);

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

    // ...
    if (null == liquidityTokenToAmountMap) {
      String liquidityAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_LIQUIDITY_ADDRESS, "");
      liquidityTokenToAmountMap = getTokenToAmountMap(liquidityAddress);
    }

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

    // ...
    if (null == dusdLMTokenToAmountMap) {
      String dusdLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_DUSD_LM_ADDRESS, "");
      dusdLMTokenToAmountMap = getTokenToAmountMap(dusdLMAddress);
    }

    if (null == btcLMTokenToAmountMap) {
      String btcLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_BTC_LM_ADDRESS, "");
      btcLMTokenToAmountMap = getTokenToAmountMap(btcLMAddress);
    }

    if (null == ethLMTokenToAmountMap) {
      String ethLMAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_ETH_LM_ADDRESS, "");
      ethLMTokenToAmountMap = getTokenToAmountMap(ethLMAddress);
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
  private CellDataList createCellDataList(
      @Nonnull Timestamp reportingTimestamp,
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
    cellDataList.add(new CellData().setRowIndex(1).setCellIndex(4).setKeepStyle(true).setValue(reportingTimestamp));
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
    cellDataList.add(new CellData().setRowIndex(17).setCellIndex(2).setKeepStyle(true).setValue(totalCustomerValue));
    cellDataList.add(new CellData().setRowIndex(17).setCellIndex(5).setKeepStyle(true).setValue(totalLockValue));

    // ...
    BigDecimal difference = totalLockValue.subtract(totalCustomerValue);
    cellDataList.add(new CellData().setRowIndex(19).setCellIndex(5).setKeepStyle(true).setValue(difference));

    cellDataList.addProperty(TOTAL_DIFFERENCE_PROPERTY, difference);

    return cellDataList;
  }

  /**
   * 
   */
  private Multimap<Integer, StakingDTO> createDepositAddressToStakingDTOMap() throws DfxException {
    LOGGER.debug("createDepositAddressToStakingDTOMap()");

    // ...
    List<StakingDTO> stakingDTOList = databaseBalanceHelper.getStakingDTOList(TokenEnum.DFI);
    stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(TokenEnum.BTC));
    stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(TokenEnum.ETH));
    stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(TokenEnum.USDT));
    stakingDTOList.addAll(databaseBalanceHelper.getStakingDTOList(TokenEnum.USDC));

    // ...
//    List<StakingDTO> stakingDTOList =
//        completeStakingDTOList.stream()
//            .filter(dto -> -1 == BigDecimal.ZERO.compareTo(dto.getVin().subtract(dto.getVout())))
//            .collect(Collectors.toList());

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
      @Nonnull String transparencyReportCustomerSheet,
      @Nonnull Map<TokenEnum, ReportData> tokenToReportDataMap,
      @Nonnull Multimap<Integer, StakingDTO> depositAddressToStakingDTOMap) throws DfxException {
    LOGGER.debug("writeReport()");

    openExcel(rootPath, fileName);

    for (ReportData reportData : tokenToReportDataMap.values()) {
      setSheet(reportData.transparencyReportTotalSheet);
      writeExcel(new RowDataList(0), reportData.transparencyReportingCellDataList);
    }

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
