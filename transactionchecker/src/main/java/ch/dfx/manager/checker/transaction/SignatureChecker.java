package ch.dfx.manager.checker.transaction;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class SignatureChecker extends TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(SignatureChecker.class);

  /**
   * 
   */
  public SignatureChecker(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiDataProvider dataProvider) {
    super(apiAccessHandler, dataProvider);
  }

  /**
   * 
   */
  public OpenTransactionDTOList checkTransactionSignature(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("checkTransactionSignature()");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      if (doCheckTransactionSignature(openTransactionDTO)) {
        checkedOpenTransactionDTOList.add(openTransactionDTO);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doCheckTransactionSignature(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("doCheckTransactionSignature()");

    boolean isValid;

    try {
      isValid = validateIssuerSignature(openTransactionDTO);

      if (!isValid) {
        openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - invalid signature");
        sendInvalidated(openTransactionDTO);
      }
    } catch (Exception e) {
      LOGGER.error("doCheckTransactionSignature", e);
      isValid = false;
    }

    return isValid;
  }

  /**
   * 
   */
  private boolean validateIssuerSignature(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("validateIssurerSignature()");

    String openTransactionIssuerSignature = openTransactionDTO.getIssuerSignature();
    String openTransactionHex = openTransactionDTO.getRawTx().getHex();

    String verifyAddress = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_VERIFY_ADDRESS);

    Boolean isValid = messageHandler.verifyMessage(verifyAddress, openTransactionIssuerSignature, openTransactionHex);
    LOGGER.debug("Open Transaction Id: " + openTransactionDTO.getId() + " / " + isValid);

    return BooleanUtils.isTrue(isValid);
  }
}
