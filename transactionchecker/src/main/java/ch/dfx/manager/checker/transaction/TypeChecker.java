package ch.dfx.manager.checker.transaction;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionTypeEnum;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class TypeChecker extends TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(TypeChecker.class);

  /**
   * 
   */
  public TypeChecker(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiDataProvider dataProvider) {
    super(apiAccessHandler, dataProvider);
  }

  /**
   * 
   */
  public OpenTransactionDTOList checkType(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("checkType()");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      if (doCheckType(openTransactionDTO)) {
        checkedOpenTransactionDTOList.add(openTransactionDTO);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doCheckType(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("doCheckType()");

    boolean isValid;

    try {
      isValid = OpenTransactionTypeEnum.UNKNOWN != openTransactionDTO.getType();

      if (!isValid) {
        openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - unknown type");
        sendInvalidated(openTransactionDTO);
      }
    } catch (Exception e) {
      LOGGER.error("doCheckType", e);
      isValid = false;
    }

    return isValid;
  }
}
