package ch.dfx.reporting.transparency;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.logging.notifier.TelegramNotifier;
import ch.dfx.config.ReportingConfigEnum;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.reporting.BalanceReporting;
import ch.dfx.reporting.BalanceReporting.BalanceReportingTypeEnum;
import ch.dfx.reporting.MasternodeReporting;
import ch.dfx.reporting.Reporting;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class StakingTransparencyReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(StakingTransparencyReporting.class);

  // ...
  private static final String TOTAL_DIFFERENCE_PROPERTY = "TOTAL_DIFFERENCE";

  // ...
  private final BalanceReporting balanceReporting;
  private final MasternodeReporting masternodeReporting;

  /**
   * 
   */
  public StakingTransparencyReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.balanceReporting =
        new BalanceReporting(network, databaseBlockHelper, databaseBalanceHelper, new ArrayList<>(), BalanceReportingTypeEnum.STAKING);

    this.masternodeReporting =
        new MasternodeReporting(network, databaseBlockHelper, databaseBalanceHelper);
  }

  /**
   * 
   */
  public void report(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String transparencyReportTotalSheet,
      @Nonnull String transparencyReportCustomerSheet,
      @Nonnull String transparencyReportMasternodeSheet) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      Date currentDate = new Date();

      // ...
      RowDataList stakingBalanceRowDataList = balanceReporting.createRowDataList(token);
      RowDataList masternodeRowDataList = masternodeReporting.createRowDataList();

      // ...
      int stakingNumberOfAddress = stakingBalanceRowDataList.size();
      BigDecimal stakingBalance = balanceReporting.getTotalBalance();

      int masternodeNumberOfAddress = masternodeRowDataList.size();
      BigDecimal masternodeBalance = masternodeReporting.getTotalBalance();

      // ...
      CellDataList transparencyReportingCellDataList =
          createCellDataList(
              currentDate, token, rootPath, fileName, transparencyReportTotalSheet,
              stakingNumberOfAddress, stakingBalance,
              masternodeNumberOfAddress, masternodeBalance);

      BigDecimal difference = (BigDecimal) transparencyReportingCellDataList.getProperty(TOTAL_DIFFERENCE_PROPERTY);

      if (-1 == BigDecimal.ZERO.compareTo(difference)) {
        writeTransparencyReport(rootPath, fileName, transparencyReportTotalSheet, transparencyReportingCellDataList);
        writeStakingBalance(currentDate, rootPath, fileName, transparencyReportCustomerSheet, stakingBalanceRowDataList);
        writeMasternodeBalance(currentDate, rootPath, fileName, transparencyReportMasternodeSheet, masternodeRowDataList);
      } else {
        String messageText =
            "Staking Transparency Report (" + token + "):\n"
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
  private CellDataList createCellDataList(
      @Nonnull Date currentDate,
      @Nonnull TokenEnum token,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String transparencyReportTotalSheet,
      int stakingNumberOfCustomer,
      @Nonnull BigDecimal stakingBalance,
      int masternodeNumberOfAddress,
      @Nonnull BigDecimal masternodeBalance) throws DfxException {
    LOGGER.debug("createCellDataList()");

    BigDecimal totalCustomerValue = BigDecimal.ZERO;
    BigDecimal totalLockValue = BigDecimal.ZERO;

    List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList();

    // ...
    totalCustomerValue = totalCustomerValue.add(stakingBalance);

    // ...
    CellDataList cellDataList = new CellDataList();
    cellDataList.add(new CellData().setRowIndex(1).setCellIndex(4).setKeepStyle(true).setValue(currentDate));
    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(0).setKeepStyle(true).setValue(stakingNumberOfCustomer));
    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(2).setKeepStyle(true).setValue(stakingBalance));

    // ...
    BigDecimal liquidityBalance = getLiquidityBalance(token, stakingAddressDTOList);
    cellDataList.add(new CellData().setRowIndex(4).setCellIndex(5).setKeepStyle(true).setValue(liquidityBalance));

    totalLockValue = totalLockValue.add(liquidityBalance);

    // ...
    totalLockValue = totalLockValue.add(masternodeBalance);

    cellDataList.add(new CellData().setRowIndex(5).setCellIndex(3).setKeepStyle(true).setValue(masternodeNumberOfAddress));
    cellDataList.add(new CellData().setRowIndex(5).setCellIndex(5).setKeepStyle(true).setValue(masternodeBalance));

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
  private BigDecimal getLiquidityBalance(
      @Nonnull TokenEnum token,
      @Nonnull List<StakingAddressDTO> stakingAddressDTOList) throws DfxException {
    LOGGER.debug("getLiquidityBalance()");

    BigDecimal totalLiquidityBalance = BigDecimal.ZERO;

    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
        LOGGER.debug("Liquidity Address: " + stakingAddressDTO.getLiquidityAddress());

        BalanceDTO balanceDTO =
            databaseBalanceHelper.getBalanceDTOByAddressNumber(token, stakingAddressDTO.getLiquidityAddressNumber());

        if (null != balanceDTO) {
          totalLiquidityBalance = totalLiquidityBalance.add(balanceDTO.getVout()).subtract(balanceDTO.getVin());
        }
      }
    }

    return totalLiquidityBalance;
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
  private void writeStakingBalance(
      @Nonnull Date reportingDate,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.debug("writeStakingBalance()");

    // ...
    CellDataList cleanCellDataList = new CellDataList();
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(reportingDate));
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(BigDecimal.ZERO));

    CellDataList cellDataList = new CellDataList();
    cellDataList.add(new CellData().setRowIndex(0).setCellIndex(2).setKeepStyle(true).setValue(balanceReporting.getTotalBalance()));

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
  private void writeMasternodeBalance(
      @Nonnull Date reportingDate,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.debug("writeMasternodeBalance()");

    // ...
    CellDataList cleanCellDataList = new CellDataList();
    cleanCellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(BigDecimal.ZERO));

    CellDataList cellDataList = new CellDataList();
    cellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(masternodeReporting.getTotalBalance()));

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
