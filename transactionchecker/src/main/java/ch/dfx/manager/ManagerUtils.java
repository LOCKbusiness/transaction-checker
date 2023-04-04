package ch.dfx.manager;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.handler.DefiMessageHandler;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public class ManagerUtils {
  private static final Logger LOGGER = LogManager.getLogger(ManagerUtils.class);

  /**
   * 
   */
  public static void setWithdrawalCheckInvalidReason(
      @Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO,
      @Nonnull String messageInfo) {
    LOGGER.trace("setWithdrawalCheckInvalidReason()");

    String message = "[Withdrawal] ID: " + transactionWithdrawalDTO.getId() + " - " + messageInfo;

    transactionWithdrawalDTO.setStateReason(message);
    transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.INVALID);
  }

  /**
   * Return: 0x00 = No Custom Type
   */
  public static byte getCustomType(
      @Nonnull DefiDataProvider dataProvider,
      @Nonnull DefiTransactionData transactionData) throws DfxException {
    LOGGER.trace("getCustomType()");

    byte customType = 0x00;

    for (DefiTransactionVoutData transactionVoutData : transactionData.getVout()) {
      DefiTransactionScriptPubKeyData transactionVoutScriptPubKeyData = transactionVoutData.getScriptPubKey();
      String scriptPubKeyHex = transactionVoutScriptPubKeyData.getHex();

      customType = dataProvider.getCustomType(scriptPubKeyHex);

      if (0x00 != customType) {
        break;
      }
    }

    return customType;
  }

  /**
   * 
   */
  public static void sendVerified(
      @Nonnull DefiMessageHandler messageHandler,
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("sendVerified()");

    String openTransactionHex = openTransactionDTO.getRawTx().getHex();
    String openTransactionCheckerSignature = messageHandler.signMessage(openTransactionHex);

    OpenTransactionVerifiedDTO openTransactionVerifiedDTO = new OpenTransactionVerifiedDTO();
    openTransactionVerifiedDTO.setSignature(openTransactionCheckerSignature);

    apiAccessHandler.sendOpenTransactionVerified(openTransactionDTO.getId(), openTransactionVerifiedDTO);
  }

  /**
   * 
   */
  public static void sendInvalidated(
      @Nonnull DefiMessageHandler messageHandler,
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("sendInvalidated()");

    String openTransactionHex = openTransactionDTO.getRawTx().getHex();
    String openTransactionCheckerSignature = messageHandler.signMessage(openTransactionHex);

    OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO = new OpenTransactionInvalidatedDTO();
    openTransactionInvalidatedDTO.setSignature(openTransactionCheckerSignature);
    openTransactionInvalidatedDTO.setReason(openTransactionDTO.getInvalidatedReason());

    apiAccessHandler.sendOpenTransactionInvalidated(openTransactionDTO.getId(), openTransactionInvalidatedDTO);
  }

  /**
   * 
   */
  private ManagerUtils() {
  }
}
