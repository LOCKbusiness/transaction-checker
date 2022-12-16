package ch.dfx.transactionserver.ymbuilder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
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
import ch.dfx.transactionserver.data.DatabaseDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class YmDepositBuilder {
  private static final Logger LOGGER = LogManager.getLogger(YmDepositBuilder.class);

  private PreparedStatement customTransactionByAddressSelectStatement = null;

  private PreparedStatement depositInsertStatement = null;

  // ...
  private final NetworkEnum network;

  private final H2DBManager databaseManager;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public YmDepositBuilder(
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

      Set<Integer> depositAddressNumberSet =
          databaseBalanceHelper.getDepositDTOList(token)
              .stream().map(DepositDTO::getDepositAddressNumber).collect(Collectors.toSet());

      // ...
      List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList(token);

      Set<Integer> rewardAddressNumberSet = new HashSet<>();
      stakingAddressDTOList.stream()
          .filter(dto -> -1 != dto.getRewardAddressNumber())
          .forEach(dto -> rewardAddressNumberSet.add(dto.getRewardAddressNumber()));

      for (StakingAddressDTO stakingAddressDTO : stakingAddressDTOList) {
        if (-1 == stakingAddressDTO.getRewardAddressNumber()) {
          LOGGER.debug("[YmDepositBuilder] Liquidity Address: " + stakingAddressDTO.getLiquidityAddress());
          List<DepositDTO> depositDTOList = getDepositDTOList(stakingAddressDTO, depositAddressNumberSet);

          for (DepositDTO depositDTO : depositDTOList) {
            int depositAddressNumber = depositDTO.getDepositAddressNumber();
            int customerAddressNumber = depositDTO.getCustomerAddressNumber();

            if (-1 != depositAddressNumber
                && -1 != customerAddressNumber
                && depositAddressNumber != customerAddressNumber
                && !rewardAddressNumberSet.contains(depositAddressNumber)
                && !rewardAddressNumberSet.contains(customerAddressNumber)) {
              insertDeposit(depositDTO);
            }
          }
        }
      }

      closeStatements();
      databaseBalanceHelper.closeStatements();

      connection.commit();
      DatabaseUtils.rollback(connection);
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      databaseManager.closeConnection(connection);

      LOGGER.debug("[YmDepositBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Custom Transaction ...
      String customTransactionByAddressSelectSql =
          "SELECT"
              + " MIN(ata_out.block_number) AS block_number,"
              + " MIN(ata_out.transaction_number) AS transaction_number,"
              + " ata_out.address_number AS out_address_number,"
              + " ata_in.address_number AS in_address_number"
              + " FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_out ata_out"
              + " JOIN " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in ata_in ON"
              + " ata_out.block_number = ata_in.block_number"
              + " AND ata_out.transaction_number = ata_in.transaction_number"
              + " AND ata_out.type_number = ata_in.type_number"
              + " AND ata_out.address_number != ata_in.address_number"
              + " AND ata_out.token_number = ata_in.token_number"
              + " WHERE"
              + " ata_out.block_number>=?"
              + " AND ata_out.address_number=?"
              + " AND ata_out.token_number=?"
              + " GROUP BY"
              + " ata_out.address_number,"
              + " ata_in.address_number";
      customTransactionByAddressSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, customTransactionByAddressSelectSql));

      // Deposit ...
      String depositInsertSql =
          "INSERT INTO " + TOKEN_NETWORK_SCHEMA + ".deposit"
              + " (token_number, liquidity_address_number, deposit_address_number, customer_address_number, start_block_number, start_transaction_number)"
              + " VALUES (?, ?, ?, ?, ?, ?)";
      depositInsertStatement = connection.prepareStatement(DatabaseUtils.replaceSchema(network, depositInsertSql));
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
      customTransactionByAddressSelectStatement.close();

      depositInsertStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private List<DepositDTO> getDepositDTOList(
      @Nonnull StakingAddressDTO stakingAddressDTO,
      @Nonnull Set<Integer> depositAddressNumberSet) throws DfxException {
    LOGGER.trace("getDepositDTOList()");

    List<DepositDTO> depositDTOList = new ArrayList<>();
    Set<Integer> unifierAddressSet = new HashSet<>();

    int liquidityStartBlockNumber = stakingAddressDTO.getStartBlockNumber();
    int liquidityAddressNumber = stakingAddressDTO.getLiquidityAddressNumber();
    int tokenNumber = stakingAddressDTO.getTokenNumber();

    List<DepositFinderDTO> depositFinderDTOList =
        getDepositFinderDTOList(liquidityStartBlockNumber, liquidityAddressNumber, tokenNumber);

    for (DepositFinderDTO depositFinderDTO : depositFinderDTOList) {
      int depositAddressNumber = depositFinderDTO.getInAddressNumber();

      if (!depositAddressNumberSet.contains(depositAddressNumber)) {
        List<DepositFinderDTO> customerFinderDTOList =
            getDepositFinderDTOList(-1, depositAddressNumber, tokenNumber);

        for (DepositFinderDTO customerFinderDTO : customerFinderDTOList) {
          int customerAddressNumber = customerFinderDTO.getInAddressNumber();

          if (liquidityAddressNumber != customerAddressNumber) {
            if (unifierAddressSet.add(customerAddressNumber)) {
              DepositDTO depositDTO = new DepositDTO(stakingAddressDTO.getTokenNumber());

              depositDTO.setLiquidityAddressNumber(liquidityAddressNumber);
              depositDTO.setDepositAddressNumber(depositAddressNumber);
              depositDTO.setCustomerAddressNumber(customerAddressNumber);
              depositDTO.setStartBlockNumber(depositFinderDTO.getBlockNumber());
              depositDTO.setStartTransactionNumber(depositFinderDTO.getTransactionNumber());

              depositDTOList.add(depositDTO);

              break;
            }
          }
        }
      }
    }

    return depositDTOList;
  }

  /**
   * 
   */
  private List<DepositFinderDTO> getDepositFinderDTOList(
      int startBlockNumber,
      int addressNumber,
      int tokenNumber) throws DfxException {
    LOGGER.trace("getDepositFinderDTOList()");

    try {
      List<DepositFinderDTO> depositFinderDTOList = new ArrayList<>();

      customTransactionByAddressSelectStatement.setInt(1, startBlockNumber);
      customTransactionByAddressSelectStatement.setInt(2, addressNumber);
      customTransactionByAddressSelectStatement.setInt(3, tokenNumber);

      ResultSet resultSet = customTransactionByAddressSelectStatement.executeQuery();

      while (resultSet.next()) {
        DepositFinderDTO depositFinderDTO =
            new DepositFinderDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("out_address_number"),
                resultSet.getInt("in_address_number"));

        depositFinderDTOList.add(depositFinderDTO);
      }

      resultSet.close();

      // ...
      Comparator<DepositFinderDTO> comparator =
          Comparator.comparing(DepositFinderDTO::getBlockNumber)
              .thenComparing(DepositFinderDTO::getTransactionNumber);
      depositFinderDTOList.sort(comparator);

      return depositFinderDTOList;
    } catch (Exception e) {
      throw new DfxException("getDepositFinderDTOList", e);
    }
  }

  /**
   * 
   */
  private void insertDeposit(@Nonnull DepositDTO depositDTO) throws DfxException {
    LOGGER.trace("insertDeposit()");

    try {
      int tokenNumber = depositDTO.getTokenNumber();
      int liquidityAddressNumber = depositDTO.getLiquidityAddressNumber();
      int depositAddressNumber = depositDTO.getDepositAddressNumber();
      int customerAddressNumber = depositDTO.getCustomerAddressNumber();

      LOGGER.debug(
          "[INSERT] Token / Liquidity / Deposit / Customer: "
              + tokenNumber + " / " + liquidityAddressNumber + " / " + depositAddressNumber + " / " + customerAddressNumber);

      depositInsertStatement.setInt(1, tokenNumber);
      depositInsertStatement.setInt(2, liquidityAddressNumber);
      depositInsertStatement.setInt(3, depositAddressNumber);
      depositInsertStatement.setInt(4, customerAddressNumber);
      depositInsertStatement.setInt(5, depositDTO.getStartBlockNumber());
      depositInsertStatement.setInt(6, depositDTO.getStartTransactionNumber());

      depositInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("insertDeposit", e);
    }
  }

  /**
   * 
   */
  private class DepositFinderDTO extends DatabaseDTO {
    private final int blockNumber;
    private final int transactionNumber;
    private final int outAddressNumber;
    private final int inAddressNumber;

    /**
     * 
     */
    private DepositFinderDTO(
        int blockNumber,
        int transactionNumber,
        int outAddressNumber,
        int inAddressNumber) {
      this.blockNumber = blockNumber;
      this.transactionNumber = transactionNumber;
      this.outAddressNumber = outAddressNumber;
      this.inAddressNumber = inAddressNumber;
    }

    private int getBlockNumber() {
      return blockNumber;
    }

    private int getTransactionNumber() {
      return transactionNumber;
    }

    private int getOutAddressNumber() {
      return outAddressNumber;
    }

    private int getInAddressNumber() {
      return inAddressNumber;
    }
  }
}
