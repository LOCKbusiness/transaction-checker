package ch.dfx.transactionserver.builder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;

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
import ch.dfx.transactionserver.data.AddressTransactionInDTO;
import ch.dfx.transactionserver.data.AddressTransactionOutDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class DepositBuilder {
  private static final Logger LOGGER = LogManager.getLogger(DepositBuilder.class);

  private PreparedStatement outTransactionByBlockAndAddressSelectStatement = null;
  private PreparedStatement inTransactionByBlockAndTransactionSelectStatement = null;

  private PreparedStatement depositInsertStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public DepositBuilder(
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
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      // ...
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
          LOGGER.debug("[DepositBuilder] Liquidity Address: " + stakingAddressDTO.getLiquidityAddress());
          List<DepositDTO> depositDTOList = getDepositDTOList(stakingAddressDTO, depositAddressNumberSet);

          for (DepositDTO depositDTO : depositDTOList) {
            fillCustomerAddress(depositDTO);
          }

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

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      LOGGER.debug("[DepositBuilder] runtime: " + (System.currentTimeMillis() - startTime));
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements()");

    try {
      // Transaction ...
      String outTransactionByBlockAndAddressSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_out WHERE block_number>=? AND address_number=?";
      outTransactionByBlockAndAddressSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, outTransactionByBlockAndAddressSelectSql));

      String inTransactionByBlockAndTransactionSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in WHERE block_number=? AND transaction_number=?";
      inTransactionByBlockAndTransactionSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, inTransactionByBlockAndTransactionSelectSql));

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
      outTransactionByBlockAndAddressSelectStatement.close();
      inTransactionByBlockAndTransactionSelectStatement.close();

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
    Set<String> unifierTransactionSet = new HashSet<>();
    Set<Integer> unifierAddressSet = new HashSet<>();

    int liquidityStartBlockNumber = stakingAddressDTO.getStartBlockNumber();
    int liquidityAddressNumber = stakingAddressDTO.getLiquidityAddressNumber();

    List<AddressTransactionOutDTO> transactionOutDTOList = getTransactionOutDTOList(liquidityStartBlockNumber, liquidityAddressNumber);

    for (AddressTransactionOutDTO transactionOutDTO : transactionOutDTOList) {
      int depositBlockNumber = transactionOutDTO.getBlockNumber();
      int depositTransactionNumber = transactionOutDTO.getTransactionNumber();

      String transactionKey = depositBlockNumber + "/" + depositTransactionNumber;

      if (unifierTransactionSet.add(transactionKey)) {
        List<AddressTransactionInDTO> depositTransactionInDTOList = getTransactionInDTOList(depositBlockNumber, depositTransactionNumber);

        for (AddressTransactionInDTO depositTransactionInDTO : depositTransactionInDTOList) {
          int addressNumber = depositTransactionInDTO.getAddressNumber();

          if (liquidityAddressNumber != addressNumber
              && !depositAddressNumberSet.contains(addressNumber)) {
            if (unifierAddressSet.add(addressNumber)) {
              DepositDTO depositDTO = new DepositDTO(stakingAddressDTO.getTokenNumber());

              depositDTO.setLiquidityAddressNumber(liquidityAddressNumber);
              depositDTO.setDepositAddressNumber(addressNumber);
              depositDTO.setStartBlockNumber(depositTransactionInDTO.getBlockNumber());
              depositDTO.setStartTransactionNumber(depositTransactionInDTO.getTransactionNumber());

              depositDTOList.add(depositDTO);
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
  private void fillCustomerAddress(@Nonnull DepositDTO depositDTO) throws DfxException {
    LOGGER.trace("fillCustomerAddress()");

    int depositStartBlockNumber = depositDTO.getStartBlockNumber();
    int depositStartTransactionNumber = depositDTO.getStartTransactionNumber();

    List<AddressTransactionInDTO> depositTransactionInDTOList = getTransactionInDTOList(depositStartBlockNumber, depositStartTransactionNumber);

    if (depositTransactionInDTOList.isEmpty()) {
      throw new DfxException("Deposit In-Transaction not found");
    }

    AddressTransactionInDTO depositTransactionInDTO = depositTransactionInDTOList.get(0);

    int customerBlockNumber = depositTransactionInDTO.getInBlockNumber();
    int customerTransactionNumber = depositTransactionInDTO.getInTransactionNumber();

    List<AddressTransactionInDTO> customerTransactionInDTOList = getTransactionInDTOList(customerBlockNumber, customerTransactionNumber);

    if (!customerTransactionInDTOList.isEmpty()) {
      AddressTransactionInDTO customerTransactionInDTO = customerTransactionInDTOList.get(0);
      depositDTO.setCustomerAddressNumber(customerTransactionInDTO.getAddressNumber());
    }
  }

  /**
   * 
   */
  private List<AddressTransactionOutDTO> getTransactionOutDTOList(
      int startBlockNumber,
      int addressNumber) throws DfxException {
    LOGGER.trace("getTransactionOutDTOList()");

    try {
      List<AddressTransactionOutDTO> transactionOutDTOList = new ArrayList<>();

      outTransactionByBlockAndAddressSelectStatement.setInt(1, startBlockNumber);
      outTransactionByBlockAndAddressSelectStatement.setInt(2, addressNumber);

      ResultSet resultSet = outTransactionByBlockAndAddressSelectStatement.executeQuery();

      while (resultSet.next()) {
        AddressTransactionOutDTO transactionOutDTO =
            new AddressTransactionOutDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("vout_number"),
                resultSet.getInt("address_number"));
        transactionOutDTO.setVout(resultSet.getBigDecimal("vout"));
        transactionOutDTO.setType(resultSet.getString("type"));

        transactionOutDTOList.add(transactionOutDTO);
      }

      resultSet.close();

      // ...
      if (transactionOutDTOList.isEmpty()) {
        throw new DfxException("cannot determine output transactions for address " + addressNumber);
      }

      // Comparator to order by block_number, transaction_number, vout_number ...
      Comparator<AddressTransactionOutDTO> comparator =
          Comparator.comparing(AddressTransactionOutDTO::getBlockNumber)
              .thenComparing(AddressTransactionOutDTO::getTransactionNumber)
              .thenComparing(AddressTransactionOutDTO::getVoutNumber);
      transactionOutDTOList.sort(comparator);

      return transactionOutDTOList;
    } catch (Exception e) {
      throw new DfxException("getTransactionOutDTOList", e);
    }
  }

  /**
   * 
   */
  private List<AddressTransactionInDTO> getTransactionInDTOList(
      int blockNumber,
      int transactionNumber) throws DfxException {
    LOGGER.trace("getTransactionInDTOList()");

    try {
      List<AddressTransactionInDTO> transactionInDTOList = new ArrayList<>();

      inTransactionByBlockAndTransactionSelectStatement.setInt(1, blockNumber);
      inTransactionByBlockAndTransactionSelectStatement.setInt(2, transactionNumber);

      ResultSet resultSet = inTransactionByBlockAndTransactionSelectStatement.executeQuery();

      while (resultSet.next()) {
        AddressTransactionInDTO transactionInDTO =
            new AddressTransactionInDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("vin_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("in_block_number"),
                resultSet.getInt("in_transaction_number"));
        transactionInDTO.setVin(resultSet.getBigDecimal("vin"));

        transactionInDTOList.add(transactionInDTO);
      }

      resultSet.close();

      // ...
      if (!transactionInDTOList.isEmpty()) {
        Comparator<AddressTransactionInDTO> comparator =
            Comparator.comparing(AddressTransactionInDTO::getVinNumber);
        transactionInDTOList.sort(comparator);
      }

      return transactionInDTOList;
    } catch (Exception e) {
      throw new DfxException("getTransactionInDTOList", e);
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
}
