package ch.dfx.manager;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;

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
    LOGGER.trace("setWithdrawalCheckInvalidReason() ...");

    String message = "[Withdrawal] ID: " + transactionWithdrawalDTO.getId() + " - " + messageInfo;

    transactionWithdrawalDTO.setStateReason(message);
    transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.INVALID);
  }

  /**
   * 
   */
  private ManagerUtils() {
  }
}
