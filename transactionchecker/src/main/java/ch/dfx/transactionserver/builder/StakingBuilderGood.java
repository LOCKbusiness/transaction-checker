package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class StakingBuilderGood {
  private static final Logger LOGGER = LogManager.getLogger(StakingBuilderGood.class);

  // ...
  private PreparedStatement stakingVinSelectStatement = null;
  private PreparedStatement stakingVoutSelectStatement = null;

  private PreparedStatement stakingSelectStatement = null;
  private PreparedStatement stakingInsertStatement = null;
  private PreparedStatement stakingUpdateStatement = null;

  // ...
  private final NetworkEnum network;

  private final H2DBManager databaseManager;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public StakingBuilderGood(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.network = network;
    this.databaseManager = databaseManager;

    this.databaseBalanceHelper = new DatabaseBalanceHelper(network);
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

      databaseBalanceHelper.openStatements(connection);
      openStatements(connection);

      calcStakingBalance(connection, token);

      closeStatements();
      databaseBalanceHelper.closeStatements();

      connection.commit();
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("[StakingBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String stakingVinSelectSql =
          "SELECT"
              + " at_in.in_block_number,"
              + " at_in.in_transaction_number,"
              + " at_in.vin"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " at_out.address_number=?"
              + " AND at_in.address_number=?";
      stakingVinSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingVinSelectSql));

      // ...
      String stakingVoutSelectSql =
          "SELECT"
              + " at_out.block_number,"
              + " at_out.transaction_number,"
              + " at_out.vout"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out ON"
              + " at_in.block_number = at_out.block_number"
              + " AND at_in.transaction_number = at_out.transaction_number"
              + " WHERE"
              + " at_in.block_number>?"
              + " AND at_in.address_number=?"
              + " AND at_out.address_number=?";
      stakingVoutSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingVoutSelectSql));

      String stakingSelectSql =
          "SELECT * FROM " + TOKEN_NETWORK_SCHEMA + ".staking"
              + " WHERE token_number=?"
              + " AND liquidity_address_number=?"
              + " AND deposit_address_number=?"
              + " AND customer_address_number=?";
      stakingSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingSelectSql));

      String stakingInsertSql =
          "INSERT INTO " + TOKEN_NETWORK_SCHEMA + ".staking"
              + " (token_number, liquidity_address_number, deposit_address_number, customer_address_number"
              + ", last_in_block_number, vin"
              + ", last_out_block_number, vout)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
      stakingInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingInsertSql));

      String stakingUpdateSql =
          "UPDATE " + TOKEN_NETWORK_SCHEMA + ".staking"
              + " SET last_in_block_number=?, vin=?"
              + " , last_out_block_number=?, vout=?"
              + " WHERE token_number=?"
              + " AND liquidity_address_number=?"
              + " AND deposit_address_number=?"
              + " AND customer_address_number=?";
      stakingUpdateStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingUpdateSql));

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
      stakingVinSelectStatement.close();
      stakingVoutSelectStatement.close();

      stakingSelectStatement.close();
      stakingInsertStatement.close();
      stakingUpdateStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private void calcStakingBalance(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("calcStakingBalance()");

    // ...
    List<DepositDTO> depositDTOList = databaseBalanceHelper.getDepositDTOList(token);

    for (DepositDTO depositDTO : depositDTOList) {
      calcBalance(connection, depositDTO);
    }
  }

  /**
   * 
   */
  private void calcBalance(
      @Nonnull Connection connection,
      @Nonnull DepositDTO depositDTO) throws DfxException {
    LOGGER.trace("calcBalance()");

    try {
      int tokenNumber = depositDTO.getTokenNumber();
      int liquidityAddressNumber = depositDTO.getLiquidityAddressNumber();
      int depositAddressNumber = depositDTO.getDepositAddressNumber();
      int customerAddressNumber = depositDTO.getCustomerAddressNumber();

      // ...
      StakingDTO stakingDTO = getStakingDTO(tokenNumber, liquidityAddressNumber, depositAddressNumber, customerAddressNumber);

      // ...
      int stakingLastInBlockNumber = stakingDTO.getLastInBlockNumber();
      int stakingLastOutBlockNumber = stakingDTO.getLastOutBlockNumber();

      // ...
      int voutStartBlockNumber = (-1 == stakingLastOutBlockNumber ? stakingLastInBlockNumber - 1 : stakingLastOutBlockNumber);

      // ...
      StakingDTO vinStakingDTO = calcVin(stakingLastInBlockNumber, tokenNumber, liquidityAddressNumber, depositAddressNumber);
      StakingDTO voutStakingDTO = calcVout(voutStartBlockNumber, stakingLastOutBlockNumber, tokenNumber, liquidityAddressNumber, customerAddressNumber);

      // ...
      int maxStakingLastInBlockNumber = stakingLastInBlockNumber;
      int maxStakingLastOutBlockNumber = stakingLastOutBlockNumber;

      maxStakingLastInBlockNumber = Math.max(maxStakingLastInBlockNumber, vinStakingDTO.getLastInBlockNumber());
      maxStakingLastOutBlockNumber = Math.max(maxStakingLastOutBlockNumber, voutStakingDTO.getLastOutBlockNumber());

      // ...
      stakingDTO.setLastInBlockNumber(maxStakingLastInBlockNumber);
      stakingDTO.setVin(vinStakingDTO.getVin());

      stakingDTO.setLastOutBlockNumber(maxStakingLastOutBlockNumber);
      stakingDTO.addVout(voutStakingDTO.getVout());

      // ...
      if (-1 == stakingLastInBlockNumber
          && -1 == stakingLastOutBlockNumber) {
        insertStaking(stakingDTO);
      } else if (stakingDTO.isInternalStateChanged()) {
        updateStaking(stakingDTO);
      }
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("calcBalance", e);
    }
  }

  /**
   * 
   */
  private StakingDTO getStakingDTO(
      int tokenNumber,
      int liquidityAddressNumber,
      int depositAddressNumber,
      int customerAddressNumber) throws DfxException {
    LOGGER.trace("getStakingDTO()");

    try {
      StakingDTO stakingDTO = new StakingDTO(tokenNumber, liquidityAddressNumber, depositAddressNumber, customerAddressNumber);

      stakingSelectStatement.setInt(1, tokenNumber);
      stakingSelectStatement.setInt(2, liquidityAddressNumber);
      stakingSelectStatement.setInt(3, depositAddressNumber);
      stakingSelectStatement.setInt(4, customerAddressNumber);

      ResultSet resultSet = stakingSelectStatement.executeQuery();

      if (resultSet.next()) {
        stakingDTO.setLastInBlockNumber(resultSet.getInt("last_in_block_number"));
        stakingDTO.setLastOutBlockNumber(resultSet.getInt("last_out_block_number"));

        // ...
        BigDecimal vin = resultSet.getBigDecimal("vin");

        if (0 == BigDecimal.ZERO.compareTo(vin)) {
          vin = BigDecimal.ZERO;
        }

        stakingDTO.setVin(vin);

        // ...
        BigDecimal vout = resultSet.getBigDecimal("vout");

        if (0 == BigDecimal.ZERO.compareTo(vout)) {
          vout = BigDecimal.ZERO;
        }

        stakingDTO.setVout(vout);

        stakingDTO.keepInternalState();
      }

      resultSet.close();

      return stakingDTO;
    } catch (Exception e) {
      throw new DfxException("getStakingDTO", e);
    }
  }

  /**
   * 
   */
  private StakingDTO calcVin(
      int lastInBlockNumber,
      int tokenNumber,
      int liquidityAddressNumber,
      int depositAddressNumber) throws DfxException {
    LOGGER.trace("calcVin()");

    try {
      stakingVinSelectStatement.setInt(1, liquidityAddressNumber);
      stakingVinSelectStatement.setInt(2, depositAddressNumber);

      StakingDTO stakingDTO = new StakingDTO(tokenNumber, liquidityAddressNumber, depositAddressNumber, -1);

      Set<String> unifierSet = new HashSet<>();

      ResultSet resultSet = stakingVinSelectStatement.executeQuery();

      while (resultSet.next()) {
        int inBlockNumber = resultSet.getInt("in_block_number");
        int inTransactionNumber = resultSet.getInt("in_transaction_number");

        String unifier = Integer.toString(inBlockNumber) + "/" + Integer.toString(inTransactionNumber);

        if (unifierSet.add(unifier)) {
          stakingDTO.addVin(resultSet.getBigDecimal("vin"));
          lastInBlockNumber = Math.max(lastInBlockNumber, inBlockNumber);
        }
      }

      stakingDTO.setLastInBlockNumber(lastInBlockNumber);

      resultSet.close();

      return stakingDTO;
    } catch (Exception e) {
      throw new DfxException("calcVin", e);
    }
  }

  /**
   * 
   */
  private StakingDTO calcVout(
      int voutStartBlockNumber,
      int lastOutBlockNumber,
      int tokenNumber,
      int liquidityAddressNumber,
      int customerAddressNumber) throws DfxException {
    LOGGER.trace("calcVout()");

    try {
      stakingVoutSelectStatement.setInt(1, voutStartBlockNumber);
      stakingVoutSelectStatement.setInt(2, liquidityAddressNumber);
      stakingVoutSelectStatement.setInt(3, customerAddressNumber);

      StakingDTO stakingDTO = new StakingDTO(tokenNumber, liquidityAddressNumber, -1, customerAddressNumber);

      Set<String> unifierSet = new HashSet<>();

      ResultSet resultSet = stakingVoutSelectStatement.executeQuery();

      while (resultSet.next()) {
        int blockNumber = resultSet.getInt("block_number");
        int transactionNumber = resultSet.getInt("transaction_number");

        String unifier = Integer.toString(blockNumber) + "/" + Integer.toString(transactionNumber);

        if (unifierSet.add(unifier)) {
          stakingDTO.addVout(resultSet.getBigDecimal("vout"));
          lastOutBlockNumber = Math.max(lastOutBlockNumber, blockNumber);
        }
      }

      stakingDTO.setLastOutBlockNumber(lastOutBlockNumber);

      resultSet.close();

      return stakingDTO;
    } catch (Exception e) {
      throw new DfxException("calcVout", e);
    }
  }

  /**
   * 
   */
  private void insertStaking(@Nonnull StakingDTO stakingDTO) throws DfxException {
    LOGGER.trace("insertStaking()");

    try {
      int tokenNumber = stakingDTO.getTokenNumber();
      int liquidityAddressNumber = stakingDTO.getLiquidityAddressNumber();
      int depositAddressNumber = stakingDTO.getDepositAddressNumber();
      int customerAddressNumber = stakingDTO.getCustomerAddressNumber();
      int lastInBlockNumber = stakingDTO.getLastInBlockNumber();
      int lastOutBlockNumber = stakingDTO.getLastOutBlockNumber();

      LOGGER.debug(
          "[INSERT] Token / Liquidity / Deposit / Customer: "
              + tokenNumber + " / " + liquidityAddressNumber + " / " + depositAddressNumber + " / " + customerAddressNumber);
      LOGGER.debug(
          "[INSERT] Last In / Last Out Block: "
              + lastInBlockNumber + " / " + lastOutBlockNumber);

      stakingInsertStatement.setInt(1, tokenNumber);
      stakingInsertStatement.setInt(2, liquidityAddressNumber);
      stakingInsertStatement.setInt(3, depositAddressNumber);
      stakingInsertStatement.setInt(4, customerAddressNumber);
      stakingInsertStatement.setInt(5, lastInBlockNumber);
      stakingInsertStatement.setBigDecimal(6, stakingDTO.getVin());
      stakingInsertStatement.setInt(7, lastOutBlockNumber);
      stakingInsertStatement.setBigDecimal(8, stakingDTO.getVout());
      stakingInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("insertStaking", e);
    }
  }

  /**
   * 
   */
  private void updateStaking(@Nonnull StakingDTO stakingDTO) throws DfxException {
    LOGGER.trace("updateStaking()");

    try {
      int tokenNumber = stakingDTO.getTokenNumber();
      int liquidityAddressNumber = stakingDTO.getLiquidityAddressNumber();
      int depositAddressNumber = stakingDTO.getDepositAddressNumber();
      int customerAddressNumber = stakingDTO.getCustomerAddressNumber();
      int lastInBlockNumber = stakingDTO.getLastInBlockNumber();
      int lastOutBlockNumber = stakingDTO.getLastOutBlockNumber();

      LOGGER.debug(
          "[UPDATE] Token / Liquidity / Deposit / Customer: "
              + tokenNumber + " / " + liquidityAddressNumber + " / " + depositAddressNumber + " / " + customerAddressNumber);
      LOGGER.debug(
          "[UPDATE] Last In / Last Out Block: "
              + lastInBlockNumber + " / " + lastOutBlockNumber);

      stakingUpdateStatement.setInt(1, lastInBlockNumber);
      stakingUpdateStatement.setBigDecimal(2, stakingDTO.getVin());
      stakingUpdateStatement.setInt(3, lastOutBlockNumber);
      stakingUpdateStatement.setBigDecimal(4, stakingDTO.getVout());

      stakingUpdateStatement.setInt(5, tokenNumber);
      stakingUpdateStatement.setInt(6, liquidityAddressNumber);
      stakingUpdateStatement.setInt(7, depositAddressNumber);
      stakingUpdateStatement.setInt(8, customerAddressNumber);

      stakingUpdateStatement.execute();
    } catch (Exception e) {
      throw new DfxException("updateStaking", e);
    }
  }
}
