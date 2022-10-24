package ch.dfx.api;

import javax.annotation.Nonnull;

import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;

/**
 * 
 */
public interface ApiAccessHandler {

  /**
   * 
   */
  void resetSignIn();

  /**
   * 
   */
  void signIn();

  /**
   *
   */
  OpenTransactionDTOList getOpenTransactionDTOList();

  /**
   * 
   */
  PendingWithdrawalDTOList getPendingWithdrawalDTOList();

  /**
   * 
   */
  void sendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionVerifiedDTO openTransactionVerifiedDTO);

  /**
   * 
   */
  void sendOpenTransactionInvalidated(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO);

}