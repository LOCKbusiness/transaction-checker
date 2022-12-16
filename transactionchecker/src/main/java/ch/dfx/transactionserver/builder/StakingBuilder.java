package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class StakingBuilder {
  private static final Logger LOGGER = LogManager.getLogger(StakingBuilder.class);

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
  public StakingBuilder(
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
          "WITH X AS ("
              + "SELECT"
              + " at_in.in_block_number,"
              + " at_in.address_number,"
              + " at_in.vin"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " at_out.address_number=?"
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
              + "SELECT"
              + " MAX(in_block_number) AS in_block_number,"
              + " address_number,"
              + " SUM(vin) AS vin"
              + " FROM X"
              + " GROUP BY"
              + " address_number";
      stakingVinSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingVinSelectSql));

      // ...
      String stakingVoutSelectSql =
          "WITH X AS ("
              + "SELECT"
              + " at_out.block_number,"
              + " at_out.address_number,"
              + " at_out.vout"
              + " FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out at_out"
              + " JOIN " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in at_in ON"
              + " at_out.block_number = at_in.block_number"
              + " AND at_out.transaction_number = at_in.transaction_number"
              + " WHERE"
              + " at_in.address_number=?"
              + " AND at_out.address_number IN"
              + " (SELECT customer_address_number FROM " + TOKEN_NETWORK_SCHEMA + ".deposit"
              + " WHERE token_number=? AND liquidity_address_number=?)"
              + " GROUP BY"
              + " at_out.block_number,"
              + " at_out.transaction_number,"
              + " at_out.address_number,"
              + " at_out.vout"
              + ")"
              + "SELECT"
              + " MAX(block_number) AS block_number,"
              + " address_number,"
              + " SUM(vout) AS vout"
              + " FROM X"
              + " GROUP BY"
              + " address_number";
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

    List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList(token);

    for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
      if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
        calcBalance(connection, token, stakingAddressDTO);
      }
    }
  }

  /**
   * 
   */
  private void calcBalance(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token,
      @Nonnull StakingAddressDTO stakingAddressDTO) throws DfxException {
    LOGGER.trace("calcBalance()");

    try {
      int liquidityAddressNumber = stakingAddressDTO.getLiquidityAddressNumber();

      // ...
      List<StakingDTO> storedStakingDTOList =
          databaseBalanceHelper.getStakingDTOListByLiquidityAdressNumber(token, liquidityAddressNumber);
      List<StakingDTO> newStakingDTOList = new ArrayList<>();

      Map<Integer, StakingDTO> storedDepositToStakingDTOMap = new HashMap<>();
      storedStakingDTOList.forEach(dto -> storedDepositToStakingDTOMap.put(dto.getDepositAddressNumber(), dto));

      // ...

      List<StakingDTO> vinStakingDTOList = calcVin(token, liquidityAddressNumber);
      List<StakingDTO> voutStakingDTOList = calcVout(token, liquidityAddressNumber);

      // ...
      Map<Integer, StakingDTO> depositToStakingDTOMap = new HashMap<>();
      vinStakingDTOList.forEach(dto -> depositToStakingDTOMap.put(dto.getDepositAddressNumber(), dto));

      Map<Integer, StakingDTO> customerToStakingDTOMap = new HashMap<>();
      voutStakingDTOList.forEach(dto -> customerToStakingDTOMap.put(dto.getCustomerAddressNumber(), dto));

      // ...
      List<DepositDTO> depositDTOList =
          databaseBalanceHelper.getDepositDTOListByLiquidityAddressNumber(token, liquidityAddressNumber);

      for (DepositDTO depositDTO : depositDTOList) {
        int depositAddressNumber = depositDTO.getDepositAddressNumber();
        int customerAddressNumber = depositDTO.getCustomerAddressNumber();

        StakingDTO storedDepositStakingDTO = storedDepositToStakingDTOMap.get(depositAddressNumber);

        if (null == storedDepositStakingDTO) {
          storedDepositStakingDTO =
              new StakingDTO(
                  token.getNumber(),
                  liquidityAddressNumber,
                  depositAddressNumber,
                  customerAddressNumber);

          newStakingDTOList.add(storedDepositStakingDTO);
        }

        StakingDTO depositStakingDTO = depositToStakingDTOMap.get(depositAddressNumber);
        StakingDTO customerStakingDTO = customerToStakingDTOMap.get(customerAddressNumber);

        if (null != depositStakingDTO) {
          storedDepositStakingDTO.setLastInBlockNumber(depositStakingDTO.getLastInBlockNumber());
          storedDepositStakingDTO.setVin(depositStakingDTO.getVin());
        }

        if (null != customerStakingDTO) {
          storedDepositStakingDTO.setLastOutBlockNumber(customerStakingDTO.getLastOutBlockNumber());
          storedDepositStakingDTO.setVout(customerStakingDTO.getVout());
        }
      }

      // ...
      for (StakingDTO newStakingDTO : newStakingDTOList) {
        insertStaking(newStakingDTO);
      }

      for (StakingDTO storedStakingDTO : storedStakingDTOList) {
        if (storedStakingDTO.isInternalStateChanged()) {
          updateStaking(storedStakingDTO);
        }
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
  private List<StakingDTO> calcVin(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("calcVin()");

    try {
      List<StakingDTO> stakingDTOList = new ArrayList<>();

      stakingVinSelectStatement.setInt(1, liquidityAddressNumber);
      stakingVinSelectStatement.setInt(2, token.getNumber());
      stakingVinSelectStatement.setInt(3, liquidityAddressNumber);

      ResultSet resultSet = stakingVinSelectStatement.executeQuery();

      while (resultSet.next()) {
        StakingDTO stakingDTO =
            new StakingDTO(token.getNumber(), liquidityAddressNumber, resultSet.getInt("address_number"), -1);

        stakingDTO.setLastInBlockNumber(resultSet.getInt("in_block_number"));
        stakingDTO.setVin(resultSet.getBigDecimal("vin"));

        stakingDTOList.add(stakingDTO);
      }

      resultSet.close();

      return stakingDTOList;
    } catch (Exception e) {
      throw new DfxException("calcVin", e);
    }
  }

  /**
   * 
   */
  private List<StakingDTO> calcVout(
      @Nonnull TokenEnum token,
      int liquidityAddressNumber) throws DfxException {
    LOGGER.trace("calcVout()");

    try {
      List<StakingDTO> stakingDTOList = new ArrayList<>();

      stakingVoutSelectStatement.setInt(1, liquidityAddressNumber);
      stakingVoutSelectStatement.setInt(2, token.getNumber());
      stakingVoutSelectStatement.setInt(3, liquidityAddressNumber);

      ResultSet resultSet = stakingVoutSelectStatement.executeQuery();

      while (resultSet.next()) {
        StakingDTO stakingDTO =
            new StakingDTO(token.getNumber(), liquidityAddressNumber, -1, resultSet.getInt("address_number"));

        stakingDTO.setLastOutBlockNumber(resultSet.getInt("block_number"));
        stakingDTO.setVout(resultSet.getBigDecimal("vout"));

        stakingDTOList.add(stakingDTO);
      }

      resultSet.close();

      return stakingDTOList;
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
