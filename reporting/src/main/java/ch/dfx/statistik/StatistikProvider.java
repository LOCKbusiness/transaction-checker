package ch.dfx.statistik;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
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
  private PreparedStatement depositSelectStatement = null;

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
  public Map<LocalDate, Integer> createStatistikData(@Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("createStatistikData()");

    long startTime = System.currentTimeMillis();

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);
      databaseBalanceHelper.openStatements(connection);
      openStatements(connection);

      // ...
      Map<LocalDate, Integer> dateToCountMap = new HashMap<>();

      // ...
      List<StakingAddressDTO> stakingAddressDTOList =
          databaseBalanceHelper.getStakingAddressDTOList(token)
              .stream().filter(dto -> -1 == dto.getRewardAddressNumber()).collect(Collectors.toList());

      for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
        BlockDTO blockDTO = databaseBlockHelper.getBlockDTOByNumber(stakingAddressDTO.getStartBlockNumber());

        if (null != blockDTO) {
          LocalDate startDate = new Date(blockDTO.getTimestamp() * 1000).toLocalDate();
          fillDateToCountMap(token, startDate, dateToCountMap);
        }
      }

      // ...
      closeStatements();
      databaseBalanceHelper.closeStatements();
      databaseBlockHelper.closeStatements();

      // ...
      return dateToCountMap;
    } catch (Exception e) {
      throw new DfxException("createStatistikData", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("[StatistikProvider] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Deposit ...
      String depositSelectSql =
          "SELECT COUNT(*) AS count"
              + " FROM " + TOKEN_NETWORK_SCHEMA + ".deposit d"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".block b ON"
              + " d.start_block_number = b.number"
              + " WHERE"
              + " d.token_number=?"
              + " AND b.timestamp BETWEEN ? AND ?";
      depositSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositSelectSql));
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
      depositSelectStatement.close();
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

      depositSelectStatement.setInt(1, token.getNumber());
      depositSelectStatement.setLong(2, timestampOfDayArray[0]);
      depositSelectStatement.setLong(3, timestampOfDayArray[1]);

      ResultSet resultSet = depositSelectStatement.executeQuery();

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
}
