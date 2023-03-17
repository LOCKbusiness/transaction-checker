package ch.dfx.reporting.transparency;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * Version 1: Different customer sheets for all Tokens (Vaults only) ...
 */
public class YieldmachineTransparencyReporting1 extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(YieldmachineTransparencyReporting1.class);

  // ...
  private static final String TOTAL_DIFFERENCE_PROPERTY = "TOTAL_DIFFERENCE";
  private static final String TOTAL_BALANCE_PROPERTY = "TOTAL_BALANCE";

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

  /**
   * 
   */
  public YieldmachineTransparencyReporting1(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper,
      @Nonnull List<String> logInfoList) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void report(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull TokenEnum token,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String transparencyReportTotalSheet,
      @Nonnull String transparencyReportCustomerSheet) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      setup();

      RowDataList yieldmachineBalanceRowDataList = getRowDataList(token);

      // ...
      int numberOfCustomer = yieldmachineBalanceRowDataList.size();
      BigDecimal stakingBalance = (BigDecimal) yieldmachineBalanceRowDataList.getProperty(TOTAL_BALANCE_PROPERTY);

      // ...
      CellDataList transparencyReportingCellDataList =
          createCellDataList(reportingTimestamp, token, rootPath, fileName, transparencyReportTotalSheet, numberOfCustomer, stakingBalance);

      BigDecimal difference = (BigDecimal) transparencyReportingCellDataList.getProperty(TOTAL_DIFFERENCE_PROPERTY);

      if (-1 == BigDecimal.ZERO.compareTo(difference)) {
        writeTransparencyReport(rootPath, fileName, transparencyReportTotalSheet, transparencyReportingCellDataList);
        writeYieldmachineBalance(reportingTimestamp, rootPath, fileName, transparencyReportCustomerSheet, yieldmachineBalanceRowDataList);
      } else {
        String messageText =
            "Yield Machine Transparency Report (" + token + "):\n"
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
  private void setup() throws DfxException {
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
  }

  /**
   * 
   */
  private CellDataList createCellDataList(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull TokenEnum token,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String btcTransparencyReportTotalSheet,
      int numberOfCustomer,
      @Nonnull BigDecimal stakingBalance) throws DfxException {
    LOGGER.debug("createCellDataList()");

    // ...
    BigDecimal totalCustomerValue = BigDecimal.ZERO;
    BigDecimal totalLockValue = BigDecimal.ZERO;

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

    cellDataList.add(new CellData().setRowIndex(5).setCellIndex(5).setKeepStyle(true).setValue(vault1Amount));
    cellDataList.add(new CellData().setRowIndex(6).setCellIndex(5).setKeepStyle(true).setValue(vault1Collateral));

    // ...
    BigDecimal vault2Amount = vault2TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault2Collateral = vault2TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(vault2Amount);
    totalLockValue = totalLockValue.add(vault2Collateral);

    cellDataList.add(new CellData().setRowIndex(7).setCellIndex(5).setKeepStyle(true).setValue(vault2Amount));
    cellDataList.add(new CellData().setRowIndex(8).setCellIndex(5).setKeepStyle(true).setValue(vault2Collateral));

    // ...
    BigDecimal vault3Amount = vault3TokenToAmountMap.getOrDefault(token.toString(), BigDecimal.ZERO);
    BigDecimal vault3Collateral = vault3TokenToVaultCollateralMap.getOrDefault(token.toString(), BigDecimal.ZERO);

    totalLockValue = totalLockValue.add(vault3Amount);
    totalLockValue = totalLockValue.add(vault3Collateral);

    cellDataList.add(new CellData().setRowIndex(9).setCellIndex(5).setKeepStyle(true).setValue(vault3Amount));
    cellDataList.add(new CellData().setRowIndex(10).setCellIndex(5).setKeepStyle(true).setValue(vault3Collateral));

    // ...
    cellDataList.add(new CellData().setRowIndex(16).setCellIndex(2).setKeepStyle(true).setValue(totalCustomerValue));
    cellDataList.add(new CellData().setRowIndex(16).setCellIndex(5).setKeepStyle(true).setValue(totalLockValue));

    // ...
    BigDecimal difference = totalLockValue.subtract(totalCustomerValue);
    cellDataList.add(new CellData().setRowIndex(18).setCellIndex(5).setKeepStyle(true).setValue(difference));

    cellDataList.addProperty(TOTAL_DIFFERENCE_PROPERTY, difference);

    return cellDataList;
  }

  /**
   * 
   */
  private RowDataList getRowDataList(@Nonnull TokenEnum token) throws DfxException {
    // ...
    RowDataList rowDataList = new RowDataList(2);

    // ...
    BigDecimal totalBalance = BigDecimal.ZERO;

    // ...
    List<StakingDTO> stakingDTOList = getStakingDTOList(token);

    for (StakingDTO stakingDTO : stakingDTOList) {
      RowData rowData = new RowData();
      rowData.addCellData(new CellData().setValue(stakingDTO.getCustomerAddress()));
      rowData.addCellData(new CellData().setValue(stakingDTO.getDepositAddress()));

      BigDecimal balance = stakingDTO.getVin().subtract(stakingDTO.getVout());
      rowData.addCellData(new CellData().setValue(balance));

      totalBalance = totalBalance.add(balance);

      rowDataList.add(rowData);
    }

    rowDataList.addProperty(TOTAL_BALANCE_PROPERTY, totalBalance);

    return rowDataList;
  }

  /**
   * 
   */
  private List<StakingDTO> getStakingDTOList(@Nonnull TokenEnum token) throws DfxException {
    List<StakingDTO> stakingDTOList = databaseBalanceHelper.getStakingDTOList(token);

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

    return stakingDTOList;
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
  private void writeTransparencyReport(
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet,
      @Nonnull CellDataList cellDataList) throws DfxException {
    LOGGER.debug("writeTransparencyReport()");

    // ...
    RowDataList rowDataList = new RowDataList(0);

    // ...
    openExcel(rootPath, fileName, sheet);
    writeExcel(rowDataList, cellDataList);
    closeExcel();
  }

  /**
   * 
   */
  private void writeYieldmachineBalance(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.debug("writeYieldmachineBalance()");

    // ...
    CellDataList cleanCellDataList = new CellDataList();
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(reportingTimestamp));
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(BigDecimal.ZERO));

    BigDecimal btcTotalBalance = (BigDecimal) rowDataList.getProperty(TOTAL_BALANCE_PROPERTY);

    CellDataList cellDataList = new CellDataList();
    cellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(btcTotalBalance));

    // ...
    openExcel(rootPath, fileName, sheet);

    cleanExcel(2);
    cleanExcel(cleanCellDataList);

    writeExcel(rowDataList, cellDataList);
    closeExcel();
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
}
