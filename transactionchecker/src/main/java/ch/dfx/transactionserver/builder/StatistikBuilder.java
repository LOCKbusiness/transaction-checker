package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.BlockDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StatistikDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;
import ch.dfx.transactionserver.database.helper.DatabaseStatistikHelper;

/**
 * 
 */
public class StatistikBuilder {
  private static final Logger LOGGER = LogManager.getLogger(StatistikBuilder.class);

  // ...
  private PreparedStatement depositCountSelectStatement = null;

  // ...
  private final NetworkEnum network;

  private final H2DBManager databaseManager;

  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseBalanceHelper;
  private final DatabaseStatistikHelper databaseStatistikHelper;

  /**
   * 
   */
  public StatistikBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);
    this.databaseStatistikHelper = new DatabaseStatistikHelper(network);
  }

  /**
   * 
   */
  public void build(@Nonnull TokenEnum token) throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);
      databaseBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);
      databaseStatistikHelper.openStatements(connection);
      openStatements(connection);

      List<StatistikDTO> statistikDTOList = databaseStatistikHelper.getStatistikDTOList(token);
      Set<LocalDate> daySet = new HashSet<>();
      statistikDTOList.forEach(dto -> daySet.add(dto.getDay()));

      // ...
      List<StakingAddressDTO> stakingAddressDTOList =
          databaseBalanceHelper.getStakingAddressDTOList()
              .stream().filter(dto -> -1 == dto.getRewardAddressNumber()).collect(Collectors.toList());

      List<StatistikDTO> newStatistikDTOList = new ArrayList<>();

      for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
        BlockDTO blockDTO = databaseBlockHelper.getBlockDTOByNumber(stakingAddressDTO.getStartBlockNumber());

        if (null != blockDTO) {
          LocalDate startDate = new Date(blockDTO.getTimestamp() * 1000).toLocalDate();
          newStatistikDTOList.addAll(createStatistikDTOList(token, startDate, daySet));
        }
      }

      // ...
      closeStatements();
      databaseStatistikHelper.closeStatements();
      databaseBalanceHelper.closeStatements();
      databaseBlockHelper.closeStatements();

      // ...
      if (newStatistikDTOList.isEmpty()) {
        saveStatistik(newStatistikDTOList);
        connection.commit();
      }
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("[StatistikBuilder] runtime: " + (System.currentTimeMillis() - startTime));
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
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private List<StatistikDTO> createStatistikDTOList(
      @Nonnull TokenEnum token,
      @Nonnull LocalDate startDate,
      @Nonnull Set<LocalDate> daySet) throws DfxException {
    LOGGER.trace("fillDateToCountMap()");

    List<StatistikDTO> statistikDTOList = new ArrayList<>();

    // ...
    LocalDate workDate = startDate;
    LocalDate endDate = LocalDate.now().plusDays(1);

    // ...
    while (workDate.isBefore(endDate)) {
      if (!daySet.contains(workDate)) {
        int count = getCount(token, workDate);

        StatistikDTO statistikDTO = new StatistikDTO(workDate, token.getNumber());
        statistikDTO.setDepositCount(count);

        statistikDTOList.add(statistikDTO);
      }

      workDate = workDate.plusDays(1);
    }

    return statistikDTOList;
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
  private long[] getTimestampOfDay(@Nonnull LocalDate localDate) {
    long startTimeOfDay = Timestamp.valueOf(LocalDateTime.of(localDate, LocalTime.MIN)).getTime() / 1000;
    long endTimeOfDay = Timestamp.valueOf(LocalDateTime.of(localDate, LocalTime.MAX)).getTime() / 1000;

    return new long[] {
        startTimeOfDay, endTimeOfDay
    };
  }

  /**
   * 
   */
  private void saveStatistik(@Nonnull List<StatistikDTO> statistikDTOList) throws DfxException {
    LOGGER.trace("saveStatistik()");

    try {
      for (StatistikDTO statistikDTO : statistikDTOList) {

      }

    } catch (Exception e) {
      throw new DfxException("saveStatistik", e);
    }
  }
}
