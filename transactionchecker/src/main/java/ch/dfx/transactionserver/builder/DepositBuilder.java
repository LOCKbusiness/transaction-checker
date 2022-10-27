package ch.dfx.transactionserver.builder;

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

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressTransactionInDTO;
import ch.dfx.transactionserver.data.AddressTransactionOutDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.LiquidityDTO;
import ch.dfx.transactionserver.database.DatabaseHelper;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class DepositBuilder {
  private static final Logger LOGGER = LogManager.getLogger(DepositBuilder.class);

  private PreparedStatement liquiditySelectStatement = null;

  private PreparedStatement numberToAddressSelectStatement = null;

  private PreparedStatement outTransactionByBlockAndAddressSelectStatement = null;
  private PreparedStatement inTransactionByBlockAndTransactionSelectStatement = null;

  private PreparedStatement depositInsertStatement = null;

  // ...
  private final H2DBManager databaseManager;
  private final DatabaseHelper databaseHelper;

  /**
   * Test: tf1q4w2vk08ewe38vqwa3p62ndjw72589fkzzruvzr
   */
  public DepositBuilder(@Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;
    this.databaseHelper = new DatabaseHelper();
  }

  /**
   * 
   */
  public void build() throws DfxException {
    LOGGER.trace("build() ...");

    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseHelper.openStatements(connection);
      openStatements(connection);

      // ...
      List<LiquidityDTO> liquidityDTOList = databaseHelper.getLiquidityDTOList();

      Set<Integer> depositAddressNumberSet =
          databaseHelper.getDepositDTOList().stream().map(DepositDTO::getDepositAddressNumber).collect(Collectors.toSet());

      // ...
      for (LiquidityDTO liquidityDTO : liquidityDTOList) {
        LOGGER.debug("Liquidity Address: " + liquidityDTO.getAddress());
        List<DepositDTO> depositDTOList = getDepositDTOList(liquidityDTO, depositAddressNumberSet);

        for (DepositDTO depositDTO : depositDTOList) {
          fillCustomerAddress(depositDTO);
        }

        // ...
        for (DepositDTO depositDTO : depositDTOList) {
          if (depositDTO.getDepositAddressNumber() != depositDTO.getCustomerAddressNumber()) {
            insertDeposit(depositDTO);
          }
        }
      }

      closeStatements();
      databaseHelper.closeStatements();

      connection.commit();
    } catch (Exception e) {
      DatabaseUtils.rollback(connection);
      throw new DfxException("build", e);
    } finally {
      databaseManager.closeConnection(connection);
    }
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      // Liquidity ...
      String liquiditySql = "SELECT * FROM public.liquidity";
      liquiditySelectStatement = connection.prepareStatement(liquiditySql);

      // Address ...
      String numberToAddressSql = "SELECT address FROM public.address WHERE number=?";
      numberToAddressSelectStatement = connection.prepareStatement(numberToAddressSql);

      // Transaction ...
      String outTransactionByBlockAndAddressSelectSql = "SELECT * FROM public.address_transaction_out WHERE block_number>=? AND address_number=?";
      outTransactionByBlockAndAddressSelectStatement = connection.prepareStatement(outTransactionByBlockAndAddressSelectSql);

      String inTransactionByBlockAndTransactionSelectSql = "SELECT * FROM public.address_transaction_in WHERE block_number=? AND transaction_number=?";
      inTransactionByBlockAndTransactionSelectStatement = connection.prepareStatement(inTransactionByBlockAndTransactionSelectSql);

      // Deposit ...
      String depositInsertSql =
          "INSERT INTO public.deposit"
              + " (liquidity_address_number, deposit_address_number, customer_address_number, start_block_number, start_transaction_number)"
              + " VALUES (?, ?, ?, ?, ?)";
      depositInsertStatement = connection.prepareStatement(depositInsertSql);
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
      liquiditySelectStatement.close();

      numberToAddressSelectStatement.close();

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
      @Nonnull LiquidityDTO liquidityDTO,
      @Nonnull Set<Integer> depositAddressNumberSet) throws DfxException {
    LOGGER.trace("getDepositDTOList() ...");

    List<DepositDTO> depositDTOList = new ArrayList<>();
    Set<String> unifierTransactionSet = new HashSet<>();
    Set<Integer> unifierAddressSet = new HashSet<>();

    int liquidityStartBlockNumber = liquidityDTO.getStartBlockNumber();
    int liquidityAddressNumber = liquidityDTO.getAddressNumber();

    List<AddressTransactionOutDTO> transactionOutDTOList = getTransactionOutDTOList(liquidityStartBlockNumber, liquidityAddressNumber);

    for (AddressTransactionOutDTO transactionOutDTO : transactionOutDTOList) {
      Integer depositBlockNumber = transactionOutDTO.getBlockNumber();
      Integer depositTransactionNumber = transactionOutDTO.getTransactionNumber();

      String transactionKey = depositBlockNumber.toString() + "/" + depositTransactionNumber.toString();

      if (unifierTransactionSet.add(transactionKey)) {
        List<AddressTransactionInDTO> depositTransactionInDTOList = getTransactionInDTOList(depositBlockNumber, depositTransactionNumber);

        for (AddressTransactionInDTO depositTransactionInDTO : depositTransactionInDTOList) {
          Integer addressNumber = depositTransactionInDTO.getAddressNumber();

          if (liquidityDTO.getAddressNumber() != addressNumber.intValue()
              && !depositAddressNumberSet.contains(addressNumber)) {
            if (unifierAddressSet.add(addressNumber)) {
              DepositDTO depositDTO = new DepositDTO();

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
    LOGGER.trace("fillCustomerAddress() ...");

    int depositStartBlockNumber = depositDTO.getStartBlockNumber();
    int depositStartTransactionNumber = depositDTO.getStartTransactionNumber();

    List<AddressTransactionInDTO> depositTransactionInDTOList = getTransactionInDTOList(depositStartBlockNumber, depositStartTransactionNumber);

    if (depositTransactionInDTOList.isEmpty()) {
      throw new DfxException("Deposit In-Transaction not found");
    }

    AddressTransactionInDTO depositTransactionInDTO = depositTransactionInDTOList.get(0);

    Integer customerBlockNumber = depositTransactionInDTO.getInBlockNumber();
    Integer customerTransactionNumber = depositTransactionInDTO.getInTransactionNumber();

    List<AddressTransactionInDTO> customerTransactionInDTOList = getTransactionInDTOList(customerBlockNumber, customerTransactionNumber);

    if (customerTransactionInDTOList.isEmpty()) {
      throw new DfxException("Customer In-Transaction not found");
    }

    AddressTransactionInDTO customerTransactionInDTO = customerTransactionInDTOList.get(0);
    depositDTO.setCustomerAddressNumber(customerTransactionInDTO.getAddressNumber());
  }

  /**
   * 
   */
  private List<AddressTransactionOutDTO> getTransactionOutDTOList(
      int startBlockNumber,
      int addressNumber) throws DfxException {
    LOGGER.trace("getTransactionOutDTOList() ...");

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
    LOGGER.trace("getTransactionInDTOList() ...");

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
      if (transactionInDTOList.isEmpty()) {
        throw new DfxException("cannot determine transaction " + transactionNumber + " in block " + blockNumber);
      }

      // ...
      Comparator<AddressTransactionInDTO> comparator =
          Comparator.comparing(AddressTransactionInDTO::getVinNumber);
      transactionInDTOList.sort(comparator);

      return transactionInDTOList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getTransactionInDTOList", e);
    }
  }

  /**
   * 
   */
  private void insertDeposit(@Nonnull DepositDTO depositDTO) throws DfxException {
    LOGGER.trace("insertDeposit() ...");

    try {
      int liquidityAddressNumber = depositDTO.getLiquidityAddressNumber();
      int depositAddressNumber = depositDTO.getDepositAddressNumber();
      int customerAddressNumber = depositDTO.getCustomerAddressNumber();

      LOGGER.debug(
          "[INSERT] Liquidity Address / Deposit Address / Customer Address: "
              + liquidityAddressNumber + " / " + depositAddressNumber + " / " + customerAddressNumber);

      depositInsertStatement.setInt(1, liquidityAddressNumber);
      depositInsertStatement.setInt(2, depositAddressNumber);
      depositInsertStatement.setInt(3, customerAddressNumber);
      depositInsertStatement.setInt(4, depositDTO.getStartBlockNumber());
      depositInsertStatement.setInt(5, depositDTO.getStartTransactionNumber());
      depositInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("insertDeposit", e);
    }
  }
}
