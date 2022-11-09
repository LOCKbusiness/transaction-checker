package ch.dfx.manager.checker.withdrawal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionRawTxDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.ManagerUtils;

/**
 * 
 */
public class SignMessageFormatChecker {
  private static final Logger LOGGER = LogManager.getLogger(SignMessageFormatChecker.class);

  // ...
  private static final String SIGN_MESSAGE_FORMAT = "Withdraw_${amount}_${asset}_from_${address}_staking_id_${stakingId}_withdrawal_id_${withdrawalId}";

  private static final Pattern SIGN_MESSAGE_PATTERN =
      Pattern.compile("^Withdraw_(\\d+\\.?\\d*)_(.+)_from_(.+)_staking_id_(\\d+)_withdrawal_id_(\\d+)$", Pattern.DOTALL);
  private static final Matcher SIGN_MESSAGE_MATCHER = SIGN_MESSAGE_PATTERN.matcher("");

  // ...
  private final DefiDataProvider dataProvider;

  /**
   * 
   */
  public SignMessageFormatChecker(@Nonnull DefiDataProvider dataProvider) {
    this.dataProvider = dataProvider;
  }

  /**
   * 
   */
  public TransactionWithdrawalDTOList checkSignMessageFormat(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkSignMessageFormat() ...");

    TransactionWithdrawalDTOList checkedTransactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    for (TransactionWithdrawalDTO transactionWithdrawalDTO : transactionWithdrawalDTOList) {
      if (checkSignMessageFormat(transactionWithdrawalDTO)) {
        checkedTransactionWithdrawalDTOList.add(transactionWithdrawalDTO);
      }
    }

    return checkedTransactionWithdrawalDTOList;
  }

  /**
   * 
   */
  private boolean checkSignMessageFormat(@Nonnull TransactionWithdrawalDTO transactionWithdrawalDTO) {
    LOGGER.trace("checkSignMessageFormat() ...");

    // ...
    try {
      OpenTransactionDTO openTransactionDTO = transactionWithdrawalDTO.getOpenTransactionDTO();

      OpenTransactionRawTxDTO openTransactionRawTxDTO = openTransactionDTO.getRawTx();
      String hex = openTransactionRawTxDTO.getHex();

      // ...
      DefiTransactionData transactionData = dataProvider.decodeRawTransaction(hex);
//    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

      // ...
      String openTransactionId = openTransactionDTO.getId();
      String rawTransactionId = transactionData.getTxid();

      if (!Objects.equals(openTransactionId, rawTransactionId)) {
        ManagerUtils.setCheckSignatureMessage(transactionWithdrawalDTO, "transaction id not matches");
        return false;
      }

      // ...
      PendingWithdrawalDTO pendingWithdrawalDTO = transactionWithdrawalDTO.getPendingWithdrawalDTO();

      String signMessage = pendingWithdrawalDTO.getSignMessage();

      SIGN_MESSAGE_MATCHER.reset(signMessage);

      if (!SIGN_MESSAGE_MATCHER.matches()) {
        ManagerUtils.setCheckSignatureMessage(transactionWithdrawalDTO, "unknown sign message format");
        return false;
      }

      String signMessageAmount = SIGN_MESSAGE_MATCHER.group(1);
      String signMessageAsset = SIGN_MESSAGE_MATCHER.group(2);
      String signMessageAddress = SIGN_MESSAGE_MATCHER.group(3);
      String signMessageStakingId = SIGN_MESSAGE_MATCHER.group(4);

      // ...
      BigDecimal transactionOutAmount = getTransactionOutAmount(transactionData, signMessageAddress);

      boolean isSignMessageAmountValid =
          checkSignMessageAmount(new BigDecimal(signMessageAmount), pendingWithdrawalDTO.getAmount(), transactionOutAmount);

      if (!isSignMessageAmountValid) {
        ManagerUtils.setCheckSignatureMessage(transactionWithdrawalDTO, "invalid amount");
        return false;
      }

      // ...
      String checkSignMessage = SIGN_MESSAGE_FORMAT;

      checkSignMessage = checkSignMessage.replace("${amount}", signMessageAmount);
      checkSignMessage = checkSignMessage.replace("${asset}", signMessageAsset);
      checkSignMessage = checkSignMessage.replace("${address}", signMessageAddress);
      checkSignMessage = checkSignMessage.replace("${stakingId}", signMessageStakingId);
      checkSignMessage = checkSignMessage.replace("${withdrawalId}", transactionWithdrawalDTO.getId().toString());

      LOGGER.debug(signMessage);
      LOGGER.debug(checkSignMessage);

      if (!signMessage.equals(checkSignMessage)) {
        ManagerUtils.setCheckSignatureMessage(transactionWithdrawalDTO, "sign message mismatch");
        return false;
      }

      transactionWithdrawalDTO.setCustomerAddress(signMessageAddress);
      transactionWithdrawalDTO.setState(TransactionWithdrawalStateEnum.SIGN_MESSAGE_FORMAT_CHECKED);

      return true;
    } catch (Exception e) {
      LOGGER.error("checkSignMessageFormat", e);
      ManagerUtils.setCheckSignatureMessage(transactionWithdrawalDTO, e.getMessage());
      return false;
    }
  }

  /**
   * 
   */
  private boolean checkSignMessageAmount(
      @Nonnull BigDecimal signMessageAmount,
      @Nonnull BigDecimal pendingWithdrawalAmount,
      @Nonnull BigDecimal transactionOutAmount) {
    LOGGER.trace("checkSignMessageAmount() ...");

    boolean isValid = false;

    if (signMessageAmount.equals(pendingWithdrawalAmount)) {
      if (signMessageAmount.setScale(8, RoundingMode.DOWN).equals(transactionOutAmount.setScale(8, RoundingMode.DOWN))) {
        isValid = true;
      }
    }

    return isValid;
  }

  /**
   * 
   */
  private BigDecimal getTransactionOutAmount(
      @Nonnull DefiTransactionData transactionData,
      @Nonnull String address) {
    LOGGER.trace("getTransactionOutAmount() ...");

    BigDecimal outAmount = BigDecimal.ZERO;

    List<DefiTransactionVoutData> transactionVoutDataList = transactionData.getVout();

    for (DefiTransactionVoutData transactionVoutData : transactionVoutDataList) {
      DefiTransactionScriptPubKeyData transactionScriptPubKeyData = transactionVoutData.getScriptPubKey();

      if (null != transactionScriptPubKeyData) {
        List<String> addressList = transactionScriptPubKeyData.getAddresses();

        if (addressList.contains(address)) {
          outAmount = outAmount.add(transactionVoutData.getValue());
        }
      }
    }

    return outAmount;
  }
}