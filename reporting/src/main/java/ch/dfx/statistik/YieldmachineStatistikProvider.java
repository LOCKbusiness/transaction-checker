package ch.dfx.statistik;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.io.File;
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
public class YieldmachineStatistikProvider extends StatistikProvider {
  private static final Logger LOGGER = LogManager.getLogger(YieldmachineStatistikProvider.class);

  // ...
  private PreparedStatement depositCountSelectStatement = null;

  private PreparedStatement depositSumVinSelectStatement = null;
  private PreparedStatement depositSumVoutSelectStatement = null;

  /**
   * 
   */
  public YieldmachineStatistikProvider(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    super(network, databaseBlockHelper, databaseBalanceHelper);
  }

  /**
   * 
   */
  @Override
  public File getDataPath() {
    return new File("data", "yieldmachine");
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
              + " FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".deposit d"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".block b ON"
              + " d.start_block_number = b.number"
              + " WHERE"
              + " d.token_number=?"
              + " AND b.timestamp BETWEEN ? AND ?";
      depositCountSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositCountSelectSql));

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
              + " FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".deposit"
              + " WHERE token_number=?)";
      depositSumVinSelectStatement =
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
              + " FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".deposit"
              + " WHERE token_number=?)"
              + " AND ata_in.address_number=?";
      depositSumVoutSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositDusdSumVoutSelectSql));
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
    LOGGER.trace("getSumVin()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositSumVinSelectStatement.setLong(1, timestampOfDayArray[0]);
      depositSumVinSelectStatement.setLong(2, timestampOfDayArray[1]);
      depositSumVinSelectStatement.setInt(3, token.getNumber());
      depositSumVinSelectStatement.setInt(4, liquidityAddressNumber);
      depositSumVinSelectStatement.setInt(5, token.getNumber());

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
  @Override
  public BigDecimal getSumVout(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber,
      @Nonnull LocalDate workDate) throws DfxException {
    LOGGER.trace("getSumVout()");

    try {
      long[] timestampOfDayArray = getTimestampOfDay(workDate);

      depositSumVoutSelectStatement.setLong(1, timestampOfDayArray[0]);
      depositSumVoutSelectStatement.setLong(2, timestampOfDayArray[1]);
      depositSumVoutSelectStatement.setInt(3, token.getNumber());
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
}
