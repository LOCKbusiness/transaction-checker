package ch.dfx.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.join.WithdrawalTransactionDTO;
import ch.dfx.api.data.join.WithdrawalTransactionDTOList;
import ch.dfx.api.data.join.WithdrawalTransactionStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionPayloadDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;
import ch.dfx.api.enumeration.InvalidReasonEnum;
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.manager.data.SignedMessageCheckDTO;
import ch.dfx.manager.data.SignedMessageCheckDTOList;
import ch.dfx.message.MessageHandler;

/**
 * 
 */
public class OpenTransactionManager {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManager.class);

  private final DefiDataProvider dataProvider;

  private final MessageHandler messageHandler;
  private final ApiAccessHandler apiAccessHandler;

  private final SignedMessageChecker signedMessageChecker;

  /**
   * 
   */
  public OpenTransactionManager() {
    this.dataProvider = TransactionCheckerUtils.createDefiDataProvider();

    this.messageHandler = new MessageHandler(dataProvider);
    this.apiAccessHandler = new ApiAccessHandler();

    this.signedMessageChecker = new SignedMessageChecker();
  }

  /**
   * 
   */
  public void execute() throws DfxException {
    LOGGER.trace("execute() ...");

    // ...
    // TODO: ONLY FOR TESTING PURPOSES! ...
    // apiAccessHandler.fakeForTest();
    apiAccessHandler.signIn();

    // ...
    PendingWithdrawalDTOList apiPendingWithdrawalDTOList = apiAccessHandler.getPendingWithdrawalDTOList();
    OpenTransactionDTOList apiOpenTransactionDTOList = apiAccessHandler.getOpenTransactionDTOList();

    // ...
    LOGGER.debug("Pending Withdrawal size: " + apiPendingWithdrawalDTOList.size());
    LOGGER.debug("Open Transaction size:   " + apiOpenTransactionDTOList.size());
    LOGGER.debug("");

    OpenTransactionDTOList workOpenTransactionDTOList = checkTransactionSignature(apiOpenTransactionDTOList);

    // TODO: Onyl shortcut, until further implementation is tested and ready ...
    justSendVerified(workOpenTransactionDTOList);

    // ...
//    WithdrawalTransactionDTOList withdrawalTransactionDTOList = createWithdrawalTransactionDTOList(apiPendingWithdrawalDTOList, workOpenTransactionDTOList);
//
//    if (!withdrawalTransactionDTOList.isEmpty()) {
//      processPendingWithdrawal(withdrawalTransactionDTOList);
//    }
//
//    // ...
//    send(withdrawalTransactionDTOList);
//
//    // ...
//    Map<String, OpenTransactionDTO> transactionIdToOpenTransactionMap = new HashMap<>();
//    workOpenTransactionDTOList.forEach(dto -> transactionIdToOpenTransactionMap.put(dto.getId(), dto));
//    withdrawalTransactionDTOList.forEach(dto -> transactionIdToOpenTransactionMap.remove(dto.getOpenTransactionDTO().getId()));
//
//    // TODO: There must be no open transactions anymore available at this point, if implementation is finished ...
//    OpenTransactionDTOList openTransactionDTOList =
//        new OpenTransactionDTOList(transactionIdToOpenTransactionMap.values());
//
//    justSendVerified(openTransactionDTOList);
  }

  /**
   * 
   */
  private OpenTransactionDTOList checkTransactionSignature(@Nonnull OpenTransactionDTOList uncheckedOpenTransactionDTOList) {
    LOGGER.trace("checkTransactionSignature() ...");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : uncheckedOpenTransactionDTOList) {
      if (doCheckTransactionSignature(openTransactionDTO)) {
        checkedOpenTransactionDTOList.add(openTransactionDTO);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doCheckTransactionSignature(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("doCheckTransactionSignature() ...");

    try {
      boolean isValid = validateIssuerSignature(openTransactionDTO);

      if (!isValid) {
        sendInvalidated(openTransactionDTO);
      }

      return isValid;
    } catch (Exception e) {
      LOGGER.error("doCheckTransactionSignature", e);
      return false;
    }
  }

  /**
   * 
   */
  private void sendVerified(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
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
  private void sendInvalidated(@Nonnull OpenTransactionDTO openTransactionDTO) throws DfxException {
    LOGGER.trace("sendInvalidated() ...");

    String openTransactionHex = openTransactionDTO.getRawTx().getHex();
    String openTransactionCheckerSignature = messageHandler.signMessage(openTransactionHex);

    OpenTransactionInvalidatedDTO openTransactionInvalidatedDTO = new OpenTransactionInvalidatedDTO();
    openTransactionInvalidatedDTO.setSignature(openTransactionCheckerSignature);
    openTransactionInvalidatedDTO.setReason(InvalidReasonEnum.INVALID_ISSUER_SIGNATURE.getReason());

    apiAccessHandler.sendOpenTransactionInvalidated(openTransactionDTO.getId(), openTransactionInvalidatedDTO);
  }

  /**
   * List of all Withdrawals and the corresponding Transactions ...
   */
  private WithdrawalTransactionDTOList createWithdrawalTransactionDTOList(
      @Nonnull PendingWithdrawalDTOList pendingWithdrawalDTOList,
      @Nonnull OpenTransactionDTOList openTransactionDTOList) {
    WithdrawalTransactionDTOList withdrawalTransactionDTOList = new WithdrawalTransactionDTOList();

    Map<Integer, PendingWithdrawalDTO> idToPendingWithdrawalDTOMap = createIdToPendingWithdrawalDTOMap(pendingWithdrawalDTOList);
    Map<Integer, OpenTransactionDTO> idToOpenTransactionDTOMap = createIdToOpenTransactionDTOMap(openTransactionDTOList);

    // ...
    for (Entry<Integer, PendingWithdrawalDTO> idToPendingWithdrawalDTOMapEntry : idToPendingWithdrawalDTOMap.entrySet()) {
      Integer id = idToPendingWithdrawalDTOMapEntry.getKey();

      OpenTransactionDTO openTransactionDTO = idToOpenTransactionDTOMap.get(id);

      if (null != openTransactionDTO) {
        WithdrawalTransactionDTO withdrawalTransactionDTO = new WithdrawalTransactionDTO();

        withdrawalTransactionDTO.setId(id);
        withdrawalTransactionDTO.setPendingWithdrawalDTO(idToPendingWithdrawalDTOMapEntry.getValue());
        withdrawalTransactionDTO.setOpenTransactionDTO(openTransactionDTO);

        withdrawalTransactionDTOList.add(withdrawalTransactionDTO);
      }
    }

    return withdrawalTransactionDTOList;
  }

  /**
   * 
   */
  private Map<Integer, PendingWithdrawalDTO> createIdToPendingWithdrawalDTOMap(@Nonnull PendingWithdrawalDTOList pendingWithdrawalDTOList) {
    LOGGER.trace("createIdToPendingWithdrawalDTOMap() ...");

    Map<Integer, PendingWithdrawalDTO> idToPendingWithdrawalDTOMap = new HashMap<>();

    for (PendingWithdrawalDTO pendingWithdrawalDTO : pendingWithdrawalDTOList) {
      Integer pendingWithdrawalId = pendingWithdrawalDTO.getId();

      if (null != pendingWithdrawalId) {
        idToPendingWithdrawalDTOMap.put(pendingWithdrawalId, pendingWithdrawalDTO);
      }
    }

    return idToPendingWithdrawalDTOMap;
  }

  /**
   * 
   */
  private Map<Integer, OpenTransactionDTO> createIdToOpenTransactionDTOMap(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("createIdToOpenTransactionDTOMap() ...");

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

    return idToOpenTransactionDTOMap;
  }

  /**
   * 
   */
  private void processPendingWithdrawal(@Nonnull WithdrawalTransactionDTOList withdrawalTransactionDTOList) {
    LOGGER.trace("processPendingWithdrawal() ...");

    checkWithdrawalMessage(withdrawalTransactionDTOList);

//   checkWithdrawalBalance(checkedWithdrawalTransactionDTOList);
  }

  /**
   * 
   */
  private WithdrawalTransactionDTOList checkWithdrawalMessage(@Nonnull WithdrawalTransactionDTOList withdrawalTransactionDTOList) {
    LOGGER.trace("checkWithdrawalMessage() ...");

    WithdrawalTransactionDTOList checkedWithdrawalTransactionDTOList = new WithdrawalTransactionDTOList();

    // Check 1: Check the sign message format ...
    checkSignMessageFormat(withdrawalTransactionDTOList);

    // Check 2: Check the message signature ...
    SignedMessageCheckDTOList uncheckedSignedMessageCheckDTOList = createSignedMessageCheckDTOList(withdrawalTransactionDTOList);
    SignedMessageCheckDTOList checkedSignedMessageCheckDTOList = signedMessageChecker.checkSignature(uncheckedSignedMessageCheckDTOList);

    // ...
    Map<Integer, WithdrawalTransactionDTO> idToWithdrawalTransactionDTOMap = new HashMap<>();
    withdrawalTransactionDTOList.forEach(dto -> idToWithdrawalTransactionDTOMap.put(dto.getId(), dto));

    Map<Integer, SignedMessageCheckDTO> idToCheckedSignedMessageCheckDTOMap = new HashMap<>();
    checkedSignedMessageCheckDTOList.forEach(dto -> idToCheckedSignedMessageCheckDTOMap.put(dto.getId(), dto));

    // ...
    for (Entry<Integer, WithdrawalTransactionDTO> idToWithdrawalTransactionDTOMapEntry : idToWithdrawalTransactionDTOMap.entrySet()) {
      Integer id = idToWithdrawalTransactionDTOMapEntry.getKey();
      WithdrawalTransactionDTO withdrawalTransactionDTO = idToWithdrawalTransactionDTOMapEntry.getValue();

      SignedMessageCheckDTO checkedSignedMessageCheckDTO = idToCheckedSignedMessageCheckDTOMap.get(id);

      if (null != checkedSignedMessageCheckDTO
          && checkedSignedMessageCheckDTO.isValid()) {
        withdrawalTransactionDTO.setState(WithdrawalTransactionStateEnum.SIGNATURE_CHECKED);
        checkedWithdrawalTransactionDTOList.add(withdrawalTransactionDTO);
      } else {
        withdrawalTransactionDTO.setCheckMessage("[processPendingWithdrawal] ID: " + id + " - invalid signature");
        withdrawalTransactionDTO.setState(WithdrawalTransactionStateEnum.INVALID);
      }
    }

    return checkedWithdrawalTransactionDTOList;
  }

  /**
   * 
   */
  private void checkSignMessageFormat(@Nonnull WithdrawalTransactionDTOList withdrawalTransactionDTOList) {
    LOGGER.trace("checkSignMessageFormat() ...");

    for (WithdrawalTransactionDTO withdrawalTransactionDTO : withdrawalTransactionDTOList) {
      signedMessageChecker.checkSignMessageFormat(withdrawalTransactionDTO);
    }
  }

  /**
   * 
   */
  private SignedMessageCheckDTOList createSignedMessageCheckDTOList(@Nonnull WithdrawalTransactionDTOList withdrawalTransactionDTOList) {
    LOGGER.trace("createSignedMessageCheckDTOList() ...");

    SignedMessageCheckDTOList signedMessageCheckDTOList = new SignedMessageCheckDTOList();

    for (WithdrawalTransactionDTO withdrawalTransactionDTO : withdrawalTransactionDTOList) {
      if (WithdrawalTransactionStateEnum.SIGN_MESSAGE_FORMAT_CHECKED == withdrawalTransactionDTO.getState()) {
        PendingWithdrawalDTO pendingWithdrawalDTO = withdrawalTransactionDTO.getPendingWithdrawalDTO();

        SignedMessageCheckDTO signedMessageCheckDTO = new SignedMessageCheckDTO();

        signedMessageCheckDTO.setId(pendingWithdrawalDTO.getId());
        signedMessageCheckDTO.setMessage(pendingWithdrawalDTO.getSignMessage());
        signedMessageCheckDTO.setAddress(withdrawalTransactionDTO.getAddress());
        signedMessageCheckDTO.setSignature(pendingWithdrawalDTO.getSignature());

        signedMessageCheckDTOList.add(signedMessageCheckDTO);
      }
    }

    return signedMessageCheckDTOList;
  }

  /**
   * 
   */
  private void send(@Nonnull WithdrawalTransactionDTOList withdrawalTransactionDTOList) throws DfxException {
    LOGGER.trace("send() ...");

    for (WithdrawalTransactionDTO withdrawalTransactionDTO : withdrawalTransactionDTOList) {
      OpenTransactionDTO openTransactionDTO = withdrawalTransactionDTO.getOpenTransactionDTO();

      // TODO: End state must be: VALID ...
      if (WithdrawalTransactionStateEnum.SIGNATURE_CHECKED == withdrawalTransactionDTO.getState()) {
        sendVerified(openTransactionDTO);
      } else {
        sendInvalidated(openTransactionDTO);
      }
    }
  }

  /**
   * 
   */
  private void justSendVerified(@Nonnull OpenTransactionDTOList openTransactionDTOList) throws DfxException {
    LOGGER.trace("justSignByMyself() ...");

    // ...
    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      sendVerified(openTransactionDTO);
    }
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
}
