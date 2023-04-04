package ch.dfx.manager.filler;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.TransactionCheckerUtils;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.common.enumeration.TokenEnum;

/**
 * 
 */
public class PendingWithdrawalDTOFiller {
  private static final Logger LOGGER = LogManager.getLogger(PendingWithdrawalDTOFiller.class);

  /**
   * 
   */
  public PendingWithdrawalDTOFiller() {
  }

  /**
   * Set all used data fields to empty values or default values,
   * if they are not set via the API ...
   */
  public void fillEmptyData(@Nonnull PendingWithdrawalDTO pendingWithdrawalDTO) {
    LOGGER.trace("fillEmptyData()");

    // ID ...
    pendingWithdrawalDTO.setId(TransactionCheckerUtils.zeroIfNull(pendingWithdrawalDTO.getId()));

    // Sign Message ...
    pendingWithdrawalDTO.setSignMessage(TransactionCheckerUtils.emptyIfNull(pendingWithdrawalDTO.getSignMessage()));

    // Signature ...
    pendingWithdrawalDTO.setSignature(TransactionCheckerUtils.emptyIfNull(pendingWithdrawalDTO.getSignature()));

    // Amount ...
    pendingWithdrawalDTO.setAmount(TransactionCheckerUtils.zeroIfNull(pendingWithdrawalDTO.getAmount()));

    // Token ...
    pendingWithdrawalDTO.setToken(TokenEnum.createWithText(pendingWithdrawalDTO.getAsset(), TokenEnum.DFI));
  }
}
