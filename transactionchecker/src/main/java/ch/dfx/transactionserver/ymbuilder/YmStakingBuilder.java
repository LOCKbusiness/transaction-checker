package ch.dfx.transactionserver.ymbuilder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.enumeration.TokenEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class YmStakingBuilder {
  private static final Logger LOGGER = LogManager.getLogger(YmStakingBuilder.class);

  // ...
  private PreparedStatement stakingAmountSelectStatement = null;

  private PreparedStatement stakingSelectStatement = null;
  private PreparedStatement stakingInsertStatement = null;
  private PreparedStatement stakingUpdateStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public YmStakingBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    this.network = network;

    this.databaseBalanceHelper = databaseBalanceHelper;
  }

  /**
   * 
   */
  public void build(
      @Nonnull Connection connection,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.debug("build(): token=" + token);

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      calcStakingBalance(connection, token);

      closeStatements();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      LOGGER.debug("[YmStakingBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      String stakingAmountSelectSql =
          "SELECT"
              + " MAX(ata_out.block_number) AS block_number,"
              + " SUM(ata_in.amount) AS sum"
              + " FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in ata_in ON"
              + " ata_out.block_number = ata_in.block_number"
              + " AND ata_out.transaction_number = ata_in.transaction_number"
              + " AND ata_out.type_number= ata_in.type_number"
              + " AND ata_out.token_number = ata_in.token_number"
              + " WHERE"
              + " ata_out.block_number>?"
              + " AND ata_out.token_number=?"
              + " AND ata_out.address_number=?"
              + " AND ata_in.address_number=?";
      stakingAmountSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingAmountSelectSql));

      String stakingSelectSql =
          "SELECT * FROM " + TOKEN_YIELDMACHINE_SCHEMA + ".staking"
              + " WHERE token_number=?"
              + " AND liquidity_address_number=?"
              + " AND deposit_address_number=?"
              + " AND customer_address_number=?";
      stakingSelectStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingSelectSql));

      String stakingInsertSql =
          "INSERT INTO " + TOKEN_YIELDMACHINE_SCHEMA + ".staking"
              + " (token_number, liquidity_address_number, deposit_address_number, customer_address_number"
              + ", last_in_block_number, vin"
              + ", last_out_block_number, vout)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
      stakingInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, stakingInsertSql));

      String stakingUpdateSql =
          "UPDATE " + TOKEN_YIELDMACHINE_SCHEMA + ".staking"
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
    List<DepositDTO> depositDTOList = databaseBalanceHelper.getDepositDTOList();

    for (DepositDTO depositDTO : depositDTOList) {
      calcBalance(connection, depositDTO, token);
    }
  }

  /**
   * 
   */
  private void calcBalance(
      @Nonnull Connection connection,
      @Nonnull DepositDTO depositDTO,
      @Nonnull TokenEnum token) throws DfxException {
    LOGGER.trace("calcBalance()");

    try {
      int tokenNumber = token.getNumber();
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
      StakingDTO vinStakingDTO = calcInAmount(stakingLastInBlockNumber, tokenNumber, liquidityAddressNumber, depositAddressNumber);
      StakingDTO voutStakingDTO = calcOutAmount(voutStartBlockNumber, stakingLastOutBlockNumber, tokenNumber, liquidityAddressNumber, customerAddressNumber);

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
      if (-1 != maxStakingLastInBlockNumber) {
        if (-1 == stakingLastInBlockNumber
            && -1 == stakingLastOutBlockNumber) {
          insertStaking(stakingDTO);
        } else if (stakingDTO.isInternalStateChanged()) {
          updateStaking(stakingDTO);
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
  private StakingDTO calcInAmount(
      int lastInBlockNumber,
      int tokenNumber,
      int liquidityAddressNumber,
      int depositAddressNumber) throws DfxException {
    LOGGER.trace("calcInAmount()");

    try {
      stakingAmountSelectStatement.setInt(1, lastInBlockNumber);
      stakingAmountSelectStatement.setInt(2, tokenNumber);
      stakingAmountSelectStatement.setInt(3, liquidityAddressNumber);
      stakingAmountSelectStatement.setInt(4, depositAddressNumber);

      StakingDTO stakingDTO = new StakingDTO(tokenNumber, liquidityAddressNumber, depositAddressNumber, -1);

      ResultSet resultSet = stakingAmountSelectStatement.executeQuery();

      if (resultSet.next()) {
        int blockNumber = resultSet.getInt("block_number");

        if (!resultSet.wasNull()) {
          stakingDTO.setLastInBlockNumber(blockNumber);
          stakingDTO.setVin(resultSet.getBigDecimal("sum"));
        }
      }

      resultSet.close();

      return stakingDTO;
    } catch (Exception e) {
      throw new DfxException("calcInAmount", e);
    }
  }

  /**
   * 
   */
  private StakingDTO calcOutAmount(
      int outStartBlockNumber,
      int lastOutBlockNumber,
      int tokenNumber,
      int liquidityAddressNumber,
      int customerAddressNumber) throws DfxException {
    LOGGER.trace("calcOutAmount()");

    try {
      stakingAmountSelectStatement.setInt(1, outStartBlockNumber);
      stakingAmountSelectStatement.setInt(2, tokenNumber);
      stakingAmountSelectStatement.setInt(3, customerAddressNumber);
      stakingAmountSelectStatement.setInt(4, liquidityAddressNumber);

      StakingDTO stakingDTO = new StakingDTO(tokenNumber, liquidityAddressNumber, -1, customerAddressNumber);

      ResultSet resultSet = stakingAmountSelectStatement.executeQuery();

      if (resultSet.next()) {
        int blockNumber = resultSet.getInt("block_number");

        if (!resultSet.wasNull()) {
          stakingDTO.setLastOutBlockNumber(blockNumber);
          stakingDTO.setVout(resultSet.getBigDecimal("sum"));
        }
      } else {
        stakingDTO.setVout(BigDecimal.ZERO);
      }

      resultSet.close();

      return stakingDTO;
    } catch (Exception e) {
      throw new DfxException("calcOutAmount", e);
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
