package ch.dfx.api;

import javax.annotation.Nonnull;

import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;
import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public interface ApiAccessHandler {

  /**
   * 
   */
  void fakeForTest();

  /**
   * 
   */
  void resetSignIn();

  /**
   * 
   */
  void signIn() throws DfxException;

  /**
   *
   */
  OpenTransactionDTOList getOpenTransactionDTOList() throws DfxException;

  /**
   * 
   */
  PendingWithdrawalDTOList getPendingWithdrawalDTOList() throws DfxException;

  /**
   * 
   */
  void sendOpenTransactionVerified(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionVerifiedDTO openTransactionVerifiedDTO) throws DfxException;

  /**
   * 
   */
  void sendOpenTransactionInvalidated(
      @Nonnull String openTransactionId,
      @Nonnull OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO) throws DfxException;

}