package ch.dfx.manager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionPayloadDTO;
import ch.dfx.api.data.transaction.OpenTransactionRawTxDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.data.transaction.DefiTransactionData;
import ch.dfx.defichain.data.transaction.DefiTransactionScriptPubKeyData;
import ch.dfx.defichain.data.transaction.DefiTransactionVoutData;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.message.MessageHandler;

/**
 * 
 */
public class OpenTransactionManagerNew {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManagerNew.class);

  // ...
  private static final String SIGN_MESSAGE_FORMAT = "Withdraw_${amount}_${asset}_from_${address}_staking_id_${stakingId}_withdrawal_id_${withdrawalId}";

  private static final Pattern SIGN_MESSAGE_PATTERN =
      Pattern.compile("^Withdraw_(\\d+\\.?\\d*)_(.+)_from_(.+)_staking_id_(\\d+)_withdrawal_id_(\\d+)$", Pattern.DOTALL);
  private static final Matcher SIGN_MESSAGE_MATCHER = SIGN_MESSAGE_PATTERN.matcher("");

  // ...
  private final DefiDataProvider dataProvider;

  private final MessageHandler messageHandler;
  private final ApiAccessHandler apiAccessHandler;

  /**
   * 
   */
  public OpenTransactionManagerNew() {
    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    this.messageHandler = new MessageHandler(dataProvider);
    this.apiAccessHandler = new ApiAccessHandler();
  }

  /**
   * 
   */
  public void execute() throws DfxException {
    LOGGER.trace("execute() ...");

    // TODO: ONLY FOR TESTING PURPOSES! ...
    apiAccessHandler.fakeForTest();
    // apiAccessHandler.signIn();

    // ...
    PendingWithdrawalDTOList pendingWithdrawalDTOList = apiAccessHandler.getPendingWithdrawalDTOList();
    OpenTransactionDTOList openTransactionDTOList = apiAccessHandler.getOpenTransactionDTOList();

    // ...
    LOGGER.debug("Pending Withdrawal size: " + pendingWithdrawalDTOList.size());
    LOGGER.debug("Open Transaction size:   " + openTransactionDTOList.size());
    LOGGER.debug("");

    // ...
    Map<Integer, OpenTransactionDTO> idToOpenTransactionDTOMap = new HashMap<>();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      OpenTransactionPayloadDTO openTransactionPayloadDTO = openTransactionDTO.getPayload();

      if (null != openTransactionPayloadDTO) {
        Integer openTransactionPayloadId = openTransactionPayloadDTO.getId();

        if (null != openTransactionPayloadId) {
          idToOpenTransactionDTOMap.put(openTransactionPayloadId, openTransactionDTO);
        }
      }
    }

    // ...
    for (PendingWithdrawalDTO pendingWithdrawalDTO : pendingWithdrawalDTOList) {
      Integer pendingWithdrawalId = pendingWithdrawalDTO.getId();
      String signMessage = pendingWithdrawalDTO.getSignMessage();
      String signature = pendingWithdrawalDTO.getSignature();
      BigDecimal amount = pendingWithdrawalDTO.getAmount();

      OpenTransactionDTO openTransactionDTO = idToOpenTransactionDTOMap.get(pendingWithdrawalId);

      if (null != openTransactionDTO) {
        checkSignature(pendingWithdrawalDTO, openTransactionDTO);
      }
    }

    System.out.println();

//    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
//      String openTransactionHex = openTransactionDTO.getRawTx().getHex();
//      analyzeTransaction(openTransactionHex);
//
//      break;
//    }

    // ...
    // ...
    // ...
//    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
//      boolean isValid = signatureCheck(openTransactionDTO);
//
//      String openTransactionHex = openTransactionDTO.getRawTx().getHex();
//      String openTransactionCheckerSignature = messageHandler.signMessage(openTransactionHex);
//
//      if (isValid) {
//        OpenTransactionVerifiedDTO openTransactionVerifiedDTO = new OpenTransactionVerifiedDTO();
//        openTransactionVerifiedDTO.setSignature(openTransactionCheckerSignature);
//
//        apiAccessHandler.sendOpenTransactionVerified(openTransactionDTO.getId(), openTransactionVerifiedDTO);
//      } else {
//        OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO = new OpenTransactionInvalidatedDTO();
//        openTransactionInvalidatedDTO.setSignature(openTransactionCheckerSignature);
//        openTransactionInvalidatedDTO.setReason(InvalidReasonEnum.INVALID_ISSUER_SIGNATURE.getReason());
//
//        apiAccessHandler.sendOpenTransactionInvalidated(openTransactionDTO.getId(), openTransactionInvalidatedDTO);
//      }
//    }

    // ...
    // ...
    // ...
//    List<OpenTransactionMasternodeDTO> openTransactionMasternodeDTOList =
//        openTransactionHandler.getTransactionMasternodeDTOList();
//
//    if (!openTransactionMasternodeDTOList.isEmpty()) {
//      MasternodeBuilder masternodeBuilder = new MasternodeBuilder();
//      masternodeBuilder.build(openTransactionMasternodeDTOList);
//    }
//
//    // ...
//    openTransactionDTOList.stream()
//        .filter(dto -> OpenTransactionStateEnum.WORK == dto.getState())
//        .forEach(dto -> dto.setState(OpenTransactionStateEnum.PROCESSED));
//
//    openTransactionHandler.writeOpenTransactionDTOList(openTransactionDTOList);
  }

  /**
   * 
   */
  private void checkSignature(
      @Nonnull PendingWithdrawalDTO pendingWithdrawalDTO,
      @Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    // ...
    Integer pendingWithdrawalId = pendingWithdrawalDTO.getId();

    OpenTransactionRawTxDTO openTransactionRawTxDTO = openTransactionDTO.getRawTx();

    if (null == openTransactionRawTxDTO) {
      throw new DfxException("ID: " + pendingWithdrawalId + " - no raw transaction found");
    }

    // ...
    String hex = openTransactionRawTxDTO.getHex();

    if (StringUtils.isEmpty(hex)) {
      throw new DfxException("ID: " + pendingWithdrawalId + " - no raw transaction found");
    }

    // ...
    DefiTransactionData transactionData = dataProvider.decodeRawTransaction(hex);
//    DefiCustomData customData = dataProvider.decodeCustomTransaction(hex);

    // ...
    String openTransactionId = openTransactionDTO.getId();
    String rawTransactionId = transactionData.getTxid();

    if (!Objects.equals(openTransactionId, rawTransactionId)) {
      throw new DfxException("ID: " + pendingWithdrawalId + " - transaction id not matches");
    }

    // ...
    String signMessage = pendingWithdrawalDTO.getSignMessage();

    SIGN_MESSAGE_MATCHER.reset(signMessage);

    if (!SIGN_MESSAGE_MATCHER.matches()) {
      throw new DfxException("ID: " + pendingWithdrawalId + " - unknown sign message format");
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
      throw new DfxException("ID: " + pendingWithdrawalId + " - invalid amount");
    }

    // ...
    String checkSignMessage = SIGN_MESSAGE_FORMAT;

    checkSignMessage = checkSignMessage.replace("${amount}", signMessageAmount);
    checkSignMessage = checkSignMessage.replace("${asset}", signMessageAsset);
    checkSignMessage = checkSignMessage.replace("${address}", signMessageAddress);
    checkSignMessage = checkSignMessage.replace("${stakingId}", signMessageStakingId);
    checkSignMessage = checkSignMessage.replace("${withdrawalId}", pendingWithdrawalId.toString());

    LOGGER.debug(signMessage);
    LOGGER.debug(checkSignMessage);

    if (!signMessage.equals(checkSignMessage)) {
      throw new DfxException("ID: " + pendingWithdrawalId + " - sign message mismatch");
    }

    Boolean signMessageValid = messageHandler.verifyMessage(signMessageAddress, pendingWithdrawalDTO.getSignature(), checkSignMessage);

    if (!BooleanUtils.isTrue(signMessageValid)) {
      throw new DfxException("ID: " + pendingWithdrawalId + " - sign message not verified");
    }
  }

  /**
   * 
   */
  private boolean checkSignMessageAmount(
      @Nonnull BigDecimal signMessageAmount,
      @Nonnull BigDecimal pendingWithdrawalAmount,
      @Nonnull BigDecimal transactionOutAmount) {
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

  /**
   * 
   */
  private boolean signatureCheck(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("signatureCheck() ...");

    boolean isValid = validateIssuerSignature(openTransactionDTO);

    if (isValid) {
      isValid = validateWithdrawSigner(openTransactionDTO);
    }

    return isValid;
  }

  /**
   * 
   */
  private boolean validateIssuerSignature(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("validateIssurerSignature() ...");

    String openTransactionIssuerSignature = openTransactionDTO.getIssuerSignature();
    String openTransactionHex = openTransactionDTO.getRawTx().getHex();

    String verifyAddress = ConfigPropertyProvider.getInstance().getProperty(PropertyEnum.DFI_VERIFY_ADDRESS);

    Boolean isValid = messageHandler.verifyMessage(verifyAddress, openTransactionIssuerSignature, openTransactionHex);
    LOGGER.debug("Open Transaction Id: " + openTransactionDTO.getId() + " / " + isValid);

    return BooleanUtils.isTrue(isValid);
  }

  /**
   * 
   */
  private boolean validateWithdrawSigner(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("validateWithdrawSigner() ...");

    // String withdrawMessage = openTransactionDTO.getWithdrawMessage();

    return true;
  }
}
