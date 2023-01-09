package ch.dfx.statistik;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.builder.DepositBuilder;
import ch.dfx.transactionserver.data.BlockDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class StatistikProvider extends DepositBuilder {
  private static final Logger LOGGER = LogManager.getLogger(StatistikProvider.class);

  // ...
  private PreparedStatement depositCountSelectStatement = null;

  private PreparedStatement depositSumVinSelectStatement = null;
  private PreparedStatement depositSumVoutSelectStatement = null;

  // ...
  private final NetworkEnum network;
  private final H2DBManager databaseManager;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public StatistikProvider(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    super(network, databaseManager);

    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);
  }

  /**
   * 
   */
  public void fillDepositStatistikData(
      @Nonnull TokenEnum token,
      @Nonnull Map<LocalDate, Integer> dateToCountMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVinMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVoutMap) throws DfxException {
    LOGGER.trace("fillDepositStatistikData()");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);
      databaseBalanceHelper.openStatements(connection);
      openStatements(connection);

      // ...
      List<StakingAddressDTO> stakingAddressDTOList =
          databaseBalanceHelper.getStakingAddressDTOList(token)
              .stream().filter(dto -> -1 == dto.getRewardAddressNumber()).collect(Collectors.toList());

      for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
        BlockDTO blockDTO = databaseBlockHelper.getBlockDTOByNumber(stakingAddressDTO.getStartBlockNumber());

        if (null != blockDTO) {
          int liquidityAddressNumber = stakingAddressDTO.getLiquidityAddressNumber();
          LocalDate startDate = new Date(blockDTO.getTimestamp() * 1000).toLocalDate();
          fillDateToCountMap(token, startDate, dateToCountMap);

          fillDateToVinMap(token, liquidityAddressNumber, startDate, dateToSumVinMap);
          fillDateToVoutMap(token, liquidityAddressNumber, startDate, dateToSumVoutMap);
        }
      }

      // ...
      closeStatements();
      databaseBalanceHelper.closeStatements();
      databaseBlockHelper.closeStatements();

    } catch (Exception e) {
      throw new DfxException("fillDepositStatistikData", e);
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Deposit ...
      String depositCountSelectSql =
          "SELECT COUNT(*) AS count"
              + " FROM " + TOKEN_NETWORK_SCHEMA + ".deposit d"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".block b ON"
              + " d.start_block_number = b.number"
              + " WHERE"
              + " d.token_number=?"
              + " AND b.timestamp BETWEEN ? AND ?";
      depositCountSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositCountSelectSql));

      String depositSumVinSelectSql =
          "WITH X AS ("
              + " SELECT"
              + " at_in.vin"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " b.number = at_out.block_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " b.timestamp>=? AND b.timestamp<=?"
              + " AND at_out.address_number=?"
              + " AND at_in.address_number IN"
              + " (SELECT deposit_address_number FROM " + TOKEN_NETWORK_SCHEMA + ".deposit"
              + " WHERE token_number=? AND liquidity_address_number=?)"
              + " GROUP BY"
              + " at_out.block_number,"
              + " at_in.in_block_number,"
              + " at_in.in_transaction_number,"
              + " at_in.address_number,"
              + " at_in.vin"
              + ")"
              + " SELECT"
              + " SUM(vin) AS sum_vin"
              + " FROM X";
      depositSumVinSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositSumVinSelectSql));

      String depositSumVoutSelectSql =
          "WITH X AS ("
              + " SELECT"
              + " at_out.vout"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " b.number = at_out.block_number"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " b.timestamp>=? AND b.timestamp<=?"
              + " AND at_in.address_number=?"
              + " AND at_out.address_number IN"
              + " (SELECT customer_address_number FROM " + TOKEN_NETWORK_SCHEMA + ".deposit"
              + " WHERE token_number=? AND liquidity_address_number=?)"
              + " GROUP BY"
              + " at_out.block_number,"
              + " at_out.transaction_number,"
              + " at_out.address_number,"
              + " at_out.vout"
              + ")"
              + " SELECT"
              + " SUM(vout) AS sum_vout"
              + " FROM X";
      depositSumVoutSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositSumVoutSelectSql));

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
      depositCountSelectStatement.close();

      depositSumVinSelectStatement.close();
      depositSumVoutSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void fillDateToCountMap(
      @Nonnull TokenEnum token,
      @Nonnull LocalDate startDate,
      @Nonnull Map<LocalDate, Integer> dateToCountMap) throws DfxException {
    LOGGER.trace("fillDateToCountMap()");

    // ...
    LocalDate workDate = startDate;
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      LOGGER.debug("fillDateToCountMap: " + workDate);

      int count = getCount(token, workDate);

      dateToCountMap.merge(workDate, count, (currVal, newVal) -> currVal += newVal);

      workDate = workDate.plusDays(1);
    }
  }

  /**
   * 
   */
  private int getCount(
      @Nonnull TokenEnum token,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getCount()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositCountSelectStatement.setInt(1, token.getNumber());
      depositCountSelectStatement.setLong(2, timestampOfDayArray[0]);
      depositCountSelectStatement.setLong(3, timestampOfDayArray[1]);

      ResultSet resultSet = depositCountSelectStatement.executeQuery();

      int count = 0;

      if (resultSet.next()) {
        count = resultSet.getInt("count");
      }

      resultSet.close();

      return count;
    } catch (Exception e) {
      throw new DfxException("getCount", e);
    }
  }

  /**
   * 
   */
  private void fillDateToVinMap(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate startDate,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVinMap) throws DfxException {
    LOGGER.trace("fillDateToVinMap()");

    // ...
    LocalDate workDate = startDate;
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      LOGGER.debug("fillDateToVinMap: " + workDate);

      BigDecimal sumVin = getSumVin(token, liquidityAddressNumber, workDate);

      dateToSumVinMap.merge(workDate, sumVin, (currVal, newVal) -> currVal.add(newVal));

      workDate = workDate.plusDays(1);
    }
  }

  /**
   * 
   */
  private BigDecimal getSumVin(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getSumVin()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositSumVinSelectStatement.setLong(1, timestampOfDayArray[0]);
      depositSumVinSelectStatement.setLong(2, timestampOfDayArray[1]);
      depositSumVinSelectStatement.setInt(3, liquidityAddressNumber);
      depositSumVinSelectStatement.setInt(4, token.getNumber());
      depositSumVinSelectStatement.setInt(5, liquidityAddressNumber);

      ResultSet resultSet = depositSumVinSelectStatement.executeQuery();

      BigDecimal sumVin = null;

      if (resultSet.next()) {
        sumVin = resultSet.getBigDecimal("sum_vin");
      }

      if (null == sumVin) {
        sumVin = BigDecimal.ZERO;
      }

      resultSet.close();

      return sumVin;
    } catch (Exception e) {
      throw new DfxException("getSumVin", e);
    }
  }

  /**
   * 
   */
  private void fillDateToVoutMap(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate startDate,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVoutMap) throws DfxException {
    LOGGER.trace("fillDateToVoutMap()");

    // ...
    LocalDate workDate = startDate;
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      LOGGER.debug("fillDateToVoutMap: " + workDate);

      BigDecimal sumVout = getSumVout(token, liquidityAddressNumber, workDate);

      dateToSumVoutMap.merge(workDate, sumVout, (currVal, newVal) -> currVal.add(newVal));

      workDate = workDate.plusDays(1);
    }
  }

  /**
   * 
   */
  private BigDecimal getSumVout(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getSumVout()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositSumVoutSelectStatement.setLong(1, timestampOfDayArray[0]);
      depositSumVoutSelectStatement.setLong(2, timestampOfDayArray[1]);
      depositSumVoutSelectStatement.setInt(3, liquidityAddressNumber);
      depositSumVoutSelectStatement.setInt(4, token.getNumber());
      depositSumVoutSelectStatement.setInt(5, liquidityAddressNumber);

      ResultSet resultSet = depositSumVoutSelectStatement.executeQuery();

      BigDecimal sumVin = null;

      if (resultSet.next()) {
        sumVin = resultSet.getBigDecimal("sum_vout");
      }

      if (null == sumVin) {
        sumVin = BigDecimal.ZERO;
      }

      resultSet.close();

      return sumVin;
    } catch (Exception e) {
      throw new DfxException("getSumVout", e);
    }
  }

  /**
   * 
   */
  private long[] getTimestampOfDay(@Nonnull LocalDate localDate) {
    long startTimeOfDay = Timestamp.valueOf(LocalDateTime.of(localDate, LocalTime.MIN)).getTime() / 1000;
    long endTimeOfDay = Timestamp.valueOf(LocalDateTime.of(localDate, LocalTime.MAX)).getTime() / 1000;

    return new long[] {
        startTimeOfDay, endTimeOfDay
    };
  }
}
