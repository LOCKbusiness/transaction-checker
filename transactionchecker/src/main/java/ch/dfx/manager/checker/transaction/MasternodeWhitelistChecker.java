package ch.dfx.manager.checker.transaction;

import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_STAKING_SCHEMA;
import static ch.dfx.transactionserver.database.DatabaseUtils.TOKEN_YIELDMACHINE_SCHEMA;

import java.sql.Connection;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.transactionserver.data.MasternodeWhitelistDTO;
import ch.dfx.transactionserver.database.H2DBManager;
import ch.dfx.transactionserver.database.helper.DatabaseBalanceHelper;
import ch.dfx.transactionserver.database.helper.DatabaseBlockHelper;

/**
 * 
 */
public class MasternodeWhitelistChecker {
  private static final Logger LOGGER = LogManager.getLogger(MasternodeWhitelistChecker.class);

  private final H2DBManager databaseManager;
  private final DatabaseBlockHelper databaseBlockHelper;
  private final DatabaseBalanceHelper databaseStakingBalanceHelper;
  private final DatabaseBalanceHelper databaseYieldmachineBalanceHelper;

  /**
   * 
   */
  public MasternodeWhitelistChecker(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager) {
    this.databaseManager = databaseManager;

    this.databaseBlockHelper = new DatabaseBlockHelper(network);
    this.databaseStakingBalanceHelper = new DatabaseBalanceHelper(network);
    this.databaseYieldmachineBalanceHelper = new DatabaseBalanceHelper(network);
  }

  /**
   * 
   */
  public boolean checkMasternodeWhitelist(@Nonnull String address) {
    LOGGER.trace("checkMasternodeWhitelist()");

    return checkMasternodeWhitelist(Arrays.asList(address));
  }

  /**
   * 
   */
  public boolean checkMasternodeWhitelist(@Nonnull List<String> voutAddressList) {
    LOGGER.trace("checkMasternodeWhitelist()");

    // ...
    boolean isValid;

    // ...
    BitSet bitSet = new BitSet(voutAddressList.size());
    bitSet.clear();

    // ...
    Connection connection = null;

    try {
      connection = databaseManager.openConnection();

      databaseBlockHelper.openStatements(connection);

      // ...
      Set<String> liquidityAddressSet = createLiquidityAddressSet(connection);

      // ...
      for (int i = 0; i < voutAddressList.size(); i++) {
        String voutAddress = voutAddressList.get(i);

        if (liquidityAddressSet.contains(voutAddress)) {
          bitSet.set(i);
        } else {
          MasternodeWhitelistDTO masternodeWhitelistDTO =
              databaseBlockHelper.getMasternodeWhitelistDTOByOwnerAddress(voutAddress);

          if (null != masternodeWhitelistDTO
              && voutAddress.equals(masternodeWhitelistDTO.getOwnerAddress())) {
            bitSet.set(i);
          }
        }
      }

      isValid = bitSet.cardinality() == voutAddressList.size();

      // ...
      databaseBlockHelper.closeStatements();
    } catch (Exception e) {
      LOGGER.error("checkMasternodeWhitelist", e);
      isValid = false;
    } finally {
      databaseManager.closeConnection(connection);
    }

    return isValid;
  }

  /**
   * 
   */
  private Set<String> createLiquidityAddressSet(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("createLiquidityAddressSet()");

    try {
      databaseStakingBalanceHelper.openStatements(connection, TOKEN_STAKING_SCHEMA);
      databaseYieldmachineBalanceHelper.openStatements(connection, TOKEN_YIELDMACHINE_SCHEMA);

      // ...
      Set<String> liquidityAddressSet = new HashSet<>();
      databaseStakingBalanceHelper.getStakingAddressDTOList().forEach(dto -> liquidityAddressSet.add(dto.getLiquidityAddress()));
      databaseYieldmachineBalanceHelper.getStakingAddressDTOList().forEach(dto -> liquidityAddressSet.add(dto.getLiquidityAddress()));

      // ...
      databaseStakingBalanceHelper.closeStatements();
      databaseYieldmachineBalanceHelper.closeStatements();

      return liquidityAddressSet;
    } catch (DfxException e) {
      throw e;
    } catch (Exception e) {
      throw new DfxException("createLiquidityAddressSet", e);
    }
  }
}
