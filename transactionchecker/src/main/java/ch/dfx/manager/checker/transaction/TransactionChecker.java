package ch.dfx.manager.checker.transaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionPayloadDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.handler.DefiMessageHandler;
import ch.dfx.defichain.provider.DefiDataProvider;

/**
 * 
 */
public abstract class TransactionChecker {
  private static final Logger LOGGER = LogManager.getLogger(TransactionChecker.class);

  // ...
  private final ApiAccessHandler apiAccessHandler;
  protected final DefiMessageHandler messageHandler;

  /**
   * 
   */
  public TransactionChecker(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiDataProvider dataProvider) {
    this.apiAccessHandler = apiAccessHandler;

    this.messageHandler = new DefiMessageHandler(dataProvider);
  }

  /**
   * 
   */
  public void sendVerified(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("sendVerified() ...");

    String openTransactionHex = openTransactionDTO.getRawTx().getHex();
    String openTransactionCheckerSignature = messageHandler.signMessage(openTransactionHex);

    OpenTransactionVerifiedDTO openTransactionVerifiedDTO = new OpenTransactionVerifiedDTO();
    openTransactionVerifiedDTO.setSignature(openTransactionCheckerSignature);

    apiAccessHandler.sendOpenTransactionVerified(openTransactionDTO.getId(), openTransactionVerifiedDTO);
  }

  /**
   * 
   */
  public void sendInvalidated(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("sendInvalidated() ...");

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
  public @Nullable Integer getWithdrawalId(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("getWithdrawalId() ...");

    Integer withdrawalId = null;

    OpenTransactionPayloadDTO openTransactionPayloadDTO = openTransactionDTO.getPayload();

    if (null != openTransactionPayloadDTO) {
      withdrawalId = openTransactionPayloadDTO.getId();
    }

    return withdrawalId;
  }
}
