package ch.dfx.transactionserver.handler;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressDTO;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class DatabaseAddressHandler {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseAddressHandler.class);

  // ...
  private final NetworkEnum network;

  private final Map<String, AddressDTO> newAddressMap;

  // ...
  private int nextAddressNumber = 0;

  /**
   * 
   */
  public DatabaseAddressHandler(@Nonnull NetworkEnum network) {
    this.network = network;

    this.newAddressMap = new LinkedHashMap<>();
  }

  /**
   * 
   */
  public void setup(@Nonnull Connection connection) throws DfxException {
    newAddressMap.clear();

    nextAddressNumber = DatabaseUtils.getNextAddressNumber(network, connection);
  }

  /**
   * 
   */
  public void reset() {
    newAddressMap.clear();
  }

  /**
   * 
   */
  public Map<String, AddressDTO> getNewAddressMap() {
    return newAddressMap;
  }

  /**
   * 
   */
  public @Nonnull AddressDTO getAddressDTO(
      @Nonnull DatabaseBlockHelper databaseBlockHelper,
      @Nonnull String address) throws DfxException {
    LOGGER.trace("getAddressDTO()");

    AddressDTO addressDTO = newAddressMap.get(address);

    if (null == addressDTO) {
      addressDTO = databaseBlockHelper.getAddressDTOByAddress(address);
    }

    if (null == addressDTO) {
      addressDTO = new AddressDTO(nextAddressNumber++, address);
      newAddressMap.put(address, addressDTO);
    }

    return addressDTO;
  }
}
