package ch.dfx.reporting;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
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
import ch.dfx.defichain.data.price.DefiFixedIntervalPriceData;
import ch.dfx.defichain.data.vault.DefiListVaultData;
import ch.dfx.defichain.data.vault.DefiVaultData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.CellDataList;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 *
 */
public class VaultReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(VaultReporting.class);

  // ...
  private static final DecimalFormat GERMAN_DECIMAL_FORMAT = new DecimalFormat("#,##0.00000000");
  private static final DecimalFormat GERMAN_PERCENTAGE_FORMAT = new DecimalFormat("#,##0.00");

  private static final BigDecimal HUNDERED = new BigDecimal(100);

  private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
  private static final int SCALE = 8;

  // ...
  private final List<String> logInfoList;

  // ...
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public VaultReporting(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper,
      @Nonnull List<String> logInfoList) {
    super(network, databaseBlockHelper, databaseBalanceHelper);

    this.logInfoList = logInfoList;

    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();
  }

  /**
   * 
   */
  public void report(
      @Nonnull Connection connection,
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull TokenEnum token,
      @Nonnull String rootPath,
      @Nonnull String fileName,
      @Nonnull String sheet) throws DfxException {
    LOGGER.debug("report()");

    long startTime = System.currentTimeMillis();

    try {
      BigDecimal totalStakingBalance = getTotalStakingBalance(connection, token);

      String liquidityAddress = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_LIQUIDITY_ADDRESS, "");
      String vault1Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT1_ADDRESS, "");
      // String vault2Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT2_ADDRESS, "");
      // String vault3Address = ConfigProvider.getInstance().getValue(ReportingConfigEnum.YM_VAULT3_ADDRESS, "");

      // ...
      Map<String, BigDecimal> liquidityTokenToAmountMap = getTokenToAmountMap(liquidityAddress);
      Map<String, BigDecimal> vaultTokenToAmountMap = getTokenToAmountMap(vault1Address);

      // ...
      if (StringUtils.isNotEmpty(vault1Address)) {
        List<DefiListVaultData> listVaultDataList = dataProvider.listVaults(vault1Address);

        if (!listVaultDataList.isEmpty()) {
          DefiListVaultData listVaultData = listVaultDataList.get(0);

          RowDataList rowDataList = new RowDataList(2);
          CellDataList cellDataList =
              doCheckVaultValue(reportingTimestamp, listVaultData, liquidityTokenToAmountMap, vaultTokenToAmountMap, totalStakingBalance);

          openExcel(rootPath, fileName, sheet);
          writeExcel(rowDataList, cellDataList);
          closeExcel();
        }
      }
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private BigDecimal getTotalStakingBalance(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("getTotalStakingBalance()");

    try {
      BigDecimal balance = BigDecimal.ZERO;

      String selectSql =
          "SELECT sum(vin)-sum(vout) AS balance"
              + " FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".staking WHERE token_number=" + token.getNumber();

      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(DatabaseUtils.replaceSchema(network, selectSql));

      if (resultSet.next()) {
        balance = resultSet.getBigDecimal("balance");
      }

      resultSet.close();
      statement.close();

      return balance;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getTotalStakingBalance", e);
    }
  }

  /**
   * 
   */
  private Map<String, BigDecimal> getTokenToAmountMap(@Nonnull String address) throws DfxException {
    List<String> accountList = dataProvider.getAccount(address);

    return TransactionCheckerUtils.getTokenToAmountMap(accountList);
  }

  /**
   * 
   */
  private CellDataList doCheckVaultValue(
      @Nonnull Timestamp reportingTimestamp,
      @Nonnull DefiListVaultData listVaultData,
      @Nonnull Map<String, BigDecimal> liquidityTokenToAmountMap,
      @Nonnull Map<String, BigDecimal> vaultTokenToAmountMap,
      @Nonnull BigDecimal totalStakingBalance) throws DfxException {
    LOGGER.trace("checkVaultValue()");

    // ...
    BigDecimal liquidityDusdAmount = liquidityTokenToAmountMap.getOrDefault("DUSD", BigDecimal.ZERO);

    BigDecimal ourSpyAmount = vaultTokenToAmountMap.getOrDefault("SPY", BigDecimal.ZERO);
    BigDecimal ourDusdAmount = vaultTokenToAmountMap.getOrDefault("DUSD", BigDecimal.ZERO);
    BigDecimal ourSpyDusdAmount = vaultTokenToAmountMap.getOrDefault("SPY-DUSD", BigDecimal.ZERO);

    // ...
    DefiPoolPairData poolPairData = dataProvider.getPoolPair("SPY-DUSD");
    Pair<BigDecimal, BigDecimal> poolTokenAmountPair = TransactionCheckerUtils.getPoolTokenAmountPair(poolPairData, ourSpyDusdAmount);

    BigDecimal ourSpyPoolAmount = poolTokenAmountPair.getLeft();
    BigDecimal ourDusdPoolAmount = poolTokenAmountPair.getRight();

    // ...
    DefiFixedIntervalPriceData spyFixedIntervalPriceData = dataProvider.getFixedIntervalPrice("SPY/USD");
    DefiFixedIntervalPriceData dusdFixedIntervalPriceData = dataProvider.getFixedIntervalPrice("DUSD/USD");

    BigDecimal spyActivePrice = spyFixedIntervalPriceData.getActivePrice();
    BigDecimal dusdActivePrice = dusdFixedIntervalPriceData.getActivePrice();

    // ...
    DefiVaultData vaultData = dataProvider.getVault(listVaultData.getVaultId());

    List<String> collateralAmounts = vaultData.getCollateralAmounts();
    Map<String, BigDecimal> collateralTokenToAmountMap = TransactionCheckerUtils.getTokenToAmountMap(collateralAmounts);
    BigDecimal collateralDusdAmount = collateralTokenToAmountMap.getOrDefault("DUSD", BigDecimal.ZERO);

    List<String> loanAmounts = vaultData.getLoanAmounts();
    Map<String, BigDecimal> loanTokenToAmountMap = TransactionCheckerUtils.getTokenToAmountMap(loanAmounts);
    BigDecimal loanSpyAmount = loanTokenToAmountMap.getOrDefault("SPY", BigDecimal.ZERO);

    // ...
    BigDecimal ourSpyValue = ourSpyAmount.multiply(spyActivePrice);
    BigDecimal ourSpyPoolValue = ourSpyPoolAmount.multiply(spyActivePrice);
    BigDecimal loanSpyPoolValue = loanSpyAmount.multiply(spyActivePrice);
    BigDecimal ourDusdValue = ourDusdAmount.multiply(dusdActivePrice);
    BigDecimal ourDusdPoolValue = ourDusdPoolAmount.multiply(dusdActivePrice);

    BigDecimal liquidityDusdValue = liquidityDusdAmount.multiply(dusdActivePrice);
    BigDecimal collateralDusdValue = collateralDusdAmount.multiply(dusdActivePrice);

    // ...
    BigDecimal totalSpyValue = ourSpyValue.add(ourSpyPoolValue).subtract(loanSpyPoolValue);
    BigDecimal totalDusdValue = ourDusdValue.add(ourDusdPoolValue).add(liquidityDusdValue).add(collateralDusdValue);
    BigDecimal totalValue = totalSpyValue.add(totalDusdValue);

    // ...
    CellDataList cellDataList = new CellDataList();
    cellDataList.add(new CellData().setRowIndex(0).setCellIndex(1).setKeepStyle(true).setValue(reportingTimestamp));

    // Amount ...
    cellDataList.add(createCellData(3, ourSpyAmount));
    cellDataList.add(createCellData(4, ourDusdAmount));

    cellDataList.add(createCellData(6, ourSpyPoolAmount));
    cellDataList.add(createCellData(7, ourDusdPoolAmount));

    cellDataList.add(createCellData(9, loanSpyAmount));
    cellDataList.add(createCellData(10, collateralDusdAmount));

    cellDataList.add(createCellData(12, liquidityDusdAmount));

    // Value ...
    cellDataList.add(createCellData(16, spyActivePrice));
    cellDataList.add(createCellData(17, dusdActivePrice));

    cellDataList.add(createCellData(19, ourSpyValue));
    cellDataList.add(createCellData(20, ourDusdValue));

    cellDataList.add(createCellData(22, ourSpyPoolValue));
    cellDataList.add(createCellData(23, ourDusdPoolValue));

    cellDataList.add(createCellData(25, loanSpyPoolValue));
    cellDataList.add(createCellData(26, collateralDusdValue));

    cellDataList.add(createCellData(28, liquidityDusdValue));

    cellDataList.add(createCellData(30, totalSpyValue));
    cellDataList.add(createCellData(31, totalDusdValue));
    cellDataList.add(createCellData(32, totalValue));

    // Staking ...
    BigDecimal difference = totalStakingBalance.divide(totalValue, MATH_CONTEXT).setScale(SCALE, RoundingMode.HALF_UP);
    difference = BigDecimal.ONE.subtract(difference).abs();

    cellDataList.add(createCellData(35, totalStakingBalance));
    cellDataList.add(createCellData(36, difference));

    // ...
    logInfoList.add("Vault Balance:          " + GERMAN_DECIMAL_FORMAT.format(totalValue));
    logInfoList.add("Staking Balance:      " + GERMAN_DECIMAL_FORMAT.format(totalStakingBalance));
    logInfoList.add("Difference:                 " + GERMAN_PERCENTAGE_FORMAT.format(difference.multiply(HUNDERED)) + "%");

    return cellDataList;
  }

  /**
   * 
   */
  private CellData createCellData(int rowIndex, BigDecimal value) {
    return new CellData().setRowIndex(rowIndex).setCellIndex(1).setKeepStyle(true).setValue(value);
  }
}
