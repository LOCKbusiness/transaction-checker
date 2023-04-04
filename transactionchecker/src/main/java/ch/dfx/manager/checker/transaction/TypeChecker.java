package ch.dfx.manager.checker.transaction;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.enumeration.ApiTransactionTypeEnum;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.handler.DefiMessageHandler;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.ManagerUtils;

/**
 * 
 */
public class TypeChecker extends TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(TypeChecker.class);

  // ...
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public TypeChecker(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiMessageHandler messageHandler,
      @Nonnull DefiDataProvider dataProvider) {
    super(apiAccessHandler, messageHandler);

    this.dataProvider = dataProvider;
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
      ApiTransactionTypeEnum apiTransactionType = openTransactionDTO.getType();

      DefiTransactionData transactionData = openTransactionDTO.getTransactionData();

      byte customType = ManagerUtils.getCustomType(dataProvider, transactionData);
      String customTypeCode = "0";

      if (0x00 != customType) {
        customTypeCode = String.valueOf((char) customType);
      }

      isValid = apiTransactionType.checkCustomType(customTypeCode);

      if (!isValid) {
        openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - type mismatch");
        sendInvalidated(openTransactionDTO);
      }
    } catch (Exception e) {
      LOGGER.error("doCheckType", e);
      isValid = false;
    }

    return isValid;
  }
}
