package ch.dfx.transactionserver.ymbuilder;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_NETWORK_CUSTOM_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_PUBLIC_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

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
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressTransactionInDTO;
import ch.dfx.transactionserver.data.DatabaseDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.StakingAddressDTO;
import ch.dfx.transactionserver.data.TransactionCustomAccountToAccountInDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;

/**
 * 
 */
public class YmDepositBuilder {
  private static final Logger LOGGER = LogManager.getLogger(YmDepositBuilder.class);

  private PreparedStatement customTransactionByAddressSelectStatement = null;

  private PreparedStatement addressTransactionInByBlockAndTransactionSelectStatement = null;
  private PreparedStatement customAccountToAccountInByBlockAndAddressSelectStatement = null;

  private PreparedStatement depositInsertStatement = null;

  // ...
  private final NetworkEnum network;

  private final DatabaseBalanceHelper databaseBalanceHelper;

  /**
   * 
   */
  public YmDepositBuilder(
      @Nonnull NetworkEnum network,
      @Nonnull DatabaseBalanceHelper databaseBalanceHelper) {
    this.network = network;

    this.databaseBalanceHelper = databaseBalanceHelper;
  }

  /**
   * 
   */
  public void build(@Nonnull Connection connection) throws DfxException {
    LOGGER.debug("build()");

    long startTime = System.currentTimeMillis();

    try {
      openStatements(connection);

      Set<Integer> depositAddressNumberSet =
          databaseBalanceHelper.getDepositDTOList()
              .stream().map(DepositDTO::getDepositAddressNumber).collect(Collectors.toSet());

      // ...
      List<StakingAddressDTO> stakingAddressDTOList = databaseBalanceHelper.getStakingAddressDTOList();

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

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
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
              + " GROUP BY"
              + " ata_out.address_number,"
              + " ata_in.address_number";
      customTransactionByAddressSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, customTransactionByAddressSelectSql));

      // ...
      String addressTransactionInByBlockAndTransactionSelectSql =
          "SELECT * FROM " + TOKEN_PUBLIC_SCHEMA + ".address_transaction_in"
              + " WHERE block_number=? AND transaction_number=?";
      addressTransactionInByBlockAndTransactionSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, addressTransactionInByBlockAndTransactionSelectSql));

      // ...
      String customAccountToAccountInByBlockAndAddressSelectSql =
          "SELECT * FROM " + TOKEN_NETWORK_CUSTOM_SCHEMA + ".account_to_account_in"
              + " WHERE block_number=? AND address_number=?";
      customAccountToAccountInByBlockAndAddressSelectStatement =
          connection.prepareStatement(DatabaseUtils.replaceSchema(network, customAccountToAccountInByBlockAndAddressSelectSql));

      // Deposit ...
      String depositInsertSql =
          "INSERT INTO " + TOKEN_YIELDMACHINE_SCHEMA + ".deposit"
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

      addressTransactionInByBlockAndTransactionSelectStatement.close();
      customAccountToAccountInByBlockAndAddressSelectStatement.close();

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

    List<DepositFinderDTO> depositFinderDTOList =
        getDepositFinderDTOList(liquidityStartBlockNumber, liquidityAddressNumber);

    for (DepositFinderDTO depositFinderDTO : depositFinderDTOList) {
      int depositAddressNumber = depositFinderDTO.getInAddressNumber();

      if (!depositAddressNumberSet.contains(depositAddressNumber)) {
        Integer tokenNumber = depositFinderDTO.getTokenNumber();

        DepositDTO depositDTO = null;

        if (0 == tokenNumber) {
          depositDTO =
              getDFIDeposit(liquidityAddressNumber, depositFinderDTO, unifierAddressSet);
        } else {
          depositDTO =
              getTokenDepositDTO(liquidityAddressNumber, depositFinderDTO, unifierAddressSet);
        }

        if (null != depositDTO) {
          depositDTOList.add(depositDTO);
        }
      }
    }

    return depositDTOList;
  }

  /**
   * 
   */
  private @Nullable DepositDTO getDFIDeposit(
      int liquidityAddressNumber,
      @Nonnull DepositFinderDTO depositFinderDTO,
      @Nonnull Set<Integer> unifierAddressSet) throws DfxException {
    LOGGER.trace("getDFIDeposit()");

    DepositDTO depositDTO = null;

    int blockNumber = depositFinderDTO.getBlockNumber();
    int transactionNumber = depositFinderDTO.getTransactionNumber();
    int depositAddressNumber = depositFinderDTO.getInAddressNumber();
    int tokenNumber = depositFinderDTO.getTokenNumber();

    List<AddressTransactionInDTO> addressTransactionInDTOList = getAddressTransactionInDTOList(blockNumber, transactionNumber);

    if (!addressTransactionInDTOList.isEmpty()) {
      AddressTransactionInDTO addressTransactionInDTO = addressTransactionInDTOList.get(0);

      int inBlockNumber = addressTransactionInDTO.getInBlockNumber();
      int inTransactionNumber = addressTransactionInDTO.getInTransactionNumber();

      AddressTransactionInDTO customerAddressTransactionInDTO = getCustomerAddressTransactionInDTO(inBlockNumber, inTransactionNumber);

      if (null != customerAddressTransactionInDTO) {
        int customerAddressNumber = customerAddressTransactionInDTO.getAddressNumber();

        if (liquidityAddressNumber != customerAddressNumber) {
          if (unifierAddressSet.add(customerAddressNumber)) {
            depositDTO = new DepositDTO(tokenNumber);

            depositDTO.setLiquidityAddressNumber(liquidityAddressNumber);
            depositDTO.setDepositAddressNumber(depositAddressNumber);
            depositDTO.setCustomerAddressNumber(customerAddressNumber);
            depositDTO.setStartBlockNumber(customerAddressTransactionInDTO.getBlockNumber());
            depositDTO.setStartTransactionNumber(customerAddressTransactionInDTO.getTransactionNumber());
          }
        }
      }
    }

    return depositDTO;
  }

  /**
   * 
   */
  private @Nullable AddressTransactionInDTO getCustomerAddressTransactionInDTO(
      int inBlockNumber,
      int inTransactionNumber) throws DfxException {
    AddressTransactionInDTO addressTransactionInDTO = null;

    List<AddressTransactionInDTO> addressTransactionInDTOList = getAddressTransactionInDTOList(inBlockNumber, inTransactionNumber);

    if (!addressTransactionInDTOList.isEmpty()) {
      addressTransactionInDTO = addressTransactionInDTOList.get(0);
    }

    return addressTransactionInDTO;
  }

  /**
   * 
   */
  private List<AddressTransactionInDTO> getAddressTransactionInDTOList(
      int blockNumber,
      int transactionNumber) throws DfxException {
    LOGGER.trace("getAddressTransactionInDTOList()");

    try {
      List<AddressTransactionInDTO> addressTransactionInDTOList = new ArrayList<>();

      addressTransactionInByBlockAndTransactionSelectStatement.setInt(1, blockNumber);
      addressTransactionInByBlockAndTransactionSelectStatement.setInt(2, transactionNumber);

      ResultSet resultSet = addressTransactionInByBlockAndTransactionSelectStatement.executeQuery();

      while (resultSet.next()) {
        AddressTransactionInDTO addressTransactionInDTO =
            new AddressTransactionInDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("vin_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("in_block_number"),
                resultSet.getInt("in_transaction_number"));
        addressTransactionInDTO.setVin(resultSet.getBigDecimal("vin"));

        addressTransactionInDTOList.add(addressTransactionInDTO);
      }

      resultSet.close();

      // ...
      if (!addressTransactionInDTOList.isEmpty()) {
        Comparator<AddressTransactionInDTO> comparator =
            Comparator.comparing(AddressTransactionInDTO::getVinNumber);
        addressTransactionInDTOList.sort(comparator);
      }

      return addressTransactionInDTOList;
    } catch (Exception e) {
      throw new DfxException("getAddressTransactionInDTOList", e);
    }
  }

  /**
   * 
   */
  private @Nullable DepositDTO getTokenDepositDTO(
      int liquidityAddressNumber,
      @Nonnull DepositFinderDTO depositFinderDTO,
      @Nonnull Set<Integer> unifierAddressSet) throws DfxException {
    LOGGER.trace("getTokenDepositDTO()");

    DepositDTO depositDTO = null;

    int tokenNumber = depositFinderDTO.getTokenNumber();

    int depositAddressNumber = depositFinderDTO.getInAddressNumber();

    List<DepositFinderDTO> customerFinderDTOList =
        getDepositFinderDTOList(-1, depositAddressNumber);

    for (DepositFinderDTO customerFinderDTO : customerFinderDTOList) {
      int customerAddressNumber = customerFinderDTO.getInAddressNumber();

      if (liquidityAddressNumber != customerAddressNumber) {
        if (unifierAddressSet.add(customerAddressNumber)) {
          depositDTO = new DepositDTO(tokenNumber);

          depositDTO.setLiquidityAddressNumber(liquidityAddressNumber);
          depositDTO.setDepositAddressNumber(depositAddressNumber);
          depositDTO.setCustomerAddressNumber(customerAddressNumber);
          depositDTO.setStartBlockNumber(customerFinderDTO.getBlockNumber());
          depositDTO.setStartTransactionNumber(customerFinderDTO.getTransactionNumber());

          break;
        }
      }
    }

    return depositDTO;
  }

  /**
   * 
   */
  private List<DepositFinderDTO> getDepositFinderDTOList(
      int startBlockNumber,
      int addressNumber) throws DfxException {
    LOGGER.trace("getDepositFinderDTOList()");

    try {
      List<DepositFinderDTO> depositFinderDTOList = new ArrayList<>();

      customTransactionByAddressSelectStatement.setInt(1, startBlockNumber);
      customTransactionByAddressSelectStatement.setInt(2, addressNumber);

      ResultSet resultSet = customTransactionByAddressSelectStatement.executeQuery();

      while (resultSet.next()) {
        int blockNumber = resultSet.getInt("block_number");
        int inAddressNumber = resultSet.getInt("in_address_number");

        TransactionCustomAccountToAccountInDTO firstTransactionCustomAccountToAccountInDTO =
            getFirstTransactionCustomAccountToAccountInDTO(blockNumber, inAddressNumber);

        int transactionNumber = firstTransactionCustomAccountToAccountInDTO.getTransactionNumber();
        Integer tokenNumber = firstTransactionCustomAccountToAccountInDTO.getTokenNumber();

        DepositFinderDTO depositFinderDTO =
            new DepositFinderDTO(blockNumber, transactionNumber, inAddressNumber, tokenNumber);

        depositFinderDTOList.add(depositFinderDTO);
      }

      resultSet.close();

      // ...
      Comparator<DepositFinderDTO> comparator =
          Comparator.comparing(DepositFinderDTO::getTokenNumber)
              .thenComparing(DepositFinderDTO::getBlockNumber)
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
  private TransactionCustomAccountToAccountInDTO getFirstTransactionCustomAccountToAccountInDTO(int blockNumber, int addressNumber) throws DfxException {
    LOGGER.trace("getFirstTransactionCustomAccountToAccountInDTO()");

    try {
      customAccountToAccountInByBlockAndAddressSelectStatement.setInt(1, blockNumber);
      customAccountToAccountInByBlockAndAddressSelectStatement.setInt(2, addressNumber);

      List<TransactionCustomAccountToAccountInDTO> transactionCustomAccountToAccountInDTOList = new ArrayList<>();

      ResultSet resultSet = customAccountToAccountInByBlockAndAddressSelectStatement.executeQuery();

      while (resultSet.next()) {
        TransactionCustomAccountToAccountInDTO transactionCustomAccountToAccountInDTO =
            new TransactionCustomAccountToAccountInDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("type_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("token_number"));

        transactionCustomAccountToAccountInDTOList.add(transactionCustomAccountToAccountInDTO);
      }

      resultSet.close();

      // ...
      Comparator<TransactionCustomAccountToAccountInDTO> comparator =
          Comparator.comparing(TransactionCustomAccountToAccountInDTO::getTransactionNumber);
      transactionCustomAccountToAccountInDTOList.sort(comparator);

      return transactionCustomAccountToAccountInDTOList.get(0);
    } catch (Exception e) {
      throw new DfxException("getFirstTransactionCustomAccountToAccountInDTO", e);
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
    private final int inAddressNumber;
    private final int tokenNumber;

    /**
     * 
     */
    private DepositFinderDTO(
        int blockNumber,
        int transactionNumber,
        int inAddressNumber,
        int tokenNumber) {
      this.blockNumber = blockNumber;
      this.transactionNumber = transactionNumber;
      this.inAddressNumber = inAddressNumber;
      this.tokenNumber = tokenNumber;
    }

    private int getBlockNumber() {
      return blockNumber;
    }

    private int getTransactionNumber() {
      return transactionNumber;
    }

    private int getInAddressNumber() {
      return inAddressNumber;
    }

    private int getTokenNumber() {
      return tokenNumber;
    }
  }
}
