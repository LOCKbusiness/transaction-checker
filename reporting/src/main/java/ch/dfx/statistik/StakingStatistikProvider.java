package ch.dfx.statistik;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class StakingStatistikProvider extends StatistikProvider {
  private static final Logger LOGGER = LogManager.getLogger(StakingStatistikProvider.class);

  // ...
  private PreparedStatement depositCountSelectStatement = null;

  private PreparedStatement depositSumVinSelectStatement = null;
  private PreparedStatement depositSumVoutSelectStatement = null;

  /**
   * 
   */
  public StakingStatistikProvider(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);
  }

  /**
   * 
   */
  @Override
  public void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // ...
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

      // ...
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
      depositSumVoutSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositSumVoutSelectSql));
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  @Override
  public void closeStatements() throws DfxException {
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
  @Override
  public int getCount(
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
  @Override
  public BigDecimal getSumVin(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getDfiSumVin()");

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
      throw new DfxException("getDfiSumVin", e);
    }
  }

  /**
   * 
   */
  @Override
  public BigDecimal getSumVout(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getDfiSumVout()");

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
      throw new DfxException("getDfiSumVout", e);
    }
  }
}
