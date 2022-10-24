package ch.dfx.transactionserver.builder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class StakingBuilder {
  private static final Logger LOGGER = LogManager.getLogger(StakingBuilder.class);

  private PreparedStatement stakingSelectStatement = null;
  private PreparedStatement stakingInsertStatement = null;
  private PreparedStatement stakingUpdateStatement = null;

  private PreparedStatement stakingVinVoutSelectStatement = null;

  private final DatabaseHelper databaseHelper;

  /**
   * 
   */
  public StakingBuilder() {
    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  public List<StakingDTO> build() throws DfxException {
    LOGGER.trace("build() ...");

    Connection connection = null;

    try {
      connection = H2DBManager.getInstance().openConnection();

      databaseHelper.openStatements(connection);
      openStatements(connection);

      List<StakingDTO> stakingDTOList = calcStakingBalance(connection);

      closeStatements();
      databaseHelper.closeStatements();

      // DatabaseUtils.rollback(connection);
      connection.commit();

      return stakingDTOList;
    } catch (DfxException e) {
      DatabaseUtils.rollback(connection);
      throw e;
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      H2DBManager.getInstance().closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      String stakingSelectSql =
          "SELECT * FROM public.staking"
              + " WHERE liquidity_address_number=?"
              + " AND deposit_address_number=?"
              + " AND customer_address_number=?";
      stakingSelectStatement = connection.prepareStatement(stakingSelectSql);

      String stakingInsertSql =
          "INSERT INTO public.staking"
              + " (liquidity_address_number, deposit_address_number, customer_address_number"
              + ", last_in_block_number, vin"
              + ", last_out_block_number, vout)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?)";
      stakingInsertStatement = connection.prepareStatement(stakingInsertSql);

      String stakingUpdateSql =
          "UPDATE public.staking"
              + " SET last_in_block_number=?, vin=?"
              + " , last_out_block_number=?, vout=?"
              + " WHERE liquidity_address_number=?"
              + " AND deposit_address_number=?"
              + " AND customer_address_number=?";
      stakingUpdateStatement = connection.prepareStatement(stakingUpdateSql);

      // ...
      String stakingVinVoutSelectSql =
          "WITH AT_OUT_1 AS ("
              + " SELECT at_out.* FROM ADDRESS_TRANSACTION_OUT at_out"
              + " WHERE at_out.block_number > ? AND at_out.address_number = ?"
              + "), AT_IN_1 AS ("
              + " SELECT * FROM ADDRESS_TRANSACTION_IN at_in"
              + " WHERE at_in.block_number > ? AND at_in.address_number = ?"
              + "), AT_OUT_2 AS ("
              + " SELECT at_out.* FROM ADDRESS_TRANSACTION_OUT at_out"
              + " WHERE at_out.block_number > ? AND at_out.address_number = ?"
              + ")"
              + "SELECT"
              + " at_out_1.vout AS vout_1,"
              + " at_out_2.block_number,"
              + " at_out_2.vout AS vout_2"
              + " FROM"
              + " AT_OUT_1 at_out_1"
              + " JOIN AT_IN_1 at_in_1 ON"
              + " at_out_1.block_number = at_in_1.in_block_number"
              + " AND at_out_1.transaction_number = at_in_1.in_transaction_number"
              + " AND at_out_1.address_number = at_in_1.address_number"
              + " JOIN AT_OUT_2 at_out_2 ON"
              + " at_in_1.block_number = at_out_2.block_number"
              + " AND at_in_1.transaction_number = at_out_2.transaction_number";
      stakingVinVoutSelectStatement = connection.prepareStatement(stakingVinVoutSelectSql);
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements() ...");

    try {
      stakingSelectStatement.close();
      stakingInsertStatement.close();
      stakingUpdateStatement.close();

      stakingVinVoutSelectStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private List<StakingDTO> calcStakingBalance(@Nonnull Connection connection) throws DfxException {
    LOGGER.debug("calcStakingBalance() ...");

    List<StakingDTO> stakingDTOList = new ArrayList<>();

    List<DepositDTO> depositDTOList = databaseHelper.getDepositDTOList();

    for (DepositDTO depositDTO : depositDTOList) {
      StakingDTO stakingDTO =
          calcBalance(connection, depositDTO);
      stakingDTO.setDepositDTO(depositDTO);

      stakingDTOList.add(stakingDTO);
    }

    return stakingDTOList;
  }

  /**
   * 
   */
  private StakingDTO calcBalance(
      @Nonnull Connection connection,
      @Nonnull DepositDTO depositDTO) throws DfxException {
    LOGGER.trace("calcBalance() ...");

    try {
      int liquidityAddressNumber = depositDTO.getLiquidityAddressNumber();
      int depositAddressNumber = depositDTO.getDepositAddressNumber();
      int customerAddressNumber = depositDTO.getCustomerAddressNumber();

      // ...
      StakingDTO stakingDTO = getStakingDTO(liquidityAddressNumber, depositAddressNumber, customerAddressNumber);

      // ...
      int stakingLastInBlockNumber = stakingDTO.getLastInBlockNumber();
      int stakingLastOutBlockNumber = stakingDTO.getLastOutBlockNumber();

      // ...
      StakingDTO vinStakingDTO = calcVin(stakingLastInBlockNumber, liquidityAddressNumber, depositAddressNumber);
      StakingDTO voutStakingDTO = calcVout(stakingLastOutBlockNumber, liquidityAddressNumber, customerAddressNumber);

      // ...
      int maxStakingLastInBlockNumber = stakingLastInBlockNumber;
      int maxStakingLastOutBlockNumber = stakingLastOutBlockNumber;

      maxStakingLastInBlockNumber = Math.max(maxStakingLastInBlockNumber, vinStakingDTO.getLastInBlockNumber());
      maxStakingLastOutBlockNumber = Math.max(maxStakingLastOutBlockNumber, voutStakingDTO.getLastOutBlockNumber());

      // ...
      stakingDTO.setLastInBlockNumber(maxStakingLastInBlockNumber);
      stakingDTO.addVin(vinStakingDTO.getVin());
      stakingDTO.setLastOutBlockNumber(maxStakingLastOutBlockNumber);
      stakingDTO.addVout(voutStakingDTO.getVout());

      // ...
      if (-1 == stakingLastInBlockNumber
          && -1 == stakingLastOutBlockNumber) {
        insertStaking(stakingDTO);
      } else if (maxStakingLastInBlockNumber > stakingLastInBlockNumber
          || maxStakingLastOutBlockNumber > stakingLastOutBlockNumber) {
        updateStaking(stakingDTO);
      }

      return stakingDTO;
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
      int liquidityAddressNumber,
      int depositAddressNumber,
      int customerAddressNumber) throws DfxException {
    LOGGER.trace("getStakingDTO() ...");

    try {
      StakingDTO stakingDTO = new StakingDTO(liquidityAddressNumber, depositAddressNumber, customerAddressNumber);

      stakingSelectStatement.setInt(1, liquidityAddressNumber);
      stakingSelectStatement.setInt(2, depositAddressNumber);
      stakingSelectStatement.setInt(3, customerAddressNumber);

      ResultSet resultSet = stakingSelectStatement.executeQuery();

      if (resultSet.next()) {
        stakingDTO.setLastInBlockNumber(DatabaseUtils.getIntOrDefault(resultSet, "last_in_block_number", -1));
        stakingDTO.setVin(DatabaseUtils.getBigDecimalOrDefault(resultSet, "vin", BigDecimal.ZERO));

        stakingDTO.setLastOutBlockNumber(DatabaseUtils.getIntOrDefault(resultSet, "last_out_block_number", -1));
        stakingDTO.setVout(DatabaseUtils.getBigDecimalOrDefault(resultSet, "vout", BigDecimal.ZERO));
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
      int liquidityAddressNumber,
      int depositAddressNumber) throws DfxException {
    LOGGER.trace("calcVin() ...");

    try {
      stakingVinVoutSelectStatement.setInt(1, lastInBlockNumber);
      stakingVinVoutSelectStatement.setInt(2, depositAddressNumber);

      stakingVinVoutSelectStatement.setInt(3, lastInBlockNumber);
      stakingVinVoutSelectStatement.setInt(4, depositAddressNumber);

      stakingVinVoutSelectStatement.setInt(5, lastInBlockNumber);
      stakingVinVoutSelectStatement.setInt(6, liquidityAddressNumber);

      StakingDTO stakingDTO = new StakingDTO(liquidityAddressNumber, depositAddressNumber, -1);

      ResultSet resultSet = stakingVinVoutSelectStatement.executeQuery();

      while (resultSet.next()) {
        stakingDTO.addVin(resultSet.getBigDecimal("vout_1"));
        lastInBlockNumber = Math.max(lastInBlockNumber, resultSet.getInt(2));
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
      int lastOutBlockNumber,
      int liquidityAddressNumber,
      int customerAddressNumber) throws DfxException {
    LOGGER.trace("calcVout() ...");

    try {
      stakingVinVoutSelectStatement.setInt(1, lastOutBlockNumber);
      stakingVinVoutSelectStatement.setInt(2, liquidityAddressNumber);

      stakingVinVoutSelectStatement.setInt(3, lastOutBlockNumber);
      stakingVinVoutSelectStatement.setInt(4, liquidityAddressNumber);

      stakingVinVoutSelectStatement.setInt(5, lastOutBlockNumber);
      stakingVinVoutSelectStatement.setInt(6, customerAddressNumber);

      StakingDTO stakingDTO = new StakingDTO(liquidityAddressNumber, -1, customerAddressNumber);

      ResultSet resultSet = stakingVinVoutSelectStatement.executeQuery();

      while (resultSet.next()) {
        stakingDTO.addVout(resultSet.getBigDecimal("vout_2"));
        lastOutBlockNumber = Math.max(lastOutBlockNumber, resultSet.getInt(2));
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
    LOGGER.trace("insertStaking() ...");

    try {
      int liquidityAddressNumber = stakingDTO.getLiquidityAddressNumber();
      int depositAddressNumber = stakingDTO.getDepositAddressNumber();
      int customerAddressNumber = stakingDTO.getCustomerAddressNumber();
      int lastInBlockNumber = stakingDTO.getLastInBlockNumber();
      int lastOutBlockNumber = stakingDTO.getLastOutBlockNumber();

      LOGGER.debug(
          "[INSERT] Liquidity / Deposit / Customer Address: "
              + liquidityAddressNumber + " / " + depositAddressNumber + " / " + customerAddressNumber);
      LOGGER.debug(
          "[INSERT] Last In / Last Out Block: "
              + lastInBlockNumber + " / " + lastOutBlockNumber);

      stakingInsertStatement.setInt(1, liquidityAddressNumber);
      stakingInsertStatement.setInt(2, depositAddressNumber);
      stakingInsertStatement.setInt(3, customerAddressNumber);
      stakingInsertStatement.setInt(4, lastInBlockNumber);
      stakingInsertStatement.setBigDecimal(5, stakingDTO.getVin());
      stakingInsertStatement.setInt(6, lastOutBlockNumber);
      stakingInsertStatement.setBigDecimal(7, stakingDTO.getVout());
      stakingInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("insertStaking", e);
    }
  }

  /**
   * 
   */
  private void updateStaking(@Nonnull StakingDTO stakingDTO) throws DfxException {
    LOGGER.trace("updateStaking() ...");

    try {
      int liquidityAddressNumber = stakingDTO.getLiquidityAddressNumber();
      int depositAddressNumber = stakingDTO.getDepositAddressNumber();
      int customerAddressNumber = stakingDTO.getCustomerAddressNumber();
      int lastInBlockNumber = stakingDTO.getLastInBlockNumber();
      int lastOutBlockNumber = stakingDTO.getLastOutBlockNumber();

      LOGGER.debug(
          "[UPDATE] Liquidity / Deposit / Customer Address: "
              + liquidityAddressNumber + " / " + depositAddressNumber + " / " + customerAddressNumber);
      LOGGER.debug(
          "[UPDATE] Last In / Last Out Block: "
              + lastInBlockNumber + " / " + lastOutBlockNumber);

      stakingUpdateStatement.setInt(1, lastInBlockNumber);
      stakingUpdateStatement.setBigDecimal(2, stakingDTO.getVin());
      stakingUpdateStatement.setInt(3, lastOutBlockNumber);
      stakingUpdateStatement.setBigDecimal(4, stakingDTO.getVout());
      stakingUpdateStatement.setInt(5, liquidityAddressNumber);
      stakingUpdateStatement.setInt(6, depositAddressNumber);
      stakingUpdateStatement.setInt(7, customerAddressNumber);

      stakingUpdateStatement.execute();
    } catch (Exception e) {
      throw new DfxException("updateStaking", e);
    }
  }

}
