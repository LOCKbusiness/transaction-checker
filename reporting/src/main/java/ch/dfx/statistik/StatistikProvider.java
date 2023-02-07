package ch.dfx.statistik;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;

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
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class StatistikProvider extends DepositBuilder {
  private static final Logger LOGGER = LogManager.getLogger(StatistikProvider.class);

  // ...
  private PreparedStatement depositCountSelectStatement = null;

  private PreparedStatement depositDfiSumVinSelectStatement = null;
  private PreparedStatement depositDfiSumVoutSelectStatement = null;

  private PreparedStatement depositDusdSumVinSelectStatement = null;
  private PreparedStatement depositDusdSumVoutSelectStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public StatistikProvider(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBalanceHelper);

    this.network = network;

    this.databaseBlockHelper = databaseBlockHelper;
    this.databaseBalanceHelper = databaseBalanceHelper;
  }

  /**
   * 
   */
  public void fillDepositStatistikData(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull Map<LocalDate, Integer> dateToCountMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVinMap,
      @Nonnull Map<LocalDate, BigDecimal> dateToSumVoutMap) throws DfxException {
    LOGGER.trace("fillDfiDepositStatistikData()");

    try {
      openStatements(connection);

      // ...
      List<StakingAddressDTO> stakingAddressDTOList =
          databaseBalanceHelper.getStakingAddressDTOList()
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
    } catch (Exception e) {
      throw new DfxException("fillDepositStatistikData", e);
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
              + " FROM " + TOKEN_STAKING_SCHEMA + ".deposit d"
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
              + " (SELECT deposit_address_number FROM " + TOKEN_STAKING_SCHEMA + ".deposit"
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
      depositDfiSumVinSelectStatement =
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
              + " (SELECT customer_address_number FROM " + TOKEN_STAKING_SCHEMA + ".deposit"
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
      depositDfiSumVoutSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositSumVoutSelectSql));

      String depositDusdSumVinSelectSql =
          "SELECT"
              + " SUM(ata_in.amount) AS sum_vin"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out ON"
              + " b.number = ata_out.block_number"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in ata_in ON"
              + " ata_out.block_number = ata_in.block_number"
              + " AND ata_out.transaction_number = ata_in.transaction_number"
              + " AND ata_out.type_number = ata_in.type_number"
              + " AND ata_out.token_number = ata_in.token_number"
              + " WHERE"
              + " b.timestamp>=? AND b.timestamp<=?"
              + " AND ata_out.token_number=?"
              + " AND ata_out.address_number=?"
              + " AND ata_in.address_number IN ("
              + " SELECT deposit_address_number"
              + " FROM mainnet.deposit"
              + " WHERE token_number=?)";
      depositDusdSumVinSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositDusdSumVinSelectSql));

      String depositDusdSumVoutSelectSql =
          "SELECT"
              + " SUM(ata_in.amount) AS sum_vout"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".block b"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out ON"
              + " b.number = ata_out.block_number"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in ata_in ON"
              + " ata_out.block_number = ata_in.block_number"
              + " AND ata_out.transaction_number = ata_in.transaction_number"
              + " AND ata_out.type_number = ata_in.type_number"
              + " AND ata_out.token_number = ata_in.token_number"
              + " WHERE"
              + " b.timestamp>=? AND b.timestamp<=?"
              + " AND ata_out.token_number=?"
              + " AND ata_out.address_number IN ("
              + " SELECT customer_address_number"
              + " FROM mainnet.deposit"
              + " WHERE token_number=?)"
              + " AND ata_in.address_number=?";
      depositDusdSumVoutSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositDusdSumVoutSelectSql));
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

      depositDfiSumVinSelectStatement.close();
      depositDfiSumVoutSelectStatement.close();

      depositDusdSumVinSelectStatement.close();
      depositDusdSumVoutSelectStatement.close();
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
      BigDecimal sumVin;

      if (TokenEnum.DFI == token) {
        sumVin = getDfiSumVin(token, liquidityAddressNumber, workDate);
      } else {
        sumVin = getDusdSumVin(token, liquidityAddressNumber, workDate);
      }

      dateToSumVinMap.merge(workDate, sumVin, (currVal, newVal) -> currVal.add(newVal));

      workDate = workDate.plusDays(1);
    }
  }

  /**
   * 
   */
  private BigDecimal getDfiSumVin(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getDfiSumVin()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositDfiSumVinSelectStatement.setLong(1, timestampOfDayArray[0]);
      depositDfiSumVinSelectStatement.setLong(2, timestampOfDayArray[1]);
      depositDfiSumVinSelectStatement.setInt(3, liquidityAddressNumber);
      depositDfiSumVinSelectStatement.setInt(4, token.getNumber());
      depositDfiSumVinSelectStatement.setInt(5, liquidityAddressNumber);

      ResultSet resultSet = depositDfiSumVinSelectStatement.executeQuery();

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
      throw new DfxException("getDfiSumVin", e);
    }
  }

  /**
   * 
   */
  private BigDecimal getDusdSumVin(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getDusdSumVin()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositDusdSumVinSelectStatement.setLong(1, timestampOfDayArray[0]);
      depositDusdSumVinSelectStatement.setLong(2, timestampOfDayArray[1]);
      depositDusdSumVinSelectStatement.setInt(3, token.getNumber());
      depositDusdSumVinSelectStatement.setInt(4, liquidityAddressNumber);
      depositDusdSumVinSelectStatement.setInt(5, token.getNumber());

      ResultSet resultSet = depositDusdSumVinSelectStatement.executeQuery();

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
      throw new DfxException("getDusdSumVin", e);
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
      BigDecimal sumVout;

      if (TokenEnum.DFI == token) {
        sumVout = getDfiSumVout(token, liquidityAddressNumber, workDate);
      } else {
        sumVout = getDusdSumVout(token, liquidityAddressNumber, workDate);
      }

      dateToSumVoutMap.merge(workDate, sumVout, (currVal, newVal) -> currVal.add(newVal));

      workDate = workDate.plusDays(1);
    }
  }

  /**
   * 
   */
  private BigDecimal getDfiSumVout(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getDfiSumVout()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositDfiSumVoutSelectStatement.setLong(1, timestampOfDayArray[0]);
      depositDfiSumVoutSelectStatement.setLong(2, timestampOfDayArray[1]);
      depositDfiSumVoutSelectStatement.setInt(3, liquidityAddressNumber);
      depositDfiSumVoutSelectStatement.setInt(4, token.getNumber());
      depositDfiSumVoutSelectStatement.setInt(5, liquidityAddressNumber);

      ResultSet resultSet = depositDfiSumVoutSelectStatement.executeQuery();

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
      throw new DfxException("getDfiSumVout", e);
    }
  }

  /**
   * 
   */
  private BigDecimal getDusdSumVout(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getDusdSumVout()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositDusdSumVoutSelectStatement.setLong(1, timestampOfDayArray[0]);
      depositDusdSumVoutSelectStatement.setLong(2, timestampOfDayArray[1]);
      depositDusdSumVoutSelectStatement.setInt(3, token.getNumber());
      depositDusdSumVoutSelectStatement.setInt(4, token.getNumber());
      depositDusdSumVoutSelectStatement.setInt(5, liquidityAddressNumber);

      ResultSet resultSet = depositDusdSumVoutSelectStatement.executeQuery();

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
      throw new DfxException("getDusdSumVout", e);
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
