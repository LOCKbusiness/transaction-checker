package ch.dfx.manager.checker.transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.handler.DefiMessageHandler;

/**
 * 
 */
public class VoutAddressChecker extends TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(VoutAddressChecker.class);

  // ...
  private final AddressWhitelistChecker addressWhitelistChecker;

  /**
   * 
   */
  public VoutAddressChecker(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiMessageHandler messageHandler,
      @Nonnull AddressWhitelistChecker addressWhitelistChecker) {
    super(apiAccessHandler, messageHandler);

    this.addressWhitelistChecker = addressWhitelistChecker;
  }

  /**
   * 
   */
  public OpenTransactionDTOList checkVoutAddress(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("checkVoutAddress()");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      if (doCheckVoutAddress(openTransactionDTO)) {
        checkedOpenTransactionDTOList.add(openTransactionDTO);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doCheckVoutAddress(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("doCheckVoutAddress()");

    boolean isValid;

    try {
      DefiTransactionData transactionData = openTransactionDTO.getTransactionData();

      Set<String> voutAddressSet = getVoutAddressSet(transactionData);

      isValid = addressWhitelistChecker.checkAddressWhitelist(new ArrayList<>(voutAddressSet), false);

      if (!isValid) {
        openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - invalid vout address");
        sendInvalidated(openTransactionDTO);
      }
    } catch (Exception e) {
      LOGGER.error("doCheckVoutAddress", e);
      isValid = false;
    }

    return isValid;
  }

  /**
   * 
   */
  private Set<String> getVoutAddressSet(@Nonnull DefiTransactionData transactionData) {
    Set<String> voutAddressSet = new HashSet<>();

    List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();

    for (DefiTransactionVoutData transactionVoutData : transactionVoutDataList) {
      DefiTransactionScriptPubKeyData transactionScriptPubKeyData = transactionVoutData.getScriptPubKey();

      if (null != transactionScriptPubKeyData) {
        List<String> addressList = transactionScriptPubKeyData.getAddresses();

        if (null != addressList) {
          voutAddressSet.addAll(addressList);
        }
      }
    }

    return voutAddressSet;
  }
}
