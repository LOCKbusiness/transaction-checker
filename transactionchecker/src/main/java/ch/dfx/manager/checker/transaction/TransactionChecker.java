package ch.dfx.manager.checker.transaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionPayloadDTO;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.defichain.handler.DefiMessageHandler;
import ch.dfx.manager.ManagerUtils;

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
      @Nonnull DefiMessageHandler messageHandler) {
    this.apiAccessHandler = apiAccessHandler;
    this.messageHandler = messageHandler;
  }

  /**
   * 
   */
  public void sendInvalidated(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("sendInvalidated()");

    ManagerUtils.sendInvalidated(messageHandler, apiAccessHandler, openTransactionDTO);
  }

  /**
   * 
   */
  public @Nullable Integer getWithdrawalId(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("getWithdrawalId()");

    Integer withdrawalId = null;

    OpenTransactionPayloadDTO openTransactionPayloadDTO = openTransactionDTO.getPayload();

    if (null != openTransactionPayloadDTO) {
      withdrawalId = openTransactionPayloadDTO.getId();
    }

    return withdrawalId;
  }
}
