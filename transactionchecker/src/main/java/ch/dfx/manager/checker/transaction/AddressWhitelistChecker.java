package ch.dfx.manager.checker.transaction;

import java.sql.Connection;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.AddressWhitelistDTO;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.data.VaultWhitelistDTO;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class AddressWhitelistChecker {
  private static final Logger LOGGER = LogManager.getLogger(AddressWhitelistChecker.class);

  private final H2DBManager databaseManager;
  private final DatabaseBlockHelper databaseBlockHelper;

  /**
   * 
   */
  public AddressWhitelistChecker(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
  }

  /**
   * 
   */
  public boolean checkAddressWhitelist(
      @Nonnull String address,
      boolean withRewardAddress) {
    LOGGER.trace("checkAddressWhitelist()");

    return checkAddressWhitelist(Arrays.asList(address), withRewardAddress);
  }

  /**
   * Liquidity Address
   * Reward Address
   * Vault Address
   * Masternode Address
   */
  public boolean checkAddressWhitelist(
      @Nonnull List<String> addressList,
      boolean withRewardAddress) {
    LOGGER.trace("checkAddressWhitelist()");

    // ...
    boolean isValid;

    // ...
    BitSet bitSet = new BitSet(addressList.size());
    bitSet.clear();

    // ...
    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);

      // ...
      Set<String> addressCheckSet = createAddressCheckSet(withRewardAddress);

      // ...
      for (int i = 0; i < addressList.size(); i++) {
        String address = addressList.get(i);

        if (addressCheckSet.contains(address)) {
          bitSet.set(i);
        } else {
          MasternodeWhitelistDTO masternodeWhitelistDTO =
              databaseBlockHelper.getMasternodeWhitelistDTOByOwnerAddress(address);

          if (null != masternodeWhitelistDTO
              && address.equals(masternodeWhitelistDTO.getOwnerAddress())) {
            bitSet.set(i);
          }
        }
      }

      isValid = bitSet.cardinality() == addressList.size();

      // ...
      databaseBlockHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkAddressWhitelist", e);
      isValid = false;
    } finally {
      databaseManager.closeConnection(connection);
    }

    return isValid;
  }

  /**
   * Address Whitelist (Liquidity / Reward)
   * Vault Whitelist
   */
  private Set<String> createAddressCheckSet(boolean withRewardAddress) throws DfxException {
    LOGGER.trace("createAddressCheckSet()");

    // ...
    List<AddressWhitelistDTO> workAddressWhitelistDTOList;
    List<AddressWhitelistDTO> addressWhitelistDTOList = databaseBlockHelper.getAddressWhitelistDTOList();

    if (withRewardAddress) {
      workAddressWhitelistDTOList = addressWhitelistDTOList;
    } else {
      workAddressWhitelistDTOList =
          addressWhitelistDTOList.stream()
              .filter(dto -> !"reward".equalsIgnoreCase(dto.getType()))
              .collect(Collectors.toList());
    }

    // ...
    List<VaultWhitelistDTO> vaultWhitelistDTOList = databaseBlockHelper.getVaultWhitelistDTOList();

    // ...
    Set<String> addressCheckSet = new HashSet<>();

    workAddressWhitelistDTOList.forEach(dto -> addressCheckSet.add(dto.getAddress()));
    vaultWhitelistDTOList.forEach(dto -> addressCheckSet.add(dto.getAddress()));

    return addressCheckSet;
  }
}
