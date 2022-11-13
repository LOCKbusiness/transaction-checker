package ch.dfx.manager;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.common.enumeration.NetworkEnum;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.checker.withdrawal.SignMessageFormatChecker;
import ch.dfx.manager.checker.withdrawal.SignMessageSignatureChecker;
import ch.dfx.manager.checker.withdrawal.StakingBalanceChecker;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class WithdrawalManager {
  private static final Logger LOGGER = LogManager.getLogger(WithdrawalManager.class);

  // ...
  private final SignMessageFormatChecker signMessageFormatChecker;
  private final SignMessageSignatureChecker signMessageSignatureChecker;
  private final StakingBalanceChecker stakingBalanceChecker;

  /**
   * 
   */
  public WithdrawalManager(
      @Nonnull NetworkEnum network,
      @Nonnull H2DBManager databaseManager,
      @Nonnull DefiDataProvider dataProvider) {
    Objects.requireNonNull(network, "null 'network' not allowed");

    this.signMessageFormatChecker = new SignMessageFormatChecker(dataProvider);
    this.signMessageSignatureChecker = new SignMessageSignatureChecker(network);
    this.stakingBalanceChecker = new StakingBalanceChecker(databaseManager);
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkSignMessageFormat(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkSignMessageFormat()");

    return signMessageFormatChecker.checkSignMessageFormat(transactionWithdrawalDTOList);
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkSignMessageSignature(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkSignMessageSignature()");

    return signMessageSignatureChecker.checkSignMessageSignature(transactionWithdrawalDTOList);

  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkStakingBalance(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkStakingBalance()");

//    return stakingBalanceChecker.checkStakingBalance(transactionWithdrawalDTOList);
    return stakingBalanceChecker.checkStakingBalanceNew(transactionWithdrawalDTOList);
  }
}
