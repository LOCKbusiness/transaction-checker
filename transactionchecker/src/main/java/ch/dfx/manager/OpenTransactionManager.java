package ch.dfx.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.api.ApiAccessHandler;
import ch.dfx.api.data.join.TransactionWithdrawalDTO;
import ch.dfx.api.data.join.TransactionWithdrawalDTOList;
import ch.dfx.api.data.join.TransactionWithdrawalStateEnum;
import ch.dfx.api.data.transaction.OpenTransactionDTO;
import ch.dfx.api.data.transaction.OpenTransactionDTOList;
import ch.dfx.api.data.transaction.OpenTransactionInvalidatedDTO;
import ch.dfx.api.data.transaction.OpenTransactionPayloadDTO;
import ch.dfx.api.data.transaction.OpenTransactionRawTxDTO;
import ch.dfx.api.data.transaction.OpenTransactionVerifiedDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTO;
import ch.dfx.api.data.withdrawal.PendingWithdrawalDTOList;
import ch.dfx.common.TransactionCheckerUtils;
import ch.dfx.common.enumeration.PropertyEnum;
import ch.dfx.common.errorhandling.DfxException;
import ch.dfx.common.provider.ConfigPropertyProvider;
import ch.dfx.defichain.provider.DefiDataProvider;
import ch.dfx.message.MessageHandler;
import ch.dfx.transactionserver.database.DatabaseUtils;
import ch.dfx.transactionserver.database.H2DBManager;

/**
 * 
 */
public class OpenTransactionManager {
  private static final Logger LOGGER = LogManager.getLogger(OpenTransactionManager.class);

  // ...
  private PreparedStatement apiDuplicateCheckInsertStatement = null;

  // ...
  private final ApiAccessHandler apiAccessHandler;

  private final MessageHandler messageHandler;
  private final WithdrawalManager withdrawalManager;

  /**
   * 
   */
  public OpenTransactionManager(
      @Nonnull ApiAccessHandler apiAccessHandler,
      @Nonnull DefiDataProvider dataProvider) {
    this.apiAccessHandler = apiAccessHandler;

    this.messageHandler = new MessageHandler(dataProvider);
    this.withdrawalManager = new WithdrawalManager(dataProvider);
  }

  /**
   * 
   */
  public void execute() throws DfxException {
    LOGGER.trace("execute() ...");

    // ...
    OpenTransactionDTOList apiOpenTransactionDTOList = apiAccessHandler.getOpenTransactionDTOList();
    PendingWithdrawalDTOList apiPendingWithdrawalDTOList = apiAccessHandler.getPendingWithdrawalDTOList();

    // ...
    LOGGER.debug(
        "Transaction / Withdrawal Size: "
            + apiOpenTransactionDTOList.size() + " / " + apiPendingWithdrawalDTOList.size());

    // ...
    fillEmptyDataOfReceivedDTO(apiOpenTransactionDTOList, apiPendingWithdrawalDTOList);

    // ...
    OpenTransactionDTOList workOpenTransactionDTOList = processOpenTransaction(apiOpenTransactionDTOList);

    // ...
    TransactionWithdrawalDTOList transactionWithdrawalDTOList =
        createTransactionWithdrawalDTOList(workOpenTransactionDTOList, apiPendingWithdrawalDTOList);

    if (!transactionWithdrawalDTOList.isEmpty()) {
      processPendingWithdrawal(transactionWithdrawalDTOList);
      send(transactionWithdrawalDTOList);
    }

    // ...
    Map<String, OpenTransactionDTO> transactionIdToOpenTransactionMap = new HashMap<>();
    workOpenTransactionDTOList.forEach(dto -> transactionIdToOpenTransactionMap.put(dto.getId(), dto));
    transactionWithdrawalDTOList.forEach(dto -> transactionIdToOpenTransactionMap.remove(dto.getOpenTransactionDTO().getId()));

    // TODO: There must be no open transactions anymore available at this point,
    // TODO: if implementation is finished ...
    OpenTransactionDTOList openTransactionDTOList =
        new OpenTransactionDTOList(transactionIdToOpenTransactionMap.values());

    // TODO: Onyl shortcut,
    // TODO: until further implementation is tested and ready ...
    justSendVerified(openTransactionDTOList);
  }

  /**
   * OpenTransactionDTO / PendingWithdrawalDTO:
   * 
   * Set all used data fields to empty values or default values,
   * if they are not set via the API ...
   */
  private void fillEmptyDataOfReceivedDTO(
      @Nonnull OpenTransactionDTOList apiOpenTransactionDTOList,
      @Nonnull PendingWithdrawalDTOList apiPendingWithdrawalDTOList) {

    // Transaction ...
    for (OpenTransactionDTO openTransactionDTO : apiOpenTransactionDTOList) {
      // ID ...
      openTransactionDTO.setId(TransactionCheckerUtils.emptyIfNull(openTransactionDTO.getId()));

      // Signature ...
      openTransactionDTO.setIssuerSignature(TransactionCheckerUtils.emptyIfNull(openTransactionDTO.getIssuerSignature()));

      // Raw Transaction ...
      OpenTransactionRawTxDTO openTransactionRawTxDTO = openTransactionDTO.getRawTx();

      if (null == openTransactionRawTxDTO) {
        openTransactionRawTxDTO = new OpenTransactionRawTxDTO();
        openTransactionDTO.setRawTx(openTransactionRawTxDTO);
      }

      openTransactionRawTxDTO.setHex(TransactionCheckerUtils.emptyIfNull(openTransactionRawTxDTO.getHex()));
    }

    // Withdrawal ...
    for (PendingWithdrawalDTO pendingWithdrawalDTO : apiPendingWithdrawalDTOList) {
      // ID ...
      pendingWithdrawalDTO.setId(TransactionCheckerUtils.zeroIfNull(pendingWithdrawalDTO.getId()));

      // Sign Message ...
      pendingWithdrawalDTO.setSignMessage(TransactionCheckerUtils.emptyIfNull(pendingWithdrawalDTO.getSignMessage()));

      // Signature ...
      pendingWithdrawalDTO.setSignature(TransactionCheckerUtils.emptyIfNull(pendingWithdrawalDTO.getSignature()));

      // Amount ...
      pendingWithdrawalDTO.setAmount(TransactionCheckerUtils.zeroIfNull(pendingWithdrawalDTO.getAmount()));
    }
  }

  /**
   * 
   */
  private OpenTransactionDTOList processOpenTransaction(@Nonnull OpenTransactionDTOList apiOpenTransactionDTOList) {
    LOGGER.trace("processOpenTransaction() ...");

    // Check 1: Transaction Hex Size ...
    OpenTransactionDTOList workOpenTransactionDTOList = checkSize(apiOpenTransactionDTOList);

    // Check 2: Transaction (Withdrawal) Duplicated ...
    workOpenTransactionDTOList = checkDuplicated(workOpenTransactionDTOList);

    // Check 3: Transaction Signature ...
    return checkTransactionSignature(workOpenTransactionDTOList);
  }

  /**
   * 
   */
  private OpenTransactionDTOList checkSize(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("checkSize() ...");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      if (doCheckSize(openTransactionDTO)) {
        checkedOpenTransactionDTOList.add(openTransactionDTO);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doCheckSize(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("doCheckSize() ...");

    boolean isValid;

    try {
      int rawTransactionMaxSize = ConfigPropertyProvider.getInstance().getIntValueOrDefault(PropertyEnum.RAW_TRANSACTION_MAX_SIZE, 250000);

      String hex = openTransactionDTO.getRawTx().getHex();

      isValid = rawTransactionMaxSize > hex.length();

      if (!isValid) {
        openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - size too big");
        sendInvalidated(openTransactionDTO);
      }
    } catch (Exception e) {
      LOGGER.error("doCheckSize", e);
      isValid = false;
    }

    return isValid;
  }

  /**
   * 
   */
  private OpenTransactionDTOList checkDuplicated(@Nonnull OpenTransactionDTOList apiOpenTransactionDTOList) {
    LOGGER.trace("checkDuplicated() ...");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    // ...
    if (!apiOpenTransactionDTOList.isEmpty()) {
      Connection connection = null;

      try {
        connection = H2DBManager.getInstance().openConnection();

        openStatements(connection);

        for (OpenTransactionDTO apiOpenTransactionDTO : apiOpenTransactionDTOList) {
          if (doCheckInsertApiDuplicate(apiOpenTransactionDTO)) {
            checkedOpenTransactionDTOList.add(apiOpenTransactionDTO);
          }
        }

        closeStatements();

        connection.commit();
      } catch (Exception e) {
        DatabaseUtils.rollback(connection);
        LOGGER.error("checkDuplicated", e);
      } finally {
        H2DBManager.getInstance().closeConnection(connection);
      }
    }

    return checkedOpenTransactionDTOList;
  }

  /**
   * 
   */
  private boolean doCheckInsertApiDuplicate(@Nonnull OpenTransactionDTO apiOpenTransactionDTO) {
    LOGGER.trace("doCheckInsertApiDuplicate() ...");

    boolean isValid;

    try {
      Integer payloadId = getPayloadId(apiOpenTransactionDTO);

      if (null == payloadId) {
        isValid = true;
      } else {
        isValid = apiDuplicateCheckInsert(payloadId, apiOpenTransactionDTO.getId());

        if (!isValid) {
          apiOpenTransactionDTO.setInvalidatedReason("[Withdrawal] ID: " + payloadId + " - duplicated");
          sendInvalidated(apiOpenTransactionDTO);
        }
      }
    } catch (Exception e) {
      LOGGER.error("doCheckInsertApiDuplicate", e);
      isValid = false;
    }

    return isValid;
  }

  /**
   * 
   */
  private boolean apiDuplicateCheckInsert(
      @Nonnull Integer payloadId,
      @Nonnull String transactionId) {
    LOGGER.trace("apiDuplicateCheckInsert() ...");

    boolean isValid;

    try {
      apiDuplicateCheckInsertStatement.setInt(1, payloadId);
      apiDuplicateCheckInsertStatement.setString(2, transactionId);
      apiDuplicateCheckInsertStatement.execute();

      isValid = true;
    } catch (Exception e) {
      LOGGER.info("apiDuplicateCheckInsert: WithdrawId=" + payloadId + " / TransactionId=" + transactionId);
      isValid = false;
    }

    return isValid;
  }

  /**
   * 
   */
  private void openStatements(@Nonnull Connection connection) throws DfxException {
    LOGGER.trace("openStatements() ...");

    try {
      String apiDuplicateCheckInsertSql = "INSERT INTO public.api_duplicate_check (withdrawal_id, transaction_id) VALUES (?, ?)";
      apiDuplicateCheckInsertStatement = connection.prepareStatement(apiDuplicateCheckInsertSql);
    } catch (Exception e) {
      throw new DfxException("openStatements", e);
    }
  }

  /**
   * 
   */
  private void closeStatements() throws DfxException {
    LOGGER.trace("closeStatements() ...");

    try {
      apiDuplicateCheckInsertStatement.close();
    } catch (Exception e) {
      throw new DfxException("closeStatements", e);
    }
  }

  /**
   * 
   */
  private OpenTransactionDTOList checkTransactionSignature(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("checkTransactionSignature() ...");

    OpenTransactionDTOList checkedOpenTransactionDTOList = new OpenTransactionDTOList();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
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

    boolean isValid;

    try {
      isValid = validateIssuerSignature(openTransactionDTO);

      if (!isValid) {
        openTransactionDTO.setInvalidatedReason("[Transaction] ID: " + openTransactionDTO.getId() + " - invalid signature");
        sendInvalidated(openTransactionDTO);
      }
    } catch (Exception e) {
      LOGGER.error("doCheckTransactionSignature", e);
      isValid = false;
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
   * List of all Transactions and the corresponding Withdrawals ...
   */
  private TransactionWithdrawalDTOList createTransactionWithdrawalDTOList(
      @Nonnull OpenTransactionDTOList openTransactionDTOList,
      @Nonnull PendingWithdrawalDTOList pendingWithdrawalDTOList) {
    TransactionWithdrawalDTOList transactionWithdrawalDTOList = new TransactionWithdrawalDTOList();

    Map<Integer, OpenTransactionDTO> withdrawalIdToOpenTransactionDTOMap = createWithdrawalIdToOpenTransactionDTOMap(openTransactionDTOList);
    Map<Integer, PendingWithdrawalDTO> withdrawalIdToPendingWithdrawalDTOMap = createWithdrawalIdToPendingWithdrawalDTOMap(pendingWithdrawalDTOList);

    for (Entry<Integer, OpenTransactionDTO> withdrawalIdToOpenTransactionDTOMapEntry : withdrawalIdToOpenTransactionDTOMap.entrySet()) {
      Integer withdrawalId = withdrawalIdToOpenTransactionDTOMapEntry.getKey();

      PendingWithdrawalDTO pendingWithdrawalDTO = withdrawalIdToPendingWithdrawalDTOMap.get(withdrawalId);

      if (null != pendingWithdrawalDTO) {
        TransactionWithdrawalDTO transactionWithdrawalDTO = new TransactionWithdrawalDTO();

        transactionWithdrawalDTO.setId(withdrawalId);
        transactionWithdrawalDTO.setOpenTransactionDTO(withdrawalIdToOpenTransactionDTOMapEntry.getValue());
        transactionWithdrawalDTO.setPendingWithdrawalDTO(pendingWithdrawalDTO);

        transactionWithdrawalDTOList.add(transactionWithdrawalDTO);
      }
    }

    return transactionWithdrawalDTOList;
  }

  /**
   * 
   */
  private Map<Integer, OpenTransactionDTO> createWithdrawalIdToOpenTransactionDTOMap(@Nonnull OpenTransactionDTOList openTransactionDTOList) {
    LOGGER.trace("createWithdrawalIdToOpenTransactionDTOMap() ...");

    Map<Integer, OpenTransactionDTO> withdrawalIdToOpenTransactionDTOMap = new HashMap<>();

    for (OpenTransactionDTO openTransactionDTO : openTransactionDTOList) {
      Integer payloadId = getPayloadId(openTransactionDTO);

      if (null != payloadId) {
        withdrawalIdToOpenTransactionDTOMap.put(payloadId, openTransactionDTO);
      }
    }

    return withdrawalIdToOpenTransactionDTOMap;
  }

  /**
   * 
   */
  private Map<Integer, PendingWithdrawalDTO> createWithdrawalIdToPendingWithdrawalDTOMap(@Nonnull PendingWithdrawalDTOList pendingWithdrawalDTOList) {
    LOGGER.trace("createWithdrawalIdToPendingWithdrawalDTOMap() ...");

    Map<Integer, PendingWithdrawalDTO> withdrawalIdToPendingWithdrawalDTOMap = new HashMap<>();

    for (PendingWithdrawalDTO pendingWithdrawalDTO : pendingWithdrawalDTOList) {
      Integer pendingWithdrawalId = pendingWithdrawalDTO.getId();

      if (null != pendingWithdrawalId) {
        withdrawalIdToPendingWithdrawalDTOMap.put(pendingWithdrawalId, pendingWithdrawalDTO);
      }
    }

    return withdrawalIdToPendingWithdrawalDTOMap;
  }

  /**
   * 
   */
  private @Nullable Integer getPayloadId(@Nonnull OpenTransactionDTO openTransactionDTO) {
    LOGGER.trace("getPayloadId() ...");

    Integer payloadId = null;

    OpenTransactionPayloadDTO openTransactionPayloadDTO = openTransactionDTO.getPayload();

    if (null != openTransactionPayloadDTO) {
      payloadId = openTransactionPayloadDTO.getId();
    }

    return payloadId;
  }

  /**
   * 
   */
  private void processPendingWithdrawal(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("processPendingWithdrawal() ...");

    TransactionWithdrawalDTOList checkWithdrawalMessage = checkWithdrawalMessage(transactionWithdrawalDTOList);

//   checkWithdrawalBalance(checkedtransactionWithdrawalDTOList);
  }

  /**
   * 
   */
  private TransactionWithdrawalDTOList checkWithdrawalMessage(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) {
    LOGGER.trace("checkWithdrawalMessage() ...");

    // Check 1: Check the sign message format ...
    TransactionWithdrawalDTOList checktransactionWithdrawalDTOList = withdrawalManager.checkSignMessageFormat(transactionWithdrawalDTOList);

    // Check 2: Check the message signature ...
    return withdrawalManager.checkSignMessageSignature(checktransactionWithdrawalDTOList);
  }

  /**
   * 
   */
  private void send(@Nonnull TransactionWithdrawalDTOList transactionWithdrawalDTOList) throws DfxException {
    LOGGER.trace("send() ...");

    for (TransactionWithdrawalDTO transactionWithdrawalDTO : transactionWithdrawalDTOList) {
      OpenTransactionDTO openTransactionDTO = transactionWithdrawalDTO.getOpenTransactionDTO();

      // TODO: End state must be: VALID
      // TODO: in final implementation ...
      if (TransactionWithdrawalStateEnum.SIGNATURE_CHECKED == transactionWithdrawalDTO.getState()) {
        sendVerified(openTransactionDTO);
      } else {
        openTransactionDTO.setInvalidatedReason(transactionWithdrawalDTO.getStateReason());
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
    openTransactionInvalidatedDTO.setReason(openTransactionDTO.getInvalidatedReason());

    apiAccessHandler.sendOpenTransactionInvalidated(openTransactionDTO.getId(), openTransactionInvalidatedDTO);
  }
}
