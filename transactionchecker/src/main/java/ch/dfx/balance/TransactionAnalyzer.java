package ch.dfx.balance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressTransactionInDTO;
import ch.dfx.transactionserver.data.DepositDTO;
import ch.dfx.transactionserver.data.LiquidityDTO;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class TransactionAnalyzer {
  private static final Logger LOGGER = LogManager.getLogger(TransactionAnalyzer.class);

  private PreparedStatement liquiditySelectStatement = null;

  private PreparedStatement numberToAddressSelectStatement = null;

  private PreparedStatement firstTransactionByBlockAndAddressSelectStatement = null;
  private PreparedStatement transactionByBlockAndTransactionSelectStatement = null;

  private PreparedStatement depositInsertStatement = null;

  /**
   * Test: tf1q4w2vk08ewe38vqwa3p62ndjw72589fkzzruvzr
   */
  public TransactionAnalyzer() {
  }

  /**
   * 
   */
  public void run() throws DfxException {
    Connection connection = null;

    try {
      connection = H2DBManager.getInstance().openConnection();
      openStatements(connection);

      // ...
      List<LiquidityDTO> liquidityDTOList = getLiquidityDTOList();

      for (LiquidityDTO liquidityDTO : liquidityDTOList) {
        List<DepositDTO> depositDTOList = getDepositDTOList(liquidityDTO);

        for (DepositDTO depositDTO : depositDTOList) {
          fillCustomerAddress(liquidityDTO.getStartBlockNumber(), depositDTO);
          LOGGER.debug("Deposit: " + depositDTO);
        }

        // ...
        for (DepositDTO depositDTO : depositDTOList) {
          String customerAddress = getAddressByNumber(depositDTO.getCustomerAddressNumber());
          String depositAddress = getAddressByNumber(depositDTO.getDepositAddressNumber());
          // insertDeposit(depositDTO);
          LOGGER.debug(customerAddress + " === " + depositAddress);
        }
      }

      closeStatements();
      // connection.commit();
    } catch (Exception e) {
      // DatabaseUtils.rollback(connection);
      throw new DfxException("run", e);
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
      // Liquidity ...
      String liquiditySql = "SELECT * FROM public.liquidity";
      liquiditySelectStatement = connection.prepareStatement(liquiditySql);

      // Address ...
      String numberToAddressSql = "SELECT address FROM public.address WHERE number=?";
      numberToAddressSelectStatement = connection.prepareStatement(numberToAddressSql);

      // Transaction ...
      String firstTransactionByBlockAndAddressSql =
          "SELECT * FROM public.address_transaction_in WHERE block_number>=? AND address_number=?"
              + " ORDER BY block_number, transaction_number, vin_number";
      firstTransactionByBlockAndAddressSelectStatement = connection.prepareStatement(firstTransactionByBlockAndAddressSql);

      String transactionByBlockAndTransactionSql = "SELECT * FROM public.address_transaction_in WHERE block_number=? AND transaction_number=?";
      transactionByBlockAndTransactionSelectStatement = connection.prepareStatement(transactionByBlockAndTransactionSql);

      // Deposit ...
      String depositInsertSql =
          "INSERT INTO public.deposit"
              + " (customer_address_number, deposit_address_number, start_block_number, start_transaction_number)"
              + " VALUES (?, ?, ?, ?)";
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

      firstTransactionByBlockAndAddressSelectStatement.close();
      transactionByBlockAndTransactionSelectStatement.close();

      depositInsertStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private List<LiquidityDTO> getLiquidityDTOList() throws DfxException {
    LOGGER.trace("getLiquidityDTOList() ...");

    try {
      List<LiquidityDTO> liquidityDTOList = new ArrayList<>();

      ResultSet resultSet = liquiditySelectStatement.executeQuery();

      while (resultSet.next()) {
        LiquidityDTO liquidityDTO = new LiquidityDTO();
        liquidityDTO.setAddressNumber(resultSet.getInt("address_number"));
        liquidityDTO.setStartBlockNumber(resultSet.getInt("start_block_number"));
        liquidityDTO.setStartTransactionNumber(resultSet.getInt("start_transaction_number"));

        liquidityDTOList.add(liquidityDTO);
      }
      return liquidityDTOList;
    } catch (Exception e) {
      throw new DfxException("getLiquidityDTOList", e);
    }
  }

  /**
   * 
   */
  private List<DepositDTO> getDepositDTOList(@Nonnull LiquidityDTO liquidityDTO) throws DfxException {
    LOGGER.trace("getDepositDTOList() ...");

    List<DepositDTO> depositDTOList = new ArrayList<>();
    Set<Integer> unifierDepositSet = new HashSet<>();

    int liquidityStartBlockNumber = liquidityDTO.getStartBlockNumber();
    int liquidityAddressNumber = liquidityDTO.getAddressNumber();

    List<AddressTransactionInDTO> liquidityTransactionInDataList = getFirstTransactionInList(liquidityStartBlockNumber, liquidityAddressNumber);

    for (AddressTransactionInDTO liquidityTransactionInData : liquidityTransactionInDataList) {
//      LOGGER.debug(
//          "Liquidity: "
//              + liquidityTransactionInData.getBlockNumber() + " | " + liquidityTransactionInData.getTransactionNumber()
//              + " | " + liquidityTransactionInData.getInBlockNumber() + " | " + liquidityTransactionInData.getInTransactionNumber()
//              + " | " + liquidityTransactionInData.getAddressNumber());

      List<AddressTransactionInDTO> depositTransactionInDTOList =
          getTransactionInList(liquidityTransactionInData.getInBlockNumber(), liquidityTransactionInData.getInTransactionNumber());

      for (AddressTransactionInDTO depositTransactionInDTO : depositTransactionInDTOList) {
        Integer addressNumber = depositTransactionInDTO.getAddressNumber();

//        LOGGER.debug(
//            "Deposit: "
//                + depositTransactionInData.getBlockNumber() + " | " + depositTransactionInData.getTransactionNumber()
//                + " | " + depositTransactionInData.getInBlockNumber() + " | " + depositTransactionInData.getInTransactionNumber()
//                + " | " + addressNumber);

        if (liquidityDTO.getAddressNumber() != addressNumber.intValue()) {
          if (unifierDepositSet.add(addressNumber)) {
            DepositDTO depositDTO = new DepositDTO();

            depositDTO.setDepositAddressNumber(depositTransactionInDTO.getAddressNumber());
            depositDTO.setStartBlockNumber(depositTransactionInDTO.getBlockNumber());
            depositDTO.setStartTransactionNumber(depositTransactionInDTO.getTransactionNumber());

            depositDTOList.add(depositDTO);
          }
        }
      }
    }

    return depositDTOList;
  }

  /**
   * 
   */
  private void fillCustomerAddress(
      int startBlockNumber,
      @Nonnull DepositDTO depositDTO) throws DfxException {
    LOGGER.trace("getCustomerAddress() ...");

    List<AddressTransactionInDTO> depositTransactionInDTOList =
        getFirstTransactionInList(startBlockNumber, depositDTO.getDepositAddressNumber());

    if (!depositTransactionInDTOList.isEmpty()) {
      AddressTransactionInDTO depositTransactionInDTO = depositTransactionInDTOList.get(0);

      List<AddressTransactionInDTO> customerTransactionInDTOList =
          getTransactionInList(depositTransactionInDTO.getInBlockNumber(), depositTransactionInDTO.getInTransactionNumber());

      if (!customerTransactionInDTOList.isEmpty()) {
        AddressTransactionInDTO customerTransactionInDTO = customerTransactionInDTOList.get(0);

        depositDTO.setCustomerAddressNumber(customerTransactionInDTO.getAddressNumber());
      }
    }
  }

  /**
   * 
   */
  private String getAddressByNumber(int addressNumber) throws DfxException {
    LOGGER.trace("getAddressNumber() ...");

    try {
      String address = null;

      numberToAddressSelectStatement.setInt(1, addressNumber);

      ResultSet resultSet = numberToAddressSelectStatement.executeQuery();

      if (resultSet.next()) {
        address = resultSet.getString("address");
      }

      if (null == address) {
        throw new DfxException("Unknown address number: " + addressNumber);
      }

      return address;
    } catch (Exception e) {
      throw new DfxException("getAddressNumber", e);
    }
  }

  /**
   * 
   */
  private List<AddressTransactionInDTO> getFirstTransactionInList(
      int startBlockNumber,
      int addressNumber) throws DfxException {
    LOGGER.trace("getFirstTransactionInList() ...");

    try {
      Set<String> unifierSet = new HashSet<>();
      List<AddressTransactionInDTO> transactionInList = new ArrayList<>();

      firstTransactionByBlockAndAddressSelectStatement.setInt(1, startBlockNumber);
      firstTransactionByBlockAndAddressSelectStatement.setInt(2, addressNumber);

      ResultSet resultSet = firstTransactionByBlockAndAddressSelectStatement.executeQuery();

      while (resultSet.next()) {
        int blockNumber = resultSet.getInt("block_number");
        int transactionNumber = resultSet.getInt("transaction_number");

        String uniqKey = Integer.toString(blockNumber) + "/" + Integer.toString(transactionNumber);

        if (unifierSet.add(uniqKey)) {
          AddressTransactionInDTO cacheAddressTransactionInData =
              new AddressTransactionInDTO(
                  blockNumber,
                  transactionNumber,
                  resultSet.getInt("vin_number"),
                  resultSet.getInt("address_number"),
                  resultSet.getInt("in_block_number"),
                  resultSet.getInt("in_transaction_number"));
          cacheAddressTransactionInData.setVin(resultSet.getBigDecimal("vin"));

          transactionInList.add(cacheAddressTransactionInData);
        }
      }

      resultSet.close();

      // ...
      if (transactionInList.isEmpty()) {
        throw new DfxException("cannot determine transactions for address " + addressNumber);
      }

      // ...
//      Comparator<CacheAddressTransactionInData> comparator =
//          Comparator.comparing(CacheAddressTransactionInData::getBlockNumber)
//              .thenComparing(CacheAddressTransactionInData::getVinNumber);
//      transactionInList.sort(comparator);

      return transactionInList;
    } catch (Exception e) {
      throw new DfxException("getFirstTransactionInList", e);
    }
  }

  /**
   * 
   */
  private List<AddressTransactionInDTO> getTransactionInList(
      int blockNumber,
      int transactionNumber) throws DfxException {
    LOGGER.trace("getTransactionInList() ...");

    try {
      List<AddressTransactionInDTO> transactionInList = new ArrayList<>();

      transactionByBlockAndTransactionSelectStatement.setInt(1, blockNumber);
      transactionByBlockAndTransactionSelectStatement.setInt(2, transactionNumber);

      ResultSet resultSet = transactionByBlockAndTransactionSelectStatement.executeQuery();

      while (resultSet.next()) {
        AddressTransactionInDTO cacheAddressTransactionInData =
            new AddressTransactionInDTO(
                resultSet.getInt("block_number"),
                resultSet.getInt("transaction_number"),
                resultSet.getInt("vin_number"),
                resultSet.getInt("address_number"),
                resultSet.getInt("in_block_number"),
                resultSet.getInt("in_transaction_number"));
        cacheAddressTransactionInData.setVin(resultSet.getBigDecimal("vin"));

        transactionInList.add(cacheAddressTransactionInData);
      }

      resultSet.close();

      // ...
      if (transactionInList.isEmpty()) {
        throw new DfxException("cannot determine transaction " + transactionNumber + " in block " + blockNumber);
      }

      // ...
      Comparator<AddressTransactionInDTO> comparator =
          Comparator.comparing(AddressTransactionInDTO::getBlockNumber)
              .thenComparing(AddressTransactionInDTO::getVinNumber);
      transactionInList.sort(comparator);

      return transactionInList;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("getTransactionInList", e);
    }
  }

  /**
   * 
   */
  private void insertDeposit(@Nonnull DepositDTO depositDTO) throws DfxException {
    LOGGER.trace("insertDeposit() ...");

    try {
      depositInsertStatement.setInt(1, depositDTO.getCustomerAddressNumber());
      depositInsertStatement.setInt(2, depositDTO.getDepositAddressNumber());
      depositInsertStatement.setInt(3, depositDTO.getStartBlockNumber());
      depositInsertStatement.setInt(4, depositDTO.getStartTransactionNumber());
      depositInsertStatement.execute();
    } catch (Exception e) {
      throw new DfxException("insertDeposit", e);
    }
  }
}
