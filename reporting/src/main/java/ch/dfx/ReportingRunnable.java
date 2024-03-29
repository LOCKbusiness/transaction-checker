package ch.dfx;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.logging.notifier.TelegramNotifier;
import ch.dfx.config.ReportingConfigEnum;
import ch.dfx.reporting.BalanceReporting;
import ch.dfx.reporting.LiquidityMasternodeStakingReporting;
import ch.dfx.reporting.VaultReporting;
import ch.dfx.reporting.YieldmachineYieldReporting;
import ch.dfx.reporting.transparency.StakingTransparencyReporting;
import ch.dfx.reporting.transparency.YieldmachineImpermanentLossReporting;
import ch.dfx.reporting.transparency.YieldmachineTransparencyReporting3;
import ch.dfx.statistik.StakingStatistikProvider;
import ch.dfx.statistik.StatistikReporting;
import ch.dfx.statistik.YieldmachineStatistikProvider;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.scheduler.SchedulerProviderRunnable;

/**
 * 
 */
public class ReportingRunnable implements SchedulerProviderRunnable {
  private static final Logger LOGGER = LogManager.getLogger(ReportingRunnable.class);

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
  public ReportingRunnable(
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

      List<String> logInfoList = new ArrayList<>();

      createStakingBalanceReport(connection, logInfoList);
      createLiquidityMasternodeStakingBalanceReport(connection, logInfoList);
      createVaultReport(connection, logInfoList);

      createStakingTransparencyReport(connection);
      createYieldmachineTransparencyReport(connection);
//      createYieldmachineImpermanentLossReport();

      createYieldmachineYieldReport(connection);

      createStatistikReport(connection);

      writeLogInfo(logInfoList);

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
  private void createStakingBalanceReport(
      @Nonnull Connection connection,
      @Nonnull List<String> logInfoList) {
    LOGGER.trace("createStakingBalanceReport() ...");

    try {
      String rootPath = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String balanceFileName = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_BALANCE_FILENAME);
      String stakingBalanceSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_BALANCE_STAKING_SHEET);
      String yieldmachineBalanceSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_BALANCE_YIELDMACHINE_SHEET);

      if (null != rootPath
          && null != balanceFileName
          && null != stakingBalanceSheet
          && null != yieldmachineBalanceSheet) {
        Timestamp reportingTimestamp = TransactionCheckerUtils.getCurrentTimeInUTC();

        BalanceReporting stakingBalanceReporting =
            new BalanceReporting(network, databaseBlockHelper, databaseStakingBalanceHelper,
                logInfoList, BalanceReporting.BalanceReportingTypeEnum.STAKING);
        stakingBalanceReporting.report(connection, reportingTimestamp, rootPath, balanceFileName, stakingBalanceSheet);

        BalanceReporting yieldmachineBalanceReporting =
            new BalanceReporting(network, databaseBlockHelper, databaseYieldmachineBalanceHelper,
                logInfoList, BalanceReporting.BalanceReportingTypeEnum.YIELD_MACHINE);
        yieldmachineBalanceReporting.report(connection, reportingTimestamp, rootPath, balanceFileName, yieldmachineBalanceSheet);

        logInfoList.add("");
      }
    } catch (Exception e) {
      LOGGER.error("createStakingBalanceReport", e);
    }
  }

  /**
   * 
   */
  private void createLiquidityMasternodeStakingBalanceReport(
      @Nonnull Connection connection,
      @Nonnull List<String> logInfoList) {
    LOGGER.trace("createLiquidityMasternodeStakingBalanceReport() ...");

    try {
      String rootPath = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String checkFileName = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_FILENAME);
      String checkSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_LIQUIDITY_MASTERNODE_STAKING_CHECK_SHEET);

      if (null != rootPath
          && null != checkFileName
          && null != checkSheet) {
        Timestamp reportingTimestamp = TransactionCheckerUtils.getCurrentTimeInUTC();

        LiquidityMasternodeStakingReporting liquidityMasternodeStakingReporting =
            new LiquidityMasternodeStakingReporting(network, databaseBlockHelper, databaseStakingBalanceHelper, logInfoList);
        liquidityMasternodeStakingReporting.report(connection, reportingTimestamp, TokenEnum.DFI, rootPath, checkFileName, checkSheet);

        logInfoList.add("");
      }
    } catch (Exception e) {
      LOGGER.error("createLiquidityMasternodeStakingBalanceReport", e);
    }
  }

  /**
   * 
   */
  private void createVaultReport(
      @Nonnull Connection connection,
      @Nonnull List<String> logInfoList) {
    LOGGER.trace("createVaultReport() ...");

    try {
      String rootPath = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String checkFileName = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_VAULT_CHECK_FILENAME);
      String checkSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_VAULT_CHECK_SHEET);

      if (null != rootPath
          && null != checkFileName
          && null != checkSheet) {
        Timestamp reportingTimestamp = TransactionCheckerUtils.getCurrentTimeInUTC();

        VaultReporting vaultReporting =
            new VaultReporting(network, databaseBlockHelper, databaseStakingBalanceHelper, logInfoList);
        vaultReporting.report(connection, reportingTimestamp, TokenEnum.DUSD, rootPath, checkFileName, checkSheet);
      }
    } catch (Exception e) {
      LOGGER.error("createVaultReport", e);
    }
  }

  /**
   * 
   */
  private void createStatistikReport(@Nonnull Connection connection) {
    LOGGER.trace("createStatistikReport() ...");

    try {
      String rootPath = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String statistikFileName = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_STATISTIK_FILENAME);
      String statistikDfiDataSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_STATISTIK_DFI_DATA_SHEET);
      String statistikDusdDataSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_STATISTIK_DUSD_DATA_SHEET);

      if (null != rootPath
          && null != statistikFileName
          && null != statistikDfiDataSheet
          && null != statistikDusdDataSheet) {
        // ...
        StakingStatistikProvider stakingStatistikProvider =
            new StakingStatistikProvider(network, databaseBlockHelper, databaseStakingBalanceHelper);

        StatistikReporting stakingStatistikReporting =
            new StatistikReporting(network, databaseBlockHelper, databaseStakingBalanceHelper, stakingStatistikProvider);
        stakingStatistikReporting.report(connection, TokenEnum.DFI, rootPath, statistikFileName, statistikDfiDataSheet);

        // ...
        YieldmachineStatistikProvider yieldmachineStatistikProvider =
            new ch.dfx.statistik.YieldmachineStatistikProvider(network, databaseBlockHelper, databaseYieldmachineBalanceHelper);

        StatistikReporting yieldmachineStatistikReporting =
            new StatistikReporting(network, databaseBlockHelper, databaseYieldmachineBalanceHelper, yieldmachineStatistikProvider);
        yieldmachineStatistikReporting.report(connection, TokenEnum.DUSD, rootPath, statistikFileName, statistikDusdDataSheet);
      }
    } catch (Exception e) {
      LOGGER.error("createStatistikReport", e);
    }
  }

  /**
   * 
   */
  private void createStakingTransparencyReport(@Nonnull Connection connection) {
    LOGGER.trace("createStakingTransparencyReport() ...");

    try {
      String rootPath = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String fileName = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_STAKING_FILENAME);
      String fileNameIntern = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_STAKING_FILENAME_INTERN);
      String totalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_STAKING_TOTAL_SHEET);
      String customerSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_STAKING_CUSTOMER_SHEET);
      String masternodeSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_STAKING_MASTERNODE_SHEET);

      if (null != rootPath
          && null != fileName
          && null != fileNameIntern
          && null != totalSheet
          && null != customerSheet
          && null != masternodeSheet) {
        Timestamp reportingTimestamp = TransactionCheckerUtils.getCurrentTimeInUTC();

        StakingTransparencyReporting transparencyReporting =
            new StakingTransparencyReporting(network, databaseBlockHelper, databaseStakingBalanceHelper);
        transparencyReporting
            .report(connection, reportingTimestamp, TokenEnum.DFI, rootPath, fileName, fileNameIntern, totalSheet, customerSheet, masternodeSheet);
      }
    } catch (Exception e) {
      LOGGER.error("createStakingTransparencyReport", e);
    }
  }

  /**
   * 
   */
  private void createYieldmachineTransparencyReport(@Nonnull Connection connection) {
    LOGGER.trace("createYieldmachineTransparencyReport() ...");

    try {
      // ...
      String rootPath = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String fileName = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_FILENAME);
      String fileNameIntern = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_FILENAME_INTERN);

      String totalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_TOTAL_SHEET);
      String transactionSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_TRANSACTION_SHEET);
      String customerSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_CUSTOMER_SHEET);

      String diagramSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_DIAGRAM_SHEET);
      String historyAmountSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_HISTORY_AMOUNT_SHEET);
      String historyInterimDifferenceSheet =
          ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_HISTORY_INTERIM_DIFFERENCE_SHEET);
      String historyAssetPriceSheet =
          ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_HISTORY_ASSET_PRICE_SHEET);
      String historyPriceSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_HISTORY_PRICE_SHEET);

      String dfiTotalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_DFI_TOTAL_SHEET);
      String dusdTotalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_DUSD_TOTAL_SHEET);
      String btcTotalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_BTC_TOTAL_SHEET);
      String ethTotalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_ETH_TOTAL_SHEET);
      String usdtTotalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_USDT_TOTAL_SHEET);
      String usdcTotalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_USDC_TOTAL_SHEET);
      String eurocTotalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_EUROC_TOTAL_SHEET);
      String spyTotalSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_TRANSPARENCY_REPORT_YIELDMACHINE_SPY_TOTAL_SHEET);

      // ...
      Map<YieldmachineTransparencyReporting3.SheetEnum, String> sheetIdToSheetNameMap = new EnumMap<>(YieldmachineTransparencyReporting3.SheetEnum.class);
      sheetIdToSheetNameMap.put(YieldmachineTransparencyReporting3.SheetEnum.TOTAL, totalSheet);
      sheetIdToSheetNameMap.put(YieldmachineTransparencyReporting3.SheetEnum.TRANSACTION, transactionSheet);
      sheetIdToSheetNameMap.put(YieldmachineTransparencyReporting3.SheetEnum.CUSTOMER, customerSheet);
      sheetIdToSheetNameMap.put(YieldmachineTransparencyReporting3.SheetEnum.DIAGRAM, diagramSheet);
      sheetIdToSheetNameMap.put(YieldmachineTransparencyReporting3.SheetEnum.HISTORY_AMOUNT, historyAmountSheet);
      sheetIdToSheetNameMap.put(YieldmachineTransparencyReporting3.SheetEnum.HISTORY_INTERIM_DIFFERENCE, historyInterimDifferenceSheet);
      sheetIdToSheetNameMap.put(YieldmachineTransparencyReporting3.SheetEnum.HISTORY_ASSET_PRICE, historyAssetPriceSheet);
      sheetIdToSheetNameMap.put(YieldmachineTransparencyReporting3.SheetEnum.HISTORY_PRICE, historyPriceSheet);

      boolean isSheetNameNull = sheetIdToSheetNameMap.values().stream().anyMatch(sheetName -> null == sheetName);

      // ...
      Map<TokenEnum, String> tokenToSheetNameMap = new EnumMap<>(TokenEnum.class);
      tokenToSheetNameMap.put(TokenEnum.DFI, dfiTotalSheet);
      tokenToSheetNameMap.put(TokenEnum.DUSD, dusdTotalSheet);
      tokenToSheetNameMap.put(TokenEnum.BTC, btcTotalSheet);
      tokenToSheetNameMap.put(TokenEnum.ETH, ethTotalSheet);
      tokenToSheetNameMap.put(TokenEnum.USDT, usdtTotalSheet);
      tokenToSheetNameMap.put(TokenEnum.USDC, usdcTotalSheet);
      tokenToSheetNameMap.put(TokenEnum.EUROC, eurocTotalSheet);
      tokenToSheetNameMap.put(TokenEnum.SPY, spyTotalSheet);

      boolean isTokenSheetNameNull = tokenToSheetNameMap.values().stream().anyMatch(sheetName -> null == sheetName);

      if (null != rootPath
          && null != fileName
          && null != fileNameIntern
          && !isSheetNameNull
          && !isTokenSheetNameNull) {
        Timestamp reportingTimestamp = TransactionCheckerUtils.getCurrentTimeInUTC();

        YieldmachineTransparencyReporting3 transparencyReporting =
            new YieldmachineTransparencyReporting3(network, databaseBlockHelper, databaseYieldmachineBalanceHelper);

        transparencyReporting.report(reportingTimestamp, rootPath, fileName, fileNameIntern, sheetIdToSheetNameMap, tokenToSheetNameMap);
      }
    } catch (Exception e) {
      LOGGER.error("createYieldmachineBTCTransparencyReport", e);
    }
  }

  /**
   * 
   */
  private void createYieldmachineImpermanentLossReport() {
    LOGGER.trace("createYieldmachineImpermanentLossReport() ...");

    try {
      // ...
      String rootPath = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String fileName = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_IMPERMANENT_LOSS_REPORT_FILENAME);
      String btcPoolSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_IMPERMANENT_LOSS_REPORT_BTC_DFI_SHEET);
      String ethPoolSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_IMPERMANENT_LOSS_REPORT_ETH_DFI_SHEET);
      String dusdPoolSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_IMPERMANENT_LOSS_REPORT_DUSD_DFI_SHEET);
      String usdtPoolSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_IMPERMANENT_LOSS_REPORT_USDT_DUSD_SHEET);
      String usdcPoolSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_IMPERMANENT_LOSS_REPORT_USDC_DUSD_SHEET);
      String eurocPoolSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_IMPERMANENT_LOSS_REPORT_EUROC_DUSD_SHEET);
      String spyPoolSheet = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_IMPERMANENT_LOSS_REPORT_SPY_DUSD_SHEET);

      Map<TokenEnum, String> tokenToPoolSheetNameMap = new EnumMap<>(TokenEnum.class);
      tokenToPoolSheetNameMap.put(TokenEnum.BTC, btcPoolSheet);
      tokenToPoolSheetNameMap.put(TokenEnum.ETH, ethPoolSheet);
      tokenToPoolSheetNameMap.put(TokenEnum.DUSD, dusdPoolSheet);
      tokenToPoolSheetNameMap.put(TokenEnum.USDT, usdtPoolSheet);
      tokenToPoolSheetNameMap.put(TokenEnum.USDC, usdcPoolSheet);
      tokenToPoolSheetNameMap.put(TokenEnum.EUROC, eurocPoolSheet);
      tokenToPoolSheetNameMap.put(TokenEnum.SPY, spyPoolSheet);

      boolean isTokenPoolSheetNameNull = tokenToPoolSheetNameMap.values().stream().anyMatch(sheetName -> null == sheetName);

      if (null != rootPath
          && null != fileName
          && !isTokenPoolSheetNameNull) {
        Timestamp reportingTimestamp = TransactionCheckerUtils.getCurrentTimeInUTC();

        YieldmachineImpermanentLossReporting yieldmachineImpermanentLossReporting =
            new YieldmachineImpermanentLossReporting(network, databaseBlockHelper, databaseYieldmachineBalanceHelper);

        // yieldmachineImpermanentLossReporting.updateData();
        yieldmachineImpermanentLossReporting.report(reportingTimestamp, rootPath, fileName, tokenToPoolSheetNameMap);
      }
    } catch (Exception e) {
      LOGGER.error("createYieldmachineImpermanentLossReport", e);
    }
  }

  /**
   * 
   */
  private void createYieldmachineYieldReport(@Nonnull Connection connection) {
    LOGGER.trace("createYieldmachineYieldReport() ...");

    try {
      // ...
      String rootPath = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_ROOT_PATH);
      String fileName = ConfigProvider.getInstance().getValue(ReportingConfigEnum.GOOGLE_YIELD_REPORT_YIELDMACHINE_FILENAME);

      if (null != rootPath
          && null != fileName) {
        Timestamp reportingTimestamp = TransactionCheckerUtils.getCurrentTimeInUTC();

        YieldmachineYieldReporting yieldmachineYieldReporting =
            new YieldmachineYieldReporting(network, databaseBlockHelper, databaseYieldmachineBalanceHelper);

        yieldmachineYieldReporting.report(reportingTimestamp, rootPath, fileName);
      }
    } catch (Exception e) {
      LOGGER.error("createYieldmachineYieldReport", e);
    }
  }

  /**
   * 
   */
  private void writeLogInfo(@Nonnull List<String> logInfoList) {
    LOGGER.trace("writeLogInfo()");

    try {
      StringBuilder logInfoBuilder = new StringBuilder();

      for (String logInfo : logInfoList) {
        logInfoBuilder.append(logInfo).append("\n");
      }

      String telegramToken = ConfigProvider.getInstance().getValue(ReportingConfigEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_TOKEN);
      String telegramChatId = ConfigProvider.getInstance().getValue(ReportingConfigEnum.TELEGRAM_AUTOMATIC_INFORMATION_BOT_CHAT_ID);

      if (null != telegramToken
          && null != telegramChatId) {
        TelegramNotifier telegramNotifier = new TelegramNotifier();
        telegramNotifier.sendMessage(telegramToken, telegramChatId, logInfoBuilder.toString());
      }
    } catch (Exception e) {
      LOGGER.error("writeLogInfo", e);
    }
  }
}
