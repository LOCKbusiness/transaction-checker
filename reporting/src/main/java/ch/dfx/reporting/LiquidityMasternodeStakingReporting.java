package ch.dfx.reporting;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.LiquidityDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class LiquidityMasternodeStakingReporting extends Reporting {
  private static final Logger LOGGER = LogManager.getLogger(LiquidityMasternodeStakingReporting.class);

  // ...
  private static final int MASTERNODE_BALANCE = 20000;
  private static final int MASTERNODE_FEE = 10;

  /**
   * 
   */
  public LiquidityMasternodeStakingReporting(@Nonnull H2DBManager databaseManager) {
    super(databaseManager);
  }

  /**
   * 
   */
  public void report() throws DfxException {
    LOGGER.debug("report()");

    String googleRootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
    String googleFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_LIQUIDITY_MASTERNODE_STAKING_BALANCE_SHEET);

    if (!StringUtils.isEmpty(googleFileName)) {
      Connection connection = databaseManager.openConnection();
      databaseHelper.openStatements(connection);

      List<LiquidityDTO> liquidityDTOList = databaseHelper.getLiquidityDTOList();
      List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = databaseHelper.getMasternodeWhitelistDTOList();
      List<StakingDTO> stakingDTOList = databaseHelper.getStakingDTOList();

      RowDataList rowDataList = createRowDataList(liquidityDTOList, masternodeWhitelistDTOList, stakingDTOList);
      writeExcel(googleRootPath, googleFileName, rowDataList);

      databaseHelper.closeStatements();
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private RowDataList createRowDataList(
      @Nonnull List<LiquidityDTO> liquidityDTOList,
      @Nonnull List<MasternodeWhitelistDTO> masternodeWhitelistDTOList,
      @Nonnull List<StakingDTO> stakingDTOList) throws DfxException {
    LOGGER.trace("createRowDataList()");

    RowDataList rowDataList = new RowDataList();

    // ...
    BigDecimal liquidityMasternodeBalance = BigDecimal.ZERO;

    // ...
    BigDecimal liquidityBalance = fillLiquidityBalance(liquidityDTOList, rowDataList);
    liquidityMasternodeBalance = liquidityMasternodeBalance.add(liquidityBalance);

    // ...
    BigDecimal masternodeBalance = fillMasternodeBalance(masternodeWhitelistDTOList, rowDataList);
    liquidityMasternodeBalance = liquidityMasternodeBalance.add(masternodeBalance);

    // ...
    BigDecimal stakingBalance = fillStakingBalance(stakingDTOList, rowDataList);
    BigDecimal finalBalance = liquidityMasternodeBalance.subtract(stakingBalance);

    // ...
    RowData rowData = new RowData();
    rowData.addCellData(new CellData().setValue("Differenz:"));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(finalBalance));

    rowDataList.add(rowData);

    return rowDataList;
  }

  /**
   * 
   */
  private BigDecimal fillLiquidityBalance(
      @Nonnull List<LiquidityDTO> liquidityDTOList,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("fillLiquidityBalance()");

    BigDecimal liquidityBalance = BigDecimal.ZERO;

    for (LiquidityDTO liquidityDTO : liquidityDTOList) {
      BalanceDTO liquidityBalanceDTO =
          databaseHelper.getBalanceDTOByAddressNumber(liquidityDTO.getAddressNumber());
      if (null != liquidityBalanceDTO) {
        BigDecimal vout = liquidityBalanceDTO.getVout();
        BigDecimal vin = liquidityBalanceDTO.getVin();

        liquidityBalance = liquidityBalance.add(vout.subtract(vin));
      }
    }

    // ...
    RowData rowData = new RowData();
    rowData.addCellData(new CellData().setValue("Liquidity Balance:"));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(liquidityBalance));

    rowDataList.add(rowData);

    return liquidityBalance;
  }

  /**
   * 
   */
  private BigDecimal fillMasternodeBalance(
      @Nonnull List<MasternodeWhitelistDTO> masternodeWhitelistDTOList,
      @Nonnull RowDataList rowDataList) {
    LOGGER.trace("fillMasternodeBalance()");

    BigDecimal totalBalance = BigDecimal.ZERO;

    // ...
    long numberOfEnabledMasternodes = masternodeWhitelistDTOList.stream().filter(dto -> "ENABLED".equals(dto.getState())).count();
    BigDecimal masternodeBalance = BigDecimal.valueOf(MASTERNODE_BALANCE).multiply(BigDecimal.valueOf(numberOfEnabledMasternodes));
    totalBalance = totalBalance.add(masternodeBalance);

    // ...
    RowData rowData1 = new RowData();
    rowData1.addCellData(new CellData().setValue("Masternode (enabled):"));
    rowData1.addCellData(new CellData().setValue(numberOfEnabledMasternodes));
    rowData1.addCellData(new CellData().setValue(MASTERNODE_BALANCE));
    rowData1.addCellData(new CellData().setValue(masternodeBalance));

    rowDataList.add(rowData1);

    // ...
    long numberOfPreResignedMasternodes = masternodeWhitelistDTOList.stream().filter(dto -> "PRE_RESIGNED".equals(dto.getState())).count();
    BigDecimal preresignedMasternodeBalance = BigDecimal.valueOf(MASTERNODE_BALANCE).multiply(BigDecimal.valueOf(numberOfPreResignedMasternodes));
    totalBalance = totalBalance.add(preresignedMasternodeBalance);

    // ...
    RowData rowData2 = new RowData();
    rowData2.addCellData(new CellData().setValue("Masternode (pre resigned):"));
    rowData2.addCellData(new CellData().setValue(numberOfPreResignedMasternodes));
    rowData2.addCellData(new CellData().setValue(MASTERNODE_BALANCE));
    rowData2.addCellData(new CellData().setValue(preresignedMasternodeBalance));

    rowDataList.add(rowData2);

    // ...
    long totalNumberOfMasternodes = masternodeWhitelistDTOList.stream().filter(dto -> null != dto.getState()).count();
    BigDecimal totalMasternodeFee = BigDecimal.valueOf(MASTERNODE_FEE).multiply(BigDecimal.valueOf(totalNumberOfMasternodes));
    totalBalance = totalBalance.add(totalMasternodeFee);

    RowData rowData3 = new RowData();
    rowData3.addCellData(new CellData().setValue("Masternode Fee:"));
    rowData3.addCellData(new CellData().setValue(totalNumberOfMasternodes));
    rowData3.addCellData(new CellData().setValue(MASTERNODE_FEE));
    rowData3.addCellData(new CellData().setValue(totalMasternodeFee));

    rowDataList.add(rowData3);

    return totalBalance;
  }

  /**
   * 
   */
  private BigDecimal fillStakingBalance(
      @Nonnull List<StakingDTO> stakingDTOList,
      @Nonnull RowDataList rowDataList) {
    LOGGER.trace("fillStakingBalance()");

    BigDecimal stakingBalance = BigDecimal.ZERO;

    for (StakingDTO stakingDTO : stakingDTOList) {
      BigDecimal vin = stakingDTO.getVin();
      BigDecimal vout = stakingDTO.getVout();

      stakingBalance = stakingBalance.add(vin.subtract(vout));
    }

    RowData rowData = new RowData();
    rowData.addCellData(new CellData().setValue("Staking Balance:"));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(stakingBalance));

    rowDataList.add(rowData);

    return stakingBalance;
  }
}
