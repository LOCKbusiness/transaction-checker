package ch.dfx.reporting;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;

import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.excel.data.CellData;
import ch.dfx.excel.data.RowData;
import ch.dfx.excel.data.RowDataList;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.data.BalanceDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
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

  // ...
  private PreparedStatement transactionVoutSelectStatement = null;

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

    long startTime = System.currentTimeMillis();

    try {
      String googleRootPath = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_ROOT_PATH);
      String googleFileName = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.GOOGLE_LIQUIDITY_MASTERNODE_STAKING_BALANCE_SHEET);

      if (!StringUtils.isEmpty(googleFileName)) {
        Connection connection = databaseManager.openConnection();
        databaseHelper.openStatements(connection);
        openStatements(connection);

        List<StakingAddressDTO> stakingAddressDTOList = databaseHelper.getStakingAddressDTOList();
        List<MasternodeWhitelistDTO> masternodeWhitelistDTOList = databaseHelper.getMasternodeWhitelistDTOList();
        List<StakingDTO> stakingDTOList = databaseHelper.getStakingDTOList();

        RowDataList rowDataList = createRowDataList(stakingAddressDTOList, masternodeWhitelistDTOList, stakingDTOList);
        writeExcel(googleRootPath, googleFileName, rowDataList);

        closeStatements();
        databaseHelper.closeStatements();
        databaseManager.closeConnection(connection);
      }
    } finally {
      LOGGER.debug("runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String transactionVoutSelectSql =
          "SELECT"
              + " at_in.address_number,"
              + " at_out.block_number,"
              + " at_out.transaction_number,"
              + " at_out.vout_number,"
              + " at_out.vout"
              + " FROM public.address_transaction_in at_in"
              + " JOIN public.address_transaction_out at_out ON"
              + " at_in.block_number = at_out .block_number"
              + " AND at_in.transaction_number = at_out.transaction_number"
              + " AND at_in.address_number != at_out.address_number"
              + " WHERE"
              + " at_in.address_number not in (SELECT deposit_address_number FROM public.staking)"
              + " AND at_out.address_number=?"
              + " GROUP BY"
              + " at_in.address_number,"
              + " at_out.block_number,"
              + " at_out.transaction_number,"
              + " at_out.vout_number,"
              + " at_out.vout";
      transactionVoutSelectStatement = connection.prepareStatement(transactionVoutSelectSql);
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements()");

    try {
      transactionVoutSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private RowDataList createRowDataList(
      @Nonnull List<StakingAddressDTO> stakingAddressDTOList,
      @Nonnull List<MasternodeWhitelistDTO> masternodeWhitelistDTOList,
      @Nonnull List<StakingDTO> stakingDTOList) throws DfxException {
    LOGGER.trace("createRowDataList()");

    RowDataList rowDataList = new RowDataList();

    // ...
    BigDecimal liquidityMasternodeBalance = BigDecimal.ZERO;

    // ...
    BigDecimal liquidityBalance = fillLiquidityBalance(stakingAddressDTOList, rowDataList);
    liquidityMasternodeBalance = liquidityMasternodeBalance.add(liquidityBalance);

    // ...
    BigDecimal masternodeBalance = fillMasternodeBalance(masternodeWhitelistDTOList, rowDataList);
    liquidityMasternodeBalance = liquidityMasternodeBalance.add(masternodeBalance);

    BigDecimal otherBalance = fillWithDifferentOtherAmounts(stakingAddressDTOList, masternodeWhitelistDTOList, rowDataList);
    liquidityMasternodeBalance = liquidityMasternodeBalance.add(otherBalance);

    // ...
    BigDecimal stakingBalance = fillStakingBalance(stakingDTOList, rowDataList);
    BigDecimal finalBalance = liquidityMasternodeBalance.add(stakingBalance);

    // ...
    addEmptyLine(rowDataList);

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
      @Nonnull List<StakingAddressDTO> stakingAddressDTOList,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("fillLiquidityBalance()");

    BigDecimal liquidityBalance = BigDecimal.ZERO;

    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
        BalanceDTO liquidityBalanceDTO =
            databaseHelper.getBalanceDTOByAddressNumber(stakingAddressDTO.getLiquidityAddressNumber());

        if (null != liquidityBalanceDTO) {
          BigDecimal vout = liquidityBalanceDTO.getVout();
          BigDecimal vin = liquidityBalanceDTO.getVin();

          liquidityBalance = liquidityBalance.add(vout.subtract(vin));
        }
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

    // enabled ...
    long numberOfEnabledMasternodes = masternodeWhitelistDTOList.stream().filter(dto -> "ENABLED".equals(dto.getState())).count();
    BigDecimal masternodeBalance = BigDecimal.valueOf(MASTERNODE_BALANCE).multiply(BigDecimal.valueOf(numberOfEnabledMasternodes));
    totalBalance = totalBalance.add(masternodeBalance);

    // ...
    addEmptyLine(rowDataList);

    RowData enabledRowData = new RowData();
    enabledRowData.addCellData(new CellData().setValue("Masternode (enabled):"));
    enabledRowData.addCellData(new CellData().setValue(numberOfEnabledMasternodes));
    enabledRowData.addCellData(new CellData().setValue(MASTERNODE_BALANCE));
    enabledRowData.addCellData(new CellData().setValue(masternodeBalance));

    rowDataList.add(enabledRowData);

    // pre enabled ...
    long numberOfPreEnabledMasternodes = masternodeWhitelistDTOList.stream().filter(dto -> "PRE_ENABLED".equals(dto.getState())).count();
    BigDecimal preEnabledMasternodeBalance = BigDecimal.valueOf(MASTERNODE_BALANCE).multiply(BigDecimal.valueOf(numberOfPreEnabledMasternodes));
    totalBalance = totalBalance.add(preEnabledMasternodeBalance);

    // ...
    RowData preEnabledRowData = new RowData();
    preEnabledRowData.addCellData(new CellData().setValue("Masternode (pre enabled):"));
    preEnabledRowData.addCellData(new CellData().setValue(numberOfPreEnabledMasternodes));
    preEnabledRowData.addCellData(new CellData().setValue(MASTERNODE_BALANCE));
    preEnabledRowData.addCellData(new CellData().setValue(preEnabledMasternodeBalance));

    rowDataList.add(preEnabledRowData);

    // pre resigned ...
    long numberOfPreResignedMasternodes = masternodeWhitelistDTOList.stream().filter(dto -> "PRE_RESIGNED".equals(dto.getState())).count();
    BigDecimal preResignedMasternodeBalance = BigDecimal.valueOf(MASTERNODE_BALANCE).multiply(BigDecimal.valueOf(numberOfPreResignedMasternodes));
    totalBalance = totalBalance.add(preResignedMasternodeBalance);

    // ...
    RowData preResignedRowData = new RowData();
    preResignedRowData.addCellData(new CellData().setValue("Masternode (pre resigned):"));
    preResignedRowData.addCellData(new CellData().setValue(numberOfPreResignedMasternodes));
    preResignedRowData.addCellData(new CellData().setValue(MASTERNODE_BALANCE));
    preResignedRowData.addCellData(new CellData().setValue(preResignedMasternodeBalance));

    rowDataList.add(preResignedRowData);

    // resigned ...
    long numberOfResignedMasternodes = masternodeWhitelistDTOList.stream().filter(dto -> "RESIGNED".equals(dto.getState())).count();
    BigDecimal resignedMasternodeBalance = BigDecimal.valueOf(MASTERNODE_BALANCE).multiply(BigDecimal.valueOf(numberOfResignedMasternodes));

    totalBalance = totalBalance.add(resignedMasternodeBalance);

    // ...
    RowData resignedRowData = new RowData();
    resignedRowData.addCellData(new CellData().setValue("Masternode (resigned):"));
    resignedRowData.addCellData(new CellData().setValue(numberOfResignedMasternodes));
    resignedRowData.addCellData(new CellData().setValue(MASTERNODE_BALANCE));
    resignedRowData.addCellData(new CellData().setValue(resignedMasternodeBalance));

    rowDataList.add(resignedRowData);

    // total fee ...
    long totalNumberOfMasternodes = masternodeWhitelistDTOList.stream().filter(dto -> null != dto.getState()).count();
    BigDecimal totalMasternodeFee = BigDecimal.valueOf(MASTERNODE_FEE).multiply(BigDecimal.valueOf(totalNumberOfMasternodes));
    totalBalance = totalBalance.add(totalMasternodeFee);

    RowData totalFeeRowData = new RowData();
    totalFeeRowData.addCellData(new CellData().setValue("Masternode Fee:"));
    totalFeeRowData.addCellData(new CellData().setValue(totalNumberOfMasternodes));
    totalFeeRowData.addCellData(new CellData().setValue(MASTERNODE_FEE));
    totalFeeRowData.addCellData(new CellData().setValue(totalMasternodeFee));

    rowDataList.add(totalFeeRowData);

    return totalBalance;
  }

  /**
   * 
   */
  private BigDecimal fillWithDifferentOtherAmounts(
      @Nonnull List<StakingAddressDTO> stakingAddressDTOList,
      @Nonnull List<MasternodeWhitelistDTO> masternodeWhitelistDTOList,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("fillWithDifferentOtherAmounts()");

    Set<Integer> rewardAddressNumberSet = createRewardAddressNumberSet(stakingAddressDTOList);
    Set<Integer> masternodeAddressNumberSet = createMasternodeAddressNumberSet(masternodeWhitelistDTOList);

    BigDecimal totalDifferentOtherAmount = BigDecimal.ZERO;

    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
        totalDifferentOtherAmount = totalDifferentOtherAmount.subtract(
            fillWithDifferentOtherAmounts(stakingAddressDTO.getLiquidityAddressNumber(), rewardAddressNumberSet, masternodeAddressNumberSet, rowDataList));
      }
    }

    return totalDifferentOtherAmount;
  }

  /**
   * 
   */
  private Set<Integer> createRewardAddressNumberSet(@Nonnull List<StakingAddressDTO> stakingAddressDTOList) {
    Set<Integer> rewardAddressNumberSet = new HashSet<>();

    stakingAddressDTOList.stream()
        .filter(dto -> -1 != dto.getRewardAddressNumber())
        .forEach(dto -> rewardAddressNumberSet.add(dto.getRewardAddressNumber()));

    return rewardAddressNumberSet;
  }

  /**
   * 
   */
  private Set<Integer> createMasternodeAddressNumberSet(@Nonnull List<MasternodeWhitelistDTO> masternodeWhitelistDTOList) throws DfxException {
    Set<Integer> masternodeAddressNumberSet = new HashSet<>();

    for (MasternodeWhitelistDTO masternodeWhitelistDTO : masternodeWhitelistDTOList) {
      String ownerAddress = masternodeWhitelistDTO.getOwnerAddress();
      AddressDTO ownerAddressDTO = databaseHelper.getAddressDTOByAddress(ownerAddress);

      if (null != ownerAddressDTO) {
        masternodeAddressNumberSet.add(ownerAddressDTO.getNumber());
      }
    }

    return masternodeAddressNumberSet;
  }

  /**
   * 
   */
  private BigDecimal fillWithDifferentOtherAmounts(
      int liquidityAddressNumber,
      @Nonnull Set<Integer> rewardAddressNumberSet,
      @Nonnull Set<Integer> masternodeAddressNumberSet,
      @Nonnull RowDataList rowDataList) throws DfxException {
    LOGGER.trace("fillWithDifferentOtherAmounts()");

    try {
      transactionVoutSelectStatement.setInt(1, liquidityAddressNumber);

      ArrayListMultimap<String, Integer> groupAddressMap = ArrayListMultimap.create();
      Map<String, BigDecimal> groupVoutMap = new HashMap<>();

      ResultSet resultSet = transactionVoutSelectStatement.executeQuery();

      while (resultSet.next()) {
        int x = resultSet.getInt("block_number");
        int y = resultSet.getInt("transaction_number");
        int z = resultSet.getInt("vout_number");
        String group = x + "/" + y + "/" + z;

        groupAddressMap.put(group, resultSet.getInt("address_number"));
        groupVoutMap.put(group, resultSet.getBigDecimal("vout"));
      }

      resultSet.close();

      // ...
      BigDecimal masternodePaybackFee = BigDecimal.ZERO;
      BigDecimal masternodeResignReturn = BigDecimal.ZERO;
      BigDecimal unknownAmount = BigDecimal.ZERO;

      for (Entry<String, BigDecimal> groupVoutMapEntry : groupVoutMap.entrySet()) {
        String group = groupVoutMapEntry.getKey();
        BigDecimal vout = groupVoutMapEntry.getValue();

        boolean addressFound = false;

        for (Integer addressNumber : groupAddressMap.get(group)) {
          if (rewardAddressNumberSet.contains(addressNumber)) {
            masternodePaybackFee = masternodePaybackFee.subtract(vout);
            addressFound = true;
            break;
          } else if (masternodeAddressNumberSet.contains(addressNumber)) {
            masternodeResignReturn = masternodeResignReturn.subtract(vout);
            addressFound = true;
            break;
          }
        }

        if (!addressFound) {
          unknownAmount = unknownAmount.subtract(vout);
        }
      }

      // ...
      BigDecimal differentOtherAmount = BigDecimal.ZERO;
      differentOtherAmount = differentOtherAmount.subtract(masternodePaybackFee);
      differentOtherAmount = differentOtherAmount.subtract(masternodeResignReturn);
      differentOtherAmount = differentOtherAmount.subtract(unknownAmount);

      // ...
      addEmptyLine(rowDataList);

      RowData rowData1 = new RowData();
      rowData1.addCellData(new CellData().setValue("Masternode Payback Fee:"));
      rowData1.addCellData(new CellData().setValue(null));
      rowData1.addCellData(new CellData().setValue(null));
      rowData1.addCellData(new CellData().setValue(masternodePaybackFee));

      rowDataList.add(rowData1);

      RowData rowData2 = new RowData();
      rowData2.addCellData(new CellData().setValue("Masternode Resign Return:"));
      rowData2.addCellData(new CellData().setValue(null));
      rowData2.addCellData(new CellData().setValue(null));
      rowData2.addCellData(new CellData().setValue(masternodeResignReturn));

      rowDataList.add(rowData2);

      RowData rowData3 = new RowData();
      rowData3.addCellData(new CellData().setValue("Unknown Amount:"));
      rowData3.addCellData(new CellData().setValue(null));
      rowData3.addCellData(new CellData().setValue(null));
      rowData3.addCellData(new CellData().setValue(unknownAmount));

      rowDataList.add(rowData3);

      return differentOtherAmount;
    } catch (Exception e) {
      throw new DfxException("fillWithDifferentOtherAmounts", e);
    }
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

      stakingBalance = stakingBalance.subtract(vin.subtract(vout));
    }

    addEmptyLine(rowDataList);

    RowData rowData = new RowData();
    rowData.addCellData(new CellData().setValue("Staking Balance:"));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(stakingBalance));

    rowDataList.add(rowData);

    return stakingBalance;
  }

  /**
   * 
   */
  private void addEmptyLine(@Nonnull RowDataList rowDataList) {
    RowData rowData = new RowData();
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(null));
    rowData.addCellData(new CellData().setValue(null));

    rowDataList.add(rowData);

  }
}
