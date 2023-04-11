package ch.dfx.reporting.compare;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.sql.Connection;
import java.sql.Timestamp;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.config.ReportingConfigEnum;
import ch.dfx.reporting.BalanceReporting;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class APICompareRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(APICompareRunnable.class);

  // ...
  private final NetworkEnum network;
  private final H2DBManager databaseManager;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseStakingBalanceHelper;
  private final DatabaseBalanceHelper databaseYieldmachineBalanceHelper;

  private boolean isProcessing = false;

  /**
   * 
   */
  public APICompareRunnable(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseStakingBalanceHelper = new DatabaseBalanceHelper(network);
    this.databaseYieldmachineBalanceHelper = new DatabaseBalanceHelper(network);
  }

  @Override
  public boolean isProcessing() {
    return isProcessing;
  }

  @Override
  public void run() {
    LOGGER.debug("run() ...");

    long startTime = System.currentTimeMillis();

    isProcessing = true;

    try {
      doRun();
    } catch (Throwable t) {
      LOGGER.error("run", t);
    } finally {
      isProcessing = false;

      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void doRun() {
    LOGGER.trace("doRun() ...");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);
      databaseStakingBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);
      databaseYieldmachineBalanceHelper.openStatements(connection, TOKEN_YIELDMACHINE_SCHEMA);

      Timestamp reportingTimestamp = TransactionCheckerUtils.getCurrentTimeInUTC();

      createBalanceCompareReport(reportingTimestamp);
      createTransactionCompareReport(connection);

      // ...
      databaseBlockHelper.closeStatements();
      databaseStakingBalanceHelper.closeStatements();
      databaseYieldmachineBalanceHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("doRun", e);
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void createBalanceCompareReport(@Nonnull Timestamp reportingTimestamp) {
    LOGGER.trace("createBalanceCompareReport() ...");

    try {
      String rootPath = getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);

      // ...
      String stakingFileName = getValue(ReportingConfigEnum.GOOGLE_BALANCE_COMPARE_REPORT_STAKING_FILENAME);
      String yieldmachineFileName = getValue(ReportingConfigEnum.GOOGLE_BALANCE_COMPARE_REPORT_YIELDMACHINE_FILENAME);

      // ...
      if (null != rootPath
          && null != stakingFileName
          && null != yieldmachineFileName) {
        APISelector apiSelector = new APISelector(databaseStakingBalanceHelper, databaseYieldmachineBalanceHelper);
        apiSelector.selectAllCustomerBalances();

        // ...
        APIBalanceCompareReporting apiStakingBalanceCompareReporting =
            new APIBalanceCompareReporting(
                network, databaseBlockHelper, databaseStakingBalanceHelper,
                BalanceReporting.BalanceReportingTypeEnum.STAKING);
        apiStakingBalanceCompareReporting.report(reportingTimestamp, rootPath, stakingFileName, apiSelector);

        APIBalanceCompareReporting apiYieldmachineBalanceCompareReporting =
            new APIBalanceCompareReporting(
                network, databaseBlockHelper, databaseStakingBalanceHelper,
                BalanceReporting.BalanceReportingTypeEnum.YIELD_MACHINE);
        apiYieldmachineBalanceCompareReporting.report(reportingTimestamp, rootPath, yieldmachineFileName, apiSelector);
      }
    } catch (Exception e) {
      LOGGER.error("createBalanceCompareReport", e);
    }
  }

  /**
   * 
   */
  private void createTransactionCompareReport(@Nonnull Connection connection) {
    LOGGER.trace("createTransactionCompareReport() ...");

    try {
      String rootPath = getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String stakingFileName = getValue(ReportingConfigEnum.GOOGLE_BALANCE_COMPARE_REPORT_STAKING_FILENAME);
      String stakingDiffTxSheetName = getValue(ReportingConfigEnum.GOOGLE_BALANCE_COMPARE_REPORT_STAKING_DIFF_TX_SHEET);

      String yieldmachineFileName = getValue(ReportingConfigEnum.GOOGLE_BALANCE_COMPARE_REPORT_YIELDMACHINE_FILENAME);
      String yieldmachineDiffTxSheetName = getValue(ReportingConfigEnum.GOOGLE_BALANCE_COMPARE_REPORT_YIELDMACHINE_DIFF_TX_SHEET);

      // ...
      if (null != rootPath
          && null != stakingFileName
          && null != stakingDiffTxSheetName
          && null != yieldmachineFileName
          && null != yieldmachineDiffTxSheetName) {
        APISelector apiSelector = new APISelector(databaseStakingBalanceHelper, databaseYieldmachineBalanceHelper);

        APITransactionCompareReporting apiStakingTransactionCompareReporting =
            new APITransactionCompareReporting(
                network, databaseBlockHelper, databaseStakingBalanceHelper,
                BalanceReporting.BalanceReportingTypeEnum.STAKING);
        apiStakingTransactionCompareReporting.report(connection, rootPath, stakingFileName, stakingDiffTxSheetName, apiSelector);

        APITransactionCompareReporting apiYieldmachineTransactionCompareReporting =
            new APITransactionCompareReporting(
                network, databaseBlockHelper, databaseYieldmachineBalanceHelper,
                BalanceReporting.BalanceReportingTypeEnum.YIELD_MACHINE);
        apiYieldmachineTransactionCompareReporting.report(connection, rootPath, yieldmachineFileName, yieldmachineDiffTxSheetName, apiSelector);
      }
    } catch (Exception e) {
      LOGGER.error("createTransactionCompareReport", e);
    }
  }

  /**
   * 
   */
  private String getValue(@Nonnull ReportingConfigEnum reportingConfig) {
    return ConfigProvider.getInstance().getValue(reportingConfig);
  }
}
