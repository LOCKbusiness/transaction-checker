package ch.dfx.manager.checker.transaction;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class SizeChecker extends TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(SizeChecker.class);

  /**
   * 
   */
  public SizeChecker(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiDataProvider dataProvider) {
    super(apiAccessHandler, dataProvider);
  }

  /**
   * 
   */
  public OpenTransactionDTOList checkSize(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("checkSize()");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      if (doCheckSize(openTransactionDTO)) {
        checkedOpenTransactionDTOList.add(openTransactionDTO);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doCheckSize(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("doCheckSize()");

    boolean isValid;

    try {
      int rawTransactionMaxSize = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RAW_TRANSACTION_MAX_SIZE, 250000);

      String hex = openTransactionDTO.getRawTx().getHex();

      isValid = rawTransactionMaxSize > hex.length();

      if (!isValid) {
        openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - size too big");
        sendInvalidated(openTransactionDTO);
      }
    } catch (Exception e) {
      LOGGER.error("doCheckSize", e);
      isValid = false;
    }

    return isValid;
  }
}
